package com.hbm.blockentity.network;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.network.RadioTorchBaseBlock;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.network.RadioTorchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

/** CE {@code TileEntityRadioTorchBase}. */
public class RadioTorchBaseBlockEntity extends MachineBaseBlockEntity
        implements ITickableBE, IControlReceiver, MenuProvider {

    public static final int MAPPING_SIZE = 16;

    public String channel = "";
    public int lastState = 0;
    public long lastUpdate;
    public boolean polling = false;
    public boolean customMap = false;
    public String[] mapping = new String[MAPPING_SIZE];

    public RadioTorchBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, 0);
    }

    public RadioTorchBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state, slots, false, false);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    protected Direction getTorchFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(RadioTorchBaseBlock.FACING)) {
            return state.getValue(RadioTorchBaseBlock.FACING);
        }
        return Direction.UP;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        networkPackNT(50);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 16 * 16;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("isPolling")) this.polling = data.getBoolean("isPolling");
        if (data.contains("hasMapping")) this.customMap = data.getBoolean("hasMapping");
        if (data.contains("channel")) this.channel = data.getString("channel");
        for (int i = 0; i < MAPPING_SIZE; i++) {
            if (data.contains("mapping" + i)) {
                this.mapping[i] = data.getString("mapping" + i);
            }
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("isPolling", polling);
        tag.putBoolean("hasMapping", customMap);
        tag.putInt("lastPower", lastState);
        tag.putLong("lastTime", lastUpdate);
        if (channel != null) tag.putString("channel", channel);
        for (int i = 0; i < MAPPING_SIZE; i++) {
            if (mapping[i] != null) tag.putString("mapping" + i, mapping[i]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        polling = tag.getBoolean("isPolling");
        customMap = tag.getBoolean("hasMapping");
        lastState = tag.getInt("lastPower");
        lastUpdate = tag.getLong("lastTime");
        channel = tag.getString("channel");
        for (int i = 0; i < MAPPING_SIZE; i++) {
            mapping[i] = tag.getString("mapping" + i);
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(polling);
        buf.writeBoolean(customMap);
        buf.writeInt(lastState);
        buf.writeBoolean(channel != null);
        if (channel != null) buf.writeUtf(channel);
        for (int i = 0; i < MAPPING_SIZE; i++) {
            buf.writeBoolean(mapping[i] != null);
            if (mapping[i] != null) buf.writeUtf(mapping[i]);
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        polling = buf.readBoolean();
        customMap = buf.readBoolean();
        lastState = buf.readInt();
        if (buf.readBoolean()) channel = buf.readUtf();
        for (int i = 0; i < MAPPING_SIZE; i++) {
            if (buf.readBoolean()) mapping[i] = buf.readUtf();
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RadioTorchMenu(id, inv, this);
    }
}
