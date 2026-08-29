package com.hbm.damage.tags;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * Custom {@link DamageType} tags with no vanilla equivalent, needed to port flags and predicates from CE's
 * {@code com.hbm.lib.ModDamageSource} that {@code net.minecraft.tags.DamageTypeTags} does not cover.
 */
public interface ModDamageTypeTags {

    /**
     * Replacement for CE's {@code setDamageIsAbsolute()}. CE's flag only ever bypassed
     * {@code DamageResistanceHandler}'s own armor-value system - it never interacted with vanilla's Resistance
     * potion effect or other status-effect-based reduction - so this tag is populated independently rather than
     * composed from vanilla's {@code BYPASSES_EFFECTS}/{@code BYPASSES_RESISTANCE}, which would additionally grant
     * immunity to those vanilla mechanics.
     */
    TagKey<DamageType> ABSOLUTE = key("absolute");

    /** Replacement for {@code ModDamageSource.getIsTau(DamageSource)}. */
    TagKey<DamageType> IS_TAU = key("is_tau");

    /** Replacement for {@code ModDamageSource.getIsSubatomic(DamageSource)}. */
    TagKey<DamageType> IS_SUBATOMIC = key("is_subatomic");

    /**
     * Energy-weapon grouping. CE's 1.12.2 DamageSource had no such concept; membership (the four generic Sedna
     * energy categories) mirrors the Neo Edition reference's own {@code NtmDamageTypeTags.IS_ENERGY}.
     */
    TagKey<DamageType> IS_ENERGY = key("is_energy");

    private static TagKey<DamageType> key(String path) {
        return TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }
}
