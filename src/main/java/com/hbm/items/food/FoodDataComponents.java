package com.hbm.items.food;

import com.hbm.main.MainRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components backing item-stack state that CE stored as raw item damage/NBT, for items ported
 * in {@code com.hbm.items.food}. Registered here (rather than the mod-wide
 * {@code com.hbm.items.HbmDataComponents}) because that class lives outside this area's package
 * scope; folding the two together is an integration-time decision for whoever owns that class -
 * mirrors {@code com.hbm.items.machine.MachineDataComponents}'s own note on the same tradeoff.
 */
public final class FoodDataComponents {

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MainRegistry.MODID);

    /**
     * {@code ItemCanteen}'s reuse-cooldown, counted down in seconds by {@link ItemCanteen#inventoryTick}.
     * CE stored this as the stack's item-damage value (a plain int reused as a cooldown timer, not a
     * durability bar - see docs/phase1/items_food_gear.md's NBT/Data-Component notes) - it is
     * deliberately not {@code DataComponents.DAMAGE}, since 1.21's damage bar is a genuine "this item is
     * worn out" concept and a canteen's cooldown is not that.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CANTEEN_COOLDOWN =
            register("canteen_cooldown", Codec.INT, ByteBufCodecs.INT);

    private FoodDataComponents() {
    }

    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
            String name, Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        return DATA_COMPONENT_TYPES.register(name, () -> DataComponentType.<T>builder()
                .persistent(codec)
                .networkSynchronized(streamCodec)
                .build());
    }

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus);
    }
}
