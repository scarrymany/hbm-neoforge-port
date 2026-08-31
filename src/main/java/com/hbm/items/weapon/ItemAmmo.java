package com.hbm.items.weapon;

import com.hbm.items.ItemAmmoEnums.IAmmoItemEnum;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Port of CE's {@code com.hbm.items.weapon.ItemAmmo<E>} (see
 * upstream/hbm-ce/src/main/java/com/hbm/items/weapon/ItemAmmo.java) - a generic,
 * metadata-multiplexed {@code ItemEnumMulti<E>} subclass CE used to back its {@code ammo_shell}
 * (240mm tank shell), {@code ammo_fireext} (fire-extinguisher payload), and {@code ammo_misc}
 * families, plus the nested {@code AmmoItemTrait} gameplay-tag vocabulary those families' enum
 * constants (CE's {@code IAmmoItemEnum} implementors) carry a {@code Set} of (see CE
 * {@code ModItems.java:1961-1964} for the three real instantiations, all now mirrored by
 * {@link com.hbm.items.ItemAmmoEnums}'s {@code Ammo240Shell}/{@code AmmoFireExt}/{@code AmmoMisc}).
 * <p>
 * CE's generic {@code ItemEnumMulti<E>} base (a shared texture-atlas array plus per-damage-value
 * {@code ModelResourceLocation} baking, one registry item spanning several metadata-selected
 * variants) has no equivalent anywhere in this port and is not rebuilt here: 1.21.1 items carry no
 * metadata/damage-value axis at all. This port's established convention for every other CE
 * {@code ItemEnumMulti} family (see {@code ItemSoyuz}, {@code ItemTrain}, and
 * {@code com.hbm.items.weapon.grenade}'s shell/filling/fuze/extra cards, all flattening one CE
 * multi-metadata item into several standalone registered {@code Item}s) is followed here too: one
 * {@code ItemAmmo<E>} instance now represents exactly one already-selected {@code E} variant,
 * passed straight into the constructor rather than looked up off a stack's damage value - ready for
 * whichever later phase wires up flattened {@code ammo_shell_*}/{@code ammo_misc_*} registrations
 * the same way {@code GrenadeItems} did for its own metadata families.
 * <p>
 * Nothing in this port constructs an {@code ItemAmmo} yet - {@code ItemAmmoEnums.AmmoFireExt}'s
 * only real consumer, {@code XFactoryTool}'s fire-extinguisher ammo, already registers plain
 * flattened {@code Item}s (see {@code XFactoryTool#ITEM_FEXT_WATER}/{@code _FOAM}/{@code _SAND}),
 * bypassing this class entirely, and {@code ammo_shell}/{@code ammo_misc} have no registration site
 * in this port at all yet. This file exists so {@link AmmoItemTrait} below - CE's real
 * gameplay-trait vocabulary, referenced by {@code ItemAmmoEnums}'s {@code IAmmoItemEnum} constants
 * - has somewhere real to live, matching CE's own nesting under {@code ItemAmmo}; without it,
 * {@code com.hbm.items.ItemAmmoEnums} (a real, already-committed file) does not compile.
 */
public class ItemAmmo<E extends Enum<E> & IAmmoItemEnum> extends Item {

    private final E value;

    public ItemAmmo(Properties properties, E value) {
        super(properties);
        this.value = value;
    }

    public E getValue() {
        return value;
    }

    /** CE: {@code "item." + num.getInternalName()}, see {@code ItemAmmo#getTranslationKey}. */
    @Override
    public String getDescriptionId(ItemStack stack) {
        return "item." + value.getInternalName();
    }

    /**
     * Verbatim port of CE's {@code ItemAmmo.AmmoItemTrait} (80 constants) - the gameplay-effect tag
     * vocabulary CE's ammo-variant enums (e.g. {@code ItemAmmoEnums.AmmoFireExt}) attach via
     * {@code IAmmoItemEnum#getTraits()}. Pure data, no Minecraft/NeoForge types involved.
     */
    public enum AmmoItemTrait {
        CON_ACCURACY2,
        CON_DAMAGE,
        CON_HEAVY_WEAR,
        CON_LING_FIRE,
        CON_NN,
        CON_NO_DAMAGE,
        CON_NO_EXPLODE1,
        CON_NO_EXPLODE2,
        CON_NO_EXPLODE3,
        CON_NO_FIRE,
        CON_NO_MIRV,
        CON_NO_PROJECTILE,
        CON_PENETRATION,
        CON_RADIUS,
        CON_RANGE2,
        CON_SING_PROJECTILE,
        CON_SPEED,
        CON_SUPER_WEAR,
        CON_WEAR,
        NEU_40MM,
        NEU_BLANK,
        NEU_BOAT,
        NEU_BOXCAR,
        NEU_BUILDING,
        NEU_CHLOROPHYTE,
        NEU_ERASER,
        NEU_FUN,
        NEU_HEAVY_METAL,
        NEU_HOMING,
        NEU_JOLT,
        NEU_LESS_BOUNCY,
        NEU_MASKMAN_FLECHETTE,
        NEU_MASKMAN_METEORITE,
        NEU_MORE_BOUNCY,
        NEU_NO_BOUNCE,
        NEU_NO_CON,
        NEU_STARMETAL,
        NEU_TRACER,
        NEU_UHH,
        NEU_LEADBURSTER,
        NEU_WARCRIME1,
        NEU_WARCRIME2,
        PRO_ACCURATE1,
        PRO_ACCURATE2,
        PRO_BALEFIRE,
        PRO_BOMB_COUNT,
        PRO_CAUSTIC,
        PRO_CHAINSAW,
        PRO_CHLORINE,
        PRO_DAMAGE,
        PRO_DAMAGE_SLIGHT,
        PRO_EMP,
        PRO_EXPLOSIVE,
        PRO_FALLOUT,
        PRO_FIT_357,
        PRO_FLAMES,
        PRO_GRAVITY,
        PRO_HEAVY_DAMAGE,
        PRO_INCENDIARY,
        PRO_LUNATIC,
        PRO_MARAUDER,
        PRO_MINING,
        PRO_NO_GRAVITY,
        PRO_NUCLEAR,
        PRO_PENETRATION,
        PRO_PERCUSSION,
        PRO_PHOSPHORUS,
        PRO_PHOSPHORUS_SPLASH,
        PRO_POISON_GAS,
        PRO_RADIUS,
        PRO_RADIUS_HIGH,
        PRO_RANGE,
        PRO_ROCKET,
        PRO_ROCKET_PROPELLED,
        PRO_SHRAPNEL,
        PRO_SPEED,
        PRO_STUNNING,
        PRO_TOXIC,
        PRO_WEAR,
        PRO_WITHERING;

        public final String key;

        AmmoItemTrait() {
            this.key = "desc.item.ammo." + name().toLowerCase(Locale.US);
        }
    }
}
