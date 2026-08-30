package com.hbm.blocks.bomb;

import com.hbm.api.block.IExploder;
import com.hbm.entity.item.EntityTNTPrimedBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Ported from CE's {@code com.hbm.blocks.bomb.BlockDetonatable} (82 lines, read in full) -
 * {@code docs/phase3/bomb_blocks_and_detonators.md} Section A. Base for every "catches fire /
 * chain-detonates instead of vanishing" explosive block: when destroyed by another explosion this
 * spawns {@link EntityTNTPrimedBase} with a short randomized "pop fuse" rather than being removed
 * silently, and never drops loot when destroyed that way.
 * <p>
 * <b>1.12 -&gt; 1.21.1 hook mapping</b> (confirmed against this port's already-committed
 * {@code com.hbm.explosion.vanillant.standard.BlockProcessorStandard}, which documents the exact
 * same delegate chain, and cross-checked for API shape only against Neo Edition's real, compiling
 * {@code com.hbm.blocks.bomb.DetonatableBlock}): CE's {@code Block#onBlockExploded} (called by the
 * explosion engine <em>before</em> the block is actually removed, with vanilla's own default body
 * performing that removal) is replaced one-to-one by {@link #wasExploded}, invoked via
 * {@code BlockState#onBlockExploded} while the target position's {@link BlockState} is still
 * present - this method reads it, removes the block itself, then spawns the primed entity, matching
 * CE's own read-then-super-removes ordering exactly. CE's {@code canDropFromExplosion(Explosion)}
 * becomes the real 1.21.1 {@link #canDropFromExplosion(BlockState, BlockGetter, BlockPos, Explosion)}
 * override (confirmed real shape from Neo Edition's parallel class).
 * <p>
 * <b>Fire-adjacency ignition</b>: CE's {@code BlockFlammable.shouldIgnite} (a manual 6-neighbor
 * {@code Blocks.FIRE} identity scan, gated on {@code flammability != 0} - not vanilla's
 * {@code FlammableBlockRegistry} spread mechanic) is reimplemented directly here rather than
 * pulling in a full flammable-block framework, since no such framework exists yet in this port and
 * the only two real CE subclasses of this class in scope ({@link BlockTNTBase} and its own
 * descendants) fully override {@link #neighborChanged} themselves anyway (CE's
 * {@code BlockPlasticExplosive}, the other direct subclass, is out of this package's scope per the
 * research report footnote - a cosmetic/crafting block with no {@code IBomb} involvement). The
 * {@code encouragement}/{@code flammability} constructor numbers are preserved for documentation
 * and the ignition gate, but are <b>not yet wired</b> into NeoForge's {@code FlammableBlockRegistry}
 * (which would need its own {@code FMLCommonSetupEvent} listener class - a small, clearly separable
 * follow-up, not required for the manual adjacency-ignition path this package's blocks actually use)
 * - so ambient vanilla fire slowly spreading onto these blocks over time (as opposed to fire placed
 * directly adjacent, which the neighbor-changed check below still catches immediately) is a documented
 * gap, not a silent behavior change for anything this package exercises.
 */
public abstract class BlockDetonatable extends Block implements IExploder {

    /** CE fire-spread "encouragement" value - not yet wired to {@code FlammableBlockRegistry}, see class javadoc. */
    protected final int encouragement;
    /** CE fire-spread "flammability" value; also gates {@link #shouldIgnite} (0 = never manually ignites from adjacent fire). */
    protected final int flammability;
    /** Shorter fuse used when this block is caught in someone else's explosion, rather than lighting itself. */
    protected final int popFuse;
    protected final boolean detonateOnCollision;
    protected final boolean detonateOnShot;

    protected BlockDetonatable(Properties properties, int encouragement, int flammability, int popFuse,
            boolean detonateOnCollision, boolean detonateOnShot) {
        super(properties);
        this.encouragement = encouragement;
        this.flammability = flammability;
        this.popFuse = popFuse;
        this.detonateOnCollision = detonateOnCollision;
        this.detonateOnShot = detonateOnShot;
    }

    /**
     * CE: {@code onBlockExploded} (state cached) then {@code super.onBlockExploded} (vanilla default
     * removal). Reads the still-present state, removes the block, then spawns the primed entity -
     * see class javadoc for why the read must happen before removal.
     */
    @Override
    public void wasExploded(Level level, BlockPos pos, @Nullable Explosion explosion) {
        if (level.isClientSide()) return;
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) return;

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        EntityTNTPrimedBase tntPrimed = new EntityTNTPrimedBase(level,
                pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                explosion != null ? explosion.getIndirectSourceEntity() : null, state);

        tntPrimed.fuse = popFuse <= 0 ? 0 : level.random.nextInt(popFuse) + popFuse / 2;
        tntPrimed.detonateOnCollision = detonateOnCollision;

        level.addFreshEntity(tntPrimed);
    }

    @Override
    public boolean canDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return false;
    }

    /**
     * CE's own base-class ignite-by-adjacent-fire path. Dead code for every concrete block this
     * package registers ({@link BlockTNTBase} fully overrides {@link #neighborChanged} with its own
     * redstone-aware logic and never calls {@code super}) - kept for fidelity with CE's class shape
     * and any future direct subclass.
     */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide() && shouldIgnite(level, pos)) {
            wasExploded(level, pos, null);
        }
    }

    /** CE: manual 6-neighbor scan for a literal {@code Blocks.FIRE} state - see class javadoc. */
    protected boolean shouldIgnite(Level level, BlockPos pos) {
        if (flammability == 0) return false;

        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).is(Blocks.FIRE)) {
                return true;
            }
        }

        return false;
    }

    /** CE: weapon-fire instant-detonation hook (only {@code detonateOnShot} blocks react) - no in-scope subclass sets this true yet, ported for the future gun-package hookup. */
    public void onShot(Level level, BlockPos pos) {
        if (!detonateOnShot) return;

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        explodeEntity(level, pos, null);
    }
}
