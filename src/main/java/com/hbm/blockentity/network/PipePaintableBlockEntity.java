package com.hbm.blockentity.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Colorable/disguisable fluid duct, ported from CE's {@code FluidDuctPaintable$TileEntityPipePaintable}
 * inner class. Keeps CE's disguise-block state (right-click with a full opaque block to disguise this
 * duct as it, screwdriver to remove) but drops the CTM ({@code team.chisel.ctm.api.IFacade}) render
 * hook and the baked-model quad assembly entirely - both pure rendering concerns, and both flagged as
 * unresolved cross-package questions by {@code docs/phase2/network_fluid_ducts.md}'s Open questions
 * ("whoever covers that area to confirm whether the color state is TE NBT ... or ItemStack NBT")
 * rather than resolved here. The disguise state itself (which real block+state this duct is dressed
 * as) is real TE NBT either way, so it is kept and synced now; only the CTM-specific facade lookup and
 * the client-side quad baking are deferred to Phase 5.
 */
public class PipePaintableBlockEntity extends PipeBaseBlockEntity {

    @Nullable
    private Block disguise;

    public PipePaintableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
            Block stored = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(tag.getString("disguise")));
            this.disguise = stored == Blocks.AIR ? null : stored;
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
