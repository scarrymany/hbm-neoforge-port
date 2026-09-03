package com.hbm.blocks.machine;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.blockentity.machine.CargoElevatorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Port of CE {@code com.hbm.blocks.machine.BlockCargoElevator} - a hydraulic cargo elevator platform.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/blocks/machine/BlockCargoElevator.java (3x1x3 Dummyable)
 * <p>
 * Ported: toggleElevator interaction (CE :82-122), dynamic height growth (CE :92-117),
 * collision boxes for pillars+platform (CE :126-138, :208-219), dynamic drops based on height (CE :190-205),
 * custom highlight rendering (CE :162-187).
 */
public class BlockCargoElevator extends BlockDummyable {

    public static final MapCodec<BlockCargoElevator> CODEC = simpleCodec(BlockCargoElevator::new);

    public BlockCargoElevator(Properties props) {
        super(props);
    }

    @Override
    protected @NotNull MapCodec<? extends BlockDummyable> codec() {
        return CODEC;
    }

    @Override
    public int[] getDimensions() {
        // CE getDimensions() returns {0, 0, 1, 1, 1, 1} for a 3x1x3 footprint (xMin, xMax, yMin, yMax, zMin, zMax)
        return new int[]{0, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        // CE getOffset() = 1 (core at y=0)
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        // Only core (META >= 12) has a BlockEntity
        int meta = state.getValue(META);
        if (meta >= 12) {
            return new CargoElevatorBlockEntity(pos, state);
        }
        return null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return ITickableBE.ticker();
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        // CE uses ENTITYBLOCK_ANIMATED; for now MODEL is simpler (TESR deferred)
        return RenderShape.MODEL;
    }

    // CE :82-122: onBlockActivated - toggle elevator or grow height
    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player.isCrouching()) return InteractionResult.PASS;

        BlockPos corePos = findCore(level, pos);
        if (corePos == null) return InteractionResult.SUCCESS;

