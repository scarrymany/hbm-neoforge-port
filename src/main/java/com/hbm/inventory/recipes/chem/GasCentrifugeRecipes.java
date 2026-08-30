package com.hbm.inventory.recipes.chem;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.GasCentrifugeRecipes} - the real gas-centrifuge
 * isotope-enrichment cascade math ({@code docs/phase2/machines_chemical_isotope.md}'s headline
 * finding). Every {@link PseudoFluidType}'s four numbers ({@code fluidConsumed}, {@code
 * fluidProduced}, {@code outputFluid}, item {@code output}) are preserved <b>verbatim</b> from CE -
 * this table IS the mod's nuclear-fuel-cycle enrichment yield/cost curve, ported exactly rather than
 * approximated.
 * <p>
 * <b>Item substitution</b> (documented, not silent, same precedent as {@code RefineryRecipes}'s
 * sulfur substitution): CE's plain {@code ModItems.fluorite} byproduct item is not yet registered in
 * this port (only the higher-tier {@code crystal_fluorite} exists, see
 * {@link PlateCrystalWasteItems#CRYSTAL_FLUORITE}) - every {@code fluorite} byproduct below
 * substitutes {@code CRYSTAL_FLUORITE} until a plain fluorite item lands.
 * {@code ModItems.nugget_pu238}/{@code nugget_pu_mix} (the plutonium chain's byproducts) map onto
 * this port's {@link IngotNuggetItems#NUGGET_PU238}/{@link IngotNuggetItems#NUGGET_PU_MIX} directly
 * (both confirmed present). The irradiated-water ({@code WATZ}/{@code MUD}) chain's
 * {@code powder_iron}/{@code powder_lead}/{@code dust}/{@code nuclear_waste_tiny} byproducts:
 * {@code dust} (plain rock dust) and {@code nuclear_waste_tiny} are not yet registered in this port,
 * so that one chain's byproducts substitute {@link BilletPowderItems#POWDER_IRON}/
 * {@link BilletPowderItems#POWDER_LEAD} only, dropping the two not-yet-existing items rather than
 * guessing at a replacement - <b>TODO(items-followup)</b>.
 */
public final class GasCentrifugeRecipes {

    public static final class PseudoFluidType {

        public static final Map<String, PseudoFluidType> TYPES = new HashMap<>();

        public static final PseudoFluidType NONE = new PseudoFluidType("NONE", 0, 0, null, false, (ItemStack[]) null);

        // uranium enrichment cascade - NUF6 -> LEUF6 -> MEUF6 -> HEUF6 (terminal, speed-upgrade gated)
        public static PseudoFluidType HEUF6;
        public static PseudoFluidType MEUF6;
        public static PseudoFluidType LEUF6;
        public static PseudoFluidType NUF6;

        // plutonium chain - single stage
        public static PseudoFluidType PF6;

        // irradiated-water chain - MUD -> MUD_HEAVY (terminal)
        public static PseudoFluidType MUD_HEAVY;
        public static PseudoFluidType MUD;

        static {
            HEUF6 = new PseudoFluidType("HEUF6", 300, 0, NONE, true,
                    new ItemStack(IngotNuggetItems.NUGGET_U238.get(), 2),
                    new ItemStack(IngotNuggetItems.NUGGET_U235.get(), 1),
                    new ItemStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get(), 1));
            MEUF6 = new PseudoFluidType("MEUF6", 200, 100, HEUF6, false,
                    new ItemStack(IngotNuggetItems.NUGGET_U238.get(), 1));
            LEUF6 = new PseudoFluidType("LEUF6", 300, 200, MEUF6, false,
                    new ItemStack(IngotNuggetItems.NUGGET_U238.get(), 1),
                    new ItemStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get(), 1));
            NUF6 = new PseudoFluidType("NUF6", 400, 300, LEUF6, false,
                    new ItemStack(IngotNuggetItems.NUGGET_U238.get(), 1));

            PF6 = new PseudoFluidType("PF6", 300, 0, NONE, false,
                    new ItemStack(IngotNuggetItems.NUGGET_PU238.get(), 1),
                    new ItemStack(IngotNuggetItems.NUGGET_PU_MIX.get(), 2),
                    new ItemStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get(), 1));

            MUD_HEAVY = new PseudoFluidType("MUD_HEAVY", 500, 0, NONE, false,
                    new ItemStack(BilletPowderItems.POWDER_IRON.get(), 1));
            MUD = new PseudoFluidType("MUD", 1000, 500, MUD_HEAVY, false,
                    new ItemStack(BilletPowderItems.POWDER_LEAD.get(), 1));
        }

        public final String name;
        private final int fluidConsumed;
        private final int fluidProduced;
        private final PseudoFluidType outputFluid;
        private final boolean isHighSpeed;
        private final ItemStack[] output;

        PseudoFluidType(String name, int fluidConsumed, int fluidProduced, PseudoFluidType outputFluid, boolean isHighSpeed, ItemStack... output) {
            this.name = name;
            this.fluidConsumed = fluidConsumed;
            this.fluidProduced = fluidProduced;
            this.outputFluid = outputFluid;
            this.isHighSpeed = isHighSpeed;
            this.output = output;
            TYPES.put(name, this);
        }

        public int getFluidConsumed() {
            return fluidConsumed;
        }

        public int getFluidProduced() {
            return fluidProduced;
        }

        public PseudoFluidType getOutputType() {
            return outputFluid;
        }

        public ItemStack[] getOutput() {
            return output;
        }

        public boolean getIfHighSpeed() {
            return isHighSpeed;
        }

        public String getTranslationKey() {
            return "hbmpseudofluid." + name.toLowerCase(Locale.US);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /** Real feed fluid -> the pseudo-fluid it converts into 1:1 the moment it enters the machine's real tank. */
    public static final Map<FluidType, PseudoFluidType> FLUID_CONVERSIONS = new HashMap<>();

    private static boolean registered = false;

    private GasCentrifugeRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        FLUID_CONVERSIONS.put(Fluids.UF6, PseudoFluidType.NUF6);
        FLUID_CONVERSIONS.put(Fluids.PUF6, PseudoFluidType.PF6);
        FLUID_CONVERSIONS.put(Fluids.WATZ, PseudoFluidType.MUD);
    }
}
