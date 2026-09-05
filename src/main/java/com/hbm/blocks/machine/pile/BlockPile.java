package com.hbm.blocks.machine.pile;

import com.hbm.api.block.IToolable;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.pile.PileBaseBlockEntity;
import com.hbm.blockentity.machine.pile.PileBlockEntities;
import com.hbm.blockentity.machine.pile.PileCoreBlockEntity;
import com.hbm.blocks.ILookOverlay;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code BlockPile} ({@code BlockPile.java}). Metas 0–8. Creative tab null.
 * Drop nothing; break (unless {@code meltingDown}) → {@code pile_brick} + core {@code destroy()}.
 */
public class BlockPile extends BaseEntityBlock implements IToolable, ILookOverlay {

    public static final MapCodec<BlockPile> CODEC = simpleCodec(BlockPile::new);

    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 8);

    public static final int META_DUMMY = 0;
    public static final int META_CORE = 1;
    public static final int META_CHANNEL = 2;
    public static final int META_FUEL_IN = 3;
    public static final int META_FUEL_OUT = 4;
    public static final int META_AIR_IN = 5;
    public static final int META_AIR_OUT = 6;
    public static final int META_CONTROL = 7;
    public static final int META_EDGE = 8;

    private static boolean reverting;

    public BlockPile(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(META, META_DUMMY));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(META);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (state.getValue(META) == META_CORE) {
            return new PileCoreBlockEntity(PileBlockEntities.PILE_CORE.get(), pos, state);
        }
        return new PileBaseBlockEntity(PileBlockEntities.PILE_BASE.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == PileBlockEntities.PILE_CORE.get() || type == PileBlockEntities.PILE_BASE.get()) {
            return ITickableBE.ticker();
        }
        return null;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !PileCoreBlockEntity.meltingDown && !level.isClientSide) {
            breakPile(level, pos, newState);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * CE {@code BlockPile.breakBlock}: revert to {@code pile_brick} + {@code core.destroy()}.
     * 1.21 {@code setBlock(air)} writes air *after* {@code onRemove}, so same-pos brick
     * must be queued via {@code server.execute} or the outer write clobbers it.
     */
    public static void breakPile(Level level, BlockPos pos) {
        breakPile(level, pos, level.getBlockState(pos));
    }

    public static void breakPile(Level level, BlockPos pos, BlockState newState) {
        if (reverting || PileCoreBlockEntity.meltingDown || level.isClientSide) return;
        reverting = true;
        try {
            BlockEntity tile = level.getBlockEntity(pos);
            boolean placeBrick = true;
            PileCoreBlockEntity core = null;
            if (tile instanceof PileBaseBlockEntity pile) {
                placeBrick = pile.coreY >= 0;
                core = pile.getCore();
            }
            if (placeBrick && !newState.is(PileBlocks.PILE_BRICK.get())) {
                BlockPos frozen = pos.immutable();
                if (level.getServer() != null) {
                    level.getServer().execute(() -> {
                        if (!level.getBlockState(frozen).is(PileBlocks.PILE_BRICK.get())) {
                            level.setBlock(frozen, PileBlocks.PILE_BRICK.get().defaultBlockState(), 3);
                        }
                    });
                } else {
                    level.setBlock(pos, PileBlocks.PILE_BRICK.get().defaultBlockState(), 3);
                }
            }
            if (core != null && !core.isRemoved()) core.destroy();
        } finally {
            reverting = false;
        }
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ,
                           InteractionHand hand, ToolType tool) {
        if (tool != ToolType.HAND_DRILL) return false;

        BlockPos pos = new BlockPos(x, y, z);
        BlockEntity tile = world.getBlockEntity(pos);

        if (tile instanceof PileCoreBlockEntity || world.getBlockState(pos).getValue(META) == META_CORE) {
            sendError(player, "Cannot intersect core");
            return false;
        }

        if (tile instanceof PileBaseBlockEntity base) {
            if (world.isClientSide) return true;
            PileCoreBlockEntity core = base.getCore();
            if (core != null) {
                Direction dir = side.getOpposite();
                return core.drillChannel(x, y, z, dir, player);
            }
        }

        sendError(player, "No core found");
        return false;
    }

    // TODO(CE: MachinePWRController.sendError): floating marker particle — chat only.
    public static void sendError(Player player, String message) {
        player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        int meta = world.getBlockState(pos).getValue(META);
        List<Component> text = new ArrayList<>();
        if (meta == META_FUEL_IN) text.add(Component.literal("Fuel Loading Port"));
        if (meta == META_FUEL_OUT) text.add(Component.literal("Fuel Ejection Port"));
        if (meta == META_AIR_IN) text.add(Component.literal("Air Inlet"));
        if (meta == META_AIR_OUT) text.add(Component.literal("Air Outlet"));
        if (meta == META_CONTROL) text.add(Component.literal("Control Rod Channel"));

        if (meta == META_CORE) {
            BlockEntity tile = world.getBlockEntity(pos);
            if (tile instanceof PileCoreBlockEntity core) {
                text.add(Component.literal("Max Temp: " + Math.round(core.highestHeat) + " / " + PileCoreBlockEntity.MAX_HEAT + "°C"));
            }
        }

        if (!text.isEmpty()) {
            ILookOverlay.printGeneric(event, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
        }
    }
}
