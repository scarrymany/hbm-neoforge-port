package com.hbm.items.machine;

import com.hbm.items.ItemBase;

/**
 * Spent arc furnace electrode. Purely an inert byproduct item - CE gave it the same metadata enum
 * as {@link ItemArcElectrode} just to share texture/name plumbing; here it is one plain registered
 * item per grade with no logic beyond identity.
 */
public class ItemArcElectrodeBurnt extends ItemBase {

    private final ItemArcElectrode.EnumElectrodeType type;

    public ItemArcElectrodeBurnt(ItemArcElectrode.EnumElectrodeType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public ItemArcElectrode.EnumElectrodeType getType() {
        return this.type;
    }
}
