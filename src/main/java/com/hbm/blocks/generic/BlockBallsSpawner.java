package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;
import com.hbm.entity.mob.EntityBOTPrimeHead;
import com.hbm.entity.mob.WormEntityTypes;
import com.hbm.items.special.SpecialItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Direct port of CE's {@code com.hbm.blocks.generic.BlockBallsSpawner} (46 lines, read in full) - see
 * {@code docs/phase4/entities_bosses.md}'s worm-boss table (spawn mechanism #2). A right-click-with-
 * {@code mech_key} single-use structure-summon block: right-clicking with {@link SpecialItems#MECH_KEY}
 * held consumes the key, spawns {@link EntityBOTPrimeHead} at Y=300 above the block with a controlled
 * {@code -1.0} Y-velocity fall-in entrance, and turns the block into
 * {@link BallsSpawnerBlocks#BALLS_SPAWNER_SPENT} (cannot be re-triggered).
 * <p>
 * CE's real placement path for this block is the {@code JungleDungeon} world-gen structure - explicitly
 * out of this package's scope per the research report's own Open questions (exact spawn rarity/biome
 * gate never confirmed, {@code CellularDungeon} not read) and this package's task brief. This block is
 * registered as a plain placeable/obtainable block instead (see {@link BallsSpawnerBlocks}) so the
 * right-click-summon mechanic itself is real and testable without the dungeon generator; the "used"
 * replacement block is this port's own {@code balls_spawner_spent} rather than CE's dungeon-specific
 * {@code brick_jungle_cracked} (which belongs to the deferred {@code JungleDungeon} block set and does
 * not exist in this port).
 */
public class BlockBallsSpawner extends BlockBase {

    public BlockBallsSpawner(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() != SpecialItems.MECH_KEY.get()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide()) {
            stack.shrink(1);

            EntityBOTPrimeHead head = new EntityBOTPrimeHead(WormEntityTypes.BOTPRIME_HEAD.get(), level);
            head.setPos(pos.getX() + 0.5D, 300, pos.getZ() + 0.5D);
            head.setDeltaMovement(0.0D, -1.0D, 0.0D);
            head.spawnBody();
            level.addFreshEntity(head);

            level.setBlock(pos, BallsSpawnerBlocks.BALLS_SPAWNER_SPENT.get().defaultBlockState(), 3);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
