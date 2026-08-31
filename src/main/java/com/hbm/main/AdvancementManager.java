package com.hbm.main;

import com.hbm.config.GeneralConfig;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import javax.annotation.Nullable;

/**
 * Ported from CE's {@code com.hbm.main.AdvancementManager} (a flat list of ~65
 * {@code net.minecraft.advancements.Advancement} fields, resolved once per server start and granted
 * to players by name elsewhere in CE). This is the shared, standalone forward-reference target named
 * by {@code docs/phase4/satellites_followup_and_loot_pools.md} (its {@code AdvancementManager}
 * section) and {@code docs/phase4/entities_bosses.md} (Headline finding #4 and its Deferred-scope
 * entry) — until this class existed, both reports' own call sites (satellite orbit hooks, boss death
 * handlers) were left as documented {@code TODO(advancements)}/{@code TODO(AdvancementManager)}
 * forward references. This class does not itself wire any of those call sites back in — it is pure
 * infrastructure other Phase 4 packages call once it lands, per this package's own task brief.
 *
 * <p><b>1.21.1 API shape — not independently verified against a real compiled jar in this sandbox</b>
 * (no NeoForge/Minecraft jar reachable; Neo Edition carries zero Java code touching advancements, only
 * datapack JSON). The shape below is well-established Mojang-mapping knowledge for 1.20.2+, per both
 * research reports' own "Key design/API decisions" sections:
 * <ul>
 *     <li>CE's raw {@code Advancement} fields become {@link AdvancementHolder} (a
 *     {@code record(ResourceLocation id, Advancement value)}).</li>
 *     <li>CE's {@code serv.getAdvancementManager().getAdvancement(id)} becomes
 *     {@code server.getAdvancements().get(ResourceLocation)} (nullable, off
 *     {@link MinecraftServer#getAdvancements()} → {@link ServerAdvancementManager}).</li>
 *     <li>CE's {@code player.getAdvancements().getProgress(a)} becomes
 *     {@code player.getAdvancements().getOrStartProgress(AdvancementHolder)}.</li>
 *     <li>CE's own typo'd {@code getRemaningCriteria()} becomes the corrected
 *     {@code getRemainingCriteria()}.</li>
 *     <li>CE's {@code grantCriterion(a, s)} becomes
 *     {@code player.getAdvancements().award(AdvancementHolder, String)}.</li>
 * </ul>
 *
 * <p>CE's hook point ({@code AdvancementManager.init(MinecraftServer)}, called from
 * {@code MainRegistry}'s {@code FMLServerStartingEvent} handler) maps onto NeoForge's
 * {@link ServerStartingEvent} — confirmed by this port's own ground rules to be a GAME-bus event
 * (it does <b>not</b> implement {@code net.neoforged.fml.event.IModBusEvent}, unlike
 * {@code FMLServerStartingEvent} in old Forge, which was mod-bus), so this class uses the default
 * {@code bus = EventBusSubscriber.Bus.MOD}-free (i.e. {@code Bus.GAME}) subscriber form, matching
 * e.g. {@code handler.pollution.PollutionHandler}/{@code handler.neutron.NeutronHandler}. This port
 * has no other {@code ServerStartingEvent} listener anywhere — this is the first.
 *
 * <p>Every gate check below calls {@link GeneralConfig#ENABLE_ADVANCEMENTS}{@code .get()} live
 * rather than caching a copy, matching this port's established {@code ModConfigSpec.BooleanValue}
 * convention (CE's own {@code enableAdvancements} field is itself a load-time cache of a Forge config
 * value — the live {@code .get()} call is this port's already-chosen equivalent, not a new design
 * decision; e.g. {@code ContaminationUtil.java}, {@code LoadedBaseBlockEntity.java}).
 *
 * <p><b>Fixed crash-on-startup (review pass):</b> {@link GeneralConfig#ENABLE_ADVANCEMENTS} defaults
 * to {@code true}, so {@link #init} runs on every default-config server start - but zero
 * {@code data/hbm/advancement/*.json} datapack files exist anywhere in this port yet (confirmed by
 * repo-wide search; CE's own real advancement JSON tree under
 * {@code assets/hbm/advancements/*.json} - 1.12's still-client-resource-adjacent location for the
 * then-new JSON advancement system - has not been ported/relocated to 1.21's
 * {@code data/<namespace>/advancement/} datapack path by any wave yet). {@link #load} previously threw
 * via {@code Objects.requireNonNull} the moment {@link #init} hit the first ({@code achsacrifice})
 * lookup, which - since {@link ServerAdvancementManager#get} returns {@code null} for literally every
 * one of the ~65 ids with no backing JSON - crashed {@link #onServerStarting} (and therefore server
 * startup) unconditionally out of the box. {@link #load} now logs a warning and stores {@code null}
 * instead of throwing (fields are {@code @Nullable} accordingly), and {@link #grantAchievement}/
 * {@link #hasAdvancement} both no-op/return {@code false} on a {@code null} advancement rather than
 * their own previous {@code Objects.requireNonNull} throw - this class degrades to a safe no-op
 * everywhere until the real advancement JSON tree is ported, instead of taking the server down.
 */
