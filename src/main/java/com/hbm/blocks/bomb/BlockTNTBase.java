package com.hbm.blocks.bomb;

import com.hbm.api.block.IToolable;
import com.hbm.entity.item.EntityTNTPrimedBase;
import com.hbm.util.ChatBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * Ported from CE's {@code com.hbm.blocks.bomb.BlockTNTBase} (139 lines, read in full) -
 * {@code docs/phase3/bomb_blocks_and_detonators.md} Section A. Redstone power and a burning arrow
 * both always force-prime this block (spawning {@link EntityTNTPrimedBase} regardless of the
 * "ignite on break" toggle); ambient adjacent fire and manual player mining both respect that same
 * toggle (off by default) via {@link #prime}. {@link #onScrew} gives every subclass generic
 * defuser/screwdriver dispatch for free via this port's already-committed {@code IToolable}/
 * {@code ItemTooling} chain.
 * <p>
 * CE's {@code META} int property (0/1 = "ignite on break" off/on) is a real per-placement
 * {@link BlockState} toggle, not a metadata content variant - so per this port's flattening ground
 * rule (which only applies to distinct-content damage values) it survives as a genuine
 * {@link BooleanProperty} ({@link #IGNITE_ON_BREAK}) rather than becoming separate registry entries,
 * exactly like {@code BlockGrate}'s own placement-derived {@code HEIGHT} property.
 */
public abstract class BlockTNTBase extends BlockDetonatable implements IToolable {

    public static final BooleanProperty IGNITE_ON_BREAK = BooleanProperty.create("ignite_on_break");

    protected BlockTNTBase(Properties properties) {
        super(properties, 15, 100, 20, false, false);
        registerDefaultState(this.stateDefinition.any().setValue(IGNITE_ON_BREAK, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(IGNITE_ON_BREAK);
    }

    /** CE: {@code onBlockAdded} - only reacts once, at initial placement (mirrors 1.12's own semantics). */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (oldState.is(this)) return;
        reactToNeighbors(level, pos);
    }

    /** CE: {@code neighborChanged} - redstone power always force-primes; otherwise fall back to the fire-adjacency check. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        reactToNeighbors(level, pos);
    }

    private void reactToNeighbors(Level level, BlockPos pos) {
        if (level.isClientSide()) return;

        if (level.hasNeighborSignal(pos)) {
            prime(level, pos, true, null);
        } else {
            checkAndIgnite(level, pos);
        }
    }

    /** CE: {@code checkAndIgnite} + {@code onPlayerDestroy} - fire-adjacency always removes the block, but only spawns a primed entity if the toggle is on. */
    private void checkAndIgnite(Level level, BlockPos pos) {
        if (shouldIgnite(level, pos)) {
            prime(level, pos, false, null);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** CE: {@code onPlayerDestroy} - manually mining this block respects the "ignite on break" toggle, same as ambient fire adjacency. */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        super.playerWillDestroy(level, pos, state, player);
        prime(level, pos, false, player);
    }

    /**
     * CE: {@code prime}. Spawns the primed entity if {@code forceIgnite} is set or the block's own
     * {@link #IGNITE_ON_BREAK} flag is on; does not itself remove the block - every call site
     * decides removal separately, matching CE's own split (redstone/flint/arrow paths remove
     * immediately after calling this, {@link #playerWillDestroy} lets the ordinary break sequence
     * remove it).
     */
    protected void prime(Level level, BlockPos pos, boolean forceIgnite, @Nullable LivingEntity placer) {
        if (level.isClientSide()) return;
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) return;
        if (!forceIgnite && !state.getValue(IGNITE_ON_BREAK)) return;

        EntityTNTPrimedBase tnt = new EntityTNTPrimedBase(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, placer, state);
        level.addFreshEntity(tnt);
        level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** CE: {@code onBlockActivated}'s {@code Items.FLINT_AND_STEEL} branch - forces ignition regardless of the toggle. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof FlintAndSteelItem) {
            if (!level.isClientSide()) {
                prime(level, pos, true, player);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * CE: {@code onEntityCollision}'s burning-{@code EntityArrow} branch. {@code entityInside} is
     * this port's confirmed real 1.21.1 per-tick entity/block-overlap hook (there is no direct
     * "arrow stuck in this exact block" event); reliability against a literal arrow-embed may differ
     * slightly from 1.12's own collision timing, a reasonable-effort port rather than a hard stub.
     */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide() && entity instanceof AbstractArrow arrow && arrow.isOnFire()) {
            LivingEntity shooter = arrow.getOwner() instanceof LivingEntity living ? living : null;
            prime(level, pos, true, shooter);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** CE: {@code onScrew} - DEFUSER unconditionally dismantles-and-redrops a clean copy; SCREWDRIVER toggles {@link #IGNITE_ON_BREAK}. */
    @Override
    public boolean onScrew(Level level, Player player, int x, int y, int z, Direction side, float fX, float fY, float fZ,
            InteractionHand hand, ToolType tool) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);

        if (tool == ToolType.DEFUSER) {
            if (!level.isClientSide()) {
                level.destroyBlock(pos, false);
                Block.popResource(level, pos, new ItemStack(this.asItem()));
            }
            return true;
        }

        if (tool != ToolType.SCREWDRIVER) return false;

        if (!level.isClientSide()) {
            boolean next = !state.getValue(IGNITE_ON_BREAK);
            level.setBlock(pos, state.setValue(IGNITE_ON_BREAK, next), 3);

            if (next) {
                player.sendSystemMessage(ChatBuilder.start("[ Ignite On Break: Enabled ]").color(ChatFormatting.RED).flush());
            } else {
                player.sendSystemMessage(ChatBuilder.start("[ Ignite On Break: Disabled ]").color(ChatFormatting.GOLD).flush());
            }
        }
        return true;
    }
}
