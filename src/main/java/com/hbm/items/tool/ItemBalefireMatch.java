package com.hbm.items.tool;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Flint-and-steel analogue that lights a specific ritual/pyre block instead of plain fire. Ported
 * from CE's {@code com.hbm.items.tool.ItemBalefireMatch}.
 *
 * <p><b>Stubbed pending {@code ModBlocks.balefire}</b>: CE's {@code useOn} places
 * {@code ModBlocks.balefire.getDefaultState()} on right-clicking an air block. No balefire block
 * exists anywhere in this port yet ({@code com.hbm.blocks.ModBlocks} is still the Phase 0 registry
 * skeleton) - per the port plan's "stub with a documented TODO rather than blocking" rule, the item
 * itself is registered (so future callers/recipes can reference it) but its place-block behavior is
 * left a no-op {@link InteractionResult#PASS} until that block lands.
 */
public class ItemBalefireMatch extends Item {

    public ItemBalefireMatch(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // TODO(phase2-or-later): once a balefire block exists, port CE's behavior here - on an air
        // block in the clicked face's direction, play SoundEvents.FLINTANDSTEEL_USE, place the
        // balefire block, and damage this stack by 1 (matching ItemMatch's plain-fire equivalent).
        return InteractionResult.PASS;
    }
}
