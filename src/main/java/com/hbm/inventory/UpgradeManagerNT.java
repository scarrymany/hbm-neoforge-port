package com.hbm.inventory;

import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.EnumMap;
import java.util.Map;

/**
 * Ported (narrowed) from CE's {@code com.hbm.inventory.UpgradeManagerNT} (89 lines, read in full) -
 * a small, genuinely-missing dependency flagged by {@code docs/phase2/oil_production_chain.md}'s
 * Deferred scope #3 ("every extraction TE in this area uses it for the SPEED/POWER/OVERDRIVE/
 * AFTERBURN upgrade-slot system... only the machine-side manager class that reads those slots is
 * missing"). Narrow enough (89 lines in CE, no other machine family in this port references it yet)
 * that porting it now - rather than stubbing the whole upgrade mechanic - is the lower-risk choice;
 * a future machine family that also needs it can depend on this class directly.
 *
 * <p><b>One deliberate shape change from CE</b>: CE's constructor takes the owning {@code TileEntity}
 * and calls back into a {@code com.hbm.tileentity.IUpgradeInfoProvider} marker interface (for
 * {@code getValidUpgrades()} and a GUI-tooltip {@code provideInfo} callback) that this port has not
 * ported - matching the precedent already set by {@link ItemMachineUpgrade}'s own javadoc, which
 * dropped that same interface's tooltip-lookup half as "a defensive, optional enhancement... not a
 * hard requirement". The tooltip half is dropped here too; the level-capping half ({@code
 * getValidUpgrades()}, i.e. "how high can each upgrade type go on this specific machine") is kept,
 * but as a plain constructor argument instead of an interface callback, so a caller needs no marker
 * interface of its own - just a {@code Map<UpgradeType, Integer>} of per-type level caps.</p>
 *
 * <p>The slot-content-unchanged cache check below intentionally uses {@link java.util.Arrays#equals}
 * over an {@code ItemStack[]}, which (matching CE - vanilla {@link ItemStack} does not override
 * {@code equals}) compares by reference, not by content: the cache only actually invalidates when a
 * slot's stack *instance* changes (a real insert/extract/creative-set), not on every call re-reading
 * the same unchanged stack object - this is CE's own intended caching behavior, not a bug.</p>
 */
public class UpgradeManagerNT {

    private final Map<UpgradeType, Integer> maxLevels;
    private final Map<UpgradeType, Integer> upgrades = new EnumMap<>(UpgradeType.class);
    private ItemStack[] cachedSlots;

    public UpgradeManagerNT(Map<UpgradeType, Integer> maxLevels) {
        this.maxLevels = maxLevels;
    }

    public void checkSlots(IItemHandler inventory, int start, int end) {
        ItemStack[] slots = new ItemStack[end - start + 1];
        for (int i = start; i <= end; i++) slots[i - start] = inventory.getStackInSlot(i);
        checkSlotsInternal(slots);
    }

    private void checkSlotsInternal(ItemStack[] slots) {
        if (java.util.Arrays.equals(slots, cachedSlots)) return;
        cachedSlots = slots.clone();

        upgrades.clear();

        for (ItemStack stack : slots) {
            if (stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemMachineUpgrade upgrade) {
                Integer cap = maxLevels.get(upgrade.getType());
                if (cap == null) continue; // this machine doesn't accept this upgrade type at all

                int levelBefore = upgrades.getOrDefault(upgrade.getType(), 0);
                int level = levelBefore + upgrade.getTier();
                upgrades.put(upgrade.getType(), Math.min(level, cap));
            }
        }
    }

    public int getLevel(UpgradeType type) {
        return upgrades.getOrDefault(type, 0);
    }
}
