package com.hbm.api.rbmk;

import net.minecraft.world.item.ItemStack;

/**
 * Implemented by any RBMK column that can be loaded/unloaded with an {@link ItemStack} by non-GUI
 * code (a crane, an autoloader, redstone-over-radio, etc) - a fuel rod loading fuel, a control rod
 * loading its rod item, and so on. CE: {@code com.hbm.tileentity.machine.rbmk.IRBMKLoadable}
 * (33 lines, read in full), unchanged - zero real dependency beyond {@link ItemStack} itself,
 * trivially portable.
 */
public interface IRBMKLoadable {

    /**
     * @param toLoad the ItemStack that should be loaded
     * @return TRUE if the provided ItemStack can be inserted into the column
     */
    boolean canLoad(ItemStack toLoad);

    /**
     * Loads the given ItemStack. Callers must check {@link #canLoad} first.
     */
    void load(ItemStack toLoad);

    /**
     * @return TRUE if the column contains an ItemStack that can be unloaded
     */
    boolean canUnload();

    /**
     * @return The next ItemStack to be unloaded
     */
    ItemStack provideNext();

    /**
     * Removes the next ItemStack as part of the unloading process.
     */
    void unload();
}