@EventBusSubscriber(modid = MainRegistry.MODID)
public final class AdvancementManager {

    private AdvancementManager() {
    }

    // --- Misc / hidden / secret achievements -------------------------------------------------
    public static AdvancementHolder achSacrifice;
    public static AdvancementHolder achImpossible;
    public static AdvancementHolder achTOB;
    public static AdvancementHolder achPotato;
    public static AdvancementHolder achC20_5;
    public static AdvancementHolder achFiend;
    public static AdvancementHolder achFiend2;
    public static AdvancementHolder achRadPoison;
    public static AdvancementHolder achRadDeath;
    public static AdvancementHolder achStratum;
    public static AdvancementHolder achOmega12;
    public static AdvancementHolder achSomeWounds;
    public static AdvancementHolder achSlimeball;
    public static AdvancementHolder achSulfuric;
    public static AdvancementHolder achGoFish;
    public static AdvancementHolder achNo9;
    public static AdvancementHolder achInferno;
    public static AdvancementHolder achRedRoom;
    /** CE: the hidden achievement granted to everyone within 50 blocks when an
     *  {@code EntityCreeperTainted} is killed by {@code ModDamageSource.boxcar}
     *  ({@code ModEventHandler#onEntityDeath}, CE field name confirmed by direct read). */
    public static AdvancementHolder bobHidden;
    public static AdvancementHolder horizonsStart;
    public static AdvancementHolder horizonsEnd;
    public static AdvancementHolder horizonsBonus;
    public static AdvancementHolder bossCreeper;
    public static AdvancementHolder bossMeltdown;
    public static AdvancementHolder bossMaskman;
    public static AdvancementHolder bossWorm;
    public static AdvancementHolder bossUFO;
    public static AdvancementHolder digammaSee;
    public static AdvancementHolder digammaFeel;
    public static AdvancementHolder digammaKnow;
    public static AdvancementHolder digammaKauaiMoho;
    public static AdvancementHolder digammaUpOnTop;

