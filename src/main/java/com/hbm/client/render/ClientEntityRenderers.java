package com.hbm.client.render;

import net.minecraft.client.renderer.entity.EntityRenderers;

import com.hbm.entity.ConveyorEntityTypes;
import com.hbm.entity.GunEntityTypes;
import com.hbm.entity.cart.CartEntityTypes;
import com.hbm.entity.effect.EffectEntityTypes;
import com.hbm.entity.effect.GravityWellEntityTypes;
import com.hbm.entity.grenade.GrenadeEntityTypes;
import com.hbm.entity.item.BoatEntityTypes;
import com.hbm.entity.item.DroneEntityTypes;
import com.hbm.entity.item.ParachuteCrateEntityTypes;
import com.hbm.entity.item.TntPrimedEntityTypes;
import com.hbm.entity.logic.NukeEntityTypes;
import com.hbm.entity.logic.PlaneEntityTypes;
import com.hbm.entity.logic.SatellitePayloadEntityTypes;
import com.hbm.entity.missile.MissileEntityTypes;
import com.hbm.entity.mob.CreeperVariantEntityTypes;
import com.hbm.entity.mob.Phase9MobEntityTypes;
import com.hbm.entity.mob.glyphid.GlyphidEntityTypes;
import com.hbm.entity.mob.MaskmanEntityTypes;
import com.hbm.entity.mob.Phase4BossEntityTypes2;
import com.hbm.entity.mob.RadBeastEntityTypes;
import com.hbm.entity.mob.WormEntityTypes;
import com.hbm.entity.projectile.ChopperMineEntityTypes;
import com.hbm.entity.projectile.FallingNukeEntityTypes;
import com.hbm.entity.projectile.MeteorEntityTypes;
import com.hbm.entity.projectile.Phase9TailEntityTypes;
import com.hbm.entity.projectile.RubbleEntityTypes;
import com.hbm.entity.train.TrainEntityTypes;

import com.hbm.client.render.entity.effect.BlackHoleRenderer;
import com.hbm.client.render.entity.effect.CloudFleijaRenderer;
import com.hbm.client.render.entity.effect.CloudSoliniumRenderer;
import com.hbm.client.render.entity.effect.CloudTomRenderer;
import com.hbm.client.render.entity.effect.EmpBlastRenderer;
import com.hbm.client.render.entity.effect.QuasarRenderer;
import com.hbm.client.render.entity.effect.TorexRenderer;
import com.hbm.client.render.entity.logic.BomberRenderer;
import com.hbm.client.render.entity.logic.DeathBlastRenderer;
import com.hbm.client.render.entity.logic.OrbitalLaserRenderer;
import com.hbm.client.render.entity.missile.MirvRenderer;
import com.hbm.client.render.entity.mob.CyberCrabRenderer;
import com.hbm.client.render.entity.mob.DuckRenderer;
import com.hbm.client.render.entity.mob.HunterChopperRenderer;
import com.hbm.client.render.entity.mob.MaskManRenderer;
import com.hbm.client.render.entity.mob.QuackosRenderer;
import com.hbm.client.render.entity.mob.RadBeastRenderer;
import com.hbm.client.render.entity.mob.TaintCrabRenderer;
import com.hbm.client.render.entity.mob.TeslaCrabRenderer;
import com.hbm.client.render.entity.mob.UfoRenderer;
import com.hbm.client.render.entity.mob.WormBodyRenderer;
import com.hbm.client.render.entity.mob.WormHeadRenderer;

