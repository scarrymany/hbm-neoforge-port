package com.hbm.blocks.network;

import com.hbm.api.block.IToolable;
import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.api.conveyor.IConveyorItem;
import com.hbm.api.conveyor.IConveyorPackage;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.blockentity.network.ConveyorBlockEntities;
import com.hbm.blockentity.network.CraneSplitterBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.entity.ConveyorEntityTypes;
import com.hbm.entity.item.EntityMovingItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.blocks.network.CraneSplitter} (read in full) - a 2-wide
 * {@link BlockDummyable} multiblock, ratio-based 1-input/2-output item splitter for conveyor
 * networks. {@code getDimensions() = {0,0,0,0,0,1}} (1 dummy to the "right" of the core, in
 * {@link com.hbm.handler.MultiblockHandlerXR}'s {@code {UP,DOWN,NORTH,SOUTH,WEST,EAST}} baseline
 * frame).
 * <p>
 * CE's companion {@code DummyBlockCraneSplitter}/{@code TileEntityProxyCombo(false,false,false)} pair
 * (a separate registry block + a capability-delegating placeholder block entity for the dummy
 * position) is <b>not</b> ported - this port's established {@link BlockDummyable} convention (see
 * {@code PylonMediumBlock}) is one registry {@code Block} shared by every position of a multiblock,
 * with {@link #newBlockEntity} returning a real block entity only for the core (meta 12-15) and
 * {@code null} everywhere else. The dummy position needs no block entity at all here: CE's
 * {@code TileEntityProxyCombo(false,false,false)} was constructed with every capability flag off (no
 * inventory/power/fluid delegation), i.e. it existed purely to satisfy 1.12's "every
 * {@code BlockContainer} position needs *a* tile entity" requirement - a requirement modern
 * {@code BaseEntityBlock}/{@code newBlockEntity} does not have.
 * <p>
 * {@link IConveyorBelt}/{@link IEnterableBlock}/{@link IToolable} are already-ported Phase 0
 * interfaces (see {@code docs/phase2/blocks_network_conveyor_crane.md}); no changes were needed to
 * implement them here.
 */
public class CraneSplitterBlock extends BlockDummyable
        implements IConveyorBelt, IEnterableBlock, ITooltipProvider, IToolable, ILookOverlay {

    public CraneSplitterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 0, 0, 0, 1};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new CraneSplitterBlockEntity(ConveyorBlockEntities.CRANE_SPLITTER.get(), pos, state)
                : null;
    }

    private Direction getCustomMap(int meta) {
        return switch (meta) {
            case 2, 14 -> Direction.EAST;
            case 5, 12 -> Direction.SOUTH;
            case 3, 15 -> Direction.WEST;
            default -> Direction.NORTH;
        };
    }

    @Override
    public boolean canItemEnter(Level world, int x, int y, int z, Direction dir, IConveyorItem entity) {
        return getTravelDirection(world, new BlockPos(x, y, z)) == dir;
    }

    public Direction getTravelDirection(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        int meta = state.getValue(META);

        if (meta < 12) {
            BlockPos corePos = findCore(world, pos);
            if (corePos != null) {
                meta = world.getBlockState(corePos).getValue(META);
            }
        }

        return getCustomMap(meta).getOpposite();
    }

    @Override
    public boolean canItemStay(Level world, int x, int y, int z, Vec3 itemPos) {
        return true;
    }

    @Override
    public boolean canPackageEnter(Level world, int x, int y, int z, Direction dir, IConveyorPackage entity) {
        return false;
    }

    @Override
    public void onPackageEnter(Level world, int x, int y, int z, Direction dir, IConveyorPackage entity) {
    }

    @Override
    public Vec3 getTravelLocation(Level world, int x, int y, int z, Vec3 itemPos, double speed) {
        BlockPos pos = new BlockPos(x, y, z);
        Direction dir = this.getTravelDirection(world, pos);
        Vec3 snap = this.getClosestSnappingPosition(world, pos, itemPos);
        Vec3 dest = new Vec3(
                snap.x - dir.getStepX() * speed,
                snap.y - dir.getStepY() * speed,
                snap.z - dir.getStepZ() * speed);
        Vec3 motion = dest.subtract(itemPos);
        double len = motion.length();
        if (len < 1.0e-6) return itemPos;
        return new Vec3(
                itemPos.x + motion.x / len * speed,
                itemPos.y + motion.y / len * speed,
                itemPos.z + motion.z / len * speed);
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level world, BlockPos pos, Vec3 itemPos) {
        Direction dir = this.getTravelDirection(world, pos);

        double posX = Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1);
        double posZ = Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1);

        double x = pos.getX() + 0.5;
        double z = pos.getZ() + 0.5;
        double y = pos.getY() + 0.25;

        if (dir.getAxis() == Direction.Axis.X) {
            x = posX;
        } else if (dir.getAxis() == Direction.Axis.Z) {
            z = posZ;
        }

        return new Vec3(x, y, z);
    }

    @Override
    public void onItemEnter(Level world, int x, int y, int z, Direction dir, IConveyorItem entity) {
        if (entity == null || entity.getItemStack().isEmpty() || entity.getItemStack().getCount() <= 0) {
            return;
        }
        BlockPos pos = new BlockPos(x, y, z);
        if (!(findCoreBlockEntity(world, pos) instanceof CraneSplitterBlockEntity splitter)) return;

        BlockPos corePos = splitter.getBlockPos();
        int coreMeta = world.getBlockState(corePos).getValue(META);
        Direction coreDir = Direction.from3DDataValue(coreMeta - offset);
        // "the right belt" relative to the core's own facing - derived from (and matching)
        // MultiblockHandlerXR.rotate's {UP,DOWN,NORTH,SOUTH,WEST,EAST} dummy-placement math for this
        // block's {0,0,0,0,0,1} dimensions, not a re-implementation of CE's ForgeDirection rotation
        // table (which has no 1.21.1 equivalent in this port).
        Direction dummyDir = coreDir.getCounterClockWise();

        ItemStack[] splits = splitter.splitStack(entity.getItemStack());

        spawnMovingItem(world, corePos, splits[0]);
        spawnMovingItem(world, corePos.relative(dummyDir), splits[1]);
    }

    private void spawnMovingItem(Level world, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() <= 0) return;

        EntityMovingItem moving = new EntityMovingItem(ConveyorEntityTypes.MOVING_ITEM.get(), world);
        Vec3 itemPos = new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        Vec3 snap = this.getClosestSnappingPosition(world, pos, itemPos);
        moving.moveTo(snap.x, snap.y, snap.z);
        moving.setItemStack(stack);
        world.addFreshEntity(moving);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        this.addStandardInfo(tooltip);
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ, InteractionHand hand,
                            ToolType tool) {
        if (world.isClientSide) return true;
        if (tool != ToolType.SCREWDRIVER) return false;

        BlockPos pos = new BlockPos(x, y, z);
        if (!(findCoreBlockEntity(world, pos) instanceof CraneSplitterBlockEntity crane)) return false;

        // The core of the dummy is always the left hand block
        boolean isLeft = pos.equals(crane.getBlockPos());
        int adjust = player.isShiftKeyDown() ? -1 : 1;

        if (isLeft) {
            crane.leftRatio = (byte) Mth.clamp(crane.leftRatio + adjust, 1, 16);
        } else {
            crane.rightRatio = (byte) Mth.clamp(crane.rightRatio + adjust, 1, 16);
        }

        crane.setChanged();
        crane.networkPackNT(15);

        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void printHook(RenderGuiEvent.Pre event, Level world, BlockPos pos) {
        if (!(findCoreBlockEntity(world, pos) instanceof CraneSplitterBlockEntity crane)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal("Splitter ratio: " + crane.leftRatio + ":" + crane.rightRatio));

        ILookOverlay.printGeneric(event, Component.translatable(this.getDescriptionId() + ".name"), 0xffff00, 0x404000, text);
    }
}
