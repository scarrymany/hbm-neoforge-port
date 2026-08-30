package com.hbm.items.armor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Ported unchanged from CE's {@code com.hbm.items.armor.IArmorDisableModel}: lets an equipped
 * armor piece hide a specific body part of the player model (e.g. a full-helmet piece hiding the
 * vanilla hair/hat overlay, or a full envirosuit hiding the arms so its own model can render in
 * their place). Implemented by {@link com.hbm.items.gear.ArmorFSB}; consumed by the player-render
 * layer that decides which vanilla body parts to skip - that render layer is Phase 5 (Client &amp;
 * UX) per this port's phase boundary (see {@code docs/phase3/armor_equippable_framework.md}'s
 * Deferred scope), this interface only needs to exist and compile for now.
 */
public interface IArmorDisableModel {

    boolean disablesPart(Player player, ItemStack stack, EnumPlayerPart part);

    enum EnumPlayerPart {
        HEAD,
        HAT,
        BODY,
        LEFT_ARM,
        RIGHT_ARM,
        LEFT_LEG,
        RIGHT_LEG;

        public static final EnumPlayerPart[] VALUES = values();
    }
}
