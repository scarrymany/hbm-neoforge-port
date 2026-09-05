package com.hbm.blockentity.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.IKeypadHandler;
import com.hbm.inventory.container.machine.KeypadMenu;
import com.hbm.util.Keypad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** CE {@code TileEntitySlidingBlastDoorKeypad}. */
public class SlidingBlastDoorKeypadBlockEntity extends LoadedBaseBlockEntity
        implements ITickableBE, IKeypadHandler, MenuProvider {

    public final Keypad keypad = new Keypad(this);

    public SlidingBlastDoorKeypadBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        keypad.update();
    }

    @Override
    public Keypad getKeypad() {
        return keypad;
    }

    @Override
    public void keypadActivated() {
        if (level == null || !(getBlockState().getBlock() instanceof BlockDummyable dummyable)) return;
        var core = dummyable.findCoreBlockEntity(level, worldPosition);
        if (core instanceof SlidingBlastDoorBlockEntity door) door.toggle();
    }

    @Override
    public void passwordSet() {
        if (level == null || !(getBlockState().getBlock() instanceof BlockDummyable dummyable)) return;
        BlockPos corePos = dummyable.findCore(level, worldPosition);
        if (corePos == null) return;
        BlockEntity core = level.getBlockEntity(corePos);
        if (core instanceof SlidingBlastDoorBlockEntity door) {
            door.keypadLocked = true;
            door.setChanged();
            BlockPos offset = worldPosition.subtract(corePos);
            BlockPos otherPad = corePos.offset(-offset.getX(), offset.getY(), -offset.getZ());
            if (level.getBlockEntity(otherPad) instanceof IKeypadHandler other) {
                Keypad pad = other.getKeypad();
                pad.clearCode();
                pad.isSettingCode = false;
                pad.storedCode = this.keypad.storedCode;
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        keypad.writeToNbt(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        keypad.readFromNbt(tag);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.ntmKeypad");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new KeypadMenu(id, inv, this);
    }
}
