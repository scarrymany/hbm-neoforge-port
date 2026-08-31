package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.PWRBlockEntities;
import com.hbm.blockentity.machine.PWRControllerBlockEntity;
import com.hbm.blockentity.machine.PWRProxyBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import com.mojang.serialization.MapCodec;

/**
 * Ported from CE's {@code MachinePWRController} (regname {@code pwr_controller}, read in full).
 * Per {@code docs/phase2/reactors_breeding_pwr.md}'s Key design decision, this is deliberately
 * <b>not</b> a {@link com.hbm.blocks.BlockDummyable} - CE's own class is a plain
 * {@code BlockContainerBakeable} (this port's closest equivalent: a plain {@link BaseEntityBlock}
 * with a horizontal {@code FACING} property) that performs its own bespoke recursive flood-fill
 * ({@link #assemble}/{@link #floodFill}, ported near-verbatim) and, on success, physically replaces
 * every structural block in the discovered volume with the shared {@link PWRProxyBlock} proxy - a
 * second, independent multiblock idiom from {@code MultiblockHandlerXR}/{@code BlockDummyable},
 * confirmed by that report's own "do not attempt to reuse BlockDummyable/MultiblockHandlerXR for the
 * PWR" instruction.
 *
 * <p>The six casing/core visual blocks CE recognizes by identity ({@code pwr_casing}/
 * {@code pwr_reflector}/{@code pwr_port}, {@code pwr_heatex}/{@code pwr_heatsink}/
 * {@code pwr_neutron_source}) are Phase 1 content resolved lazily by id through
 * {@link PWRPhase1Blocks} (see that class's own javadoc for why - no exported field to reference
 * directly without editing the already-committed {@code GenericBlocks}); {@code pwr_fuelrod}/
 * {@code pwr_control}/{@code pwr_channel} are this package's own new {@link BlockPillarPWR}
 * instances, referenced directly via {@link PWRBlocks}.
 *
 * <p><b>Error reporting</b>: CE's {@code sendError} pushes a floating marker particle via
 * {@code AuxParticlePacketNT}/{@code HbmEffectNT} - neither exists in this port (grepped, zero
 * hits) and no Phase 2 package has ported CE's aux-particle system. Substituted with
 * {@code player.sendSystemMessage(...)}, the same fallback this port already uses elsewhere for a
 * CE particle-toast with no ported equivalent (see e.g. {@code ItemOreDensityScanner}'s own
 * documented substitution) - same information reaches the player, different presentation.
 */
public class MachinePWRControllerBlock extends BaseEntityBlock {

    public static final MapCodec<MachinePWRControllerBlock> CODEC = simpleCodec(MachinePWRControllerBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final int MAX_SIZE = 4096;

    // Mutable per-Block-instance scratch state for the flood-fill, ported as-is from CE (CE's own
    // MachinePWRController carries these three fields + `errored` directly on the Block singleton
    // too - assembly only ever runs synchronously on the server main thread in response to one
    // player's right-click, never concurrently, so this is not a new risk introduced by the port).
    private final Map<BlockPos, BlockState> assembly = new HashMap<>();
    private final Map<BlockPos, BlockState> fuelRods = new HashMap<>();
    private final Map<BlockPos, BlockState> sources = new HashMap<>();
    private boolean errored;

    public MachinePWRControllerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PWRControllerBlockEntity(PWRBlockEntities.PWR_CONTROLLER.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == PWRBlockEntities.PWR_CONTROLLER.get() ? ITickableBE.ticker() : null;
    }

    /**
     * CE: {@code if (!player.isSneaking()) { assemble-or-open } else { pass through }} - sneaking
     * lets a wrench (or any other sneak-interacting item) reach the block underneath instead of
     * triggering assembly/GUI, matching CE's own {@code onBlockActivated} contract exactly.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        if (level.getBlockEntity(pos) instanceof PWRControllerBlockEntity controller) {
            if (!controller.assembled) {
                assemble(level, pos, state, player);
            } else {
                player.openMenu(controller, pos);
            }
        }
        return InteractionResult.CONSUME;
    }

    /** Ported from CE's {@code assemble()}. */
    public void assemble(Level level, BlockPos pos, BlockState state, Player player) {
        assembly.clear();
        fuelRods.clear();
        sources.clear();
        errored = false;

        assembly.put(pos, state);
        Direction dir = state.getValue(FACING).getOpposite();
        floodFill(level, pos.relative(dir), player);

        if (fuelRods.isEmpty()) {
            sendError(player, "Fuel rods required");
            errored = true;
        }
        if (sources.isEmpty()) {
            sendError(player, "Neutron sources required");
            errored = true;
        }

        if (level.getBlockEntity(pos) instanceof PWRControllerBlockEntity controller) {
            if (!errored) {
                for (Map.Entry<BlockPos, BlockState> entry : assembly.entrySet()) {
                    BlockPos partPos = entry.getKey();
                    BlockState originalState = entry.getValue();
                    Block block = originalState.getBlock();

                    if (block != this) {
                        boolean ioEnabled = block == PWRPhase1Blocks.port();
                        BlockState replacement = PWRBlocks.PWR_PROXY.get().defaultBlockState()
                                .setValue(PWRProxyBlock.IO_ENABLED, ioEnabled);
                        level.setBlock(partPos, replacement, 3);

                        if (level.getBlockEntity(partPos) instanceof PWRProxyBlockEntity proxy) {
                            proxy.setOriginal(originalState, pos);
                        }
                    }
                }
                controller.setup(assembly, fuelRods);
            }
            controller.assembled = !errored;
            controller.setChanged();
        }
    }

    private void floodFill(Level level, BlockPos pos, Player player) {
        if (assembly.containsKey(pos) || errored) return;
        if (assembly.size() >= MAX_SIZE) {
            sendError(player, "Max size exceeded (" + MAX_SIZE + ")");
            errored = true;
            return;
        }

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (isValidCasing(block)) {
            assembly.put(pos, state);
            return;
        }

        if (isValidCore(block)) {
            assembly.put(pos, state);
            if (block == PWRBlocks.PWR_FUELROD.get()) fuelRods.put(pos, state);
            if (block == PWRPhase1Blocks.neutronSource()) sources.put(pos, state);

            for (Direction facing : Direction.values()) {
                floodFill(level, pos.relative(facing), player);
            }
            return;
        }

        sendError(player, "Invalid block in structure: " + state.getBlock().getName().getString());
        errored = true;
    }

    private static void sendError(Player player, String message) {
        player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    private boolean isValidCore(Block block) {
        return block == PWRBlocks.PWR_FUELROD.get() || block == PWRBlocks.PWR_CONTROL.get() || block == PWRBlocks.PWR_CHANNEL.get()
                || block == PWRPhase1Blocks.heatex() || block == PWRPhase1Blocks.heatsink() || block == PWRPhase1Blocks.neutronSource();
    }

    private boolean isValidCasing(Block block) {
        return block == PWRPhase1Blocks.casing() || block == PWRPhase1Blocks.reflector() || block == PWRPhase1Blocks.port();
    }
}
