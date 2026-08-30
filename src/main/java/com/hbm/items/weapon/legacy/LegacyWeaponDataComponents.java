package com.hbm.items.weapon.legacy;

import com.hbm.main.MainRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components for the legacy (pre-Sedna) charge-weapon family ({@code gun_b92}/{@code gun_b93}/
 * {@code gun_b92_ammo}), per this port's per-package {@code DataComponents} convention (see
 * {@code com.hbm.items.weapon.sedna.GunDataComponents}/{@code com.hbm.items.weapon.grenade
 * .GrenadeDataComponents} as templates). Replaces CE's flat {@code animation}/{@code energy}
 * {@code NBTTagCompound} ints on {@code GunB92}/{@code GunB93}/{@code GunB92Cell} (all 3 read/write
 * the exact same two key names in CE, confirmed by reading {@code GunB92.java} in full).
 */
public final class LegacyWeaponDataComponents {

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MainRegistry.MODID);

    /** CE {@code "animation"} - the charge-up/fire animation-cycle counter. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ANIMATION = intComponent("legacy_gun_animation");
    /** CE {@code "energy"} - accumulated charge count (0-10 on the gun itself, 0-25 on a cell). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY = intComponent("legacy_gun_energy");

    private LegacyWeaponDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus);
    }

    private static DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> intComponent(String name) {
        return DATA_COMPONENT_TYPES.register(name, () -> DataComponentType.<Integer>builder()
                .persistent(Codec.INT)
                .networkSynchronized(ByteBufCodecs.INT)
                .build());
    }
}