/**
 * Bulk safe-fallback {@link EntityRenderers#register} pass for every one of this port's ~23
 * {@code *EntityTypes.java} {@code DeferredRegister<EntityType<?>>} holders (94 concrete
 * {@code DeferredHolder<EntityType<?>, EntityType<X>>} fields total as of Phase 5, enumerated below by
 * source file), none of which had ANY client renderer registered anywhere in this port before this
 * class was added (confirmed by a whole-repo grep for {@code EntityRenderers.register} returning zero
 * hits prior to this file).
 *
 * <p><b>Why this exists / why it is not optional polish:</b> CE (1.12.2 Forge,
 * {@code upstream/hbm-ce/.../main/ClientProxy.java}) only ever called
 * {@code net.minecraftforge.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler}
 * for entities that already had a real renderer written — skipping it for the rest was harmless,
 * Forge 1.12.2 just drew nothing for an unregistered entity. NeoForge 1.21.1 removed that leniency:
 * {@code net.minecraft.client.renderer.entity.EntityRenderDispatcher} throws the moment it is asked to
 * render an {@code EntityType} with no {@link EntityRenderers#register} call anywhere. That call is a
 * plain vanilla static method (not a NeoForge event), confirmed real/legal from inside
 * {@code FMLClientSetupEvent.enqueueWork} by {@code upstream/neo-edition}'s own compiling
 * {@code ClientProxy.registerEntityRenderers()} (grepped for {@code EntityRenderers.register} — used
 * strictly to confirm this API shape, not for behavior) and independently corroborated by this port's
 * own {@code docs/phase5/boss_and_vehicle_entity_renderers.md} and
 * {@code docs/phase5/reactor_and_explosion_visual_effects.md} research reports (both already concluded
 * the identical registration shape before this class was written). This means every Phase 3/4 entity —
 * guns, missiles, grenades, mines, meteors, gravity wells, trains, carts, planes, drones, bosses,
 * creeper variants — was a live client-crash landmine the instant one was spawned near a client; this
 * class is the single foundation fix that makes all of Phase 4's already-shipped spawn logic safe to
 * playtest.
 *
 * <p><b>Fallback selection rule (kept deliberately simple, one line of judgment per entity — see each
 * inline comment below for the one-line reason, not a design essay):</b>
 * <ul>
 *   <li>{@link EmptyEntityRenderer} (fully invisible, no nameplate) for pure-logic/collision-only
 *   entities with no meaningful body of their own: flying ordnance (bullets, missiles, grenades,
 *   mines, the falling nuke and TOM payload bombs, meteors, rubble debris), blast/cloud/FX marker
 *   entities (nuke explosions, EMP/fire/mist/torex clouds, satellite lasers/blasts, gravity wells),
 *   the conveyor's moving-item ghost, primed TNT, and the two train dummy/seat marker entities. A
 *   floating nameplate on any of these would be actively misleading, not helpfully "debuggable."</li>
 *   <li>{@link FallbackEntityRenderer} (invisible body, vanilla nameplate-when-named/looked-at) for
 *   entities that are meant to be persistent, player-visible world objects: living mobs/bosses,
 *   vehicles (planes, trains, minecarts, drones), and the parachute crate. Seeing at least a nameplate
 *   marker for these is a genuinely useful debug aid until the Content wave's bespoke renderer lands
 *   (per this task's brief: "better UX to see a placeholder than nothing debuggable").</li>
 * </ul>
 *
 * <p><b>This is explicitly a foundation-wave placeholder layer, not the Content wave's finished
 * renderers.</b> Every call below is written as its own labeled statement (never a loop over a
 * computed list) specifically so a later Content-wave agent can find-and-replace exactly one
 * {@code EntityRenderers.register(...)} line per entity with a real, CE-faithful renderer (see
 * {@code docs/phase5/boss_and_vehicle_entity_renderers.md} and
 * {@code docs/phase5/reactor_and_explosion_visual_effects.md} for the per-entity work already
 * researched) without touching any other entity's line or this class's structure. Some entries —
 * simple projectiles, pure blast/cloud FX, the train dummy/seat markers — may legitimately stay on
 * {@link EmptyEntityRenderer} forever; that is a correct end state for those, not a leftover TODO.
 *
 * <h2>Review finding (r4-rbmk-explosion-vfx-review): {@link #registerAll()} was never called</h2>
 * This class's own javadoc claims {@link #registerAll()} is "called once from {@code
 * com.hbm.main.ClientModRegistry#onClientSetup}'s {@code FMLClientSetupEvent.enqueueWork(...)}
 * lambda" - but that lambda (grep-confirmed, {@code ClientModRegistry.java}'s own {@code
 * onClientSetup}) is still empty, and a whole-repo grep for {@code ClientEntityRenderers} turned up
 * no call to {@link #registerAll()} anywhere at all. Concretely: <b>every one of this port's ~94
 * custom {@code EntityType}s currently has no renderer registered</b> - not even the safe {@link
 * EmptyEntityRenderer}/{@link FallbackEntityRenderer} fallback this whole class exists to provide -
 * which per this class's own "why this exists" section above is a guaranteed
 * {@code EntityRenderDispatcher} crash the instant any one of them is rendered client-side, not a
 * cosmetic gap. {@code ClientModRegistry.java} is a forbidden shared-aggregator file this task may
 * not edit directly (ground rule 7) - the one-line fix ({@code
 * com.hbm.client.render.ClientEntityRenderers.registerAll();} inside {@code onClientSetup}'s {@code
 * enqueueWork} lambda, matching {@code docs/phase5/renderer_framework_and_obj_models.md}'s own
 * confirmed-real "plain vanilla static method, called directly from {@code
 * FMLClientSetupEvent.enqueueWork}" pattern - deliberately <b>not</b> a self-registering {@code
 * EntityRenderersEvent.RegisterRenderers} subscriber, which would diverge from that already-
 * researched convention and risk a double-registration if a coordinator also wires the {@code
 * enqueueWork} call per that convention) is reported as this task's own {@code wiringSnippets}
 * entry for the coordinator to apply.
 */
