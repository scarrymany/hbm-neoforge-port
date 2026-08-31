package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.CrucibleBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.container.machine.CrucibleMenus;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Block + {@code BlockItem} registration for the Crucible - mirrors {@code ProcessingBlocks}'
 * established shape (table-driven {@code registerAll()}/{@code registerBlock} helper, block-entity
 * type in a sibling {@code com.hbm.blockentity.machine.CrucibleBlockEntities} class, {@code MenuType}
 * registered from here too) so this whole family wires into the game with exactly one call from
 * {@code ModBlocks.register()} and no other shared-file edit (see this task's wiring notes for that
 * one line).
 * <p>
 * CE's {@code MachineCrucible} used {@code Material.ROCK} (no direct 1.21 equivalent) - matched here
 * against {@code SoundType.STONE} + a stone-machine strength profile, the same rock-like block
 * properties {@code ProcessingBlocks}/{@code PowerGenBlocks} already use for their own metal/stone
 * multiblocks, since CE's real {@code MachineCrucible} constructor carries no explicit
 * hardness/resistance override of its own to match instead.
 */
public final class CrucibleBlocks {

    private static final BlockBehaviour.Properties CRUCIBLE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 15.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();

    public static DeferredBlock<MachineCrucibleBlock> MACHINE_CRUCIBLE;

    private CrucibleBlocks() {
    }

    public static void registerAll() {
        MACHINE_CRUCIBLE = registerBlock("machine_crucible", () -> new MachineCrucibleBlock(CRUCIBLE_PROPS));

        CrucibleBlockEntities.registerAll();
        CrucibleMenus.registerAll();
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }
}
