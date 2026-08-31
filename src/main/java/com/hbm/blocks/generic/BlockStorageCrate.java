package com.hbm.blocks.generic;

import com.hbm.hazard.HazardComponents;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Marker interface for CE's mass-storage crate/safe block family, ported from
 * {@code upstream/hbm-ce}'s {@code com.hbm.blocks.generic.BlockStorageCrate} (read in full).
 * <p>
 * In CE this is a concrete {@code BlockContainer} subclass extended directly by {@code crate_iron}/
 * {@code crate_steel}/{@code crate_desh}/{@code safe} (CE's {@code ModBlocks.java}, verbatim
 * {@code new BlockStorageCrate(...)} instances) and indirectly by {@code crate_tungsten} through
 * CE's one-line {@code BlockStorageCrateRadResistant extends BlockStorageCrate} subclass. CE's
 * {@code BlockCanCrate}/{@code BlockJungleCrate}/{@code BlockSupplyCrate}/{@code BlockAmmoCrate}/
 * {@code BlockCrate} are a <b>separate, unrelated</b> block family in CE - each extends plain
 * {@code Block}/{@code BlockContainer}/{@code BlockFalling} directly, confirmed by reading all five
 * files - so despite the shared "crate" naming, this port's already-committed
 * {@link BlockCrate}/{@link BlockCanCrate}/{@link BlockJungleCrate}/{@link BlockSupplyCrate}/
 * {@link BlockAmmoCrate} correctly do <b>not</b> implement this interface and were not touched by
 * this class's introduction.
 * <p>
 * This port had already ported CE's real {@code BlockStorageCrate} lineage (before this interface
 * existed) as {@link com.hbm.blocks.machine.CrateBlock} - one class parameterized by
 * {@code CrateBlockEntity.CrateType} standing in for CE's five near-duplicate subclasses/instances
 * (crate_iron/steel/tungsten/desh/safe), registered by
 * {@code com.hbm.blocks.machine.StorageMachineBlocks#registerCrates}. Turning
 * {@code BlockStorageCrate} into an abstract class and inserting it into that already-shipped
 * class's hierarchy would force giving up its existing {@code extends BaseEntityBlock} (Java has no
 * multiple inheritance); a marker interface {@link com.hbm.blocks.machine.CrateBlock} can simply add
 * to its {@code implements} clause is far less invasive - it already implements one such marker
 * interface for the tungsten grade ({@link com.hbm.interfaces.IRadResistantBlock}) - and is exactly
 * what {@link com.hbm.hazard.transformer.HazardTransformerRadiationContainer}'s
 * {@code instanceof BlockStorageCrate} check needs to keep working unmodified.
 *
 * @see com.hbm.hazard.transformer.HazardTransformerRadiationContainer
 * @see com.hbm.blockentity.machine.CrateBlockEntity
 */
public interface BlockStorageCrate {

    /**
     * Contained-item radiation, read by
     * {@link com.hbm.hazard.transformer.HazardTransformerRadiationContainer} via
     * {@code stack.getOrDefault(BlockStorageCrate.CRATE_RAD_KEY.get(), 0D)}. CE stores this as a raw
     * {@code double} NBT tag directly on the dropped crate item's root compound, keyed {@code "cRads"}
     * (see CE's {@code BlockStorageCrate#CRATE_RAD_KEY} and
     * {@code TileEntityCrateBase#applyDropData}/{@code #assembleDropTag}); the 1.21 data-component
     * system replaces that raw NBT double with a real {@link DataComponentType}, registered alongside
     * this port's other hazard components in {@link HazardComponents} (see
     * {@link HazardComponents#CRATE_RAD_KEY}) and simply re-exposed here under the name CE's
     * consumer code expects.
     * <p>
     * Populated by {@code com.hbm.blockentity.machine.CrateBlockEntity#writeItemComponents}, a direct
     * port of CE's {@code TileEntityCrateBase.buildDropData} radiation sum.
     */
    DeferredHolder<DataComponentType<?>, DataComponentType<Double>> CRATE_RAD_KEY = HazardComponents.CRATE_RAD_KEY;
}