    // --- Progression achievements --------------------------------------------------------------
    public static AdvancementHolder achBurnerPress;
    public static AdvancementHolder achBlastFurnace;
    public static AdvancementHolder achAssembly;
    public static AdvancementHolder achSelenium;
    public static AdvancementHolder achChemplant;
    public static AdvancementHolder achConcrete;
    public static AdvancementHolder achPolymer;
    public static AdvancementHolder achDesh;
    public static AdvancementHolder achTantalum;
    public static AdvancementHolder achRedBalloons;
    public static AdvancementHolder achManhattan;
    public static AdvancementHolder achGasCent;
    public static AdvancementHolder achCentrifuge;
    public static AdvancementHolder achFOEQ;
    public static AdvancementHolder achSoyuz;
    public static AdvancementHolder achSpace;
    public static AdvancementHolder achSchrab;
    public static AdvancementHolder achAcidizer;
    public static AdvancementHolder achRadium;
    public static AdvancementHolder achTechnetium;
    public static AdvancementHolder achZIRNOXBoom;
    public static AdvancementHolder achChicagoPile;
    public static AdvancementHolder achSILEX;
    public static AdvancementHolder achWatz;
    public static AdvancementHolder achWatzBoom;
    public static AdvancementHolder achRBMK;
    public static AdvancementHolder achRBMKBoom;
    public static AdvancementHolder achBismuth;
    public static AdvancementHolder achBreeding;
    public static AdvancementHolder achFusion;
    public static AdvancementHolder achMeltdown;

    public static AdvancementHolder progress_dfc;
    public static AdvancementHolder root;

