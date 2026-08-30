package com.hbm.blocks.bomb;

import com.hbm.blockentity.bomb.NukeCasingBlockEntities;
import com.hbm.blockentity.bomb.NukeN2BlockEntity;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Ported from CE's {@code NukeN2} (216 lines, read in full) - yield scales with charge count, no fallout ({@code statFacNoRad}). */
public class NukeN2Block extends NukeCasingBlockBase {

    public NukeN2Block(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukeN2BlockEntity(NukeCasingBlockEntities.NUKE_N2.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    private void igniteTestBomb(Level level, @Nullable Entity detonator, BlockPos pos, int radius) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);

        EntityNukeExplosionMK5 mk5 = EntityNukeExplosionMK5.statFacNoRad(level, radius, x, y, z).setDetonator(detonator);
        if (detonator == null && level.getBlockEntity(pos) instanceof NukeN2BlockEntity n2) {
            mk5.detonator = n2.placerID;
        }
        level.addFreshEntity(mk5);

        if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
            EntityNukeTorex.statFac(level, x, y, z, radius);
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (!(level.getBlockEntity(pos) instanceof NukeN2BlockEntity be)) return BombReturnCode.UNDEFINED;

        int charges = be.countCharges();
        if (charges > 0) {
            be.clearSlots();
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            int radius = (int) (BombConfig.N2_RADIUS.get() * charges / 12F);
            igniteTestBomb(level, detonator, pos, radius);
            return BombReturnCode.DETONATED;
        }
        return BombReturnCode.ERROR_MISSING_COMPONENT;
    }
}
