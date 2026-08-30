package com.hbm.items.tool;

import com.hbm.main.MainRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components backing the two pieces of ItemStack state CE stored as raw NBT on tool items in
 * this package: {@code ItemToolAbilityFueled}'s fuel tank level and {@code ItemToolAbilityPower}'s
 * battery charge.
 *
 * <p>{@link com.hbm.api.energymk2.IBatteryItem}'s own javadoc anticipates a single shared
 * {@code hbm:battery_charge} component "registered by whichever area owns the mod's data-component
 * registry" - no such shared component exists anywhere in the tree yet, and registering one isn't
 * this area's call to make (it would live in {@code com.hbm.api.energymk2}, outside this area's
 * scope, and a concurrently-working energy-system agent may be defining it right now). This class
 * therefore registers a package-local {@code hbm:tool_charge} component instead, so
 * {@code ItemToolAbilityPower} has a working, self-contained implementation today. Migrating it
 * onto the real shared battery component (once one exists) is a follow-up integration task, not a
 * functional regression - the stored value and its semantics are identical either way.
 */
public final class ToolDataComponents {

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MainRegistry.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TOOL_FUEL =
            DATA_COMPONENT_TYPES.register("tool_fuel", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> TOOL_CHARGE =
            DATA_COMPONENT_TYPES.register("tool_charge", () -> DataComponentType.<Long>builder()
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
                    .build());

    private ToolDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus);
    }
}
