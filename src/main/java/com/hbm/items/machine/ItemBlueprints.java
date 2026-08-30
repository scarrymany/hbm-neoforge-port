package com.hbm.items.machine;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.Supplier;

/**
 * A discovered/rolled blueprint. CE stored the pool key as a raw "pool" NBT string and used
 * per-metadata baked models for base/discover/secret/528 texture variants; this is one registry
 * item with the pool key as a data component, matching {@link ItemBlueprintFolder}'s roll output.
 */
public class ItemBlueprints extends ItemBase {

    public ItemBlueprints(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.pass(stack);

        String pool = grabPool(stack);
        if (pool == null || pool.startsWith(GenericRecipes.POOL_PREFIX_SECRET)) return InteractionResultHolder.pass(stack);

        List<ItemStack> inventory = player.getInventory().items;
        int paperSlot = -1;
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).is(Items.PAPER)) {
                paperSlot = i;
                break;
            }
        }
        if (paperSlot < 0) return InteractionResultHolder.pass(stack);

        inventory.get(paperSlot).shrink(1);
        player.swing(hand);

        ItemStack copy = stack.copyWithCount(1);

        if (!player.getAbilities().instabuild) {
            if (stack.getCount() < stack.getMaxStackSize()) {
                stack.grow(1);
                return InteractionResultHolder.success(stack);
            }
            if (!player.getInventory().add(copy)) {
                player.drop(copy, false);
            }
        } else {
            player.drop(copy, false);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String poolName = grabPool(stack);
        if (poolName == null) return;

        List<String> pool = GenericRecipes.blueprintPools.get(poolName);
        if (pool == null || pool.isEmpty()) return;

        if (poolName.startsWith(GenericRecipes.POOL_PREFIX_SECRET)) {
            tooltip.add(Component.literal("Cannot be copied!").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.literal("Right-click to copy (requires paper)").withStyle(ChatFormatting.YELLOW));
        }

        for (String name : pool) {
            GenericRecipe recipe = GenericRecipes.pooledBlueprints.get(name);
            if (recipe != null) tooltip.add(recipe.getLocalizedName());
        }
    }

    public static String grabPool(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemBlueprints)) return null;
        return stack.get(MachineDataComponents.BLUEPRINT_POOL.get());
    }

    /**
     * Builds a rolled blueprint stack. Takes the registered {@code blueprints} item as a supplier
     * (rather than looking it up internally via {@code ModItems}) so this class has no
     * compile-time dependency on {@code ModItems}, which in turn depends on this class to
     * register it.
     */
    public static ItemStack make(Supplier<? extends Item> blueprintsItem, String pool) {
        ItemStack stack = new ItemStack(blueprintsItem.get());
        stack.set(MachineDataComponents.BLUEPRINT_POOL.get(), pool);
        return stack;
    }
}
