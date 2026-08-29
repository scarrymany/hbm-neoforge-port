package com.hbm.lib;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CE's version is a bare {@code SoundEvent} field registry, hand-populated in an {@code init()}
 * method called by the old {@code SoundHandler} registry-event listener. NeoForge requires every
 * registry entry to go through a {@link DeferredRegister}, so each field here is a
 * {@code DeferredHolder<SoundEvent, SoundEvent>} instead of a raw {@code SoundEvent}; call sites
 * elsewhere in the mod must call {@code .get()} to obtain the actual {@link SoundEvent}.
 * <p>
 * CE's sound path strings are mixed-case (e.g. {@code "weapon.rpgReload"}), which was already
 * borderline-invalid under vanilla's resource path rules and is flatly rejected by
 * {@link ResourceLocation} in 1.21 (paths must be {@code [a-z0-9_.-/]}). Every path below has been
 * mechanically converted to lowercase snake_case (insert {@code _} before each original uppercase
 * letter, then lowercase everything) - the same transformation Neo Edition applied by hand to the
 * subset of these sounds it has ported so far, cross-checked against its output where an equivalent
 * entry exists. The asset pipeline (out of this area's scope) must rename the corresponding
 * {@code .ogg} files and {@code sounds.json} entries under {@code assets/hbm/sounds} to match.
 * <p>
 * CE's {@code init()} also wrote three of its own fields into {@code GunConfiguration}'s static
 * reload-sound defaults ({@code GunConfiguration.RSOUND_LAUNCHER} etc.). {@code GunConfiguration}
 * lives in {@code com.hbm.handler}, a different area's scope and not yet ported, so that wiring is
 * dropped here; whichever area ports {@code GunConfiguration} must assign those fields from
 * {@code HBMSoundHandler.rpgReload.get()} / {@code reloadGrenade.get()} / {@code reloadShotgun.get()}
 * / {@code reloadMag.get()} / {@code reloadRevolver.get()} / {@code fatmanReload.get()} once it exists.
 */
public final class HBMSoundHandler {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MainRegistry.MODID);

    private static final Map<ResourceLocation, SoundEvent> DYNAMIC_SOUNDS = new ConcurrentHashMap<>();

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, name)));
    }

    public static final DeferredHolder<SoundEvent, SoundEvent> alarmHatch = reg("alarm.hatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmAutopilot = reg("alarm.autopilot");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmAMSSiren = reg("alarm.ams_siren");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmBlastDoor = reg("alarm.blast_door_alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmAPCLoop = reg("alarm.apc_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmKlaxon = reg("alarm.klaxon");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmFoKlaxonA = reg("alarm.fo_klaxon_a");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmFoKlaxonB = reg("alarm.fo_klaxon_b");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmRegular = reg("alarm.regular_siren");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmClassic = reg("alarm.classic");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmBank = reg("alarm.bank_alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmBeep = reg("alarm.beep_siren");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmContainer = reg("alarm.container_alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmSweep = reg("alarm.sweep_siren");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmStrider = reg("alarm.strider_siren");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmAirRaid = reg("alarm.air_raid");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmNostromo = reg("alarm.nostromo_siren");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmEas = reg("alarm.eas_alarm");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmAPCPass = reg("alarm.apc_pass");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmRazorTrain = reg("alarm.razortrain_horn");
    public static final DeferredHolder<SoundEvent, SoundEvent> soyuzed = reg("alarm.soyuzed");
    public static final DeferredHolder<SoundEvent, SoundEvent> metalStep = reg("step.metal");
    public static final DeferredHolder<SoundEvent, SoundEvent> iron = reg("step.iron");
    public static final DeferredHolder<SoundEvent, SoundEvent> ironLand = reg("step.iron_land");
    public static final DeferredHolder<SoundEvent, SoundEvent> ironJump = reg("step.iron_jump");
    public static final DeferredHolder<SoundEvent, SoundEvent> poweredStep = reg("step.powered");
    public static final DeferredHolder<SoundEvent, SoundEvent> lambdaCore = reg("music.recordlambdacore");
    public static final DeferredHolder<SoundEvent, SoundEvent> sectorSweep = reg("music.recordsectorsweep");
    public static final DeferredHolder<SoundEvent, SoundEvent> vortalCombat = reg("music.recordvortalcombat");
    public static final DeferredHolder<SoundEvent, SoundEvent> glass = reg("music.transmission");
    public static final DeferredHolder<SoundEvent, SoundEvent> metalBlock = reg("step.metal_block");
    public static final DeferredHolder<SoundEvent, SoundEvent> explosionSmallNear = reg("weapon.explosion_small_near");
    public static final DeferredHolder<SoundEvent, SoundEvent> explosionSmallFar = reg("weapon.explosion_small_far");
    public static final DeferredHolder<SoundEvent, SoundEvent> explosionLargeNear = reg("weapon.explosion_large_near");
    public static final DeferredHolder<SoundEvent, SoundEvent> explosionLargeFar = reg("weapon.explosion_large_far");
    public static final DeferredHolder<SoundEvent, SoundEvent> fel = reg("block.fel");
    public static final DeferredHolder<SoundEvent, SoundEvent> fensuHum = reg("block.fensu_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> hephaestusRunning = reg("block.hephaestus_running");
    public static final DeferredHolder<SoundEvent, SoundEvent> meteoriteFallingLoop = reg("entity.meteorite_falling_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> pressOperate = reg("block.pressoperate");
    public static final DeferredHolder<SoundEvent, SoundEvent> laserBang = reg("weapon.laser_bang");
    public static final DeferredHolder<SoundEvent, SoundEvent> blockDebris = reg("block.debris");
    public static final DeferredHolder<SoundEvent, SoundEvent> rbmkLid = reg("block.rbmk_lid");
    public static final DeferredHolder<SoundEvent, SoundEvent> syringeUse = reg("item.syringe");
    public static final DeferredHolder<SoundEvent, SoundEvent> sparkShoot = reg("weapon.spark_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> leverStart = reg("block.lever_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> leverStop = reg("block.lever_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> metalImpact = reg("block.metal_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> spark = reg("block.spark");
    public static final DeferredHolder<SoundEvent, SoundEvent> b92Reload = reg("weapon.b92_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> techBleep = reg("item.tech_bleep");
    public static final DeferredHolder<SoundEvent, SoundEvent> techBoop = reg("item.tech_boop");
    public static final DeferredHolder<SoundEvent, SoundEvent> hornNearSingle = reg("block.horn_near_single");
    public static final DeferredHolder<SoundEvent, SoundEvent> largeTurbineRunning = reg("block.large_turbine");
    public static final DeferredHolder<SoundEvent, SoundEvent> engine = reg("block.engine");
    public static final DeferredHolder<SoundEvent, SoundEvent> hornNearDual = reg("block.horn_near_dual");
    public static final DeferredHolder<SoundEvent, SoundEvent> hornFarSingle = reg("block.horn_far_single");
    public static final DeferredHolder<SoundEvent, SoundEvent> hornFarDual = reg("block.horn_far_dual");
    public static final DeferredHolder<SoundEvent, SoundEvent> reactorLoop = reg("block.reactor_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> reactorStart = reg("block.reactor_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> reactorStop = reg("block.reactor_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> chemicalPlant = reg("block.chemical_plant");
    public static final DeferredHolder<SoundEvent, SoundEvent> potatOSRandom = reg("potatos.random");
    public static final DeferredHolder<SoundEvent, SoundEvent> weaponSpinDown = reg("weapon.spindown");
    public static final DeferredHolder<SoundEvent, SoundEvent> weaponSpinUp = reg("weapon.spinup");
    public static final DeferredHolder<SoundEvent, SoundEvent> sawShoot = reg("weapon.saw_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> rpgShoot = reg("weapon.rpg_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> reloadTurret = reg("weapon.reload_turret");
    public static final DeferredHolder<SoundEvent, SoundEvent> rifleShoot = reg("weapon.rifle_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> defabShoot = reg("weapon.defab_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> flamethrowerIgnite = reg("weapon.flamethrower_ignite");
    public static final DeferredHolder<SoundEvent, SoundEvent> flamethrowerShoot = reg("weapon.flamethrower_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> tauShoot = reg("weapon.tau_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> oldExplosion = reg("entity.old_explosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> nuclearExplosion = reg("weapon.nuclear_explosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> robinExplosion = reg("weapon.robin_explosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> boiler = reg("block.boiler");
    public static final DeferredHolder<SoundEvent, SoundEvent> boiler_groan1 = reg("block.boiler_groan0");
    public static final DeferredHolder<SoundEvent, SoundEvent> boiler_groan2 = reg("block.boiler_groan1");
    public static final DeferredHolder<SoundEvent, SoundEvent> boiler_groan3 = reg("block.boiler_groan2");
    public static final DeferredHolder<SoundEvent, SoundEvent> ciwsSpindown = reg("weapon.ciws_spindown");
    public static final DeferredHolder<SoundEvent, SoundEvent> ciwsSpinup = reg("weapon.ciws_spinup");
    public static final DeferredHolder<SoundEvent, SoundEvent> ciwsFiringLoop = reg("weapon.ciws_firing_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> warnOverspeed = reg("block.warn_overspeed");
    public static final DeferredHolder<SoundEvent, SoundEvent> planeShotDown = reg("entity.plane_shot_down");
    public static final DeferredHolder<SoundEvent, SoundEvent> bombWhistle = reg("entity.bomb_whistle");
    public static final DeferredHolder<SoundEvent, SoundEvent> mortarWhistle = reg("entity.mortar_whistle");
    public static final DeferredHolder<SoundEvent, SoundEvent> planeCrash = reg("entity.plane_crash");
    public static final DeferredHolder<SoundEvent, SoundEvent> missileTakeoff = reg("weapon.missile_take_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> bomberSmallLoop = reg("entity.bomber_small_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> bomberLoop = reg("entity.bomber_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> stingerLockon = reg("weapon.stinger_lock_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> trainHorn = reg("alarm.trainhorn");
    public static final DeferredHolder<SoundEvent, SoundEvent> bombDet = reg("entity.bomb_det");
    public static final DeferredHolder<SoundEvent, SoundEvent> rumble = reg("misc.rumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> pipeFail = reg("entity.pipefail");
    public static final DeferredHolder<SoundEvent, SoundEvent> lpwstart = reg("misc.lpwstart");
    public static final DeferredHolder<SoundEvent, SoundEvent> lpwstop = reg("misc.lpwstop");
    public static final DeferredHolder<SoundEvent, SoundEvent> lpwloop = reg("misc.lpwloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> htrstart = reg("misc.htrstart");
    public static final DeferredHolder<SoundEvent, SoundEvent> htrstop = reg("misc.htrstop");
    public static final DeferredHolder<SoundEvent, SoundEvent> htrloop = reg("misc.htrloop");
    public static final DeferredHolder<SoundEvent, SoundEvent> rocketTakeoff = reg("entity.rocket_takeoff");
    public static final DeferredHolder<SoundEvent, SoundEvent> rocketIgnition = reg("entity.rocket_ignition");
    public static final DeferredHolder<SoundEvent, SoundEvent> rocketFlyLight = reg("entity.rocket_fly_light");
    public static final DeferredHolder<SoundEvent, SoundEvent> rocketFlyHeavy = reg("entity.rocket_fly_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> silencerShoot = reg("weapon.silencer_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> rpgReload = reg("weapon.rpg_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> reloadGrenade = reg("weapon.hk_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> reloadShotgun = reg("weapon.shotgun_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> reloadMag = reg("weapon.mag_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> reloadRevolver = reg("weapon.revolver_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> fatmanReload = reg("weapon.fatman_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> boatWeapon = reg("weapon.boat");
    public static final DeferredHolder<SoundEvent, SoundEvent> ricochet = reg("weapon.ricochet");
    public static final DeferredHolder<SoundEvent, SoundEvent> grenadeBounce = reg("weapon.g_bounce");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarmGambit = reg("alarm.gambit");
    public static final DeferredHolder<SoundEvent, SoundEvent> revolverShoot = reg("weapon.revolver_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> heavyShoot = reg("weapon.heavy_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> schrabidiumShoot = reg("weapon.schrabidiumshoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> revolverShootAlt = reg("weapon.revolver_shoot_alt");
    public static final DeferredHolder<SoundEvent, SoundEvent> hkShoot = reg("weapon.hk_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> shotgunShoot = reg("weapon.shotgun_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> shottyShoot = reg("weapon.shotty_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> uziShoot = reg("weapon.uzi_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> calShoot = reg("weapon.cal_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> lacunaeShoot = reg("weapon.lacunae_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> fatmanShoot = reg("weapon.fatman_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> osiprShoot = reg("weapon.osipr_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> zomgShoot = reg("weapon.zomg_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> jetpackTank = reg("item.jetpack_tank");
    public static final DeferredHolder<SoundEvent, SoundEvent> switchmode1 = reg("weapon.switchmode1");
    public static final DeferredHolder<SoundEvent, SoundEvent> switchmode2 = reg("weapon.switchmode2");
    public static final DeferredHolder<SoundEvent, SoundEvent> nullTau = reg("misc.null_tau");
    public static final DeferredHolder<SoundEvent, SoundEvent> nullRadar = reg("misc.null_radar");
    public static final DeferredHolder<SoundEvent, SoundEvent> immolatorIgnite = reg("weapon.immolator_ignite");
    public static final DeferredHolder<SoundEvent, SoundEvent> immolatorShoot = reg("weapon.immolator_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> defabSpinup = reg("weapon.defab_spinup");
    public static final DeferredHolder<SoundEvent, SoundEvent> cryolatorShoot = reg("weapon.cryolator_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> singFlyby = reg("weapon.sing_flyby");
    public static final DeferredHolder<SoundEvent, SoundEvent> osiprCharging = reg("weapon.osipr_charging");
    public static final DeferredHolder<SoundEvent, SoundEvent> leverActionReload = reg("weapon.lever_action_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> follyOpen = reg("weapon.folly_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> follyReload = reg("weapon.folly_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> follyClose = reg("weapon.folly_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> follyFire = reg("weapon.folly_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> follyBuzzer = reg("weapon.folly_buzzer");
    public static final DeferredHolder<SoundEvent, SoundEvent> follyAquired = reg("weapon.folly_aquired");
    public static final DeferredHolder<SoundEvent, SoundEvent> chopperDrop = reg("entity.chopper_drop");
    public static final DeferredHolder<SoundEvent, SoundEvent> pyroOperate = reg("block.pyro_operate");
    public static final DeferredHolder<SoundEvent, SoundEvent> electricHum = reg("block.electric_hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> crateBreak = reg("block.crate_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> itemUnpack = reg("item.unpack");
    public static final DeferredHolder<SoundEvent, SoundEvent> centrifugeOperate = reg("block.centrifuge_operate");
    public static final DeferredHolder<SoundEvent, SoundEvent> buttonNo = reg("block.button_no");
    public static final DeferredHolder<SoundEvent, SoundEvent> buttonYes = reg("block.button_yes");
    public static final DeferredHolder<SoundEvent, SoundEvent> railgunFire = reg("block.railgun_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> railgunOrientation = reg("block.railgun_orientation");
    public static final DeferredHolder<SoundEvent, SoundEvent> railgunCharge = reg("block.railgun_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> shutdown = reg("block.shutdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> broadcast1 = reg("block.broadcast1");
    public static final DeferredHolder<SoundEvent, SoundEvent> broadcast2 = reg("block.broadcast2");
    public static final DeferredHolder<SoundEvent, SoundEvent> broadcast3 = reg("block.broadcast3");
    public static final DeferredHolder<SoundEvent, SoundEvent> geiger1 = reg("item.geiger1");
    public static final DeferredHolder<SoundEvent, SoundEvent> geiger2 = reg("item.geiger2");
    public static final DeferredHolder<SoundEvent, SoundEvent> geiger3 = reg("item.geiger3");
    public static final DeferredHolder<SoundEvent, SoundEvent> geiger4 = reg("item.geiger4");
    public static final DeferredHolder<SoundEvent, SoundEvent> geiger5 = reg("item.geiger5");
    public static final DeferredHolder<SoundEvent, SoundEvent> geiger6 = reg("item.geiger6");
    public static final DeferredHolder<SoundEvent, SoundEvent> voices1 = reg("item.voices1");
    public static final DeferredHolder<SoundEvent, SoundEvent> voices2 = reg("item.voices2");
    public static final DeferredHolder<SoundEvent, SoundEvent> voices3 = reg("item.voices3");
    public static final DeferredHolder<SoundEvent, SoundEvent> voices4 = reg("item.voices4");
    public static final DeferredHolder<SoundEvent, SoundEvent> voices5 = reg("item.voices5");
    public static final DeferredHolder<SoundEvent, SoundEvent> voices6 = reg("item.voices6");
    public static final DeferredHolder<SoundEvent, SoundEvent> voices7 = reg("item.voices7");
    public static final DeferredHolder<SoundEvent, SoundEvent> voices8 = reg("item.voices8");
    public static final DeferredHolder<SoundEvent, SoundEvent> lockOpen = reg("block.lock_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> pinBreak = reg("item.pin_break");
    public static final DeferredHolder<SoundEvent, SoundEvent> pinUnlock = reg("item.pin_unlock");
    public static final DeferredHolder<SoundEvent, SoundEvent> lockHang = reg("block.lock_hang");
    public static final DeferredHolder<SoundEvent, SoundEvent> vaultScrapeNew = reg("block.vault_scrape_new");
    public static final DeferredHolder<SoundEvent, SoundEvent> vaultThudNew = reg("block.vault_thud_new");
    public static final DeferredHolder<SoundEvent, SoundEvent> missileAssembly2 = reg("block.missile_assembly2");
    public static final DeferredHolder<SoundEvent, SoundEvent> sonarPing = reg("block.sonar_ping");
    public static final DeferredHolder<SoundEvent, SoundEvent> radawayUse = reg("item.radaway");
    public static final DeferredHolder<SoundEvent, SoundEvent> gasmaskScrew = reg("item.gasmask_screw");
    public static final DeferredHolder<SoundEvent, SoundEvent> spray = reg("item.spray");
    public static final DeferredHolder<SoundEvent, SoundEvent> repair = reg("item.repair");
    public static final DeferredHolder<SoundEvent, SoundEvent> nullChopper = reg("misc.null_chopper");
    public static final DeferredHolder<SoundEvent, SoundEvent> chopperCharge = reg("entity.chopper_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> nullCrashing = reg("misc.null_crashing");
    public static final DeferredHolder<SoundEvent, SoundEvent> chopperDamage = reg("entity.chopper_damage");
    public static final DeferredHolder<SoundEvent, SoundEvent> nullMine = reg("misc.null_mine");
    public static final DeferredHolder<SoundEvent, SoundEvent> openDoor = reg("block.open_door");
    public static final DeferredHolder<SoundEvent, SoundEvent> closeDoor = reg("block.close_door");
    public static final DeferredHolder<SoundEvent, SoundEvent> openC = reg("block.openc");
    public static final DeferredHolder<SoundEvent, SoundEvent> closeC = reg("block.closec");
    public static final DeferredHolder<SoundEvent, SoundEvent> steamEngineOperate = reg("block.steam_engine_operate");
    public static final DeferredHolder<SoundEvent, SoundEvent> boltgun = reg("item.boltgun");
    public static final DeferredHolder<SoundEvent, SoundEvent> bang = reg("weapon.bang");
    public static final DeferredHolder<SoundEvent, SoundEvent> slice = reg("weapon.slice");
    public static final DeferredHolder<SoundEvent, SoundEvent> kaping = reg("weapon.kapeng");
    public static final DeferredHolder<SoundEvent, SoundEvent> pipePlaced = reg("block.pipe_placed");
    public static final DeferredHolder<SoundEvent, SoundEvent> tesla = reg("weapon.tesla");
    public static final DeferredHolder<SoundEvent, SoundEvent> cybercrab = reg("entity.cybercrab");
    public static final DeferredHolder<SoundEvent, SoundEvent> osiprReload = reg("weapon.osipr_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> soyuzReady = reg("block.soyuz_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> soyuzTakeOff = reg("entity.soyuz_takeoff");
    public static final DeferredHolder<SoundEvent, SoundEvent> chime = reg("alarm.chime");
    public static final DeferredHolder<SoundEvent, SoundEvent> tauChargeLoop2 = reg("weapon.tau_charge_loop2");
    public static final DeferredHolder<SoundEvent, SoundEvent> chopperFlyingLoop = reg("entity.chopper_flying_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> chopperCrashingLoop = reg("entity.chopper_crashing_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> chopperMineLoop = reg("entity.chopper_mine_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> lacunaeSpinup = reg("weapon.lacunae_spinup");
    public static final DeferredHolder<SoundEvent, SoundEvent> lacunaeSpindown = reg("weapon.lacunae_spindown");
    public static final DeferredHolder<SoundEvent, SoundEvent> teslaShoot = reg("weapon.tesla_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> stop = reg("weapon.stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> bonk = reg("weapon.bonk");
    public static final DeferredHolder<SoundEvent, SoundEvent> glauncher = reg("weapon.glauncher");
    public static final DeferredHolder<SoundEvent, SoundEvent> hksShoot = reg("weapon.hks_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> vice = reg("item.vice");
    public static final DeferredHolder<SoundEvent, SoundEvent> screm = reg("block.screm");
    public static final DeferredHolder<SoundEvent, SoundEvent> upgradePlug = reg("item.upgrade_plug");
    public static final DeferredHolder<SoundEvent, SoundEvent> tauChargeLoop = reg("weapon.tau_charge_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> quadroReload = reg("weapon.quadro_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> fstbmbStart = reg("weapon.fstbmb_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> fstbmbPing = reg("weapon.fstbmb_ping");
    public static final DeferredHolder<SoundEvent, SoundEvent> ducc = reg("entity.ducc");
    public static final DeferredHolder<SoundEvent, SoundEvent> whack = reg("weapon.whack");
    public static final DeferredHolder<SoundEvent, SoundEvent> turbofanOperate = reg("block.turbofan_operate");
    public static final DeferredHolder<SoundEvent, SoundEvent> slicer = reg("entity.slicer");
    public static final DeferredHolder<SoundEvent, SoundEvent> megaquacc = reg("entity.megaquacc");
    public static final DeferredHolder<SoundEvent, SoundEvent> chainsaw = reg("weapon.chainsaw");
    public static final DeferredHolder<SoundEvent, SoundEvent> battery = reg("item.battery");
    public static final DeferredHolder<SoundEvent, SoundEvent> rocketFlame = reg("weapon.rocket_flame");
    public static final DeferredHolder<SoundEvent, SoundEvent> rocketEngine = reg("entity.rocket_engine");
    public static final DeferredHolder<SoundEvent, SoundEvent> ballsLaser = reg("weapon.balls_laser");
    public static final DeferredHolder<SoundEvent, SoundEvent> dartShoot = reg("weapon.dart_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> jetpack = reg("weapon.jetpack");
    public static final DeferredHolder<SoundEvent, SoundEvent> mukeExplosion = reg("weapon.muke_explosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> explosion_medium = reg("weapon.explosion_medium");
    public static final DeferredHolder<SoundEvent, SoundEvent> explosion_tiny = reg("weapon.explosion_tiny");
    public static final DeferredHolder<SoundEvent, SoundEvent> crucibleStart = reg("weapon.crucible_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> crucibleEnd = reg("weapon.crucible_end");
    public static final DeferredHolder<SoundEvent, SoundEvent> crucibleSwing = reg("weapon.crucible_swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> crucibleLoop = reg("weapon.crucible_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> cDeploy = reg("weapon.c_deploy");
    public static final DeferredHolder<SoundEvent, SoundEvent> jsg_reload0 = reg("weapon.jsg_reload0");
    public static final DeferredHolder<SoundEvent, SoundEvent> jsg_reload1 = reg("weapon.jsg_reload1");
    public static final DeferredHolder<SoundEvent, SoundEvent> mob_gib = reg("weapon.mob_gib");
    public static final DeferredHolder<SoundEvent, SoundEvent> blood_splat = reg("weapon.blood_splat");
    public static final DeferredHolder<SoundEvent, SoundEvent> hit_dirt = reg("weapon.hit_dirt");
    public static final DeferredHolder<SoundEvent, SoundEvent> hit_metal = reg("weapon.hit_metal");
    public static final DeferredHolder<SoundEvent, SoundEvent> hit_flesh = reg("weapon.hit_flesh");
    public static final DeferredHolder<SoundEvent, SoundEvent> vomit = reg("entity.vomit");
    public static final DeferredHolder<SoundEvent, SoundEvent> chekhov_fire = reg("turret.chekhov_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> jeremy_fire = reg("turret.jeremy_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> jeremy_reload = reg("turret.jeremy_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> richard_fire = reg("turret.richard_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> howard_fire = reg("turret.howard_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> howard_reload = reg("turret.howard_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> sentryFire = reg("turret.sentry_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> sentryLockon = reg("turret.sentry_lockon");
    public static final DeferredHolder<SoundEvent, SoundEvent> rbmk_explosion = reg("block.rbmk_explosion");
    public static final DeferredHolder<SoundEvent, SoundEvent> rbmk_az5_cover = reg("block.rbmk_az5_cover");
    public static final DeferredHolder<SoundEvent, SoundEvent> bobble = reg("block.bobble");
    public static final DeferredHolder<SoundEvent, SoundEvent> crateOpen = reg("block.crate_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> crateClose = reg("block.crate_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> storageOpen = reg("block.storage_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> storageClose = reg("block.storage_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> turbinegasRunning = reg("block.turbinegas_running");
    public static final DeferredHolder<SoundEvent, SoundEvent> turbinegasShutdown = reg("block.turbinegas_shutdown");
    public static final DeferredHolder<SoundEvent, SoundEvent> turbinegasStartup = reg("block.turbinegas_startup");
    public static final DeferredHolder<SoundEvent, SoundEvent> chungus_lever = reg("block.chungus_lever");
    public static final DeferredHolder<SoundEvent, SoundEvent> chungusOperate = reg("block.chungus_turbine_running");
    public static final DeferredHolder<SoundEvent, SoundEvent> dflash = reg("weapon.d_flash");
    public static final DeferredHolder<SoundEvent, SoundEvent> cough = reg("player.cough");
    public static final DeferredHolder<SoundEvent, SoundEvent> gulp = reg("player.gulp");
    public static final DeferredHolder<SoundEvent, SoundEvent> groan = reg("player.groan");
    public static final DeferredHolder<SoundEvent, SoundEvent> ufoBeam = reg("entity.ufo_beam");
    public static final DeferredHolder<SoundEvent, SoundEvent> ufoBlast = reg("entity.ufo_blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> iGeneratorOperate = reg("block.igenerator_operate");
    public static final DeferredHolder<SoundEvent, SoundEvent> transitionSealOpen = reg("block.door.transitionseal");
    public static final DeferredHolder<SoundEvent, SoundEvent> siloopen = reg("block.door.siloopen");
    public static final DeferredHolder<SoundEvent, SoundEvent> siloclose = reg("block.door.siloclose");
    public static final DeferredHolder<SoundEvent, SoundEvent> garage = reg("block.door.garage");
    public static final DeferredHolder<SoundEvent, SoundEvent> garage_stop = reg("block.door.garagestop");
    public static final DeferredHolder<SoundEvent, SoundEvent> door_spinny = reg("block.door.lever");
    public static final DeferredHolder<SoundEvent, SoundEvent> wgh_big_start = reg("block.door.wgh_big_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> wgh_big_stop = reg("block.door.wgh_big_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> wgh_start = reg("block.door.wgh_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> wgh_stop = reg("block.door.wgh_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> alarm6 = reg("block.door.alarm6");
    public static final DeferredHolder<SoundEvent, SoundEvent> qe_sliding_shut = reg("block.door.qe_sliding_shut");
    public static final DeferredHolder<SoundEvent, SoundEvent> qe_sliding_opened = reg("block.door.qe_sliding_opened");
    public static final DeferredHolder<SoundEvent, SoundEvent> qe_sliding_opening = reg("block.door.qe_sliding_opening");
    public static final DeferredHolder<SoundEvent, SoundEvent> hatch_open = reg("block.door.hatch_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> sliding_seal_open = reg("block.door.sliding_seal_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> sliding_seal_stop = reg("block.door.sliding_seal_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> revolverCock = reg("weapon.reload.revolver_cock");
    public static final DeferredHolder<SoundEvent, SoundEvent> magSmallRemove = reg("weapon.reload.mag_small_remove");
    public static final DeferredHolder<SoundEvent, SoundEvent> magSmallInsert = reg("weapon.reload.mag_small_insert");
    public static final DeferredHolder<SoundEvent, SoundEvent> revolverClose = reg("weapon.reload.revolver_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> dryFireClick = reg("weapon.reload.dry_fire_click");
    public static final DeferredHolder<SoundEvent, SoundEvent> revolverSpin = reg("weapon.reload.revolver_spin");
    public static final DeferredHolder<SoundEvent, SoundEvent> leverCock = reg("weapon.reload.lever_cock");
    public static final DeferredHolder<SoundEvent, SoundEvent> openLatch = reg("weapon.reload.open_latch");
    public static final DeferredHolder<SoundEvent, SoundEvent> magRemove = reg("weapon.reload.mag_remove");
    public static final DeferredHolder<SoundEvent, SoundEvent> magInsert = reg("weapon.reload.mag_insert");
    public static final DeferredHolder<SoundEvent, SoundEvent> pistolCock = reg("weapon.reload.pistol_cock");
    public static final DeferredHolder<SoundEvent, SoundEvent> shotgunReload = reg("weapon.reload.shotgun_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> insertCanister = reg("weapon.reload.insert_canister");
    public static final DeferredHolder<SoundEvent, SoundEvent> impact = reg("weapon.reload.impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> glReload = reg("weapon.gl_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> glOpen = reg("weapon.gl_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> glClose = reg("weapon.gl_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> flameLoop = reg("weapon.fire.flame_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> pressureValve = reg("weapon.reload.pressure_valve");
    public static final DeferredHolder<SoundEvent, SoundEvent> shotgunCockOpen = reg("weapon.reload.shotgun_cock_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> shotgunCock = reg("weapon.reload.shotgun_cock");
    public static final DeferredHolder<SoundEvent, SoundEvent> shotgunCockClose = reg("weapon.reload.shotgun_cock_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> gunWhack = reg("weapon.foley.gun_whack");
    public static final DeferredHolder<SoundEvent, SoundEvent> lockon = reg("weapon.fire.lockon");
    public static final DeferredHolder<SoundEvent, SoundEvent> boltOpen = reg("weapon.reload.bolt_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> boltClose = reg("weapon.reload.bolt_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> grenadeTech = reg("weapon.reload.grenade_tech");
    public static final DeferredHolder<SoundEvent, SoundEvent> grenadeNuka = reg("weapon.reload.grenade_nuka");
    public static final DeferredHolder<SoundEvent, SoundEvent> shredderCycle = reg("weapon.fire.shredder_cycle");
    public static final DeferredHolder<SoundEvent, SoundEvent> rifleCock = reg("weapon.reload.rifle_cock");
    public static final DeferredHolder<SoundEvent, SoundEvent> screw = reg("weapon.reload.screw");
    public static final DeferredHolder<SoundEvent, SoundEvent> insertRocket = reg("weapon.reload.insert_rocket");
    public static final DeferredHolder<SoundEvent, SoundEvent> tau = reg("weapon.fire.tau");
    public static final DeferredHolder<SoundEvent, SoundEvent> tauLoop = reg("weapon.fire.tau_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> fatmanFull = reg("weapon.reload.fatman_full");
    public static final DeferredHolder<SoundEvent, SoundEvent> coilgunReload = reg("weapon.coilgun_reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> smack = reg("weapon.fire.smack");
    public static final DeferredHolder<SoundEvent, SoundEvent> squeakyToy = reg("block.squeaky_toy");
    public static final DeferredHolder<SoundEvent, SoundEvent> hundunsMagnificentHowl = reg("block.hunduns_magnificent_howl");
    public static final DeferredHolder<SoundEvent, SoundEvent> motor = reg("block.motor");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireSilenced = reg("weapon.fire.silenced");
    public static final DeferredHolder<SoundEvent, SoundEvent> firePistol = reg("weapon.fire.pistol");
    public static final DeferredHolder<SoundEvent, SoundEvent> firePistolLight = reg("weapon.fire.pistollight");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireBlackPowder = reg("weapon.fire.black_powder");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireUzi = reg("weapon.fire.uzi");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireGreaseGun = reg("weapon.fire.grease_gun");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireAberrator = reg("weapon.fire.aberrator");
    public static final DeferredHolder<SoundEvent, SoundEvent> coilgunShoot = reg("weapon.coilgun_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireTauRelease = reg("weapon.fire.tau_release");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireFatman = reg("weapon.fire.fatman");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireRifle = reg("weapon.fire.rifle");
    public static final DeferredHolder<SoundEvent, SoundEvent> shoot44 = reg("weapon.44_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireShotgun = reg("weapon.fire.shotgun");
    public static final DeferredHolder<SoundEvent, SoundEvent> loudestNoiseOnEarth = reg("weapon.fire.loudest_noise_on_earth");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireTesla = reg("weapon.fire.tesla");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireLaser = reg("weapon.fire.laser");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireShotgunAuto = reg("weapon.fire.shotgun_alt");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireShotgunAlt = reg("weapon.fire.shotgun_auto");
    public static final DeferredHolder<SoundEvent, SoundEvent> glShoot = reg("weapon.gl_shoot");
    public static final DeferredHolder<SoundEvent, SoundEvent> mk108Shoot = reg("weapon.fire.mk108");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireLaserGatling = reg("weapon.fire.laser_gatling");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireRifleHeavy = reg("weapon.fire.rifle_heavy");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireAssault = reg("weapon.fire.assault");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireDisintegration = reg("weapon.fire.disintegration");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireLaserPistol = reg("weapon.fire.laser_pistol");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireStab = reg("weapon.fire.stab");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireGrenade = reg("weapon.fire.grenade");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireAmat = reg("weapon.fire.amat");
    public static final DeferredHolder<SoundEvent, SoundEvent> fireExtinguisher = reg("weapon.extinguisher");
    public static final DeferredHolder<SoundEvent, SoundEvent> assemblerStrike = reg("block.assembler_strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> assemblerStart = reg("block.assembler_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> assemblerStop = reg("block.assembler_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> assemblerCut = reg("block.assembler_cut");
    public static final DeferredHolder<SoundEvent, SoundEvent> tubeFwoomp = reg("weapon.reload.tube_fwoomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> fusionReactorRunning = reg("block.fusion_reactor_running");
    public static final DeferredHolder<SoundEvent, SoundEvent> plinkShell = reg("weapon.casing.shell");
    public static final DeferredHolder<SoundEvent, SoundEvent> plinkSmall = reg("weapon.casing.small");
    public static final DeferredHolder<SoundEvent, SoundEvent> plinkMedium = reg("weapon.casing.medium");
    public static final DeferredHolder<SoundEvent, SoundEvent> plinkLarge = reg("weapon.casing.large");

    private HBMSoundHandler() {
    }

    public static SoundEvent[] geigerSounds() {
        return new SoundEvent[]{geiger1.get(), geiger2.get(), geiger3.get(), geiger4.get(), geiger5.get(), geiger6.get()};
    }

    public static SoundEvent[] voiceSounds() {
        return new SoundEvent[]{voices1.get(), voices2.get(), voices3.get(), voices4.get(), voices5.get(), voices6.get(), voices7.get(), voices8.get()};
    }

    public static SoundEvent[] boilerGroanSounds() {
        return new SoundEvent[]{boiler_groan1.get(), boiler_groan2.get(), boiler_groan3.get()};
    }

    /**
     * CE's {@code getOrCreate(ResourceLocation)}: builds an ad-hoc, unregistered {@link SoundEvent}
     * for callers that only have a raw location (e.g. gun configs pointing at a custom sound). Unlike
     * the fields above, this does not go through {@link #SOUND_EVENTS} - registries are frozen after
     * the {@code RegisterEvent} phase in NeoForge, so a sound minted after that point cannot be added
     * to {@code BuiltInRegistries.SOUND_EVENT}. It still plays correctly through client-side sound
     * APIs, which only need a {@link SoundEvent} instance and its location, not a registry entry.
     */
    public static SoundEvent getOrCreate(ResourceLocation loc) {
        return DYNAMIC_SOUNDS.computeIfAbsent(loc, SoundEvent::createVariableRangeEvent);
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
