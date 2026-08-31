package com.hbm.itempool;

/**
 * Port of CE's {@code com.hbm.itempool.ItemPoolsVendingMachine} (SHA {@code 565968ad}).
 * {@code canned_conserve} BEEF/TUBE remapped to discrete conserve ids if registered.
 */
public final class ItemPoolsVendingMachine {

    public static final String POOL_SODA = "POOL_SODA";
    public static final String POOL_SNACKS = "POOL_SNACKS";

    private ItemPoolsVendingMachine() {
    }

    public static void init() {
        ItemPool soda = new ItemPool(POOL_SODA);
        ItemPoolLookups.add(soda, "bottle_nuka", 1, 1, 10);
        ItemPoolLookups.add(soda, "bottle_cherry", 1, 1, 5);
        ItemPoolLookups.add(soda, "bottle_quantum", 1, 1, 1);
        ItemPoolLookups.add(soda, "can_bepis", 1, 1, 10);
        ItemPoolLookups.add(soda, "can_luna", 1, 1, 10);
        ItemPoolLookups.add(soda, "can_mug", 1, 1, 10);
        ItemPoolLookups.add(soda, "can_breen", 1, 1, 1);

        ItemPool snacks = new ItemPool(POOL_SNACKS);
        ItemPoolLookups.add(snacks, "definitelyfood", 1, 1, 10);
        ItemPoolLookups.add(snacks, "canned_conserve_beef", 1, 1, 5);
        ItemPoolLookups.add(snacks, "canned_conserve_tube", 1, 1, 5);
        ItemPoolLookups.add(snacks, "twinkie", 1, 1, 10);
        ItemPoolLookups.add(snacks, "chocolate", 1, 1, 10);
    }
}
