package com.hbm.blocks.generic;

import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.potion.HbmPotionEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Ported from CE's {@code com.hbm.blocks.generic.BlockFallout} (131 lines, read in full) - the thin
 * "ash" carpet {@link com.hbm.entity.effect.EntityFalloutRain} scatters onto exposed surfaces near
 * ground zero (CE field name {@code ModBlocks.fallout}, registry name {@code "fallout"}).
 * <p>
 * <b>Not the same class as this port's {@code BlockHazardFalling}</b>, despite that class's own
 * javadoc naming {@code block_fallout} as one of its two real CE uses - that is a real but entirely
 * different CE block ({@code ModBlocks.block_fallout}, a {@code BlockFalling}-derived radioactive
 * sand deposit alongside {@code block_yellowcake}/{@code ash_digamma}/{@code sand_uranium}, CE
 * {@code ModBlocks.java:609}). CE's real {@code BlockFallout} (this class) extends plain {@code
 * Block} directly and hand-rolls its own sand-like placement/neighbor-update checks
 * ({@code canPlaceBlockAt}/{@code neighborChanged}) rather than delegating to {@code BlockFalling} -
 * confirmed by reading CE's actual {@code BlockFallout.java} source, not just the task's own
 * shorthand description of it. Docs/phase4/fallout_rain_and_effects.md's own "Key design/API
 * decisions" section repeats the {@code BlockHazardFalling} conflation; this port implements the
 * real CE class hierarchy instead.
 * <p>
 * <b>Simplifications versus CE</b> (none behavior-critical - see class discussion): (1) CE's 0-6
 * {@code META} property (a self-stacking depth counter never actually driven above 0 by any real CE
 * placement call site, since every {@code new EntityFalloutRain}/{@code EntityWastePearl}/
 * {@code BlockGasRadonDense} placement uses {@code getDefaultState()}) is dropped - a single default
 * state, matching this port's established "cosmetic CE metadata not reflected in gameplay" precedent
 * (e.g. {@code PlantBlocks}' own waste-block note); (2) CE's {@code getItemDropped} returning a
 * separate standalone {@code ModItems.fallout} material item (also used in a "2 fallout item -> 1
 * fallout block" crafting recipe, {@code MineralRecipes.java:104}, out of this package's scope) is
 * collapsed into this port's standard block+matching-{@code BlockItem} pattern instead - see
 * {@code FalloutBlocks} javadoc; (3) CE's {@code setSoundType(SoundType.GROUND)} (a 1.12 sound type
 * with no direct 1.21 namesake) maps to {@link net.minecraft.world.level.block.SoundType#GRAVEL},
 * the closest vanilla "loose earthy debris" analogue; (4) {@code isReplaceable}/{@code
 * ContaminationUtil.isRadImmune}-gated {@code canEntitySpawn} are not ported (mob-spawn suppression
 * on ash and "anything can silently overwrite this block on placement" are both minor, non-blocking
 * conveniences - flagged as a known gap rather than guessed at with an unconfirmed modern API shape).
 * <p>
 * What IS ported faithfully: the self-perpetuating {@code updateTick}/{@code scheduleUpdate} chain
 * (10-40 tick random interval, matching {@code level.scheduleTick(pos, this, 10 + rand.nextInt(30))}
 * on both placement and every subsequent tick), the exact {@code
 * ChunkRadiationManager.proxy.incrementRad(level, pos, 1, 100)} 4-argument call (a new, real call
 * site for {@code docs/phase4/chunk_radiation_system.md} to add to its list), and the unconditional
 * (no armor/hazmat check at this layer - that lives inside {@code RadiationEffect}'s own tick)
 * {@code HbmPotionEffects.RADIATION} application on step-on, 2400 ticks / amplifier 14.
 */
public class BlockFallout extends Block {

    /** CE's {@code getBoundingBox}: a thin 0-0.125-high carpet, not a full cube. */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 2, 16);

    public BlockFallout(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 10 + level.getRandom().nextInt(30));
        }
    }

    /** CE {@code updateTick}: re-arms itself every cycle, incrementing chunk radiation each time. */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        ChunkRadiationManager.proxy.incrementRad(level, pos, 1, 100);
        level.scheduleTick(pos, this, 10 + random.nextInt(30));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * CE {@code onEntityWalk}: unconditional radiation potion on step-on - {@code RadiationEffect}'s
     * own periodic tick (not this block) is where armor/hazmat/creative checks apply, matching
     * CE's real {@code HbmPotion.radiation.performEffect} chain (see class javadoc).
     */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(HbmPotionEffects.RADIATION, 2 * 60 * 20, 14));
        }
        super.stepOn(level, pos, state, entity);
    }

    /**
     * CE {@code canPlaceBlockAt}: not on ice, and only atop leaves or an opaque/movement-blocking
     * surface. The {@code (state.getValue(META) & 7) == 7} self-stacking branch is dropped along
     * with the {@code META} property itself (see class javadoc); {@code state.isOpaqueCube() &&
     * state.getMaterial().blocksMovement()} is approximated with {@link BlockState#isFaceSturdy}
     * (the modern "solid enough to stand on" proxy already used by this port's {@code Landmine}).
     */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        Block block = belowState.getBlock();
        if (block == Blocks.ICE || block == Blocks.PACKED_ICE) return false;
        return belowState.is(BlockTags.LEAVES) || belowState.isFaceSturdy(level, below, Direction.UP);
    }

    /** CE {@code neighborChanged}: silently vanishes (no drop) once its support is gone. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide() && !state.canSurvive(level, pos)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return true;
    }
}
