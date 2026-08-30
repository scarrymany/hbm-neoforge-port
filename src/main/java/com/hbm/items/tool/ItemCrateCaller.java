package com.hbm.items.tool;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Novelty "call in a loot crate" item: randomly drops one of several crate blocks near the player.
 * Ported from CE's {@code com.hbm.items.tool.ItemCrateCaller}.
 *
 * <p><b>Stubbed pending an accessible crate block registration.</b> CE's {@code onItemRightClick}
 * places one of {@code ModBlocks.crate}/{@code crate_weapon}/{@code crate_metal}/{@code crate_lead}/
 * {@code crate_red} at a random nearby ground position. Block <em>classes</em> for this family
 * already exist in this port ({@code com.hbm.blocks.generic.BlockCrate} via
 * {@code GenericCrateBlocks}), but as of this writing {@code GenericCrateBlocks.registerAll()} is
 * never called from {@code ModBlocks.register()} (so no crate block is actually registered at
 * runtime) and the class exposes no public field/accessor for the registered
 * {@code DeferredBlock<BlockCrate>} instances even once it is wired up - see this area's final
 * report for the cross-area follow-up. Per the port plan's "stub with a documented TODO rather than
 * blocking" rule, the item itself is registered now (durability-based charge count preserved) and
 * its place-crate behavior is deferred until a crate block is reachable from this package.
 */
public class ItemCrateCaller extends Item {

    public ItemCrateCaller(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // TODO(cross-area follow-up): once GenericCrateBlocks exposes a registered crate block
        // accessor, port CE's behavior here - damage this stack by 1, pick one of
        // crate/crate_weapon/crate_metal/crate_lead/crate_red with CE's weighted odds
        // (350/1000 weapon, 100/1000 metal, 50/1000 lead, 1/1000 red, else standard), and place it
        // at y=255 within +-15 blocks of the player if that position is air.
        return InteractionResultHolder.pass(stack);
    }
}
