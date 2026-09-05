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
 * {@code HazardRegistry.registerItems()} (Exact CE {@code :191-196}: radiation 5F on
 * {@code demon_core_open}, ground-drop swaps the stack to {@code demon_core_closed} and spawns
 * {@code screwdriver}, radiation 100000F on {@code demon_core_closed}). This class itself is,
 * faithfully to CE's commented-out overrides, an empty marker subclass backing
 * {@code demon_core_open}; all of its real behavior arrives via that hazard binding.
 */
public class ItemDemonCore extends ItemBase {

    public ItemDemonCore(Properties properties) {
        super(properties);
    }
}
