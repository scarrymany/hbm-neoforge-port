package com.hbm.blockentity.machine.rbmk;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.machine.rbmk.RBMKConsoleMenu;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * RBMK reactor console - the "bespoke RBMK-specific structure scan" this task's instructions flagged
 * as the alternative to {@code BlockDummyable}/{@code MultiblockHandlerXR}: unlike every concrete
 * column (each its own 1x1xN {@code BlockDummyable} multiblock, see
 * {@link com.hbm.blocks.machine.rbmk.RBMKBaseBlock}), the console is a plain single block that scans
 * a flat 15x15 XZ grid of {@link RBMKBaseBlockEntity} columns around an operator-chosen target
 * position at a fixed Y, on a 10-tick cadence - ported from CE's {@code TileEntityRBMKConsole}
 * (655 lines; {@code rescan()}/{@code prepareScreenInfo()} read in full, the OpenComputers/6-screen-
 * configuration/JEI-adjacent remainder signature-surveyed only). This is data aggregation only - the
 * console owns no flux/heat/meltdown math of its own, matching the research report's framing.
 */
public class RBMKConsoleBlockEntity extends MachineBaseBlockEntity implements ITickableBE, IControlReceiver, MenuProvider {

    public static final int GRID = 15;
    public static final int FLUX_BUFFER = 60;

    public final RBMKColumn[] columns = new RBMKColumn[GRID * GRID];
    public final int[] fluxBuffer = new int[FLUX_BUFFER];

    private int targetX, targetY, targetZ;
    private byte rotation;

    public RBMKConsoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkConsole");
    }

    public void setTarget(int x, int y, int z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        setChanged();
    }

    public void rotate() {
        this.rotation = (byte) ((this.rotation + 1) % 4);
        setChanged();
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (level.getGameTime() % 10 == 0) {
            rescan();
        }

        dataChanged();
        networkPackMK2(50);
    }

    private void rescan() {
        double flux = 0;

        for (int index = 0; index < columns.length; index++) {
            int rx = xFromIndex(index);
            int rz = zFromIndex(index);

            BlockEntity te = level.getBlockEntity(new BlockPos(targetX + rx, targetY, targetZ + rz));

            if (te instanceof RBMKBaseBlockEntity rbmk) {
                columns[index] = rbmk.getConsoleData();
                if (te instanceof RBMKRodBlockEntity fuel) {
                    flux += fuel.lastFluxQuantity;
                }
            } else {
                columns[index] = null;
            }
        }

        System.arraycopy(fluxBuffer, 1, fluxBuffer, 0, fluxBuffer.length - 1);
        fluxBuffer[fluxBuffer.length - 1] = (int) flux;
    }

    public int xFromIndex(int index) {
        int half = GRID / 2;
        return switch (rotation) {
            case 1 -> half - index % GRID;
            case 2 -> half - index / GRID;
            case 3 -> index % GRID - half;
            default -> index / GRID - half;
        };
    }

    public int zFromIndex(int index) {
        int half = GRID / 2;
        return switch (rotation) {
            case 1 -> index / GRID - half;
            case 2 -> half - index % GRID;
            case 3 -> half - index / GRID;
            default -> index % GRID - half;
        };
    }

    @Override
    public boolean isUseableByPlayer(Player player) {
        return net.minecraft.world.Container.stillValidBlockEntity(this, player);
    }

    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("targetX")) {
            setTarget(data.getInt("targetX"), data.getInt("targetY"), data.getInt("targetZ"));
        }
        if (data.contains("rotate")) {
            rotate();
        }
    }

    @Override
    public void receiveControl(ServerPlayer player, CompoundTag data) {
        receiveControl(data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("targetX", targetX);
        tag.putInt("targetY", targetY);
        tag.putInt("targetZ", targetZ);
        tag.putByte("rotation", rotation);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        targetX = tag.getInt("targetX");
        targetY = tag.getInt("targetY");
        targetZ = tag.getInt("targetZ");
        rotation = tag.getByte("rotation");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(targetX);
        buf.writeInt(targetY);
        buf.writeInt(targetZ);
        buf.writeByte(rotation);

        ByteBuf plain = Unpooled.buffer();
        for (int i = 0; i < fluxBuffer.length; i++) plain.writeInt(fluxBuffer[i]);
        for (RBMKColumn column : columns) RBMKColumn.writeToBuf(plain, column);
        byte[] bytes = new byte[plain.readableBytes()];
        plain.readBytes(bytes);
        buf.writeVarInt(bytes.length);
        buf.writeBytes(bytes);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        targetX = buf.readInt();
        targetY = buf.readInt();
        targetZ = buf.readInt();
        rotation = buf.readByte();

        int len = buf.readVarInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        ByteBuf plain = Unpooled.wrappedBuffer(bytes);
        for (int i = 0; i < fluxBuffer.length; i++) fluxBuffer[i] = plain.readInt();
        for (int i = 0; i < columns.length; i++) columns[i] = RBMKColumn.readFromBuf(plain);
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RBMKConsoleMenu(containerId, playerInventory, this);
    }
}
