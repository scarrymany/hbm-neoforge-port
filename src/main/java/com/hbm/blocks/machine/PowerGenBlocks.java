package com.hbm.blocks.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.container.machine.PowerGenMenus;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Block + {@code BlockItem} registration for Phase 2's power-generation family (burner/steam
 * engines, RTGs, turbines, diesel/gas generators, the solar boiler+mirror pair) - see
 * {@code docs/phase2/machines_power_generation.md}. Mirrors {@code StorageMachineBlocks}' shape
 * (that concurrent Phase 2 pass's own table-driven {@code registerAll()}/{@code registerBlock}
 * helper), itself following {@code GenericDecoBlocks}' established convention.
 * <p>
 * Block-entity-type registration lives in the sibling {@link com.hbm.blockentity.machine.PowerGenBlockEntities}
 * class (that class's {@code BlockEntityType.Builder.of} calls read the {@link DeferredBlock} fields
 * below); {@link com.hbm.inventory.container.machine.PowerGenMenus}' {@code MenuType}s are triggered
 * from this class's {@link #registerAll()} too, so wiring this family into the game needs exactly
 * one call from {@code ModBlocks.register()} (see this task's wiring notes) - no other shared file
 * needs a direct edit.
 * <p>
 * <b>Not ported</b> (see the research report's own framing): {@code TileEntityMachineIGenerator}/
 * {@code MachineIGenerator} (a non-functional decorative easter egg even in CE - "port as inert
 * multiblock decoration or drop, at the implementer's discretion"; dropped here, zero gameplay value)
 * and {@code MachineGenerator} (a zero-logic decorative block with no paired tile entity, unrelated
 * to power generation despite the name). {@code TileEntityDiFurnaceRTG}/{@code TileEntityRtgFurnace}
 * are RTG-fuel-accelerated *smelting* machines, not power generators (zero HE output) - out of this
 * family's scope per the report's own classification, left to whichever area owns RTG-accelerated
 * processing.
 */
public final class PowerGenBlocks {

    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 15.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    private static final BlockBehaviour.Properties MIRROR_PROPS =
            BlockBehaviour.Properties.of().strength(2.0F, 10.0F).sound(SoundType.METAL);

    public static DeferredBlock<MachineRTGBlock> MACHINE_RTG;
    public static DeferredBlock<MachineMiniRTGBlock> MACHINE_MINI_RTG;
    public static DeferredBlock<MachineMiniRTGBlock> MACHINE_POWER_RTG;
    public static DeferredBlock<MachineSteamEngineBlock> MACHINE_STEAM_ENGINE;
    public static DeferredBlock<MachineDieselBlock> MACHINE_DIESEL;
    public static DeferredBlock<MachineCombustionEngineBlock> MACHINE_COMBUSTION_ENGINE;
    public static DeferredBlock<MachineTurbineBlock> MACHINE_TURBINE;
    public static DeferredBlock<MachineLargeTurbineBlock> MACHINE_LARGE_TURBINE;
    public static DeferredBlock<MachineIndustrialTurbineBlock> MACHINE_INDUSTRIAL_TURBINE;
    public static DeferredBlock<MachineTurbineGasBlock> MACHINE_TURBINE_GAS;
    public static DeferredBlock<MachineSolarBoilerBlock> MACHINE_SOLAR_BOILER;
    public static DeferredBlock<SolarMirrorBlock> SOLAR_MIRROR;

    private PowerGenBlocks() {
    }

    public static void registerAll() {
        MACHINE_RTG = registerBlock("machine_rtg_grey", () -> new MachineRTGBlock(MACHINE_PROPS));
        MACHINE_MINI_RTG = registerBlock("machine_minirtg", () -> new MachineMiniRTGBlock(MACHINE_PROPS, false));
        MACHINE_POWER_RTG = registerBlock("machine_powerrtg", () -> new MachineMiniRTGBlock(MACHINE_PROPS, true));
        MACHINE_STEAM_ENGINE = registerBlock("machine_steam_engine", () -> new MachineSteamEngineBlock(MACHINE_PROPS));
        MACHINE_DIESEL = registerBlock("machine_diesel", () -> new MachineDieselBlock(MACHINE_PROPS));
        MACHINE_COMBUSTION_ENGINE = registerBlock("machine_combustion_engine", () -> new MachineCombustionEngineBlock(MACHINE_PROPS));
        MACHINE_TURBINE = registerBlock("machine_turbine", () -> new MachineTurbineBlock(MACHINE_PROPS));
        MACHINE_LARGE_TURBINE = registerBlock("machine_large_turbine", () -> new MachineLargeTurbineBlock(MACHINE_PROPS));
        MACHINE_INDUSTRIAL_TURBINE = registerBlock("machine_industrial_turbine", () -> new MachineIndustrialTurbineBlock(MACHINE_PROPS));
        MACHINE_TURBINE_GAS = registerBlock("machine_turbine_gas", () -> new MachineTurbineGasBlock(MACHINE_PROPS));
        MACHINE_SOLAR_BOILER = registerBlock("machine_solar_boiler", () -> new MachineSolarBoilerBlock(MACHINE_PROPS));
        SOLAR_MIRROR = registerBlock("solar_mirror", () -> new SolarMirrorBlock(MIRROR_PROPS));
        // CE ModBlocks.java:979 — BlockBase transformer casings (assembler leftover).
        registerBlock("machine_transformer", () -> new com.hbm.blocks.BlockBase(MACHINE_PROPS));
        registerBlock("machine_transformer_20", () -> new com.hbm.blocks.BlockBase(MACHINE_PROPS));
        registerBlock("machine_transformer_dnt", () -> new com.hbm.blocks.BlockBase(MACHINE_PROPS));
        registerBlock("machine_transformer_dnt_20", () -> new com.hbm.blocks.BlockBase(MACHINE_PROPS));

        com.hbm.blockentity.machine.PowerGenBlockEntities.registerAll();
        PowerGenMenus.registerAll();
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }
}
