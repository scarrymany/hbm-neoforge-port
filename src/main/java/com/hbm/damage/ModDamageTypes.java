package com.hbm.damage;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;

/**
 * Data-driven replacement for CE's {@code com.hbm.lib.ModDamageSource}. 1.21's DamageSource is final and can no
 * longer be subclassed, so every CE DamageSource singleton and every string id passed to
 * {@code EntityDamageSourceIndirect} becomes a datapack {@link DamageType} entry keyed here.
 *
 * The msgId passed to each {@link DamageType} is the exact original CE identifier string (used verbatim by CE for
 * its untranslated death messages), so any existing "death.attack.<msgId>" translations carry over unchanged. The
 * {@link ResourceKey} path is a snake_case form of that same id, since datapack registry paths cannot contain
 * uppercase letters.
 *
 * This interface only supplies the registry keys and their bootstrap. Flag-equivalent grouping (explosion,
 * projectile, bypasses armor, ...) lives in {@link com.hbm.damage.tags.ModDamageTypeTags} and
 * {@link com.hbm.damage.datagen.ModDamageTypeTagsProvider}. Building an actual {@code DamageSource} instance from one of
 * these keys is the responsibility of the area that owns the corresponding attack (entities/projectiles, weapons),
 * via {@code level.damageSources().source(key, ...)}.
 */
public interface ModDamageTypes {

    // CE never overrides 1.12.2 vanilla DamageSource's protected hungerDamage field, so every ModDamageSource
    // singleton silently inherits its default of 0.3F. That default, not the Neo Edition reference's 0.1F, is
    // what these DamageType entries need to match to preserve CE's actual hunger-exhaustion behavior.
    float DEFAULT_EXHAUSTION = 0.3F;

    // --- ModDamageSource static singleton fields -----------------------------------------------------------

    ResourceKey<DamageType> NUCLEAR_BLAST = key("nuclear_blast");
    ResourceKey<DamageType> BLAST = key("blast");
    ResourceKey<DamageType> MUD_POISONING = key("mud_poisoning");
    ResourceKey<DamageType> ACID = key("acid");
    ResourceKey<DamageType> EUTHANIZED_SELF = key("euthanized_self");
    ResourceKey<DamageType> EUTHANIZED_SELF_2 = key("euthanized_self_2");
    ResourceKey<DamageType> TAU_BLAST = key("tau_blast");
    ResourceKey<DamageType> DIGAMMA = key("digamma");
    ResourceKey<DamageType> RADIATION = key("radiation");
    ResourceKey<DamageType> SUICIDE = key("suicide");
    ResourceKey<DamageType> RUBBLE = key("rubble");
    ResourceKey<DamageType> SHRAPNEL = key("shrapnel");
    ResourceKey<DamageType> BLACK_HOLE = key("black_hole");
    /** CE field name is {@code turbofan}, but the CE DamageSource id string is "blender". */
    ResourceKey<DamageType> BLENDER = key("blender");
    ResourceKey<DamageType> METEORITE = key("meteorite");
    ResourceKey<DamageType> BOXCAR = key("boxcar");
    ResourceKey<DamageType> BOAT = key("boat");
    ResourceKey<DamageType> BUILDING = key("building");
    ResourceKey<DamageType> TAINT = key("taint");
    ResourceKey<DamageType> AMS = key("ams");
    ResourceKey<DamageType> AMS_CORE = key("ams_core");
    ResourceKey<DamageType> BROADCAST = key("broadcast");
    ResourceKey<DamageType> BANG = key("bang");
    ResourceKey<DamageType> PC = key("pc");
    ResourceKey<DamageType> CLOUD = key("cloud");
    ResourceKey<DamageType> LEAD = key("lead");
    ResourceKey<DamageType> ENERVATION = key("enervation");
    ResourceKey<DamageType> ELECTRICITY = key("electricity");
    ResourceKey<DamageType> EXHAUST = key("exhaust");
    ResourceKey<DamageType> SPIKES = key("spikes");
    ResourceKey<DamageType> LUNAR = key("lunar");
    ResourceKey<DamageType> SLICER = key("slicer");
    ResourceKey<DamageType> CRUCIBLE = key("crucible");
    ResourceKey<DamageType> MONOXIDE = key("monoxide");
    ResourceKey<DamageType> ASBESTOS = key("asbestos");
    ResourceKey<DamageType> BLACKLUNG = key("blacklung");
    ResourceKey<DamageType> MKU = key("mku");
    ResourceKey<DamageType> VACUUM = key("vacuum");
    ResourceKey<DamageType> OVERDOSE = key("overdose");
    ResourceKey<DamageType> MICROWAVE = key("microwave");
    ResourceKey<DamageType> NITAN = key("nitan");

    // --- ids only ever seen as EntityDamageSourceIndirect string literals in ModDamageSource's causeXxxDamage
    // factories. The DamageSource(holder, direct, causing) construction itself belongs to whichever area owns the
    // projectile entity (see the entities/projectiles area).

