package com.hbm.blockentity.network;

import com.hbm.inventory.container.network.RadioTorchCounterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/** CE {@code TileEntityRadioTorchCounter} — 3 filter slots, count matching items in adjacent inv. */
public class RadioTorchCounterBlockEntity extends RadioTorchBaseBlockEntity {

    public static final int FILTERS = 3;
    public final String[] channels = new String[FILTERS];
    public final int[] lastCount = new int[FILTERS];

    public RadioTorchCounterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, FILTERS);
        for (int i = 0; i < FILTERS; i++) channels[i] = "";
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot >= 0 && slot < FILTERS;
    }

    @Override
    public void updateEntity() {
        if (level != null && !level.isClientSide) {
            Direction dir = getTorchFacing().getOpposite();
            BlockPos adj = worldPosition.relative(dir);
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, adj, dir.getOpposite());
            if (handler != null) {
                for (int i = 0; i < FILTERS; i++) {
                    ItemStack filter = inventory.getStackInSlot(i);
                    if (filter.isEmpty() || channels[i] == null || channels[i].isEmpty()) continue;
                    int count = 0;
                    for (int s = 0; s < handler.getSlots(); s++) {
                        ItemStack have = handler.getStackInSlot(s);
                        if (!have.isEmpty() && ItemStack.isSameItemSameComponents(have, filter)) {
                            count += have.getCount();
                        }
                    }
                    if (polling || lastCount[i] != count) {
                        RTTYSystem.broadcast(level, channels[i], count);
                    }
                    lastCount[i] = count;
                }
            }
        }
        super.updateEntity();
    }

    @Override
    public void receiveControl(CompoundTag data) {
        super.receiveControl(data);
        for (int i = 0; i < FILTERS; i++) {
            if (data.contains("channel" + i)) channels[i] = data.getString("channel" + i);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("polling", polling);
        for (int i = 0; i < FILTERS; i++) {
            tag.putString("channel" + i, channels[i] == null ? "" : channels[i]);
            tag.putInt("lastCount" + i, lastCount[i]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        polling = tag.getBoolean("polling") || tag.getBoolean("isPolling");
        for (int i = 0; i < FILTERS; i++) {
            channels[i] = tag.getString("channel" + i);
            lastCount[i] = tag.getInt("lastCount" + i);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RadioTorchCounterMenu(id, inv, this);
    }
}
