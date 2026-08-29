package com.hbm.util;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;

public final class EnumUtil {

    private EnumUtil() {
    }

    public static final InteractionHand[] HANDS = InteractionHand.values();
    public static final EquipmentSlot[] ENTITY_EQUIPMENT_SLOTS = EquipmentSlot.values();

    /**
     * @deprecated this creates a new E[] object on every call, use {@link #grabEnumSafely(Enum[], int)} instead.
     */
    @Deprecated(forRemoval = true, since = "1.5.1.1")
    public static <E extends Enum<E>> E grabEnumSafely(Class<E> theEnum, int index) {
        E[] values = theEnum.getEnumConstants();
        return values[Math.floorMod(index, values.length)];
    }

    public static <E extends Enum<E>> E grabEnumSafely(E[] values, int index) {
        return values[Math.floorMod(index, values.length)];
    }
}
