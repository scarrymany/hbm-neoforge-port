package com.hbm.hazard;

import com.hbm.config.RadiationConfig;
import com.hbm.config.ServerConfig;
import com.hbm.hazard.modifier.IHazardModifier;
import com.hbm.hazard.transformer.IHazardTransformer;
import com.hbm.hazard.type.IHazardType;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry engine: tag/item/exact-stack lookup maps, a blacklist, a transformer chain wrapped around the merge, and
 * the public API other systems use to pull hazard data for a stack and apply its effects.
 * <p>
 * This is a from-scratch NeoForge port of CE's engine. CE additionally layered a threaded, cached,
 * volatility-protected per-player scan pipeline (ASM-hooked inventory deltas, a two-tier Guava cache keyed on a
 * neutron-contamination-aware NBT hash) on top of this mechanism as a performance optimization. That layer is not
 * ported here: it depends on {@code ContaminationUtil}'s neutron-tracking NBT key (not yet migrated to a data
 * component - see {@code HazardRegistry} risk notes) and on ASM hooks whose NeoForge event equivalents were not
 * confirmed against a live example. {@link #updatePlayerInventory}, {@link #updateLivingInventory} and
 * {@link #updateDroppedItem} below are a straightforward, uncached tick-driven replacement, matching the shape
 * already proven to work in the Neo Edition reference (`HazardSystem` there has no caching either).
 */
public class HazardSystem {

    /**
     * Map for item tags, always evaluated first. Avoid registering HazardData with 'doesOverride', as internal order
     * depends on iteration order over the stack's own tags.
     */
    public static final Map<TagKey<Item>, HazardData> tagMap = new LinkedHashMap<>();
    /**
     * Map for items, either with wildcard variants or stuff that's expected to have a variety of forms, like tools.
     */
    public static final Map<Item, HazardData> itemMap = new HashMap<>();
    /**
     * Very specific stacks with item and (legacy meta-equivalent) matching. {@link ComparableStack} does not support
     * NBT/component matching; to scale hazards with stack data use {@link IHazardModifier}.
     */
    public static final Map<ComparableStack, HazardData> stackMap = new HashMap<>();
    /**
     * For items that should, for whichever reason, be completely exempt from the hazard system.
     */
    public static final Set<ComparableStack> stackBlacklist = new HashSet<>();
    public static final Set<TagKey<Item>> tagBlacklist = new HashSet<>();
    /**
     * List of hazard transformers, called in order before and after unrolling all the HazardEntries.
     */
    public static final List<IHazardTransformer> trafos = new ArrayList<>();

    private HazardSystem() {
    }

    // ==================== registration ====================

    /**
     * Registers {@link HazardData} for an item tag. Tag mappings are evaluated before item and stack mappings.
     */
    public static void register(final TagKey<Item> tag, final HazardData data) {
        tagMap.put(tag, data);
    }

    /**
     * Convenience overload resolving {@code namespace:path} to an item tag, mirroring CE's OreDictionary name
     * registration.
     */
    public static void register(final String tagName, final HazardData data) {
        register(itemTag(tagName), data);
    }

    /**
     * Registers {@link HazardData} for all stacks of a given {@link Item} (any variant). Evaluated after tag
     * mappings and before exact-stack mappings.
     */
    public static void register(final Item item, final HazardData data) {
        itemMap.put(item, data);
    }

    /**
     * Registers {@link HazardData} for the item form of a {@link Block}.
     */
    public static void register(final Block block, final HazardData data) {
        itemMap.put(block.asItem(), data);
    }

    /**
     * Registers {@link HazardData} for an item addressed by registry name. Unlike CE, this resolves immediately:
     * NeoForge's {@code DeferredRegister}/{@code RegisterEvent} already guarantees items exist by the time mod code
     * runs common setup, so there is no need for a deferred registration queue.
     */
    public static void register(final ResourceLocation loc, final HazardData data) {
        final Item item = BuiltInRegistries.ITEM.getOptional(loc).orElse(null);
        if (item == null) {
            MainRegistry.logger.warn("HazardSystem.register: no item registered under {}, skipping.", loc);
            return;
        }
        itemMap.put(item, data);
    }

    /**
     * Registers {@link HazardData} for an exact item/(legacy meta-equivalent) pair, normalized via
     * {@link ComparableStack#makeSingular()} so registration is count-insensitive.
     */
    public static void register(final ItemStack stack, final HazardData data) {
        stackMap.put(new ComparableStack(stack).makeSingular(), data);
    }

    /**
     * Registers {@link HazardData} for an exact {@link ComparableStack} key. Callers are responsible for providing a
     * singular key if count-insensitivity is desired.
     */
    public static void register(final ComparableStack comp, final HazardData data) {
        stackMap.put(comp, data);
    }

    /**
     * Registers hazard data for an object key (tag name, tag, item, block, stack, or comparable stack).
     */
    @SuppressWarnings("unchecked")
    public static void register(final Object o, final HazardData data) {
        if (o instanceof String s) register(s, data);
        else if (o instanceof TagKey<?> tag && tag.isFor(Registries.ITEM)) register((TagKey<Item>) tag, data);
        else if (o instanceof Item i) register(i, data);
        else if (o instanceof ResourceLocation rl) register(rl, data);
        else if (o instanceof Block b) register(b, data);
        else if (o instanceof ItemStack is) register(is, data);
        else if (o instanceof ComparableStack cs) register(cs, data);
        else throw new IllegalArgumentException("Unsupported key type for register: " + (o == null ? "null" : o.getClass().getName()));
    }

    // ==================== unregistration ====================

    public static boolean unregister(final TagKey<Item> tag) {
        return tagMap.remove(tag) != null;
    }

    public static boolean unregister(final String tagName) {
        return unregister(itemTag(tagName));
    }

    public static boolean unregister(final Item item) {
        return itemMap.remove(item) != null;
    }

    public static boolean unregister(final ResourceLocation loc) {
        final Item item = BuiltInRegistries.ITEM.getOptional(loc).orElse(null);
        return item != null && itemMap.remove(item) != null;
    }

    public static boolean unregister(final Block block) {
        return itemMap.remove(block.asItem()) != null;
    }

    public static boolean unregister(final ItemStack stack) {
        return stackMap.remove(new ComparableStack(stack).makeSingular()) != null;
    }

    public static boolean unregister(final ComparableStack comp) {
        return stackMap.remove(comp) != null;
    }

    /**
     * Unregister hazard data for the given key, or a collection/array of keys.
     */
    @SuppressWarnings("unchecked")
    public static boolean unregister(final Object o) {
        if (o == null) return false;
        if (o instanceof Collection<?> c) {
            boolean removed = false;
            for (final Object element : c) removed |= unregister(element);
            return removed;
        }
        if (o.getClass().isArray()) {
            boolean removed = false;
            final int length = Array.getLength(o);
            for (int i = 0; i < length; i++) removed |= unregister(Array.get(o, i));
            return removed;
        }
        if (o instanceof String s) return unregister(s);
        if (o instanceof TagKey<?> tag && tag.isFor(Registries.ITEM)) return unregister((TagKey<Item>) tag);
        if (o instanceof Item i) return unregister(i);
        if (o instanceof ResourceLocation rl) return unregister(rl);
        if (o instanceof Block b) return unregister(b);
        if (o instanceof ItemStack is) return unregister(is);
        if (o instanceof ComparableStack cs) return unregister(cs);
        throw new IllegalArgumentException("Unsupported key type for unregister: " + o.getClass().getName());
    }

    // ==================== blacklist ====================

    public static void blacklist(final ItemStack stack) {
        stackBlacklist.add(new ComparableStack(stack).makeSingular());
    }

    public static void blacklist(final ComparableStack comp) {
        stackBlacklist.add(comp.makeSingular());
    }

    public static void blacklist(final TagKey<Item> tag) {
        tagBlacklist.add(tag);
    }

    public static void blacklist(final String tagName) {
        blacklist(itemTag(tagName));
    }

    /**
     * Prevents the stack from returning any HazardData. Collections/arrays are expanded recursively.
     */
    @SuppressWarnings("unchecked")
    public static void blacklist(final Object o) {
        if (o instanceof Collection<?> c) {
            for (final Object element : c) blacklist(element);
            return;
        }
        if (o != null && o.getClass().isArray()) {
            final int length = Array.getLength(o);
            for (int i = 0; i < length; i++) blacklist(Array.get(o, i));
            return;
        }
        if (o instanceof ItemStack is) blacklist(is);
        else if (o instanceof ComparableStack cs) blacklist(cs);
        else if (o instanceof String s) blacklist(s);
        else if (o instanceof TagKey<?> tag && tag.isFor(Registries.ITEM)) blacklist((TagKey<Item>) tag);
        else throw new IllegalArgumentException("Unsupported key type for blacklist: " + (o == null ? "null" : o.getClass().getName()));
    }

    public static boolean unblacklist(final ItemStack stack) {
        return stackBlacklist.remove(new ComparableStack(stack).makeSingular());
    }

    public static boolean unblacklist(final ComparableStack comp) {
        return stackBlacklist.remove(comp.makeSingular());
    }

    public static boolean unblacklist(final TagKey<Item> tag) {
        return tagBlacklist.remove(tag);
    }

    public static boolean unblacklist(final String tagName) {
        return unblacklist(itemTag(tagName));
    }

    @SuppressWarnings("unchecked")
    public static boolean unblacklist(final Object o) {
        if (o == null) return false;
        if (o instanceof Collection<?> c) {
            boolean removed = false;
            for (final Object element : c) removed |= unblacklist(element);
            return removed;
        }
        if (o.getClass().isArray()) {
            boolean removed = false;
            final int length = Array.getLength(o);
            for (int i = 0; i < length; i++) removed |= unblacklist(Array.get(o, i));
            return removed;
        }
        if (o instanceof ItemStack is) return unblacklist(is);
        if (o instanceof ComparableStack cs) return unblacklist(cs);
        if (o instanceof String s) return unblacklist(s);
        if (o instanceof TagKey<?> tag && tag.isFor(Registries.ITEM)) return unblacklist((TagKey<Item>) tag);
        throw new IllegalArgumentException("Unsupported key type for unblacklist: " + o.getClass().getName());
    }

    /**
     * Checks whether the given stack is blacklisted by exact item/(legacy meta-equivalent) or by tag.
     */
    public static boolean isItemBlacklisted(final ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (stackBlacklist.contains(new ComparableStack(stack).makeSingular())) return true;
        return stack.getTags().anyMatch(tagBlacklist::contains);
    }

    // ==================== lookup ====================

    /**
     * @return {@code true} if there exists any applicable {@link HazardEntry} for the stack (count-insensitive).
     */
    public static boolean isStackHazardous(final ItemStack stack) {
        return stack != null && !stack.isEmpty() && !getHazardsFromStack(stack).isEmpty();
    }

    /**
     * Returns the full list of applicable {@link HazardEntry} for this stack.
     * <br><br>ORDER:
     * <ol>
     * <li>item tags
     * <li>item
     * <li>exact stack
     * </ol>
     * "Applicable" means entries overridden or excluded via mutex are not in this list. Entries marked as
     * "overriding" delete all entries gathered before it. Mutex entries prevent subsequent colliding entries from
     * being considered.
     *
     * @apiNote count insensitive; the returned list has been run through {@link #trafos} but not through modifiers.
     */
    public static List<HazardEntry> getHazardsFromStack(final ItemStack stack) {
        if (stack.isEmpty() || isItemBlacklisted(stack)) {
            return Collections.emptyList();
        }

        final ComparableStack compStack = new ComparableStack(stack).makeSingular();

        final List<HazardData> chronological = new ArrayList<>();
        stack.getTags().forEach(tag -> {
            final HazardData data = tagMap.get(tag);
            if (data != null) chronological.add(data);
        });

        final HazardData itemData = itemMap.get(stack.getItem());
        if (itemData != null) chronological.add(itemData);

        final HazardData stackData = stackMap.get(compStack);
        if (stackData != null) chronological.add(stackData);

        final List<HazardEntry> entries = new ArrayList<>();

        for (final IHazardTransformer trafo : trafos) {
            trafo.transformPre(stack, entries);
        }

        int mutex = 0;
        for (final HazardData data : chronological) {
            if (data.doesOverride()) entries.clear();
            if ((data.getMutex() & mutex) == 0) {
                entries.addAll(data.entries);
                mutex |= data.getMutex();
            }
        }

        for (final IHazardTransformer trafo : trafos) {
            trafo.transformPost(stack, entries);
        }

        return entries;
    }

    /**
     * Computes the summed effective level for a specific hazard type from the stack.
     */
    public static double getHazardLevelFromStack(final ItemStack stack, final IHazardType hazard) {
        double totalLevel = 0.0;
        for (final HazardEntry entry : getHazardsFromStack(stack)) {
            if (entry.type == hazard) {
                totalLevel += IHazardModifier.evalAllModifiers(stack, null, entry.baseLevel, entry.mods);
            }
        }
        return totalLevel;
    }

    public static double getRawRadsFromBlock(final Block b) {
        return getHazardLevelFromStack(new ItemStack(b.asItem()), HazardRegistry.RADIATION);
    }

    public static double getRawRadsFromStack(final ItemStack stack) {
        return getHazardLevelFromStack(stack, HazardRegistry.RADIATION);
    }

    // ==================== application ====================

    public static void applyHazards(final Block b, final LivingEntity entity) {
        applyHazards(new ItemStack(b.asItem()), entity);
    }

    /**
     * Iterates all assigned hazards of the given stack and applies their effects to the holder.
     */
    public static void applyHazards(final ItemStack stack, final LivingEntity entity) {
        if (stack.isEmpty()) return;
        for (final HazardEntry hazard : getHazardsFromStack(stack)) {
            hazard.applyHazard(stack, entity);
        }
    }

    /**
     * Applies hazards for every stack in a player's main inventory, armor and offhand. Intended to be called once
     * per server tick from an {@code EntityTickEvent} for {@link Player} entities; internally gated to CE's
     * {@link RadiationConfig#hazardRate} cadence since the individual {@link IHazardType} implementations already
     * scale their effect by that same rate.
     */
    public static void updatePlayerInventory(final Player player) {
        if (!onHazardTick(player)) return;

        for (final ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty()) applyHazards(stack, player);
        }
        for (final ItemStack stack : player.getInventory().armor) {
            if (!stack.isEmpty()) applyHazards(stack, player);
        }
        for (final ItemStack stack : player.getInventory().offhand) {
            if (!stack.isEmpty()) applyHazards(stack, player);
        }
    }

    /**
     * Applies hazards for every equipped item slot of a non-player living entity. Same tick cadence as
     * {@link #updatePlayerInventory}.
     */
    public static void updateLivingInventory(final LivingEntity entity) {
        if (!onHazardTick(entity)) return;

        for (final EquipmentSlot slot : EquipmentSlot.values()) {
            final ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) applyHazards(stack, entity);
        }
    }

    /**
     * Updates hazards emitted by a dropped {@link ItemEntity}, gated to {@link ServerConfig#ITEM_HAZARD_DROP_TICKRATE}.
     */
    public static void updateDroppedItem(final ItemEntity entity) {
        if (entity.level().isClientSide() || entity.isRemoved()) return;
        final ItemStack stack = entity.getItem();
        if (stack.isEmpty()) return;

        final int tickrate = Math.max(1, ServerConfig.ITEM_HAZARD_DROP_TICKRATE.get());
        if (entity.level().getGameTime() % tickrate != 0) return;

        for (final HazardEntry entry : getHazardsFromStack(stack)) {
            entry.type.updateEntity(entity, IHazardModifier.evalAllModifiers(stack, null, entry.baseLevel, entry.mods));
        }
    }

    /**
     * Adds hazard tooltip info for the given stack.
     */
    @OnlyIn(Dist.CLIENT)
    public static void addHazardInfo(final ItemStack stack, final Player player, final List<Component> list) {
        for (final HazardEntry hazard : getHazardsFromStack(stack)) {
            hazard.type.addHazardInformation(player, list, hazard.baseLevel, stack, hazard.mods);
        }
    }

    private static boolean onHazardTick(final LivingEntity entity) {
        return entity.level().getGameTime() % Math.max(1, RadiationConfig.hazardRate) == 0;
    }

    private static TagKey<Item> itemTag(final String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.parse(name));
    }
}
