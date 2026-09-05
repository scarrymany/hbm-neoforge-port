package com.hbm.gametest;

import com.hbm.blockentity.machine.MachineAssemblyMachineBlockEntity;
import com.hbm.blockentity.machine.chem.ChemPlantBlockEntity;
import com.hbm.blockentity.machine.rbmk.RBMKRodBlockEntity;
import com.hbm.blocks.machine.ProcessingBlocks;
import com.hbm.blocks.machine.chem.ChemIsotopeBlocks;
import com.hbm.blocks.machine.rbmk.RBMKBlocks;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.items.machine.rbmk.RBMKRods;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Bonus GameTest automation for docs/phase6/playtest_scenarios.md, scenario 1 ("full progression
 * chain"), steps 2-4 specifically (assembler, chem plant, RBMK core-heat rise) - the parts of that
 * scenario that are pure server-side state machines with no real player/GUI input, per this task's
 * own instruction on what belongs in GameTest versus a manual script. Steps 1 (ore -> steel,
 * expected-fail by design - see that document's own writeup) and 5 (the nuke) are intentionally
 * NOT automated here: step 1 has no code path to test (nothing to place), and step 5's blast radius
 * (175 blocks, {@code BombConfig.MAN_RADIUS}) is exactly the kind of large-radius, real-world-tick
 * stress test {@link ExplosionPerfGameTests} already covers at Tsar scale - duplicating it here at
 * a smaller radius would not add coverage.
 *
 * <p><b>Verification status</b>: this class has never been compiled or run - this sandbox cannot
 * run {@code ./gradlew} (see this task's own structured-output notes and
 * docs/phase6/playtest_scenarios.md §0). Every block-entity field/method name referenced below
 * (e.g. {@code MachineAssemblyMachineBlockEntity.BATTERY_SLOT}, {@code ChemPlantBlockEntity
 * .outputTanks}, {@code ItemRBMKRod.getCoreHeat}) was confirmed by directly reading the real,
 * already-committed source file it comes from - cited per-method below - so the domain logic this
 * class exercises is grounded. {@code @GameTestHolder} and {@code @PrefixGameTestTemplate} are
 * real NeoForge 1.21.1 annotations (confirmed against a fresh clone of neoforged/NeoForge, branch
 * {@code 1.21.1}) and are used correctly below. {@code @EmptyTemplate} is NOT real: it does not
 * exist anywhere in {@code net.neoforged.neoforge.gametest} in that branch, nor in any other
 * NeoForge branch checked (1.20.2 through the current tip, 25w14craftmine) - it was evidently
 * invented by whoever originally wrote this class, with no real basis. Per this fix's own task
 * instructions, its usages below are stubbed out (commented, with a TODO) rather than guessed at,
 * since there is no verified NeoForge/vanilla mechanism in 1.21.1 for an auto-generated, data-file
 * -free "empty" test structure of a given size - see the TODO at the first usage for the full
 * explanation and what a real fix would need.
 */
@GameTestHolder("hbm")
@PrefixGameTestTemplate(false)
public final class ProgressionChainGameTests {

    private static final ResourceLocation BATTERY_CREATIVE_ID =
            ResourceLocation.fromNamespaceAndPath("hbm", "battery_creative");

    /** Lazily resolved so this class does not need a direct static field for {@code hbm:plate_steel}. */
    private static final java.util.function.Supplier<Item> ITEM_PLATE_STEEL = () ->
            BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("hbm", "plate_steel"));

    private ProgressionChainGameTests() {
    }

    private static ItemStack creativeBattery() {
        Item battery = BuiltInRegistries.ITEM.get(BATTERY_CREATIVE_ID);
        return new ItemStack(battery);
    }

    /**
     * Scenario 1 step 2: assembler turns 1 {@code hbm:ingot_steel} into 1 {@code hbm:plate_steel}.
     * Recipe confirmed at {@code src/main/resources/data/hbm/recipe/assembler/plate_steel.json}
     * ({@code duration: 60} ticks, {@code power: 100}/tick). Slot layout confirmed
     * {@code MachineAssemblyMachineBlockEntity.java:74-80}: slot 0 battery, slots 4-15 input, slot
     * 16 output.
     */
    // TODO(gametest, fc7-gametest-emptytemplate): removed `@EmptyTemplate(value = {5, 5, 5})`.
    // That annotation does not exist in real NeoForge 1.21.1 (or any NeoForge branch from 1.20.2
    // through the current tip, 25w14craftmine - confirmed against a fresh clone of
    // neoforged/NeoForge) - it was invented, not a real API. With `@GameTest(template = "", ...)`
    // above, NeoForge's own GameTestRegistry#turnMethodIntoTestFunction derives the structure
    // resource name from the class+method (roughly
    // `hbm:progressionchaingametests.assemblercraftssteelplate`, per
    // net.neoforged.neoforge.gametest.GameTestHooks#prefixGameTestTemplate) and looks it up as a
    // real registered structure template - there is no vanilla/NeoForge mechanism in this version
    // to synthesize an empty NxHxD structure without one. A real fix needs either an actual
    // `.nbt` structure template at that resource location (intended size was 5x5x5), or some
    // other verified way to register an empty template at runtime. This stub only unblocks
    // `compileJava`; the test is not runnable as-is. See this class's own javadoc above.
    @GameTest(template = "", timeoutTicks = 200)
    public static void assemblerCraftsSteelPlate(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ProcessingBlocks.MACHINE_ASSEMBLER.get());

        if (!(helper.getBlockEntity(pos) instanceof MachineAssemblyMachineBlockEntity be)) {
            helper.fail("Expected a MachineAssemblyMachineBlockEntity at " + pos);
            return;
        }

        be.inventory.setStackInSlot(MachineAssemblyMachineBlockEntity.BATTERY_SLOT, creativeBattery());
        be.inventory.setStackInSlot(MachineAssemblyMachineBlockEntity.INPUT_START,
                new ItemStack(IngotNuggetItems.INGOT_STEEL.get()));

        helper.succeedWhen(() -> {
            ItemStack output = be.inventory.getStackInSlot(MachineAssemblyMachineBlockEntity.OUTPUT_SLOT);
            helper.assertTrue(!output.isEmpty() && output.getItem() == ITEM_PLATE_STEEL.get(),
                    "expected hbm:plate_steel in the assembler output slot, got " + output);
        });
    }

    /**
     * Scenario 1 step 3: chem plant runs {@code chem.ethanol} (10x {@code minecraft:sugar} -> 1000mB
     * ethanol, {@code duration: 100} ticks - {@code inventory/recipes/chem/ChemPlantRecipes.java:66
     * -70}, the only fluid-input-free recipe in this port's chem plant table). Battery slot
     * Exact CE {@code TileEntityMachineChemicalPlant} 0 / item in 4-6.
     */
    // TODO(gametest, fc7-gametest-emptytemplate): removed `@EmptyTemplate(value = {5, 5, 5})` -
    // same non-existent-annotation issue as assemblerCraftsSteelPlate above; see that method's
    // TODO and this class's javadoc for the full explanation.
    @GameTest(template = "", timeoutTicks = 300)
    public static void chemPlantMakesEthanol(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ChemIsotopeBlocks.CHEM_PLANT.get());

        if (!(helper.getBlockEntity(pos) instanceof ChemPlantBlockEntity be)) {
            helper.fail("Expected a ChemPlantBlockEntity at " + pos);
            return;
        }

        be.inventory.setStackInSlot(ChemPlantBlockEntity.BATTERY_SLOT, creativeBattery());
        be.inventory.setStackInSlot(ChemPlantBlockEntity.ITEM_IN_START, new ItemStack(Items.SUGAR, 10));

        helper.succeedWhen(() -> helper.assertTrue(
                be.outputTanks[0].getFill() > 0,
                "expected the chem plant's first output tank to have received ethanol, fill="
                        + be.outputTanks[0].getFill()));
    }

    /**
     * Scenario 1 step 4: a lone, unshielded {@code rbmk_fuel_po210be} rod's own core heat rises
     * with zero external flux, since it is a self-igniting ({@code selfRate=50},
     * {@code EnumBurnFunc.PASSIVE}) source - confirmed {@code items/machine/rbmk/RBMKRods.java
     * :170-173}. The column auto-fills its full multiblock height on placement of the single
     * {@code rbmk_rod} block (BlockDummyable convention, confirmed
     * {@code blocks/machine/rbmk/RBMKBaseBlock.java}), so only one {@code setBlock} call is needed.
     */
    // TODO(gametest, fc7-gametest-emptytemplate): removed `@EmptyTemplate(value = {5, 8, 5})` -
    // same non-existent-annotation issue as assemblerCraftsSteelPlate above; see that method's
    // TODO and this class's javadoc for the full explanation.
    @GameTest(template = "", timeoutTicks = 400)
    public static void rbmkRodCoreHeatRises(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, RBMKBlocks.ROD.get());

        if (!(helper.getBlockEntity(pos) instanceof RBMKRodBlockEntity be)) {
            helper.fail("Expected an RBMKRodBlockEntity at " + pos);
            return;
        }

        be.inventory.setStackInSlot(0, new ItemStack(RBMKRods.PO210BE.get()));

        double startingCoreHeat = ItemRBMKRod.getCoreHeat(be.inventory.getStackInSlot(0));

        // Give the rod's own updateEntity() a few ticks to run before sampling again - core heat
        // is expected to have moved measurably by then (heat=0.1 per this fuel's registration,
        // applied every tick once burn() starts producing nonzero outFlux from selfRate alone).
        helper.runAfterDelay(60, () -> {
            double laterCoreHeat = ItemRBMKRod.getCoreHeat(be.inventory.getStackInSlot(0));
            if (laterCoreHeat > startingCoreHeat) {
                helper.succeed();
            } else {
                helper.fail("RBMK rod core heat did not rise: started at " + startingCoreHeat
                        + ", still " + laterCoreHeat + " after 60 ticks");
            }
        });
    }
}
