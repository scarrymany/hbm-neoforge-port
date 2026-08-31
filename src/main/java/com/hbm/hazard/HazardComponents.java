package com.hbm.hazard;

import com.hbm.main.MainRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components that replace the raw NBT keys the hazard system used to read directly off {@link
 * net.minecraft.world.item.ItemStack} in CE.
 * <p>
 * NBT key -&gt; component mapping:
 * <ul>
 *     <li>{@code hfrHazRadiation} (float) -&gt; {@link #BONUS_RADIATION} - see
 *     {@link com.hbm.hazard.transformer.HazardTransformerRadiationNBT}</li>
 *     <li>{@code timer} (int) -&gt; {@link #UNSTABLE_DECAY_TIMER} - see
 *     {@link com.hbm.hazard.type.HazardTypeUnstable}</li>
 *     <li>{@code cRads} (double, CE's {@code BlockStorageCrate.CRATE_RAD_KEY}) -&gt;
 *     {@link #CRATE_RAD_KEY} - see {@link com.hbm.blocks.generic.BlockStorageCrate}, which
 *     re-exposes this same holder under the name its consumer code
 *     ({@link com.hbm.hazard.transformer.HazardTransformerRadiationContainer}) expects</li>
 * </ul>
 */
public class HazardComponents {

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MainRegistry.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> BONUS_RADIATION =
            DATA_COMPONENT_TYPES.register("bonus_radiation", () -> DataComponentType.<Float>builder()
                    .persistent(Codec.FLOAT)
                    .networkSynchronized(ByteBufCodecs.FLOAT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> UNSTABLE_DECAY_TIMER =
            DATA_COMPONENT_TYPES.register("unstable_decay_timer", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /**
     * Contained-item radiation carried by a dropped {@link com.hbm.blocks.generic.BlockStorageCrate}
     * item (CE: raw {@code double} NBT tag {@code "cRads"}, written straight onto the dropped item's
     * root NBT compound - see {@code upstream/hbm-ce}'s {@code BlockStorageCrate#CRATE_RAD_KEY} and
     * {@code TileEntityCrateBase#applyDropData}/{@code #assembleDropTag}). {@code Double}, not
     * {@code Float}, to match CE's {@code NBTTagCompound#setDouble}/{@code #getDouble} exactly.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> CRATE_RAD_KEY =
            DATA_COMPONENT_TYPES.register("crate_rad_key", () -> DataComponentType.<Double>builder()
                    .persistent(Codec.DOUBLE)
                    .networkSynchronized(ByteBufCodecs.DOUBLE)
                    .build());

    public static void register(final IEventBus modEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus);
    }
}
