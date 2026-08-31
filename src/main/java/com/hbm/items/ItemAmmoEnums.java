package com.hbm.items;

import com.google.common.collect.ImmutableSet;
import com.hbm.items.weapon.ItemAmmo;

import java.util.Set;

/**
 * Ported verbatim from CE - pure data, no Minecraft/NeoForge types involved.
 *
 * Depends on {@link com.hbm.items.weapon.ItemAmmo.AmmoItemTrait} - see that class's javadoc for how
 * this port represents CE's {@code ItemAmmo<E>}/{@code ItemEnumMulti<E>} metadata-multiplexed item
 * family.
 */
public class ItemAmmoEnums {

    public enum AmmoFireExt implements IAmmoItemEnum {
        WATER("ammo_fireext"),
        FOAM("ammo_fireext_foam"),
        SAND("ammo_fireext_sand");

        public static final AmmoFireExt[] VALUES = values();

        private final Set<ItemAmmo.AmmoItemTrait> traits;
        private final String unloc;

        AmmoFireExt(String unloc, ItemAmmo.AmmoItemTrait... traits) {
            this.traits = safeAssign(traits);
            this.unloc = unloc;
        }

        @Override
        public Set<ItemAmmo.AmmoItemTrait> getTraits() {
            return traits;
        }

        @Override
        public String getInternalName() {
            return unloc;
        }
    }

    public enum AmmoMisc implements IAmmoItemEnum {
        DGK("ammo_dgk");

        public static final AmmoMisc[] VALUES = values();

        private final Set<ItemAmmo.AmmoItemTrait> traits;
        private final String unloc;

        AmmoMisc(String unloc, ItemAmmo.AmmoItemTrait... traits) {
            this.traits = safeAssign(traits);
            this.unloc = unloc;
        }

        @Override
        public Set<ItemAmmo.AmmoItemTrait> getTraits() {
            return traits;
        }

        @Override
        public String getInternalName() {
            return unloc;
        }
    }

    public enum Ammo240Shell implements IAmmoItemEnum {
        STOCK("ammo_shell"),
        EXPLOSIVE("ammo_shell_explosive"),
        APFSDS_T("ammo_shell_apfsds_t"),
        APFSDS_DU("ammo_shell_apfsds_du"),
        W9("ammo_shell_w9");

        public static final Ammo240Shell[] VALUES = values();

        private final Set<ItemAmmo.AmmoItemTrait> traits;
        private final String unloc;

        Ammo240Shell(String unloc, ItemAmmo.AmmoItemTrait... traits) {
            this.traits = safeAssign(traits);
            this.unloc = unloc;
        }

        @Override
        public Set<ItemAmmo.AmmoItemTrait> getTraits() {
            return traits;
        }

        @Override
        public String getInternalName() {
            return unloc;
        }
    }

    public interface IAmmoItemEnum {
        Set<ItemAmmo.AmmoItemTrait> getTraits();
        String getInternalName();
    }

    static Set<ItemAmmo.AmmoItemTrait> safeAssign(ItemAmmo.AmmoItemTrait[] traits) {
        return traits == null ? ImmutableSet.of() : ImmutableSet.copyOf(traits);
    }
}
