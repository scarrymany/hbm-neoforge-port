package com.hbm.interfaces;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface IAnimatedDoor extends IDoor {

    @OnlyIn(Dist.CLIENT)
    void handleNewState(DoorState state);

    /**
     * Returns a fresh animation-start timestamp when crossing from a stationary
     * into a moving state; otherwise returns the current sysTime unchanged.
     */
    static long clientAnimStart(DoorState oldState, DoorState newState, long currentSysTime) {
        return oldState != newState && oldState.isStationaryState() && newState.isMovingState()
                ? System.currentTimeMillis()
                : currentSysTime;
    }
}
