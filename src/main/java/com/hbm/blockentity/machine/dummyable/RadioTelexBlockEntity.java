package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.RadioTelexMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityRadioTelex} — tx/rx channel + 5-line buffers.
 * OpenComputers / radio-bus send skipped (no telex network in this port).
 */
public class RadioTelexBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, MenuProvider {

    public String txChannel = "";
    public String rxChannel = "";
    public final String[] txBuffer = {"", "", "", "", ""};
    public final String[] rxBuffer = {"", "", "", "", ""};

    public RadioTelexBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.radioTelex");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        dataChanged();
        networkPackMK2(25);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("tx", txChannel);
        tag.putString("rx", rxChannel);
        for (int i = 0; i < 5; i++) {
            tag.putString("tx" + i, txBuffer[i]);
            tag.putString("rx" + i, rxBuffer[i]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        txChannel = tag.getString("tx");
        rxChannel = tag.getString("rx");
        for (int i = 0; i < 5; i++) {
            txBuffer[i] = tag.getString("tx" + i);
            rxBuffer[i] = tag.getString("rx" + i);
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeUtf(txChannel);
        buf.writeUtf(rxChannel);
        for (int i = 0; i < 5; i++) {
            buf.writeUtf(txBuffer[i]);
            buf.writeUtf(rxBuffer[i]);
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        txChannel = buf.readUtf();
        rxChannel = buf.readUtf();
        for (int i = 0; i < 5; i++) {
            txBuffer[i] = buf.readUtf();
            rxBuffer[i] = buf.readUtf();
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RadioTelexMenu(id, inv, this);
    }
}
