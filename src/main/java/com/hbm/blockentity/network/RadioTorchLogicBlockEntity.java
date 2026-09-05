package com.hbm.blockentity.network;

import com.hbm.blockentity.network.RTTYSystem.RTTYChannel;
import com.hbm.blocks.network.RadioTorchBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** CE {@code TileEntityRadioTorchLogic}. */
public class RadioTorchLogicBlockEntity extends RadioTorchBaseBlockEntity {

    public boolean descending = false;
    public int[] conditions = new int[MAPPING_SIZE];

    public RadioTorchLogicBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        for (int i = 0; i < MAPPING_SIZE; i++) {
            if (mapping[i] == null) mapping[i] = "";
        }
    }

    @Override
    public void updateEntity() {
        if (level != null && !level.isClientSide) {
            if ((channel != null && !channel.isEmpty()) || polling) {
                RTTYChannel chan = (channel == null || channel.isEmpty()) ? null : RTTYSystem.listen(level, channel);
                if (chan != null && (polling || (chan.timeStamp > lastUpdate - 1 && chan.timeStamp != -1))) {
                    String msg = "" + chan.signal;
                    lastUpdate = level.getGameTime();
                    int nextState = 0;
                    if (descending) {
                        for (int i = 15; i >= 0; i--) {
                            if (parseSignal(msg, i)) {
                                nextState = i;
                                break;
                            }
                        }
                    } else {
                        for (int i = 0; i < 16; i++) {
                            if (parseSignal(msg, i)) {
                                nextState = i;
                                break;
                            }
                        }
                    }
                    if (lastState != nextState) {
                        lastState = nextState;
                        applyState();
                    }
                } else if (polling && lastState != 0) {
                    lastState = 0;
                    applyState();
                }
            }
        }
        super.updateEntity();
    }

    public boolean parseSignal(String signal, int index) {
        if (conditions[index] <= 5) {
            long sig;
            long map;
            try {
                sig = Long.parseLong(signal);
                map = Long.parseLong(mapping[index] == null ? "" : mapping[index]);
            } catch (Exception x) {
                return false;
            }
            return switch (conditions[index]) {
                case 1 -> sig <= map;
                case 2 -> sig >= map;
                case 3 -> sig > map;
                case 4 -> sig == map;
                case 5 -> sig != map;
                default -> sig < map;
            };
        }
        String map = mapping[index] == null ? "" : mapping[index];
        return switch (conditions[index]) {
            case 7 -> !signal.equals(map);
            case 8 -> signal.contains(map);
            case 9 -> !signal.contains(map);
            default -> signal.equals(map);
        };
    }

    private void applyState() {
        if (level == null) return;
        BlockState state = getBlockState();
        boolean newLit = lastState > 0;
        if (state.hasProperty(RadioTorchBaseBlock.LIT) && state.getValue(RadioTorchBaseBlock.LIT) != newLit) {
            level.setBlock(worldPosition, state.setValue(RadioTorchBaseBlock.LIT, newLit), 3);
        }
        level.updateNeighborsAt(worldPosition, state.getBlock());
        setChanged();
    }

    @Override
    public void receiveControl(CompoundTag data) {
        super.receiveControl(data);
        if (data.contains("descending")) descending = data.getBoolean("descending");
        for (int i = 0; i < MAPPING_SIZE; i++) {
            if (data.contains("conditions" + i)) conditions[i] = data.getInt("conditions" + i);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("descending", descending);
        for (int i = 0; i < MAPPING_SIZE; i++) {
            tag.putInt("conditions" + i, conditions[i]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        descending = tag.getBoolean("descending");
        for (int i = 0; i < MAPPING_SIZE; i++) {
            conditions[i] = tag.getInt("conditions" + i);
        }
    }
}
