package com.hbm.items.special;

import com.hbm.main.MainRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components backing this area's NBT->component conversions (see docs/phase1/items_special.md
 * finding 5), following the same one-DeferredRegister-per-area pattern as
 * {@code com.hbm.items.HbmDataComponents} and {@code com.hbm.items.special.BedrockOreComponents}.
 * <p>
 * NBT key -> component mapping:
 * <ul>
 *     <li>{@code ItemCell}: metadata (NTM fluid id, 0 = empty) -> {@link #CELL_FLUID_ID},
 *     {@code DataComponentType<Integer>}. CE flattened this per-fluid via item metadata; the port
 *     keeps a single {@code hbm:cell} item with the fluid id carried as a component instead (see
 *     the design decision recorded in docs/phase1/items_special.md's per-file table).</li>
 *     <li>{@code ItemHot}/{@code ItemHotDusted}: {@code "heat"} (int) -> {@link #HEAT},
 *     {@code DataComponentType<Integer>}.</li>
 *     <li>{@code ItemClayTablet}: {@code "tabletSeed"} (long) -> {@link #TABLET_SEED},
 *     {@code DataComponentType<Long>}.</li>
 *     <li>{@code ItemBookLore}: {@code "k"}/{@code "p"}/{@code "cov_col"}/{@code "tit_col"}/
 *     {@code "p1..pN"} -> {@link #BOOK_LORE}, {@code DataComponentType<BookLoreContent>}
 *     (see {@link BookLoreContent}).</li>
 * </ul>
 */
public final class SpecialItemComponents {

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MainRegistry.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CELL_FLUID_ID =
            DATA_COMPONENT_TYPES.register("cell_fluid_id", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> HEAT =
            DATA_COMPONENT_TYPES.register("heat", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> TABLET_SEED =
            DATA_COMPONENT_TYPES.register("tablet_seed", () -> DataComponentType.<Long>builder()
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BookLoreContent>> BOOK_LORE =
            DATA_COMPONENT_TYPES.register("book_lore", () -> DataComponentType.<BookLoreContent>builder()
                    .persistent(BookLoreContent.CODEC)
                    .networkSynchronized(BookLoreContent.STREAM_CODEC)
                    .build());

    private SpecialItemComponents() {
    }

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus);
    }
}
