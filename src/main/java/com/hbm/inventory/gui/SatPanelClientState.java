package com.hbm.inventory.gui;

import com.hbm.packet.toclient.SatPanelPayload;

import javax.annotation.Nullable;

/**
 * Client-side mailbox for the latest {@link SatPanelPayload} received while
 * {@link com.hbm.items.tool.ItemSatInterface} is the active hotbar item. {@link SatInterfaceScreen}/
 * {@link SatCoordScreen} poll this every frame rather than holding their own connection - matches
 * CE's own {@code ItemSatInterface.currentSat} static client field (a plain static mailbox, not a
 * push-to-screen callback), just re-typed onto the new payload record.
 */
public final class SatPanelClientState {

    @Nullable
    public static volatile SatPanelPayload LATEST;

    private SatPanelClientState() {
    }
}
