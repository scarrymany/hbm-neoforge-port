package com.hbm.itempool;

/**
 * Port of CE's {@code com.hbm.itempool.ItemPoolsRedRoom} (SHA {@code 293649fc}). Pedestal / black-slab
 * / black-part pools. {@code item_secret} meta (FOLLY / SELENIUM_STEEL / CONTROLLER / CANISTER)
 * skipped — no discrete secret items in this port.
 */
public final class ItemPoolsRedRoom {

    public static final String POOL_RED_PEDESTAL = "POOL_RED_PEDESTAL";
    public static final String POOL_BLACK_SLAB = "POOL_BLACK_SLAB";
    public static final String POOL_BLACK_PART = "POOL_BLACK_PART";

    private ItemPoolsRedRoom() {
    }

    public static void init() {
        ItemPool pedestal = new ItemPool(POOL_RED_PEDESTAL);
        ItemPoolLookups.add(pedestal, "armor_polish", 1, 1, 10);
        ItemPoolLookups.add(pedestal, "bandaid", 1, 1, 10);
        ItemPoolLookups.add(pedestal, "serum", 1, 1, 10);
        ItemPoolLookups.add(pedestal, "quartz_plutonium", 1, 1, 10);
        ItemPoolLookups.add(pedestal, "morning_glory", 1, 1, 10);
        ItemPoolLookups.add(pedestal, "spider_milk", 1, 1, 10);
        ItemPoolLookups.add(pedestal, "ink", 1, 1, 10);
        ItemPoolLookups.add(pedestal, "heart_container", 1, 1, 10);
        ItemPoolLookups.add(pedestal, "black_diamond", 1, 1, 10);
        ItemPoolLookups.add(pedestal, "scrumpy", 1, 1, 10);
        ItemPoolLookups.add(pedestal, "wild_p", 1, 1, 5);
        ItemPoolLookups.add(pedestal, "ballistic_gauntlet", 1, 1, 10);
        ItemPoolLookups.add(pedestal, "card_aos", 1, 1, 5);
        ItemPoolLookups.add(pedestal, "card_qos", 1, 1, 5);
        ItemPoolLookups.add(pedestal, "starmetal_sword", 1, 1, 5);
        ItemPoolLookups.add(pedestal, "gem_alexandrite", 1, 1, 5);
        ItemPoolLookups.add(pedestal, "crackpipe", 1, 1, 5);
        ItemPoolLookups.add(pedestal, "flask_infusion", 1, 1, 5);
        ItemPoolLookups.add(pedestal, "boxcar", 1, 1, 5);
        ItemPoolLookups.add(pedestal, "book_of_", 1, 1, 5);
        ItemPoolLookups.add(pedestal, "gun_hangman", 1, 1, 1);
        ItemPoolLookups.add(pedestal, "gun_mas36", 1, 1, 1);
        // SKIPPED: item_secret FOLLY

        ItemPool slab = new ItemPool(POOL_BLACK_SLAB);
        ItemPoolLookups.add(slab, "clay_tablet", 1, 1, 10);

        ItemPool part = new ItemPool(POOL_BLACK_PART);
        // SKIPPED: item_secret SELENIUM_STEEL / CONTROLLER / CANISTER — no discrete ids
        ItemPoolLookups.add(part, "ingot_selenium", 4, 4, 10);
    }
}
