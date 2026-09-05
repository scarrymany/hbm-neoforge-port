package com.hbm.items.special;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.damage.ModDamageTypes;
import com.hbm.entity.effect.EntityVortex;
import com.hbm.entity.projectile.EntityMeteor;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.items.machine.MachineDataComponents;
import com.hbm.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;
import java.util.function.Supplier;

/**
 * Exact CE {@code ItemGlitch} {@code :46-183} + {@code IBatteryItem} {@code :195-217}
 * ({@code getCharge}/{@code getMaxCharge} 200, mutate no-op, discharge 200). Polaroid tooltip stay
 * skipped. Cases 5–7 ({@code getItemDropped} loot), 8 ({@code ammo_container} unregistered), 11–12
 * ({@code EntityBoxcar} is ThrownTail stub) stay skipped — no invent.
 */
public class ItemGlitch extends Item implements IBatteryItem {

    public ItemGlitch(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        stack.hurtAndBreak(5, player, LivingEntity.getSlotForHand(hand));

        if (!level.isClientSide()) {
            switch (level.getRandom().nextInt(31)) {
                case 0 -> player.displayClientMessage(Component.translatable("chat.glitch.0"), false);
                case 1 -> player.displayClientMessage(Component.translatable("chat.glitch.1"), false);
                case 2 -> player.hurt(level.damageSources().source(ModDamageTypes.RADIATION), 1000F);
                case 3 -> player.hurt(level.damageSources().source(ModDamageTypes.BOXCAR), 1000F);
                case 4 -> player.hurt(level.damageSources().source(ModDamageTypes.BLACK_HOLE), 1000F);
                case 9 -> give(player, "nuke_advanced_kit", 1);
                case 10 -> give(player, "nuke_starter_kit", 1);
                case 13 -> {
                    give(player, "bottle_rad", 1);
                    give(player, "geiger_counter", 1);
                    player.displayClientMessage(Component.translatable("chat.glitch.13a"), false);
                    player.displayClientMessage(Component.translatable("chat.glitch.13b"), false);
                }
                case 14 -> {
                    player.getInventory().dropAll();
                    ExplosionChaos.burn(level, null,
                            new BlockPos((int) player.getX(), (int) player.getY(), (int) player.getZ()), 5);
                }
                case 15 -> {
                    for (int i = 0; i < 36; i++) {
                        player.getInventory().add(new ItemStack(Blocks.DIRT, 64));
                    }
                }
                case 16 -> player.displayClientMessage(Component.translatable("chat.glitch.16"), false);
                case 17 -> player.displayClientMessage(Component.translatable("chat.glitch.17"), false);
                case 18 -> {
                    give(player, "nothing", 12);
                    player.displayClientMessage(Component.translatable("chat.glitch.18"), false);
                }
                case 19 -> player.displayClientMessage(Component.translatable("chat.glitch.19"), false);
                case 20 -> player.displayClientMessage(Component.translatable("chat.glitch.20"), false);
                case 21 -> {
                    give(player, "missile_nuclear", 1);
                    player.displayClientMessage(Component.translatable("chat.glitch.21"), false);
                }
                case 22 -> player.displayClientMessage(Component.translatable("chat.glitch.22"), false);
                case 23 -> player.displayClientMessage(Component.translatable("chat.glitch.23"), false);
                case 24 -> {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60 * 20, 9));
                    player.displayClientMessage(Component.translatable("chat.glitch.24"), false);
                }
                case 25 -> {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60 * 20, 9));
                    player.displayClientMessage(Component.translatable("chat.glitch.25"), false);
                }
                case 26 -> {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60 * 20, 9));
                    player.displayClientMessage(Component.translatable("chat.glitch.26"), false);
                }
                case 27 -> {
                    EntityVortex vortex = new EntityVortex(level, 2.5F);
                    vortex.setPos(player.getX(), player.getY() - 15, player.getZ());
                    level.addFreshEntity(vortex);
                }
                case 28 -> {
                    EntityMeteor meteor = new EntityMeteor(level);
                    meteor.setPos(player.getX(), player.getY() + 100, player.getZ());
                    level.addFreshEntity(meteor);
                    player.displayClientMessage(Component.translatable("chat.glitch.28"), false);
                }
                case 29 -> {
                    ExplosionLarge.spawnBurst(level, player.getX(), player.getY(), player.getZ(), 27, 3);
                    player.displayClientMessage(Component.translatable("chat.glitch.29"), false);
                }
                case 30 -> {
                    give(player, "plate_saturnite", 1);
                    player.displayClientMessage(Component.translatable("chat.glitch.30"), false);
                }
                default -> {
                    // 5-7 meteor-treasure getItemDropped, 8 ammo_container, 11-12 EntityBoxcar stub
                }
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("desc.glitch"));
    }

    private static void give(Player player, String path, int count) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
        if (item == Items.AIR) return;
        player.getInventory().add(new ItemStack(item, count));
    }

    @Override
    public Supplier<DataComponentType<Long>> getChargeComponent() {
        return MachineDataComponents.CHARGE;
    }

    @Override
    public void chargeBattery(ItemStack stack, long i) {}

    @Override
    public void setCharge(ItemStack stack, long i) {}

    @Override
    public void dischargeBattery(ItemStack stack, long i) {}

    @Override
    public long getCharge(ItemStack stack) {
        return 200;
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return 200;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return 0;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return 200;
    }
}
