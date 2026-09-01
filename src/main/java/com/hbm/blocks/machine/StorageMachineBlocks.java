package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.CrateBlockEntity.CrateType;
import com.hbm.blocks.BlockBase;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Table-driven registration for Phase 2's storage-machine family (mass crates, single-block
 * batteries/capacitors, the capacitor bus, and this pass's own scoped-down single-block fluid tank -
 * see {@code docs/phase2/machines_storage.md}). Mirrors {@code com.hbm.blocks.generic.GenericCrateBlocks}'s
 * table-driven-{@code registerAll()} shape and its {@code registerBlock} helper.
 *
 * <p>Block-entity-type registration lives in the sibling
 * {@link com.hbm.blockentity.machine.StorageBlockEntities} class instead of here, matching the task's
 * suggested block/block-entity package split - that class's {@code BlockEntityType.Builder.of} calls
 * read the {@link DeferredBlock} fields this class exposes below, the same cross-reference shape
 * {@code GenericCrateBlocks} itself uses in a single file (just split across two files here since both
 * halves are large enough on their own).
 *
 * <p><b>Ids and creative tabs, copied verbatim from CE's {@code ModBlocks.java}</b> (read in full - see
 * the research report's registry table): all five crate grades + {@code safe} sit on
 * {@link ModCreativeTabs#MACHINE} (CE: {@code MainRegistry.machineTab}); all five battery grades are
 * {@code setCreativeTab(null)} in CE itself (hidden from creative, "still real CE content" per this
 * port's {@code ItemBattery} precedent - not a phase-2 port simplification); all five capacitor grades
 * plus {@code capacitor_bus} sit on {@link ModCreativeTabs#MACHINE}. The new
 * {@code machine_fluidtank_basic} id (not a CE id - see {@code FluidTankBlock}'s javadoc) is placed on
 * {@link ModCreativeTabs#MACHINE} too, matching CE's real {@code machine_fluidtank}'s own tab.
 */
public final class StorageMachineBlocks {

    private static final float HARDNESS_RESISTANCE_DEFAULT = 5.0F;

    public static final Map<CrateType, DeferredBlock<CrateBlock>> CRATES = new EnumMap<>(CrateType.class);

    public static DeferredBlock<BatteryBlock> BATTERY_POTATO;
    public static DeferredBlock<BatteryBlock> BATTERY;
    public static DeferredBlock<BatteryBlock> BATTERY_LITHIUM;
    public static DeferredBlock<BatteryBlock> BATTERY_SCHRABIDIUM;
    public static DeferredBlock<BatteryBlock> BATTERY_DINEUTRONIUM;

    public static DeferredBlock<CapacitorBlock> CAPACITOR_COPPER;
    public static DeferredBlock<CapacitorBlock> CAPACITOR_GOLD;
    public static DeferredBlock<CapacitorBlock> CAPACITOR_NIOBIUM;
    public static DeferredBlock<CapacitorBlock> CAPACITOR_TANTALIUM;
    public static DeferredBlock<CapacitorBlock> CAPACITOR_SCHRABIDATE;
    public static DeferredBlock<CapacitorBusBlock> CAPACITOR_BUS;

    public static DeferredBlock<FluidTankBlock> FLUID_TANK_BASIC;

    private StorageMachineBlocks() {
    }

    public static void registerAll() {
        registerCrates();
        registerBatteries();
        registerCapacitors();
        registerFluidTank();
    }

    private static void registerCrates() {
        registerCrate(CrateType.IRON, "crate_iron", 5.0F, 10.0F);
        registerCrate(CrateType.STEEL, "crate_steel", 5.0F, 20.0F);
        registerCrate(CrateType.TUNGSTEN, "crate_tungsten", 15.0F, 10000.0F);
        registerCrate(CrateType.DESH, "crate_desh", 7.5F, 300.0F);
        registerCrate(CrateType.SAFE, "safe", 7.5F, 10000.0F);
    }

    private static void registerCrate(CrateType type, String name, float hardness, float resistance) {
        DeferredBlock<CrateBlock> block = ModBlocks.BLOCKS.register(name,
                () -> new CrateBlock(BlockBehaviour.Properties.of().strength(hardness, resistance).sound(SoundType.METAL), type));
        ModItems.ITEMS.register(name, () -> new CrateBlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        CRATES.put(type, block);
    }

    private static void registerBatteries() {
        BATTERY_POTATO = registerBattery("machine_battery_potato", 10_000L);
        BATTERY = registerBattery("machine_battery", 1_000_000L);
        BATTERY_LITHIUM = registerBattery("machine_lithium_battery", 50_000_000L);
        BATTERY_SCHRABIDIUM = registerBattery("machine_schrabidium_battery", 25_000_000_000L);
        BATTERY_DINEUTRONIUM = registerBattery("machine_dineutronium_battery", 1_000_000_000_000L);
        // CE ModBlocks.java:970 / PlasmaForgeRecipes.java:176 ass.fensusan. Casing only.
        registerBlock("machine_battery_redd", () -> new BlockBase(batteryProps().sound(SoundType.METAL)),
                ModCreativeTabs.MACHINE);
    }

    /** All five grades: {@code setCreativeTab(null)} in CE itself - see class javadoc. */
    private static DeferredBlock<BatteryBlock> registerBattery(String name, long maxPower) {
        return registerBlock(name, () -> new BatteryBlock(batteryProps(), maxPower), null);
    }

    private static BlockBehaviour.Properties batteryProps() {
        return BlockBehaviour.Properties.of().strength(HARDNESS_RESISTANCE_DEFAULT, 10.0F);
    }

    private static void registerCapacitors() {
        CAPACITOR_COPPER = registerCapacitor("capacitor_copper", 1_000_000L);
        CAPACITOR_GOLD = registerCapacitor("capacitor_gold", 5_000_000L);
        CAPACITOR_NIOBIUM = registerCapacitor("capacitor_niobium", 25_000_000L);
        CAPACITOR_TANTALIUM = registerCapacitor("capacitor_tantalium", 150_000_000L);
        CAPACITOR_SCHRABIDATE = registerCapacitor("capacitor_schrabidate", 50_000_000_000L);

        CAPACITOR_BUS = registerBlock("capacitor_bus",
                () -> new CapacitorBusBlock(BlockBehaviour.Properties.of().strength(HARDNESS_RESISTANCE_DEFAULT, 10.0F).sound(SoundType.METAL)),
                ModCreativeTabs.MACHINE);
    }

    private static DeferredBlock<CapacitorBlock> registerCapacitor(String name, long maxPower) {
        return registerBlock(name,
                () -> new CapacitorBlock(BlockBehaviour.Properties.of().strength(HARDNESS_RESISTANCE_DEFAULT, 10.0F).sound(SoundType.METAL), maxPower),
                ModCreativeTabs.MACHINE);
    }

    private static void registerFluidTank() {
        FLUID_TANK_BASIC = registerBlock("machine_fluidtank_basic",
                () -> new FluidTankBlock(BlockBehaviour.Properties.of().strength(HARDNESS_RESISTANCE_DEFAULT, 20.0F).sound(SoundType.METAL)),
                ModCreativeTabs.MACHINE);
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(
            String name, Supplier<T> factory, @Nullable ResourceKey<CreativeModeTab> tab) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        if (tab != null) {
            CreativeTabContents.add(tab, block);
        }
        return block;
    }
}
