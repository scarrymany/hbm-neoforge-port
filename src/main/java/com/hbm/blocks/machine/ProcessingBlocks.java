package com.hbm.blocks.machine;

import com.hbm.blocks.BlockBase;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.container.machine.ProcessingMenus;
import com.hbm.inventory.recipes.ProcessingRecipes;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Block + {@code BlockItem} registration for Phase 2's shredder/assembler/crystallizer/mixer family -
 * see {@code docs/phase2/machines_shredder_assembler_crystallizer_mixer.md}. Mirrors
 * {@code PowerGenBlocks}' shape (table-driven {@code registerAll()}/{@code registerBlock} helper,
 * block-entity types in a sibling {@code com.hbm.blockentity.machine.ProcessingBlockEntities} class,
 * {@code MenuType}s registered from here too) - see that class's own javadoc for why this whole
 * family wires into the game with exactly one call from {@code ModBlocks.register()} and no other
 * shared-file edit (this task's wiring notes name that one line).
 * <p>
 * {@link #registerAll()} also bootstraps {@link ProcessingRecipes} (forces its static
 * {@code RecipeType}/{@code RecipeSerializer} fields to register into {@code HbmRecipes}'s shared
 * {@code DeferredRegister}s - see that class's own javadoc for why this is the one guaranteed-safe
 * place to trigger that class's loading, strictly before {@code HbmRecipes.register(modEventBus)}
 * runs later in {@code MainRegistry}'s constructor).
 */
public final class ProcessingBlocks {

    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 15.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();

    public static DeferredBlock<MachineShredderBlock> MACHINE_SHREDDER;
    public static DeferredBlock<MachineAssemblyMachineBlock> MACHINE_ASSEMBLER;
    public static DeferredBlock<MachineCrystallizerBlock> MACHINE_CRYSTALLIZER;
    public static DeferredBlock<MachineMixerBlock> MACHINE_MIXER;

    private ProcessingBlocks() {
    }

    public static void registerAll() {
        MACHINE_SHREDDER = registerBlock("machine_shredder", () -> new MachineShredderBlock(MACHINE_PROPS));
        MACHINE_ASSEMBLER = registerBlock("machine_assembly_machine", () -> new MachineAssemblyMachineBlock(MACHINE_PROPS));
        MACHINE_CRYSTALLIZER = registerBlock("machine_crystallizer", () -> new MachineCrystallizerBlock(MACHINE_PROPS));
        MACHINE_MIXER = registerBlock("machine_mixer", () -> new MachineMixerBlock(MACHINE_PROPS));
        // CE ModBlocks.java:1057 / AssemblyMachineRecipes.java:245 ass.precass. Casing only.
        registerBlock("machine_precass", () -> new BlockBase(MACHINE_PROPS));

        com.hbm.blockentity.machine.ProcessingBlockEntities.registerAll();
        ProcessingMenus.registerAll();
        ProcessingRecipes.bootstrap();
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }
}
