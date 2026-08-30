package com.hbm.handler.ability;

import com.hbm.util.i18n.I18nUtil;

/**
 * Common contract for every tool/weapon ability singleton ({@link IToolAreaAbility},
 * {@link IToolHarvestAbility}, {@link IWeaponAbility}). Ported verbatim from CE - identical
 * behavior, only the translation lookup changed to this port's {@link I18nUtil} (confirmed
 * already present, client/server dispatching internally).
 */
public interface IBaseAbility extends Comparable<IBaseAbility> {

    String getName();

    default String getExtension(int level) {
        return "";
    }

    /** Client-only: server code should build a translatable component instead of calling this. */
    default String getFullName(int level) {
        return I18nUtil.resolveKey(getName()) + getExtension(level);
    }

    default boolean isAllowed() {
        return true;
    }

    /**
     * 1 means no support for levels (the level is always 0). Every {@code int level} parameter
     * elsewhere in the ability framework must be between 0 and {@code levels() - 1} inclusive.
     */
    default int levels() {
        return 1;
    }

    default int sortOrder() {
        return hashCode();
    }

    @Override
    default int compareTo(IBaseAbility o) {
        return sortOrder() - o.sortOrder();
    }
}
