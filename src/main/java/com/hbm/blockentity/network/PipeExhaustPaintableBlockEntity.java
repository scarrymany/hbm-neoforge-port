package com.hbm.blockentity.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Paintable exhaust duct, ported from CE's
 * {@code FluidDuctPaintableBlockExhaust$TileEntityPipeExhaustPaintable}. Same scope reduction as
 * {@link PipePaintableBlockEntity} (disguise-block state kept, CTM/baked-model rendering deferred) -
 * see that class's javadoc - layered onto {@link PipeExhaustBlockEntity} instead of
 * {@link PipeBaseBlockEntity} since the exhaust variant needs the three-smoke-node lifecycle, exactly
 * matching CE's own {@code extends TileEntityPipeExhaust}.
 */
public class PipeExhaustPaintableBlockEntity extends PipeExhaustBlockEntity {

    @Nullable
    private Block disguise;

    public PipeExhaustPaintableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public Block getDisguise() {
        return disguise;
    }

    public void setDisguise(@Nullable Block disguise) {
        this.disguise = disguise;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("disguise")) {
            this.disguise = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(tag.getString("disguise")));
        } else {
            this.disguise = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (disguise != null) {
            tag.putString("disguise", BuiltInRegistries.BLOCK.getKey(disguise).toString());
        }
    }

    @Override
    public void serializeInitial(RegistryFriendlyByteBuf buf) {
        super.serializeInitial(buf);
        buf.writeUtf(disguise != null ? BuiltInRegistries.BLOCK.getKey(disguise).toString() : "");
    }

    @Override
    public void deserializeInitial(RegistryFriendlyByteBuf buf) {
        super.deserializeInitial(buf);
        String id = buf.readUtf();
        this.disguise = id.isEmpty() ? null : BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
    }
}
