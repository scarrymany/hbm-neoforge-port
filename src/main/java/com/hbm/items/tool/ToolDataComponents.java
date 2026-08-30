package com.hbm.items.tool;

import com.hbm.main.MainRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Data components backing ItemStack state CE stored as raw NBT on tool items in this package:
 * {@code ItemToolAbilityFueled}'s fuel tank level, {@code ItemToolAbilityPower}'s battery charge,
 * and {@link ItemColtanCompass}'s persisted target deposit coordinates.
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

    /** {@link ItemColtanCompass} target deposit X coordinate (CE NBT key "colX"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COLTAN_X =
            DATA_COMPONENT_TYPES.register("coltan_x", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    /** {@link ItemColtanCompass} target deposit Z coordinate (CE NBT key "colZ"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COLTAN_Z =
            DATA_COMPONENT_TYPES.register("coltan_z", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    /**
     * {@link ItemDetonator}'s stored target position (CE NBT keys "x"/"y"/"z" on the detonator
     * stack itself). Vanilla {@link BlockPos} ships its own {@code Codec}/{@code StreamCodec}
     * ({@link BlockPos#CODEC}/{@link BlockPos#STREAM_CODEC}), so no custom codec is needed here.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> DETONATOR_POS =
            DATA_COMPONENT_TYPES.register("detonator_pos", () -> DataComponentType.<BlockPos>builder()
                    .persistent(BlockPos.CODEC)
                    .networkSynchronized(BlockPos.STREAM_CODEC)
                    .build());

    /**
     * {@link ItemMultiDetonator}'s stored target position list (CE NBT keys "xValues"/"yValues"/
     * "zValues" - three parallel {@code int[]} arrays on the detonator stack, collapsed here into
     * one ordered {@code List<BlockPos>}).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<BlockPos>>> MULTI_DETONATOR_POS =
            DATA_COMPONENT_TYPES.register("multi_detonator_pos", () -> DataComponentType.<List<BlockPos>>builder()
                    .persistent(BlockPos.CODEC.listOf())
                    .networkSynchronized(BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build());

    /**
     * {@link ItemRTTYPager}'s stored channel name (CE NBT key {@code "chan"} - a plain string, no
     * special codec needed).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> PAGER_CHANNEL =
            DATA_COMPONENT_TYPES.register("pager_channel", () -> DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());

    private ToolDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus);
    }
}
