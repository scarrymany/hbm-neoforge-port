package com.hbm.entity.grenade;

import com.hbm.items.weapon.ItemGenericGrenade;

/**
 * Port of CE's {@code com.hbm.entity.grenade.IGenericGrenade} marker interface - implemented by the
 * legacy grenade family's concrete entities so their shared {@code explode()} dispatch can look the
 * originating {@link ItemGenericGrenade} back up.
 */
public interface IGenericGrenade {

    ItemGenericGrenade getGrenade();
}
