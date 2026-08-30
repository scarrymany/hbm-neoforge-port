package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.GunDataComponents;
import com.hbm.main.MainRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * The Data Component replacement for CE's {@code XWeaponModManager.KEY_MOD_LIST_} + config index -
 * a per-{@code GunConfig}-index {@code int[]} of installed mod ids, stored directly on the gun's
 * {@code NBTTagCompound} in CE. Registered here per this port's per-package {@code DataComponents}
 * convention (see {@code GunDataComponents}/{@code GrenadeDataComponents} - one class per package,
 * this package being {@code com.hbm.items.weapon.sedna.mods}), and per
 * {@code docs/phase3/gun_framework.md}'s own "NBT -> Data Component notes" section (which flags
 * {@code KEY_MOD_LIST_N} by name as one of the flat per-config-index NBT keys {@code ItemGunBaseNT}'s
 * Data-Component conversion needs to cover).
 * <p>
 * <b>Element type is {@code List<List<String>>}, not {@code List<List<ResourceLocation>>}.</b> Each
 * installed mod is stored as its {@link IWeaponMod#getId()} {@link net.minecraft.resources.ResourceLocation}
 * rendered via {@code toString()} (round-tripped with {@code ResourceLocation.parse} on read) rather
 * than a {@code ResourceLocation}-valued codec directly - this sidesteps needing to independently
 * confirm {@code ResourceLocation.CODEC}/{@code STREAM_CODEC} exist with those exact names in this
 * Mojang-mapped 1.21.1 tree (this sandbox cannot run gradlew - see this task's structured output for
 * the exact static verification performed), when {@link Codec#STRING}/{@link ByteBufCodecs#STRING_UTF8}
 * are already used for exactly this purpose elsewhere in this port (e.g.
 * {@code MachineDataComponents.BLUEPRINT_POOL}/{@code TURRET_NAMES}), a strictly safer choice for
 * identical on-disk semantics. The outer {@code List} is indexed by {@code GunConfig} index (one
 * mod-id list per config slot, exactly matching CE's {@code KEY_MOD_LIST_ + cfg} naming), reusing
 * {@link GunDataComponents#getIndexed}/{@link GunDataComponents#updateIndexed}'s generic grow-on-demand
 * helpers directly rather than reimplementing that indexing dance a second time.
 */
public final class WeaponModDataComponents {

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MainRegistry.MODID);

    /** One entry per {@code GunConfig} index; each entry is the list of installed mod ids (as {@code ResourceLocation.toString()}) for that config slot - CE's {@code KEY_MOD_LIST_N}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<List<String>>>> MOD_LISTS =
            DATA_COMPONENT_TYPES.register("weapon_mod_lists", () -> DataComponentType.<List<List<String>>>builder()
                    .persistent(Codec.STRING.listOf().listOf())
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()))
                    .build());

    private WeaponModDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus);
    }
}
