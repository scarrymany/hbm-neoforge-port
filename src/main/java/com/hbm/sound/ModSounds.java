package com.hbm.sound;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * NeoForge {@link SoundEvent} registry, ported from CE's {@code com.hbm.lib.HBMSoundHandler}
 * (which itself carried a "TODO: rename to NTMSounds" note - this class is that rename).
 * <p>
 * CE registered ids through a hand-rolled {@code Object2ObjectLinkedOpenHashMap} populated by
 * a {@code register(String)} helper and flushed into the vanilla registry from a
 * {@code RegistryEvent.Register<SoundEvent>} handler in {@code main.ModEventHandler}. NeoForge
 * has no such deferred registration event, so every id below is a {@link DeferredHolder} created
 * through {@link #reg(String)} and registered eagerly with {@link #SOUND_EVENTS}; call
 * {@link #register(IEventBus)} from the mod's constructor to hook it into the mod event bus.
 * <p>
 * <b>Id casing:</b> {@link ResourceLocation} paths must be all-lowercase in 1.21.1
 * (CE's mixed-case paths, e.g. {@code "alarm.amsSiren"}, are illegal). Every id here is CE's
 * original id lowercased with no other structural change (no re-segmenting into snake_case,
 * unlike the Neo Edition reference project, which renamed many ids outright and must not be
 * copied here). The later asset-port phase must ship {@code sounds.json} entries and
 * {@code .ogg} files under these exact lowercase paths, not CE's original mixed-case ones.
 * <p>
 * Sounds are not yet wired to any block/item/entity - that lands with each content area as it
 * is ported. {@link #GEIGER_SOUNDS}, {@link #VOICE_SOUNDS} and {@link #BOILER_GROAN_SOUNDS}
 * mirror CE's {@code geigerSounds}/{@code voiceSounds}/{@code boilerGroanSounds} arrays; resolve
 * an entry with {@code .get()} only after the mod event bus has fired the registration event
 * (i.e. not during static init of another class).
 * <p>
 * <b>Known design gap - dynamic sound ids:</b> CE's {@code HBMSoundHandler.getOrCreate(ResourceLocation)}
 * lets code register a brand-new {@code SoundEvent} at runtime, which {@code config.CassetteJsonConfig}
 * relies on to mint a {@code SoundEvent} for every user-authored cassette entry found while reading its
 * config file. This class deliberately has no equivalent: NeoForge's {@link DeferredRegister} can only
 * queue registrations up to the point its {@link net.neoforged.neoforge.registries.RegisterEvent} fires
 * on the mod event bus, so ids that are not known statically at that point cannot be added later the way
 * CE's mutable {@code ALL_SOUNDS} map allowed. Porting the cassette/config area (a later phase) must design a
 * real replacement instead of reviving this pattern - for example, reading the config early enough (e.g.
 * from a {@code FMLCommonSetupEvent}-adjacent config-load step) to register every discovered id through
 * {@link #SOUND_EVENTS} before {@code RegisterEvent} fires, or shipping a fixed pool of pre-registered
 * cassette slots that the config assigns ids into. This file intentionally leaves that redesign to the
 * cassette/config area rather than stubbing it here.
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MainRegistry.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_HATCH = reg("alarm.hatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_AUTOPILOT = reg("alarm.autopilot");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_AMSSIREN = reg("alarm.amssiren");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_BLAST_DOOR = reg("alarm.blastdooralarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_APCLOOP = reg("alarm.apcloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_KLAXON = reg("alarm.klaxon");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_FO_KLAXON_A = reg("alarm.foklaxona");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_FO_KLAXON_B = reg("alarm.foklaxonb");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_REGULAR = reg("alarm.regularsiren");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_CLASSIC = reg("alarm.classic");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_BANK = reg("alarm.bankalarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_BEEP = reg("alarm.beepsiren");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_CONTAINER = reg("alarm.containeralarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_SWEEP = reg("alarm.sweepsiren");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_STRIDER = reg("alarm.stridersiren");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_AIR_RAID = reg("alarm.airraid");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_NOSTROMO = reg("alarm.nostromosiren");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_EAS = reg("alarm.easalarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_APCPASS = reg("alarm.apcpass");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_RAZOR_TRAIN = reg("alarm.razortrainhorn");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOYUZED = reg("alarm.soyuzed");
    public static final DeferredHolder<SoundEvent, SoundEvent> METAL_STEP = reg("step.metal");
    public static final DeferredHolder<SoundEvent, SoundEvent> IRON = reg("step.iron");
    public static final DeferredHolder<SoundEvent, SoundEvent> IRON_LAND = reg("step.iron_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> IRON_JUMP = reg("step.iron_jump");
    public static final DeferredHolder<SoundEvent, SoundEvent> POWERED_STEP = reg("step.powered");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAMBDA_CORE = reg("music.recordlambdacore");
    public static final DeferredHolder<SoundEvent, SoundEvent> SECTOR_SWEEP = reg("music.recordsectorsweep");
    public static final DeferredHolder<SoundEvent, SoundEvent> VORTAL_COMBAT = reg("music.recordvortalcombat");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLASS = reg("music.transmission");
    public static final DeferredHolder<SoundEvent, SoundEvent> METAL_BLOCK = reg("step.metalblock");
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_SMALL_NEAR = reg("weapon.explosion_small_near");
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_SMALL_FAR = reg("weapon.explosion_small_far");
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_LARGE_NEAR = reg("weapon.explosion_large_near");
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_LARGE_FAR = reg("weapon.explosion_large_far");
    public static final DeferredHolder<SoundEvent, SoundEvent> FEL = reg("block.fel");
    public static final DeferredHolder<SoundEvent, SoundEvent> FENSU_HUM = reg("block.fensuhum");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEPHAESTUS_RUNNING = reg("block.hephaestusrunning");
    public static final DeferredHolder<SoundEvent, SoundEvent> METEORITE_FALLING_LOOP = reg("entity.meteoritefallingloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> PRESS_OPERATE = reg("block.pressoperate");
    public static final DeferredHolder<SoundEvent, SoundEvent> LASER_BANG = reg("weapon.laserbang");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_DEBRIS = reg("block.debris");
    public static final DeferredHolder<SoundEvent, SoundEvent> RBMK_LID = reg("block.rbmklid");
    public static final DeferredHolder<SoundEvent, SoundEvent> SYRINGE_USE = reg("item.syringe");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPARK_SHOOT = reg("weapon.sparkshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEVER_START = reg("block.leverstart");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEVER_STOP = reg("block.leverstop");
    public static final DeferredHolder<SoundEvent, SoundEvent> METAL_IMPACT = reg("block.metalimpact");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPARK = reg("block.spark");
    public static final DeferredHolder<SoundEvent, SoundEvent> B92_RELOAD = reg("weapon.b92reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> TECH_BLEEP = reg("item.techbleep");
    public static final DeferredHolder<SoundEvent, SoundEvent> TECH_BOOP = reg("item.techboop");
    public static final DeferredHolder<SoundEvent, SoundEvent> HORN_NEAR_SINGLE = reg("block.hornnearsingle");
    public static final DeferredHolder<SoundEvent, SoundEvent> LARGE_TURBINE_RUNNING = reg("block.largeturbine");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENGINE = reg("block.engine");
    public static final DeferredHolder<SoundEvent, SoundEvent> HORN_NEAR_DUAL = reg("block.hornneardual");
    public static final DeferredHolder<SoundEvent, SoundEvent> HORN_FAR_SINGLE = reg("block.hornfarsingle");
    public static final DeferredHolder<SoundEvent, SoundEvent> HORN_FAR_DUAL = reg("block.hornfardual");
    public static final DeferredHolder<SoundEvent, SoundEvent> REACTOR_LOOP = reg("block.reactorloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> REACTOR_START = reg("block.reactorstart");
    public static final DeferredHolder<SoundEvent, SoundEvent> REACTOR_STOP = reg("block.reactorstop");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHEMICAL_PLANT = reg("block.chemicalplant");
    public static final DeferredHolder<SoundEvent, SoundEvent> POTATOS_RANDOM = reg("potatos.random");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_SPIN_DOWN = reg("weapon.spindown");
    public static final DeferredHolder<SoundEvent, SoundEvent> WEAPON_SPIN_UP = reg("weapon.spinup");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAW_SHOOT = reg("weapon.sawshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> RPG_SHOOT = reg("weapon.rpgshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> RELOAD_TURRET = reg("weapon.reloadturret");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFLE_SHOOT = reg("weapon.rifleshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEFAB_SHOOT = reg("weapon.defabshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLAMETHROWER_IGNITE = reg("weapon.flamethrowerignite");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLAMETHROWER_SHOOT = reg("weapon.flamethrowershoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> TAU_SHOOT = reg("weapon.taushoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> OLD_EXPLOSION = reg("entity.oldexplosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> NUCLEAR_EXPLOSION = reg("weapon.nuclearexplosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROBIN_EXPLOSION = reg("weapon.robin_explosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOILER = reg("block.boiler");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOILER_GROAN1 = reg("block.boilergroan0");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOILER_GROAN2 = reg("block.boilergroan1");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOILER_GROAN3 = reg("block.boilergroan2");
    public static final DeferredHolder<SoundEvent, SoundEvent> CIWS_SPINDOWN = reg("weapon.ciwsspindown");
    public static final DeferredHolder<SoundEvent, SoundEvent> CIWS_SPINUP = reg("weapon.ciwsspinup");
    public static final DeferredHolder<SoundEvent, SoundEvent> CIWS_FIRING_LOOP = reg("weapon.ciwsfiringloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> WARN_OVERSPEED = reg("block.warnoverspeed");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLANE_SHOT_DOWN = reg("entity.planeshotdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOMB_WHISTLE = reg("entity.bombwhistle");
    public static final DeferredHolder<SoundEvent, SoundEvent> MORTAR_WHISTLE = reg("entity.mortarwhistle");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLANE_CRASH = reg("entity.planecrash");
    public static final DeferredHolder<SoundEvent, SoundEvent> MISSILE_TAKEOFF = reg("weapon.missiletakeoff");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOMBER_SMALL_LOOP = reg("entity.bombersmallloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOMBER_LOOP = reg("entity.bomberloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> STINGER_LOCKON = reg("weapon.stingerlockon");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRAIN_HORN = reg("alarm.trainhorn");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOMB_DET = reg("entity.bombdet");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUMBLE = reg("misc.rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIPE_FAIL = reg("entity.pipefail");
    public static final DeferredHolder<SoundEvent, SoundEvent> LPWSTART = reg("misc.lpwstart");
    public static final DeferredHolder<SoundEvent, SoundEvent> LPWSTOP = reg("misc.lpwstop");
    public static final DeferredHolder<SoundEvent, SoundEvent> LPWLOOP = reg("misc.lpwloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> HTRSTART = reg("misc.htrstart");
    public static final DeferredHolder<SoundEvent, SoundEvent> HTRSTOP = reg("misc.htrstop");
    public static final DeferredHolder<SoundEvent, SoundEvent> HTRLOOP = reg("misc.htrloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_TAKEOFF = reg("entity.rockettakeoff");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_IGNITION = reg("entity.rocketignition");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_FLY_LIGHT = reg("entity.rocketflylight");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_FLY_HEAVY = reg("entity.rocketflyheavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENCER_SHOOT = reg("weapon.silencershoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> RPG_RELOAD = reg("weapon.rpgreload");
    public static final DeferredHolder<SoundEvent, SoundEvent> RELOAD_GRENADE = reg("weapon.hkreload");
    public static final DeferredHolder<SoundEvent, SoundEvent> RELOAD_SHOTGUN = reg("weapon.shotgunreload");
    public static final DeferredHolder<SoundEvent, SoundEvent> RELOAD_MAG = reg("weapon.magreload");
    public static final DeferredHolder<SoundEvent, SoundEvent> RELOAD_REVOLVER = reg("weapon.revolverreload");
    public static final DeferredHolder<SoundEvent, SoundEvent> FATMAN_RELOAD = reg("weapon.fatmanreload");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOAT_WEAPON = reg("weapon.boat");
    public static final DeferredHolder<SoundEvent, SoundEvent> RICOCHET = reg("weapon.ricochet");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRENADE_BOUNCE = reg("weapon.gbounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM_GAMBIT = reg("alarm.gambit");
    public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER_SHOOT = reg("weapon.revolvershoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEAVY_SHOOT = reg("weapon.heavyshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCHRABIDIUM_SHOOT = reg("weapon.schrabidiumshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER_SHOOT_ALT = reg("weapon.revolvershootalt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HK_SHOOT = reg("weapon.hkshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOTGUN_SHOOT = reg("weapon.shotgunshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOTTY_SHOOT = reg("weapon.shottyshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> UZI_SHOOT = reg("weapon.uzishoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAL_SHOOT = reg("weapon.calshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> LACUNAE_SHOOT = reg("weapon.lacunaeshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> FATMAN_SHOOT = reg("weapon.fatmanshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> OSIPR_SHOOT = reg("weapon.osiprshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> ZOMG_SHOOT = reg("weapon.zomgshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> JETPACK_TANK = reg("item.jetpacktank");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWITCHMODE1 = reg("weapon.switchmode1");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWITCHMODE2 = reg("weapon.switchmode2");
    public static final DeferredHolder<SoundEvent, SoundEvent> NULL_TAU = reg("misc.nulltau");
    public static final DeferredHolder<SoundEvent, SoundEvent> NULL_RADAR = reg("misc.nullradar");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMMOLATOR_IGNITE = reg("weapon.immolatorignite");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMMOLATOR_SHOOT = reg("weapon.immolatorshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> DEFAB_SPINUP = reg("weapon.defabspinup");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYOLATOR_SHOOT = reg("weapon.cryolatorshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> SING_FLYBY = reg("weapon.singflyby");
    public static final DeferredHolder<SoundEvent, SoundEvent> OSIPR_CHARGING = reg("weapon.osiprcharging");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEVER_ACTION_RELOAD = reg("weapon.leveractionreload");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOLLY_OPEN = reg("weapon.follyopen");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOLLY_RELOAD = reg("weapon.follyreload");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOLLY_CLOSE = reg("weapon.follyclose");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOLLY_FIRE = reg("weapon.follyfire");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOLLY_BUZZER = reg("weapon.follybuzzer");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOLLY_AQUIRED = reg("weapon.follyaquired");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHOPPER_DROP = reg("entity.chopperdrop");
    public static final DeferredHolder<SoundEvent, SoundEvent> PYRO_OPERATE = reg("block.pyrooperate");
    public static final DeferredHolder<SoundEvent, SoundEvent> ELECTRIC_HUM = reg("block.electrichum");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRATE_BREAK = reg("block.cratebreak");
    public static final DeferredHolder<SoundEvent, SoundEvent> ITEM_UNPACK = reg("item.unpack");
    public static final DeferredHolder<SoundEvent, SoundEvent> CENTRIFUGE_OPERATE = reg("block.centrifugeoperate");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUTTON_NO = reg("block.buttonno");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUTTON_YES = reg("block.buttonyes");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAILGUN_FIRE = reg("block.railgunfire");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAILGUN_ORIENTATION = reg("block.railgunorientation");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAILGUN_CHARGE = reg("block.railguncharge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHUTDOWN = reg("block.shutdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROADCAST1 = reg("block.broadcast1");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROADCAST2 = reg("block.broadcast2");
    public static final DeferredHolder<SoundEvent, SoundEvent> BROADCAST3 = reg("block.broadcast3");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER1 = reg("item.geiger1");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER2 = reg("item.geiger2");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER3 = reg("item.geiger3");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER4 = reg("item.geiger4");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER5 = reg("item.geiger5");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEIGER6 = reg("item.geiger6");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICES1 = reg("item.voices1");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICES2 = reg("item.voices2");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICES3 = reg("item.voices3");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICES4 = reg("item.voices4");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICES5 = reg("item.voices5");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICES6 = reg("item.voices6");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICES7 = reg("item.voices7");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOICES8 = reg("item.voices8");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOCK_OPEN = reg("block.lockopen");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIN_BREAK = reg("item.pinbreak");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIN_UNLOCK = reg("item.pinunlock");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOCK_HANG = reg("block.lockhang");
    public static final DeferredHolder<SoundEvent, SoundEvent> VAULT_SCRAPE_NEW = reg("block.vaultscrapenew");
    public static final DeferredHolder<SoundEvent, SoundEvent> VAULT_THUD_NEW = reg("block.vaultthudnew");
    public static final DeferredHolder<SoundEvent, SoundEvent> MISSILE_ASSEMBLY2 = reg("block.missileassembly2");
    public static final DeferredHolder<SoundEvent, SoundEvent> SONAR_PING = reg("block.sonarping");
    public static final DeferredHolder<SoundEvent, SoundEvent> RADAWAY_USE = reg("item.radaway");
    public static final DeferredHolder<SoundEvent, SoundEvent> GASMASK_SCREW = reg("item.gasmaskscrew");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPRAY = reg("item.spray");
    public static final DeferredHolder<SoundEvent, SoundEvent> REPAIR = reg("item.repair");
    public static final DeferredHolder<SoundEvent, SoundEvent> NULL_CHOPPER = reg("misc.nullchopper");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHOPPER_CHARGE = reg("entity.choppercharge");
    public static final DeferredHolder<SoundEvent, SoundEvent> NULL_CRASHING = reg("misc.nullcrashing");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHOPPER_DAMAGE = reg("entity.chopperdamage");
    public static final DeferredHolder<SoundEvent, SoundEvent> NULL_MINE = reg("misc.nullmine");
    public static final DeferredHolder<SoundEvent, SoundEvent> OPEN_DOOR = reg("block.opendoor");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLOSE_DOOR = reg("block.closedoor");
    public static final DeferredHolder<SoundEvent, SoundEvent> OPEN_C = reg("block.openc");
    public static final DeferredHolder<SoundEvent, SoundEvent> CLOSE_C = reg("block.closec");
    public static final DeferredHolder<SoundEvent, SoundEvent> STEAM_ENGINE_OPERATE = reg("block.steamengineoperate");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOLTGUN = reg("item.boltgun");
    public static final DeferredHolder<SoundEvent, SoundEvent> BANG = reg("weapon.bang");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLICE = reg("weapon.slice");
    public static final DeferredHolder<SoundEvent, SoundEvent> KAPING = reg("weapon.kapeng");
    public static final DeferredHolder<SoundEvent, SoundEvent> PIPE_PLACED = reg("block.pipeplaced");
    public static final DeferredHolder<SoundEvent, SoundEvent> TESLA = reg("weapon.tesla");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYBERCRAB = reg("entity.cybercrab");
    public static final DeferredHolder<SoundEvent, SoundEvent> OSIPR_RELOAD = reg("weapon.osiprreload");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOYUZ_READY = reg("block.soyuzready");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOYUZ_TAKE_OFF = reg("entity.soyuztakeoff");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME = reg("alarm.chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> TAU_CHARGE_LOOP2 = reg("weapon.tauchargeloop2");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHOPPER_FLYING_LOOP = reg("entity.chopperflyingloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHOPPER_CRASHING_LOOP = reg("entity.choppercrashingloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHOPPER_MINE_LOOP = reg("entity.choppermineloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> LACUNAE_SPINUP = reg("weapon.lacunaespinup");
    public static final DeferredHolder<SoundEvent, SoundEvent> LACUNAE_SPINDOWN = reg("weapon.lacunaespindown");
    public static final DeferredHolder<SoundEvent, SoundEvent> TESLA_SHOOT = reg("weapon.teslashoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> STOP = reg("weapon.stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> BONK = reg("weapon.bonk");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLAUNCHER = reg("weapon.glauncher");
    public static final DeferredHolder<SoundEvent, SoundEvent> HKS_SHOOT = reg("weapon.hksshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> VICE = reg("item.vice");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCREM = reg("block.screm");
    public static final DeferredHolder<SoundEvent, SoundEvent> UPGRADE_PLUG = reg("item.upgradeplug");
    public static final DeferredHolder<SoundEvent, SoundEvent> TAU_CHARGE_LOOP = reg("weapon.tauchargeloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> QUADRO_RELOAD = reg("weapon.quadroreload");
    public static final DeferredHolder<SoundEvent, SoundEvent> FSTBMB_START = reg("weapon.fstbmbstart");
    public static final DeferredHolder<SoundEvent, SoundEvent> FSTBMB_PING = reg("weapon.fstbmbping");
    public static final DeferredHolder<SoundEvent, SoundEvent> DUCC = reg("entity.ducc");
    public static final DeferredHolder<SoundEvent, SoundEvent> WHACK = reg("weapon.whack");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURBOFAN_OPERATE = reg("block.turbofanoperate");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLICER = reg("entity.slicer");
    public static final DeferredHolder<SoundEvent, SoundEvent> MEGAQUACC = reg("entity.megaquacc");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAINSAW = reg("weapon.chainsaw");
    public static final DeferredHolder<SoundEvent, SoundEvent> BATTERY = reg("item.battery");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_FLAME = reg("weapon.rocketflame");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROCKET_ENGINE = reg("entity.rocketengine");
    public static final DeferredHolder<SoundEvent, SoundEvent> BALLS_LASER = reg("weapon.ballslaser");
    public static final DeferredHolder<SoundEvent, SoundEvent> DART_SHOOT = reg("weapon.dartshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> JETPACK = reg("weapon.jetpack");
    public static final DeferredHolder<SoundEvent, SoundEvent> MUKE_EXPLOSION = reg("weapon.mukeexplosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_MEDIUM = reg("weapon.explosion_medium");
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION_TINY = reg("weapon.explosion_tiny");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUCIBLE_START = reg("weapon.crucible_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUCIBLE_END = reg("weapon.crucible_end");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUCIBLE_SWING = reg("weapon.crucible_swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUCIBLE_LOOP = reg("weapon.crucible_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> C_DEPLOY = reg("weapon.cdeploy");
    public static final DeferredHolder<SoundEvent, SoundEvent> JSG_RELOAD0 = reg("weapon.jsg_reload0");
    public static final DeferredHolder<SoundEvent, SoundEvent> JSG_RELOAD1 = reg("weapon.jsg_reload1");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOB_GIB = reg("weapon.mob_gib");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOOD_SPLAT = reg("weapon.blood_splat");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIT_DIRT = reg("weapon.hit_dirt");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIT_METAL = reg("weapon.hit_metal");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIT_FLESH = reg("weapon.hit_flesh");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOMIT = reg("entity.vomit");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHEKHOV_FIRE = reg("turret.chekhov_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> JEREMY_FIRE = reg("turret.jeremy_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> JEREMY_RELOAD = reg("turret.jeremy_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> RICHARD_FIRE = reg("turret.richard_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOWARD_FIRE = reg("turret.howard_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> HOWARD_RELOAD = reg("turret.howard_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTRY_FIRE = reg("turret.sentry_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> SENTRY_LOCKON = reg("turret.sentry_lockon");
    public static final DeferredHolder<SoundEvent, SoundEvent> RBMK_EXPLOSION = reg("block.rbmk_explosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> RBMK_AZ5_COVER = reg("block.rbmk_az5_cover");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOBBLE = reg("block.bobble");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRATE_OPEN = reg("block.crateopen");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRATE_CLOSE = reg("block.crateclose");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORAGE_OPEN = reg("block.storageopen");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORAGE_CLOSE = reg("block.storageclose");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURBINEGAS_RUNNING = reg("block.turbinegasrunning");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURBINEGAS_SHUTDOWN = reg("block.turbinegasshutdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> TURBINEGAS_STARTUP = reg("block.turbinegasstartup");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUNGUS_LEVER = reg("block.chunguslever");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHUNGUS_OPERATE = reg("block.chungusturbinerunning");
    public static final DeferredHolder<SoundEvent, SoundEvent> DFLASH = reg("weapon.dflash");
    public static final DeferredHolder<SoundEvent, SoundEvent> COUGH = reg("player.cough");
    public static final DeferredHolder<SoundEvent, SoundEvent> GULP = reg("player.gulp");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROAN = reg("player.groan");
    public static final DeferredHolder<SoundEvent, SoundEvent> UFO_BEAM = reg("entity.ufobeam");
    public static final DeferredHolder<SoundEvent, SoundEvent> UFO_BLAST = reg("entity.ufoblast");
    public static final DeferredHolder<SoundEvent, SoundEvent> I_GENERATOR_OPERATE = reg("block.igeneratoroperate");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRANSITION_SEAL_OPEN = reg("block.door.transitionseal");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILOOPEN = reg("block.door.siloopen");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILOCLOSE = reg("block.door.siloclose");
    public static final DeferredHolder<SoundEvent, SoundEvent> GARAGE = reg("block.door.garage");
    public static final DeferredHolder<SoundEvent, SoundEvent> GARAGE_STOP = reg("block.door.garagestop");
    public static final DeferredHolder<SoundEvent, SoundEvent> DOOR_SPINNY = reg("block.door.lever");
    public static final DeferredHolder<SoundEvent, SoundEvent> WGH_BIG_START = reg("block.door.wgh_big_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> WGH_BIG_STOP = reg("block.door.wgh_big_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> WGH_START = reg("block.door.wgh_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> WGH_STOP = reg("block.door.wgh_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ALARM6 = reg("block.door.alarm6");
    public static final DeferredHolder<SoundEvent, SoundEvent> QE_SLIDING_SHUT = reg("block.door.qe_sliding_shut");
    public static final DeferredHolder<SoundEvent, SoundEvent> QE_SLIDING_OPENED = reg("block.door.qe_sliding_opened");
    public static final DeferredHolder<SoundEvent, SoundEvent> QE_SLIDING_OPENING = reg("block.door.qe_sliding_opening");
    public static final DeferredHolder<SoundEvent, SoundEvent> HATCH_OPEN = reg("block.door.hatch_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLIDING_SEAL_OPEN = reg("block.door.sliding_seal_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> SLIDING_SEAL_STOP = reg("block.door.sliding_seal_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER_COCK = reg("weapon.reload.revolvercock");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAG_SMALL_REMOVE = reg("weapon.reload.magsmallremove");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAG_SMALL_INSERT = reg("weapon.reload.magsmallinsert");
    public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER_CLOSE = reg("weapon.reload.revolverclose");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRY_FIRE_CLICK = reg("weapon.reload.dryfireclick");
    public static final DeferredHolder<SoundEvent, SoundEvent> REVOLVER_SPIN = reg("weapon.reload.revolverspin");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEVER_COCK = reg("weapon.reload.levercock");
    public static final DeferredHolder<SoundEvent, SoundEvent> OPEN_LATCH = reg("weapon.reload.openlatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAG_REMOVE = reg("weapon.reload.magremove");
    public static final DeferredHolder<SoundEvent, SoundEvent> MAG_INSERT = reg("weapon.reload.maginsert");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_COCK = reg("weapon.reload.pistolcock");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOTGUN_RELOAD = reg("weapon.reload.shotgunreload");
    public static final DeferredHolder<SoundEvent, SoundEvent> INSERT_CANISTER = reg("weapon.reload.insertcanister");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT = reg("weapon.reload.impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> GL_RELOAD = reg("weapon.glreload");
    public static final DeferredHolder<SoundEvent, SoundEvent> GL_OPEN = reg("weapon.glopen");
    public static final DeferredHolder<SoundEvent, SoundEvent> GL_CLOSE = reg("weapon.glclose");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLAME_LOOP = reg("weapon.fire.flameloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> PRESSURE_VALVE = reg("weapon.reload.pressurevalve");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOTGUN_COCK_OPEN = reg("weapon.reload.shotguncockopen");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOTGUN_COCK = reg("weapon.reload.shotguncock");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOTGUN_COCK_CLOSE = reg("weapon.reload.shotguncockclose");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUN_WHACK = reg("weapon.foley.gunwhack");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOCKON = reg("weapon.fire.lockon");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_OPEN = reg("weapon.reload.boltopen");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOLT_CLOSE = reg("weapon.reload.boltclose");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRENADE_TECH = reg("weapon.reload.grenadetech");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRENADE_NUKA = reg("weapon.reload.grenadenuka");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHREDDER_CYCLE = reg("weapon.fire.shreddercycle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFLE_COCK = reg("weapon.reload.riflecock");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCREW = reg("weapon.reload.screw");
    public static final DeferredHolder<SoundEvent, SoundEvent> INSERT_ROCKET = reg("weapon.reload.insertrocket");
    public static final DeferredHolder<SoundEvent, SoundEvent> TAU = reg("weapon.fire.tau");
    public static final DeferredHolder<SoundEvent, SoundEvent> TAU_LOOP = reg("weapon.fire.tauloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> FATMAN_FULL = reg("weapon.reload.fatmanfull");
    public static final DeferredHolder<SoundEvent, SoundEvent> COILGUN_RELOAD = reg("weapon.coilgunreload");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMACK = reg("weapon.fire.smack");
    public static final DeferredHolder<SoundEvent, SoundEvent> SQUEAKY_TOY = reg("block.squeakytoy");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUNDUNS_MAGNIFICENT_HOWL = reg("block.hundunsmagnificenthowl");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOTOR = reg("block.motor");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_SILENCED = reg("weapon.fire.silenced");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_PISTOL = reg("weapon.fire.pistol");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_PISTOL_LIGHT = reg("weapon.fire.pistollight");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_BLACK_POWDER = reg("weapon.fire.blackpowder");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_UZI = reg("weapon.fire.uzi");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_GREASE_GUN = reg("weapon.fire.greasegun");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_ABERRATOR = reg("weapon.fire.aberrator");
    public static final DeferredHolder<SoundEvent, SoundEvent> COILGUN_SHOOT = reg("weapon.coilgunshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_TAU_RELEASE = reg("weapon.fire.taurelease");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_FATMAN = reg("weapon.fire.fatman");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_RIFLE = reg("weapon.fire.rifle");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOOT44 = reg("weapon.44shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_SHOTGUN = reg("weapon.fire.shotgun");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOUDEST_NOISE_ON_EARTH = reg("weapon.fire.loudestnoiseonearth");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_TESLA = reg("weapon.fire.tesla");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_LASER = reg("weapon.fire.laser");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_SHOTGUN_AUTO = reg("weapon.fire.shotgunalt");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_SHOTGUN_ALT = reg("weapon.fire.shotgunauto");
    public static final DeferredHolder<SoundEvent, SoundEvent> GL_SHOOT = reg("weapon.glshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> MK108_SHOOT = reg("weapon.fire.mk108");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_LASER_GATLING = reg("weapon.fire.lasergatling");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_RIFLE_HEAVY = reg("weapon.fire.rifleheavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_ASSAULT = reg("weapon.fire.assault");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_DISINTEGRATION = reg("weapon.fire.disintegration");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_LASER_PISTOL = reg("weapon.fire.laserpistol");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_STAB = reg("weapon.fire.stab");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_GRENADE = reg("weapon.fire.grenade");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_AMAT = reg("weapon.fire.amat");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE_EXTINGUISHER = reg("weapon.extinguisher");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASSEMBLER_STRIKE = reg("block.assemblerstrike");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASSEMBLER_START = reg("block.assemblerstart");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASSEMBLER_STOP = reg("block.assemblerstop");
    public static final DeferredHolder<SoundEvent, SoundEvent> ASSEMBLER_CUT = reg("block.assemblercut");
    public static final DeferredHolder<SoundEvent, SoundEvent> TUBE_FWOOMP = reg("weapon.reload.tubefwoomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> FUSION_REACTOR_RUNNING = reg("block.fusionreactorrunning");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLINK_SHELL = reg("weapon.casing.shell");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLINK_SMALL = reg("weapon.casing.small");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLINK_MEDIUM = reg("weapon.casing.medium");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLINK_LARGE = reg("weapon.casing.large");

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> GEIGER_SOUNDS =
            List.of(GEIGER1, GEIGER2, GEIGER3, GEIGER4, GEIGER5, GEIGER6);

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> VOICE_SOUNDS =
            List.of(VOICES1, VOICES2, VOICES3, VOICES4, VOICES5, VOICES6, VOICES7, VOICES8);

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> BOILER_GROAN_SOUNDS =
            List.of(BOILER_GROAN1, BOILER_GROAN2, BOILER_GROAN3);

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String path) {
        return SOUND_EVENTS.register(path,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path)));
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
