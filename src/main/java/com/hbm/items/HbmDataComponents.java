package com.hbm.items;

import com.hbm.main.MainRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Custom data components backing item-stack state that CE stored as raw NBT keys.
 *
 * This class did not exist in CE (which had no data component system) and is not part of the
 * file list this agent was scoped to, but ISatChip and BrokenItem - both in scope - need a
 * DataComponentType to replace their NBT reads/writes, and no such registry class exists
 * anywhere else in the target tree yet. It lives in com.hbm.items (this agent's package) rather
 * than a new com.hbm.items.component package so it stays inside scope; the integration step
 * should decide whether to fold it into a broader mod-wide component registry later.
 *
 * Pattern confirmed against the Neo Edition reference's NtmDataComponents
 * (com.hbm.items.component.NtmDataComponents): DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID)
 * plus DataComponentType.builder().persistent(codec).networkSynchronized(streamCodec).build().
 *
 * NBT key -> component mapping:
 *   BrokenItem  : "itemID" + "itemMeta" (a wrapped Item registry name + damage value) -> hbm:wrapped_item, DataComponentType<ItemStack>
 *                 storing the full wrapped ItemStack directly (meta/damage-as-variant no longer exists post-flattening).
 *   ISatChip    : "freq" (int, stored directly in the stack's tag compound) -> hbm:sat_freq, DataComponentType<Integer>
 */
public final class HbmDataComponents {

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MainRegistry.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemStack>> WRAPPED_ITEM =
            DATA_COMPONENT_TYPES.register("wrapped_item", () -> DataComponentType.<ItemStack>builder()
                    .persistent(ItemStack.OPTIONAL_CODEC)
                    .networkSynchronized(ItemStack.OPTIONAL_STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SAT_FREQ =
            DATA_COMPONENT_TYPES.register("sat_freq", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    private HbmDataComponents() {}

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus);
    }
}
