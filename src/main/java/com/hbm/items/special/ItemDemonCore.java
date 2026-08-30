package com.hbm.items.special;

import com.hbm.items.ItemBase;

/**
 * Port of CE's {@code ItemDemonCore}. CE's own version ships with its {@code onEntityItemUpdate}/
 * {@code addInformation} overrides fully commented out (see {@code upstream/hbm-ce/.../
 * ItemDemonCore.java}) - the open-core-closes-on-drop behavior and its radiation hazard instead
 * live entirely in CE's {@code HazardRegistry} via a custom {@code HazardTypeDangerousDrop} entry
 * keyed to {@code demon_core_open}/{@code demon_core_closed} (Pattern F, see
 * docs/phase1/hazard_bindings_plan.md section 2).
 * <p>
 * That {@code HazardTypeDangerousDrop} binding now lives in
 * {@code HazardRegistry.registerItems()} (radiation 5F on {@code demon_core_open}, a drop handler
 * that swaps the dropped entity's stack to {@code demon_core_closed}, and radiation 100000F on
 * {@code demon_core_closed}). This class itself is, faithfully to CE's commented-out overrides, an
 * empty marker subclass backing {@code demon_core_open}; all of its real behavior arrives via that
 * hazard binding, not an override here. CE's paired screwdriver-item respawn on drop is not carried
 * over - no {@code ItemTooling}/screwdriver item exists anywhere in this port yet.
 */
public class ItemDemonCore extends ItemBase {

    public ItemDemonCore(Properties properties) {
        super(properties);
    }
}
