package com.hbm.items.machine;

import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ItemBase;
import com.hbm.main.MainRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Foundry crucible scrap: melted-down material carried by a plain resource item, one per
 * smeltable/additive {@link NTMMaterial}. CE modeled this as one registry entry with the
 * material's numeric id as metadata (see {@code ItemAutogen}); it is really part of the
 * material/shape item-generation pipeline rather than bespoke machine content (per the porting
 * plan), so each material gets its own registered {@code scrap_<material>} item here rather than a
 * shared metadata-multi item. The (Phase 2) foundry tile entity is the only real consumer; this
 * class carries no tile entity reference of its own.
 */
public class ItemScraps extends ItemBase {

    private final NTMMaterial material;

    public ItemScraps(NTMMaterial material, Properties properties) {
        super(properties);
        this.material = material;
    }

    public NTMMaterial getMaterial() {
        return this.material;
    }

    public static int getAmount(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.SCRAP_AMOUNT.get(), MaterialShapes.INGOT.q(1));
    }

    public static boolean isLiquid(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(MachineDataComponents.SCRAP_LIQUID.get()));
    }

    public static ItemStack create(ItemStack scrapItem, int amount, boolean liquid) {
        ItemStack stack = scrapItem.copyWithCount(1);
        stack.set(MachineDataComponents.SCRAP_AMOUNT.get(), amount);
        if (liquid) stack.set(MachineDataComponents.SCRAP_LIQUID.get(), true);
        return stack;
    }

    /** CE {@code ItemScraps.create(MaterialStack, liquid)} — flattened {@code scraps_<mat>}. */
    public static ItemStack create(Mats.MaterialStack stack, boolean liquid) {
        if (stack == null || stack.material == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(
                MainRegistry.MODID, "scraps_" + stack.material.getRegistryName()));
        if (item == Items.AIR) return ItemStack.EMPTY;
        return create(new ItemStack(item), stack.amount, liquid);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int amount = getAmount(stack);
        tooltip.add(Component.literal(amount + " mB"));
        if (isLiquid(stack) && this.material.smeltable == NTMMaterial.SmeltingBehavior.ADDITIVE) {
            tooltip.add(Component.literal("Additive, not castable!").withStyle(ChatFormatting.DARK_RED));
        }
    }

    /** True for exactly the materials CE registers a scrap variant for, per {@code Mats.orderedList}. */
    public static boolean isScrappable(NTMMaterial material) {
        return material.smeltable == NTMMaterial.SmeltingBehavior.SMELTABLE || material.smeltable == NTMMaterial.SmeltingBehavior.ADDITIVE;
    }
}
