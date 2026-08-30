package com.hbm.items.special;

import net.minecraft.world.item.Item;

/**
 * Port of CE's {@code ItemClayTablet} ({@code clay_tablet}): right-clicking assigned a random world-
 * generation seed (used by CE's {@code GUIScreenClayTablet} to render a deterministic pattern) and
 * opened that GUI. Per docs/phase1/items_special.md finding 3, no menu/screen framework has been
 * ported yet - the item shell registers now; {@link SpecialItemComponents#TABLET_SEED} is the
 * NBT->component replacement for CE's {@code tabletSeed} long, ready for whichever future pass wires
 * up the actual seed assignment and screen once a menu framework exists. No {@code use()} override
 * is added here since assigning the seed only makes sense paired with actually opening the GUI that
 * reads it back.
 */
public class ItemClayTablet extends Item {

    public ItemClayTablet(Properties properties) {
        super(properties);
    }
}
