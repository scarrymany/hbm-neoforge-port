package com.hbm.blockentity.machine.dummyable;

import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code TileEntityMachineCompressorCompact} extends compressor base; only {@code getConPos} + fan.
 * TODO(CE: TileEntityMachineCompressorCompact.java:18-29): client fan TESR {@code RenderCompressorCompact}.
 */
public class MachineCompressorCompactBlockEntity extends MachineCompressorBlockEntity {

    public float fanSpin;
    public float prevFanSpin;

    public MachineCompressorCompactBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (level != null && level.isClientSide) {
            this.prevFanSpin = this.fanSpin;
            if (this.isOn) {
                this.fanSpin += 45;
                if (this.fanSpin >= 360) {
                    this.prevFanSpin -= 360;
                    this.fanSpin -= 360;
                }
            }
        }
    }

    @Override
    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + rot.getStepX() * 4, y + 1, z + rot.getStepZ() * 4, rot),
                new DirPos(x - rot.getStepX() * 4, y + 1, z - rot.getStepZ() * 4, rot.getOpposite()),
                new DirPos(x + dir.getStepX() * 2 - rot.getStepX(), y + 1, z + dir.getStepZ() * 2 - rot.getStepZ(), dir),
                new DirPos(x + dir.getStepX() * 2 + rot.getStepX(), y + 1, z + dir.getStepZ() * 2 + rot.getStepZ(), dir),
                new DirPos(x - dir.getStepX() * 2 - rot.getStepX(), y + 1, z - dir.getStepZ() * 2 - rot.getStepZ(), dir.getOpposite()),
                new DirPos(x - dir.getStepX() * 2 + rot.getStepX(), y + 1, z - dir.getStepZ() * 2 + rot.getStepZ(), dir.getOpposite()),
        };
    }

    private Direction coreFacing() {
        int meta = getBlockState().getValue(BlockDummyable.META);
        return meta >= 12 ? Direction.from3DDataValue(meta - BlockDummyable.offset) : Direction.NORTH;
    }
}
