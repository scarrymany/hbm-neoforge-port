package com.hbm.blocks.machine;

import com.hbm.blocks.BlockBase;
import com.hbm.blocks.BlockFallingBase;
import com.hbm.blocks.ModBlocks;
import com.hbm.blockentity.machine.oil.OilChainBlockEntities;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.container.machine.oil.OilChainMenus;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Block + {@code BlockItem} registration for Phase 2's oil production chain (derrick, pumpjack,
 * fracking tower, refinery) - see {@code docs/phase2/oil_production_chain.md}. Mirrors
 * {@code PowerGenBlocks}' established shape (table-driven {@code registerAll()}/
 * {@code registerMachine} helper, block-entity registration in a sibling
 * {@link OilChainBlockEntities} class, {@link OilChainMenus}' {@code MenuType}s triggered from here
 * too) so wiring this family into the game needs exactly one call from {@code ModBlocks.register()} -
 * no other shared file needs a direct edit.
 *
 * <p>Also closes this area's own small, explicitly-cheap Deferred-scope gap (research report
 * Deferred scope #7): {@code ore_oil_empty}/{@code ore_bedrock_oil} (needed for the extractors'
 * {@code canSuckBlock}/bedrock-suck checks to ever find anything once Phase 4 world-gen places real
 * deposits) and {@code ore_oil_sand}/{@code sand_dirty}/{@code sand_dirty_red}/{@code stone_cracked}
 * (fracking's {@link com.hbm.world.feature.OilSpot} staining targets) - all six are plain decorative
 * blocks in CE too (a {@code BlockBase}/{@code BlockFallingBase} instance apiece, no dedicated class,
 * no special drop behavior beyond {@code dropSelf}), registered here rather than invented as new
 * files, matching this port's own {@code PlantBlocks} precedent for {@code dirt_dead}/{@code dirt_oily}.
 * {@code ore_oil}/{@code oil_pipe}/{@code dirt_oily}/{@code dirt_dead} are NOT re-registered here -
 * already registered elsewhere in this port (Phase 0/1), referenced by the oil-chain block entities
 * via a lazy registry lookup instead (see {@code OilDrillBaseBlockEntity#resolve}) to avoid a
 * cross-package compile-time field dependency for a handful of identity checks.</p>
 */
public final class OilChainBlocks {

    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 15.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    private static final BlockBehaviour.Properties ORE_EMPTY_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.STONE);
    private static final BlockBehaviour.Properties BEDROCK_ORE_PROPS =
            BlockBehaviour.Properties.of().strength(-1.0F, 3_600_000F).sound(SoundType.STONE);
    private static final BlockBehaviour.Properties SAND_PROPS =
            BlockBehaviour.Properties.of().strength(0.5F, 1.0F).sound(SoundType.SAND);
    private static final BlockBehaviour.Properties CRACKED_STONE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F).sound(SoundType.STONE);

    public static DeferredBlock<MachineOilWellBlock> MACHINE_OIL_WELL;
    public static DeferredBlock<MachinePumpjackBlock> MACHINE_PUMPJACK;
    public static DeferredBlock<MachineFrackingTowerBlock> MACHINE_FRACKING_TOWER;
    public static DeferredBlock<MachineRefineryBlock> MACHINE_REFINERY;

    public static DeferredBlock<BlockBase> ORE_OIL_EMPTY;
    public static DeferredBlock<BlockBase> ORE_BEDROCK_OIL;
    public static DeferredBlock<BlockFallingBase> ORE_OIL_SAND;
    public static DeferredBlock<BlockFallingBase> SAND_DIRTY;
    public static DeferredBlock<BlockFallingBase> SAND_DIRTY_RED;
    public static DeferredBlock<BlockBase> STONE_CRACKED;

    private OilChainBlocks() {
    }

    public static void registerAll() {
        MACHINE_OIL_WELL = registerMachine("machine_well", () -> new MachineOilWellBlock(MACHINE_PROPS));
        MACHINE_PUMPJACK = registerMachine("machine_pumpjack", () -> new MachinePumpjackBlock(MACHINE_PROPS));
        MACHINE_FRACKING_TOWER = registerMachine("machine_fracking_tower", () -> new MachineFrackingTowerBlock(MACHINE_PROPS));
        MACHINE_REFINERY = registerMachine("machine_refinery", () -> new MachineRefineryBlock(MACHINE_PROPS));
        // flare / vacuum_distill / radiolysis are DummyableProcessBlocks (full TEs).

        ORE_OIL_EMPTY = registerResource("ore_oil_empty", () -> new BlockBase(ORE_EMPTY_PROPS));
        ORE_BEDROCK_OIL = registerResource("ore_bedrock_oil", () -> new BlockBase(BEDROCK_ORE_PROPS));
        ORE_OIL_SAND = registerResource("ore_oil_sand", () -> new BlockFallingBase(SAND_PROPS));
        SAND_DIRTY = registerResource("sand_dirty", () -> new BlockFallingBase(SAND_PROPS));
        SAND_DIRTY_RED = registerResource("sand_dirty_red", () -> new BlockFallingBase(SAND_PROPS));
        STONE_CRACKED = registerResource("stone_cracked", () -> new BlockBase(CRACKED_STONE_PROPS));

        OilChainBlockEntities.registerAll();
        OilChainMenus.registerAll();
        // NOT called here: RefineryRecipes.registerRefinery(). registerAll() runs synchronously from
        // ModBlocks.register(modEventBus), itself called from MainRegistry's constructor - strictly
        // before any RegisterEvent fires - but registerRefinery() resolves DeferredItem.get() (e.g.
        // PlateCrystalWasteItems.CRYSTAL_SULFUR) eagerly while building its ItemStack outputs, which
        // would throw IllegalStateException at startup (same bug class as the sedna gun eager-field
        // crash, see XFactory556mm's javadoc). Moved into CommonEvents#commonSetup's enqueueWork,
        // which this port already uses for exactly this timing requirement - see CommonEvents.java.
    }

    private static <T extends Block> DeferredBlock<T> registerMachine(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }

    private static <T extends Block> DeferredBlock<T> registerResource(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.RESOURCE, block);
        return block;
    }
}
