package com.hbm.blockentity.machine.pile;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.machine.pile.BlockPileDevice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityPileDeviceBase}.
 */
public abstract class PileDeviceBaseBlockEntity extends LoadedBaseBlockEntity implements ITickableBE {

    public int chanNum;

    public PileDeviceBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Direction getOrientation() {
        return Direction.from3DDataValue(getBlockState().getValue(BlockPileDevice.META) % 4 + 2);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(this.chanNum);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.chanNum = buf.readInt();
    }
}
