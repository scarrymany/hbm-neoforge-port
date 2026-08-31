package com.hbm.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * NBT key -> Data Component mapping for this file (see the Phase 0 hard rule on ItemStack NBT):
 * <ul>
 *     <li>{@code addTooltipToStack}'s {@code display.Lore} tag is now stashed inside
 *     {@code minecraft:custom_data} via {@link TagsUtil}, keeping CE's original key shape. Vanilla's
 *     own {@code DataComponents.LORE} ({@link net.minecraft.world.item.component.ItemLore}) is the
 *     more idiomatic target for a real tooltip and should replace this once the tooltip-rendering call
 *     sites are ported - flagged here rather than switched to now because that touches how every
 *     caller of this method reads the lore back, which is outside this area's scope.</li>
 *     <li>{@code addStacksToNBT}/{@code readStacksFromNBT}'s slotted {@code items} tag is kept inside
 *     {@code minecraft:custom_data} under the same key. Vanilla's {@code DataComponents.CONTAINER}
 *     ({@code ItemContainerContents}) looks like a plausible native replacement for a slotted stack
 *     list, but this port has no verified working example of its exact API surface (Neo Edition does
 *     not use it either), so it is not used here rather than guessed at - a real
 *     {@code DataComponentType} for this is flagged as follow-up design work per the Phase 0 research
 *     report.</li>
 * </ul>
 * CE's ore-dictionary and metadata-based helpers ({@code getOreDictNames}, the {@code meta} overloads
 * of {@code itemStackFrom}) have no 1.21 equivalent (metadata and {@code OreDictionary} are both gone,
 * replaced by data components and the tag system respectively) and are adapted accordingly below.
 * {@code comparableStackFrom(...)} is dropped entirely: it built
 * {@code com.hbm.inventory.RecipesCommon.ComparableStack}, which lives in the inventory/recipe area
 * (out of this area's scope) and has not been redesigned for a world without item metadata yet: adding
 * an equivalent helper here would mean guessing at that type's future constructor shape.
 */
public class ItemStackUtil {

    public static ItemStack carefulCopy(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        return stack.copy();
    }

    /**
     * Creates a new array that only contains the copied range.
     */
    @NotNull
    public static ItemStack[] carefulCopyArrayTruncate(@NotNull final IItemHandler inv, final int start, final int end) {
        if (end < start) {
            throw new IllegalArgumentException("end must be >= start");
        }

        final int length = end - start + 1;
        final ItemStack[] copy = new ItemStack[length];
        for (int idx = 0; idx < length; idx++) {
            copy[idx] = carefulCopy(inv.getStackInSlot(start + idx));
        }

        return copy;
    }

    public static ItemStack carefulCopyWithSize(final ItemStack stack, final int size) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;

        final ItemStack copy = stack.copy();
        copy.setCount(size);
        return copy;
    }

    /**
     * Runs carefulCopy over the entire ItemStack array.
     */
    public static ItemStack[] carefulCopyArray(final ItemStack[] array) {
        return carefulCopyArray(array, 0, array.length - 1);
    }

    /**
     * Recreates the ItemStack array and only runs carefulCopy over the supplied range. All other fields remain null.
     */
    public static ItemStack[] carefulCopyArray(final ItemStack[] array, final int start, final int end) {
        if (array == null) return null;

        final ItemStack[] copy = new ItemStack[array.length];

        for (int i = start; i <= end; i++) {
            copy[i] = carefulCopy(array[i]);
        }

        return copy;
    }

    /**
     * Creates a new array that only contains the copied range.
     */
    public static ItemStack[] carefulCopyArrayTruncate(final ItemStack[] array, final int start, final int end) {
        if (array == null) return null;

        final int length = end - start + 1;
        final ItemStack[] copy = new ItemStack[length];

        for (int i = 0; i < length; i++) {
            copy[i] = carefulCopy(array[start + i]);
        }

        return copy;
    }

    /**
     * UNSAFE! Will ignore all existing lore and override it! In its current state, only fit for items
     * we know don't have any existing lore! Will, however, respect existing custom data.
     */
    public static ItemStack addTooltipToStack(final ItemStack stack, final String... lines) {
        final CompoundTag tag = TagsUtil.getCustomData(stack);
        final CompoundTag display = new CompoundTag();
        final ListTag lore = new ListTag();

        for (final String line : lines) {
            lore.add(StringTag.valueOf("§r§7" + line));
        }

        display.put("Lore", lore);
        tag.put("display", display);
        TagsUtil.putCustomData(stack, tag);

        return stack;
    }

    /**
     * Automatically adds multistack labels for displays that use a ton of items (like construction recipe handlers).
     */
    public static ItemStack addStackSizeLabel(ItemStack stack) {

        if (stack.getCount() > 64) {
            int stacks = stack.getCount() / 64;
            int items = stack.getCount() % 64;
            addTooltipToStack(stack, "§c" + stacks + "x64" + (items > 0 ? (" + " + items) : ""));
        }

        return stack;
    }

    public static void addNBTFromString(ItemStack stack, String nbt) {
        try {
            Tag parsed = TagParser.parseTag(nbt);
            if (parsed instanceof CompoundTag compound) {
                TagsUtil.putCustomData(stack, compound);
            }
        } catch (Exception ignored) {
        }
    }

    public static void addStacksToNBT(final HolderLookup.Provider provider, final ItemStack stack, final ItemStack... stacks) {

        final CompoundTag tag = TagsUtil.getCustomData(stack);
        final ListTag items = new ListTag();

        for (int i = 0; i < stacks.length; i++) {
            if (stacks[i] != null && !stacks[i].isEmpty()) {
                final CompoundTag slotNBT = new CompoundTag();
                slotNBT.putByte("slot", (byte) i);
                stacks[i].save(provider, slotNBT);
                items.add(slotNBT);
            }
        }
        tag.put("items", items);
        TagsUtil.putCustomData(stack, tag);
    }

    public static ItemStack[] readStacksFromNBT(ItemStack stack, HolderLookup.Provider provider, int count) {
        if (!TagsUtil.hasCustomData(stack)) return null;

        final CompoundTag tag = TagsUtil.getCustomData(stack);
        if (!tag.contains("items")) return null;

        final ListTag list = tag.getList("items", 10);
        if (count < 1) count = list.size();

        final ItemStack[] stacks = new ItemStack[count];

        for (int i = 0; i < count; i++) {
            final CompoundTag slotNBT = list.getCompound(i);
            final byte slot = slotNBT.getByte("slot");
            if (slot >= 0 && slot < stacks.length) {
                stacks[slot] = ItemStack.parse(provider, slotNBT).orElse(ItemStack.EMPTY);
            }
        }

        return stacks;
    }

    public static ItemStack[] readStacksFromNBT(ItemStack stack, HolderLookup.Provider provider) {
        return readStacksFromNBT(stack, provider, 0);
    }

    public static ItemStack[] readStacksFromNBT(ItemStack stack) {
        return readStacksFromNBT(stack, RegistryAccess.EMPTY);
    }

    /**
     * Returns a List<String> of all tag ids for this stack (CE's ore-dictionary equivalent - the ore
     * dictionary itself is gone from 1.21, replaced by the vanilla/Forge tag system). Stack cannot be
     * null, list is empty when there are no tags.
     */
    public static List<String> getOreDictNames(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Collections.emptyList();
        }

        final List<String> list = new ArrayList<>();
        stack.getTags().forEach(tagKey -> list.add(tagKey.location().toString()));
        return list;
    }

    /**
     * CE compared item + metadata. Metadata no longer exists in 1.21 (item variants are now separate
     * {@link Item} instances or distinguished by data components), so this compares item identity only.
     */
    public static boolean isSameMetaItem(final ItemStack stack1, final ItemStack stack2) {
        return stack1.getItem() == stack2.getItem();
    }

    public static boolean isSameMetaItem(final ItemStack stack, final Item item) {
        return stack.getItem() == item;
    }

    // ItemStack from Item

    public static ItemStack itemStackFrom(final Item item) {
        return new ItemStack(item);
    }

    public static ItemStack itemStackFrom(final Item item, final int amount) {
        return new ItemStack(item, amount);
    }

    // ItemStack from Block

    public static ItemStack itemStackFrom(final Block block) {
        return new ItemStack(block);
    }

    public static ItemStack itemStackFrom(final Block block, final int amount) {
        return new ItemStack(block, amount);
    }

    // ItemStack from ItemStack, required for MetaItems

    @Deprecated
    public static ItemStack itemStackFrom(final ItemStack stack) {
        return stack;
    }

    public static ItemStack itemStackFrom(final ItemStack stack, final int amount) {
        return itemStackFrom(stack.getItem(), amount);
    }

    // ItemStack from a parsed NBT compound

    public static ItemStack itemStackFrom(final HolderLookup.Provider provider, final CompoundTag compoundTag) {
        return ItemStack.parse(provider, compoundTag).orElse(ItemStack.EMPTY);
    }

    public static boolean areStacksCompatible(ItemStack sta1, ItemStack sta2) {
        return ItemStack.isSameItemSameComponents(sta1, sta2);
    }
}
