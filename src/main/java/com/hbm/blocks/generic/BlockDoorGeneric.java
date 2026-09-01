package com.hbm.blocks.generic;

import com.hbm.api.block.IToolable;
import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.DoorGenericBlockEntities;
import com.hbm.blockentity.machine.DoorGenericBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IDoor;
import com.hbm.interfaces.IRadResistantBlock;
import com.hbm.items.tool.ItemKey;
import com.hbm.items.tool.ItemLock;
import com.hbm.items.tool.ItemTooling;
import com.hbm.tileentity.DoorDecl;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
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
 * CE {@code BlockDoorGeneric}. TESR / SEDNA / Galacticraft leftover
 * TODO(CE: DoorDecl.java:1285-1292): IRenderDoors OBJ skins.
 * TODO(CE: BlockDoorGeneric.java:17): IPartialSealableBlock.
 */
public class BlockDoorGeneric extends BlockDummyable implements IRadResistantBlock, IBomb, IToolable {

    public final DoorDecl type;
    public final boolean radResistant;

    public BlockDoorGeneric(Properties properties, DoorDecl type, boolean radResistant) {
        super(properties);
        this.type = type;
        this.radResistant = radResistant;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new DoorGenericBlockEntity(DoorGenericBlockEntities.DOOR_GENERIC.get(), pos, state)
                : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DoorGenericBlockEntities.DOOR_GENERIC.get() ? ITickableBE.ticker() : null;
    }

    @Override
    public int[] getDimensions() {
        return type.getDimensions();
    }

    @Override
    public int getOffset() {
        return type.getBlockOffset();
    }

    @Override
    public boolean checkRequirement(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        if (!super.checkRequirement(level, placedPos, dir, placementOffset)) return false;
        int[][] extra = type.getExtraDimensions();
        if (extra == null) return true;
        BlockPos core = placedPos.relative(dir, placementOffset);
        for (int[] dims : extra) {
            if (!MultiblockHandlerXR.checkSpace(level, core, dims, placedPos, dir)) return false;
        }
        return true;
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);
        int[][] extra = type.getExtraDimensions();
        if (extra == null) return;
        BlockPos core = placedPos.relative(dir, placementOffset);
        for (int[] dims : extra) {
            MultiblockHandlerXR.fillSpace(level, core, dims, this, dir);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity te = findCoreBlockEntity(level, pos);
        if (!(te instanceof DoorGenericBlockEntity door)) return InteractionResult.FAIL;

        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof ItemTooling tool && tool.getType() == ToolType.SCREWDRIVER) {
            if (door.getConfiguredMode() == IDoor.Mode.TOOLABLE) {
                if (!door.canToggleRedstone(player)) return InteractionResult.FAIL;
                door.toggleRedstoneMode();
                return InteractionResult.SUCCESS;
            }
        }

        if (held.getItem() instanceof ItemLock || held.getItem() instanceof ItemKey) {
            return InteractionResult.PASS;
        }

        if (door.isRedstoneOnly()) return InteractionResult.FAIL;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        if (door.canAccess(player)) {
            return door.tryToggle() ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        return InteractionResult.FAIL;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide) {
            BlockEntity te = findCoreBlockEntity(level, pos);
            if (te instanceof DoorGenericBlockEntity door) {
                door.updateRedstonePower(pos);
            }
        }
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
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
        int meta = state.getValue(META);
        BlockPos core = findCore(level, pos);
        if (core == null) return Shapes.block();
        BlockEntity local = level.getBlockEntity(pos);
        boolean open = hasExtra(meta) || (local instanceof DoorGenericBlockEntity door && door.shouldUseBB);
        BlockState coreState = level.getBlockState(core);
        if (!coreState.hasProperty(META)) return Shapes.block();
        int facingMeta = coreState.getValue(META) - offset;
        Direction facing = Direction.from3DDataValue(facingMeta);
        BlockPos rel = pos.subtract(core);
        Rotation rot = ceBlockRotation(facing).getRotated(Rotation.COUNTERCLOCKWISE_90);
        rel = DoorGenericBlockEntity.rotate(rel, rot);
        AABB box = type.getBlockBound(rel, open);
        if (box.getXsize() == 0 && box.getYsize() == 0 && box.getZsize() == 0) return Shapes.empty();
        AABB world = switch (facingMeta) {
            case 2 -> new AABB(1 - box.minX, box.minY, 1 - box.minZ, 1 - box.maxX, box.maxY, 1 - box.maxZ);
            case 4 -> new AABB(1 - box.minZ, box.minY, box.minX, 1 - box.maxZ, box.maxY, box.maxX);
            case 3 -> new AABB(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
            case 5 -> new AABB(box.minZ, box.minY, 1 - box.minX, box.maxZ, box.maxY, 1 - box.maxX);
            default -> new AABB(0, 0, 0, 1, 1, 1);
        };
        return Shapes.create(world);
    }

    /** CE {@code ForgeDirection.getBlockRotation()}. */
    private static Rotation ceBlockRotation(Direction dir) {
        return switch (dir) {
            case SOUTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            case WEST -> Rotation.CLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (radResistant && !level.isClientSide && !state.is(oldState.getBlock())) {
            RadiationSystemNT.markSectionForRebuild(level, pos);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (radResistant && !level.isClientSide && !state.is(newState.getBlock())) {
            RadiationSystemNT.markSectionForRebuild(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public boolean isRadResistant(Level level, BlockPos pos) {
        if (!radResistant) return false;
        if (level != null) {
            BlockPos core = findCore(level, pos);
            if (core != null && level.getBlockEntity(core) instanceof IDoor door) {
                return door.getState() == IDoor.DoorState.CLOSED;
            }
        }
        return false;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        BlockEntity te = findCoreBlockEntity(level, pos);
        if (!(te instanceof DoorGenericBlockEntity door)) return BombReturnCode.ERROR_INCOMPATIBLE;
        if (!door.getDoorType().remoteControllable()) return BombReturnCode.ERROR_INCOMPATIBLE;
        return door.tryToggle() ? BombReturnCode.TRIGGERED : BombReturnCode.ERROR_INCOMPATIBLE;
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER || !player.isShiftKeyDown()) return false;
        BlockPos core = findCore(world, new BlockPos(x, y, z));
        if (core == null) return false;
        if (!(world.getBlockEntity(core) instanceof DoorGenericBlockEntity door)) return false;
        if (!door.getDoorType().hasSkins()) return false;
        if (world.isClientSide) return true;
        return door.cycleSkinIndex();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        if (radResistant) {
            tooltip.add(Component.literal("§2[").append(Component.translatable("trait.radshield")).append("]"));
        }
        float hardness = this.getExplosionResistance();
        if (hardness > 50) {
            tooltip.add(Component.translatable("trait.blastres", hardness).withStyle(ChatFormatting.GOLD));
        }
        if (type.hasSkins()) {
            tooltip.add(Component.translatable("desc.doors_skin"));
        }
    }
}
