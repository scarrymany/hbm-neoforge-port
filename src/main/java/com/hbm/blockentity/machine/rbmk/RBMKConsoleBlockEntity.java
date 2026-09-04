package com.hbm.blockentity.machine.rbmk;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKControlManualBlockEntity.RBMKColor;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.machine.rbmk.RBMKConsoleMenu;
import com.hbm.util.EnumUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Console. Exact CE {@code TileEntityRBMKConsole.java:76-100}/{:228-303}/{:361-391}: 15×15 rescan,
 * {@code getX/ZFromIndex} rotation, {@code receiveControl} rod level / color / compressor / screens.
 * TESR / 6-screen display blit stay skipped. Crane console stays unregistered.
 */
public class RBMKConsoleBlockEntity extends MachineBaseBlockEntity implements ITickableBE, IControlReceiver, MenuProvider {

    public static final int GRID = 15;
    public static final int FLUX_BUFFER = 60;

    public final RBMKColumn[] columns = new RBMKColumn[GRID * GRID];
    public final int[] fluxBuffer = new int[FLUX_BUFFER];
    public final RBMKScreen[] screens = new RBMKScreen[6];

    private int targetX, targetY, targetZ;
    private byte rotation;

    public RBMKConsoleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0, false, false);
        for (int i = 0; i < screens.length; i++) screens[i] = new RBMKScreen();
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
            int rx = getXFromIndex(index);
            int rz = getZFromIndex(index);

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

    /** CE {@code :361-375}. */
    public int getXFromIndex(int col) {
        int i = col % 15 - 7;
        int j = col / 15 - 7;
        return switch (rotation) {
            case 1 -> -j;
            case 2 -> -i;
            case 3 -> j;
            default -> i;
        };
    }

    /** CE {@code :377-391}. */
    public int getZFromIndex(int col) {
        int i = col % 15 - 7;
        int j = col / 15 - 7;
        return switch (rotation) {
            case 1 -> i;
            case 2 -> -j;
            case 3 -> -i;
            default -> j;
        };
    }

    /** @deprecated use {@link #getXFromIndex(int)} */
    public int xFromIndex(int index) {
        return getXFromIndex(index);
    }

    /** @deprecated use {@link #getZFromIndex(int)} */
    public int zFromIndex(int index) {
        return getZFromIndex(index);
    }

    @Override
    public boolean isUseableByPlayer(Player player) {
        return net.minecraft.world.Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean hasPermission(Player player) {
        // CE :228-229 — length from integer pos < 20
        double dx = worldPosition.getX() - player.getX();
        double dy = worldPosition.getY() - player.getY();
        double dz = worldPosition.getZ() - player.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz) < 20;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (level == null) return;

        if (data.contains("targetX")) {
            setTarget(data.getInt("targetX"), data.getInt("targetY"), data.getInt("targetZ"));
        }
        if (data.contains("rotate")) {
            rotate();
        }

        // CE :234-250
        if (data.contains("level")) {
            for (String key : data.getAllKeys()) {
                if (key.startsWith("sel_")) {
                    int index = data.getInt(key);
                    int x = getXFromIndex(index);
                    int z = getZFromIndex(index);
                    BlockEntity te = level.getBlockEntity(new BlockPos(targetX + x, targetY, targetZ + z));
                    if (te instanceof RBMKControlManualBlockEntity rod) {
                        rod.startingLevel = rod.extraction;
                        rod.setTarget(Mth.clamp(data.getDouble("level"), 0, 1));
                        te.setChanged();
                    }
                }
            }
        }

        // CE :252-257
        if (data.contains("toggle")) {
            int slot = data.getByte("toggle");
            if (slot >= 0 && slot < screens.length) {
                int next = this.screens[slot].type.ordinal() + 1;
                this.screens[slot].type = ScreenType.VALUES[next % ScreenType.VALUES.length];
                setChanged();
            }
        }

        // CE :259-271
        if (data.contains("id")) {
            int slot = data.getByte("id");
            if (slot >= 0 && slot < screens.length) {
                List<Integer> list = new ArrayList<>();
                for (int i = 0; i < GRID * GRID; i++) {
                    if (data.getBoolean("s" + i)) list.add(i);
                }
                this.screens[slot].columns = list.toArray(new Integer[0]);
                setChanged();
            }
        }

        // CE :273-288
        if (data.contains("assignColor")) {
            int color = data.getByte("assignColor");
            int[] cols = data.getIntArray("cols");
            for (int i : cols) {
                int x = getXFromIndex(i);
                int z = getZFromIndex(i);
                BlockEntity te = level.getBlockEntity(new BlockPos(targetX + x, targetY, targetZ + z));
                if (te instanceof RBMKControlManualBlockEntity rod) {
                    rod.color = EnumUtil.grabEnumSafely(RBMKColor.VALUES, color);
                    te.setChanged();
                }
            }
        }

        // CE :290-303
        if (data.contains("compressor")) {
            int[] cols = data.getIntArray("cols");
            for (int i : cols) {
                int x = getXFromIndex(i);
                int z = getZFromIndex(i);
                BlockEntity te = level.getBlockEntity(new BlockPos(targetX + x, targetY, targetZ + z));
                if (te instanceof RBMKBoilerBlockEntity boiler) {
                    boiler.cyceCompressor();
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("targetX", targetX);
        tag.putInt("targetY", targetY);
        tag.putInt("targetZ", targetZ);
        tag.putByte("rotation", rotation);
        for (int i = 0; i < screens.length; i++) {
            tag.putByte("t" + i, (byte) screens[i].type.ordinal());
            int[] cols = new int[screens[i].columns.length];
            for (int c = 0; c < cols.length; c++) cols[c] = screens[i].columns[c];
            tag.putIntArray("s" + i, cols);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        targetX = tag.getInt("targetX");
        targetY = tag.getInt("targetY");
        targetZ = tag.getInt("targetZ");
        rotation = tag.getByte("rotation");
        for (int i = 0; i < screens.length; i++) {
            if (tag.contains("t" + i)) {
                screens[i].type = ScreenType.VALUES[tag.getByte("t" + i) % ScreenType.VALUES.length];
            }
            if (tag.contains("s" + i)) {
                int[] raw = tag.getIntArray("s" + i);
                Integer[] boxed = new Integer[raw.length];
                for (int c = 0; c < raw.length; c++) boxed[c] = raw[c];
                screens[i].columns = boxed;
            }
        }
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

    /** CE {@code TileEntityRBMKConsole.ScreenType}. */
    public enum ScreenType {
        NONE(0), COL_TEMP(18), ROD_EXTRACTION(36), FUEL_DEPLETION(54), FUEL_POISON(72), FUEL_TEMP(90);

        public static final ScreenType[] VALUES = values();
        public final int offset;

        ScreenType(int offset) {
            this.offset = offset;
        }
    }

    /** CE {@code TileEntityRBMKConsole.RBMKScreen}. */
    public static class RBMKScreen {
        public ScreenType type = ScreenType.NONE;
        public Integer[] columns = new Integer[0];
        public String display = null;
    }
}
