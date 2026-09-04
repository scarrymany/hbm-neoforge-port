package com.hbm.main;

import com.hbm.damage.ModDamageTypes;
import com.hbm.handler.ArmorModHandler;
import com.hbm.hazard.HazardSystem;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.items.food.FoodDataComponents;
import com.hbm.potion.HbmPotionEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Random;

/**
 * Game-bus per-entity tick dispatch, split out of {@link CommonEvents} - see that class's javadoc for
 * why {@link EntityTickEvent.Pre} (a game-bus event) cannot share one {@code @EventBusSubscriber}
 * class with {@code FMLCommonSetupEvent} (a mod-bus event). This class uses the annotation's default
 * {@code bus = Bus.GAME}, which is correct here (unlike {@link CommonEvents}).
 */
@EventBusSubscriber(modid = MainRegistry.MODID)
public class CommonTickEvents {

    private static final Random RAND = new Random();

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

    /** Exact CE {@code ModEventHandler.onLivingUpdate} armor-mod tick ({@code :1229-1234}). */
    private static void tickArmorMods(LivingEntity living) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) continue;
            ItemStack armor = living.getItemBySlot(slot);
            if (armor.isEmpty() || !ArmorModHandler.hasMods(armor)) continue;
            for (ItemStack mod : ArmorModHandler.pryMods(armor)) {
                if (!mod.isEmpty() && mod.getItem() instanceof ItemArmorMod armorMod) {
                    armorMod.modUpdate(living, armor);
                }
            }
        }
    }
}
