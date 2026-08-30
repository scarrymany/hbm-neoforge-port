package com.hbm.api.rbmk;

import com.hbm.config.RBMKConfig;
import net.minecraft.server.level.ServerLevel;

/**
 * Static, pure-read accessors for every RBMK reactor tunable ("dial").
 * <p>
 * CE: {@code com.hbm.tileentity.machine.rbmk.RBMKDials} (387 lines, read in full) - a
 * {@code World -> double/int/boolean} accessor over ~26 per-world gamerules
 * ({@code world.getGameRules().getString(key)}, hand-parsed to the right primitive per accessor,
 * lazily created via {@code createDials(World)} the first time a world loads).
 * <p>
 * <b>Design decision (flagged explicitly by docs/phase2/rbmk_reactor.md's Open Questions, exactly
 * as that report asked for):</b> this port has zero {@code GameRules}/{@code RegisterGameRulesEvent}
 * precedent anywhere in its own source or in the Neo Edition reference (confirmed by repo-wide
 * grep before writing this class), NeoForge 1.21.1's real typed {@code GameRules.Key<T>} API may
 * not even ship a built-in floating-point gamerule value type the way ~15 of these double-valued
 * dials would need, and this port already ships a fully-wired TOML {@link net.neoforged.neoforge.common.ModConfigSpec}
 * config system with zero new infrastructure required. Every dial is therefore backed by a
 * per-SERVER config value ({@link RBMKConfig}, registered into the SERVER spec so it is
 * auto-synced to every connecting client exactly like CE's vanilla-synced gamerules were) instead
 * of a per-WORLD gamerule.
 * <p>
 * Every accessor below keeps CE's {@code World} parameter (typed here as {@link ServerLevel}, since
 * RBMK simulation is server-only in CE) purely for call-site compatibility and forward-flexibility
 * - it is never dereferenced, so passing {@code null} is always safe, which also makes every dial
 * trivially usable from an isolated unit test with no {@link ServerLevel} in scope at all. CE's
 * client-side column-height caching workaround
 * ({@code clientColumnHeightRuleValue}/{@code updateClientColumnHeightRuleValue}/
 * {@code resetClientColumnHeightRuleValue}) existed only to route around vanilla 1.12 GameRules'
 * per-dimension replication quirks; NeoForge's {@code ModConfigSpec} SERVER-type sync already
 * delivers one consistent value to the client with no per-dimension scoping involved, so that
 * workaround has no reason to exist here and is dropped.
 */
public final class RBMKDials {

    private RBMKDials() {
    }

    /** CE: {@code getPassiveCooling(World)}. Heat/tick removed from an isolated column passively, {@code >= 0}. */
    public static double getPassiveCooling(ServerLevel level) {
        return RBMKConfig.PASSIVE_COOLING.get();
    }

    /** CE: {@code getPassiveCoolingInner(World)}. Heat/tick removed from a column passively when fully surrounded, {@code [0;1]}. */
    public static double getPassiveCoolingInner(ServerLevel level) {
        return RBMKConfig.PASSIVE_COOLING_INNER.get();
    }

    /** CE: {@code getColumnHeatFlow(World)}. Step size {@code [0;1]} for how quickly neighboring column heat equalizes per tick. */
    public static double getColumnHeatFlow(ServerLevel level) {
        return RBMKConfig.COLUMN_HEAT_FLOW.get();
    }

    /** CE: {@code getFuelDiffusionMod(World)}. Multiplier, {@code >= 0}, for fuel rod core/hull heat equalization speed. */
    public static double getFuelDiffusionMod(ServerLevel level) {
        return RBMKConfig.FUEL_DIFFUSION_MOD.get();
    }

    /** CE: {@code getFuelHeatProvision(World)}. Step size {@code [0;1]} for fuel rod hull-to-column heat transfer. */
    public static double getFuelHeatProvision(ServerLevel level) {
        return RBMKConfig.FUEL_HEAT_PROVISION.get();
    }

    /**
     * CE: {@code getColumnHeightRuleValue(World)}. The raw stacked-block-count dial, including the
     * core block itself, {@code [2;16]}. Synchronization/persistence code should use this accessor
     * (not {@link #getColumnHeight}) when it needs the literal configured payload.
     */
    public static int getColumnHeightRuleValue(ServerLevel level) {
        return RBMKConfig.COLUMN_HEIGHT.get();
    }

    /**
     * CE: {@code getColumnHeight(World)}. The vertical offset from the RBMK core block to the
     * topmost dummy/extra block - one less than {@link #getColumnHeightRuleValue} because that
     * counts the core block too. {@code [1;15]}.
     */
    public static int getColumnHeight(ServerLevel level) {
        return getColumnHeightRuleValue(level) - 1;
    }

    /** CE: {@code getPermaScrap(World)}. Whether RBMK debris/scrap entities persist indefinitely instead of despawning. */
    public static boolean getPermaScrap(ServerLevel level) {
        return RBMKConfig.PERMANENT_SCRAP.get();
    }

    /** CE: {@code getBoilerHeatConsumption(World)}. Heat consumed per steam unit an RBMK boiler column produces, {@code >= 0}. */
    public static double getBoilerHeatConsumption(ServerLevel level) {
        return RBMKConfig.BOILER_HEAT_CONSUMPTION.get();
    }

