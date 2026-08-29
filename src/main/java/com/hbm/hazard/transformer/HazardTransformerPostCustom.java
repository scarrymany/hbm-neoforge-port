package com.hbm.hazard.transformer;

import com.hbm.hazard.HazardEntry;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.main.MainRegistry;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Generic addon-facing extension point: a per-item multiplier and post-transform functions, plus per-stack-key
 * (item/meta, or item/meta+custom-data) post-transform functions. Always kept last in
 * {@link com.hbm.hazard.HazardSystem#trafos}.
 * <p>
 * The NBT-sensitive stack key uses the vanilla {@link DataComponents#CUSTOM_DATA} component instead of reading
 * {@code ItemStack} NBT directly, since raw NBT compound reads on an {@code ItemStack} are no longer legal in 1.21.
 */
public class HazardTransformerPostCustom implements IHazardTransformer {

    private static final Object2DoubleOpenHashMap<Item> ITEM_MULTIPLIERS = new Object2DoubleOpenHashMap<>();
    private static final Map<Item, ObjectArrayList<BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>>>> ITEM_POST = new Object2ObjectOpenHashMap<>();
    private static final Map<StackKey, ObjectArrayList<BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>>>> STACK_POST = new Object2ObjectOpenHashMap<>();

    static {
        ITEM_MULTIPLIERS.defaultReturnValue(1.0);
    }

    private static List<HazardEntry> safeApply(final BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>> fn, final ItemStack stack, final List<HazardEntry> input) {
        try {
            return fn.apply(stack, Collections.unmodifiableList(input));
        } catch (Throwable t) {
            MainRegistry.logger.debug("PostTransformer exception", t);
            return input;
        }
    }

    public static void addItemPost(final Item item, final BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>> fn) {
        ITEM_POST.computeIfAbsent(item, k -> new ObjectArrayList<>()).add(fn);
    }

    public static void removeItemPost(final Item item, final BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>> fn) {
        final ObjectArrayList<BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>>> list = ITEM_POST.get(item);
        if (list != null) {
            list.remove(fn);
            if (list.isEmpty()) ITEM_POST.remove(item);
        }
    }

    public static void addStackPost(final StackKey key, final BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>> fn) {
        STACK_POST.computeIfAbsent(key, k -> new ObjectArrayList<>()).add(fn);
    }

    public static void removeStackPost(final StackKey key, final BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>> fn) {
        final ObjectArrayList<BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>>> list = STACK_POST.get(key);
        if (list != null) {
            list.remove(fn);
            if (list.isEmpty()) STACK_POST.remove(key);
        }
    }

    public static void setItemMultiplier(final Item item, final double multiplier) {
        ITEM_MULTIPLIERS.put(item, multiplier);
    }

    public static void clearItemMultiplier(final Item item) {
        ITEM_MULTIPLIERS.removeDouble(item);
    }

    public static boolean hasItemMultiplier(final Item item) {
        return ITEM_MULTIPLIERS.containsKey(item);
    }

    public static double getItemMultiplier(final Item item) {
        return ITEM_MULTIPLIERS.getDouble(item);
    }

    private static void applyStackPostList(final StackKey key, final ItemStack stack, final List<HazardEntry> entries) {
        final List<BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>>> stackFns = STACK_POST.get(key);
        if (stackFns == null || stackFns.isEmpty()) return;

        List<HazardEntry> current = entries;
        for (final BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>> fn : stackFns) {
            final List<HazardEntry> next = safeApply(fn, stack, current);
            if (next != null) current = next;
        }
        if (current != entries) {
            entries.clear();
            entries.addAll(current);
        }
    }

    @Override
    public void transformPre(final ItemStack stack, final List<HazardEntry> entries) {
    }

    @Override
    public void transformPost(final ItemStack stack, final List<HazardEntry> entries) {
        if (stack == null || stack.isEmpty()) return;

        final Item item = stack.getItem();
        final boolean hasItemLevel = ITEM_MULTIPLIERS.containsKey(item) || ITEM_POST.containsKey(item);
        StackKey genericKey = null, nbtKey = null;
        if (!hasItemLevel) {
            genericKey = StackKey.of(stack, false);
            if (!STACK_POST.containsKey(genericKey)) {
                nbtKey = StackKey.of(stack, true);
                if (!STACK_POST.containsKey(nbtKey)) return;
            }
        }

        // 1) Apply item-wide multiplier, if present
        if (ITEM_MULTIPLIERS.containsKey(item)) {
            double mult = ITEM_MULTIPLIERS.getDouble(item);
            if (Double.isNaN(mult)) mult = 1.0;
            if (Math.abs(mult - 1.0) > 1e-6) {
                final List<HazardEntry> scaled = new ArrayList<>(entries.size());
                for (final HazardEntry e : entries) {
                    if (e != null) scaled.add(e.clone(mult));
                }
                entries.clear();
                entries.addAll(scaled);
            }
        }

        // 2) Apply item-level post transforms (NBT-agnostic)
        final List<BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>>> itemFns = ITEM_POST.get(item);
        if (itemFns != null && !itemFns.isEmpty()) {
            List<HazardEntry> current = entries;
            for (final BiFunction<ItemStack, List<HazardEntry>, List<HazardEntry>> fn : itemFns) {
                final List<HazardEntry> next = safeApply(fn, stack, current);
                if (next != null) current = next;
            }
            if (current != entries) {
                entries.clear();
                entries.addAll(current);
            }
        }

        // 3) Apply stack-level post transforms: generic (meta-only) first, then custom-data sensitive.
        if (genericKey == null) genericKey = StackKey.of(stack, false);
        applyStackPostList(genericKey, stack, entries);
        if (nbtKey == null) nbtKey = StackKey.of(stack, true);
        applyStackPostList(nbtKey, stack, entries);
    }

    public record StackKey(ComparableStack base, CompoundTag customData) {

        public static StackKey of(final ItemStack stack, final boolean respectCustomData) {
            final ComparableStack cs = new ComparableStack(stack).makeSingular();
            CompoundTag tag = null;
            if (respectCustomData) {
                final CustomData data = stack.get(DataComponents.CUSTOM_DATA);
                if (data != null && !data.isEmpty()) {
                    tag = data.copyTag();
                }
            }
            return new StackKey(cs, tag);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (!(o instanceof StackKey that)) return false;
            if (!base.equals(that.base)) return false;
            if (customData == null && that.customData == null) return true;
            if (customData == null || that.customData == null) return false;
            return customData.equals(that.customData);
        }

        @Override
        public int hashCode() {
            int h = base.hashCode();
            if (customData != null) {
                h = 31 * h + customData.hashCode();
            }
            return h;
        }
    }
}
