package com.hbm.items.weapon.sedna;

import com.hbm.main.MainRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Data components backing {@link ItemGunBaseNT}'s runtime state, replacing CE's flat per-index NBT
 * keys under the {@code O_GUNCONFIG_}/mag-state key families (see {@code docs/phase3/
 * gun_framework.md}'s "NBT -> Data Component notes" section, read in full - this class is the
 * concrete implementation of what that section recommends). Registered here, per this port's
 * per-package {@code DataComponents} convention (see {@code com.hbm.items.HbmDataComponents}/
 * {@code com.hbm.items.tool.ToolDataComponents}/{@code com.hbm.items.machine.MachineDataComponents}/
 * {@code com.hbm.items.armor.ArmorDataComponents}), not a shared central registry.
 * <p>
 * {@link #GUN_STATES} and {@link #MAG_STATES} are both variable-length lists rather than a fixed set
 * of scalar components, because CE's own per-index key count is determined at content-definition
 * time by however many {@code GunConfig}s a gun declares (almost always 1, but the framework
 * genuinely supports more) and however many distinct magazine indices a gun's receivers use - a
 * fixed component-per-index scheme would require inventing a component for every index a gun could
 * ever plausibly have. {@link #getIndexed}/{@link #updateIndexed} are the shared grow-on-demand list
 * helpers both {@link ItemGunBaseNT} (for {@link #GUN_STATES}) and {@code com.hbm.items.weapon.sedna.mags}
 * (for {@link #MAG_STATES}) read/write through, so there is exactly one place that knows how to widen
 * one of these lists to cover a newly-touched index.
 * <p>
 * The remaining, non-indexed global flags (CE's {@code drawn}/{@code aiming}/{@code lockontarget}/
 * {@code lockedon}/{@code cancel}/{@code eqipped} keys, none of which are per-config-index) are
 * ordinary individual scalar components, matching {@code MachineDataComponents}'s established
 * "many small components" style for this shape of state rather than bundling them into one record -
 * the task's own Data Component instruction only prescribes a record shape for the per-config-index
 * and per-mag-index state above.
 */
public final class GunDataComponents {

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MainRegistry.MODID);

    /** One entry per {@code configs_DNA} index - CE's {@code state_N}/{@code timer_N}/{@code mode_N}/{@code wear_N}/{@code mouseN_N}/{@code lastanim_N}/{@code animtimer_N}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<GunStateComponent>>> GUN_STATES =
            DATA_COMPONENT_TYPES.register("gun_states", () -> DataComponentType.<List<GunStateComponent>>builder()
                    .persistent(GunStateComponent.CODEC.listOf())
                    .networkSynchronized(GunStateComponent.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build());

    /** One entry per magazine index (a mag's own {@code index} field, not the config index) - CE's {@code magcount_N}/{@code magtype_N}/{@code magprev_N}/{@code magafter_N}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<MagState>>> MAG_STATES =
            DATA_COMPONENT_TYPES.register("mag_states", () -> DataComponentType.<List<MagState>>builder()
                    .persistent(MagState.CODEC.listOf())
                    .networkSynchronized(MagState.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build());

    /** CE {@code KEY_DRAWN}. Defined for parity; not read by any Package B logic (CE itself never calls its own getter either - see {@code ItemGunBaseNT}'s javadoc note). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> DRAWN = bool("gun_drawn");
    /** CE {@code KEY_AIMING}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> AIMING = bool("gun_aiming");
    /** CE {@code KEY_LOCKONTARGET} - a target entity id, or absent/0 for none. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LOCKON_TARGET =
            DATA_COMPONENT_TYPES.register("gun_lockon_target", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());
    /** CE {@code KEY_LOCKEDON}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LOCKED_ON = bool("gun_locked_on");
    /** CE {@code ItemGunStinger.KEY_LOCKINGON}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> LOCKING_ON = bool("gun_locking_on");
    /** CE {@code ItemGunStinger.KEY_LOCKONPROGRESS}. Absent ≡ {@code 0} like {@code getValueInt}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LOCKON_PROGRESS = integer("gun_lockon_progress");
    /** CE {@code KEY_CANCELRELOAD}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> RELOAD_CANCEL = bool("gun_reload_cancel");
    /** CE {@code KEY_EQUIPPED} (CE's own key literally misspells "equipped" as {@code eqipped"} - not preserved here, this is a component name not a wire-compatible NBT key). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> EQUIPPED = bool("gun_equipped");
    /** CE {@code ItemGunNI4NI.KEY_COIN_COUNT}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COIN_COUNT = integer("gun_coin_count");
    /** CE {@code ItemGunNI4NI.KEY_COIN_CHARGE}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> COIN_CHARGE = integer("gun_coin_charge");
    /** CE {@code ItemGunChargeThrower.KEY_LASTHOOK} ({@code "lasthook"}). Absent ≡ {@code 0} like {@code getValueInt}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LAST_HOOK = integer("gun_last_hook");

    private static DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> bool(String name) {
        return DATA_COMPONENT_TYPES.register(name, () -> DataComponentType.<Boolean>builder()
                .persistent(Codec.BOOL)
                .networkSynchronized(ByteBufCodecs.BOOL)
                .build());
    }

    private static DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> integer(String name) {
        return DATA_COMPONENT_TYPES.register(name, () -> DataComponentType.<Integer>builder()
                .persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.INT)
                .build());
    }

    private GunDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus);
    }

    /** Reads index {@code index} of a {@code List<T>}-valued component, or {@code fallback} if the component/index is absent - the read half of the grow-on-demand list convention described in this class's javadoc. */
    public static <T> T getIndexed(ItemStack stack, DeferredHolder<DataComponentType<?>, DataComponentType<List<T>>> type, int index, T fallback) {
        List<T> list = stack.get(type.get());
        if (list == null || index < 0 || index >= list.size()) return fallback;
        T value = list.get(index);
        return value != null ? value : fallback;
    }

    /**
     * Widens a {@code List<T>}-valued component to cover {@code index} (padding new slots with
     * {@code fallback}, exactly as CE's flat NBT keys implicitly "pad" with an all-zero default for
     * an index nothing has written yet) and applies {@code mutator} to that one index, replacing the
     * whole list in one {@code stack.set} call - Data Components have no partial-field mutation, so
     * every setter in this package funnels through here rather than reimplementing this dance
     * per-field.
     */
    public static <T> void updateIndexed(ItemStack stack, DeferredHolder<DataComponentType<?>, DataComponentType<List<T>>> type, int index, T fallback, UnaryOperator<T> mutator) {
        if (index < 0) return;
        List<T> current = stack.getOrDefault(type.get(), List.of());
        int size = Math.max(current.size(), index + 1);
        List<T> updated = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            updated.add(i < current.size() ? current.get(i) : fallback);
        }
        updated.set(index, mutator.apply(updated.get(index)));
        stack.set(type.get(), List.copyOf(updated));
    }
}
