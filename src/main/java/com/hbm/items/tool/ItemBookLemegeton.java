package com.hbm.items.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code com.hbm.items.tool.ItemBookLemegeton} ({@code book_lemegeton}): an
 * occult/ritual grimoire whose right-click opened {@code GUILemegeton}/{@code ContainerLemegeton}.
 * <p>
 * <b>Recipe data ready, menu still deferred</b> ({@code mrec-08-chemplant-misc} pass, see
 * {@code docs/phase7/mrec_08_chemplant_misc.md}): the 37-entry transmutation table this book's menu
 * would consume is fully ported ({@code com.hbm.inventory.recipes.machine.LemegetonRecipes}, JSON
 * files under {@code data/hbm/recipe/lemegeton/}) - the item-registry side of this feature has no gap
 * at all. What is still missing is the {@code Menu}/{@code Screen} itself: CE's
 * {@code ContainerLemegeton} has <i>no backing block/tile entity</i> (just the player's own inventory
 * plus 2 extra slots), and this port has no precedent yet for an item-triggered (non-block-entity)
 * {@link net.minecraft.world.inventory.AbstractContainerMenu} - every {@code Menu} class in this port
 * today is opened from a block entity right-click. That is UI-infrastructure work, not recipe-data
 * porting, and is left for a future pass (likely {@code player.openMenu(new SimpleMenuProvider(...))}
 * from {@link #appendHoverText}'s sibling {@code use()} override, once such a menu exists) - not, as
 * this class previously claimed, blocked on "no Menu/Screen framework exists in this port yet" (the
 * port has one; it just has no screenless-item-triggered precedent for this class to copy).
 */
public class ItemBookLemegeton extends Item {

    public ItemBookLemegeton(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Ars Goetia - the Lesser Key of Solomon."));
    }

    // Menu-opening interaction deferred - see class javadoc. No use() override until a
    // MenuProvider/Screen equivalent of CE's GUILemegeton/ContainerLemegeton exists.
}
