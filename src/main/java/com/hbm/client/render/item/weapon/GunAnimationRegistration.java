package com.hbm.client.render.item.weapon;

import com.hbm.client.render.item.HbmItemRendererRegistry;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.content.GunEnergyItems;
import com.hbm.items.weapon.sedna.content.GunHeavyItems;
import com.hbm.items.weapon.sedna.content.GunLauncherItems;
import com.hbm.items.weapon.sedna.content.GunPistolItems;
import com.hbm.items.weapon.sedna.content.GunRifleItems;
import com.hbm.items.weapon.sedna.content.GunShotgunItems;
import com.hbm.main.MainRegistry;
import com.hbm.render.anim.sedna.BusAnimationKeyframeSedna.IType;
import com.hbm.render.anim.sedna.BusAnimationSedna;
import com.hbm.render.anim.sedna.BusAnimationSequenceSedna;
import com.hbm.weapon.anim.GunAnimationType;
import com.hbm.weapon.anim.HbmAnimationType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.function.BiFunction;

/**
 * Client-only registration entry point for this task's ({@code c6-weapon-gun-rendering}) 3 fully-
 * wired guns - registers each gun's {@link ItemRenderGunBase} instance (via
 * {@link HbmItemRendererRegistry#register}, the confirmed pattern from f9) and its
 * {@link GunConfig#anim(BiFunction)} animation lookup, mirroring Neo Edition's own confirmed real
 * {@code GunFactoryClient.init(RegisterClientExtensionsEvent)} entry point (read in full for API
 * shape) - one call per gun, all from client bootstrap code, never from a common
 * {@code XFactory*.java} construction site (see {@link GunConfig}'s own updated class javadoc for
 * why that split matters).
 *
 * <p>{@code bus = EventBusSubscriber.Bus.MOD} - {@link RegisterClientExtensionsEvent} implements
 * {@code IModBusEvent} and only fires on the mod bus (this port's own ground rules, worked example:
 * {@code com.hbm.main.ClientModRegistry}; also independently confirmed by
 * {@link HbmItemRendererRegistry}'s own javadoc discussion of the same event).
 *
 * <h2>Animation lambdas - ported verbatim from CE, not invented</h2>
 * {@link #SPAS12_ANIM}/{@link #UZI_ANIM}/{@link #AM180_ANIM} below are direct, field-for-field ports
 * of CE's {@code XFactory12ga.LAMBDA_SPAS_ANIMS}/{@code XFactory9mm.LAMBDA_UZI_ANIMS}/
 * {@code XFactory22lr.LAMBDA_AM180_ANIMS} (all 3 read in full) - every numeric keyframe value,
 * duration, and interpolation type is copied as-is; only the animation-map lookup source changed
 * ({@code ResourceManager.spas_12_anim}/{@code am180_anim} -&gt;
 * {@link GunModels#spas12Anim()}/{@link GunModels#am180Anim()}) and the switch target changed from
 * CE's concrete {@code AnimationEnums.GunAnimation} enum to this port's own
 * {@link GunAnimationType} (the same value set, confirmed 1:1 - see that enum's own javadoc).
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class GunAnimationRegistration {

    public static final Map<Item, ItemRenderGunBase> RENDERERS = new IdentityHashMap<>();

    private GunAnimationRegistration() {
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        bind(event, new ItemRenderSpas12(), GunShotgunItems.GUN_SPAS12.get(), SPAS12_ANIM);
        bind(event, new ItemRenderUzi(), GunPistolItems.GUN_UZI.get(), UZI_ANIM);
        bind(event, new ItemRenderUziAkimbo(), GunPistolItems.GUN_UZI_AKIMBO.get(), UZI_ANIM);
        bind(event, new ItemRenderAm180(), GunPistolItems.GUN_AM180.get(), AM180_ANIM);

        bindTex(event, new ItemRenderPepperbox(), "pepperbox_tex", GunPistolItems.GUN_PEPPERBOX.get(), GunAnims.wrap(GunAnims.LAMBDA_PEPPERBOX_ANIMS));
        bind(event, new ItemRenderAtlas(GunModels.tex("bio_revolver_tex")), GunPistolItems.GUN_LIGHT_REVOLVER.get(), GunAnims.wrap(GunAnims.LAMBDA_ATLAS_ANIMS));
        bind(event, new ItemRenderAtlas(GunModels.tex("bio_revolver_atlas_tex")), GunPistolItems.GUN_LIGHT_REVOLVER_ATLAS.get(), GunAnims.wrap(GunAnims.LAMBDA_ATLAS_ANIMS));
        bindTex(event, new ItemRenderDANI(), "dani_celestial_tex", GunPistolItems.GUN_LIGHT_REVOLVER_DANI.get(), GunAnims.wrap(GunAnims.LAMBDA_DANI_ANIMS));
        bind(event, new ItemRenderHenry(GunModels.tex("henry_tex")), GunPistolItems.GUN_HENRY.get(), GunAnims.wrap(GunAnims.LAMBDA_HENRY_ANIMS));
        bind(event, new ItemRenderHenry(GunModels.tex("henry_lincoln_tex")), GunPistolItems.GUN_HENRY_LINCOLN.get(), GunAnims.wrap(GunAnims.LAMBDA_HENRY_ANIMS));
        bind(event, new ItemRenderHeavyRevolver(GunModels.tex("heavy_revolver_tex")), GunPistolItems.GUN_HEAVY_REVOLVER.get(), GunAnims.wrap(GunAnims.LAMBDA_NOPIP_ANIMS));
        bind(event, new ItemRenderHeavyRevolver(GunModels.tex("lilmac_tex")), GunPistolItems.GUN_HEAVY_REVOLVER_LILMAC.get(), GunAnims.wrap(GunAnims.LAMBDA_LILMAC_ANIMS));
        bind(event, new ItemRenderHeavyRevolver(GunModels.tex("heavy_revolver_protege_tex")), GunPistolItems.GUN_HEAVY_REVOLVER_PROTEGE.get(), GunAnims.wrap(GunAnims.LAMBDA_LILMAC_ANIMS));
        bindTex(event, new ItemRenderHangman(), "hangman_tex", GunPistolItems.GUN_HANGMAN.get(), GunAnims.wrap(GunAnims.LAMBDA_HANGMAN_ANIMS));
        bindTex(event, new ItemRenderGreasegun(), "greasegun_tex", GunPistolItems.GUN_GREASEGUN.get(), GunAnims.wrap(GunAnims.LAMBDA_GREASEGUN_ANIMS));
        bindTex(event, new ItemRenderLAG(), "mike_hawk_tex", GunPistolItems.GUN_LAG.get(), GunAnims.wrap(GunAnims.LAMBDA_LAG_ANIMS));
        bindTex(event, new ItemRenderStarF(), "star_f_tex", GunPistolItems.GUN_STAR_F.get(), GunAnims.wrap(GunAnims.LAMBDA_STAR_F_ANIMS));
        bindTex(event, new ItemRenderStarFAkimbo(), "star_f_tex", GunPistolItems.GUN_STAR_F_AKIMBO.get(), GunAnims.wrap(GunAnims.LAMBDA_STAR_F_ANIMS));

        bind(event, new ItemRenderMaresleg(GunModels.tex("maresleg_tex")), GunShotgunItems.GUN_MARESLEG.get(), GunAnims.wrap(GunAnims.LAMBDA_MARESLEG_ANIMS));
        bindTex(event, new ItemRenderMareslegAkimbo(), "maresleg_tex", GunShotgunItems.GUN_MARESLEG_AKIMBO.get(), GunAnims.wrap(GunAnims.LAMBDA_MARESLEG_SHORT_ANIMS));
        bind(event, new ItemRenderMaresleg(GunModels.tex("maresleg_broken_tex")), GunShotgunItems.GUN_MARESLEG_BROKEN.get(), GunAnims.wrap(GunAnims.LAMBDA_MARESLEG_SHORT_ANIMS));
        bindTex(event, new ItemRenderLiberator(), "liberator_tex", GunShotgunItems.GUN_LIBERATOR.get(), GunAnims.wrap(GunAnims.LAMBDA_LIBERATOR_ANIMS));
        bind(event, new ItemRenderShredder(GunModels.tex("shredder_tex")), GunShotgunItems.GUN_AUTOSHOTGUN.get(), GunAnims.wrap(GunAnims.LAMBDA_SHREDDER_ANIMS));
        bind(event, new ItemRenderShredder(GunModels.tex("shredder_orig_tex")), GunShotgunItems.GUN_AUTOSHOTGUN_SHREDDER.get(), GunAnims.wrap(GunAnims.LAMBDA_SHREDDER_ANIMS));
        bind(event, new ItemRenderSexy(GunModels.tex("sexy_tex")), GunShotgunItems.GUN_AUTOSHOTGUN_SEXY.get(), GunAnims.wrap(GunAnims.LAMBDA_SEXY_ANIMS));
        bind(event, new ItemRenderDoubleBarrel(GunModels.tex("double_barrel_tex")), GunShotgunItems.GUN_DOUBLE_BARREL.get(), GunAnims.wrap(GunAnims.LAMBDA_DOUBLE_BARREL_ANIMS));
        bind(event, new ItemRenderDoubleBarrel(GunModels.tex("double_barrel_sacred_dragon_tex")), GunShotgunItems.GUN_DOUBLE_BARREL_SACRED_DRAGON.get(), GunAnims.wrap(GunAnims.LAMBDA_DOUBLE_BARREL_ANIMS));
        bind(event, new ItemRenderSexy(GunModels.tex("heretic_tex")), GunShotgunItems.GUN_AUTOSHOTGUN_HERETIC.get(), GunAnims.wrap(GunAnims.LAMBDA_SEXY_ANIMS));

        bind(event, new ItemRenderG3(GunModels.tex("g3_tex")), GunRifleItems.GUN_G3.get(), GunAnims.wrap(GunAnims.LAMBDA_G3_ANIMS));
        bind(event, new ItemRenderG3(GunModels.tex("g3_zebra_tex")), GunRifleItems.GUN_G3_ZEBRA.get(), GunAnims.wrap(GunAnims.LAMBDA_G3_ANIMS));
        bindTex(event, new ItemRenderSTG77(), "stg77_tex", GunRifleItems.GUN_STG77.get(), GunAnims.wrap(GunAnims.LAMBDA_STG77_ANIMS));
        bindTex(event, new ItemRenderCarbine(), "carbine_tex", GunRifleItems.GUN_CARBINE.get(), GunAnims.wrap(GunAnims.LAMBDA_CARBINE_ANIMS));
        bind(event, new ItemRenderMinigun(GunModels.tex("minigun_tex")), GunRifleItems.GUN_MINIGUN.get(), GunAnims.wrap(GunAnims.LAMBDA_MINIGUN_ANIMS));
        bind(event, new ItemRenderMinigun(GunModels.tex("minigun_lacunae_tex")), GunRifleItems.GUN_MINIGUN_LACUNAE.get(), GunAnims.wrap(GunAnims.LAMBDA_MINIGUN_ANIMS));
        bindTex(event, new ItemRenderMinigunDual(), "minigun_dual_tex", GunRifleItems.GUN_MINIGUN_DUAL.get(), GunAnims.wrap(GunAnims.LAMBDA_MINIGUN_ANIMS));
        bindTex(event, new ItemRenderMAS36(), "mas36_tex", GunRifleItems.GUN_MAS36.get(), GunAnims.wrap(GunAnims.LAMBDA_MAS36_ANIMS));
        bind(event, new ItemRenderAmat(GunModels.tex("amat_tex")), GunRifleItems.GUN_AMAT.get(), GunAnims.wrap(GunAnims.LAMBDA_AMAT_ANIMS));
        bind(event, new ItemRenderAmat(GunModels.tex("amat_subtlety_tex")), GunRifleItems.GUN_AMAT_SUBTLETY.get(), GunAnims.wrap(GunAnims.LAMBDA_AMAT_ANIMS));
        bind(event, new ItemRenderAmat(GunModels.tex("amat_penance_tex")), GunRifleItems.GUN_AMAT_PENANCE.get(), GunAnims.wrap(GunAnims.LAMBDA_AMAT_ANIMS));
        bindTex(event, new ItemRenderM2(), "m2_tex", GunRifleItems.GUN_M2.get(), GunAnims.wrap(GunAnims.LAMBDA_M2_ANIMS));

        bindTex(event, new ItemRenderFlaregun(), "flaregun_tex", GunLauncherItems.GUN_FLAREGUN.get(), GunAnims.wrap(GunAnims.LAMBDA_FLAREGUN_ANIMS));
        bindTex(event, new ItemRenderCongoLake(), "congolake_tex", GunLauncherItems.GUN_CONGOLAKE.get(), GunAnims.wrap(GunAnims.LAMBDA_CONGOLAKE_ANIMS));
        bindTex(event, new ItemRenderMK108(), "mk108_tex", GunLauncherItems.GUN_MK108.get(), GunAnims.wrap(GunAnims.LAMBDA_MK108_ANIMS));
        bindTex(event, new ItemRenderBolter(), "bolter_tex", GunLauncherItems.GUN_BOLTER.get(), GunAnims.wrap(GunAnims.LAMBDA_BOLTER_ANIMS));

        bindTex(event, new ItemRenderPanzerschreck(), "panzerschreck_tex", GunHeavyItems.GUN_PANZERSCHRECK.get(), GunAnims.wrap(GunAnims.LAMBDA_PANZERSCHRECK_ANIMS));
        bindTex(event, new ItemRenderStinger(), "stinger_tex", GunHeavyItems.GUN_STINGER.get(), GunAnims.wrap(GunAnims.LAMBDA_PANZERSCHRECK_ANIMS));
        bindTex(event, new ItemRenderQuadro(), "quadro_tex", GunHeavyItems.GUN_QUADRO.get(), GunAnims.wrap(GunAnims.LAMBDA_QUADRO_ANIMS));
        bindTex(event, new ItemRenderMissileLauncher(), "missile_launcher_tex", GunHeavyItems.GUN_MISSILE_LAUNCHER.get(), GunAnims.wrap(GunAnims.LAMBDA_MISSILE_LAUNCHER_ANIMS));
        bindTex(event, new ItemRenderChargeThrower(), "charge_thrower_tex", GunHeavyItems.GUN_CHARGE_THROWER.get(), GunAnims.wrap(GunAnims.LAMBDA_CT_ANIMS));
        bind(event, new ItemRenderFlamer(GunModels.tex("flamethrower_tex")), GunHeavyItems.GUN_FLAMER.get(), GunAnims.wrap(GunAnims.LAMBDA_FLAMER_ANIMS));
        bind(event, new ItemRenderFlamer(GunModels.tex("flamethrower_topaz_tex")), GunHeavyItems.GUN_FLAMER_TOPAZ.get(), GunAnims.wrap(GunAnims.LAMBDA_FLAMER_ANIMS));
        bind(event, new ItemRenderFlamer(GunModels.tex("flamethrower_daybreaker_tex")), GunHeavyItems.GUN_FLAMER_DAYBREAKER.get(), GunAnims.wrap(GunAnims.LAMBDA_FLAMER_ANIMS));
        bindTex(event, new ItemRenderChemthrower(), "chemthrower_tex", GunHeavyItems.GUN_CHEMTHROWER.get(), GunAnims.wrap(GunAnims.LAMBDA_CHEMTHROWER_ANIMS));
        bindTex(event, new ItemRenderDrill(), "drill_tex", GunHeavyItems.GUN_DRILL.get(), GunAnims.wrap(GunAnims.LAMBDA_DRILL_ANIMS));
        bindTex(event, new ItemRenderPAMelee(), "lance_tex", GunHeavyItems.GUN_PA_MELEE.get(), GunAnims.wrap(GunAnims.LAMBDA_MELEE_ANIMS));
        bindTex(event, new ItemRenderDebug(), "debug_gun_tex", GunHeavyItems.GUN_DEBUG.get(), GunAnims.wrap(GunAnims.LAMBDA_DEBUG_ANIMS));

        bindTex(event, new ItemRenderTau(), "tau_tex", GunEnergyItems.GUN_TAU.get(), GunAnims.wrap(GunAnims.LAMBDA_TAU_ANIMS));
        bindTex(event, new ItemRenderCoilgun(), "coilgun_tex", GunEnergyItems.GUN_COILGUN.get(), GunAnims.wrap(GunAnims.LAMBDA_COILGUN_ANIMS));
        bindTex(event, new ItemRenderNI4NI(), "n_i_4_n_i_tex", GunEnergyItems.GUN_N_I_4_N_I.get(), GunAnims.wrap(GunAnims.LAMBDA_NI4NI_ANIMS));
        bindTex(event, new ItemRenderTeslaCannon(), "tesla_cannon_tex", GunEnergyItems.GUN_TESLA_CANNON.get(), GunAnims.wrap(GunAnims.LAMBDA_TESLA_ANIMS));
        bind(event, new ItemRenderLaserPistol(GunModels.tex("laser_pistol_tex")), GunEnergyItems.GUN_LASER_PISTOL.get(), GunAnims.wrap(GunAnims.LAMBDA_LASER_PISTOL));
        bind(event, new ItemRenderLaserPistol(GunModels.tex("laser_pistol_pew_pew_tex")), GunEnergyItems.GUN_LASER_PISTOL_PEW_PEW.get(), GunAnims.wrap(GunAnims.LAMBDA_LASER_PISTOL));
        bind(event, new ItemRenderLaserPistol(GunModels.tex("laser_pistol_morning_glory_tex")), GunEnergyItems.GUN_LASER_PISTOL_MORNING_GLORY.get(), GunAnims.wrap(GunAnims.LAMBDA_LASER_PISTOL));
        bindTex(event, new ItemRenderLasrifle(), "lasrifle_tex", GunEnergyItems.GUN_LASRIFLE.get(), GunAnims.wrap(GunAnims.LAMBDA_LASRIFLE));
        bindTex(event, new ItemRenderFatMan(), "fatman_tex", GunEnergyItems.GUN_FATMAN.get(), GunAnims.wrap(GunAnims.LAMBDA_FATMAN_ANIMS));
        bindTex(event, new ItemRenderFolly(), "folly_tex", GunEnergyItems.GUN_FOLLY.get(), GunAnims.wrap(GunAnims.LAMBDA_FOLLY_ANIMS));
        bindTex(event, new ItemRenderAberrator(), "aberrator_tex", GunEnergyItems.GUN_ABERRATOR.get(), GunAnims.wrap(GunAnims.LAMBDA_ABERRATOR));
        bindTex(event, new ItemRenderEOTT(), "eott_tex", GunEnergyItems.GUN_ABERRATOR_EOTT.get(), GunAnims.wrap(GunAnims.LAMBDA_ABERRATOR));
    }

    private static void bind(RegisterClientExtensionsEvent event, ItemRenderGunBase renderer, Item item,
                             java.util.function.BiFunction<ItemStack, HbmAnimationType, com.hbm.render.anim.sedna.BusAnimationSedna> anim) {
        HbmItemRendererRegistry.register(event, renderer, item);
        RENDERERS.put(item, renderer);
        ItemGunBaseNT gun = (ItemGunBaseNT) item;
        for (int i = 0; i < gun.getConfigCount(); i++) {
            gun.getConfig(null, i).anim(anim);
        }
    }

    private static void bindTex(RegisterClientExtensionsEvent event, ItemRenderGunBase renderer, String texField, Item item,
                                java.util.function.BiFunction<ItemStack, HbmAnimationType, com.hbm.render.anim.sedna.BusAnimationSedna> anim) {
        try {
            renderer.getClass().getField("texture").set(renderer, GunModels.tex(texField));
        } catch (ReflectiveOperationException ignored) {
            // constructor-injected texture already set
        }
        bind(event, renderer, item, anim);
    }

    // ------------------------------------------------------------------------------------
    // SPAS-12 - CE: XFactory12ga.LAMBDA_SPAS_ANIMS
    // ------------------------------------------------------------------------------------

    public static final BiFunction<ItemStack, HbmAnimationType, BusAnimationSedna> SPAS12_ANIM = (stack, rawType) -> {
        if (!(rawType instanceof GunAnimationType type)) return null;
        return switch (type) {
            case EQUIP -> new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(-60, 0, 0, 0).addPos(0, 0, -3, 500, IType.SIN_DOWN));
            case CYCLE -> GunModels.spas12Anim().get("Fire");
            case CYCLE_DRY -> GunModels.spas12Anim().get("FireDry");
            case ALT_CYCLE -> GunModels.spas12Anim().get("FireAlt");
            case RELOAD -> {
                boolean empty = magazineEmpty((ItemGunBaseNT) stack.getItem(), stack, 0);
                yield GunModels.spas12Anim().get(empty ? "ReloadEmptyStart" : "ReloadStart");
            }
            case RELOAD_CYCLE -> GunModels.spas12Anim().get("Reload");
            case RELOAD_END -> GunModels.spas12Anim().get("ReloadEnd");
            case JAMMED -> GunModels.spas12Anim().get("Jammed");
            case INSPECT -> GunModels.spas12Anim().get("Inspect");
            default -> null;
        };
    };

    // ------------------------------------------------------------------------------------
    // Uzi - CE: XFactory9mm.LAMBDA_UZI_ANIMS - fully programmatic, no dedicated animation JSON.
    // ------------------------------------------------------------------------------------

    public static final BiFunction<ItemStack, HbmAnimationType, BusAnimationSedna> UZI_ANIM = (stack, rawType) -> {
        if (!(rawType instanceof GunAnimationType type)) return null;
        return switch (type) {
            case EQUIP -> new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(80, 0, 0, 0).addPos(80, 0, 0, 500).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("STOCKBACK", new BusAnimationSequenceSedna().addPos(-200, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("STOCKFRONT", new BusAnimationSequenceSedna().addPos(180, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
            case CYCLE -> new BusAnimationSedna()
                    .addBus("RECOIL", new BusAnimationSequenceSedna().addPos(0, 0, ItemGunBaseNT.getIsAiming(stack) ? -0.5 : -0.75, 25, IType.SIN_DOWN).addPos(0, 0, 0, 75, IType.SIN_FULL));
            case CYCLE_DRY -> new BusAnimationSedna()
                    .addBus("LIFT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 500).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(0, 0, -2, 150, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
            case RELOAD -> {
                boolean empty = magazineEmpty((ItemGunBaseNT) stack.getItem(), stack, 0);
                yield new BusAnimationSedna()
                        .addBus("MAG", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(0, -10, 0, 250, IType.SIN_UP).addPos(0, -10, 0, 750).addPos(0, 0, 0, 500, IType.SIN_DOWN))
                        .addBus("LIFT", new BusAnimationSequenceSedna().addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 2000).addPos(0, 0, 0, 500, IType.SIN_FULL))
                        .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 2000).addPos(0, 0, -2, 150, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP))
                        .addBus("BULLET", new BusAnimationSequenceSedna().addPos(empty ? 0 : 1, 0, 0, 0).addPos(empty ? 0 : 1, 0, 0, 500).addPos(1, 0, 0, 0));
            }
            case JAMMED -> new BusAnimationSedna()
                    .addBus("LIFT", new BusAnimationSequenceSedna().addPos(0, 0, 0, 500).addPos(-25, 0, 0, 250, IType.SIN_FULL).addPos(-25, 0, 0, 1250).addPos(0, 0, 0, 500, IType.SIN_FULL))
                    .addBus("SLIDE", new BusAnimationSequenceSedna().addPos(0, 0, 0, 1000).addPos(0, 0, -2, 150, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 500).addPos(0, 0, -2, 150, IType.SIN_FULL).addPos(0, 0, 0, 50, IType.SIN_UP));
            case INSPECT -> new BusAnimationSedna()
                    .addBus("YEET", new BusAnimationSequenceSedna().addPos(0, -1, 0, 100).addPos(0, 0, 0, 100, IType.SIN_UP).addPos(0, 12, 0, 350, IType.SIN_DOWN).addPos(0, 0, 0, 350, IType.SIN_UP).addPos(0, -1, 0, 50, IType.SIN_DOWN).addPos(0, 0, 0, 100, IType.SIN_FULL))
                    .addBus("SPEEN", new BusAnimationSequenceSedna().addPos(0, 0, 0, 250).addPos(-360, 0, 0, 600));
            default -> null;
        };
    };

    // ------------------------------------------------------------------------------------
    // AM-180 - CE: XFactory22lr.LAMBDA_AM180_ANIMS, non-legacy (GUN_ANIMS_LEGACY == false) branch only.
    // ------------------------------------------------------------------------------------

    public static final BiFunction<ItemStack, HbmAnimationType, BusAnimationSedna> AM180_ANIM = (stack, rawType) -> {
        if (!(rawType instanceof GunAnimationType type)) return null;
        return switch (type) {
            case EQUIP -> new BusAnimationSedna()
                    .addBus("EQUIP", new BusAnimationSequenceSedna().addPos(45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_FULL));
            case CYCLE -> GunModels.am180Anim().get("Fire");
            case CYCLE_DRY -> GunModels.am180Anim().get("FireDry");
            case RELOAD -> GunModels.am180Anim().get("Reload");
            case JAMMED -> GunModels.am180Anim().get("Jammed");
            case INSPECT -> GunModels.am180Anim().get("Inspect");
            default -> null;
        };
    };

    /** CE: {@code gun.getConfig(stack,0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, MainRegistry.proxy.me().inventory) <= 0} - ported using this port's own client-safe local-player-inventory read. */
    private static boolean magazineEmpty(ItemGunBaseNT gun, ItemStack stack, int cfgIndex) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        GunConfig config = gun.getConfig(stack, cfgIndex);
        return config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, player.getInventory()) <= 0;
    }
}
