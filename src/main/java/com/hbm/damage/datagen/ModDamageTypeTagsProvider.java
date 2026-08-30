package com.hbm.damage.datagen;

import com.hbm.damage.ModDamageTypes;
import com.hbm.damage.tags.ModDamageTypeTags;
import com.hbm.main.MainRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/**
 * Ports every builder flag from CE's {@code com.hbm.lib.ModDamageSource} (setExplosion, setDamageBypassesArmor,
 * setDamageIsAbsolute, setDamageAllowedInCreativeMode, setProjectile, setFireDamage) onto the matching vanilla or
 * custom {@link net.minecraft.world.damagesource.DamageType} tag for each key in {@link ModDamageTypes}.
 */
public class ModDamageTypeTagsProvider extends DamageTypeTagsProvider {

    public ModDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, @Nullable ExistingFileHelper helper) {
        super(output, provider, MainRegistry.MODID, helper);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(DamageTypeTags.IS_EXPLOSION)
                .add(
                        ModDamageTypes.NUCLEAR_BLAST,
                        ModDamageTypes.BLAST,
                        ModDamageTypes.TAU_BLAST,
                        ModDamageTypes.SEDNA_EXPLOSION
                );

        this.tag(DamageTypeTags.IS_FIRE)
                .add(
                        ModDamageTypes.FLAMETHROWER,
                        ModDamageTypes.SEDNA_FIRE
                );

        this.tag(DamageTypeTags.IS_PROJECTILE)
                .add(
                        ModDamageTypes.SUICIDE,
                        ModDamageTypes.RUBBLE,
                        ModDamageTypes.SHRAPNEL,
                        ModDamageTypes.REVOLVER_BULLET,
                        ModDamageTypes.GUN_GIB,
                        ModDamageTypes.CHOPPER_BULLET,
                        ModDamageTypes.TAU,
                        ModDamageTypes.COMBINE_BALL,
                        ModDamageTypes.SUBATOMIC_1,
                        ModDamageTypes.SUBATOMIC_2,
                        ModDamageTypes.SUBATOMIC_3,
                        ModDamageTypes.SUBATOMIC_4,
                        ModDamageTypes.SUBATOMIC_5,
                        // gun-framework ballistics core (BulletConfig.getDamage): matches CE's own
                        // BulletConfig.getDamage's `case PHYSICAL -> dmg.setProjectile();` - see
                        // docs/phase3/gun_framework.md.
                        ModDamageTypes.SEDNA_PHYSICAL
                );

        this.tag(DamageTypeTags.BYPASSES_ARMOR)
                .add(
                        ModDamageTypes.MUD_POISONING,
                        ModDamageTypes.EUTHANIZED_SELF,
                        ModDamageTypes.EUTHANIZED_SELF_2,
                        ModDamageTypes.TAU_BLAST,
                        ModDamageTypes.DIGAMMA,
                        ModDamageTypes.RADIATION,
                        ModDamageTypes.BLACK_HOLE,
                        ModDamageTypes.BLENDER,
                        ModDamageTypes.METEORITE,
                        ModDamageTypes.BOXCAR,
                        ModDamageTypes.BOAT,
                        ModDamageTypes.BUILDING,
                        ModDamageTypes.TAINT,
                        ModDamageTypes.AMS,
                        ModDamageTypes.AMS_CORE,
                        ModDamageTypes.BROADCAST,
                        ModDamageTypes.BANG,
                        ModDamageTypes.PC,
                        ModDamageTypes.CLOUD,
                        ModDamageTypes.LEAD,
                        ModDamageTypes.ENERVATION,
                        ModDamageTypes.ELECTRICITY,
                        ModDamageTypes.EXHAUST,
                        ModDamageTypes.SPIKES,
                        ModDamageTypes.LUNAR,
                        ModDamageTypes.SLICER,
                        ModDamageTypes.CRUCIBLE,
                        ModDamageTypes.MONOXIDE,
                        ModDamageTypes.ASBESTOS,
                        ModDamageTypes.BLACKLUNG,
                        ModDamageTypes.MKU,
                        ModDamageTypes.VACUUM,
                        ModDamageTypes.OVERDOSE,
                        ModDamageTypes.MICROWAVE,
                        ModDamageTypes.NITAN,
                        ModDamageTypes.TAU,
                        ModDamageTypes.COMBINE_BALL,
                        ModDamageTypes.SUBATOMIC_1,
                        ModDamageTypes.SUBATOMIC_2,
                        ModDamageTypes.SUBATOMIC_3,
                        ModDamageTypes.SUBATOMIC_4,
                        ModDamageTypes.SUBATOMIC_5,
                        ModDamageTypes.EUTHANIZED,
                        ModDamageTypes.ELECTRIFIED,
                        ModDamageTypes.FLAMETHROWER,
                        ModDamageTypes.PLASMA,
                        ModDamageTypes.ICE,
                        ModDamageTypes.LASER
                );

        this.tag(DamageTypeTags.BYPASSES_INVULNERABILITY)
                .add(
                        ModDamageTypes.DIGAMMA,
                        ModDamageTypes.AMS,
                        ModDamageTypes.NITAN
                );

        this.tag(ModDamageTypeTags.ABSOLUTE)
                .add(
                        ModDamageTypes.DIGAMMA,
                        ModDamageTypes.BLACK_HOLE,
                        ModDamageTypes.BLENDER,
                        ModDamageTypes.METEORITE,
                        ModDamageTypes.BOXCAR,
                        ModDamageTypes.BOAT,
                        ModDamageTypes.BUILDING,
                        ModDamageTypes.TAINT,
                        ModDamageTypes.AMS,
                        ModDamageTypes.AMS_CORE,
                        ModDamageTypes.BROADCAST,
                        ModDamageTypes.BANG,
                        ModDamageTypes.PC,
                        ModDamageTypes.CLOUD,
                        ModDamageTypes.LEAD,
                        ModDamageTypes.ENERVATION,
                        ModDamageTypes.ELECTRICITY,
                        ModDamageTypes.EXHAUST,
                        ModDamageTypes.LUNAR,
                        ModDamageTypes.SLICER,
                        ModDamageTypes.CRUCIBLE,
                        ModDamageTypes.MONOXIDE,
                        ModDamageTypes.ASBESTOS,
                        ModDamageTypes.BLACKLUNG,
                        ModDamageTypes.MKU,
                        ModDamageTypes.VACUUM,
                        ModDamageTypes.OVERDOSE,
                        ModDamageTypes.MICROWAVE,
                        ModDamageTypes.NITAN,
                        ModDamageTypes.TAU
                );

        this.tag(DamageTypeTags.BYPASSES_EFFECTS)
                .add(
                        ModDamageTypes.DIGAMMA,
                        ModDamageTypes.BLACK_HOLE,
                        ModDamageTypes.BLENDER,
                        ModDamageTypes.METEORITE,
                        ModDamageTypes.BOXCAR,
                        ModDamageTypes.BOAT,
                        ModDamageTypes.BUILDING,
                        ModDamageTypes.TAINT,
                        ModDamageTypes.AMS,
                        ModDamageTypes.AMS_CORE,
                        ModDamageTypes.BROADCAST,
                        ModDamageTypes.BANG,
                        ModDamageTypes.PC,
                        ModDamageTypes.CLOUD,
                        ModDamageTypes.LEAD,
                        ModDamageTypes.ENERVATION,
                        ModDamageTypes.ELECTRICITY,
                        ModDamageTypes.EXHAUST,
                        ModDamageTypes.LUNAR,
                        ModDamageTypes.SLICER,
                        ModDamageTypes.CRUCIBLE,
                        ModDamageTypes.MONOXIDE,
                        ModDamageTypes.ASBESTOS,
                        ModDamageTypes.BLACKLUNG,
                        ModDamageTypes.MKU,
                        ModDamageTypes.VACUUM,
                        ModDamageTypes.OVERDOSE,
                        ModDamageTypes.MICROWAVE,
                        ModDamageTypes.NITAN,
                        ModDamageTypes.TAU
                );

        this.tag(DamageTypeTags.BYPASSES_RESISTANCE)
                .addTag(DamageTypeTags.BYPASSES_EFFECTS);

        this.tag(ModDamageTypeTags.IS_TAU)
                .add(ModDamageTypes.TAU);

        this.tag(ModDamageTypeTags.IS_SUBATOMIC)
                .add(
                        ModDamageTypes.SUBATOMIC_1,
                        ModDamageTypes.SUBATOMIC_2,
                        ModDamageTypes.SUBATOMIC_3,
                        ModDamageTypes.SUBATOMIC_4,
                        ModDamageTypes.SUBATOMIC_5
                );

        this.tag(ModDamageTypeTags.IS_ENERGY)
                .add(
                        ModDamageTypes.SEDNA_LASER,
                        ModDamageTypes.SEDNA_MICROWAVE,
                        ModDamageTypes.SEDNA_SUBATOMIC,
                        ModDamageTypes.SEDNA_ELECTRIC,
                        // gun-framework ballistics core: SEDNA_PLASMA is a new ModDamageTypes entry
                        // this package adds (see this task's wiring snippet for ModDamageTypes.java) -
                        // grouped into IS_ENERGY alongside the other three energy-weapon Sedna damage
                        // classes for the same reason this tag already groups SEDNA_MICROWAVE in
                        // beyond CE's own dmgClass switch statement (see docs/phase3/gun_framework.md).
                        ModDamageTypes.SEDNA_PLASMA
                );
    }
}
