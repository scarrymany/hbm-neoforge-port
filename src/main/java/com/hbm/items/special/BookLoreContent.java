package com.hbm.items.special;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * Replaces CE's {@code ItemBookLore} NBT shape (see docs/phase1/items_special.md finding 5):
 * {@code k} (lore key, String), {@code cov_col}/{@code tit_col} (cover/title tint, int), and one
 * {@code p1..pN} compound per page, each holding {@code a1..aN} string args. {@code pages} here is
 * the flattened equivalent, indexed 0-based (page 1's args at index 0), replacing CE's separate
 * {@code p} (page count) key with {@code pages.size()}.
 * <p>
 * Only the data shape is ported here; {@code ItemBookLore}'s GUI (CE's {@code GUIBookLore}) has no
 * ported menu/screen framework to open into yet (docs/phase1/items_special.md finding 3), so nothing
 * in the port reads this component back out today. It exists so the item shell can carry faithful
 * state once that framework lands, without inventing a second incompatible shape later.
 */
public record BookLoreContent(String key, int coverColor, int titleColor, List<List<String>> pages) {

    public static final BookLoreContent EMPTY = new BookLoreContent("", 0, 0, List.of());

    public BookLoreContent withPageArgs(int page, List<String> args) {
        if (page < 1 || page > pages.size()) {
            return this;
        }
        List<List<String>> updated = new java.util.ArrayList<>(pages);
        updated.set(page - 1, List.copyOf(args));
        return new BookLoreContent(key, coverColor, titleColor, updated);
    }

    public static final Codec<BookLoreContent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("key").forGetter(BookLoreContent::key),
            Codec.INT.fieldOf("cover_color").forGetter(BookLoreContent::coverColor),
            Codec.INT.fieldOf("title_color").forGetter(BookLoreContent::titleColor),
            Codec.STRING.listOf().listOf().fieldOf("pages").forGetter(BookLoreContent::pages)
    ).apply(instance, BookLoreContent::new));

    public static final StreamCodec<ByteBuf, BookLoreContent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, BookLoreContent::key,
            ByteBufCodecs.INT, BookLoreContent::coverColor,
            ByteBufCodecs.INT, BookLoreContent::titleColor,
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()), BookLoreContent::pages,
            BookLoreContent::new
    );
}
