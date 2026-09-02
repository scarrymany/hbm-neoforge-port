package com.hbm.blockentity.machine;

import com.hbm.blockentity.machine.FloodlightBlockEntity;
import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class FloodlightBeamBlockEntity extends BlockEntity {

    public FloodlightBlockEntity cache;
    public int sourceX;
    public int sourceY;
    public int sourceZ;
    public int index;

    public FloodlightBeamBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.FLOODLIGHT_BEAM_ENTITY.get(), pos, state);
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        if (level.getGameTime() % 5 != 0) return;

        if (cache == null) {
            if (level.hasChunkAt(new BlockPos(sourceX, sourceY, sourceZ))) {
                BlockEntity tile = level.getBlockEntity(new BlockPos(sourceX, sourceY, sourceZ));
                if (tile instanceof FloodlightBlockEntity floodlight) {
                    cache = floodlight; // chunk is loaded, tile exists -> cache
                } else {
                    level.setBlock(getBlockPos(), Blocks.AIR.defaultBlockState(), 2); // chunk is loaded, tile does not exist -> delete self
                }
            }
        }

        if ((cache != null && (cache.isRemoved() || !cache.isOn || !getBlockPos().equals(cache.lightPos[index]))) || sourceY == 0) {
            level.setBlock(getBlockPos(), Blocks.AIR.defaultBlockState(), 2);
        }
    }

    public void setSource(FloodlightBlockEntity floodlight, int x, int y, int z, int i) {
        cache = floodlight;
        sourceX = x;
        sourceY = y;
        sourceZ = z;
        index = i;
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        this.sourceX = tag.getInt("sourceX");
        this.sourceY = tag.getInt("sourceY");
        this.sourceZ = tag.getInt("sourceZ");
        this.index = tag.getInt("index");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("sourceX", sourceX);
        tag.putInt("sourceY", sourceY);
        tag.putInt("sourceZ", sourceZ);
        tag.putInt("index", index);
    }
}