    /** CE: {@code getControlSpeed(World)}. Multiplier, {@code >= 0}, for control rod movement speed. */
    public static double getControlSpeed(ServerLevel level) {
        return RBMKConfig.CONTROL_SPEED_MOD.get();
    }

    /** CE: {@code getReactivityMod(World)}. Global multiplier, {@code >= 0}, for fuel rod output flux. */
    public static double getReactivityMod(ServerLevel level) {
        return RBMKConfig.REACTIVITY_MOD.get();
    }

    /** CE: {@code getOutgasserMod(World)}. Multiplier, {@code >= 0}, for outgasser processing speed. */
    public static double getOutgasserMod(ServerLevel level) {
        return RBMKConfig.OUTGASSER_MOD.get();
    }

    /** CE: {@code getSurgeMod(World)}. Multiplier, {@code >= 0}, for the control-rod withdrawal power surge - see {@link RBMKControlMath#getEffectiveMult}. */
    public static double getSurgeMod(ServerLevel level) {
        return RBMKConfig.SURGE_MOD.get();
    }

    /** CE: {@code getFluxRange(World)}. How many blocks a normal fuel rod's neutron flux travels, {@code [1;100]}. */
    public static int getFluxRange(ServerLevel level) {
        return RBMKConfig.FLUX_RANGE.get();
    }

    /** CE: {@code getReaSimRange(World)}. How many blocks a ReaSim fuel rod's neutron flux travels, {@code [1;100]}. */
    public static int getReaSimRange(ServerLevel level) {
        return RBMKConfig.REASIM_RANGE.get();
    }

    /** CE: {@code getReaSimCount(World)}. How many neutron streams a ReaSim fuel rod emits, {@code [1;24]}. */
    public static int getReaSimCount(ServerLevel level) {
        return RBMKConfig.REASIM_COUNT.get();
    }

    /** CE: {@code getReaSimOutputMod(World)}. Per-stream output flux multiplier, {@code >= 0}, for ReaSim fuel rods. */
    public static double getReaSimOutputMod(ServerLevel level) {
        return RBMKConfig.REASIM_OUTPUT_MOD.get();
    }

    /** CE: {@code getReasimBoilers(World)}. Whether every RBMK column acts like a boiler with dedicated in/outlet blocks. CE additionally ORs this with a {@code GeneralConfig.enable528 && enable528ReasimBoilers} pair this port's {@code GeneralConfig} does not expose - dropped, not a real behavior loss (both flags default {@code false} in CE). */
    public static boolean getReasimBoilers(ServerLevel level) {
        return RBMKConfig.REASIM_BOILERS.get();
    }

    /** CE: {@code getReaSimBoilerSpeed(World)}. Fraction {@code [0;1]} of the possible ReaSim steam produced per tick. */
    public static double getReaSimBoilerSpeed(ServerLevel level) {
        return RBMKConfig.REASIM_BOILER_SPEED.get();
    }

    /** CE: {@code getMeltdownsDisabled(World)}. Whether fuel columns should initiate a meltdown when overheating - inverted sense preserved (default {@code false} so older worlds keep meltdowns on). */
    public static boolean getMeltdownsDisabled(ServerLevel level) {
        return RBMKConfig.DISABLE_MELTDOWNS.get();
    }

    /** CE: {@code getOverpressure(World)}. Whether connected fluid pipes/turbines explode during a meltdown (Package C / Phase 4 scope; this dial is ported here for completeness even though this package does not consume it). */
    public static boolean getOverpressure(ServerLevel level) {
        return RBMKConfig.ENABLE_MELTDOWN_OVERPRESSURE.get();
    }

    /** CE: {@code getModeratorEfficiency(World)}. Fraction {@code [0;1]} of neutrons moderated fast-to-slow per moderator column passed through. */
    public static double getModeratorEfficiency(ServerLevel level) {
        return RBMKConfig.MODERATOR_EFFICIENCY.get();
    }

    /** CE: {@code getAbsorberEfficiency(World)}. Fraction {@code [0;1]} of flux absorbed per absorber column a stream hits. */
    public static double getAbsorberEfficiency(ServerLevel level) {
        return RBMKConfig.ABSORBER_EFFICIENCY.get();
    }

    /** CE: {@code getReflectorEfficiency(World)}. Fraction {@code [0;1]} of flux reflected per reflector column a stream hits. */
    public static double getReflectorEfficiency(ServerLevel level) {
        return RBMKConfig.REFLECTOR_EFFICIENCY.get();
    }

    /** CE: {@code getAbsorberHeatConversion(World)}. Degrees C generated per unit of flux an absorber column absorbs, {@code [0;1]} (CE clamps this to {@code [0;1]} despite the "degrees per flux" framing implying it could exceed 1 - preserved exactly as CE's real, if oddly-clamped, behavior). */
    public static double getAbsorberHeatConversion(ServerLevel level) {
        return RBMKConfig.ABSORBER_HEAT_CONVERSION.get();
    }

    /** CE: {@code getDepletion(World)}. Whether fuel rods deplete at all; inverted sense preserved ({@code !disableDepletion}). Disabling this makes rods last forever. */
    public static boolean getDepletion(ServerLevel level) {
        return !RBMKConfig.DISABLE_DEPLETION.get();
    }

    /** CE: {@code getXenon(World)}. Whether xenon poisoning is calculated at all; inverted sense preserved ({@code !disableXenon}). */
    public static boolean getXenon(ServerLevel level) {
        return !RBMKConfig.DISABLE_XENON.get();
    }
}
