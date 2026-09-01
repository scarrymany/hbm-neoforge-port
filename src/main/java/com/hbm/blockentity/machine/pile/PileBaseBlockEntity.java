package com.hbm.blockentity.machine.pile;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.machine.pile.BlockPile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityPileBaseMK2}. Stores core XYZ ({@code coreY=-999} unset).
 * Tick: loaded missing/invalid core → {@link BlockPile#breakPile}.
 */
public class PileBaseBlockEntity extends LoadedBaseBlockEntity implements ITickableBE {

    public PileCoreBlockEntity cachedCore;
    public int coreX;
    public int coreY = -999;
    public int coreZ;

    public PileBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public PileBaseBlockEntity setCore(int x, int y, int z) {
        this.coreX = x;
        this.coreY = y;
        this.coreZ = z;
        setChanged();
        return this;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        if (coreY < 0) return;
        PileCoreBlockEntity controller = getCore();
        BlockPos corePos = new BlockPos(coreX, coreY, coreZ);
        if ((controller == null || controller.isRemoved()) && level.isLoaded(corePos)) {
            BlockPile.breakPile(level, worldPosition, getBlockState());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        coreX = tag.getInt("cX");
        coreY = tag.getInt("cY");
        coreZ = tag.getInt("cZ");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("cX", coreX);
        tag.putInt("cY", coreY);
        tag.putInt("cZ", coreZ);
    }

    @Override
    public void setChanged() {
        if (this.level != null) {
            this.level.blockEntityChanged(this.worldPosition);
        }
    }

    public PileCoreBlockEntity getCore() {
        if (cachedCore != null && !cachedCore.isRemoved()) return cachedCore;
        if (level == null) return null;
        BlockPos corePos = new BlockPos(coreX, coreY, coreZ);
        if (level.isLoaded(corePos)) {
            BlockEntity tile = level.getBlockEntity(corePos);
            if (tile instanceof PileCoreBlockEntity core) {
                cachedCore = core;
                return core;
            }
        }
        return null;
    }
}
