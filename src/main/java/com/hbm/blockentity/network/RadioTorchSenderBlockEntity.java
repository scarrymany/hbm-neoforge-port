package com.hbm.blockentity.network;

import com.hbm.blocks.network.RadioTorchBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** CE {@code TileEntityRadioTorchSender}. */
public class RadioTorchSenderBlockEntity extends RadioTorchBaseBlockEntity {

    public RadioTorchSenderBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        if (level != null && !level.isClientSide) {
            Direction facing = getTorchFacing();
            BlockPos inputPos = worldPosition.relative(facing.getOpposite());
            BlockState inputState = level.getBlockState(inputPos);

            int input = level.getSignal(inputPos, facing);
            if (inputState.hasAnalogOutputSignal()) {
                input = inputState.getAnalogOutputSignal(level, inputPos);
            }
            input = Mth.clamp(input, 0, 15);

            boolean shouldSend = this.polling;
            if (input != this.lastState) {
                this.lastState = input;
                BlockState state = getBlockState();
                boolean newLit = this.lastState > 0;
                if (state.hasProperty(RadioTorchBaseBlock.LIT) && state.getValue(RadioTorchBaseBlock.LIT) != newLit) {
                    level.setBlock(worldPosition, state.setValue(RadioTorchBaseBlock.LIT, newLit), 3);
                }
                setChanged();
                shouldSend = true;
            }

            if (shouldSend && channel != null && !channel.isEmpty()) {
                String toSend = this.customMap ? this.mapping[input] : (input + "");
                if (toSend != null && !toSend.isEmpty()) {
                    RTTYSystem.broadcast(level, this.channel, toSend);
                }
            }
        }
        super.updateEntity();
    }
}
