package com.hbm.module;

import com.hbm.util.ItemStackUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * NeoForge port of CE's {@code ModulePatternMatcher} - filter matching logic for crane machines.
 * Simplified without bedrock-ore mode (CE-specific). Three main modes remain:
 * - EXACT: item + components must match
 * - WILDCARD: only item type must match
 * - OreDict: tag-based matching (CE's ore-dictionary equivalent)
 */
public class ModulePatternMatcher {

    public static final String MODE_EXACT = "exact";
    public static final String MODE_WILDCARD = "wildcard";
    public String[] modes;

    public ModulePatternMatcher() {
        this.modes = new String[1];
    }

    public ModulePatternMatcher(int count) {
        this.modes = new String[count];
    }

    /**
     * Smart pattern initialization: tries to detect ore-dict tags (ingot, block, dust, etc.)
     * and falls back to exact/wildcard based on item type.
     */
    public void initPatternSmart(Level world, ItemStack stack, int i) {
        if (world.isClientSide) return;

        if (stack == null || stack.isEmpty()) {
            modes[i] = null;
            return;
        }

        List<String> names = ItemStackUtil.getOreDictNames(stack);

        if (iterateAndCheck(names, i, "ingot")) return;
        if (iterateAndCheck(names, i, "block")) return;
        if (iterateAndCheck(names, i, "dust")) return;
        if (iterateAndCheck(names, i, "nugget")) return;
        if (iterateAndCheck(names, i, "plate")) return;

        // CE: bedrock-ore check omitted (CE-specific item type)
        // Default: check if item has variants (CE used hasSubtypes, 1.21 uses components)
        if (hasVariants(stack)) {
            modes[i] = MODE_EXACT;
        } else {
            modes[i] = MODE_WILDCARD;
        }
    }

    private boolean iterateAndCheck(List<String> names, int i, String prefix) {
        for (String s : names) {
            if (s.startsWith(prefix)) {
                modes[i] = s;
                return true;
            }
        }
        return false;
    }

    /**
     * Standard pattern initialization: no ore-dict detection, just exact vs wildcard.
     */
    public void initPatternStandard(Level world, ItemStack stack, int i) {
        if (world.isClientSide) return;

        if (stack == null || stack.isEmpty()) {
            modes[i] = null;
            return;
        }

        if (hasVariants(stack)) {
            modes[i] = MODE_EXACT;
        } else {
            modes[i] = MODE_WILDCARD;
        }
    }

    /**
     * Cycles through available modes when user right-clicks filter slot.
     * CE order: EXACT → BEDROCK → WILDCARD → oreDict[0] → oreDict[1] → ... → EXACT
     * Ported order: EXACT → WILDCARD → oreDict[0] → oreDict[1] → ... → EXACT
     */
    public void nextMode(Level world, ItemStack pattern, int i) {
        if (world.isClientSide) return;

        if (pattern == null || pattern.isEmpty()) {
            modes[i] = null;
            return;
        }

        if (modes[i] == null) {
            modes[i] = MODE_EXACT;
        } else if (MODE_EXACT.equals(modes[i])) {
            // CE: bedrock mode omitted
            modes[i] = MODE_WILDCARD;
        } else if (MODE_WILDCARD.equals(modes[i])) {
            List<String> names = ItemStackUtil.getOreDictNames(pattern);

            if (names.isEmpty()) {
                modes[i] = MODE_EXACT;
            } else {
                modes[i] = names.get(0);
            }
        } else {
            // Currently in oreDict mode, cycle to next or back to EXACT
            List<String> names = ItemStackUtil.getOreDictNames(pattern);

            if (names.size() < 2 || modes[i].equals(names.get(names.size() - 1))) {
                modes[i] = MODE_EXACT;
            } else {
                for (int j = 0; j < names.size() - 1; j++) {
                    if (modes[i].equals(names.get(j))) {
                        modes[i] = names.get(j + 1);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Check if input stack matches filter stack according to the mode.
     * CE logic preserved: EXACT checks item identity + components (CE: tags),
     * WILDCARD checks only item type, oreDict checks tag presence.
     */
    public boolean isValidForFilter(ItemStack filter, int index, ItemStack input) {
        String mode = modes[index];

        if (mode == null) {
            modes[index] = mode = MODE_EXACT;
        }

        switch (mode) {
            case MODE_EXACT:
                return ItemStack.isSameItemSameComponents(input, filter);
            case MODE_WILDCARD:
                return input.getItem() == filter.getItem();
            default:
                // OreDict mode: check if input has the tag
                List<String> keys = ItemStackUtil.getOreDictNames(input);
                return keys.contains(mode);
        }
    }

    /**
     * 1.21 heuristic for "has variants": check if stack has any non-empty data components.
     * CE used hasSubtypes(); 1.21 has no direct equivalent.
     */
    private boolean hasVariants(ItemStack stack) {
        return !stack.getComponents().isEmpty();
    }

    public void readFromNBT(CompoundTag nbt) {
        for (int i = 0; i < modes.length; i++) {
            if (nbt.contains("mode" + i)) {
                modes[i] = nbt.getString("mode" + i);
            } else {
                modes[i] = null;
            }
        }
    }

    public void writeToNBT(CompoundTag nbt) {
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] != null) {
                nbt.putString("mode" + i, modes[i]);
            }
        }
    }

    /**
     * User-facing label for GUI tooltips.
     */
    public static String getLabel(String mode) {
        switch (mode) {
            case MODE_EXACT:
                return "§eItem and components match";
            case MODE_WILDCARD:
                return "§eItem matches";
            default:
                return "§eTag matches: " + mode;
        }
    }
}
