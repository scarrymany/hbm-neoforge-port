package com.hbm.entity.mob;

/**
 * CE: {@code com.hbm.entity.mob.IFlyingCreature} (10 lines).
 */
public interface IFlyingCreature {

    int STATE_WALKING = 0;
    int STATE_FLYING = 1;

    int getFlyingState();

    void setFlyingState(int state);
}
