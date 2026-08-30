package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * PWR (pressurized water reactor) fuel. Tooltip/data item using a reactivity-function helper, no
 * tile entity reference. CE's fifteen metadata grades (fresh/hot/depleted x enrichment level)
 * become fifteen registered instances of this class instead of metadata on one item.
 */
public class ItemPWRFuel extends ItemBase {

    private final EnumPWRFuel type;

    public ItemPWRFuel(EnumPWRFuel type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public EnumPWRFuel getType() {
        return this.type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String color = ChatFormatting.GOLD.toString();
        String reset = ChatFormatting.RESET.toString();

        tooltip.add(Component.literal(color + "Heat per flux: " + reset + this.type.heatEmission + " TU"));
        tooltip.add(Component.literal(color + "Reaction function: " + reset + this.type.function.getLabelForFuel()));
        tooltip.add(Component.literal(color + "Fuel type: " + reset + this.type.function.getDangerFromFuel()));
    }

    public enum EnumPWRFuel {
        MEU(5.0D, new FuelReactivityFunction.Logarithmic(20 * 30).withDiv(2_500)),
        HEU233(7.5D, new FuelReactivityFunction.Sqrt(25)),
        HEU235(7.5D, new FuelReactivityFunction.Sqrt(22.5)),
        MEN(7.5D, new FuelReactivityFunction.Logarithmic(22.5 * 30).withDiv(2_500)),
        HEN237(7.5D, new FuelReactivityFunction.Sqrt(27.5)),
        MOX(7.5D, new FuelReactivityFunction.Logarithmic(20 * 30).withDiv(2_500)),
        MEP(7.5D, new FuelReactivityFunction.Logarithmic(22.5 * 30).withDiv(2_500)),
        HEP239(10.0D, new FuelReactivityFunction.Sqrt(22.5)),
        HEP241(10.0D, new FuelReactivityFunction.Sqrt(25)),
        MEA(7.5D, new FuelReactivityFunction.Logarithmic(25 * 30).withDiv(2_500)),
        HEA242(10.0D, new FuelReactivityFunction.Sqrt(25)),
        HES326(12.5D, new FuelReactivityFunction.Sqrt(27.5)),
        HES327(12.5D, new FuelReactivityFunction.Sqrt(30)),
        BFB_AM_MIX(2.5D, new FuelReactivityFunction.Sqrt(15), 250_000_000),
        BFB_PU241(2.5D, new FuelReactivityFunction.Sqrt(15), 250_000_000);

        public static final EnumPWRFuel[] VALUES = values();

        public final double heatEmission;
        public final FuelReactivityFunction function;
        public final double yield;

        EnumPWRFuel(double heatEmission, FuelReactivityFunction function) {
            this(heatEmission, function, 1_000_000_000);
        }

        EnumPWRFuel(double heatEmission, FuelReactivityFunction function, double yield) {
            this.heatEmission = heatEmission;
            this.function = function;
            this.yield = yield;
        }

        /**
         * Narrow gap-fill for {@code com.hbm.blockentity.machine.PWRControllerBlockEntity} (Phase 2
         * PWR package): CE's own {@code TileEntityPWRController.update()} calls
         * {@code fuel.function.effonix(fluxPerRod)} directly - a legal same-behavior cross-package
         * call there because CE's {@code Function} class is public. This port's
         * {@link FuelReactivityFunction} was deliberately scoped package-private in Phase 1 (see its
         * own javadoc) since no consumer outside {@code com.hbm.items.machine} existed yet; the PWR
         * controller is now that consumer. Rather than widen {@link FuelReactivityFunction}/
         * {@code effonix} to public (CE's own scope, more than this one call site needs), this single
         * public forwarding method gives the controller exactly the one operation it requires, with
         * {@link FuelReactivityFunction} itself staying package-private.
         */
        public double reactivity(double flux) {
            return this.function.effonix(flux);
        }
    }
}
