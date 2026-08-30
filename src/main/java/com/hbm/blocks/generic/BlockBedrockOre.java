package com.hbm.blocks.generic;

import com.hbm.api.block.IDrillInteraction;
import com.hbm.api.block.IMiningDrill;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * Ported from CE's {@code BlockBedrockOre}: an unbreakable-by-hand bedrock-tier ore, mineable only
 * through {@link IDrillInteraction}. CE registered a single instance ({@code ore_bedrock_coltan})
 * and picked its drop item by identity-checking {@code this == ModBlocks.ore_bedrock_coltan}; since
 * this class only ever backs that one block, the port bakes the drop item straight into the
 * constructor instead of re-deriving it from a field-identity check. The drop is threaded in as a
 * {@link Supplier} (matching {@code OreBlocks}'s own {@code IOreType} convention) rather than a
 * resolved {@link Item}, since the registration call site needs to reference another
 * {@code DeferredItem}'s {@code .get()} lazily - items and blocks are separate
 * {@code DeferredRegister}s, and calling {@code .get()} eagerly at block-construction time (during
 * the blocks registry's own {@code RegisterEvent}) is not guaranteed safe.
 */
public class BlockBedrockOre extends Block implements IDrillInteraction {

    private final Supplier<Item> drillDrop;

    public BlockBedrockOre(Properties properties, Supplier<Item> drillDrop) {
        super(properties);
        this.drillDrop = drillDrop;
    }

    @Override
    public boolean canBreak(Level world, int x, int y, int z, BlockState state, IMiningDrill drill) {
        return drill.getDrillRating() > 70;
    }

    @Override
    public ItemStack extractResource(Level world, int x, int y, int z, BlockState state, IMiningDrill drill) {
        if (drill.getDrillRating() > 70) {
            return ItemStack.EMPTY;
        }
        return world.getRandom().nextInt(50) == 0 ? new ItemStack(drillDrop.get()) : ItemStack.EMPTY;
    }

    @Override
    public float getRelativeHardness(Level world, int x, int y, int z, BlockState state, IMiningDrill drill) {
        return 30;
    }
}
