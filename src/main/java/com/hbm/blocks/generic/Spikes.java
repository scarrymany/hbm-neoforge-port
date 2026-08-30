package com.hbm.blocks.generic;

import com.hbm.damage.ModDamageTypes;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Damage-on-touch spikes, ported from CE's {@code Spikes}. Uses the port's data-driven
 * {@link ModDamageTypes#SPIKES} damage type (CE's {@code ModDamageSource.spikes}).
 */
public class Spikes extends Block {

    public Spikes(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof LivingEntity && entity.getDeltaMovement().y < -0.1) {
            boolean hurt = entity.hurt(level.damageSources().source(ModDamageTypes.SPIKES), 100F);
            if (hurt) {
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        HBMSoundHandler.slicer.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            }
        }
    }
}
