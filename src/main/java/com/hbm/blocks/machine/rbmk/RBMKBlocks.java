package com.hbm.blocks.machine.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKBlockEntities;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Block + {@code BlockItem} registration for every RBMK reactor column, plus the console and the two
 * standalone fluid-pipe stubs (inlet/outlet) and the autoloader - this work package's full scope per
 * {@code docs/phase2/rbmk_reactor.md}. Mirrors {@code PWRBlocks}' shape (table-driven
 * {@code registerAll()}, block-entity registration in the sibling
 * {@link com.hbm.blockentity.machine.rbmk.RBMKBlockEntities}) - see that class's own javadoc for the
 * full rationale. Wiring this whole family into the game needs exactly one call from
 * {@code ModBlocks.register()} (see this task's wiring notes).
 * <p>
 * Every column extends {@link RBMKBaseBlock} (itself {@link BlockDummyable}) - a 1x1xN multiblock per
 * column, not one big reactor-wide multiblock, per this task's own headline finding. Registry names
 * are this port's own choice (CE's real names were not exhaustively cross-checked given this
 * package's size); functionally equivalent, not guaranteed byte-identical to CE's registry ids.
 */
public final class RBMKBlocks {

    private static final BlockBehaviour.Properties COLUMN_PROPS =
            BlockBehaviour.Properties.of().strength(3.0F, 30.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();

    public static DeferredBlock<RBMKRodBlock> ROD, ROD_MOD;
    public static DeferredBlock<RBMKRodReaSimBlock> ROD_REASIM, ROD_REASIM_MOD;
    public static DeferredBlock<RBMKModeratorBlock> MODERATOR;
    public static DeferredBlock<RBMKAbsorberBlock> ABSORBER;
    public static DeferredBlock<RBMKReflectorBlock> REFLECTOR;
    public static DeferredBlock<RBMKBlankBlock> BLANK;
    public static DeferredBlock<RBMKControlManualBlock> CONTROL, CONTROL_MOD, CONTROL_REASIM;
    public static DeferredBlock<RBMKControlAutoBlock> CONTROL_AUTO, CONTROL_REASIM_AUTO;
    public static DeferredBlock<RBMKBoilerBlock> BOILER;
    public static DeferredBlock<RBMKOutgasserBlock> OUTGASSER;
    public static DeferredBlock<RBMKCoolerBlock> COOLER;
    public static DeferredBlock<RBMKHeaterBlock> HEATER;
    public static DeferredBlock<RBMKStorageBlock> STORAGE;
    public static DeferredBlock<RBMKInletBlock> INLET;
    public static DeferredBlock<RBMKOutletBlock> OUTLET;
    public static DeferredBlock<RBMKAutoloaderBlock> AUTOLOADER;
    public static DeferredBlock<RBMKConsoleBlock> CONSOLE;

    // Mini-panels (display family)
    public static DeferredBlock<RBMKDisplayBlankBlock> DISPLAY_BLANK;
    public static DeferredBlock<RBMKNumitronBlock> NUMITRON;
    public static DeferredBlock<RBMKTerminalBlock> TERMINAL;

    private RBMKBlocks() {
    }

    public static void registerAll() {
        ROD = registerBlock("rbmk_rod", () -> new RBMKRodBlock(COLUMN_PROPS, false));
        ROD_MOD = registerBlock("rbmk_rod_mod", () -> new RBMKRodBlock(COLUMN_PROPS, true));
        ROD_REASIM = registerBlock("rbmk_rod_reasim", () -> new RBMKRodReaSimBlock(COLUMN_PROPS, false));
        ROD_REASIM_MOD = registerBlock("rbmk_rod_reasim_mod", () -> new RBMKRodReaSimBlock(COLUMN_PROPS, true));
        MODERATOR = registerBlock("rbmk_moderator", () -> new RBMKModeratorBlock(COLUMN_PROPS));
        ABSORBER = registerBlock("rbmk_absorber", () -> new RBMKAbsorberBlock(COLUMN_PROPS));
        REFLECTOR = registerBlock("rbmk_reflector", () -> new RBMKReflectorBlock(COLUMN_PROPS));
        BLANK = registerBlock("rbmk_blank", () -> new RBMKBlankBlock(COLUMN_PROPS));
        CONTROL = registerBlock("rbmk_control", () -> new RBMKControlManualBlock(COLUMN_PROPS, false, false));
        CONTROL_MOD = registerBlock("rbmk_control_mod", () -> new RBMKControlManualBlock(COLUMN_PROPS, true, false));
        CONTROL_REASIM = registerBlock("rbmk_control_reasim", () -> new RBMKControlManualBlock(COLUMN_PROPS, false, true));
        CONTROL_AUTO = registerBlock("rbmk_control_auto", () -> new RBMKControlAutoBlock(COLUMN_PROPS, false, false));
        CONTROL_REASIM_AUTO = registerBlock("rbmk_control_reasim_auto", () -> new RBMKControlAutoBlock(COLUMN_PROPS, false, true));
        BOILER = registerBlock("rbmk_boiler", () -> new RBMKBoilerBlock(COLUMN_PROPS));
        OUTGASSER = registerBlock("rbmk_outgasser", () -> new RBMKOutgasserBlock(COLUMN_PROPS));
        COOLER = registerBlock("rbmk_cooler", () -> new RBMKCoolerBlock(COLUMN_PROPS));
        HEATER = registerBlock("rbmk_heater", () -> new RBMKHeaterBlock(COLUMN_PROPS));
        STORAGE = registerBlock("rbmk_storage", () -> new RBMKStorageBlock(COLUMN_PROPS));
        INLET = registerBlock("rbmk_inlet", () -> new RBMKInletBlock(MACHINE_PROPS));
        OUTLET = registerBlock("rbmk_outlet", () -> new RBMKOutletBlock(MACHINE_PROPS));
        AUTOLOADER = registerBlock("rbmk_autoloader", () -> new RBMKAutoloaderBlock(MACHINE_PROPS));
        CONSOLE = registerBlock("rbmk_console", () -> new RBMKConsoleBlock(MACHINE_PROPS));

        // Mini-panels
        BlockBehaviour.Properties panelProps = BlockBehaviour.Properties.of().strength(3.0F, 30.0F).sound(SoundType.METAL).noOcclusion();
        DISPLAY_BLANK = registerBlock("rbmk_display_blank", () -> new RBMKDisplayBlankBlock(panelProps));
        NUMITRON = registerBlock("rbmk_numitron", () -> new RBMKNumitronBlock(panelProps));
        TERMINAL = registerBlock("rbmk_terminal", () -> new RBMKTerminalBlock(panelProps));

        RBMKBlockEntities.registerAll();
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.CONTROL, block);
        return block;
    }
}
