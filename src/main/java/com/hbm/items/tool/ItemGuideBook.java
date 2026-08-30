package com.hbm.items.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code com.hbm.items.tool.ItemGuideBook} ({@code book_guide_book}): an in-game
 * multi-chapter guide book whose right-click opened a page-flipping {@code GUIScreenGuide}. Per
 * {@code docs/phase1/items_tool.md} finding 3 (no {@code AbstractContainerMenu}/{@code Screen}
 * framework has been ported yet), this follows the exact shell pattern already established by
 * {@link com.hbm.items.special.ItemBook} for the same reason: register the item now, leave the
 * menu-opening interaction as an explicit, documented gap rather than a fake/partial
 * implementation.
 *
 * <p>CE's four metadata variants ({@code TEST}/{@code RBMK}/{@code HADRON}/{@code STARTER}, each a
 * different book "cover"/chapter set) are not reproduced as separate items or variants here: 1.21
 * has no item metadata, and the chapter content itself lives entirely inside the not-yet-ported
 * {@code GUIScreenGuide}/{@code GuidePage} rendering this class deferred - there is nothing for a
 * variant to select between yet. Revisit variant handling (data components, or separate registered
 * items per chapter) once that GUI framework exists.
 */
public class ItemGuideBook extends Item {

    public ItemGuideBook(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("A guide to the wonders (and horrors) of nuclear technology."));
    }

    // Menu-opening interaction deferred - see class javadoc. No use()/useOn() override until a
    // MenuProvider/Screen equivalent of CE's GUIScreenGuide exists.
}
