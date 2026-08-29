package com.hbm.interfaces;

import net.minecraft.world.entity.player.Player;

/**
 * Override AbstractContainerMenu#removed(Player) to listen close events.
 */
public interface IContainerOpenEventListener {
    void onContainerOpened(Player player);
}
