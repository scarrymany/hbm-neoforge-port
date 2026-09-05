package com.hbm.blockentity.machine.rbmk;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

/**
 * CE: com.hbm.tileentity.machine.rbmk.TileEntityRBMKDisplay
 * RBMK display panel - scans a 7x7 grid of RBMK columns around a target position and syncs
 * their console data to client for rendering. No GUI - display-only.
 * Rotates with screwdriver (block handles onScrew).
 */
public class RBMKDisplayBlockEntity extends LoadedBaseBlockEntity implements ITickableBE {

    private int targetX;
    private int targetY;
    private int targetZ;

    private byte rotation;
    private AABB bb;

    public RBMKColumn[] columns = new RBMKColumn[7 * 7];

    public RBMKDisplayBlockEntity(BlockPos pos, BlockState state) {
        super(RBMKBlockEntities.DISPLAY.get(), pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (level.getGameTime() % 10 == 0) {
            rescan();
            networkPackNT(50);
        }
    }

    public void setTarget(int x, int y, int z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.setChanged();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);

        for (RBMKColumn column : this.columns) {
            RBMKColumn.writeToBuf(buf, column);
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);

        for (int i = 0; i < this.columns.length; i++) {
            this.columns[i] = RBMKColumn.readFromBuf(buf);
        }
    }

    private void rescan() {
        for (int index = 0; index < columns.length; index++) {
            int rx = getXFromIndex(index);
            int rz = getZFromIndex(index);

            BlockPos targetPos = new BlockPos(targetX + rx, targetY, targetZ + rz);
            BlockEntity te = level.getBlockEntity(targetPos);

            if (te instanceof RBMKBaseBlockEntity base) {
                columns[index] = base.getConsoleData();
            } else {
                columns[index] = null;
            }
        }
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(nbt, registries);

        this.targetX = nbt.getInt("tX");
        this.targetY = nbt.getInt("tY");
        this.targetZ = nbt.getInt("tZ");
        this.rotation = nbt.getByte("rotation");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(nbt, registries);

        nbt.putInt("tX", this.targetX);
        nbt.putInt("tY", this.targetY);
        nbt.putInt("tZ", this.targetZ);
        nbt.putByte("rotation", this.rotation);
    }

    public void rotate() {
        rotation = (byte) ((rotation + 1) % 4);
        this.setChanged();
    }

    public int getXFromIndex(int col) {
        int i = col % 7 - 3;
        int j = col / 7 - 3;
        return switch (rotation) {
            case 1 -> -j;
            case 2 -> -i;
            case 3 -> j;
            default -> i;
        };
    }

    public int getZFromIndex(int col) {
        int i = col % 7 - 3;
        int j = col / 7 - 3;
        return switch (rotation) {
            case 1 -> i;
            case 2 -> -j;
            case 3 -> -i;
            default -> j;
        };
    }

    public @NotNull AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + 1, worldPosition.getZ() + 1);
        return bb;
    }
}
