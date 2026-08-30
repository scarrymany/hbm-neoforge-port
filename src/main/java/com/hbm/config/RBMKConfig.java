package com.hbm.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

/**
 * Backing store for {@code com.hbm.api.rbmk.RBMKDials} - port of CE's
 * {@code com.hbm.tileentity.machine.rbmk.RBMKDials.RBMKKeys}, ~26 values CE stores as per-world
 * gamerules ({@code world.getGameRules().setOrCreateGameRule(...)}, hand-parsed strings).
 * <p>
 * Registered into {@link HbmConfig}'s SERVER spec (not COMMON): in CE every one of these dials is
 * a vanilla gamerule, which is always synced from server to client, and RBMK gameplay genuinely
 * needs client-visible values (e.g. column height affects client-side rendering - CE's own
 * {@code RBMKDials} carries a dedicated client-side cache of the height dial specifically because
 * of this). NeoForge's {@code ModConfig.Type.SERVER} configs get the same client-sync behavior for
 * free, so every dial lives here rather than being split between COMMON/SERVER. See
 * {@code com.hbm.api.rbmk.RBMKDials}'s class javadoc for the full CE-gamerule-vs-port-config
 * design rationale (this is the one Open Question the package's own research report explicitly
 * asked to be decided rather than defaulted into).
 * <p>
 * <b>Integration:</b> this class does not wire itself into {@link HbmConfig} - multiple Phase 2
 * machine packages land their own config category in the same wave, so (matching this port's
 * convention for every other shared-aggregator-file collision this wave) the one-line call is left
 * for the orchestrating session to apply: add {@code RBMKConfig.init(serverBuilder);} to
 * {@link HbmConfig}'s SERVER {@code ModConfigSpec.Builder} static-init block, alongside
 * {@code ServerConfig.init(serverBuilder);}.
 */
public final class RBMKConfig {

    public static DoubleValue PASSIVE_COOLING;
    public static DoubleValue PASSIVE_COOLING_INNER;
    public static DoubleValue COLUMN_HEAT_FLOW;
    public static DoubleValue FUEL_DIFFUSION_MOD;
    public static DoubleValue FUEL_HEAT_PROVISION;
    public static IntValue COLUMN_HEIGHT;
    public static BooleanValue PERMANENT_SCRAP;
    public static DoubleValue BOILER_HEAT_CONSUMPTION;
    public static DoubleValue CONTROL_SPEED_MOD;
    public static DoubleValue REACTIVITY_MOD;
    public static DoubleValue OUTGASSER_MOD;
    public static DoubleValue SURGE_MOD;
    public static IntValue FLUX_RANGE;
    public static IntValue REASIM_RANGE;
    public static IntValue REASIM_COUNT;
    public static DoubleValue REASIM_OUTPUT_MOD;
    public static BooleanValue REASIM_BOILERS;
    public static DoubleValue REASIM_BOILER_SPEED;
    public static BooleanValue DISABLE_MELTDOWNS;
    public static BooleanValue ENABLE_MELTDOWN_OVERPRESSURE;
    public static DoubleValue MODERATOR_EFFICIENCY;
    public static DoubleValue ABSORBER_EFFICIENCY;
    public static DoubleValue REFLECTOR_EFFICIENCY;
    public static DoubleValue ABSORBER_HEAT_CONVERSION;
    public static BooleanValue DISABLE_DEPLETION;
    public static BooleanValue DISABLE_XENON;

    private RBMKConfig() {
    }

