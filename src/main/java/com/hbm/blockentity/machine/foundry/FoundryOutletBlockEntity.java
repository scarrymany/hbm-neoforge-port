package com.hbm.blockentity.machine.foundry;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blocks.machine.foundry.BlockFoundryOutlet;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.lib.ForgeDirection;
import com.hbm.util.CrucibleUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

/**
 * NeoForge port of CE {@code TileEntityFoundryOutlet} - foundry outlet BlockEntity.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityFoundryOutlet.java
 * <p>
 * Accepts flow from channels and pours down to crucible acceptors below (CE :76-105).
 * Filter + inverted-redstone gate live (CE :33-36, :56-60). Client barrier re-render and
 * foundry particle packet stay deferred.
 */
public class FoundryOutletBlockEntity extends FoundryBaseBlockEntity implements ITickableBE {

    public NTMMaterial filter = null;
    public boolean invertFilter = false;
    public boolean invertRedstone = false;

    public FoundryOutletBlockEntity(BlockPos pos, BlockState state) {
        super(FoundryBlockEntities.FOUNDRY_OUTLET_BE_TYPE.get(), pos, state);
    }

    /** CE :33-36 — blocked when invertRedstone XOR neighbor power. */
    public boolean isClosed() {
        if (level == null) return invertRedstone;
        return invertRedstone ^ level.hasNeighborSignal(worldPosition);
    }

    @Override
    public void updateEntity() {
        // TODO(CE: TileEntityFoundryOutlet.java:42-49): client-side re-render on filter/redstone change
    }

    @Override
    public boolean canAcceptPartialFlow(Level world, BlockPos p, Direction side, Mats.MaterialStack stack) {
        // CE TileEntityFoundryOutlet.java:58-60
        if (filter != null && ((filter != stack.material) ^ invertFilter)) return false;
        if (isClosed()) return false;
        BlockState state = world.getBlockState(p);
        if (!(state.getBlock() instanceof BlockFoundryOutlet)) return false;
        
        Direction facing = ((BlockFoundryOutlet) state.getBlock()).getFacing(state);
        if (side != facing.getOpposite()) return false;

        Vec3 start = new Vec3(p.getX() + 0.5, p.getY() - 0.125, p.getZ() + 0.5);
        Vec3 end = new Vec3(p.getX() + 0.5, p.getY() + 0.125 - 4, p.getZ() + 0.5);

        net.minecraft.world.level.ClipContext context = new net.minecraft.world.level.ClipContext(start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, (net.minecraft.world.entity.Entity) null);
        BlockHitResult hit = world.clip(context);

        if (hit.getType() == HitResult.Type.MISS) return false;

        ICrucibleAcceptor acc = CrucibleUtil.getPouringTarget(world, hit);
        if (acc == null) return false;

        Vec3 hitVec = hit.getLocation();
        return acc.canAcceptPartialPour(world, hit.getBlockPos(), hitVec.x, hitVec.y, hitVec.z, Direction.UP, stack);
    }

    @Override
    public Mats.MaterialStack flow(Level world, BlockPos p, Direction side, Mats.MaterialStack stack) {
        Vec3 start = new Vec3(p.getX() + 0.5, p.getY() - 0.125, p.getZ() + 0.5);
        Vec3 end = new Vec3(p.getX() + 0.5, p.getY() + 0.125 - 4, p.getZ() + 0.5);

        net.minecraft.world.level.ClipContext context = new net.minecraft.world.level.ClipContext(start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, (net.minecraft.world.entity.Entity) null);
        BlockHitResult hit = world.clip(context);

        if (hit.getType() == HitResult.Type.MISS) return stack;

        ICrucibleAcceptor acc = CrucibleUtil.getPouringTarget(world, hit);
        if (acc == null) return stack;

        Vec3 hitVec = hit.getLocation();
        Mats.MaterialStack didPour = acc.pour(world, hit.getBlockPos(), hitVec.x, hitVec.y, hitVec.z, Direction.UP, stack);

        // TODO(CE: TileEntityFoundryOutlet.java:90-102): foundry particle packet

        return didPour;
    }

    @Override
    public int getCapacity() {
        return 0;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("invert", this.invertRedstone);
        tag.putBoolean("invertFilter", this.invertFilter);
        tag.putShort("filter", this.filter == null ? -1 : (short) this.filter.id);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        this.invertRedstone = tag.getBoolean("invert");
        this.invertFilter = tag.getBoolean("invertFilter");
        if (tag.contains("filter")) {
            int id = tag.getShort("filter");
            this.filter = (id == -1) ? null : Mats.matById.get(id);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    public void markAndSync() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }
}
