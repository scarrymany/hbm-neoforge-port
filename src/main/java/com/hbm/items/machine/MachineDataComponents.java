package com.hbm.items.machine;

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
 * Data components backing item-stack state that CE stored as raw NBT keys, for every item ported
 * in {@code com.hbm.items.machine}. Registered here (rather than the mod-wide
 * {@code com.hbm.items.HbmDataComponents}) because that class lives outside this area's package
 * scope; folding the two together is an integration-time decision for whoever owns that class.
 * <p>
 * Several CE NBT key names ({@code depletion}, {@code life}) are reused across multiple unrelated
 * item classes with different meanings (arc electrode "durability", fuel rod / Zirnox rod "life",
 * pile rod MK2 / ICF pellet "depletion"). Per the porting plan each gets its own, non-generic
 * component type below instead of sharing one - never reuse one of these across two item classes
 * whose CE NBT meaning differed.
 */
public final class MachineDataComponents {

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MainRegistry.MODID);

    /** IBatteryItem charge, shared by every battery/capacitor-pack item in this package (CE NBT key "charge"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> CHARGE =
            register("charge", Codec.LONG, ByteBufCodecs.VAR_LONG);

    /** ItemArcElectrode/ItemArcElectrodeBurnt durability counter (CE NBT key "durability"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ARC_ELECTRODE_DURABILITY =
            register("arc_electrode_durability", Codec.INT, ByteBufCodecs.INT);

    /** ItemCapacitor's redcoil overcharge counter (CE NBT key "dura"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CAPACITOR_CHARGE =
            register("capacitor_charge", Codec.INT, ByteBufCodecs.INT);

    /** ItemFuelRod / ItemPlateFuel lifetime counter (CE NBT key "life"). Distinct from {@link #ZIRNOX_ROD_LIFE}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FUEL_ROD_LIFE =
            register("fuel_rod_life", Codec.INT, ByteBufCodecs.INT);

    /** ItemZirnoxRod lifetime counter (CE NBT key "life"). Distinct from {@link #FUEL_ROD_LIFE}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ZIRNOX_ROD_LIFE =
            register("zirnox_rod_life", Codec.INT, ByteBufCodecs.INT);

    /** ItemPileRodMK2 depletion fraction (CE NBT key "depletion", {@code KEY_NBT_DEPLETION}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> PILE_ROD_DEPLETION =
            register("pile_rod_depletion", Codec.DOUBLE, ByteBufCodecs.DOUBLE);

    /** ItemICFPellet fusion depletion counter (CE NBT key "depletion"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> ICF_PELLET_DEPLETION =
            register("icf_pellet_depletion", Codec.LONG, ByteBufCodecs.VAR_LONG);

    /** ItemRTGPellet decay counter (CE NBT key "PELLET_DEPLETION"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> RTG_PELLET_DEPLETION =
            register("rtg_pellet_depletion", Codec.LONG, ByteBufCodecs.VAR_LONG);

    /** ItemRBMKPellet 0-9 depletion/xenon stage (CE derived it from {@code meta % 10}, not NBT). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RBMK_PELLET_STAGE =
            register("rbmk_pellet_stage", Codec.INT, ByteBufCodecs.INT);

    /** ItemLens damage counter (CE NBT key "damage"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> LENS_DAMAGE =
            register("lens_damage", Codec.LONG, ByteBufCodecs.VAR_LONG);

    /** ItemWatzPellet enrichment yield (CE NBT key "yield"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Double>> WATZ_YIELD =
            register("watz_yield", Codec.DOUBLE, ByteBufCodecs.DOUBLE);

    /** ItemBlueprints pool selection (CE NBT key "pool"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> BLUEPRINT_POOL =
            register("blueprint_pool", Codec.STRING, ByteBufCodecs.STRING_UTF8);

    /**
     * ItemTurretBiometry's collected player names (CE NBT keys "playercount" / "player_0", "player_1", ...),
     * joined with {@code \n} - a name may not legally contain that character, so no escaping is needed.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> TURRET_NAMES =
            register("turret_names", Codec.STRING, ByteBufCodecs.STRING_UTF8);

    /** Fluid tank / icon fluid type, by {@code FluidType} id. Shared across ItemFluidTank/V2/Icon. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FLUID_ID =
            register("fluid_id", Codec.INT, ByteBufCodecs.INT);

    /** Fluid tank / icon fill amount in mB (CE NBT key "fill"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FLUID_AMOUNT =
            register("fluid_amount", Codec.INT, ByteBufCodecs.INT);

    /** ItemFluidIcon pressure (CE NBT key "pressure"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FLUID_PRESSURE =
            register("fluid_pressure", Codec.INT, ByteBufCodecs.INT);

    /** ItemICFPellet first fuel selection (CE NBT key "type1"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ICF_TYPE1 =
            register("icf_type1", Codec.INT, ByteBufCodecs.INT);

    /** ItemICFPellet second fuel selection (CE NBT key "type2"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ICF_TYPE2 =
            register("icf_type2", Codec.INT, ByteBufCodecs.INT);

    /** ItemICFPellet muon catalysis flag (CE NBT key "muon"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ICF_MUON =
            register("icf_muon", Codec.BOOL, ByteBufCodecs.BOOL);

    /** ItemCassette track reference, by {@code ItemCassette.TrackType} id. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CASSETTE_TRACK =
            register("cassette_track", Codec.INT, ByteBufCodecs.INT);

    /** ItemMold selected mold, by {@code ItemMold.Mold} id. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MOLD_ID =
            register("mold_id", Codec.INT, ByteBufCodecs.INT);

    /** ItemScraps crucible content amount (CE NBT key "amount"), quanta per {@code MaterialShapes}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SCRAP_AMOUNT =
            register("scrap_amount", Codec.INT, ByteBufCodecs.INT);

    /** ItemScraps molten/foundry-crucible display flag (CE NBT key "liquid"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SCRAP_LIQUID =
            register("scrap_liquid", Codec.BOOL, ByteBufCodecs.BOOL);

    private MachineDataComponents() {}

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