    /**
     * Resolves an already-loaded advancement by its {@code hbm:<path>} id. CE's own {@code load(adv,
     * path)} helper fails loudly ({@code Objects.requireNonNull}) on a missing datapack json, and this
     * method used to mirror that exactly - but with zero {@code data/hbm/advancement/*.json} files
     * shipped anywhere in this port yet (see this class's own javadoc), that made every single one of
     * {@link #init}'s ~65 lookups a guaranteed {@link NullPointerException} on any default-config
     * server start. Logs a warning and returns {@code null} instead now - every field below is
     * therefore {@code @Nullable} in practice until the real advancement JSON tree is ported, and
     * {@link #grantAchievement}/{@link #hasAdvancement} both tolerate that.
     */
    @Nullable
    private static AdvancementHolder load(ServerAdvancementManager advancements, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path);
        AdvancementHolder advancement = advancements.get(id);
        if (advancement == null) {
            MainRegistry.logger.warn("Missing advancement (not yet ported to a data/hbm/advancement json): {}", id);
        }
        return advancement;
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        init(event.getServer());
    }

    public static void init(MinecraftServer server) {
        if (!GeneralConfig.ENABLE_ADVANCEMENTS.get()) return;
        ServerAdvancementManager advancements = server.getAdvancements();

        achSacrifice  = load(advancements, "achsacrifice");
        achImpossible = load(advancements, "achimpossible");
        achTOB        = load(advancements, "achtob");
        achGoFish     = load(advancements, "achgofish");
        achPotato     = load(advancements, "achpotato");
        achC20_5      = load(advancements, "achc20_5");
        achFiend      = load(advancements, "achfiend");
        achFiend2     = load(advancements, "achfiend2");
        achStratum    = load(advancements, "achstratum");
        achOmega12    = load(advancements, "achomega12");

        achNo9        = load(advancements, "achno9");
        achSlimeball  = load(advancements, "achslimeball");
        achSulfuric   = load(advancements, "achsulfuric");
        achInferno    = load(advancements, "achinferno");
        achRedRoom    = load(advancements, "achredroom");

        bobHidden     = load(advancements, "bobhidden");

        horizonsStart = load(advancements, "horizonsstart");
        horizonsEnd   = load(advancements, "horizonsend");
        horizonsBonus = load(advancements, "horizonsbonus");

        bossCreeper   = load(advancements, "bosscreeper");
        bossMeltdown  = load(advancements, "bossmeltdown");
        bossMaskman   = load(advancements, "bossmaskman");
        bossWorm      = load(advancements, "bossworm");
        bossUFO       = load(advancements, "bossufo");

        achRadPoison  = load(advancements, "achradpoison");
        achRadDeath   = load(advancements, "achraddeath");

        achSomeWounds = load(advancements, "achsomewounds");

        digammaSee       = load(advancements, "digammasee");
        digammaFeel      = load(advancements, "digammafeel");
        digammaKnow      = load(advancements, "digammaknow");
        digammaKauaiMoho = load(advancements, "digammakauaimoho");
        digammaUpOnTop   = load(advancements, "digammaupontop");

        // Progression
        achBurnerPress  = load(advancements, "achburnerpress");
        achBlastFurnace = load(advancements, "achblastfurnace");
        achAssembly     = load(advancements, "achassembly");
        achSelenium     = load(advancements, "achselenium");
        achChemplant    = load(advancements, "achchemplant");
        achConcrete     = load(advancements, "achconcrete");
        achPolymer      = load(advancements, "achpolymer");
        achDesh         = load(advancements, "achdesh");
        achTantalum     = load(advancements, "achtantalum");
        achGasCent      = load(advancements, "achgascent");
        achCentrifuge   = load(advancements, "achcentrifuge");
        achFOEQ         = load(advancements, "achfoeq");
        achSoyuz        = load(advancements, "achsoyuz");
        achSpace        = load(advancements, "achspace");
        achSchrab       = load(advancements, "achschrab");
        achAcidizer     = load(advancements, "achacidizer");
        achRadium       = load(advancements, "achradium");
        achTechnetium   = load(advancements, "achtechnetium");
        achZIRNOXBoom   = load(advancements, "achzirnoxboom");
        achChicagoPile  = load(advancements, "achchicagopile");
        achSILEX        = load(advancements, "achsilex");
        achWatz         = load(advancements, "achwatz");
        achWatzBoom     = load(advancements, "achwatzboom");
        achRBMK         = load(advancements, "achrbmk");
        achRBMKBoom     = load(advancements, "achrbmkboom");
        achBismuth      = load(advancements, "achbismuth");
        achBreeding     = load(advancements, "achbreeding");
        achFusion       = load(advancements, "achfusion");
        achMeltdown     = load(advancements, "achmeltdown");
        achRedBalloons  = load(advancements, "achredballoons");
        achManhattan    = load(advancements, "achmanhattan");

        progress_dfc = load(advancements, "progress_dfc"); // 1.12.2 exclusive, kept because why not?
        // TODO: Maybe add an achievement for SAFE
        // not really, it got removed
        root = load(advancements, "root"); // 1.12.2 Root advancement
    }

    /**
     * Grants every remaining criterion of {@code advancement} to {@code player} outright (CE's
     * "grant the whole advancement, not just start progress" idiom) — mirrors CE's
     * {@code grantAchievement(EntityPlayerMP, Advancement)} exactly, modulo the
     * {@code getRemaningCriteria()} → {@code getRemainingCriteria()} and
     * {@code grantCriterion} → {@code award} renames documented on the class javadoc.
     * <p>
     * A {@code null} {@code advancement} (see {@link #load}'s own javadoc: real today for every one
     * of this class's ~65 fields, since no backing json exists yet) is a silent no-op rather than the
     * {@code Objects.requireNonNull} throw this method used to have - every boss-death/satellite-orbit
     * call site can unconditionally call this without first null-checking the field itself.
     */
    public static void grantAchievement(ServerPlayer player, @Nullable AdvancementHolder advancement) {
        if (!GeneralConfig.ENABLE_ADVANCEMENTS.get() || advancement == null) return;
        PlayerAdvancements playerAdvancements = player.getAdvancements();
        for (String criterion : playerAdvancements.getOrStartProgress(advancement).getRemainingCriteria()) {
            playerAdvancements.award(advancement, criterion);
        }
    }

    /**
     * @apiNote Call sites shall test with {@link GeneralConfig#ENABLE_ADVANCEMENTS} first.
     * A {@code null} {@code advancement} (see {@link #load}'s own javadoc) returns {@code false}
     * rather than throwing, matching {@link #grantAchievement}'s own null-tolerance.
     */
    public static boolean hasAdvancement(ServerPlayer player, @Nullable AdvancementHolder advancement) {
        if (advancement == null) return false;
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }
}
