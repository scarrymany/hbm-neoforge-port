package com.hbm.blocks.generic;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.function.Supplier;

/**
 * Statue-prop deco block with a small area-effect aura, ported from CE's {@code DecoBlockAlt} (the
 * {@code statue_elb} progression: {@code statue_elb} -> {@code statue_elb_g}/{@code statue_elb_w} ->
 * {@code statue_elb_f}). CE advances the statue by right-clicking it with two specific items
 * ({@code ModItems.nothing}, {@code ModItems.watch}); neither exists in this port's item catalog
 * yet, so the item-driven upgrade chain is a documented gap - the block shape, drop-as-base-statue
 * behavior, and the final stage's area heal/saturation pulse (CE's {@code TileEntityDecoBlockAlt},
 * hardcoded to the final stage only) are fully ported.
 */
public class DecoBlockAlt extends BaseEntityBlock {

    /**
     * {@code pulsing} is a construction-time flag (see {@code GenericDecoBlocks}' four registrations,
     * three {@code false} and one {@code true}) rather than data carried in a saved {@link BlockState},
     * so - matching neo-edition's {@code CrateBlock} precedent for its own construction-time {@code Type}
     * field - the codec's reflective-construction path just pins the common, non-pulsing variant.
     */
    public static final MapCodec<DecoBlockAlt> CODEC = simpleCodec(properties -> new DecoBlockAlt(properties, false));

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    private static final int PULSE_RADIUS = 8;

    private final boolean pulsing;
    private Supplier<? extends Block> baseStatue;

    public DecoBlockAlt(Properties properties, boolean pulsing) {
        super(properties);
        this.pulsing = pulsing;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** The block returned as this statue's drop, matching CE's {@code getItemDropped} override. */
    public void setBaseStatue(Supplier<? extends Block> baseStatue) {
        this.baseStatue = baseStatue;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(baseStatue != null ? baseStatue.get() : this));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return pulsing ? new StatuePulseBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!pulsing || level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof StatuePulseBlockEntity pulse) {
                pulse.tick(lvl, pos);
            }
        };
    }

    public static class StatuePulseBlockEntity extends BlockEntity {

        public StatuePulseBlockEntity(BlockPos pos, BlockState state) {
            super(GenericDecoBlocks.STATUE_PULSE_ENTITY_TYPE.get(), pos, state);
        }

        void tick(Level level, BlockPos pos) {
            AABB area = new AABB(pos).inflate(PULSE_RADIUS);
            List<Entity> nearby = level.getEntities((Entity) null, area);
            for (Entity entity : nearby) {
                if (entity.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > PULSE_RADIUS * PULSE_RADIUS) {
                    continue;
                }
                if (entity instanceof Player player) {
                    player.addEffect(new MobEffectInstance(MobEffects.HEAL, 5, 99));
                    player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 5, 99));
                }
            }
        }
    }
}
