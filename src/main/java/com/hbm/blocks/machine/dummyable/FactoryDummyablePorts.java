package com.hbm.blocks.machine.dummyable;

import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared Dummyable extras + IO/cool ports for assembly/chemical factory.
 * CE {@code MachineAssemblyFactory.fillSpace}/{:49-64} == {@code MachineChemicalFactory.fillSpace}.
 * CE {@code TileEntityMachineAssemblyFactory.getConPos}/{:272-305} == chem factory {:272-305}.
 */
public final class FactoryDummyablePorts {

    private FactoryDummyablePorts() {
    }

    public static Direction coreFacing(BlockState state) {
        int meta = state.getValue(BlockDummyable.META);
        return meta >= 12 ? Direction.from3DDataValue(meta - BlockDummyable.offset) : Direction.NORTH;
    }

    /** CE fillSpace after super: perimeter extras at y, roof extras at y+2 along ±rot*2. */
    public static void fillFactoryExtras(BlockDummyable block, Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        BlockPos core = placedPos.relative(dir, placementOffset);
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (Math.abs(i) == 2 || Math.abs(j) == 2) {
                    block.makeExtra(level, core.offset(i, 0, j));
                }
            }
        }
        Direction rot = dir.getClockWise();
        for (int i = -2; i <= 2; i++) {
            block.makeExtra(level, core.offset(dir.getStepX() * i + rot.getStepX() * 2, 2, dir.getStepZ() * i + rot.getStepZ() * 2));
            block.makeExtra(level, core.offset(dir.getStepX() * i - rot.getStepX() * 2, 2, dir.getStepZ() * i - rot.getStepZ() * 2));
        }
    }

    public static DirPos[] getConPos(BlockPos pos, Direction dir) {
        Direction rot = dir.getClockWise();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return new DirPos[]{
                new DirPos(x + 3, y, z - 2, Direction.EAST),
                new DirPos(x + 3, y, z, Direction.EAST),
                new DirPos(x + 3, y, z + 2, Direction.EAST),
                new DirPos(x - 3, y, z - 2, Direction.WEST),
                new DirPos(x - 3, y, z, Direction.WEST),
                new DirPos(x - 3, y, z + 2, Direction.WEST),
                new DirPos(x - 2, y, z + 3, Direction.SOUTH),
                new DirPos(x, y, z + 3, Direction.SOUTH),
                new DirPos(x + 2, y, z + 3, Direction.SOUTH),
                new DirPos(x - 2, y, z - 3, Direction.NORTH),
                new DirPos(x, y, z - 3, Direction.NORTH),
                new DirPos(x + 2, y, z - 3, Direction.NORTH),
                new DirPos(x + dir.getStepX() * 2 + rot.getStepX() * 2, y + 3, z + dir.getStepZ() * 2 + rot.getStepZ() * 2, Direction.UP),
                new DirPos(x + dir.getStepX() + rot.getStepX() * 2, y + 3, z + dir.getStepZ() + rot.getStepZ() * 2, Direction.UP),
                new DirPos(x + rot.getStepX() * 2, y + 3, z + rot.getStepZ() * 2, Direction.UP),
                new DirPos(x - dir.getStepX() + rot.getStepX() * 2, y + 3, z - dir.getStepZ() + rot.getStepZ() * 2, Direction.UP),
                new DirPos(x - dir.getStepX() * 2 + rot.getStepX() * 2, y + 3, z - dir.getStepZ() * 2 + rot.getStepZ() * 2, Direction.UP),
                new DirPos(x + dir.getStepX() * 2 - rot.getStepX() * 2, y + 3, z + dir.getStepZ() * 2 - rot.getStepZ() * 2, Direction.UP),
                new DirPos(x + dir.getStepX() - rot.getStepX() * 2, y + 3, z + dir.getStepZ() - rot.getStepZ() * 2, Direction.UP),
                new DirPos(x - rot.getStepX() * 2, y + 3, z - rot.getStepZ() * 2, Direction.UP),
                new DirPos(x - dir.getStepX() - rot.getStepX() * 2, y + 3, z - dir.getStepZ() - rot.getStepZ() * 2, Direction.UP),
                new DirPos(x - dir.getStepX() * 2 - rot.getStepX() * 2, y + 3, z - dir.getStepZ() * 2 - rot.getStepZ() * 2, Direction.UP),
                new DirPos(x + dir.getStepX() + rot.getStepX() * 3, y, z + dir.getStepZ() + rot.getStepZ() * 3, rot),
                new DirPos(x - dir.getStepX() + rot.getStepX() * 3, y, z - dir.getStepZ() + rot.getStepZ() * 3, rot),
                new DirPos(x + dir.getStepX() - rot.getStepX() * 3, y, z + dir.getStepZ() - rot.getStepZ() * 3, rot.getOpposite()),
                new DirPos(x - dir.getStepX() - rot.getStepX() * 3, y, z - dir.getStepZ() - rot.getStepZ() * 3, rot.getOpposite()),
        };
    }

    /** CE {@code TileEntityMachineChemicalFactory.getIOPos}/{:501-510} == assem factory {:157-167}. */
    public static DirPos[] getIOPos(BlockPos pos, Direction dir) {
        Direction rot = dir.getClockWise();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return new DirPos[]{
                new DirPos(x + dir.getStepX() + rot.getStepX() * 3, y, z + dir.getStepZ() + rot.getStepZ() * 3, rot),
                new DirPos(x - dir.getStepX() + rot.getStepX() * 3, y, z - dir.getStepZ() + rot.getStepZ() * 3, rot),
                new DirPos(x + dir.getStepX() - rot.getStepX() * 3, y, z + dir.getStepZ() - rot.getStepZ() * 3, rot.getOpposite()),
                new DirPos(x - dir.getStepX() - rot.getStepX() * 3, y, z - dir.getStepZ() - rot.getStepZ() * 3, rot.getOpposite()),
        };
    }

    public static DirPos[] getCoolPos(BlockPos pos, Direction dir) {
        Direction rot = dir.getClockWise();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        return new DirPos[]{
                new DirPos(x + rot.getStepX() + dir.getStepX() * 3, y, z + rot.getStepZ() + dir.getStepZ() * 3, dir),
                new DirPos(x - rot.getStepX() + dir.getStepX() * 3, y, z - rot.getStepZ() + dir.getStepZ() * 3, dir),
                new DirPos(x + rot.getStepX() - dir.getStepX() * 3, y, z + rot.getStepZ() - dir.getStepZ() * 3, dir.getOpposite()),
                new DirPos(x - rot.getStepX() - dir.getStepX() * 3, y, z - rot.getStepZ() - dir.getStepZ() * 3, dir.getOpposite()),
        };
    }
}
