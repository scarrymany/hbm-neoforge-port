package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.entity.RadarEntry;
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

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code TileEntityMachineRadarScreen}. Fields Exact CE :21-26.
 * Radar NT slot 8 linker writes entries/ref/range/linked — CE :290-304.
 */
public class RadarScreenBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, MenuProvider {

    public final List<RadarEntry> entries = new ArrayList<>();
    public int refX;
    public int refY;
    public int refZ;
    public int range;
    public boolean linked;

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
        // CE TileEntityMachineRadarScreen.java:31-33
        dataChanged();
        networkPackMK2(100);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("linked", linked);
        tag.putInt("refX", refX);
        tag.putInt("refY", refY);
        tag.putInt("refZ", refZ);
        tag.putInt("range", range);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        linked = tag.getBoolean("linked");
        refX = tag.getInt("refX");
        refY = tag.getInt("refY");
        refZ = tag.getInt("refZ");
        range = tag.getInt("range");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        // CE TileEntityMachineRadarScreen.java:37-44
        buf.writeBoolean(linked);
        buf.writeInt(refX);
        buf.writeInt(refY);
        buf.writeInt(refZ);
        buf.writeInt(range);
        buf.writeInt(entries.size());
        for (RadarEntry entry : entries) entry.toBytes(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        linked = buf.readBoolean();
        refX = buf.readInt();
        refY = buf.readInt();
        refZ = buf.readInt();
        range = buf.readInt();
        int count = buf.readInt();
        entries.clear();
        for (int i = 0; i < count; i++) {
            RadarEntry entry = new RadarEntry();
            entry.fromBytes(buf);
            entries.add(entry);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RadarScreenMenu(id, inv, this);
    }
}