public final class ClientEntityRenderers {

    private ClientEntityRenderers() {}

    public static void registerAll() {
        registerGuns();
        registerMissiles();
        registerMobsAndBosses();
        registerProjectilesAndDebris();
        registerConveyor();
        registerGrenades();
        registerTrains();
        registerParachuteCrate();
        registerDrones();
        registerTntPrimed();
        registerCarts();
        registerGravityWells();
        registerEffects();
        registerNukes();
        registerSatellitePayloads();
        registerPlanes();
        registerPhase9Tails();
    }

    /** {@code com.hbm.entity.GunEntityTypes} - hitscan-adjacent bullet/coin projectiles, invisible. */
    private static void registerGuns() {
        // MK4 bullet - fired projectile, no body worth a nameplate.
        EntityRenderers.register(GunEntityTypes.BULLET_MK4.get(), EmptyEntityRenderer::new);
        // MK4 "CL" (cluster/child?) bullet variant - same as above.
        EntityRenderers.register(GunEntityTypes.BULLET_MK4CL.get(), EmptyEntityRenderer::new);
        // Beam-weapon projectile - instantaneous/fast, invisible fallback.
        EntityRenderers.register(GunEntityTypes.BULLET_BEAM.get(), EmptyEntityRenderer::new);
        // Thrown coin entity - small thrown-item-like projectile, invisible fallback.
        EntityRenderers.register(GunEntityTypes.COIN.get(), EmptyEntityRenderer::new);
    }

