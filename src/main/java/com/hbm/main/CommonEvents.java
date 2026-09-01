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
import com.hbm.entity.mob.Phase9MobEntityTypes;
import com.hbm.entity.mob.EntityGlowingOne;
import com.hbm.entity.mob.EntityGhost;
import com.hbm.entity.mob.EntityFBI;
import com.hbm.entity.mob.EntityFBIDrone;
import com.hbm.entity.mob.EntityUndeadSoldier;
import com.hbm.entity.mob.EntityPigeon;
import com.hbm.entity.mob.EntityPlasticBag;
import com.hbm.entity.mob.EntityParasiteMaggot;
import com.hbm.entity.mob.EntityBlockSpider;
import com.hbm.entity.mob.EntityDummy;
import com.hbm.entity.mob.glyphid.GlyphidEntityTypes;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.entity.mob.glyphid.EntityGlyphidBehemoth;
import com.hbm.entity.mob.glyphid.EntityGlyphidBlaster;
import com.hbm.entity.mob.glyphid.EntityGlyphidBombardier;
import com.hbm.entity.mob.glyphid.EntityGlyphidBrawler;
import com.hbm.entity.mob.glyphid.EntityGlyphidBrenda;
import com.hbm.entity.mob.glyphid.EntityGlyphidDigger;
import com.hbm.entity.mob.glyphid.EntityGlyphidNuclear;
import com.hbm.entity.mob.glyphid.EntityGlyphidScout;
import com.hbm.entity.mob.WormEntityTypes;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.HazmatRegistry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.inventory.recipes.LiquefactionRecipes;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.inventory.recipes.ArcWelderRecipes;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipes;
import com.hbm.inventory.recipes.PlasmaForgeRecipes;
import com.hbm.inventory.recipes.SolderingRecipes;
import com.hbm.inventory.recipes.AnnihilatorRecipes;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT;
import com.hbm.inventory.recipes.CombinationRecipes;
import com.hbm.inventory.recipes.LemegetonRecipes;
import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.inventory.recipes.RockMillRecipes;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes;
import com.hbm.inventory.recipes.WasteDrumRecipes;
import com.hbm.inventory.recipes.FractionRecipes;
import com.hbm.inventory.recipes.PUREXRecipes;
import com.hbm.inventory.recipes.SolidificationRecipes;
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
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

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
            PUREXRecipes.register();
            LiquefactionRecipes.register();
            SolidificationRecipes.register();
            ParticleAcceleratorRecipes.register();
            AmmoPressRecipes.register();
            ArcWelderRecipes.register();
            SolderingRecipes.register();
            PlasmaForgeRecipes.register();
            OutgasserRecipes.register();
            LemegetonRecipes.register();
            CombinationRecipes.register();
            BlastFurnaceRecipesNT.register();
            RockMillRecipes.register();
            AnnihilatorRecipes.register();
            PressRecipes.register();
            RotaryFurnaceRecipes.register();
            WasteDrumRecipes.register();
            FractionRecipes.register();
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
            // Phase 8 — structure loot (CE ItemPools*.java). Same DeferredItem.get() timing.
            com.hbm.itempool.ItemPoolsLegacy.init();
            com.hbm.itempool.ItemPoolsComponent.init();
            com.hbm.itempool.ItemPoolsSingle.init();
            com.hbm.itempool.ItemPoolsRedRoom.init();
            com.hbm.itempool.ItemPoolsVendingMachine.init();
            com.hbm.itempool.ItemPoolsPile.init();
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

        // Phase 9 remaining CE mobs / glyphids.
        event.put(Phase9MobEntityTypes.GLOWING_ONE.get(), EntityGlowingOne.createAttributes().build());
        event.put(Phase9MobEntityTypes.GHOST.get(), EntityGhost.createAttributes().build());
        event.put(Phase9MobEntityTypes.FBI.get(), EntityFBI.createAttributes().build());
        event.put(Phase9MobEntityTypes.FBI_DRONE.get(), EntityFBIDrone.createAttributes().build());
        event.put(Phase9MobEntityTypes.UNDEAD_SOLDIER.get(), EntityUndeadSoldier.createAttributes().build());
        event.put(Phase9MobEntityTypes.PIGEON.get(), EntityPigeon.createAttributes().build());
        event.put(Phase9MobEntityTypes.PLASTIC_BAG.get(), EntityPlasticBag.createAttributes().build());
        event.put(Phase9MobEntityTypes.PARASITE_MAGGOT.get(), EntityParasiteMaggot.createAttributes().build());
        event.put(Phase9MobEntityTypes.BLOCK_SPIDER.get(), EntityBlockSpider.createAttributes().build());
        event.put(Phase9MobEntityTypes.DUMMY.get(), EntityDummy.createAttributes().build());
        event.put(GlyphidEntityTypes.GLYPHID.get(), EntityGlyphid.createAttributes().build());
        event.put(GlyphidEntityTypes.BOMBARDIER.get(), EntityGlyphidBombardier.createAttributes().build());
        event.put(GlyphidEntityTypes.BLASTER.get(), EntityGlyphidBlaster.createAttributes().build());
        event.put(GlyphidEntityTypes.BRAWLER.get(), EntityGlyphidBrawler.createAttributes().build());
        event.put(GlyphidEntityTypes.BEHEMOTH.get(), EntityGlyphidBehemoth.createAttributes().build());
        event.put(GlyphidEntityTypes.BRENDA.get(), EntityGlyphidBrenda.createAttributes().build());
        event.put(GlyphidEntityTypes.DIGGER.get(), EntityGlyphidDigger.createAttributes().build());
        event.put(GlyphidEntityTypes.NUCLEAR.get(), EntityGlyphidNuclear.createAttributes().build());
        event.put(GlyphidEntityTypes.SCOUT.get(), EntityGlyphidScout.createAttributes().build());
    }

    /**
     * Dedicated-server {@code RegisterSpawnPlacementsEvent} — gold/phosgene/volatile (and the other
     * two creeper variants) were missing this; vanilla {@code Monster} on-ground rules match CE's
     * creeper-shaped mobs. {@code bus=MOD} already on this class.
     */
    @SubscribeEvent
    public static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        registerCreeper(event, CreeperVariantEntityTypes.CREEPER_GOLD.get());
        registerCreeper(event, CreeperVariantEntityTypes.CREEPER_VOLATILE.get());
        registerCreeper(event, CreeperVariantEntityTypes.CREEPER_PHOSGENE.get());
        registerCreeper(event, CreeperVariantEntityTypes.CREEPER_TAINTED.get());
        registerCreeper(event, CreeperVariantEntityTypes.CREEPER_NUCLEAR.get());
        event.register(Phase9MobEntityTypes.GLOWING_ONE.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(Phase9MobEntityTypes.FBI.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(Phase9MobEntityTypes.UNDEAD_SOLDIER.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityUndeadSoldier::checkUndeadSpawn,
                RegisterSpawnPlacementsEvent.Operation.OR);
        event.register(Phase9MobEntityTypes.PARASITE_MAGGOT.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.OR);
        registerCreeper(event, GlyphidEntityTypes.GLYPHID.get());
        registerCreeper(event, GlyphidEntityTypes.BOMBARDIER.get());
        registerCreeper(event, GlyphidEntityTypes.BLASTER.get());
        registerCreeper(event, GlyphidEntityTypes.BRAWLER.get());
        registerCreeper(event, GlyphidEntityTypes.BEHEMOTH.get());
        registerCreeper(event, GlyphidEntityTypes.BRENDA.get());
        registerCreeper(event, GlyphidEntityTypes.DIGGER.get());
        registerCreeper(event, GlyphidEntityTypes.NUCLEAR.get());
        registerCreeper(event, GlyphidEntityTypes.SCOUT.get());
    }

    private static void registerCreeper(RegisterSpawnPlacementsEvent event, net.minecraft.world.entity.EntityType<? extends Monster> type) {
        event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.OR);
    }
}
