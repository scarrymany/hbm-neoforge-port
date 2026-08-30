package com.hbm.blockentity.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.BatteryBlock;
import com.hbm.blocks.machine.CapacitorBlock;
import com.hbm.blocks.machine.CrateBlock;
import com.hbm.blocks.machine.FluidTankBlock;
import com.hbm.blocks.machine.StorageMachineBlocks;
import com.hbm.capability.NTMEnergyCapabilityWrapper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.function.Supplier;

/**
 * {@link BlockEntityType} registration for Phase 2's storage-machine family, sibling to
 * {@link StorageMachineBlocks} (that class registers the {@link Block}s this one's
 * {@code BlockEntityType.Builder.of} calls reference - see its own javadoc for why the two are split
 * across packages instead of one file).
 *
 * <p>One {@link BlockEntityType} per block-entity <em>class</em>, valid across every grade of that
 * class's block (CE registers one CE {@code TileEntity} subclass per grade for crates/tile-entity
 * classes that need distinct behavior, but {@link CrateBlockEntity}/{@link BatteryBlockEntity}/
 * {@link CapacitorBlockEntity} already collapse that into one parameterized class each - see their
 * own javadoc - so one {@link BlockEntityType} naturally covers every grade's block instance, exactly
 * like {@code BlockEntityType.Builder.of(ctor, validBlock1, validBlock2, ...)} is designed for).
 *
 * <p>{@link #registerCapabilities} is this package's own {@link RegisterCapabilitiesEvent} handler -
 * unlike item capabilities (see {@code com.hbm.capability.ModCapabilities}, already wired to
 * {@code modEventBus} by {@code MainRegistry}), no block-entity capability listener exists anywhere in
 * this port yet, since no concrete Phase 2 machine block entity existed before this pass. Not
 * self-wiring (this class does not add itself to any event bus) - see this task's wiring notes for the
 * one extra line {@code MainRegistry} needs.
 */
public final class StorageBlockEntities {

    public static Supplier<BlockEntityType<CrateBlockEntity>> CRATE_TYPE;
    public static Supplier<BlockEntityType<BatteryBlockEntity>> BATTERY_TYPE;
    public static Supplier<BlockEntityType<CapacitorBlockEntity>> CAPACITOR_TYPE;
    public static Supplier<BlockEntityType<FluidTankBlockEntity>> FLUID_TANK_TYPE;

    private StorageBlockEntities() {
    }

    public static void registerAll() {
        CRATE_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("crate", () -> BlockEntityType.Builder.of(
                (pos, state) -> new CrateBlockEntity(CRATE_TYPE.get(), pos, state, ((CrateBlock) state.getBlock()).getCrateType()),
                StorageMachineBlocks.CRATES.values().stream().map(Supplier::get).toArray(Block[]::new)
        ).build(null));

        // BatteryBlockEntity/CapacitorBlockEntity/FluidTankBlockEntity all take a leading
        // BlockEntityType<?> constructor argument (required by MachineBaseBlockEntity/
        // LoadedBaseBlockEntity's own super(type, pos, state) constructor chain), so a bare
        // constructor reference does not match BlockEntityType.BlockEntitySupplier<T>'s
        // (BlockPos, BlockState) -> T shape the way GenericCrateBlocks' (BlockPos, BlockState)-only
        // block entities do - each needs an explicit lambda instead, closing over the type field
        // being assigned exactly like the CRATE_TYPE factory above already does.
        BATTERY_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_battery", () -> BlockEntityType.Builder.of(
                (pos, state) -> new BatteryBlockEntity(BATTERY_TYPE.get(), pos, state),
                StorageMachineBlocks.BATTERY_POTATO.get(), StorageMachineBlocks.BATTERY.get(),
                StorageMachineBlocks.BATTERY_LITHIUM.get(), StorageMachineBlocks.BATTERY_SCHRABIDIUM.get(),
                StorageMachineBlocks.BATTERY_DINEUTRONIUM.get()
        ).build(null));

        CAPACITOR_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("capacitor", () -> BlockEntityType.Builder.of(
                (pos, state) -> new CapacitorBlockEntity(CAPACITOR_TYPE.get(), pos, state),
                StorageMachineBlocks.CAPACITOR_COPPER.get(), StorageMachineBlocks.CAPACITOR_GOLD.get(),
                StorageMachineBlocks.CAPACITOR_NIOBIUM.get(), StorageMachineBlocks.CAPACITOR_TANTALIUM.get(),
                StorageMachineBlocks.CAPACITOR_SCHRABIDATE.get()
        ).build(null));

        FLUID_TANK_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_fluidtank_basic", () -> BlockEntityType.Builder.of(
                (pos, state) -> new FluidTankBlockEntity(FLUID_TANK_TYPE.get(), pos, state),
                StorageMachineBlocks.FLUID_TANK_BASIC.get()
        ).build(null));
    }

    /**
     * Wired from {@code MainRegistry}'s existing {@code modEventBus.addListener(...)} call site
     * (see this task's wiring notes) - the block-entity-capability counterpart to
     * {@code com.hbm.capability.ModCapabilities.register}'s item-capability registrations.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CRATE_TYPE.get(), (be, side) -> be.getItemHandlerCapability(side));

        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BATTERY_TYPE.get(), (be, side) -> be.getItemHandlerCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, BATTERY_TYPE.get(), (be, side) -> be.getEnergyStorageCapability(side));

        // CapacitorBlockEntity has no inventory and extends LoadedBaseBlockEntity directly (see its
        // own javadoc), so it has no getEnergyStorageCapability accessor to delegate to the way
        // MachineBaseBlockEntity subclasses above do - construct the wrapper directly instead,
        // matching MachineBaseBlockEntity#getEnergyStorageCapability's own body exactly (no
        // CapabilityContextProvider indirection needed: a capacitor is never a multiblock proxy
        // target, so its own position is always the correct accessor).
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CAPACITOR_TYPE.get(),
                (be, side) -> new NTMEnergyCapabilityWrapper(be, side == null ? null : be.getBlockPos()));

        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FLUID_TANK_TYPE.get(), (be, side) -> be.getFluidHandlerCapability(side));
    }
}
