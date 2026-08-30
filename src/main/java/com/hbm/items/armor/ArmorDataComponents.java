package com.hbm.items.armor;

import com.hbm.main.MainRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components backing the raw-NBT state CE's FSB power/fuel gate and jetpack fuel tanks used.
 * Pattern confirmed against {@code com.hbm.items.HbmDataComponents}/{@code items.tool.
 * ToolDataComponents} (already ported) - {@code DeferredRegister.create(Registries.
 * DATA_COMPONENT_TYPE, MODID)} plus {@code DataComponentType.builder().persistent(codec).
 * networkSynchronized(streamCodec).build()}. Per this package's task brief item 7:
 *
 * <ul>
 *     <li>{@link #ARMOR_CHARGE} - {@code long}, replaces {@code ArmorFSBPowered}'s raw
 *     {@code "charge"} NBT tag.</li>
 *     <li>{@link #JETPACK_FUEL} - {@code int}, replaces {@code JetpackBase}'s raw {@code "fuel"}
 *     NBT tag. Not consumed anywhere in this package (the jetpack items themselves are a later
 *     Phase 3 content package's job, per {@code docs/phase3/fsb_armor_and_jetpacks.md} headline
 *     finding #4) - registered now so that package has a component to build on.</li>
 *     <li>{@link #ARMOR_FUEL} - {@code int}, replaces {@code ArmorFSBFueled}'s raw {@code "fuel"}
 *     NBT tag. Same int-codec shape as {@link #JETPACK_FUEL} but a distinct registered id, per
 *     {@code docs/phase3/fsb_armor_and_jetpacks.md} Key design decision ("a naming call, not a new
 *     design") - {@code ArmorFSBFueled}'s fuel gate is a wholly different field from any jetpack's
 *     fuel amount even though both are plain {@code int} mB counters.</li>
 *     <li>{@link #JETPACK_GLIDER_TANK} - {@link JetpackTankState}, replaces {@code JetpackGlider}'s
 *     raw {@code "fuelTank"} NBT-compound field ({@code FluidTankNTM#writeToNBT}/{@code #readFromNBT}).
 *     See that record's own javadoc for why this is its own narrow component rather than a shared
 *     "item fluid tank" shape.</li>
 * </ul>
 */
public final class ArmorDataComponents {

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MainRegistry.MODID);

    /** {@code ArmorFSBPowered.charge} (CE NBT key {@code "charge"}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> ARMOR_CHARGE =
            DATA_COMPONENT_TYPES.register("armor_charge", () -> DataComponentType.<Long>builder()
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
                    .build());

    /** {@code JetpackBase.fuel} (CE NBT key {@code "fuel"}) - consumed by a later jetpack-content package. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> JETPACK_FUEL =
            DATA_COMPONENT_TYPES.register("jetpack_fuel", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    /** {@code ArmorFSBFueled.fuel} (CE NBT key {@code "fuel"}) - distinct id from {@link #JETPACK_FUEL}, see class javadoc. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ARMOR_FUEL =
            DATA_COMPONENT_TYPES.register("armor_fuel", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    /** {@code JetpackGlider}'s {@code FluidTankNTM}-in-NBT (CE key {@code "fuelTank"}) - see {@link JetpackTankState}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<JetpackTankState>> JETPACK_GLIDER_TANK =
            DATA_COMPONENT_TYPES.register("jetpack_glider_tank", () -> DataComponentType.<JetpackTankState>builder()
                    .persistent(JetpackTankState.CODEC)
                    .networkSynchronized(JetpackTankState.STREAM_CODEC)
                    .build());

    private ArmorDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus);
    }
}
