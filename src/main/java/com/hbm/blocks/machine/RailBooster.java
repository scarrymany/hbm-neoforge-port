package com.hbm.blocks.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * CE {@code RailBooster} ({@code ModBlocks.java}:837 / {@code RailBooster.java}:14-17) — same
 * straight high-speed rail, multiplies cart velocity by {@code 1.15} on pass.
 */
public class RailBooster extends RailHighspeed {

    public static final MapCodec<RailBooster> CODEC = simpleCodec(RailBooster::new);

    public RailBooster(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseRailBlock> codec() {
        return CODEC;
    }

    @Override
    public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        Vec3 motion = cart.getDeltaMovement();
        cart.setDeltaMovement(motion.scale(1.15));
    }
}
