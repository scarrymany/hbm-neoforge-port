package com.hbm.blocks.machine.chem;

import com.hbm.blockentity.machine.chem.ChemIsotopeBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.container.machine.chem.ChemIsotopeMenus;
import com.hbm.inventory.recipes.chem.CentrifugeRecipes;
import com.hbm.inventory.recipes.chem.ChemPlantRecipes;
import com.hbm.inventory.recipes.chem.CyclotronRecipes;
import com.hbm.inventory.recipes.chem.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.chem.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.chem.SILEXRecipes;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Block + {@code BlockItem} + recipe-table registration for Phase 2's chemical-plant/centrifuge/
 * gas-centrifuge/cyclotron/SILEX/electrolyser machine family - see
 * {@code docs/phase2/machines_chemical_isotope.md}. Mirrors {@code PowerGenBlocks}' shape (block-entity
 * registration in the sibling {@link ChemIsotopeBlockEntities} class, {@link ChemIsotopeMenus}'
 * {@code MenuType}s triggered from here too) - wiring this family into the game needs exactly one call
 * from {@code ModBlocks.register()} (see this task's wiring notes), no other shared file needs a
 * direct edit.
 * <p>
 * Also calls every recipe table's {@code register()} once (same precedent as
 * {@code OilChainBlocks} calling {@code RefineryRecipes.registerRefinery()}).
 * <p>
 * <b>Not ported this pass</b>: {@code TileEntityMachineChemicalFactory} (the 4-module Chemical
 * Factory) - the task's own machine list names "chemical plant" only; the Factory's extra coolant loop
 * and proxy-delegated capability surface are a substantially larger multiblock this pass was not asked
 * to build. {@code TileEntityMachineMiningLaser} - matched the "Laser" survey grep but is a
 * world-quarry block, not a chemistry/isotope machine (see the research doc's own recommendation to
 * treat its world-breaking loop as Phase-4-adjacent).
 */
public final class ChemIsotopeBlocks {

    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 15.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();

    public static DeferredBlock<CentrifugeBlock> CENTRIFUGE;
    public static DeferredBlock<GasCentrifugeBlock> GAS_CENTRIFUGE;
    public static DeferredBlock<SilexBlock> SILEX;
    public static DeferredBlock<CyclotronBlock> CYCLOTRON;
    public static DeferredBlock<ChemPlantBlock> CHEM_PLANT;
    public static DeferredBlock<ElectrolyserBlock> ELECTROLYSER;

    private ChemIsotopeBlocks() {
    }

    public static void registerAll() {
        CENTRIFUGE = registerBlock("machine_centrifuge", () -> new CentrifugeBlock(MACHINE_PROPS));
        GAS_CENTRIFUGE = registerBlock("machine_gascent", () -> new GasCentrifugeBlock(MACHINE_PROPS));
        SILEX = registerBlock("machine_silex", () -> new SilexBlock(MACHINE_PROPS));
        CYCLOTRON = registerBlock("machine_cyclotron", () -> new CyclotronBlock(MACHINE_PROPS));
        CHEM_PLANT = registerBlock("machine_chemical_plant", () -> new ChemPlantBlock(MACHINE_PROPS));
        ELECTROLYSER = registerBlock("machine_electrolyser", () -> new ElectrolyserBlock(MACHINE_PROPS));

        ChemIsotopeBlockEntities.registerAll();
        ChemIsotopeMenus.registerAll();

        CentrifugeRecipes.register();
        GasCentrifugeRecipes.register();
        SILEXRecipes.register();
        CyclotronRecipes.register();
        ChemPlantRecipes.register();
        ElectrolyserFluidRecipes.register();
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }
}
