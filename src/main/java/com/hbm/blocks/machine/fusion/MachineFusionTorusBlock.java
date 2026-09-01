package com.hbm.blocks.machine.fusion;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.fusion.FusionBlockEntities;
import com.hbm.blockentity.machine.fusion.FusionTorusBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ITooltipProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * CE {@code MachineFusionTorus} Dummyable {4,0,7,7,7,7} offset 7. Layout 1:1.
 * TODO(CE: MachineFusionTorus.java:87-88): AE2 TileEntityFusionTorusAE2 / ProxyCombo META≥6.
 */
public class MachineFusionTorusBlock extends BlockDummyable implements ITooltipProvider {

    public static final int[][][] LAYOUT = new int[][][]{
            new int[][]{
                    new int[]{0, 0, 0, 0, 3, 3, 3, 3, 3, 3, 3, 0, 0, 0, 0},
                    new int[]{0, 0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0, 0},
                    new int[]{0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0},
                    new int[]{0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0},
                    new int[]{3, 1, 1, 1, 1, 3, 3, 3, 3, 3, 1, 1, 1, 1, 3},
                    new int[]{3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
                    new int[]{3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
                    new int[]{3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
                    new int[]{3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
                    new int[]{3, 1, 1, 1, 3, 3, 3, 3, 3, 3, 3, 1, 1, 1, 3},
                    new int[]{3, 1, 1, 1, 1, 3, 3, 3, 3, 3, 1, 1, 1, 1, 3},
                    new int[]{0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0},
                    new int[]{0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0},
                    new int[]{0, 0, 0, 3, 1, 1, 1, 1, 1, 1, 1, 3, 0, 0, 0},
                    new int[]{0, 0, 0, 0, 3, 3, 3, 3, 3, 3, 3, 0, 0, 0, 0},
            },
            new int[][]{
                    new int[]{0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0},
                    new int[]{0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
                    new int[]{0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 2, 1, 1, 0, 0},
                    new int[]{0, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 0},
                    new int[]{1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1},
                    new int[]{1, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 1},
                    new int[]{3, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 3},
                    new int[]{3, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 3},
                    new int[]{3, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 3},
                    new int[]{1, 1, 2, 1, 1, 3, 3, 3, 3, 3, 1, 1, 2, 1, 1},
                    new int[]{1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1},
                    new int[]{0, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 0},
                    new int[]{0, 0, 1, 1, 2, 2, 2, 2, 2, 2, 2, 1, 1, 0, 0},
                    new int[]{0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0},
                    new int[]{0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0},
            },
            new int[][]{
                    new int[]{0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0},
                    new int[]{0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0},
                    new int[]{0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0},
                    new int[]{0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0},
                    new int[]{1, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 1},
                    new int[]{1, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 1},
                    new int[]{3, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 3},
                    new int[]{3, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 3},
                    new int[]{3, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 3},
                    new int[]{1, 2, 2, 2, 1, 3, 3, 3, 3, 3, 1, 2, 2, 2, 1},
                    new int[]{1, 2, 2, 2, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 1},
                    new int[]{0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0},
                    new int[]{0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0},
                    new int[]{0, 0, 0, 1, 2, 2, 2, 2, 2, 2, 2, 1, 0, 0, 0},
                    new int[]{0, 0, 0, 0, 1, 1, 3, 3, 3, 1, 1, 0, 0, 0, 0},
            }
    };

    public MachineFusionTorusBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{4, 0, 7, 7, 7, 7};
    }

    @Override
    public int getOffset() {
        return 7;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new FusionTorusBlockEntity(FusionBlockEntities.FUSION_TORUS.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == FusionBlockEntities.FUSION_TORUS.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return standardOpenBehavior(level, pos, player);
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        if (!super.checkRequirement(level, placedPos, dir, placementOffset)) return false;
        BlockPos core = placedPos.relative(dir, placementOffset);
        for (int iy = 0; iy < 5; iy++) {
            int l = iy > 2 ? 4 - iy : iy;
            int[][] layer = LAYOUT[l];
            for (int ix = 0; ix < layer.length; ix++) {
                for (int iz = 0; iz < layer.length; iz++) {
                    if (LAYOUT[l][ix][iz] <= 0) continue;
                    int ex = ix - layer.length / 2;
                    int ez = iz - layer.length / 2;
                    BlockPos check = core.offset(ex, iy, ez);
                    if (!level.getBlockState(check).canBeReplaced()) return false;
                }
            }
        }
        return true;
    }

    public void formFromStruct(Level level, BlockPos core) {
        fillSpace(level, core, Direction.NORTH, 0);
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        BlockPos core = placedPos.relative(dir, placementOffset);
        int x = core.getX();
        int y = core.getY();
        int z = core.getZ();

        for (int iy = 0; iy < 5; iy++) {
            int l = iy > 2 ? 4 - iy : iy;
            int[][] layer = LAYOUT[l];
            for (int ix = 0; ix < layer.length; ix++) {
                for (int iz = 0; iz < layer[0].length; iz++) {
                    int ex = ix - layer.length / 2;
                    int ez = iz - layer.length / 2;
                    int meta;
                    if (iy > 0) {
                        meta = Direction.UP.get3DDataValue();
                    } else if (ex < 0) {
                        meta = Direction.WEST.get3DDataValue();
                    } else if (ex > 0) {
                        meta = Direction.EAST.get3DDataValue();
                    } else if (ez < 0) {
                        meta = Direction.NORTH.get3DDataValue();
                    } else if (ez > 0) {
                        meta = Direction.SOUTH.get3DDataValue();
                    } else {
                        continue;
                    }
                    if (LAYOUT[l][ix][iz] > 0) {
                        level.setBlock(new BlockPos(x + ex, y + iy, z + ez), defaultBlockState().setValue(META, meta), 3);
                    }
                }
            }
        }

        makeExtra(level, new BlockPos(x, y + 4, z));
        makeExtra(level, new BlockPos(x + 6, y, z));
        makeExtra(level, new BlockPos(x + 6, y + 4, z));
        makeExtra(level, new BlockPos(x + 6, y, z + 2));
        makeExtra(level, new BlockPos(x + 6, y + 4, z + 2));
        makeExtra(level, new BlockPos(x + 6, y, z - 2));
        makeExtra(level, new BlockPos(x + 6, y + 4, z - 2));
        makeExtra(level, new BlockPos(x - 6, y, z));
        makeExtra(level, new BlockPos(x - 6, y + 4, z));
        makeExtra(level, new BlockPos(x - 6, y, z + 2));
        makeExtra(level, new BlockPos(x - 6, y + 4, z + 2));
        makeExtra(level, new BlockPos(x - 6, y, z - 2));
        makeExtra(level, new BlockPos(x - 6, y + 4, z - 2));
        makeExtra(level, new BlockPos(x, y, z + 6));
        makeExtra(level, new BlockPos(x, y + 4, z + 6));
        makeExtra(level, new BlockPos(x + 2, y, z + 6));
        makeExtra(level, new BlockPos(x + 2, y + 4, z + 6));
        makeExtra(level, new BlockPos(x - 2, y, z + 6));
        makeExtra(level, new BlockPos(x - 2, y + 4, z + 6));
        makeExtra(level, new BlockPos(x, y, z - 6));
        makeExtra(level, new BlockPos(x, y + 4, z - 6));
        makeExtra(level, new BlockPos(x + 2, y, z - 6));
        makeExtra(level, new BlockPos(x + 2, y + 4, z - 6));
        makeExtra(level, new BlockPos(x - 2, y, z - 6));
        makeExtra(level, new BlockPos(x - 2, y + 4, z - 6));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addStandardInfo(tooltip);
    }
}
