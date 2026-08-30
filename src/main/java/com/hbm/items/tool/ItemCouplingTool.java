package com.hbm.items.tool;

import com.hbm.items.ItemBase;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemCouplingTool} - an intentionally empty marker
 * item, checked elsewhere by identity/{@code instanceof}. CE's own comment on this class notes its
 * real behavior lives outside the class itself; no such consumer exists in this port yet (it would
 * belong to whichever area implements the mechanism that checks for this item), so this is a plain,
 * inert registration until that consumer is ported.
 */
public class ItemCouplingTool extends ItemBase {

    public ItemCouplingTool(Properties properties) {
        super(properties);
    }
}
