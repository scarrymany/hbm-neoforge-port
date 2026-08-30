package com.hbm.items.special;

import com.hbm.util.i18n.I18nUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Port of CE's {@code ItemBookLore} ({@code book_lore}): a player-writable lore book whose content
 * (key, cover/title tint, per-page string args) CE stored as raw NBT and read back through a fixed
 * GUI ({@code GUIBookLore}). Per docs/phase1/items_special.md findings 3 and 5, no menu/screen
 * framework exists yet to open into, so the item shell registers now with the NBT->component
 * conversion ({@link BookLoreContent} via {@link SpecialItemComponents#BOOK_LORE}) done ahead of
 * time; the menu-opening interaction is an explicit, documented gap.
 * <p>
 * Not ported: CE's {@code IDynamicModels} cover/title-tint baked-model retexturing (finding 6 - a
 * resource-pack/datagen concern, not registration code) and the color-handler tint indices it fed.
 */
public class ItemBookLore extends Item {

    public ItemBookLore(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BookLoreContent content = stack.get(SpecialItemComponents.BOOK_LORE.get());
        if (content == null || content.key().isEmpty()) {
            return;
        }
        String fullKey = "book_lore." + content.key() + ".author";
        String loc = I18nUtil.resolveKey(fullKey);
        if (!loc.equals(fullKey)) {
            tooltip.add(Component.translatable("book_lore.author", loc));
        }
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        BookLoreContent content = stack.get(SpecialItemComponents.BOOK_LORE.get());
        String key = (content == null || content.key().isEmpty()) ? "test" : content.key();
        return "item.hbm.book_lore." + key;
    }

    public static ItemStack createBook(Item bookLoreItem, String key, int pages, int coverColor, int titleColor) {
        ItemStack book = new ItemStack(bookLoreItem);
        List<List<String>> emptyPages = new ArrayList<>(pages);
        for (int i = 0; i < pages; i++) {
            emptyPages.add(Collections.emptyList());
        }
        book.set(SpecialItemComponents.BOOK_LORE.get(), new BookLoreContent(key, coverColor, titleColor, emptyPages));
        return book;
    }

    public static void setPageArgs(ItemStack book, int page, String... args) {
        BookLoreContent content = book.get(SpecialItemComponents.BOOK_LORE.get());
        if (content == null) {
            return;
        }
        book.set(SpecialItemComponents.BOOK_LORE.get(), content.withPageArgs(page, List.of(args)));
    }

    // Menu-opening interaction deferred - see class javadoc.
}
