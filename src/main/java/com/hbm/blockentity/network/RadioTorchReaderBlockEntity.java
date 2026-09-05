package com.hbm.blockentity.network;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

/** CE {@code TileEntityRadioTorchReader}. */
public class RadioTorchReaderBlockEntity extends RadioTorchBaseBlockEntity {

    public static final int READER_SIZE = 8;
    public final String[] channels = new String[READER_SIZE];
    public final String[] names = new String[READER_SIZE];
    public final String[] prev = new String[READER_SIZE];

    public RadioTorchReaderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (int i = 0; i < READER_SIZE; i++) {
            channels[i] = "";
            names[i] = "";
            prev[i] = "";
        }
    }

    @Override
    public void updateEntity() {
        if (level != null && !level.isClientSide) {
            Direction dir = getTorchFacing().getOpposite();
            BlockEntity tile = level.getBlockEntity(worldPosition.relative(dir));
            if (tile instanceof IRORValueProvider prov) {
                for (int i = 0; i < READER_SIZE; i++) {
                    String name = names[i];
                    String ch = channels[i];
                    if (name == null || name.isEmpty() || ch == null || ch.isEmpty()) continue;
                    String value = prov.provideRORValue(IRORValueProvider.PREFIX_VALUE + name.toLowerCase(Locale.US));
                    if (value == null) value = "";
                    if (polling || !value.equals(prev[i])) {
                        RTTYSystem.broadcast(level, ch, value);
                        prev[i] = value;
                    }
                }
            }
        }
        super.updateEntity();
    }

    @Override
    public void receiveControl(CompoundTag data) {
        super.receiveControl(data);
        for (int i = 0; i < READER_SIZE; i++) {
            if (data.contains("channels" + i)) channels[i] = data.getString("channels" + i);
            if (data.contains("names" + i)) names[i] = data.getString("names" + i);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("polling", polling);
        for (int i = 0; i < READER_SIZE; i++) {
            tag.putString("channels" + i, channels[i] == null ? "" : channels[i]);
            tag.putString("names" + i, names[i] == null ? "" : names[i]);
            tag.putString("prev" + i, prev[i] == null ? "" : prev[i]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        polling = tag.getBoolean("polling") || tag.getBoolean("isPolling");
        for (int i = 0; i < READER_SIZE; i++) {
            channels[i] = tag.getString("channels" + i);
            names[i] = tag.getString("names" + i);
            prev[i] = tag.getString("prev" + i);
        }
    }
}