    /**
     * {@code com.hbm.entity.missile.MissileEntityTypes} - every missile/warhead tier. All invisible:
     * this task's own brief explicitly flags missiles as "pure-logic projectiles, fine invisible."
     */
    private static void registerMissiles() {
        EntityRenderers.register(MissileEntityTypes.CUSTOM.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.ANTI_BALLISTIC.get(), EmptyEntityRenderer::new);
        // Coordinator fix: c4-boss-vehicle-renderers-batch1 built MirvRenderer (CE's one
        // IConstantRenderer-gated, visually-rendered missile sub-munition) but the wiring snippet
        // targeting this line was never applied - same unwired-bespoke-renderer pattern as the r4/r5
        // review already fixed elsewhere in this file.
        EntityRenderers.register(MissileEntityTypes.MIRV.get(), MirvRenderer::new);
        // Tier 0
        EntityRenderers.register(MissileEntityTypes.TEST.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.MICRO.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.SCHRABIDIUM.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.BHOLE.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.TAINT.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.EMP.get(), EmptyEntityRenderer::new);
        // Tier 1
        EntityRenderers.register(MissileEntityTypes.GENERIC.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.DECOY.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.INCENDIARY.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.CLUSTER.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.BUNKER_BUSTER.get(), EmptyEntityRenderer::new);
        // Tier 2
        EntityRenderers.register(MissileEntityTypes.STRONG.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.INCENDIARY_STRONG.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.CLUSTER_STRONG.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.BUSTER_STRONG.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.EMP_STRONG.get(), EmptyEntityRenderer::new);
        // Tier 3
        EntityRenderers.register(MissileEntityTypes.BURST.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.INFERNO.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.RAIN.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.DRILL.get(), EmptyEntityRenderer::new);
        // Tier 4
        EntityRenderers.register(MissileEntityTypes.NUCLEAR.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.NUCLEAR_MIRV.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.VOLCANO.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.DOOMSDAY.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.DOOMSDAY_RUSTED.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.N2.get(), EmptyEntityRenderer::new);
        // Scripted/special missiles
        EntityRenderers.register(MissileEntityTypes.STEALTH.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(MissileEntityTypes.SHUTTLE.get(), EmptyEntityRenderer::new);
    }

    /**
     * Living mobs, bosses, and boss-adjacent creatures from {@code com.hbm.entity.mob.*} - persistent,
     * player-visible creatures, so the nameplate-visible fallback (not fully invisible).
     *
     * <p><b>Review fix (r5-boss-vehicle-renderer-review):</b> every entity below except the 5
     * {@code CreeperVariantEntityTypes} reskins had a real, already-committed, already-CE-cross-checked
     * bespoke renderer class sitting unused under {@code com.hbm.client.render.entity.mob} (
     * {@link RadBeastRenderer}/{@link MaskManRenderer}/{@link WormHeadRenderer}/{@link
     * WormBodyRenderer}/{@link UfoRenderer}/{@link HunterChopperRenderer}/{@link CyberCrabRenderer}/
     * {@link TaintCrabRenderer}/{@link TeslaCrabRenderer}/{@link DuckRenderer}/{@link
     * QuackosRenderer}, all ported per {@code docs/phase5/boss_and_vehicle_entity_renderers.md} and
     * spot-checked against their cited CE source in this review) while this method kept registering
     * {@link FallbackEntityRenderer} for every one of them - the exact same "renderer class exists,
     * registration line was never swapped" bug already fixed once in {@link #registerEffects()}/
     * {@link #registerSatellitePayloads()} by the r4 review, just not yet caught here. Swapped in
     * below. {@code CreeperVariantEntityTypes}' 5 reskins are untouched - no bespoke renderer exists
     * for them anywhere in this port yet (confirmed by a repo-wide search of this task's own review);
     * they correctly stay on {@link FallbackEntityRenderer} as a punch-list item for whichever task
     * owns the creeper-variant family.
     */
    private static void registerMobsAndBosses() {
        // com.hbm.entity.mob.RadBeastEntityTypes - vanilla BlazeModel body + ModelBiped mask overlay.
        EntityRenderers.register(RadBeastEntityTypes.RAD_BEAST.get(), RadBeastRenderer::new);
        // com.hbm.entity.mob.MaskmanEntityTypes - OBJ-rigged multi-part humanoid.
        EntityRenderers.register(MaskmanEntityTypes.MASK_MAN.get(), MaskManRenderer::new);
        // com.hbm.entity.mob.WormEntityTypes - segmented boss, head + body parts, both single-mesh OBJ.
        EntityRenderers.register(WormEntityTypes.BOTPRIME_HEAD.get(), WormHeadRenderer::new);
        EntityRenderers.register(WormEntityTypes.BOTPRIME_BODY.get(), WormBodyRenderer::new);
        // com.hbm.entity.mob.Phase4BossEntityTypes2
        EntityRenderers.register(Phase4BossEntityTypes2.UFO.get(), UfoRenderer::new);
        EntityRenderers.register(Phase4BossEntityTypes2.HUNTER_CHOPPER.get(), HunterChopperRenderer::new);
        EntityRenderers.register(Phase4BossEntityTypes2.CYBER_CRAB.get(), CyberCrabRenderer::new);
        EntityRenderers.register(Phase4BossEntityTypes2.TAINT_CRAB.get(), TaintCrabRenderer::new);
        EntityRenderers.register(Phase4BossEntityTypes2.TESLA_CRAB.get(), TeslaCrabRenderer::new);
        // Vanilla-chicken-shaped reskins - thin ChickenRenderer subclasses, texture override (+ 25x
        // scale for Quackos) only.
        EntityRenderers.register(Phase4BossEntityTypes2.DUCK.get(), DuckRenderer::new);
        EntityRenderers.register(Phase4BossEntityTypes2.QUACKOS.get(), QuackosRenderer::new);
        // com.hbm.entity.mob.CreeperVariantEntityTypes - vanilla-Creeper-shaped reskins; still on the
        // generic fallback, no bespoke renderer exists for this family anywhere in this port yet
        // (out of this review's own c4/c5 scope - docs/phase5/boss_and_vehicle_entity_renderers.md
        // does not cover the creeper family at all). Content wave should swap these to vanilla
        // CreeperRenderer (cheap, correct-shaped) per the same pattern DuckRenderer/CreeperNuclearRenderer
        // (Neo Edition) already establish.
        EntityRenderers.register(CreeperVariantEntityTypes.CREEPER_GOLD.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(CreeperVariantEntityTypes.CREEPER_VOLATILE.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(CreeperVariantEntityTypes.CREEPER_PHOSGENE.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(CreeperVariantEntityTypes.CREEPER_TAINTED.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(CreeperVariantEntityTypes.CREEPER_NUCLEAR.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(Phase9MobEntityTypes.GLOWING_ONE.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(Phase9MobEntityTypes.GHOST.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(Phase9MobEntityTypes.FBI.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(Phase9MobEntityTypes.FBI_DRONE.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(Phase9MobEntityTypes.UNDEAD_SOLDIER.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(Phase9MobEntityTypes.PIGEON.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(Phase9MobEntityTypes.PLASTIC_BAG.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(Phase9MobEntityTypes.PARASITE_MAGGOT.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(Phase9MobEntityTypes.BLOCK_SPIDER.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(Phase9MobEntityTypes.DUMMY.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(GlyphidEntityTypes.GLYPHID.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(GlyphidEntityTypes.BOMBARDIER.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(GlyphidEntityTypes.BLASTER.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(GlyphidEntityTypes.BRAWLER.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(GlyphidEntityTypes.BEHEMOTH.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(GlyphidEntityTypes.BRENDA.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(GlyphidEntityTypes.DIGGER.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(GlyphidEntityTypes.NUCLEAR.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(GlyphidEntityTypes.SCOUT.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(BoatEntityTypes.BOAT_RUBBER.get(), FallbackEntityRenderer::new);
    }

    /**
     * Free-flying ordnance/debris from {@code com.hbm.entity.projectile.*} - same "invisible projectile"
     * bucket as missiles/bullets above.
     */
    private static void registerProjectilesAndDebris() {
        // Dropped nuclear bomb casing (gravity bomb), ballistic like a missile - invisible.
        EntityRenderers.register(FallingNukeEntityTypes.FALLING_NUKE.get(), EmptyEntityRenderer::new);
        // Chopper-dropped mine - small dropped ordnance, invisible.
        EntityRenderers.register(ChopperMineEntityTypes.CHOPPER_MINE.get(), EmptyEntityRenderer::new);
        // Falling meteor - environmental hazard projectile, invisible.
        EntityRenderers.register(MeteorEntityTypes.METEOR.get(), EmptyEntityRenderer::new);
        // Explosion/impact rubble debris chunk - transient FX-adjacent projectile, invisible.
        EntityRenderers.register(RubbleEntityTypes.RUBBLE.get(), EmptyEntityRenderer::new);
    }

    /** {@code com.hbm.entity.ConveyorEntityTypes} - the belt's moving-item ghost entity, invisible. */
    private static void registerConveyor() {
        // A nameplate on an item riding a conveyor belt would look broken; Content wave renders this
        // one via an item-icon-style renderer, not a nameplate.
        EntityRenderers.register(ConveyorEntityTypes.MOVING_ITEM.get(), EmptyEntityRenderer::new);
    }

    /** {@code com.hbm.entity.grenade.GrenadeEntityTypes} - thrown grenades, invisible like bullets. */
    private static void registerGrenades() {
        EntityRenderers.register(GrenadeEntityTypes.GRENADE_UNIVERSAL.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(GrenadeEntityTypes.GRENADE_BOUNCY_GENERIC.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(GrenadeEntityTypes.GRENADE_IMPACT_GENERIC.get(), EmptyEntityRenderer::new);
        // Gas/chemical disperser canister - same thrown-ordnance bucket.
        EntityRenderers.register(GrenadeEntityTypes.DISPERSER_CANISTER.get(), EmptyEntityRenderer::new);
    }

    /**
     * {@code com.hbm.entity.train.TrainEntityTypes} - the two dummy/marker entities stay invisible
     * forever (they have no body by design), the two real cargo tram cars get the visible fallback.
     */
    private static void registerTrains() {
        // Bounding-box-only helper entity for multi-part rail car hitboxes - no body, ever.
        EntityRenderers.register(TrainEntityTypes.BOUNDING_DUMMY.get(), EmptyEntityRenderer::new);
        // Invisible seat marker entity a player rides inside a rail car - no body, ever.
        EntityRenderers.register(TrainEntityTypes.TRAIN_SEAT.get(), EmptyEntityRenderer::new);
        // Real, player-visible cargo tram car.
        EntityRenderers.register(TrainEntityTypes.CARGO_TRAM.get(), FallbackEntityRenderer::new);
        // Its trailer car - same visibility as the tram itself.
        EntityRenderers.register(TrainEntityTypes.CARGO_TRAM_TRAILER.get(), FallbackEntityRenderer::new);
    }

    /** {@code com.hbm.entity.item.ParachuteCrateEntityTypes} - visible dropped supply crate. */
    private static void registerParachuteCrate() {
        EntityRenderers.register(ParachuteCrateEntityTypes.PARACHUTE_CRATE.get(), FallbackEntityRenderer::new);
    }

    /** {@code com.hbm.entity.item.DroneEntityTypes} - visible logistics drones. */
    private static void registerDrones() {
        EntityRenderers.register(DroneEntityTypes.DELIVERY_DRONE.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(DroneEntityTypes.REQUEST_DRONE.get(), FallbackEntityRenderer::new);
    }

    /** {@code com.hbm.entity.item.TntPrimedEntityTypes} - primed TNT, invisible like other ordnance. */
    private static void registerTntPrimed() {
        EntityRenderers.register(TntPrimedEntityTypes.TNT_PRIMED.get(), EmptyEntityRenderer::new);
    }

    /** {@code com.hbm.entity.cart.CartEntityTypes} - visible custom minecarts (all 5 concrete carts). */
    private static void registerCarts() {
        EntityRenderers.register(CartEntityTypes.CART_ORE.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(CartEntityTypes.CART_POWDER.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(CartEntityTypes.CART_SEMTEX.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(CartEntityTypes.CART_CRATE.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(CartEntityTypes.CART_DESTROYER.get(), FallbackEntityRenderer::new);
    }

    /**
     * {@code com.hbm.entity.effect.GravityWellEntityTypes} - black holes/vortices.
     *
     * <p><b>Review fix (r5-boss-vehicle-renderer-review):</b> all 4 types were still on {@link
     * EmptyEntityRenderer} here, per this method's own now-stale javadoc reasoning that the real
     * visual was "Content-wave work, not this pass's job" - but {@link BlackHoleRenderer}/{@link
     * QuasarRenderer} (real, already-committed, already-CE-cross-checked full ports of CE's {@code
     * RenderBlackHole}/{@code RenderQuasar}, per {@code docs/phase5/boss_and_vehicle_entity_renderers.md}
     * Headline finding #4 and {@code docs/phase5/reactor_and_explosion_visual_effects.md} Headline
     * finding #5) exist under {@code com.hbm.client.render.entity.effect} and were simply never wired
     * in - the same unwired-bespoke-renderer bug found throughout this method's sibling methods.
     * {@link BlackHoleRenderer#render} branches on {@code instanceof EntityVortex}/{@code
     * EntityRagingVortex} internally (disc+jets for the base {@link
     * com.hbm.entity.effect.EntityBlackHole}, swirl-only for {@link com.hbm.entity.effect.EntityVortex},
     * swirl+jets for {@link com.hbm.entity.effect.EntityRagingVortex}), so the same generic class
     * backs all 3 non-Quasar types, matching CE's own one-renderer-for-the-whole-family design (see
     * that class's own javadoc); {@link QuasarRenderer} is the thin CE-faithful purple-tinted
     * subclass for {@link com.hbm.entity.effect.EntityQuasar} only.
     */
    private static void registerGravityWells() {
        EntityRenderers.register(GravityWellEntityTypes.BLACK_HOLE.get(), BlackHoleRenderer::new);
        EntityRenderers.register(GravityWellEntityTypes.VORTEX.get(), BlackHoleRenderer::new);
        EntityRenderers.register(GravityWellEntityTypes.RAGING_VORTEX.get(), BlackHoleRenderer::new);
        EntityRenderers.register(GravityWellEntityTypes.QUASAR.get(), QuasarRenderer::new);
    }

    /**
     * {@code com.hbm.entity.effect.EffectEntityTypes} - blast/cloud FX marker entities.
     *
     * <p><b>Review fix (r4-rbmk-explosion-vfx-review):</b> {@code TOREX}/{@code CLOUD_FLEIJA}/
     * {@code CLOUD_SOLINIUM}/{@code EMP_BLAST} originally all registered to
     * {@link EmptyEntityRenderer} here - a leftover from this file's foundation-wave placeholder
     * pass that the Content-wave renderer classes ({@link TorexRenderer}, {@link
     * CloudFleijaRenderer}, {@link CloudSoliniumRenderer}, {@link EmpBlastRenderer}, all real and
     * committed under {@code com.hbm.client.render.entity.effect}) never got swapped into. Since
     * {@link net.minecraft.client.renderer.entity.EntityRenderers#register} has exactly one
     * registration per {@code EntityType} and this file is the only call site for these five types
     * (grep-confirmed), the leftover {@code EmptyEntityRenderer} silently discarded all four of
     * these renderer classes with no compile error and no runtime symptom short of "the cloud never
     * draws" - exactly the class of bug {@code docs/phase5/
     * reactor_and_explosion_visual_effects.md}'s Headline finding 4 warns about for a different
     * field-read mistake, found here instead at the registration layer. {@code FIRE_LINGERING}/
     * {@code MIST} stay on {@link EmptyEntityRenderer}: neither has a Content-wave renderer in this
     * task's scope.
     */
    private static void registerEffects() {
        EntityRenderers.register(EffectEntityTypes.TOREX.get(), TorexRenderer::new);
        EntityRenderers.register(EffectEntityTypes.CLOUD_FLEIJA.get(), CloudFleijaRenderer::new);
        EntityRenderers.register(EffectEntityTypes.CLOUD_SOLINIUM.get(), CloudSoliniumRenderer::new);
        EntityRenderers.register(EffectEntityTypes.EMP_BLAST.get(), EmpBlastRenderer::new);
        EntityRenderers.register(EffectEntityTypes.FIRE_LINGERING.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(EffectEntityTypes.MIST.get(), EmptyEntityRenderer::new);
    }

    /**
     * {@code com.hbm.entity.logic.NukeEntityTypes} - large-scale nuclear explosion FX entities,
     * invisible (see {@code docs/phase5/reactor_and_explosion_visual_effects.md} for the Content wave's
     * real mushroom-cloud/fireball VFX work these lines will eventually be replaced by).
     */
    private static void registerNukes() {
        EntityRenderers.register(NukeEntityTypes.NUKE_MK5.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(NukeEntityTypes.NUKE_MK3.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(NukeEntityTypes.BALEFIRE.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(NukeEntityTypes.FALLOUT_RAIN.get(), EmptyEntityRenderer::new);
    }

    /**
     * {@code com.hbm.entity.logic.SatellitePayloadEntityTypes} - orbital weapon payloads/blasts,
     * invisible. {@code TOM} itself is a ballistic dropped payload (same bucket as
     * {@link FallingNukeEntityTypes#FALLING_NUKE}), not a mob/vehicle, so it stays with its own
     * blast/laser/cloud siblings here rather than in the visible-fallback bucket.
     */
    private static void registerSatellitePayloads() {
        // Coordinator fix: DeathBlastRenderer/OrbitalLaserRenderer (com.hbm.client.render.entity.logic)
        // already exist, already implement the IConstantRenderer guard, and were already reported as a
        // wiring snippet by c5-boss-vehicle-renderers-batch2 - wired in here.
        EntityRenderers.register(SatellitePayloadEntityTypes.DEATH_BLAST.get(), DeathBlastRenderer::new);
        EntityRenderers.register(SatellitePayloadEntityTypes.ORBITAL_LASER.get(), OrbitalLaserRenderer::new);
        EntityRenderers.register(SatellitePayloadEntityTypes.TOM.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(SatellitePayloadEntityTypes.TOM_BLAST.get(), EmptyEntityRenderer::new);
        // CLOUD_TOM: review fix (r4-rbmk-explosion-vfx-review) - was EmptyEntityRenderer, silently
        // discarding the real, already-committed CloudTomRenderer (IConstantRenderer-driven, same
        // bug pattern as registerEffects() above - see that method's javadoc).
        EntityRenderers.register(SatellitePayloadEntityTypes.CLOUD_TOM.get(), CloudTomRenderer::new);
    }

    /**
     * {@code com.hbm.entity.logic.PlaneEntityTypes} - visible scripted aircraft.
     *
     * <p><b>Review fix (r5-boss-vehicle-renderer-review):</b> {@code BOMBER} had a real, already-
     * committed, already-CE-cross-checked {@link BomberRenderer} (full port of CE's {@code
     * RenderBomber}, dual Dornier/B-29 airframes gated on the synced {@code STYLE} byte, {@code
     * IConstantRenderer}-gated per {@code docs/phase5/boss_and_vehicle_entity_renderers.md} section J)
     * sitting unused under {@code com.hbm.client.render.entity.logic} - same unwired-bespoke-renderer
     * bug as this file's other methods. {@code C130} has no bespoke renderer anywhere in this port yet
     * (confirmed by this review's own search) - genuinely still OBJ/asset-blocked per that same
     * section, correctly left on {@link FallbackEntityRenderer}.
     */
    private static void registerPlanes() {
        EntityRenderers.register(PlaneEntityTypes.C130.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(PlaneEntityTypes.BOMBER.get(), BomberRenderer::new);
    }

    /** Phase 9 leftover CE {@code @AutoRegister} projectiles / soyuz / waypoint. Fallback OK. */
    private static void registerPhase9Tails() {
        EntityRenderers.register(Phase9TailEntityTypes.ACID_BOMB.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.CHEMICAL.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.SHRAPNEL.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.SOYUZ.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.SOYUZ_CAPSULE.get(), FallbackEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.WAYPOINT.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.SAWBLADE.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.RAINBOW.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.MINI_NUKE.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.PLASMA_BEAM.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.LASER_BEAM.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.LASER.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.ZIRNOX_DEBRIS.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.FIRE.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.ROCKET.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.RBMK_DEBRIS.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.MINI_MIRV.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.SCHRAB.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.TORPEDO.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.MINER_BEAM.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.SIEGE_LASER.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.LN2.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.SPARK_BEAM.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.MOD_BEAM.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.RAILGUN_PELLET.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.BULLET.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.DUCHESSGAMBIT.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.BUILDING.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.EXPLOSIVE_BEAM.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.AA_SHELL.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.ZETA.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.ARTILLERY_ROCKET.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.BULLET_MK2.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.BOXCAR.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.BURNING_FOEQ.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.ARTILLERY_SHELL.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.COMBINE_BALL.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.COG.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.VORTEX_BEAM.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.DISCHARGE.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.BULLET_MK3.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.BOBMAZON.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.SELENA.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.MINER_ROCKET.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.C_PACKAGE.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.ITEM_WASTE.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.FIREWORK_BALL.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.ITEM_BUOYANT.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.MINECART_TEST.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.WASTE_PEARL.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.SPEAR.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.MOD_FX_SHADOW.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.FLEIJA_RAINBOW.get(), EmptyEntityRenderer::new);
        EntityRenderers.register(Phase9TailEntityTypes.EMP.get(), EmptyEntityRenderer::new);
    }
}
