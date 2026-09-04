package com.hbm.blocks.machine.fusion;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.fusion.FusionBlockEntities;
import com.hbm.blockentity.machine.fusion.IcfBlockEntity;
import com.hbm.blockentity.machine.fusion.IcfControllerBlockEntity;
import com.hbm.blocks.ILookOverlay;
import com.hbm.util.BobMathUtil;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * CE {@code MachineICFController} — RMB assemble floodFill Exact CE {@code :96-204}.
 * On success every scanned part is replaced with {@link IcfBlock} (ports keep {@code io=true}).
 * CE {@code :115-131} / {@code BlockICF.java}.
 */
public class IcfControllerBlock extends BaseEntityBlock implements ILookOverlay {

    public static final MapCodec<IcfControllerBlock> CODEC = simpleCodec(IcfControllerBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final int MAX_SIZE = 1024;

    public IcfControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IcfControllerBlockEntity(FusionBlockEntities.ICF_CONTROLLER.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == FusionBlockEntities.ICF_CONTROLLER.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // CE MachineICFController.java:81-93
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.getBlockEntity(pos) instanceof IcfControllerBlockEntity controller && !controller.assembled) {
            assemble(level, pos);
        }
        return InteractionResult.CONSUME;
    }

    public void assemble(Level world, BlockPos pos) {
        // CE MachineICFController.java:96-144
        HashMap<BlockPos, BlockState> assembly = new HashMap<>();
        HashSet<BlockPos> casings = new HashSet<>();
        HashSet<BlockPos> ports = new HashSet<>();
        HashSet<BlockPos> cells = new HashSet<>();
        HashSet<BlockPos> emitters = new HashSet<>();
        HashSet<BlockPos> capacitors = new HashSet<>();
        HashSet<BlockPos> turbochargers = new HashSet<>();
        boolean[] errored = {false};

        assembly.put(pos, world.getBlockState(pos));
        Direction dir = world.getBlockState(pos).getValue(FACING);
        floodFill(world, pos.relative(dir.getOpposite()), assembly, casings, ports, cells, emitters, capacitors, turbochargers, errored);
        assembly.remove(pos);

        if (world.getBlockEntity(pos) instanceof IcfControllerBlockEntity controller) {
            if (!errored[0]) {
                // Exact CE MachineICFController.java:115-131 — replace scanned parts with BlockICF.
                for (Map.Entry<BlockPos, BlockState> entry : assembly.entrySet()) {
                    BlockPos partPos = entry.getKey();
                    boolean isPort = ports.contains(partPos);
                    BlockState placeholder = FusionBlocks.ICF_BLOCK.get().defaultBlockState()
                            .setValue(IcfBlock.IO_ENABLED, isPort);
                    world.setBlock(partPos, placeholder, 3);
                    if (world.getBlockEntity(partPos) instanceof IcfBlockEntity icf) {
                        icf.setOriginal(entry.getValue(), pos);
                    }
                }
                controller.setup(ports, cells, emitters, capacitors, turbochargers);
            }
            controller.assembled = !errored[0];
            controller.setChanged();
        }
    }

    private void floodFill(Level world, BlockPos pos, HashMap<BlockPos, BlockState> assembly,
                           HashSet<BlockPos> casings, HashSet<BlockPos> ports, HashSet<BlockPos> cells,
                           HashSet<BlockPos> emitters, HashSet<BlockPos> capacitors, HashSet<BlockPos> turbochargers,
                           boolean[] errored) {
        // CE MachineICFController.java:147-204
        if (assembly.containsKey(pos)) return;
        if (assembly.size() >= MAX_SIZE) {
            errored[0] = true;
            return;
        }

        BlockState state = world.getBlockState(pos);
        EnumICFPart part = partOf(state.getBlock());
        boolean validCasing = false;
        boolean validCore = false;

        if (part != null) {
            switch (part) {
                case CASING -> {
                    casings.add(pos);
                    validCasing = true;
                }
                case PORT -> {
                    ports.add(pos);
                    validCasing = true;
                }
                case CELL -> {
                    cells.add(pos);
                    validCore = true;
                }
                case EMITTER -> {
                    emitters.add(pos);
                    validCore = true;
                }
                case CAPACITOR -> {
                    capacitors.add(pos);
                    validCore = true;
                }
                case TURBO -> {
                    turbochargers.add(pos);
                    validCore = true;
                }
            }
        }

        if (validCasing) {
            assembly.put(pos, state);
            return;
        }

        if (validCore) {
            assembly.put(pos, state);
            for (Direction facing : Direction.values()) {
                floodFill(world, pos.relative(facing), assembly, casings, ports, cells, emitters, capacitors, turbochargers, errored);
            }
            return;
        }

        errored[0] = true;
    }

    @Nullable
    public static EnumICFPart partOf(Block block) {
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
        return switch (path) {
            case "icf_laser_component_casing" -> EnumICFPart.CASING;
            case "icf_laser_component_port" -> EnumICFPart.PORT;
            case "icf_laser_component_cell" -> EnumICFPart.CELL;
            case "icf_laser_component_emitter" -> EnumICFPart.EMITTER;
            case "icf_laser_component_capacitor" -> EnumICFPart.CAPACITOR;
            case "icf_laser_component_turbo" -> EnumICFPart.TURBO;
            default -> null;
        };
    }

    public enum EnumICFPart {
        CASING, PORT, CELL, EMITTER, CAPACITOR, TURBO
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        // CE MachineICFController.java:218-223
        if (!(world.getBlockEntity(pos) instanceof IcfControllerBlockEntity icf)) return;
        List<Component> text = new ArrayList<>();
        text.add(Component.literal(BobMathUtil.getShortNumber(icf.getPower()) + "/"
                + BobMathUtil.getShortNumber(icf.getMaxPower()) + " HE"));
        ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
