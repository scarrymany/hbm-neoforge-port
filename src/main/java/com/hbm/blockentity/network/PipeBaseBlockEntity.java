package com.hbm.blockentity.network;

import com.hbm.api.fluidmk2.FluidNode;
import com.hbm.api.fluidmk2.IFluidPipeMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.network.IBlockFluidDuct;
import com.hbm.capability.HbmPlayerAttachment;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.Library;
import com.hbm.uninos.UniNodespace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Default fluid-duct block entity, ported from CE's {@code com.hbm.tileentity.network.TileEntityPipeBaseNT}
 * (215 lines, read in full) - the shared base for every fluid-duct {@code BlockEntity} in this package
 * except {@link PipeExhaustBlockEntity} (which extends {@link LoadedBaseBlockEntity} directly, matching
 * CE's own {@code TileEntityPipeExhaust extends TileEntity}).
 *
 * <p>On the first server tick after load (and after every {@code #setFluidType} call, which drops the
 * node), asks {@link UniNodespace#getNode} for an existing {@link FluidNode} at this position or
 * creates and registers one via {@link IFluidPipeMK2#createNode}; {@link #canUpdate()} then stops
 * re-checking once a valid net is attached (mirroring CE's own "only update until a power net is
 * formed" comment, this port's fluid-network equivalent of
 * {@code com.hbm.blockentity.machine}'s HE-side conductor tiles once those exist).
 *
 * <p>{@link #getCachedConnectionMask}/{@link #invalidateConnectionCache} exist for
 * {@link com.hbm.blocks.network.FluidDuctBaseBlock}'s render/collision hooks once a later phase adds
 * connection-aware baked models (deferred per {@code docs/phase2/network_fluid_ducts.md}'s "Deferred
 * scope" - datagen for connection-aware pipe models is a Phase 5 concern) - the mask itself is real,
 * computed via {@link Library#canConnectFluid}, and already useful today for {@code IAnalyzable}
 * debug info; only the client remesh trigger CE fired on invalidation
 * ({@code World#markBlockRangeForRenderUpdate}) is dropped, since nothing reads this mask for
 * rendering yet.
 */
public class PipeBaseBlockEntity extends LoadedBaseBlockEntity
        implements IFluidPipeMK2, ITickableBE, ICachedPipeConnections, IFluidCopiable {

    protected FluidNode node;
    protected FluidType type = Fluids.NONE;

    private byte cachedConnectionMask;
    private boolean cachedConnectionMaskValid;

    public PipeBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public byte getCachedConnectionMask(BlockGetter access) {
        if (level != null && level.isClientSide) {
            return computeConnectionMask(access);
        }
        if (!this.cachedConnectionMaskValid) {
            this.cachedConnectionMask = computeConnectionMask(access);
            this.cachedConnectionMaskValid = true;
        }
        return this.cachedConnectionMask;
    }

    @Override
    public void invalidateConnectionCache() {
        this.cachedConnectionMaskValid = false;
    }

    private byte computeConnectionMask(BlockGetter access) {
        byte mask = 0;
        for (Direction facing : Direction.values()) {
            BlockPos adj = worldPosition.relative(facing);
            if (access instanceof Level lvl && !lvl.isLoaded(adj)) continue;
            if (Library.canConnectFluid(access, adj, facing, this.type)) {
                mask |= (byte) (1 << facing.get3DDataValue());
            }
        }
        return mask;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            invalidateConnectionCache();
            for (Direction facing : Direction.values()) {
                BlockPos neighborPos = worldPosition.relative(facing);
                if (!level.isLoaded(neighborPos)) continue;
                if (level.getBlockEntity(neighborPos) instanceof ICachedPipeConnections cached) {
                    cached.invalidateConnectionCache();
                }
            }
        }
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        if (!canUpdate()) return;

        if (this.node == null || this.node.expired) {
            if (shouldCreateNode()) {
                this.node = (FluidNode) UniNodespace.getNode(level, worldPosition, type.getNetworkProvider());

                if (this.node == null || this.node.expired) {
                    this.node = this.createNode(type);
                    UniNodespace.createNode(level, this.node);
                }
            }
        }
    }

    public boolean shouldCreateNode() {
        return true;
    }

    public FluidType getFluidType() {
        return this.type;
    }

    public void setType(FluidType type) {
        if (this.type == type) return;
        this.type = type;
        invalidateConnectionCache();
        setChanged();

        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
            IConnectionAnchors.notifyAnchors(this);
        }

        if (this.node != null) {
            UniNodespace.destroyNode(level, node);
            this.node = null;
        }
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        return dir != null && type == this.type;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide && this.node != null) {
            UniNodespace.destroyNode(level, node);
        }
    }

    /**
     * Only update until a fluid net is formed, in &gt;99% of cases the first tick. Everything else is
     * handled by neighbors and the net itself - matches CE's own {@code TileEntityPipeBaseNT#canUpdate}.
     */
    public boolean canUpdate() {
        return (this.node == null || this.node.net == null || !this.node.net.isValid()) && !this.isRemoved();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(type.getID());
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.type = Fluids.fromID(buf.readInt());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.type = Fluids.readType(tag, "type");
        invalidateConnectionCache();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        Fluids.writeType(tag, "type", this.type);
    }

    @Override
    public int[] getFluidIDToCopy() {
        return new int[] { type.getID() };
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return null;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level world, Player player, BlockPos pos) {
        int[] ids = nbt.getIntArray("fluidID");
        if (ids.length == 0) return;

        FluidType fluid = Fluids.fromID(index < ids.length ? ids[index] : 0);

        HbmPlayerAttachment data = HbmPlayerAttachment.getData(player);
        if (data.getKeyPressed(EnumKeybind.TOOL_CTRL)
                && world.getBlockState(worldPosition).getBlock() instanceof IBlockFluidDuct duct) {
            duct.changeTypeRecursively(world, worldPosition, getFluidType(), fluid, 64);
        } else {
            this.setType(fluid);
        }
    }
}
