package com.hbm.hazard.type;

import com.hbm.config.BombConfig;
import com.hbm.damage.ModDamageTypes;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.hazard.HazardComponents;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.ObjObjDoubleConsumer;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.function.ObjDoubleConsumer;

/**
 * Parametric strategy with a built-in decay-timer-to-nuclear-detonation behavior persisted via the
 * {@link HazardComponents#UNSTABLE_DECAY_TIMER} data component (formerly the raw {@code timer} NBT int), or fully
 * custom onUpdate/onDrop/tooltip lambdas.
 */
public class HazardTypeUnstable implements IHazardType {

    private final ObjObjDoubleConsumer<LivingEntity, ItemStack> onUpdate;
    private final ObjDoubleConsumer<ItemEntity> onDrop;
    private final HazardInfoConsumer customInfo;
    private int timer = -1;

    public HazardTypeUnstable(final ObjObjDoubleConsumer<LivingEntity, ItemStack> onUpdate, final ObjDoubleConsumer<ItemEntity> onDrop) {
        this(onUpdate, onDrop, null);
    }

    public HazardTypeUnstable(final int timer) {
        this(timer, null);
    }

    public HazardTypeUnstable(final ObjObjDoubleConsumer<LivingEntity, ItemStack> onUpdate, final ObjDoubleConsumer<ItemEntity> onDrop, final HazardInfoConsumer customInfo) {
        this.onUpdate = onUpdate;
        this.onDrop = onDrop;
        this.customInfo = customInfo;
    }

    public HazardTypeUnstable(final int timer, final HazardInfoConsumer customInfo) {
        if (timer <= 0) throw new IllegalArgumentException("timer must be greater than 0");
        this.timer = timer;
        this.onUpdate = (entity, stack, level) -> {
            final Level world = entity.level();
            final int count = stack.getCount();
            final int radius = scaledRadius(level, count);
            setTimer(stack, getTimer(stack) + 1);
            if (getTimer(stack) == this.timer && !world.isClientSide()) {
                stack.setCount(0);
                world.addFreshEntity(EntityNukeExplosionMK5.statFac(world, radius, entity.getX(), entity.getY(), entity.getZ()).setDetonator(entity));

                if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
                    EntityNukeTorex.statFac(world, entity.getX(), entity.getY(), entity.getZ(), radius);
                }
                world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), HBMSoundHandler.oldExplosion, SoundSource.PLAYERS, 1.0F, 1.0F);
                entity.hurt(entity.level().damageSources().source(ModDamageTypes.NUCLEAR_BLAST), 10000);
            }
        };
        this.onDrop = (itemEntity, level) -> {
            final Level world = itemEntity.level();
            final int radius = (int) level;
            setTimer(itemEntity.getItem(), getTimer(itemEntity.getItem()) + 1);

            if (getTimer(itemEntity.getItem()) == this.timer && !world.isClientSide()) {
                // CE credited the throwing player via EntityItem#getThrower(), which has no confirmed 1.21 equivalent;
                // the detonation runs without a credited detonator until the entity area supplies one.
                world.addFreshEntity(EntityNukeExplosionMK5.statFac(world, radius, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ()).setDetonator(null));

                if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
                    EntityNukeTorex.statFac(world, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), radius);
                }
                world.playSound(null, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), HBMSoundHandler.oldExplosion, SoundSource.PLAYERS, 1.0F, 1.0F);
                itemEntity.hurt(world.damageSources().source(ModDamageTypes.NUCLEAR_BLAST), 10000);
                itemEntity.discard();
            }
        };
        this.customInfo = customInfo;
    }

    @Override
    public void onUpdate(final LivingEntity target, final double level, final ItemStack stack) {
        onUpdate.accept(target, stack, level);
    }

    @Override
    public void updateEntity(final ItemEntity item, final double level) {
        onDrop.accept(item, level);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addHazardInformation(final Player player, final List<Component> tooltip, final double level, final ItemStack stack, final List<IHazardModifier> modifiers) {
        if (customInfo != null) {
            customInfo.accept(player, tooltip, level, stack, modifiers);
        } else if (this.timer != -1) {
            final int scaled = scaledRadius(level, stack.getCount());
            tooltip.add(Component.literal(I18nUtil.resolveKey("trait.unstable")).withStyle(ChatFormatting.DARK_RED));
            tooltip.add(Component.literal("Decay Time: " + (this.timer / 20) + "s - Explosion Radius: " + scaled + "m").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("Decay: " + (getTimer(stack) * 100 / this.timer) + "%").withStyle(ChatFormatting.RED));
        }
    }

    private static int scaledRadius(final double baseLevel, final int count) {
        if (count <= 1) return (int) baseLevel;
        final int r = (int) (baseLevel * Math.cbrt(count) + 0.5);
        return Math.max(1, r);
    }

    public static int getTimer(final ItemStack stack) {
        return stack.getOrDefault(HazardComponents.UNSTABLE_DECAY_TIMER.get(), 0);
    }

    public static void setTimer(final ItemStack stack, final int timer) {
        stack.set(HazardComponents.UNSTABLE_DECAY_TIMER.get(), timer);
    }
}
