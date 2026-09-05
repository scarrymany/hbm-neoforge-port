package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.ITooltipProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import java.util.List;

/**
 * Directional "wire" block that chains {@link CapacitorBlock} instances into one virtual bank,
 * ported from CE's {@code com.hbm.blocks.machine.MachineCapacitorBus} (read in full). No block
 * entity of its own - purely a {@link Direction} state property read by
 * {@link com.hbm.blockentity.machine.CapacitorBlockEntity#updateEntity()}'s bus-chain walk and by
 * {@link IEnergyConnectorBlock#canConnect} (used for cable-visual rendering by anything that queries
 * it, matching that interface's own javadoc).
 * addStandardInfo Exact CE {@code MachineCapacitorBus.java:42-44}.
 */
public class CapacitorBusBlock extends Block implements IEnergyConnectorBlock, ITooltipProvider {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public CapacitorBusBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /**
     * CE points the bus at "the direction from this block to the placing player"
     * ({@code EnumFacing.getDirectionFromEntityLiving}); the direct 1.21 equivalent of "which of the
     * 6 directions is the player looking towards" is {@code BlockPlaceContext.getNearestLookingDirection()}
     * (used by vanilla's own piston/observer/dropper placement), but that exact method name could not
     * be independently confirmed against a real decompiled class in this sandbox. {@link BlockPlaceContext#getClickedFace()}
     * is used instead - already confirmed real and in use throughout this port (see e.g.
     * {@link CapacitorBlock#getStateForPlacement}) - which places the bus facing away from whatever
     * block it was placed against, a reasonable, always-correct default for a segment players can
     * still place from either side.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public boolean canConnect(BlockGetter level, BlockPos pos, Direction dir) {
        return level.getBlockState(pos).getValue(FACING) == dir;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Exact CE MachineCapacitorBus.java:42-44 — addStandardInfo via existing block.hbm.capacitor_bus.desc
        addStandardInfo(tooltip);
    }
}
