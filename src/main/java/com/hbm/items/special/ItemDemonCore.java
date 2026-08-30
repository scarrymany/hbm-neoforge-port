package com.hbm.items.special;

import com.hbm.items.ItemBase;

/**
 * Port of CE's {@code ItemDemonCore}. CE's own version ships with its {@code onEntityItemUpdate}/
 * {@code addInformation} overrides fully commented out (see {@code upstream/hbm-ce/.../
 * ItemDemonCore.java}) - the open-core-closes-on-drop behavior and its radiation hazard instead
 * live entirely in CE's {@code HazardRegistry} via a custom {@code HazardTypeDangerousDrop} entry,
 * which this port carries over faithfully in {@code HazardRegistry.registerItems()} (Pattern F, see
 * docs/phase1/hazard_bindings_plan.md section 2). This class itself is therefore, faithfully, an
 * empty marker subclass backing {@code demon_core_open}.
 */
public class ItemDemonCore extends ItemBase {

    public ItemDemonCore(Properties properties) {
        super(properties);
    }
}
