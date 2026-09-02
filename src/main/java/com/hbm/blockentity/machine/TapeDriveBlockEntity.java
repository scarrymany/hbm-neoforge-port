package com.hbm.blockentity.machine;

import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blockentity.BlockEntityProxyBase;
import com.hbm.inventory.container.TapeDriveMenu;
import com.hbm.items.ModItems;
import com.hbm.items.machine.DriveItem;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import org.jetbrains.annotations.NotNull;

/**
 * Port of CE {@code com.hbm.tileentity.machine.TileEntityMachineTapeDrive} - a satellite data tape drive
 * that writes satellite scan outputs to empty drive items.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityMachineTapeDrive.java:29-138
 */
public class TapeDriveBlockEntity extends MachineBaseBlockEntity implements MenuProvider {

    public byte[] tapes = new byte[12];
    public static final byte SLOT_EMPTY = 0;
    public static final byte SLOT_ANY = 1;
    public static final byte SLOT_EMPTY_TAPE = 2;
    public static final byte SLOT_FILLED_TAPE = 3;

    public TapeDriveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 12, true, false);
    }

    public TapeDriveBlockEntity(BlockPos pos, BlockState state) {
        this(com.hbm.blocks.ModBlocks.TAPE_DRIVE_ENTITY.get(), pos, state);
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.machineTapeDrive");
    }

    @Override
    public void tickCommon() {
        if (level == null || level.isClientSide) return;

        if (level.getGameTime() % 10 == 0) {
            Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING).getOpposite();
            BlockPos targetPos = worldPosition.relative(facing);
            BlockEntity connected = level.getBlockEntity(targetPos);

            if (connected instanceof BlockEntityProxyBase proxy) {
                connected = proxy.getTarget();
            }

            if (connected instanceof SatLinkBlockEntity link) {
                if (link.connected) {
                    SatelliteSavedData dat = SatelliteSavedData.getData(level);
                    Satellite satellite = dat.sats.get(link.freq);

                    if (satellite != null && satellite.hasData(level)) {
                        for (int i = 0; i < 12; i++) {
                            ItemStack stack = inventory.getStackInSlot(i);
                            if (stack.isEmpty() || stack.getItem() != ModItems.DRIVE.get()) continue;

                            DriveItem.EnumDriveType type = DriveItem.getTypeFromStack(stack);
                            DriveItem.EnumDriveType ret = satellite.getOutputData(type);

                            if (ret != null) {
                                satellite.consumeData();
                                inventory.setStackInSlot(i, DriveItem.createStack(ret, 1));
                                dat.setDirty();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isItemValidForSlot(int slot, @NotNull ItemStack stack) {
        return stack.getItem() == ModItems.DRIVE.get();
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return true;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        int[] slots = new int[12];
        for (int i = 0; i < 12; i++) {
            slots[i] = i;
        }
        return slots;
    }

    @Override
    public void serialize(ByteBuf buf) {
        super.serialize(buf);

        for (int i = 0; i < 12; i++) {
            byte type = SLOT_EMPTY;
            ItemStack stack = inventory.getStackInSlot(i);

            if (!stack.isEmpty()) {
                type = SLOT_ANY;

                if (stack.getItem() == ModItems.DRIVE.get()) {
                    DriveItem.EnumDriveType driveType = DriveItem.getTypeFromStack(stack);
                    if (driveType == DriveItem.EnumDriveType.DISK_EMPTY || driveType == DriveItem.EnumDriveType.FLASH_EMPTY) {
                        type = SLOT_EMPTY_TAPE;
                    } else if (driveType == DriveItem.EnumDriveType.DISK_BROKEN || driveType == DriveItem.EnumDriveType.FLASH_BROKEN) {
                        type = SLOT_ANY;
                    } else {
                        type = SLOT_FILLED_TAPE;
                    }
                }
            }

            buf.writeByte(type);
        }
    }

    @Override
    public void deserialize(ByteBuf buf) {
        super.deserialize(buf);

        for (int i = 0; i < 12; i++) {
            this.tapes[i] = buf.readByte();
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int windowId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new TapeDriveMenu(windowId, playerInventory, this);
    }
}
