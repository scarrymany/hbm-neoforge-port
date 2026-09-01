package com.hbm.blockentity.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** CE {@code TileEntityKeypadBase} used by {@code keypad_test}. */
public class KeypadTestBlockEntity extends LoadedBaseBlockEntity
        implements ITickableBE, IKeypadHandler, MenuProvider {

    public final Keypad keypad = new Keypad(this);

    public KeypadTestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
