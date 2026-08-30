package com.hbm.blocks.bomb;

import com.hbm.blockentity.bomb.NukeCasingBlockEntities;
import com.hbm.blockentity.bomb.NukeManBlockEntity;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code NukeMan} (242 lines, read in full). CE derives {@code FACING} from a
 * {@code PropertyInteger(2,5)} set via {@code MathHelper.floor(yaw*4/360+0.5)&3} - traced against
 * vanilla 1.12's {@code EntityLivingBase.getHorizontalFacing()} index order (SOUTH,WEST,NORTH,EAST),
 * this reduces exactly to {@code getHorizontalFacing().getCounterClockWise()}: i=0 SOUTH->EAST(5),
 * i=1 WEST->SOUTH(3), i=2 NORTH->WEST(4), i=3 EAST->NORTH(2), matching CE's own per-branch results
 * one-for-one. Re-expressed here as the equivalent, much shorter {@link net.minecraft.core.Direction#getCounterClockWise()}
 * call rather than a hand re-derived {@code &3} index switch.
 */
public class NukeManBlock extends NukeCasingBlockBase {

    public NukeManBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getCounterClockWise());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukeManBlockEntity(NukeCasingBlockEntities.NUKE_MAN.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    private void igniteTestBomb(Level level, @Nullable Entity detonator, BlockPos pos) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);

        int radius = BombConfig.MAN_RADIUS.get();
        EntityNukeExplosionMK5 mk5 = EntityNukeExplosionMK5.statFac(level, radius, x, y, z).setDetonator(detonator);
        if (detonator == null && level.getBlockEntity(pos) instanceof NukeManBlockEntity man) {
            mk5.detonator = man.placerID;
        }
        level.addFreshEntity(mk5);

        if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
            EntityNukeTorex.statFac(level, x, y, z, radius);
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;
        if (!(level.getBlockEntity(pos) instanceof NukeManBlockEntity be)) return BombReturnCode.UNDEFINED;
        if (be.isReady()) {
            be.clearSlots();
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            igniteTestBomb(level, detonator, pos);
            return BombReturnCode.DETONATED;
        }
        return BombReturnCode.ERROR_MISSING_COMPONENT;
    }
}
