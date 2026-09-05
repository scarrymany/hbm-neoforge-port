package com.hbm.blocks.machine;

import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.DoorGenericBlockEntities;
import com.hbm.blockentity.machine.SlidingBlastDoorBlockEntity;
import com.hbm.blockentity.machine.SlidingBlastDoorKeypadBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.generic.GenericBlocks;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IDoor;
import com.hbm.interfaces.IRadResistantBlock;
import com.hbm.items.tool.ItemKey;
import com.hbm.items.tool.ItemLock;
import com.hbm.items.tool.ItemTooling;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * CE {@code BlockSlidingBlastDoor} — Dummyable {3,0,0,0,3,3} offset 0.
 * {@code sliding_blast_door} itself is {@code BlockDoorGeneric}; this class is legacy / _2 / keypad.
 * TODO(CE: BlockSlidingBlastDoor.java:17): Galacticraft {@code IPartialSealableBlock}.
 * Keypad extras open {@link com.hbm.inventory.gui.machine.KeypadScreen} (functional, not OBJ VFX).
 */
public class BlockSlidingBlastDoor extends BlockDummyable implements IRadResistantBlock {

    public BlockSlidingBlastDoor(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean isSameMultiblock(Block other) {
        return other == GenericBlocks.SLIDING_BLAST_DOOR_LEGACY.get()
                || other == GenericBlocks.SLIDING_BLAST_DOOR_2.get()
                || other == GenericBlocks.SLIDING_BLAST_DOOR_KEYPAD.get();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public int[] getDimensions() {
        return new int[]{3, 0, 0, 0, 3, 3};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (this == GenericBlocks.SLIDING_BLAST_DOOR_KEYPAD.get()) {
            return new SlidingBlastDoorKeypadBlockEntity(DoorGenericBlockEntities.SLIDING_BLAST_KEYPAD.get(), pos, state);
        }
        return state.getValue(META) >= 12
                ? new SlidingBlastDoorBlockEntity(DoorGenericBlockEntities.SLIDING_BLAST_DOOR.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == DoorGenericBlockEntities.SLIDING_BLAST_DOOR.get()
                || type == DoorGenericBlockEntities.SLIDING_BLAST_KEYPAD.get()) {
            return ITickableBE.ticker();
        }
        return null;
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        // CE BlockSlidingBlastDoor.java:197-208 — placed pos, offset 0. CCW dummy gets extra.
        if (this != GenericBlocks.SLIDING_BLAST_DOOR_2.get()) return;
        BlockPos pos = placedPos.above().relative(dir.getClockWise(), 3);
        BlockPos pos2 = placedPos.above().relative(dir.getCounterClockWise(), 3);
        BlockState a = level.getBlockState(pos);
        BlockState b = level.getBlockState(pos2);
        if (!a.hasProperty(META) || !b.hasProperty(META)) return;
        safeRem = true;
        level.setBlock(pos, GenericBlocks.SLIDING_BLAST_DOOR_KEYPAD.get().defaultBlockState().setValue(META, a.getValue(META)), 3);
        level.setBlock(pos2, GenericBlocks.SLIDING_BLAST_DOOR_KEYPAD.get().defaultBlockState().setValue(META, b.getValue(META) + extra), 3);
        safeRem = false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (this == GenericBlocks.SLIDING_BLAST_DOOR_KEYPAD.get()) {
            if (level.getBlockEntity(pos) instanceof MenuProvider provider) {
                player.openMenu(provider, pos);
                return InteractionResult.CONSUME;
            }
            return InteractionResult.PASS;
        }
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ItemLock || held.getItem() instanceof ItemKey) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        BlockEntity te = findCoreBlockEntity(level, pos);
        if (!(te instanceof SlidingBlastDoorBlockEntity door)) return InteractionResult.FAIL;
        if (held.getItem() instanceof ItemTooling tool && tool.getType() == ToolType.SCREWDRIVER) {
            if (door.getConfiguredMode() == IDoor.Mode.TOOLABLE) {
                if (!door.canToggleRedstone(player)) return InteractionResult.FAIL;
                door.toggleRedstoneMode();
                return InteractionResult.SUCCESS;
            }
        }
        if (door.isRedstoneOnly()) return InteractionResult.FAIL;
        return door.tryToggle(player) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return doorShape(state, level, pos);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = doorShape(state, level, pos);
        if (shape.isEmpty()) return Shapes.empty();
        AABB box = shape.bounds();
        if (box.minY == 0 && box.maxY == 0) return Shapes.empty();
        return shape;
    }

    private VoxelShape doorShape(BlockState state, BlockGetter level, BlockPos pos) {
        if (this == GenericBlocks.SLIDING_BLAST_DOOR_KEYPAD.get()) return Shapes.block();
        int meta = state.getValue(META);
        if (hasExtra(meta)) {
            if (level.getBlockState(pos.above()).getBlock() == this) return Shapes.empty();
            return Shapes.create(new AABB(0, 0.5, 0, 1, 1, 1));
        }
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof SlidingBlastDoorBlockEntity door && !door.shouldUseBB) return Shapes.empty();
        return Shapes.block();
    }

    @Override
    public boolean isRadResistant(Level level, BlockPos pos) {
        if (level == null) return false;
        BlockPos core = findCore(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof IDoor door) {
            return door.getState() == IDoor.DoorState.CLOSED;
        }
        return false;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !state.is(oldState.getBlock())) {
            RadiationSystemNT.markSectionForRebuild(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            RadiationSystemNT.markSectionForRebuild(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§2[").append(Component.translatable("trait.radshield")).append("]"));
        float hardness = this.getExplosionResistance();
        if (hardness > 50) {
            tooltip.add(Component.translatable("trait.blastres", hardness).withStyle(ChatFormatting.GOLD));
        }
        if (this == GenericBlocks.SLIDING_BLAST_DOOR_LEGACY.get()) {
            tooltip.add(Component.translatable("desc.varwin"));
        } else if (this == GenericBlocks.SLIDING_BLAST_DOOR_2.get()) {
            tooltip.add(Component.translatable("desc.varkey"));
        }
    }
}
