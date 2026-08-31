package com.hbm.main;

import com.hbm.blockentity.bomb.LaunchPadBaseBlockEntity;
import com.hbm.entity.mob.CreeperVariantEntityTypes;
import com.hbm.entity.mob.EntityCreeperGold;
import com.hbm.entity.mob.EntityCreeperNuclear;
import com.hbm.entity.mob.EntityCreeperPhosgene;
import com.hbm.entity.mob.EntityCreeperTainted;
import com.hbm.entity.mob.EntityCreeperVolatile;
import com.hbm.entity.mob.EntityBOTPrimeBody;
import com.hbm.entity.mob.EntityBOTPrimeHead;
import com.hbm.entity.mob.EntityCyberCrab;
import com.hbm.entity.mob.EntityDuck;
import com.hbm.entity.mob.EntityHunterChopper;
import com.hbm.entity.mob.EntityMaskMan;
import com.hbm.entity.mob.EntityTaintCrab;
import com.hbm.entity.mob.EntityTeslaCrab;
import com.hbm.entity.mob.EntityUFO;
import com.hbm.entity.mob.EntityRADBeast;
import com.hbm.entity.mob.MaskmanEntityTypes;
import com.hbm.entity.mob.Phase4BossEntityTypes2;
import com.hbm.entity.mob.RadBeastEntityTypes;
import com.hbm.entity.mob.WormEntityTypes;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.HazmatRegistry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.inventory.recipes.chem.CentrifugeRecipes;
import com.hbm.inventory.recipes.chem.ChemPlantRecipes;
import com.hbm.inventory.recipes.chem.CyclotronRecipes;
import com.hbm.inventory.recipes.chem.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.chem.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.chem.SILEXRecipes;
import com.hbm.itempool.ItemPoolsSatellite;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.saveddata.satellites.Satellite;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/**
 * Mod-bus common setup. {@code bus = Bus.MOD} is required: {@link FMLCommonSetupEvent} implements
 * {@code net.neoforged.fml.event.IModBusEvent} and only ever fires on the mod bus - confirmed against
 * FancyModLoader's {@code EventBusSubscriber} javadoc, which states {@code bus()} defaults to
 * {@code Bus.GAME} and does not auto-detect {@code IModBusEvent}. The game-bus per-entity tick
 * dispatch that used to live in this class was split out to {@link CommonTickEvents} for exactly this
 * reason - a single {@code @EventBusSubscriber} class can only subscribe to one bus.
 */
