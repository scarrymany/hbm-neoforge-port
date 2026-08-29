package com.hbm.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * Port of CE's runtime-editable {@code ClientConfig} (client-only HUD/rendering preferences,
 * originally backed by CE's {@code RunningConfig}/{@code ConfigWrapper} and editable in-game via
 * {@code /ntmclient}). Registered into {@link HbmConfig}'s CLIENT spec.
 * <p>
 * <b>Deliberate feature reduction:</b> CE let players edit these values live, in-game, via the
 * {@code /ntmclient} command, persisted to {@code hbmClient.json} independent of the main config
 * reload cycle. That command layer (CE's {@code RunningConfig}, {@code ConfigWrapper<T>}, and the
 * {@code /ntmclient} command itself) is dropped here in favor of NeoForge's standard
 * {@code ModConfigSpec}-backed TOML file (editable by hand, or via NeoForge's config screen if one
 * is registered), matching how the Neo Edition reference already collapsed the same CE class.
 * This is a real behavior change from CE, not an oversight - flagging for lead sign-off per the
 * area's research plan.
 */
public class ClientConfig {

    public static IntValue GEIGER_OFFSET_HORIZONTAL;
    public static IntValue GEIGER_OFFSET_VERTICAL;
    public static IntValue INFO_OFFSET_HORIZONTAL;
    public static IntValue INFO_OFFSET_VERTICAL;
    public static IntValue INFO_POSITION;
    public static BooleanValue GUN_ANIMS_LEGACY;
    public static BooleanValue GUN_MODEL_FOV;
    public static BooleanValue GUN_VISUAL_RECOIL;
    public static DoubleValue GUN_ANIMATION_SPEED;
    public static BooleanValue ITEM_TOOLTIP_SHOW_OREDICT;
    public static BooleanValue ITEM_TOOLTIP_SHOW_CUSTOM_NUKE;
    public static BooleanValue MAIN_MENU_WACKY_SPLASHES;
    public static BooleanValue DODD_RBMK_DIAGNOSTIC;
    public static BooleanValue RENDER_CABLE_HANG;
    public static BooleanValue NUKE_HUD_FLASH;
    public static BooleanValue NUKE_HUD_SHAKE;
    public static BooleanValue RENDER_REEDS;
    public static BooleanValue JEI_HIDE_SECRETS;
    public static BooleanValue COOLING_TOWER_PARTICLES;
    public static BooleanValue RENDER_REBAR_SIMPLE;
    public static IntValue RENDER_HELIOSTAT_BEAM_LIMIT;
    public static IntValue TOOL_HUD_INDICATOR_X;
    public static IntValue TOOL_HUD_INDICATOR_Y;
    public static BooleanValue SHOW_BLOCK_META_OVERLAY;
    public static BooleanValue BADGES_HUD;
    public static BooleanValue HEALTHBAR_HUD;

    static void init(ModConfigSpec.Builder builder) {
        builder.push("hud");

        GEIGER_OFFSET_HORIZONTAL = builder.comment("Horizontal pixel offset for the geiger counter HUD element.")
                .defineInRange("geigerOffsetHorizontal", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        GEIGER_OFFSET_VERTICAL = builder.comment("Vertical pixel offset for the geiger counter HUD element.")
                .defineInRange("geigerOffsetVertical", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        INFO_OFFSET_HORIZONTAL = builder.comment("Horizontal pixel offset for the info panel.")
                .defineInRange("infoOffsetHorizontal", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        INFO_OFFSET_VERTICAL = builder.comment("Vertical pixel offset for the info panel.")
                .defineInRange("infoOffsetVertical", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        INFO_POSITION = builder.comment("Info panel position: 0 - top left, 1 - top right, 2 - next to the crosshair.")
                .defineInRange("infoPosition", 0, 0, 2);
        TOOL_HUD_INDICATOR_X = builder.comment("Tool HUD indicator horizontal offset.")
                .defineInRange("toolHudIndicatorX", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        TOOL_HUD_INDICATOR_Y = builder.comment("Tool HUD indicator vertical offset.")
                .defineInRange("toolHudIndicatorY", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        BADGES_HUD = builder.comment("Shows perk/achievement badges on the HUD.").define("badgesHud", true);
        HEALTHBAR_HUD = builder.comment("Shows the custom entity healthbar HUD element.").define("healthbarHud", true);
        NUKE_HUD_FLASH = builder.comment("Toggles screen flash from nuke explosions.").define("nukeHudFlash", true);
        NUKE_HUD_SHAKE = builder.comment("Toggles HUD shake from nuke explosions.").define("nukeHudShake", true);
        SHOW_BLOCK_META_OVERLAY = builder.comment("Shows a debug overlay with block metadata/state info.").define("showBlockMetaOverlay", false);

        builder.pop();

        builder.push("guns");

        GUN_ANIMS_LEGACY = builder.comment("Uses the legacy (pre-overhaul) gun animation set.").define("gunAnimsLegacy", false);
        GUN_MODEL_FOV = builder.comment("Applies the player's FOV setting to held gun models.").define("gunModelFov", false);
        GUN_VISUAL_RECOIL = builder.comment("Toggles visual recoil on held guns.").define("gunVisualRecoil", true);
        GUN_ANIMATION_SPEED = builder.comment("Multiplier for gun animation playback speed.")
                .defineInRange("gunAnimationSpeed", 1.0D, -Double.MAX_VALUE, Double.MAX_VALUE);

        builder.pop();

        builder.push("rendering");

        ITEM_TOOLTIP_SHOW_OREDICT = builder.comment("Shows OreDict tags in item tooltips.").define("itemTooltipShowOredict", true);
        ITEM_TOOLTIP_SHOW_CUSTOM_NUKE = builder.comment("Shows custom nuke stats in item tooltips.").define("itemTooltipShowCustomNuke", true);
        MAIN_MENU_WACKY_SPLASHES = builder.comment("Toggles wacky splash text on the main menu.").define("mainMenuWackySplashes", true);
        DODD_RBMK_DIAGNOSTIC = builder.comment("Toggles the RBMK diagnostic overlay on the DoDD device.").define("doddRbmkDiagnostic", true);
        RENDER_CABLE_HANG = builder.comment("Renders cables with a hanging droop instead of straight lines.").define("renderCableHang", true);
        RENDER_REEDS = builder.comment("Renders reed/cane-like decorative stalks.").define("renderReeds", true);
        JEI_HIDE_SECRETS = builder.comment("Hides secret/hidden items from JEI.").define("jeiHideSecrets", true);
        COOLING_TOWER_PARTICLES = builder.comment("Renders cooling tower steam particles.").define("coolingTowerParticles", true);
        RENDER_REBAR_SIMPLE = builder.comment("Uses a simplified rebar model for performance.").define("renderRebarSimple", false);
        RENDER_HELIOSTAT_BEAM_LIMIT = builder.comment("Maximum number of heliostat beams rendered at once.")
                .defineInRange("renderHeliostatBeamLimit", 250, 0, Integer.MAX_VALUE);

        builder.pop();
    }
}
