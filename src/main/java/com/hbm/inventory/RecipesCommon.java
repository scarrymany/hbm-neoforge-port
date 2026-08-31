package com.hbm.inventory;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Port of CE's {@code com.hbm.inventory.RecipesCommon} comparison-key hierarchy
 * ({@code AStack}/{@code ComparableStack}/{@code NbtComparableStack}/{@code OreDictStack}/
 * {@code MetaBlock}) — pure item/block-comparison data structures with no machine/TE coupling.
 * Two real consumers already sit in this port uncompiled without this class:
 * {@link com.hbm.api.block.IToolable} (its {@code ToolType.getType} lookup) and
 * {@link com.hbm.hazard.HazardSystem} (its {@code stackMap}/{@code stackBlacklist} keys), plus
 * {@code com.hbm.items.machine.ItemBlueprints}/{@code ItemBlueprintFolder} transitively via
 * {@code com.hbm.inventory.recipes.loader.GenericRecipe(s)} (see that package for the pool system
 * this class does not itself provide).
 * <p>
 * Two 1.12 -> 1.21.1 concepts this class deliberately does NOT carry over, per
 * {@code docs/phase2/items_tool_machine_coupling_and_recipe_system.md}'s Part B design and
 * {@code ItemStackUtil}'s own header comment flagging this exact gap:
 * <ul>
 *     <li><b>Item metadata (damage-value subtypes)</b> is gone in modern Minecraft — distinct
 *     "meta variants" of a CE item are now distinct registered {@link Item} instances. CE's
 *     {@code ComparableStack.meta}/{@code OreDictionary.WILDCARD_VALUE} fields and every
 *     meta-taking constructor are dropped; {@link ComparableStack} compares only
 *     {@link Item} identity + count. Existing consumers ({@code IToolable}, {@code HazardSystem})
 *     only ever call the plain {@code ComparableStack(ItemStack)} constructor, so this is not a
 *     regression against anything already committed.</li>
 *     <li><b>{@code OreDictionary}</b> (string ore names like {@code "ingotIron"}) is gone,
 *     replaced by the tag system. {@link OreDictStack} is keyed on a {@link TagKey}&lt;{@link Item}&gt;
 *     instead of a {@code String} — the same substitution
 *     {@code upstream/neo-edition/.../RecipesCommon.java}'s own {@code TagStack} independently
 *     made (confirmed by reading that file), and the same one this port's own
 *     {@code MaterialShapes.commonTag(NTMMaterial)} already relies on elsewhere.</li>
 * </ul>
 * {@link NbtComparableStack}'s data-component match is intentionally a conservative
 * <b>exact</b>-component comparison ({@link ItemStack#isSameItemSameComponents(ItemStack)}), not
 * CE's partial/subset containment ({@code Library.tagContainsOther}) — that research doc's Open
 * Questions section explicitly flags a proper component-subset matcher (for a future
 * {@code ICustomIngredient}) as unverified, invented-not-read design work; this class does not
 * attempt it and callers should not assume subset semantics.
 * <p>
 * One further deliberate deviation from CE: {@link ComparableStack#equals}/{@link NbtComparableStack#equals}
 * here require the same runtime type (a {@code ComparableStack} never equals a
 * {@code NbtComparableStack}, even with the same item/count). CE's original allowed
 * asymmetric cross-type equality (an NBT-bearing {@code NbtComparableStack} could {@code equals()}
 * a plain {@code ComparableStack} while having a different {@code hashCode()}) — a pre-existing
 * {@code equals}/{@code hashCode} contract violation in the source, not reproduced here.
 */
public final class RecipesCommon {

    private RecipesCommon() { }

    /**
     * This is mutable! (kept consistent with CE — {@link #stacksize} is written directly by
     * {@link #singulize()}/{@code setCount}, callers that need an immutable key should
     * {@link #copy()} first.)
     */
    public abstract static class AStack implements Comparable<AStack> {

        public int stacksize;

        public int count() {
            return stacksize;
        }

        public void setCount(int c) {
            stacksize = c;
        }

        public AStack singulize() {
            stacksize = 1;
            return this;
        }

        /**
         * Whether the supplied stack is applicable for a recipe (e.g. anvils). Slightly different
         * from {@link #matchesRecipe} in CE only insofar as CE dispatched through an
         * {@code NbtComparableStack} wrapper for cross-type comparisons; this port's simplified
         * (meta-less, no cross-type {@code ComparableStack}/{@code OreDictStack} dispatch)
         * comparison model makes that indirection unnecessary — this is just {@code matchesRecipe}
         * with size ignored.
         */
        public boolean isApplicable(ItemStack stack) {
            return matchesRecipe(stack, true);
        }

        public abstract boolean matchesRecipe(ItemStack stack, boolean ignoreSize);

        public abstract AStack copy();

        public abstract AStack copy(int stacksize);

        public abstract ItemStack getStack();

        public abstract List<ItemStack> getStackList();

        /** Generates either a singleton or a full list of {@link ItemStack}s, for JEI/REI-style display. */
        public abstract List<ItemStack> extractForJEI();

        public ItemStack extractForCyclingDisplay(int cycle) {
            List<ItemStack> list = extractForJEI();
            cycle *= 50;

            // CE fell back to ModItems.nothing here; this port has not (yet) registered an
            // equivalent placeholder item, so an empty stack is the safe fallback.
            if (list.isEmpty()) return ItemStack.EMPTY;
            return list.get((int) (System.currentTimeMillis() % ((long) cycle * list.size()) / cycle));
        }

        @Override
        public String toString() {
            return "AStack: size, " + stacksize;
        }
    }

    /**
     * This is mutable! Exact-{@link Item} + count match. See the class header for why this no
     * longer carries a {@code meta} field.
     */
    public static class ComparableStack extends AStack {

        public Item item;

        public ComparableStack(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                this.item = null;
                this.stacksize = 0;
                return;
            }
            this.item = stack.getItem();
            this.stacksize = stack.getCount();
        }

        public ComparableStack(Item item) {
            this.item = item;
            this.stacksize = 1;
        }

        public ComparableStack(Item item, int stacksize) {
            this.item = item;
            this.stacksize = stacksize;
        }

        public ComparableStack(Block block) {
            this(block == null ? null : block.asItem());
        }

        public ComparableStack(Block block, int stacksize) {
            this(block == null ? null : block.asItem(), stacksize);
        }

        public ComparableStack makeSingular() {
            stacksize = 1;
            return this;
        }

        public ItemStack toStack() {
            return item == null ? ItemStack.EMPTY : new ItemStack(item, Math.max(stacksize, 1));
        }

        @Override
        public ItemStack getStack() {
            return toStack();
        }

        @Override
        public List<ItemStack> getStackList() {
            return Collections.singletonList(getStack());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            if (item == null) {
                MainRegistry.logger.warn("ComparableStack has a null item! This is likely a bug.");
            } else {
                // getKey can legitimately return null for a not-yet-registered item (e.g. queried
                // before RegisterEvent fires); fall back rather than risk an NPE here.
                ResourceLocation name = BuiltInRegistries.ITEM.getKey(item);
                if (name == null) {
                    MainRegistry.logger.warn("ComparableStack holds an item that is not registered: {}", item);
                } else {
                    result = prime * result + name.hashCode();
                }
            }
            result = prime * result + stacksize;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ComparableStack other)) return false;
            if (getClass() != obj.getClass()) return false;
            if (item == null) {
                if (other.item != null) return false;
            } else if (!item.equals(other.item)) return false;
            return stacksize == other.stacksize;
        }

        @Override
        public int compareTo(AStack stack) {
            if (stack instanceof ComparableStack comp) {
                int thisId = Item.getId(item);
                int thatId = Item.getId(comp.item);
                if (thisId != thatId) return Integer.compare(thisId, thatId);
                return Integer.compare(stacksize, comp.stacksize);
            }
            // if compared with an OreDictStack, the ComparableStack takes priority (matches CE)
            if (stack instanceof OreDictStack) return 1;
            return 0;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            if (stack == null || stack.isEmpty()) return false;
            if (stack.getItem() != this.item) return false;
            return ignoreSize || stack.getCount() >= this.stacksize;
        }

        @Override
        public AStack copy() {
            return new ComparableStack(item, stacksize);
        }

        @Override
        public AStack copy(int stacksize) {
            return new ComparableStack(item, stacksize);
        }

        @Override
        public String toString() {
            return "ComparableStack: { " + stacksize + " x " + (item == null ? "null" : BuiltInRegistries.ITEM.getKey(item)) + " }";
        }

        @Override
        public List<ItemStack> extractForJEI() {
            return Collections.singletonList(this.toStack());
        }

        public boolean isEmpty() {
            return item == null || stacksize <= 0;
        }
    }

    /**
     * This is mutable! Adds a full-stack (item + count + data components) match on top of
     * {@link ComparableStack}. See the class header re: exact vs. partial component matching.
     */
    public static class NbtComparableStack extends ComparableStack {

        protected ItemStack stack;

        public NbtComparableStack(ItemStack stack) {
            super(stack);
            this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
        }

        @Override
        public NbtComparableStack makeSingular() {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            return new NbtComparableStack(copy);
        }

        @Override
        public AStack singulize() {
            stack.setCount(1);
            this.stacksize = 1;
            return this;
        }

        @Override
        public ItemStack toStack() {
            return stack.copy();
        }

        @Override
        public ItemStack getStack() {
            return toStack();
        }

        @Override
        public int hashCode() {
            // Deliberately just the item+count hash, not folding in a components hash: this port
            // has no independently-confirmed guarantee that DataComponentMap's hashCode is a
            // meaningful content hash (unverified against a real NeoForge 1.21.1 build, per this
            // area's "read the real signature, don't guess" constraint). Equal-hashCode-for-unequal-
            // objects is always contract-safe (just weaker bucketing); the reverse is not, so this
            // errs conservative rather than risk it.
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof NbtComparableStack other)) return false;
            return super.equals(obj) && ItemStack.isSameItemSameComponents(this.stack, other.stack);
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            return super.matchesRecipe(stack, ignoreSize) && ItemStack.isSameItemSameComponents(this.stack, stack);
        }

        @Override
        public AStack copy() {
            return new NbtComparableStack(stack);
        }

        @Override
        public AStack copy(int stacksize) {
            ItemStack copy = stack.copy();
            copy.setCount(stacksize);
            return new NbtComparableStack(copy);
        }

        @Override
        public String toString() {
            return "NbtComparableStack: " + stack;
        }
    }

    /**
     * This is mutable! Modern-tag replacement for CE's ore-dictionary-name-keyed
     * {@code OreDictStack} — see the class header for why this is now {@link TagKey}-backed.
     */
    public static class OreDictStack extends AStack {

        public final TagKey<Item> tag;

        public OreDictStack(TagKey<Item> tag) {
            this.tag = tag;
            this.stacksize = 1;
        }

        public OreDictStack(TagKey<Item> tag, int stacksize) {
            this(tag);
            this.stacksize = stacksize;
        }

        /**
         * Convenience for the common-conventions {@code c:} tag namespace, e.g.
         * {@code OreDictStack.ofCommonTag("ingots/iron")} for what CE named {@code "ingotIron"}.
         */
        public static OreDictStack ofCommonTag(String path) {
            return new OreDictStack(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path)));
        }

        public static OreDictStack ofHbmTag(String path, int count) {
            return new OreDictStack(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path)), count);
        }

        public List<ItemStack> toStacks() {
            List<ItemStack> stacks = new ArrayList<>();
            for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                stacks.add(new ItemStack(holder.value(), stacksize));
            }
            return stacks;
        }

        @Override
        public ItemStack getStack() {
            List<ItemStack> stacks = toStacks();
            if (stacks.isEmpty()) return ItemStack.EMPTY;
            ItemStack stack = stacks.get(0).copy();
            stack.setCount(stacksize);
            return stack;
        }

        @Override
        public List<ItemStack> getStackList() {
            List<ItemStack> list = toStacks();
            for (ItemStack stack : list) stack.setCount(this.stacksize);
            return list;
        }

        @Override
        public int hashCode() {
            return (tag.location().toString() + stacksize).hashCode();
        }

        @Override
        public int compareTo(AStack stack) {
            if (stack instanceof OreDictStack comp) return tag.location().compareTo(comp.tag.location());
            // if compared with a ComparableStack, the OreDictStack yields (matches CE)
            if (stack instanceof ComparableStack) return -1;
            return 0;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            if (stack == null || stack.isEmpty()) return false;
            if (!ignoreSize && stack.getCount() < this.stacksize) return false;
            return stack.is(tag);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof OreDictStack other)) return false;
            return tag.location().equals(other.tag.location()) && stacksize == other.stacksize;
        }

        @Override
        public AStack copy() {
            return new OreDictStack(tag, stacksize);
        }

        @Override
        public AStack copy(int stacksize) {
            return new OreDictStack(tag, stacksize);
        }

        @Override
        public String toString() {
            return "OreDictStack: tag, " + tag.location() + ", stacksize, " + stacksize;
        }

        @Override
        public List<ItemStack> extractForJEI() {
            return getStackList();
        }
    }

    /**
     * Block + {@link BlockState} comparison key, replacing CE's {@code Block + int meta} pair —
     * modern Minecraft has no block metadata, {@link BlockState} is already the flyweight-singleton
     * equivalent, so CE's {@code META_POOLS} interning cache is not needed here.
     */
    public static final class MetaBlock {

        public final Block block;
        public final BlockState state;

        public MetaBlock(Block block, BlockState state) {
            this.block = block;
            this.state = state;
        }

        public MetaBlock(Block block) {
            this(block, block.defaultBlockState());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + BuiltInRegistries.BLOCK.getKey(block).hashCode();
            result = prime * result + state.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof MetaBlock other)) return false;
            if (block == null) {
                if (other.block != null) return false;
            } else if (!block.equals(other.block)) return false;
            return state.equals(other.state);
        }
    }

    public static MetaBlock metaOf(BlockState state) {
        return new MetaBlock(state.getBlock(), state);
    }

    public static MetaBlock metaOf(Block block) {
        return new MetaBlock(block);
    }
}
