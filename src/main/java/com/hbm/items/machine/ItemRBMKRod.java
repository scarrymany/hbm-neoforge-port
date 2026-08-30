package com.hbm.items.machine;

import com.hbm.api.rbmk.IRBMKFluxReceiver.NType;
import com.hbm.api.rbmk.RBMKDials;
import com.hbm.items.ItemBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * RBMK fuel rod item - the reactivity/xenon-poisoning/heat "pure-logic core" this package's
 * research report calls out by name as the centerpiece of the whole work package. CE:
 * {@code com.hbm.items.machine.ItemRBMKRod} (599 lines, read in full).
 * <p>
 * <b>This class did not exist anywhere in the port before this pass</b> - see
 * docs/phase2/rbmk_reactor.md's Headline finding #3: two Phase-1 hazard-modifier classes already
 * committed to this repo ({@code HazardModifierRBMKRadiation}, {@code HazardModifierRBMKHot})
 * import and call {@code ItemRBMKRod.getEnrichment}/{@code getPoisonLevel}/{@code getHullHeat}
 * today, so this class is also unblocking an already-present compile break, not just adding new
 * content.
 * <p>
 * Every simulation method below ({@link #burn}, {@link #updateHeat}, {@link #provideHeat}, and
 * every {@code static} getter/setter) is a pure function over an {@link ItemStack} plus plain
 * {@code double}s - the {@link ServerLevel} parameters (kept for CE call-site/coordination
 * compatibility, see {@code com.hbm.api.rbmk.RBMKDials}'s class javadoc) are never dereferenced
 * for anything but a {@link RBMKDials} config read, so every method here is directly testable with
 * a hand-built {@link ItemStack} and no world/{@code BlockEntity} in play at all.
 * <p>
 * <b>NBT-to-Data-Component migration (already flagged as a candidate by
 * docs/phase1/items_machine.md):</b> CE stores {@code yield}/{@code xenon}/{@code core}/{@code hull}
 * as raw NBT doubles with an eager "set every key on first touch" workaround
 * ({@code setNBTDefaults}, called from both {@code onCreated} and every setter) because leaving any
 * key unset on a fresh {@code NBTTagCompound} would silently read back as {@code 0} instead of the
 * intended per-fuel-type default. This port's {@link MachineDataComponents} entries are each
 * independent, so {@code ItemStack#getOrDefault} already returns the correct per-field default with
 * no eager initialization needed - {@code setNBTDefaults}/{@code onCreated} are dropped as
 * genuinely redundant under this data model, not silently skipped.
 */
public class ItemRBMKRod extends ItemBase {

    public ItemRBMKPellet pellet;
    /** Full display name of the fuel rod. */
    public String fullName = "";
    /** Endpoint of the reactivity function. */
    public double reactivity;
    /** Self-inflicted flux from self-igniting fuels; {@code 0} for normal fuel. */
    public double selfRate;
    public EnumBurnFunc function = EnumBurnFunc.LOG_TEN;
    public EnumDepleteFunc depFunc = EnumDepleteFunc.GENTLE_SLOPE;
    /** Multiplier for xenon production. */
    public double xGen = 0.5D;
    /** Divider for xenon burnup. */
    public double xBurn = 50D;
    /** Heat produced per outFlux. */
    public double heat = 1D;
    /** Total potential inFlux the rod can take in its lifetime. */
    public double yield;
    /** The maximum heat of the rod's hull before things go wrong. The core can be as hot as it wants. */
    public double meltingPoint = 1000D;
    /** The speed at which the core heats the hull. */
    public double diffusion = 0.02D;
    /** Neutron type, the most efficient neutron type for fission. */
    public NType nType = NType.SLOW;
    /** Release type, the type of neutrons released by this fuel. */
    public NType rType = NType.FAST;
    /** RGB color of the rod when rendered in the fuel channel. */
    public int colorTint = 0x304825;
    /** When the heat coefficient starts acting. */
    public double heatCoeffStart = 0D;
    /** When the reaction multiplier of the coefficient hits 0 after taking effect. */
    public double heatCoeffLength = 0D;

    public ItemRBMKRod(ItemRBMKPellet pellet, String fullName, Properties properties) {
        this(fullName, properties);
        this.pellet = pellet;
    }

    public ItemRBMKRod(String fullName, Properties properties) {
        super(properties);
        this.fullName = fullName;
    }

    public ItemRBMKRod setTint(int tint) {
        this.colorTint = tint;
        return this;
    }

    public ItemRBMKRod setYield(double yield) {
        this.yield = yield;
        return this;
    }

    public ItemRBMKRod setStats(double funcEnd) {
        return setStats(funcEnd, 0);
    }

    public ItemRBMKRod setStats(double funcEnd, double selfRate) {
        this.reactivity = funcEnd;
        this.selfRate = selfRate;
        return this;
    }

    public ItemRBMKRod setFunction(EnumBurnFunc func) {
        this.function = func;
        return this;
    }

    public ItemRBMKRod setDepletionFunction(EnumDepleteFunc func) {
        this.depFunc = func;
        return this;
    }

    public ItemRBMKRod setHeatCoeff(double start, double length) {
        this.heatCoeffStart = start;
        this.heatCoeffLength = length;
        return this;
    }

    public ItemRBMKRod setXenon(double gen, double burn) {
        this.xGen = gen;
        this.xBurn = burn;
        return this;
    }

    public ItemRBMKRod setHeat(double heat) {
        this.heat = heat;
        return this;
    }

    public ItemRBMKRod setDiffusion(double diffusion) {
        this.diffusion = diffusion;
        return this;
    }

    public ItemRBMKRod setMeltingPoint(double meltingPoint) {
        this.meltingPoint = meltingPoint;
        return this;
    }

    public ItemRBMKRod setNeutronTypes(NType nType, NType rType) {
        this.nType = nType;
        this.rType = rType;
        return this;
    }

    /**
     * The whole per-tick fission cycle, in exact order (CE: {@code ItemRBMKRod#burn}):
     * <ol>
     *   <li>{@code inFlux += selfRate} (self-igniting fuels).</li>
     *   <li>If xenon is enabled: burn off {@link #xenonBurnFunc} (quadratic - faster at high flux),
     *   THEN attenuate {@code inFlux *= (1 - poisonLevel)} (this is the actual poisoning effect on
     *   the CURRENT tick's flux), THEN generate {@link #xenonGenFunc} using the already-attenuated
     *   flux, then clamp {@code xenon ∈ [0;100]}.</li>
     *   <li>Compute a heat-coefficient reactivity multiplier ({@code mult}, starts at 1; ramps down
     *   via a half-sine as core heat crosses {@link #heatCoeffStart}..{@code +}{@link #heatCoeffLength}
     *   - a self-limiting "this fuel de-rates itself at high temperature" curve, off by default).</li>
     *   <li>{@code outFlux = reactivityFunc(inFlux, enrichment * mult) * RBMKDials.getReactivityMod()}.</li>
     *   <li>If depletion is enabled: {@code yield -= inFlux} (clamped {@code >= 0}) - <b>note this
     *   consumes the PRE-poison {@code inFlux} from step 1, not the post-poison value from step 2
     *   or the {@code outFlux} from step 4</b> - easy to invert by accident, preserved exactly.</li>
     *   <li>{@code coreHeat += outFlux * heat}, then rectified (clamped {@code [20;1_000_000]}, NaN -&gt; 20) and stored.</li>
     * </ol>
     *
     * @return outFlux
     */
    public double burn(ServerLevel level, ItemStack stack, double inFlux) {
        inFlux += selfRate;

        if (RBMKDials.getXenon(level)) {
            double xenon = getPoison(stack);
            xenon -= xenonBurnFunc(inFlux);

            inFlux *= (1D - getPoisonLevel(stack));

            xenon += xenonGenFunc(inFlux);

            if (xenon < 0D) xenon = 0D;
            if (xenon > 100D) xenon = 100D;

            setPoison(stack, xenon);
        }

        double mult = 1D;
        double coreHeat = this.getCoreHeat(stack);

        if (this.heatCoeffStart != 0) {
            if (coreHeat >= this.heatCoeffStart) {
                double prog = (coreHeat - this.heatCoeffStart) / this.heatCoeffLength;
                if (prog > 1) prog = 1;
                mult = Math.sin((prog * Math.PI + Math.PI) / 2);
            }
        }

        double outFlux = reactivityFunc(inFlux, getEnrichment(stack) * mult) * RBMKDials.getReactivityMod(level);

        if (RBMKDials.getDepletion(level)) {
            double y = getYield(stack);
            y -= inFlux;

            if (y < 0D) y = 0D;

            setYield(stack, y);
        }

        coreHeat += outFlux * heat;

        this.setCoreHeat(stack, rectify(coreHeat));

        return outFlux;
    }

    private double rectify(double num) {
        if (num > 1_000_000D) num = 1_000_000D;
        if (num < 20D || Double.isNaN(num)) num = 20D;
        return num;
    }

    public static double getMeltdownFactor(double meltdownPercent) {
        if (meltdownPercent == 0) return 1;
        return 1D - 0.3D * (meltdownPercent / 100D);
    }

    /**
     * Heats the core based on outFlux (done by the caller before this runs, via {@link #burn}),
     * then moves some of that heat toward the hull - core/hull equalize toward their midpoint at a
     * {@link #diffusion} fraction per tick. CE: {@code ItemRBMKRod#updateHeat}.
     */
    public void updateHeat(ServerLevel level, ItemStack stack, double mod) {
        double coreHeat = this.getCoreHeat(stack);
        double hullHeat = this.getHullHeat(stack);

        if (coreHeat > hullHeat) {
            double mid = (coreHeat - hullHeat) / 2D;

            coreHeat -= mid * this.diffusion * RBMKDials.getFuelDiffusionMod(level) * mod;
            hullHeat += mid * this.diffusion * RBMKDials.getFuelDiffusionMod(level) * mod;

            this.setCoreHeat(stack, rectify(coreHeat));
            this.setHullHeat(stack, rectify(hullHeat));
        }
    }

    /**
     * Returns one tick's worth of heat transferred from the rod's hull into its column, and cools
     * the hull accordingly. CE: {@code ItemRBMKRod#provideHeat}.
     * <p>
     * <b>Inline meltdown short-circuit, independent of the real column meltdown trigger</b> (see
     * {@code com.hbm.api.rbmk.RBMKMeltdownTrigger}'s javadoc for the full two-threshold
     * explanation): if {@code hullHeat > meltingPoint}, core/hull/column heat are instantly
     * averaged three ways and the delta is returned as component heat. This happens INSIDE the
     * fuel item's own logic, independently of - and typically well before - the column's own
     * {@code heat > maxHeat()} check that triggers the real meltdown event.
     *
     * @return heat to add to the column
     */
    public double provideHeat(ServerLevel level, ItemStack stack, double heat, double mod) {
        double hullHeat = this.getHullHeat(stack);

        if (hullHeat > this.meltingPoint) {
            double coreHeat = this.getCoreHeat(stack);
            double avg = (heat + hullHeat + coreHeat) / 3D;
            this.setCoreHeat(stack, avg);
            this.setHullHeat(stack, avg);
            return avg - heat;
        }

        if (hullHeat <= heat) return 0;

        double ret = (hullHeat - heat) / 2;

        ret *= RBMKDials.getFuelHeatProvision(level) * mod;

        hullHeat -= ret;
        this.setHullHeat(stack, hullHeat);

        return ret;
    }

    public enum EnumBurnFunc {
        /** const, no reactivity */
        PASSIVE("trait.rbmx.flux.passive"),
        /** {@code (1 - e^(-x/25)) * reactivity * 100} */
        PLATEU("trait.rbmx.flux.euler"),
        /** {@code 100 / (1 + e^(-(x - 50) / 10))} - tiny amount of reactivity at x=0! */
        SIGMOID("trait.rbmx.flux.sigmoid"),
        /** {@code log10(x + 1) * reactivity * 50} */
        LOG_TEN("trait.rbmx.flux.logten"),
        /** {@code sqrt(x) * 10 * reactivity} */
        SQUARE_ROOT("trait.rbmx.flux.squrt"),
        /** {@code x-(x²/archLength) * reactivity} */
        ARCH("trait.rbmx.flux.arch"),
        /** {@code x * reactivity} */
        LINEAR("trait.rbmx.flux.linear"),
        /** {@code x^2 / 100 * reactivity} */
        QUADRATIC("trait.rbmx.flux.quadratic"),
        /** {@code x * (sin(x) + 1)} */
        EXPERIMENTAL("trait.rbmx.flux.experimental");

        public final String title;

        EnumBurnFunc(String title) {
            this.title = title;
        }
    }

    /**
     * @param in         input flux
     * @param enrichment {@code [0;100]} (or at least those are sane levels)
     * @return the amount of reactivity yielded, unmodified by xenon
     */
    public double reactivityFunc(double in, double enrichment) {
        double flux = in * reactivityModByEnrichment(enrichment);

        return switch (this.function) {
            case PASSIVE -> selfRate * enrichment;
            case LOG_TEN -> Math.log10(flux + 1) * 0.5D * reactivity;
            case PLATEU -> (1 - Math.pow(Math.E, -flux / 25D)) * reactivity;
            case ARCH -> Math.max((flux - (flux * flux / 10000D)) / 100D * reactivity, 0D);
            case SIGMOID -> reactivity / (1 + Math.pow(Math.E, -(flux - 50D) / 10D));
            case SQUARE_ROOT -> Math.sqrt(flux) * reactivity / 10D;
            case LINEAR -> flux / 100D * reactivity;
            case QUADRATIC -> flux * flux / 10000D * reactivity;
            case EXPERIMENTAL -> flux * (Math.sin(flux) + 1) * reactivity;
        };
    }

    public enum EnumDepleteFunc {
        /** old function */
        LINEAR,
        /** for breeding fuels such as MEU, maximum of 110% at 28% depletion */
        RAISING_SLOPE,
        /** for strong breeding fuels such as Th232, maximum of 132% at 64% depletion */
        BOOSTED_SLOPE,
        /** recommended for most fuels, maximum barely over the start, near the beginning */
        GENTLE_SLOPE,
        /** for arcade-style neutron sources */
        STATIC
    }

    /**
     * The non-{@code LINEAR}/{@code STATIC} shapes are literal "breeding" curves that produce MORE
     * reactivity than raw enrichment would suggest at partial depletion - this is intentional CE
     * game design (breeder fuels get better partway through their life), not a bug, and must
     * survive the port exactly.
     */
    public double reactivityModByEnrichment(double enrichment) {
        return switch (this.depFunc) {
            case STATIC -> 1D;
            //x + sin([x - 1]^2 * pi) works, maximum of 132% at 64% depletion
            case BOOSTED_SLOPE -> enrichment + Math.sin((enrichment - 1) * (enrichment - 1) * Math.PI);
            //x + (sin(x * pi) / 2) actually works, maximum of 110% at 28% depletion
            case RAISING_SLOPE -> enrichment + (Math.sin(enrichment * Math.PI) / 2D);
            //x + (sin(x * pi) / 3) also works
            case GENTLE_SLOPE -> enrichment + (Math.sin(enrichment * Math.PI) / 3D);
            default -> enrichment; // LINEAR
        };
    }

    /** Xenon generated per tick, linear function. */
    public double xenonGenFunc(double flux) {
        return flux * xGen;
    }

    /** Xenon burned away per tick, quadratic function. */
    public double xenonBurnFunc(double flux) {
        return (flux * flux) / xBurn;
    }

    /** @return enrichment {@code [0;1]} */
    public static double getEnrichment(ItemStack stack) {
        return getYield(stack) / ((ItemRBMKRod) stack.getItem()).yield;
    }

    /** @return poison {@code [0;1]} */
    public static double getPoisonLevel(ItemStack stack) {
        return getPoison(stack) / 100D;
    }

    // START Special flux curve handling.
    // CE comment, preserved: "Nothing really uses this yet, though it's a really fun feature to play around with."

    /** For the RBMK handler to see if the rod is special. */
    public boolean specialFluxCurve = false;

    public ItemRBMKRod setFluxCurve(boolean bool) {
        specialFluxCurve = bool;
        return this;
    }

    /** Double 1: flux ratio in. Double 2: depletion value. Return: output flux ratio. */
    BiFunction<Double, Double, Double> ratioCurve;

    /** Double 1: flux quantity in. Double 2: flux ratio in. Return: output flux quantity. */
    BiFunction<Double, Double, Double> fluxCurve;

    public ItemRBMKRod setOutputRatioCurve(Function<Double, Double> func) {
        this.ratioCurve = (fluxRatioIn, depletion) -> func.apply(fluxRatioIn) * 1.0D;
        return this;
    }

    public ItemRBMKRod setDepletionOutputRatioCurve(BiFunction<Double, Double, Double> func) {
        this.ratioCurve = func;
        return this;
    }

    public ItemRBMKRod setOutputFluxCurve(BiFunction<Double, Double, Double> func) {
        this.fluxCurve = func;
        return this;
    }

    public double fluxRatioOut(double fluxRatioIn, double depletion) {
        return Math.clamp(ratioCurve.apply(fluxRatioIn, depletion), 0D, 1D);
    }

    public double fluxFromRatio(double quantity, double ratio) {
        return fluxCurve.apply(quantity, ratio);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.ITALIC + this.fullName));

        if (getHullHeat(stack) >= 50 || getCoreHeat(stack) >= 50) {
            tooltip.add(Component.literal(ChatFormatting.GOLD + "Cooling required"));
        }

        if (selfRate > 0 || this.function == EnumBurnFunc.SIGMOID) {
            tooltip.add(Component.literal(ChatFormatting.RED + "Self-igniting"));
        }

        double depletion = ((yield - getYield(stack)) / yield) * 100000D;
        tooltip.add(Component.literal(ChatFormatting.GREEN + "Depletion: " + ((int) depletion) / 1000D + "%"));
        tooltip.add(Component.literal(ChatFormatting.DARK_PURPLE + "Xenon poison: " + ((int) (getPoison(stack) * 1000D)) / 1000D + "%"));
        tooltip.add(Component.literal(ChatFormatting.BLUE + "Splits with: " + nType.name()));
        tooltip.add(Component.literal(ChatFormatting.BLUE + "Splits into: " + rType.name()));
        tooltip.add(Component.literal(ChatFormatting.GOLD + "Heat per tick at full power: " + heat + "°C"));
        tooltip.add(Component.literal(ChatFormatting.RED + "Skin temp: " + ((int) (getHullHeat(stack) * 10D)) / 10D + "°C"));
        tooltip.add(Component.literal(ChatFormatting.RED + "Core temp: " + ((int) (getCoreHeat(stack) * 10D)) / 10D + "°C"));
        tooltip.add(Component.literal(ChatFormatting.DARK_RED + "Melting point: " + meltingPoint + "°C"));

        super.appendHoverText(stack, context, tooltip, flag);
    }

    // ==================== NBT/Data Component accessors ====================

    public static void setYield(ItemStack stack, double yield) {
        stack.set(MachineDataComponents.RBMK_ROD_YIELD.get(), yield);
    }

    /** Falls back to the rod type's own max {@link #yield} when unset (CE: a fresh stack's lazily-defaulted NBT tag read back as the max yield - see this class's javadoc). */
    public static double getYield(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemRBMKRod rod)) return 0;
        return stack.getOrDefault(MachineDataComponents.RBMK_ROD_YIELD.get(), rod.yield);
    }

    public static void setPoison(ItemStack stack, double xenon) {
        stack.set(MachineDataComponents.RBMK_ROD_XENON.get(), xenon);
    }

    public static double getPoison(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.RBMK_ROD_XENON.get(), 0D);
    }

    public static void setCoreHeat(ItemStack stack, double heat) {
        stack.set(MachineDataComponents.RBMK_ROD_CORE_HEAT.get(), heat);
    }

    public static double getCoreHeat(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.RBMK_ROD_CORE_HEAT.get(), 20D);
    }

    public static void setHullHeat(ItemStack stack, double heat) {
        stack.set(MachineDataComponents.RBMK_ROD_HULL_HEAT.get(), heat);
    }

    public static double getHullHeat(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.RBMK_ROD_HULL_HEAT.get(), 20D);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getEnrichment(stack) < 1D;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * (float) (1D - getEnrichment(stack)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x30A83C;
    }
}
