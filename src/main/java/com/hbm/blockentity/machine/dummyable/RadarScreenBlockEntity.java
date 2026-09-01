package com.hbm.blockentity.machine.dummyable;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.dummyable.RadarScreenMenu;
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
 * CE {@code TileEntityMachineRadarScreen} — stores linked radar pos.
 * Radar-NT scan overlay skipped until {@code machine_radar} NT is live.
 */
public class RadarScreenBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, MenuProvider {

    public BlockPos linked = BlockPos.ZERO;
    public boolean linkedValid;

    public RadarScreenBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.radarScreen");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        linkedValid = !linked.equals(BlockPos.ZERO) && level.isLoaded(linked);
        dataChanged();
        networkPackMK2(25);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("link", linked.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        linked = BlockPos.of(tag.getLong("link"));
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(linked.asLong());
        buf.writeBoolean(linkedValid);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        linked = BlockPos.of(buf.readLong());
        linkedValid = buf.readBoolean();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RadarScreenMenu(id, inv, this);
    }
}
