package com.hbm.items.machine;

/**
 * The 8 "printing" stamp dies. CE reused {@link ItemStamp}'s metadata slot for
 * {@code PRINTING1}..{@code PRINTING8} on a single {@code stamp_book} registry entry, resolving the
 * concrete {@link ItemStamp.StampType} from the stack's damage value; each printing type is its
 * own registered instance here instead, one per {@link ItemStamp.StampType#PRINTING1}..
 * {@link ItemStamp.StampType#PRINTING8}.
 */
public class ItemStampBook extends ItemStamp {

    public ItemStampBook(StampType type, Properties properties) {
        super(type, properties);
    }
}
