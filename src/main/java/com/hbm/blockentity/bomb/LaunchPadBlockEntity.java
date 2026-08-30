package com.hbm.blockentity.bomb;

import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code com.hbm.tileentity.bomb.TileEntityLaunchPad} (162 lines, read in full) -
 * the small single-missile pad. {@code isReadyForLaunch() = delay <= 0} (a 100-tick post-launch
 * cooldown), {@code getLaunchOffset() = 1D}.
 * <p>
 * <b>Not ported</b>: CE's client-side smoke-particle spawn on missile-entity-detected-above
 * ({@code HbmEffectNT.LaunchSmoke}) - {@code com.hbm.particle}/{@code HbmEffectNT} do not exist
 * anywhere in this port (Phase 5 client/particle scope, same gap {@link
 * com.hbm.entity.missile.EntityMissileBaseNT}'s own javadoc documents for {@code spawnContrail}).
 */
public class LaunchPadBlockEntity extends LaunchPadBaseBlockEntity {

    public int delay = 0;

    public LaunchPadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 7);
    }

    @Override
    public boolean isReadyForLaunch() {
        return delay <= 0;
    }

    @Override
    public double getLaunchOffset() {
        return 1D;
    }

    @Override
    public void updateEntity() {
        if (level != null && !level.isClientSide) {
            if (this.delay > 0) delay--;

            if (!this.isMissileValid() || !this.hasFuel()) {
                this.delay = 100;
            }

            if (!this.hasFuel() || !this.isMissileValid()) {
                this.state = STATE_MISSING;
            } else {
                this.state = this.delay > 0 ? STATE_LOADING : STATE_READY;
            }
        }

        super.updateEntity();
    }

    @Override
    public void finalizeLaunch(Entity missile) {
        super.finalizeLaunch(missile);
        this.delay = 100;
    }

    @Override
    public DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.getX() + 2, p.getY(), p.getZ() - 1, Direction.EAST),
                new DirPos(p.getX() + 2, p.getY(), p.getZ() + 1, Direction.EAST),
                new DirPos(p.getX() - 2, p.getY(), p.getZ() - 1, Direction.WEST),
                new DirPos(p.getX() - 2, p.getY(), p.getZ() + 1, Direction.WEST),
                new DirPos(p.getX() - 1, p.getY(), p.getZ() + 2, Direction.SOUTH),
                new DirPos(p.getX() + 1, p.getY(), p.getZ() + 2, Direction.SOUTH),
                new DirPos(p.getX() - 1, p.getY(), p.getZ() - 2, Direction.NORTH),
                new DirPos(p.getX() + 1, p.getY(), p.getZ() - 2, Direction.NORTH)
        };
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.delay = tag.getInt("delay");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("delay", delay);
    }
}
