package com.hbm.blocks.generic;

import com.hbm.blocks.ITooltipProvider;
import com.hbm.blocks.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Stackable metal grate/catwalk floor, ported from CE's {@code BlockGrate}. The HEIGHT property
 * (0-7 stacked layers, 8/9 flush-to-ceiling variants) is a real placement-time state, not a
 * metadata content variant, so it survives as an {@link IntegerProperty} rather than being
 * flattened into separate registry entries - matching how {@link BlockRedBrick} kept its own
 * placement-derived {@code FACING} property. {@code ModBlocks.STEEL_GRATE_WIDE}'s item/XP-orb
 * "sink through" behavior is preserved via a direct sibling-field reference, exactly as CE itself
 * did with {@code ModBlocks.steel_grate_wide}.
 */
public class BlockGrate extends Block implements ITooltipProvider {

    public static final IntegerProperty HEIGHT = IntegerProperty.create("height", 0, 9);

    public BlockGrate(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HEIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HEIGHT);
    }

    private static double yOf(int height) {
        return height == 9 ? -0.125D : height * 0.125D;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int height = state.getValue(HEIGHT);
        double y = yOf(height);
        double topShrink = this == ModBlocks.STEEL_GRATE_WIDE.get() ? 0.001D : 0.0D;
        return Block.box(0, y * 16, 0, 16, (y + 0.125D - topShrink) * 16, 16);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face == Direction.DOWN) {
            return this.defaultBlockState().setValue(HEIGHT, 7);
        }
        if (face == Direction.UP) {
            return this.defaultBlockState().setValue(HEIGHT, 0);
        }
        double fractionalY = context.getClickLocation().y - context.getClickedPos().getY();
        return this.defaultBlockState().setValue(HEIGHT, (int) Math.floor(fractionalY * 8D) & 7);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide()) {
            return;
        }

        int height = state.getValue(HEIGHT);
        boolean breakIt = false;

        if (height == 9) {
            breakIt = !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        } else if (height == 8) {
            breakIt = !level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN);
        }

        if (breakIt) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (this == ModBlocks.STEEL_GRATE_WIDE.get() && (entity instanceof ItemEntity || entity instanceof ExperienceOrb)) {
            int height = state.getValue(HEIGHT);
            if (entity.getY() < pos.getY() + yOf(height) + 0.375D) {
                entity.setDeltaMovement(0, -0.25, 0);
                entity.setPos(entity.getX(), entity.getY() - 0.125D, entity.getZ());
            }
        }
        super.entityInside(state, level, pos, entity);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (this == ModBlocks.STEEL_GRATE_WIDE.get()) {
            tooltip.add(Component.translatable(this.getDescriptionId() + ".desc"));
        }
    }
}