    ResourceKey<DamageType> REVOLVER_BULLET = key("revolver_bullet");
    ResourceKey<DamageType> GUN_GIB = key("gun_gib");
    ResourceKey<DamageType> CHOPPER_BULLET = key("chopper_bullet");
    ResourceKey<DamageType> TAU = key("tau");
    ResourceKey<DamageType> COMBINE_BALL = key("combine_ball");
    ResourceKey<DamageType> SUBATOMIC_1 = key("subatomic_1");
    ResourceKey<DamageType> SUBATOMIC_2 = key("subatomic_2");
    ResourceKey<DamageType> SUBATOMIC_3 = key("subatomic_3");
    ResourceKey<DamageType> SUBATOMIC_4 = key("subatomic_4");
    ResourceKey<DamageType> SUBATOMIC_5 = key("subatomic_5");
    ResourceKey<DamageType> EUTHANIZED = key("euthanized");
    ResourceKey<DamageType> ELECTRIFIED = key("electrified");
    ResourceKey<DamageType> FLAMETHROWER = key("flamethrower");
    ResourceKey<DamageType> PLASMA = key("plasma");
    ResourceKey<DamageType> ICE = key("ice");
    ResourceKey<DamageType> LASER = key("laser");

    // --- declared as string constants in ModDamageSource (s_boil, s_acid) but not consumed by any factory method
    // there; kept for whichever area (hazards/potions) turns out to need them. See report for detail.

    ResourceKey<DamageType> BOIL = key("boil");
    ResourceKey<DamageType> ACID_PLAYER = key("acid_player");

    // --- generic Sedna weapon-config damage categories. CE's DamageSourceSednaNoAttacker/WithAttacker had no
    // fixed set of types (any string was legal), but the weapons area's DamageClass enum replacement needs a
    // closed set of DamageType keys to switch over; these 8 mirror the Neo Edition reference 1:1. Prefixed with
    // SEDNA_ to avoid clashing with the CE-native LASER/MICROWAVE keys above, which are a different concept.

