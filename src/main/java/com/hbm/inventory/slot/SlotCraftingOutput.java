package com.hbm.inventory.slot;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * XP-awarding output slot, ported from CE's {@code com.hbm.inventory.slot.SlotCraftingOutput}
 * (read in full) onto {@link IItemHandler} instead of vanilla {@code Container} (same reasoning as
 * {@link SlotTakeOnly}'s javadoc). The override set - {@link #remove(int)} tallying a
 * {@code craftBuffer}, {@link #onQuickCraft(ItemStack, int)}/{@link #checkTakeAchievements(ItemStack)}
 * flushing it into {@code ItemStack#onCraftedBy}, {@link #onTake(Player, ItemStack)} calling
 * {@link #checkTakeAchievements(ItemStack)} before the super pickup - is copied verbatim (method
 * bodies and all) from Neo Edition's real, confirmed-compiling
 * {@code com.hbm.inventory.SlotCraftingOutput}, itself the direct 1.21.1 Mojang-mapped translation
 * of CE's 1.12 {@code decrStackSize}/{@code onCrafting}/{@code onTake} hooks
 * ({@code stack.onCraftedBy(Level, Player, int)} replaces CE's
 * {@code stack.onCrafting(World, EntityPlayer, int)}; CE's separate achievement-stat call is gone -
 * this port has no achievement/statistics system ported yet, matching Neo Edition's own equivalent
 * drop of that call rather than reintroducing a stub).
 */
public class SlotCraftingOutput extends SlotItemHandler {

    private final Player player;
    private int craftBuffer;

    public SlotCraftingOutput(Player player, IItemHandler itemHandler, int index, int x, int y) {
        super(itemHandler, index, x, y);
        this.player = player;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public ItemStack remove(int amount) {
        if (this.hasItem()) {
            this.craftBuffer += Math.min(amount, this.getItem().getCount());
        }
        return super.remove(amount);
    }

    @Override
    public void onTake(Player player, ItemStack stack) {
        this.checkTakeAchievements(stack);
        super.onTake(player, stack);
    }

    @Override
    protected void onQuickCraft(ItemStack stack, int amount) {
        this.craftBuffer += amount;
        this.checkTakeAchievements(stack);
    }

    @Override
    protected void checkTakeAchievements(ItemStack stack) {
        if (this.craftBuffer > 0) {
            stack.onCraftedBy(this.player.level(), this.player, this.craftBuffer);
        }
        this.craftBuffer = 0;
    }
}
