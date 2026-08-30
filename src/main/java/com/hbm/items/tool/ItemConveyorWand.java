package com.hbm.items.tool;

import com.hbm.blocks.network.BlockConveyorBase;
import com.hbm.blocks.network.ConveyorBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.function.Supplier;

/**
 * Conveyor-belt placement wand, ported (in reduced scope) from CE's
 * {@code com.hbm.items.tool.ItemConveyorWand} (read in full - a ~330-line class). One instance per
 * conveyor speed tier (CE's single item with 4 {@code ConveyorType} metadata subtypes is flattened
 * into 4 separate registered items, matching this port's post-flattening convention - see
 * {@code CouplingToolItems} for the 4 registrations), each hardcoded to its own
 * {@link ConveyorBlocks} family entry.
 * <p>
 * <b>Scope reduction from CE, documented rather than silent</b>: CE's real item does two things -
 * (1) sneak-right-click places a single conveyor facing the player, and (2) a non-sneak click-drag
 * (mark a start face, then click an end face) auto-builds an entire snaking conveyor path between the
 * two points, complete with a client-side {@code WorldInAJar} ghost-block preview and turn/incline
 * pathfinding. This class ports only (1) - the single-placement behavior, which is CE's own fallback
 * for the common "just place one belt" case. The (2) drag-path constructor is a large, genuinely
 * separate pathfinding+client-preview subsystem ({@code WorldInAJar} does not exist anywhere in this
 * port) that belongs to a dedicated rendering/UX pass, not this machine-coupling item pass - every
 * concrete {@link ConveyorBlocks} target this item places is already real, shipped content per
 * {@code docs/phase2/blocks_network_conveyor_crane.md}.
 */
public class ItemConveyorWand extends Item {

    private final Supplier<? extends Block> conveyorBlock;

    public ItemConveyorWand(Properties properties, Supplier<? extends Block> conveyorBlock) {
        super(properties);
        this.conveyorBlock = conveyorBlock;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;

        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        Direction facing = context.getClickedFace();
        BlockPos placePos = context.getClickedPos().relative(facing);

        if (!level.getBlockState(placePos).canBeReplaced()) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockState defaultState = conveyorBlock.get().defaultBlockState();
        BlockState placeState = defaultState.hasProperty(BlockConveyorBase.FACING)
                ? defaultState.setValue(BlockConveyorBase.FACING, player.getDirection())
                : defaultState;

        level.setBlock(placePos, placeState, 11);

        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Shift right-click a face to place a conveyor belt."));
    }
}
