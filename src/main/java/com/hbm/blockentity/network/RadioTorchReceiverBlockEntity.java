package com.hbm.blockentity.network;

import com.hbm.blockentity.network.RTTYSystem.RTTYChannel;
import com.hbm.blocks.network.RadioTorchBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** CE {@code TileEntityRadioTorchReceiver}. */
public class RadioTorchReceiverBlockEntity extends RadioTorchBaseBlockEntity {

    public RadioTorchReceiverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        if (level != null && !level.isClientSide) {
            if ((channel != null && !channel.isEmpty()) || this.polling) {
                RTTYChannel chan = (channel == null || channel.isEmpty()) ? null : RTTYSystem.listen(level, this.channel);

                if (chan != null && (this.polling || (chan.timeStamp > this.lastUpdate - 1 && chan.timeStamp != -1))) {
                    String msg = "" + chan.signal;
                    this.lastUpdate = level.getGameTime();
                    int nextState = 0;

                    if (this.customMap) {
                        for (int i = 15; i >= 0; i--) {
                            if (msg.equals(this.mapping[i])) {
                                nextState = i;
                                break;
                            }
                        }
                    } else {
                        int sig = 0;
                        try {
                            sig = Integer.parseInt(msg);
                        } catch (Exception ignored) {
                        }
                        nextState = Mth.clamp(sig, 0, 15);
                    }

                    if (chan.timeStamp < this.lastUpdate - 1 && this.polling) {
                        nextState = 0;
                    }

                    if (this.lastState != nextState) {
                        this.lastState = nextState;
                        applyState();
                    }
                } else if (this.polling && this.lastState != 0) {
                    this.lastState = 0;
                    applyState();
                }
            }
        }
        super.updateEntity();
    }

    private void applyState() {
        if (level == null) return;
        BlockState state = getBlockState();
        boolean newLit = this.lastState > 0;
        if (state.hasProperty(RadioTorchBaseBlock.LIT) && state.getValue(RadioTorchBaseBlock.LIT) != newLit) {
            level.setBlock(worldPosition, state.setValue(RadioTorchBaseBlock.LIT, newLit), 3);
        }
        level.updateNeighborsAt(worldPosition, state.getBlock());
        setChanged();
    }
}
