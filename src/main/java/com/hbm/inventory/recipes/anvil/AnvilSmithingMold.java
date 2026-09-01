package com.hbm.inventory.recipes.anvil;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.items.machine.ItemMold;
import com.hbm.util.ItemStackUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * CE {@code AnvilSmithingMold}: left matches ore-dict prefix or exact stacks; right is {@code mold_base}.
 * Consumes mold_base only ({@code amountConsumed} returns the slot index).
 */
public class AnvilSmithingMold extends AnvilSmithingRecipe {

    private final String prefix;
    private final int prefixCount;
    private final ItemStack[] matchesStack;

    public AnvilSmithingMold(int meta, AStack demo, String prefix, int prefixCount) {
        super(1, moldStack(meta), demo, new ComparableStack(moldBase()));
        this.prefix = prefix;
        this.prefixCount = prefixCount;
        this.matchesStack = null;
    }

    public AnvilSmithingMold(int meta, AStack demo, ItemStack[] matches) {
        super(1, moldStack(meta), demo, new ComparableStack(moldBase()));
        this.prefix = null;
        this.prefixCount = 0;
        this.matchesStack = matches;
    }

    @Override
    public boolean matches(ItemStack left, ItemStack right) {
        if (!doesStackMatch(right, this.right)) return false;

        if (prefix != null && left.getCount() == prefixCount) {
            MaterialShapes shape = MaterialShapes.prefixByName.get(prefix);
            if (shape != null && shape.tagFolder != null) {
                String folder = shape.tagFolder;
                for (String name : ItemStackUtil.getOreDictNames(left)) {
                    String path = name.contains(":") ? name.substring(name.indexOf(':') + 1) : name;
                    boolean longer = false;
                    for (MaterialShapes other : MaterialShapes.allShapes) {
                        if (other.tagFolder != null
                                && other.tagFolder.length() > folder.length()
                                && path.startsWith(other.tagFolder + "/")) {
                            longer = true;
                            break;
                        }
                    }
                    if (!longer && path.startsWith(folder + "/")) return true;
                }
            }
        }

        if (matchesStack != null) {
            for (ItemStack stack : matchesStack) {
                if (left.getItem() == stack.getItem() && left.getCount() == stack.getCount()) return true;
            }
        }
        return false;
    }

    @Override
    public int matchesInt(ItemStack left, ItemStack right) {
        return matches(left, right) ? 0 : -1;
    }

    @Override
    public int amountConsumed(int index, boolean mirrored) {
        return index;
    }

    private static Item moldBase() {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("hbm", "mold_base"));
    }

    private static ItemStack moldStack(int meta) {
        Item mold = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("hbm", "mold"));
        if (mold == Items.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(mold);
        ItemMold.setMoldId(stack, meta);
        return stack;
    }
}
