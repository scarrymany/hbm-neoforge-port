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
import net.neoforged.neoforge.gametest.EmptyTemplate;
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
 * class exercises is grounded. The one genuinely unverified piece is the exact shape of NeoForge's
 * {@code @GameTestHolder}/{@code @PrefixGameTestTemplate}/{@code @EmptyTemplate} annotations
 * (no in-repo precedent exists to cross-check against - a grep of upstream/neo-edition for
 * "GameTest" returns zero hits); if any of their argument names differ from what's written here,
 * the fix is mechanical and does not change this class's actual test logic.
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
    @GameTest(template = "", timeoutTicks = 200)
    @EmptyTemplate(value = {5, 5, 5})
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
     * confirmed {@code ChemPlantBlockEntity.java:55} ({@code BATTERY_SLOT = 6}).
     */
    @GameTest(template = "", timeoutTicks = 300)
    @EmptyTemplate(value = {5, 5, 5})
    public static void chemPlantMakesEthanol(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ChemIsotopeBlocks.CHEM_PLANT.get());

        if (!(helper.getBlockEntity(pos) instanceof ChemPlantBlockEntity be)) {
            helper.fail("Expected a ChemPlantBlockEntity at " + pos);
            return;
        }

        be.inventory.setStackInSlot(6, creativeBattery());
        // Item-input slots start at 0 in this block entity's own layout; 10x sugar satisfies
        // chem.ethanol's AStack input (ComparableStack(Items.SUGAR, 10)).
        be.inventory.setStackInSlot(0, new ItemStack(Items.SUGAR, 10));

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
    @GameTest(template = "", timeoutTicks = 400)
    @EmptyTemplate(value = {5, 8, 5})
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
