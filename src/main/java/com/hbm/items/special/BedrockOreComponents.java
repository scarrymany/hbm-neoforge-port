package com.hbm.items.special;

import com.hbm.main.MainRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data component backing {@code ItemBedrockOreBase}'s per-{@link BedrockOreType} scan amounts (see
 * {@link BedrockOreAmounts}), replacing CE's six-key NBT compound.
 * <p>
 * Follows the same one-DeferredRegister-per-area pattern already established by
 * {@code com.hbm.hazard.HazardComponents} and {@code com.hbm.items.HbmDataComponents} rather than
 * folding into either of those (both belong to other Phase 1 areas).
 */
public final class BedrockOreComponents {

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MainRegistry.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BedrockOreAmounts>> SCAN_AMOUNTS =
            DATA_COMPONENT_TYPES.register("bedrock_ore_scan", () -> DataComponentType.<BedrockOreAmounts>builder()
                    .persistent(BedrockOreAmounts.CODEC)
                    .networkSynchronized(BedrockOreAmounts.STREAM_CODEC)
                    .build());

    private BedrockOreComponents() {
    }

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus);
    }
}
