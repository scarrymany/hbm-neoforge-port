package com.hbm.main;

import com.google.common.collect.Multimap;
import com.hbm.damage.ModDamageTypes;
import com.hbm.handler.ArmorModHandler;
import com.hbm.hazard.HazardSystem;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.items.food.FoodDataComponents;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.potion.HbmPotionEffects;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerFlyableFallEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * Game-bus per-entity tick dispatch, split out of {@link CommonEvents} - see that class's javadoc for
 * why {@link EntityTickEvent.Pre} (a game-bus event) cannot share one {@code @EventBusSubscriber}
 * class with {@code FMLCommonSetupEvent} (a mod-bus event). This class uses the annotation's default
 * {@code bus = Bus.GAME}, which is correct here (unlike {@link CommonEvents}).
 */
@EventBusSubscriber(modid = MainRegistry.MODID)
public class CommonTickEvents {

    private static final Random RAND = new Random();

    /** CE {@code EntityLivingBase.armorArray} — previous-tick armor, FEET→HEAD. */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };
    private static final Map<LivingEntity, ItemStack[]> LAST_ARMOR = new WeakHashMap<>();

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();

        if(entity instanceof Player player) {
            HazardSystem.updatePlayerInventory(player);
        }
        if(entity instanceof ItemEntity itemEntity) {
            HazardSystem.updateDroppedItem(itemEntity);
        }
        if(entity instanceof LivingEntity livingEntity) {
            HazardSystem.updateLivingInventory(livingEntity);
            tickArmorMods(livingEntity);
        }
    }

    /**
     * Exact CE {@code ModEventHandler.onPlayerTick} {@code :871-873} — {@code PlayerTickEvent}
     * Phase.START. Port {@link PlayerTickEvent.Pre} is that phase.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        if (player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmorFSB) {
            ArmorFSB.handleTick(player);
        }
    }

    /**
     * Exact CE {@code ModEventHandler.onEntityJump} {@code :1255-1257}.
     */
    @SubscribeEvent
    public static void onEntityJump(LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof Player player
                && player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmorFSB) {
            ArmorFSB.handleJump(player);
        }
    }

    /**
     * Exact CE {@code ModEventHandler.onEntityFall} {@code :814-816} — {@code EntityPlayerMP} only.
     */
    @SubscribeEvent
    public static void onEntityFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            ArmorFSB.handleFall(serverPlayer);
        }
    }

    /**
     * Exact CE {@code ModEventHandler.onPlayerFall} {@code :809-810} ({@code PlayerFlyableFallEvent}).
     */
    @SubscribeEvent
    public static void onPlayerFlyableFall(PlayerFlyableFallEvent event) {
        ArmorFSB.handleFall(event.getEntity());
    }

    /**
     * CE {@code ModEventHandler.onFoodEaten} :1481-1500 — cyanide/red pill poisoned food handler.
     * Reads {@link FoodDataComponents#CYANIDE} / {@link FoodDataComponents#RED_PILL} from consumed food.
     */
    @SubscribeEvent
    public static void onFoodEaten(LivingEntityUseItemEvent.Finish event) {
        ItemStack stack = event.getItem();
        if (stack.isEmpty() || stack.getFoodProperties(null) == null) return;

        Boolean cyanide = stack.get(FoodDataComponents.CYANIDE.get());
        if (cyanide != null && cyanide) {
            for (int i = 0; i < 10; i++) {
                var damageType = RAND.nextBoolean() ? ModDamageTypes.EUTHANIZED_SELF : ModDamageTypes.EUTHANIZED_SELF_2;
                event.getEntity().hurt(event.getEntity().damageSources().source(damageType), 1000F);
            }
        }

        Boolean redPill = stack.get(FoodDataComponents.RED_PILL.get());
        if (redPill != null && redPill) {
            event.getEntity().addEffect(new MobEffectInstance(HbmPotionEffects.DEATH, 60 * 60 * 20, 0));
        }
    }

    /**
     * Exact CE {@code ModEventHandler.onLivingUpdate} {@code :1205-1245}:
     * {@code reapply} on armor-stack change, {@code removeAttributeModifiers} from prev mods,
     * {@code modUpdate} every tick, {@code applyAttributeModifiers} on reapply.
     * IEquipReceiver / ItemModDefuser stay skipped.
     */
    private static void tickArmorMods(LivingEntity living) {
        ItemStack[] last = LAST_ARMOR.get(living);
        if (last == null) {
            last = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
        }
        ItemStack[] next = new ItemStack[4];
        for (int i = 0; i < ARMOR_SLOTS.length; i++) {
            ItemStack prev = last[i];
            ItemStack armor = living.getItemBySlot(ARMOR_SLOTS[i]);
            next[i] = armor.copy();
            boolean reapply = !ItemStack.matches(prev, armor);

            if (reapply && ArmorModHandler.hasMods(prev)) {
                for (ItemStack mod : ArmorModHandler.pryMods(prev)) {
                    if (!mod.isEmpty() && mod.getItem() instanceof ItemArmorMod armorMod) {
                        applyModAttributes(living, armorMod, prev, false);
                    }
                }
            }

            if (ArmorModHandler.hasMods(armor)) {
                for (ItemStack mod : ArmorModHandler.pryMods(armor)) {
                    if (!mod.isEmpty() && mod.getItem() instanceof ItemArmorMod armorMod) {
                        armorMod.modUpdate(living, armor);
                        if (reapply) {
                            applyModAttributes(living, armorMod, armor, true);
                        }
                    }
                }
            }
        }
        LAST_ARMOR.put(living, next);
    }

    @SuppressWarnings("unchecked")
    private static void applyModAttributes(LivingEntity living, ItemArmorMod armorMod, ItemStack armor, boolean add) {
        Multimap<Holder<Attribute>, AttributeModifier> map =
                (Multimap<Holder<Attribute>, AttributeModifier>) (Multimap<?, ?>) armorMod.getModifiers(armor);
        if (map == null) return;
        if (add) {
            living.getAttributes().addTransientAttributeModifiers(map);
        } else {
            living.getAttributes().removeAttributeModifiers(map);
        }
    }
}