    ResourceKey<DamageType> SEDNA_PHYSICAL = key("sedna_physical");
    ResourceKey<DamageType> SEDNA_FIRE = key("sedna_fire");
    ResourceKey<DamageType> SEDNA_EXPLOSION = key("sedna_explosion");
    ResourceKey<DamageType> SEDNA_ELECTRIC = key("sedna_electric");
    ResourceKey<DamageType> SEDNA_LASER = key("sedna_laser");
    ResourceKey<DamageType> SEDNA_MICROWAVE = key("sedna_microwave");
    ResourceKey<DamageType> SEDNA_SUBATOMIC = key("sedna_subatomic");
    ResourceKey<DamageType> SEDNA_OTHER = key("sedna_other");

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    static void bootstrap(BootstrapContext<DamageType> context) {
        context.register(NUCLEAR_BLAST, new DamageType("nuclearBlast", DEFAULT_EXHAUSTION));
        context.register(BLAST, new DamageType("blast", DEFAULT_EXHAUSTION));
        context.register(MUD_POISONING, new DamageType("mudPoisoning", DEFAULT_EXHAUSTION));
        context.register(ACID, new DamageType("acid", DEFAULT_EXHAUSTION));
        context.register(EUTHANIZED_SELF, new DamageType("euthanizedSelf", DEFAULT_EXHAUSTION));
        context.register(EUTHANIZED_SELF_2, new DamageType("euthanizedSelf2", DEFAULT_EXHAUSTION));
        context.register(TAU_BLAST, new DamageType("tauBlast", DEFAULT_EXHAUSTION));
        context.register(DIGAMMA, new DamageType("digamma", DEFAULT_EXHAUSTION));
        context.register(RADIATION, new DamageType("radiation", DEFAULT_EXHAUSTION));
        context.register(SUICIDE, new DamageType("suicide", DEFAULT_EXHAUSTION));
        context.register(RUBBLE, new DamageType("rubble", DEFAULT_EXHAUSTION));
        context.register(SHRAPNEL, new DamageType("shrapnel", DEFAULT_EXHAUSTION));
        context.register(BLACK_HOLE, new DamageType("blackhole", DEFAULT_EXHAUSTION));
        context.register(BLENDER, new DamageType("blender", DEFAULT_EXHAUSTION));
        context.register(METEORITE, new DamageType("meteorite", DEFAULT_EXHAUSTION));
        context.register(BOXCAR, new DamageType("boxcar", DEFAULT_EXHAUSTION));
        context.register(BOAT, new DamageType("boat", DEFAULT_EXHAUSTION));
        context.register(BUILDING, new DamageType("building", DEFAULT_EXHAUSTION));
        context.register(TAINT, new DamageType("taint", DEFAULT_EXHAUSTION));
        context.register(AMS, new DamageType("ams", DEFAULT_EXHAUSTION));
        context.register(AMS_CORE, new DamageType("amsCore", DEFAULT_EXHAUSTION));
        context.register(BROADCAST, new DamageType("broadcast", DEFAULT_EXHAUSTION));
        context.register(BANG, new DamageType("bang", DEFAULT_EXHAUSTION));
        context.register(PC, new DamageType("pc", DEFAULT_EXHAUSTION));
        context.register(CLOUD, new DamageType("cloud", DEFAULT_EXHAUSTION));
        context.register(LEAD, new DamageType("lead", DEFAULT_EXHAUSTION));
        context.register(ENERVATION, new DamageType("enervation", DEFAULT_EXHAUSTION));
        context.register(ELECTRICITY, new DamageType("electricity", DEFAULT_EXHAUSTION));
        context.register(EXHAUST, new DamageType("exhaust", DEFAULT_EXHAUSTION));
        context.register(SPIKES, new DamageType("spikes", DEFAULT_EXHAUSTION));
        context.register(LUNAR, new DamageType("lunar", DEFAULT_EXHAUSTION));
        context.register(SLICER, new DamageType("slicer", DEFAULT_EXHAUSTION));
        context.register(CRUCIBLE, new DamageType("crucible", DEFAULT_EXHAUSTION));
        context.register(MONOXIDE, new DamageType("monoxide", DEFAULT_EXHAUSTION));
        context.register(ASBESTOS, new DamageType("asbestos", DEFAULT_EXHAUSTION));
        context.register(BLACKLUNG, new DamageType("blacklung", DEFAULT_EXHAUSTION));
        context.register(MKU, new DamageType("mku", DEFAULT_EXHAUSTION));
        context.register(VACUUM, new DamageType("vacuum", DEFAULT_EXHAUSTION));
        context.register(OVERDOSE, new DamageType("overdose", DEFAULT_EXHAUSTION));
        context.register(MICROWAVE, new DamageType("microwave", DEFAULT_EXHAUSTION));
        context.register(NITAN, new DamageType("nitan", DEFAULT_EXHAUSTION));

        context.register(REVOLVER_BULLET, new DamageType("revolverBullet", DEFAULT_EXHAUSTION));
        context.register(GUN_GIB, new DamageType("gunGib", DEFAULT_EXHAUSTION));
        context.register(CHOPPER_BULLET, new DamageType("chopperBullet", DEFAULT_EXHAUSTION));
        context.register(TAU, new DamageType("tau", DEFAULT_EXHAUSTION));
        context.register(COMBINE_BALL, new DamageType("cmb", DEFAULT_EXHAUSTION));
        context.register(SUBATOMIC_1, new DamageType("subAtomic1", DEFAULT_EXHAUSTION));
        context.register(SUBATOMIC_2, new DamageType("subAtomic2", DEFAULT_EXHAUSTION));
        context.register(SUBATOMIC_3, new DamageType("subAtomic3", DEFAULT_EXHAUSTION));
        context.register(SUBATOMIC_4, new DamageType("subAtomic4", DEFAULT_EXHAUSTION));
        context.register(SUBATOMIC_5, new DamageType("subAtomic5", DEFAULT_EXHAUSTION));
        context.register(EUTHANIZED, new DamageType("euthanized", DEFAULT_EXHAUSTION));
        context.register(ELECTRIFIED, new DamageType("electrified", DEFAULT_EXHAUSTION));
        context.register(FLAMETHROWER, new DamageType("flamethrower", DEFAULT_EXHAUSTION));
        context.register(PLASMA, new DamageType("plasma", DEFAULT_EXHAUSTION));
        context.register(ICE, new DamageType("ice", DEFAULT_EXHAUSTION));
        context.register(LASER, new DamageType("laser", DEFAULT_EXHAUSTION));

        context.register(BOIL, new DamageType("boil", DEFAULT_EXHAUSTION));
        context.register(ACID_PLAYER, new DamageType("acidPlayer", DEFAULT_EXHAUSTION));

        context.register(SEDNA_PHYSICAL, new DamageType("sednaPhysical", DEFAULT_EXHAUSTION));
        context.register(SEDNA_FIRE, new DamageType("sednaFire", DEFAULT_EXHAUSTION));
        context.register(SEDNA_EXPLOSION, new DamageType("sednaExplosion", DEFAULT_EXHAUSTION));
        context.register(SEDNA_ELECTRIC, new DamageType("sednaElectric", DEFAULT_EXHAUSTION));
        context.register(SEDNA_LASER, new DamageType("sednaLaser", DEFAULT_EXHAUSTION));
        context.register(SEDNA_MICROWAVE, new DamageType("sednaMicrowave", DEFAULT_EXHAUSTION));
        context.register(SEDNA_SUBATOMIC, new DamageType("sednaSubatomic", DEFAULT_EXHAUSTION));
        context.register(SEDNA_OTHER, new DamageType("sednaOther", DEFAULT_EXHAUSTION));
    }
}