@EventBusSubscriber(modid = MainRegistry.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CommonEvents {

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            HazardRegistry.registerTrafos();
            HazardRegistry.registerItems();
            HazardRegistry.registerContaminatingDrops();
            // Flushes com.hbm.items.gear.ArmorFSB#setHazardClass's accumulated self-registrations
            // into com.hbm.util.ArmorRegistry - confirmed real call-site timing via Neo Edition's
            // own CommonEvents.commonSetup, which calls ArmorUtil.register() from this exact event.
            ArmorUtil.register();
            // CE: FMLPreInitializationEvent-time HazmatRegistry.registerHazmats() call. This port
            // splits out just the initDefault() half (registerHazmats()'s Gson config-file
            // persistence is not ported - see HazmatRegistry's own javadoc); currently a no-op
            // beyond flushing HazmatRegistry.external, since nothing populates that list yet.
            HazmatRegistry.initDefault();
            // Package C (weapon-mod eval chain) - must run after every Item/BulletConfig in
            // com.hbm.items.weapon.sedna.** has registered (RegisterEvent has already fully fired by
            // the time enqueueWork's Runnable executes), see XWeaponModManager's own class javadoc.
            XWeaponModManager.init();
            // Phase 3 (missile_launch_infra) - must run after every MissileItems/MissileEntityTypes
            // DeferredHolder has registered, matching XWeaponModManager's own timing reasoning above.
            LaunchPadBaseBlockEntity.registerLaunchables();
            // Phase 3 (missile_launch_infra) - populates com.hbm.saveddata.satellites.Satellite's
            // fixed, order-sensitive registry (see that class's own javadoc on why order matters).
            Satellite.register();
            // Phase 2 (oil_production_chain / machines_chemical_isotope) - moved here from
            // OilChainBlocks.registerAll()/ChemIsotopeBlocks.registerAll(): each of these hand-coded
            // recipe tables resolves DeferredItem.get() eagerly while building its ItemStack outputs
            // (e.g. PlateCrystalWasteItems.CRYSTAL_SULFUR, IngotNuggetItems.NUGGET_U238), so calling
            // them from a block registerAll() - which runs synchronously inside MainRegistry's
            // constructor, strictly before any RegisterEvent fires - threw IllegalStateException at
            // startup (same bug class as the sedna gun eager-field crash, see XFactory556mm's javadoc).
            // Each keeps its own idempotent "registered" guard, so calling them here (well after every
            // RegisterEvent has fired) is safe and still runs exactly once.
            RefineryRecipes.registerRefinery();
            CentrifugeRecipes.register();
            GasCentrifugeRecipes.register();
            SILEXRecipes.register();
            CyclotronRecipes.register();
            ChemPlantRecipes.register();
            ElectrolyserFluidRecipes.register();
            // Phase 4 (fallout_rain_and_effects) - com.hbm.config.FalloutConfigJSON#initDefault()
            // eagerly resolves several DeferredBlock.get() calls (e.g. WastelandVirusBlocks.SELLAFIELD)
            // while building its default block-transform table, so - same reasoning as the recipe
            // tables above - it must run after every block RegisterEvent has fired, not from
            // MainRegistry's constructor (unlike ExplosionNukeGeneric.loadSoliniumFromFile(), whose
            // solinium.cfg table resolves block ids lazily by name at actual use time instead).
            com.hbm.config.FalloutConfigJSON.initialize();
            // Phase 4 (satellites_followup_and_loot_pools) - com.hbm.itempool.ItemPoolsSatellite#init()
            // eagerly resolves several DeferredItem.get() calls (e.g. BilletPowderItems.POWDER_IRON)
            // while building its weighted pool entries, same reasoning as the recipe tables above -
            // must run after every item RegisterEvent has fired, not from a static field initializer.
            ItemPoolsSatellite.init();
            // Phase 4 (entities_vehicles_aircraft / entities_orbital_and_beam_payloads) -
            // com.hbm.itempool.ItemPoolsC130#init() has the exact same DeferredItem.get()-during-
            // pool-construction timing requirement as ItemPoolsSatellite#init() above.
            com.hbm.itempool.ItemPoolsC130.init();
            // Phase 8 — structure loot (CE ItemPoolsLegacy.java). Same DeferredItem.get() timing.
            com.hbm.itempool.ItemPoolsLegacy.init();
        });
    }

    /**
     * Phase 4 (entities_creeper_variants) - {@link EntityAttributeCreationEvent} for this port's
     * first {@code MobCategory.MONSTER} entities. Confirmed real call shape via Neo Edition's own
     * compiling {@code CommonEvents.onEntityAttributeCreation}
     * ({@code event.put(NtmEntityTypes.CREEPER_NUCLEAR.get(), CreeperNuclear.createAttributes().build())}).
     * Safe to call {@code .get()} on each {@code CreeperVariantEntityTypes} holder directly here:
     * {@link EntityAttributeCreationEvent}, like {@code FMLCommonSetupEvent} above, always fires
     * after every registry's {@code RegisterEvent} has completed.
     */
    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(CreeperVariantEntityTypes.CREEPER_GOLD.get(), EntityCreeperGold.createAttributes().build());
        event.put(CreeperVariantEntityTypes.CREEPER_VOLATILE.get(), EntityCreeperVolatile.createAttributes().build());
        event.put(CreeperVariantEntityTypes.CREEPER_PHOSGENE.get(), EntityCreeperPhosgene.createAttributes().build());
        event.put(CreeperVariantEntityTypes.CREEPER_TAINTED.get(), EntityCreeperTainted.createAttributes().build());
        event.put(CreeperVariantEntityTypes.CREEPER_NUCLEAR.get(), EntityCreeperNuclear.createAttributes().build());

        // Phase 4 (entities_bosses - BOTPrime worm boss). Same "safe to .get() here" reasoning as the
        // creeper variants above.
        event.put(WormEntityTypes.BOTPRIME_HEAD.get(), EntityBOTPrimeHead.createAttributes().build());
        event.put(WormEntityTypes.BOTPRIME_BODY.get(), EntityBOTPrimeBody.createAttributes().build());

        // Phase 4 (entities_bosses - MaskMan). Same "safe to .get() here" reasoning as above.
        event.put(MaskmanEntityTypes.MASK_MAN.get(), EntityMaskMan.createAttributes().build());

        // Phase 4 (entities_bosses / entities_vehicles_aircraft - UFO boss, Hunter Chopper, the Cyber
        // Crab family, EntityDuck/EntityQuackos). Same "safe to .get() here" reasoning as above.
        event.put(Phase4BossEntityTypes2.UFO.get(), EntityUFO.createAttributes().build());
        event.put(Phase4BossEntityTypes2.HUNTER_CHOPPER.get(), EntityHunterChopper.createAttributes().build());
        event.put(Phase4BossEntityTypes2.CYBER_CRAB.get(), EntityCyberCrab.createAttributes().build());
        event.put(Phase4BossEntityTypes2.TAINT_CRAB.get(), EntityTaintCrab.createAttributes().build());
        event.put(Phase4BossEntityTypes2.TESLA_CRAB.get(), EntityTeslaCrab.createAttributes().build());
        event.put(Phase4BossEntityTypes2.DUCK.get(), EntityDuck.createAttributes().build());
        // EntityQuackos has no createAttributes() override of its own - it inherits EntityDuck's
        // attribute set verbatim (CE's own EntityQuackos never overrides applyEntityAttributes either;
        // its invulnerability comes from getIsInvulnerable()/setHealth, not from a different max health).
        event.put(Phase4BossEntityTypes2.QUACKOS.get(), EntityDuck.createAttributes().build());

        // Phase 4 (entities_bosses - RAD Beast, boss-adjacent elite). Same "safe to .get() here"
        // reasoning as above.
        event.put(RadBeastEntityTypes.RAD_BEAST.get(), EntityRADBeast.createAttributes().build());
    }
}
