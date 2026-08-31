package com.hbm.client.render.item.weapon;

import com.hbm.client.render.item.HbmItemRendererRegistry;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.content.GunPistolItems;
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

    private GunAnimationRegistration() {
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        Item spas12 = GunShotgunItems.GUN_SPAS12.get();
        Item uzi = GunPistolItems.GUN_UZI.get();
        Item am180 = GunPistolItems.GUN_AM180.get();

        HbmItemRendererRegistry.register(event, new ItemRenderSpas12(), spas12);
        HbmItemRendererRegistry.register(event, new ItemRenderUzi(), uzi);
        HbmItemRendererRegistry.register(event, new ItemRenderAm180(), am180);

        ((ItemGunBaseNT) spas12).getConfig(null, 0).anim(SPAS12_ANIM);
        ((ItemGunBaseNT) uzi).getConfig(null, 0).anim(UZI_ANIM);
        ((ItemGunBaseNT) am180).getConfig(null, 0).anim(AM180_ANIM);
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