        BlockEntity tile = level.getBlockEntity(corePos);
        if (!(tile instanceof CargoElevatorBlockEntity elevator)) return InteractionResult.SUCCESS;

        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);

        // CE :92-117: If holding cargo_elevator block, grow height by 1 layer
        if (!heldItem.isEmpty() && heldItem.getItem() == this.asItem()) {
            boolean replaceable = true;
            int topY = corePos.getY() + elevator.height + 1;

            // Check if space above is clear (3x1x3)
            for (int x = corePos.getX() - 1; x < corePos.getX() + 2 && replaceable; x++) {
                for (int z = corePos.getZ() - 1; z < corePos.getZ() + 2; z++) {
                    BlockPos checkPos = new BlockPos(x, topY, z);
                    BlockState checkState = level.getBlockState(checkPos);
                    if (!checkState.canBeReplaced()) {
                        replaceable = false;
                        break;
                    }
                }
            }

            if (replaceable) {
                // Place dummy blocks at new top layer
                for (int x = corePos.getX() - 1; x < corePos.getX() + 2; x++) {
                    for (int z = corePos.getZ() - 1; z < corePos.getZ() + 2; z++) {
                        level.setBlock(new BlockPos(x, topY, z),
                                this.defaultBlockState().setValue(META, 1), 3);
                    }
                }
                elevator.height++;
                elevator.setChanged();

                if (!player.isCreative()) {
                    heldItem.shrink(1);
                }
            }
        } else {
            // CE :119: Empty hand = toggle elevator
            elevator.toggleElevator();
        }

        return InteractionResult.SUCCESS;
    }

    // CE :126-138, :208-219: Collision boxes - 4 corner pillars + platform
    @Override
    protected @NotNull VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        BlockPos corePos = findCore(level, pos);
        if (corePos == null) return Shapes.empty();

        BlockEntity tile = level instanceof Level lvl ? lvl.getBlockEntity(corePos) : null;
        if (!(tile instanceof CargoElevatorBlockEntity elevator)) return Shapes.empty();

        return getElevatorCollisionShape(elevator, corePos);
    }

    private VoxelShape getElevatorCollisionShape(CargoElevatorBlockEntity elevator, BlockPos corePos) {
        int x = corePos.getX();
        int y = corePos.getY();
        int z = corePos.getZ();
        int height = elevator.height + 1;
        double extension = elevator.extension;

        // CE :213-218: 4 corner pillars (0.25 block thick) + platform (0.25 block thick at extension height)
        VoxelShape pillarNW = Shapes.box(x - 1, y, z - 1, x - 0.75D, y + height, z - 0.75D);
        VoxelShape pillarNE = Shapes.box(x - 1, y, z + 1.75D, x - 0.75D, y + height, z + 2D);
        VoxelShape pillarSW = Shapes.box(x + 1.75D, y, z - 1, x + 2D, y + height, z - 0.75D);
        VoxelShape pillarSE = Shapes.box(x + 1.75D, y, z + 1.75D, x + 2D, y + height, z + 2D);
        VoxelShape platform = Shapes.box(x - 1, y + 0.75D + extension, z - 1, x + 2D, y + 1D + extension, z + 2D);

        return Shapes.or(pillarNW, pillarNE, pillarSW, pillarSE, platform);
    }

    // CE :162-187: Custom highlight rendering - draw outline for all elevator AABBs
    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean shouldDrawHighlight(Level level, BlockPos pos) {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawHighlight(RenderHighlightEvent.Block event, Level level, BlockPos pos) {
        BlockPos corePos = findCore(level, pos);
        if (corePos == null) return;

        BlockEntity tile = level.getBlockEntity(corePos);
        if (!(tile instanceof CargoElevatorBlockEntity elevator)) return;

        Vec3 camera = event.getCamera().getPosition();
        float expand = 0.002F;

        PoseStack poseStack = event.getPoseStack();
        VertexConsumer vertexConsumer = event.getMultiBufferSource().getBuffer(RenderType.lines());

        // CE :183: Draw all elevator AABBs (4 pillars + platform)
        for (AABB aabb : getElevatorAABBs(elevator, corePos)) {
            AABB transformed = aabb.inflate(expand).move(-camera.x, -camera.y, -camera.z);
            LevelRenderer.renderLineBox(poseStack, vertexConsumer, transformed, 0.0F, 0.0F, 0.0F, 0.4F);
        }
    }

    /**
     * CE :208-219 — Returns 5 AABBs: 4 corner pillars (0.25 thick) + platform (0.25 thick at extension).
     */
    private AABB[] getElevatorAABBs(CargoElevatorBlockEntity elevator, BlockPos corePos) {
        int x = corePos.getX();
        int y = corePos.getY();
        int z = corePos.getZ();
        int height = elevator.height + 1;
        double extension = elevator.extension;

        return new AABB[] {
                new AABB(x - 1, y, z - 1, x - 0.75D, y + height, z - 0.75D),
                new AABB(x - 1, y, z + 1.75D, x - 0.75D, y + height, z + 2D),
                new AABB(x + 1.75D, y, z - 1, x + 2D, y + height, z - 0.75D),
                new AABB(x + 1.75D, y, z + 1.75D, x + 2D, y + height, z + 2D),
                new AABB(x - 1, y + 0.75D + extension, z - 1, x + 2D, y + 1D + extension, z + 2D),
        };
    }

    /**
     * CE :190-205 — Dynamic drops based on elevator height: drops (height+1) blocks instead of 1.
     * Finds the core TE and returns stacks totaling elevator.height + 1.
     */
    @Override
    public @NotNull java.util.List<ItemStack> getDrops(@NotNull BlockState state, net.minecraft.world.level.storage.loot.LootParams.@NotNull Builder builder) {
        Level level = builder.getLevel();
        BlockPos pos = BlockPos.containing(builder.getParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN));

        BlockPos corePos = findCore(level, pos);
        if (corePos == null) {
            return super.getDrops(state, builder);
        }

        BlockEntity tile = level.getBlockEntity(corePos);
        if (!(tile instanceof CargoElevatorBlockEntity elevator)) {
            return super.getDrops(state, builder);
        }

        // CE :198-203: drop (height+1) blocks, split into stacks of max 64
        int toDrop = elevator.height + 1;
        java.util.List<ItemStack> drops = new java.util.ArrayList<>();
        while (toDrop > 0) {
            int perStack = Math.min(toDrop, 64);
            toDrop -= perStack;
            drops.add(new ItemStack(this, perStack));
        }
        return drops;
    }
}
