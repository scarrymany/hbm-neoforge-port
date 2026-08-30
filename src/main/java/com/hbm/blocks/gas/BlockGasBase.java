package com.hbm.blocks.gas;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Abstract gas-cloud base, ported from CE's {@code BlockGasBase}: an invisible, non-collidable,
 * non-solid cell that slowly "flows" through open space by repeatedly relocating itself into an
 * adjacent air block, one step at a time, driven by vanilla's random-tick sampler.
 * <p>
 * <b>tickRandomly/updateTick -&gt; randomTick.</b> CE sets {@code setTickRandomly(true)} and puts all
 * of its movement logic in the single {@code updateTick} method, which in 1.12 is invoked either by
 * the ambient random-tick sampler (because {@code tickRandomly} is set) or by an explicit
 * {@code World.scheduleUpdate(pos, block, delay)} call - both paths call the exact same method. 1.21
 * splits that one method into two: {@link #randomTick} (ambient sampling, gated by
 * {@link net.minecraft.world.level.block.state.BlockBehaviour.Properties#randomTicks()}) and
 * {@link #tick} (fired only by an explicit {@link LevelAccessor#scheduleTick}). This class only ever
 * needs the ambient path - {@link BlockGasFlammable} is the one that also needs the explicit-schedule
 * path, for its neighbor-ignition propagation - so only {@link #randomTick} is overridden here; see
 * that subclass for the propagation-only {@code tick} override and the shared {@code moveGas} used by
 * both.
 * <p>
 * <b>Shape/render/replace/loot.</b> CE achieves "not really there" via a pile of individual
 * overrides ({@code getCollisionBoundingBox} -&gt; {@code NULL_AABB}, {@code canCollideCheck}/
 * {@code isCollidable}/{@code isOpaqueCube}/{@code isBlockNormalCube} -&gt; {@code false},
 * {@code getRenderType} -&gt; {@code INVISIBLE}, {@code isReplaceable} -&gt; {@code true}, empty
 * {@code getDrops}/{@code quantityDropped}). In 1.21 these collapse onto a mix of instance overrides
 * ({@link #getShape}, {@link #getCollisionShape}, {@link #getRenderShape}) and declarative
 * {@link net.minecraft.world.level.block.state.BlockBehaviour.Properties} flags applied in this
 * constructor ({@code noCollission()}, {@code noOcclusion()}, {@code replaceable()},
 * {@code noLootTable()}) - baked in here, rather than left to each registration call site, since
 * every CE gas block (ported or not) shares them unconditionally.
 * <p>
 * <b>Not ported: CE's {@code isAir(state, world, pos)} override.</b> CE overrides this so
 * {@code World.destroyBlock} treats a gas cell like real air (no break sound/particles when e.g. a
 * fluid displaces it). Modern {@code BlockState.isAir()} is a cached no-arg field set from
 * {@code getBlock() instanceof AirBlock} at state-bake time, with no per-block override point taking
 * world/pos context (confirmed by grep: no gas or fluid block in the Neo Edition reference attempts
 * this) - there is nothing to hook here. Purely cosmetic (an extra break effect on an already
 * invisible, harmless, instantly-replaceable block); left as a known, documented gap rather than an
 * invented API.
 * <p>
 * <b>Not ported: CE's {@code ArmorUtil}-gated {@code randomDisplayTick} cloud particle.</b>
 * {@code com.hbm.handler.ArmorUtil} does not exist in this port yet (an items-area concern) - per
 * {@code docs/phase1/DIGEST_REMAINDER.md} ("items_block_fluid_gas.md"), this block is explicitly
 * pre-approved to ship without it and have the effect backfilled once {@code ArmorUtil} lands.
 */
public abstract class BlockGasBase extends Block {

    protected final float red;
    protected final float green;
    protected final float blue;

    public BlockGasBase(Properties properties, float r, float g, float b) {
        super(properties.randomTicks().noCollission().noOcclusion().replaceable().noLootTable());
        this.red = r;
        this.green = g;
        this.blue = b;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        moveGas(level, pos, random);
    }

    /**
     * CE's {@code updateTick} body (the 1-in-2 chance to attempt a move, first direction then a
     * fallback second direction). Exposed (not inlined into {@link #randomTick}) so
     * {@link BlockGasFlammable} can fall back to plain movement from its own combined
     * random/scheduled tick handler, exactly like CE's {@code super.updateTick(...)} call.
     */
    protected void moveGas(ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(2) == 0) {
            if (!tryMove(level, pos, getFirstDirection(level, pos))) {
                tryMove(level, pos, getSecondDirection(level, pos));
            }
        }
    }

    public abstract Direction getFirstDirection(Level level, BlockPos pos);

    public Direction getSecondDirection(Level level, BlockPos pos) {
        return getFirstDirection(level, pos);
    }

    public boolean tryMove(ServerLevel level, BlockPos pos, Direction dir) {
        BlockPos targetPos = pos.relative(dir);

        if (!level.isLoaded(targetPos)) {
            return false;
        }

        if (level.getBlockState(targetPos).isAir()) {
            level.removeBlock(pos, false);
            level.setBlock(targetPos, this.defaultBlockState(), 2);
            return true;
        }

        return false;
    }

    public int getDelay(LevelAccessor levelAccessor) {
        return 20;
    }

    public Direction randomHorizontal(RandomSource random) {
        return Direction.values()[random.nextInt(4) + 2];
    }
}
