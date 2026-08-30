package com.hbm.blockentity.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.interfaces.IDummy;
import com.hbm.interfaces.IMultiBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Ported from CE's {@code com.hbm.tileentity.machine.TileEntityDummy} (64 lines, read in full) - a
 * generic "dummy block with a back-reference" primitive, confirmed by
 * {@code docs/phase3/missile_launch_infra.md} to be a different, smaller mechanism than
 * {@link com.hbm.blocks.BlockDummyable}'s own internal master/dummy machinery (that framework's
 * dummy blocks are private to itself, not reusable by other blocks). Used by
 * {@link SiloHatchBlockEntity} to place a self-destructing marker ring in front of itself; a future
 * block could reuse this same class for an unrelated dummy-placement need.
 */
public class DummyBlockEntity extends LoadedBaseBlockEntity implements IDummy, ITickableBE {

    @Nullable
    public BlockPos target;
    private boolean needsMark = true;

    public DummyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (needsMark) {
            setChanged();
            needsMark = false;
        }

        if (target != null && !(level.getBlockState(target).getBlock() instanceof IMultiBlock)) {
            level.destroyBlock(worldPosition, false);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (target != null) {
            tag.putInt("tx", target.getX());
            tag.putInt("ty", target.getY());
            tag.putInt("tz", target.getZ());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("tx")) {
            this.target = new BlockPos(tag.getInt("tx"), tag.getInt("ty"), tag.getInt("tz"));
        } else {
            this.target = null;
        }
    }
}
