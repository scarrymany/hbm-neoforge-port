package com.hbm.blocks.machine.pile;

import com.hbm.api.block.IToolable;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.pile.PileBlockEntities;
import com.hbm.blockentity.machine.pile.PileControlBlockEntity;
import com.hbm.blockentity.machine.pile.PileCoreBlockEntity;
import com.hbm.blockentity.machine.pile.PileLoaderBlockEntity;
import com.hbm.blockentity.machine.pile.PileVentBlockEntity;
import com.hbm.blocks.ILookOverlay;
import com.hbm.lib.HBMSoundHandler;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code BlockPileDevice}. One block, metas 0–11 (type*4 + facing).
 * Item flatten 0/1/2 = {@code pile_device}/{@code _1}/{@code _2}.
 * TODO(CE: RenderPileLoader/Vent/Control.java:1): OBJ TESR lever/slider/fan — cube + CE png.
 */
public class BlockPileDevice extends BaseEntityBlock implements IToolable, ILookOverlay {

    public static final MapCodec<BlockPileDevice> CODEC = simpleCodec(BlockPileDevice::new);

    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 11);

    public static final int ITEM_META_LOADER = 0;
    public static final int ITEM_META_VENT = 1;
    public static final int ITEM_META_CONTROL = 2;

    public static final int BLOCK_META_LOADER = 0;
    public static final int BLOCK_META_VENT = 4;
    public static final int BLOCK_META_CONTROL = 8;

    public BlockPileDevice(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(META, BLOCK_META_LOADER));
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int itemMeta = 0;
        if (context.getItemInHand().getItem() instanceof PileDeviceItem item) {
            itemMeta = item.itemMeta;
        }
        int metaOffset = itemMetaToBlockMeta(itemMeta);
        int face = Mth.clamp(context.getClickedFace().get3DDataValue() - 2, 0, 3);
        return this.defaultBlockState().setValue(META, metaOffset + face);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        // CE BlockPileDevice.java:129 — only yaw-overwrite when META == CONTROL (8), not 9–11.
        if (state.getValue(META) != BLOCK_META_CONTROL || placer == null) return;
        int i = Mth.floor(placer.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        int meta = BLOCK_META_CONTROL;
        if (i == 0) meta = BLOCK_META_CONTROL;
        if (i == 1) meta = BLOCK_META_CONTROL + 3;
        if (i == 2) meta = BLOCK_META_CONTROL + 1;
        if (i == 3) meta = BLOCK_META_CONTROL + 2;
        level.setBlock(pos, state.setValue(META, meta), 2);
    }

    public static int itemMetaToBlockMeta(int meta) {
        if (meta >= ITEM_META_CONTROL) return BLOCK_META_CONTROL;
        if (meta == ITEM_META_VENT) return BLOCK_META_VENT;
        return BLOCK_META_LOADER;
    }

    public static int damageDropped(BlockState state) {
        int meta = state.getValue(META);
        if (meta >= BLOCK_META_CONTROL) return ITEM_META_CONTROL;
        if (meta >= BLOCK_META_VENT) return ITEM_META_VENT;
        return ITEM_META_LOADER;
    }

    public static Item itemForState(BlockState state) {
        int d = damageDropped(state);
        if (d == ITEM_META_CONTROL) return PileBlocks.PILE_DEVICE_CONTROL.get();
        if (d == ITEM_META_VENT) return PileBlocks.PILE_DEVICE_VENT.get();
        return PileBlocks.PILE_DEVICE_LOADER.get();
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(itemForState(state)));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target,
                                        net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
        return new ItemStack(itemForState(state));
    }

    // TODO(CE: BlockPileDevice.java:163-168): isSideSolid — 1.21 has no Block#isFaceSturdy;
    // loader/control face-only / vent none. Collision stays default cube. Do not invent.

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        int meta = state.getValue(META) - state.getValue(META) % 4;
        if (meta == BLOCK_META_LOADER) return new PileLoaderBlockEntity(PileBlockEntities.PILE_LOADER.get(), pos, state);
        if (meta == BLOCK_META_VENT) return new PileVentBlockEntity(PileBlockEntities.PILE_VENT.get(), pos, state);
        if (meta == BLOCK_META_CONTROL) return new PileControlBlockEntity(PileBlockEntities.PILE_CONTROL.get(), pos, state);
        return null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == PileBlockEntities.PILE_LOADER.get()
                || type == PileBlockEntities.PILE_VENT.get()
                || type == PileBlockEntities.PILE_CONTROL.get()) {
            return ITickableBE.ticker();
        }
        return null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!tryUseLoader(level, pos, state, player, hand, stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!tryUseLoader(level, pos, state, player, player.getUsedItemHand(), player.getItemInHand(player.getUsedItemHand()))) {
            return InteractionResult.PASS;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean tryUseLoader(Level level, BlockPos pos, BlockState state, Player player,
                                        InteractionHand hand, ItemStack held) {
        int meta = state.getValue(META);
        meta -= meta % 4;
        if (meta != BLOCK_META_LOADER) return false;
        if (level.isClientSide) return true;

        if (!(level.getBlockEntity(pos) instanceof PileLoaderBlockEntity tile)) return true;

        if (tile.insertLevel <= 0 && !tile.loading) {
            if (!held.isEmpty() && tile.stack.isEmpty() && PileLoaderBlockEntity.isItemLoadable(held)) {
                tile.stack = held.copy();
                tile.stack.setCount(1);
                held.shrink(1);
                level.playSound(null, pos, HBMSoundHandler.upgradePlug.get(), SoundSource.BLOCKS, 1F, 1F);
                tile.setChanged();
                tile.dataChanged();
                return true;
            }
            tile.loading = true;
            tile.setChanged();
        }
        return true;
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ,
                           InteractionHand hand, ToolType tool) {
        int meta = world.getBlockState(new BlockPos(x, y, z)).getValue(META);
        if (meta >= BLOCK_META_CONTROL) {
            y -= 1;
            side = Direction.UP;
        } else {
            Direction dir = Direction.from3DDataValue(meta % 4 + 2);
            x -= dir.getStepX();
            z -= dir.getStepZ();
            side = dir;
        }

        if (world.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof BlockPile pile) {
            return pile.onScrew(world, player, x, y, z, side, fX, fY, fZ, hand, tool);
        }
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        List<Component> text = new ArrayList<>();
        BlockEntity tile = world.getBlockEntity(pos);

        if (tile instanceof PileLoaderBlockEntity device) {
            text.add(Component.literal("Temp: " + Math.round(device.channelTemp) + " / " + PileCoreBlockEntity.MAX_HEAT + "°C"));
            if (!device.syncStack.isEmpty()) {
                text.add(Component.literal("Loading: ").append(device.syncStack.getHoverName()));
            }
            if (!device.channelStack.isEmpty()) {
                text.add(Component.literal("Last rod: ").append(device.channelStack.getHoverName()));
                if (device.channelDepletion > 0) {
                    text.add(Component.literal("Depletion: " + Math.round(device.channelDepletion) + "%"));
                }
            }
        }

        if (tile instanceof PileControlBlockEntity device) {
            text.add(Component.literal("Extraction level: " + (int) (device.rodLevel * 100) + "%"));
        }

        if (!text.isEmpty()) {
            ILookOverlay.printGeneric(event, Component.translatable("block.hbm.pile_device_" + damageDropped(state)),
                    0xffff00, 0x404000, text);
        }
    }
}
