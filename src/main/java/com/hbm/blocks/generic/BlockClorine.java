package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Chlorine gas cloud, ported from CE's {@code BlockClorine}. CE relies on vanilla's random tick
 * ({@code setTickRandomly(true)} + {@code updateTick} clearing the block) to make the cloud
 * eventually disperse; {@link Properties#randomTicks()} plus {@link #randomTick} reproduces that
 * exact mechanic. Fully self-contained otherwise: the CE original gates its potion effects on
 * {@code ArmorRegistry.HazardClass.GAS_LUNG} protection, which the port already carries over
 * ({@link com.hbm.util.ArmorRegistry}); the accompanying filter-wear helper
 * ({@code ArmorUtil.damageGasMaskFilter}) has no port equivalent yet (items-area concern), so a
 * protected entity simply takes no damage here instead of wearing down its filter.
 */
public class BlockClorine extends Block {

    public BlockClorine(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        if (entity instanceof Player player && (player.isSpectator() || player.isCreative())) {
            return;
        }

        if (com.hbm.util.ArmorRegistry.hasAllProtection(living, EquipmentSlot.HEAD, com.hbm.util.ArmorRegistry.HazardClass.GAS_LUNG)) {
            return;
        }

        living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5 * 20, 0));
        living.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 20, 2));
        living.addEffect(new MobEffectInstance(MobEffects.WITHER, 20, 1));
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30 * 20, 1));
        living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30 * 20, 2));
    }
}