    public static void init(ModConfigSpec.Builder builder) {
        builder.push("rbmk");

        PASSIVE_COOLING = builder
                .comment("Heat per tick removed from an isolated RBMK column passively. [CE: dialPassiveCooling]")
                .defineInRange("passiveCooling", 2.5D, 0D, Double.MAX_VALUE);
        PASSIVE_COOLING_INNER = builder
                .comment("Heat per tick removed from an RBMK column passively when fully surrounded by neighbors. [CE: dialPassiveCoolingInner]")
                .defineInRange("passiveCoolingInner", 0.1D, 0D, 1D);
        COLUMN_HEAT_FLOW = builder
                .comment("Step size (0-1) for how quickly neighboring RBMK column heat equalizes per tick. [CE: dialColumnHeatFlow]")
                .defineInRange("columnHeatFlow", 0.2D, 0D, 1D);
        FUEL_DIFFUSION_MOD = builder
                .comment("Multiplier for how quickly a fuel rod's core and hull temperatures equalize. [CE: dialDiffusionMod]")
                .defineInRange("fuelDiffusionMod", 1.0D, 0D, Double.MAX_VALUE);
        FUEL_HEAT_PROVISION = builder
                .comment("Step size (0-1) for how quickly a fuel rod's hull heat transfers into its column. [CE: dialHeatProvision]")
                .defineInRange("fuelHeatProvision", 0.2D, 0D, 1D);
        COLUMN_HEIGHT = builder
                .comment("Total stacked RBMK block count per reactor column, core block included. [CE: dialColumnHeight]")
                .defineInRange("columnHeight", 4, 2, 16);
        PERMANENT_SCRAP = builder
                .comment("Whether RBMK debris/scrap entities persist indefinitely instead of despawning. [CE: dialEnablePermaScrap]")
                .define("permanentScrap", true);
        BOILER_HEAT_CONSUMPTION = builder
                .comment("Heat consumed per unit of steam an RBMK boiler column produces. [CE: dialBoilerHeatConsumption]")
                .defineInRange("boilerHeatConsumption", 0.1D, 0D, Double.MAX_VALUE);
        CONTROL_SPEED_MOD = builder
                .comment("Multiplier for how quickly RBMK control rods move. [CE: dialControlSpeed]")
                .defineInRange("controlSpeedMod", 1.0D, 0D, Double.MAX_VALUE);
        REACTIVITY_MOD = builder
                .comment("Global multiplier for RBMK fuel rod output flux. [CE: dialReactivityMod]")
                .defineInRange("reactivityMod", 1.0D, 0D, Double.MAX_VALUE);
        OUTGASSER_MOD = builder
                .comment("Multiplier for RBMK outgasser processing speed. [CE: dialOutgasserSpeedMod]")
                .defineInRange("outgasserMod", 1.0D, 0D, Double.MAX_VALUE);
        SURGE_MOD = builder
                .comment("Multiplier for the RBMK control-rod withdrawal power surge. [CE: dialControlSurgeMod]")
                .defineInRange("surgeMod", 1.0D, 0D, Double.MAX_VALUE);
        FLUX_RANGE = builder
                .comment("How many blocks a normal RBMK fuel rod's neutron flux travels. [CE: dialFluxRange]")
                .defineInRange("fluxRange", 5, 1, 100);
        REASIM_RANGE = builder
                .comment("How many blocks a ReaSim RBMK fuel rod's neutron flux travels. [CE: dialReasimRange]")
                .defineInRange("reasimRange", 10, 1, 100);
        REASIM_COUNT = builder
                .comment("How many neutron streams a ReaSim RBMK fuel rod emits. [CE: dialReasimCount]")
                .defineInRange("reasimCount", 6, 1, 24);
        REASIM_OUTPUT_MOD = builder
                .comment("Per-stream output flux multiplier for ReaSim fuel rods, to compensate for their higher stream count. [CE: dialReasimOutputMod]")
                .defineInRange("reasimOutputMod", 1.0D, 0D, Double.MAX_VALUE);
        REASIM_BOILERS = builder
                .comment("Whether every RBMK column acts like a boiler with dedicated in/outlet blocks. [CE: dialReasimBoilers]")
                .define("reasimBoilers", false);
        REASIM_BOILER_SPEED = builder
                .comment("Fraction (0-1) of the possible ReaSim steam produced per tick. [CE: dialReasimBoilerSpeed]")
                .defineInRange("reasimBoilerSpeed", 0.05D, 0D, 1D);
        DISABLE_MELTDOWNS = builder
                .comment("Disables RBMK meltdowns entirely when a fuel column overheats. [CE: dialDisableMeltdowns]")
                .define("disableMeltdowns", false);
        ENABLE_MELTDOWN_OVERPRESSURE = builder
                .comment("Whether connected fluid pipes/turbines explode during an RBMK meltdown. [CE: dialEnableMeltdownOverpressure]")
                .define("enableMeltdownOverpressure", false);
        MODERATOR_EFFICIENCY = builder
                .comment("Fraction (0-1) of neutrons moderated from fast to slow when passing through a moderator column. [CE: dialModeratorEfficiency]")
                .defineInRange("moderatorEfficiency", 1.0D, 0D, 1D);
        ABSORBER_EFFICIENCY = builder
                .comment("Fraction (0-1) of flux absorbed when a stream hits an absorber column. [CE: dialAbsorberEfficiency]")
                .defineInRange("absorberEfficiency", 1.0D, 0D, 1D);
        REFLECTOR_EFFICIENCY = builder
                .comment("Fraction (0-1) of flux reflected when a stream hits a reflector column. [CE: dialReflectorEfficiency]")
                .defineInRange("reflectorEfficiency", 1.0D, 0D, 1D);
        ABSORBER_HEAT_CONVERSION = builder
                .comment("Degrees C generated per unit of flux absorbed by an absorber column. [CE: dialAbsorberHeatConversion]")
                .defineInRange("absorberHeatConversion", 0.05D, 0D, 1D);
        DISABLE_DEPLETION = builder
                .comment("Disables RBMK fuel rod depletion - rods last forever. [CE: dialDisableDepletion]")
                .define("disableDepletion", false);
        DISABLE_XENON = builder
                .comment("Disables RBMK xenon poisoning calculation entirely. [CE: dialDisableXenon]")
                .define("disableXenon", false);

        builder.pop();
    }
}
