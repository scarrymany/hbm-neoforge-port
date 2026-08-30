package com.hbm.items.weapon;

import com.hbm.main.MainRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components for {@code com.hbm.items.weapon}'s missile items. Owned by this package
 * (rather than a shared registry) per this port's established "per-package DataComponents class"
 * convention (see {@code com.hbm.items.machine.MachineDataComponents}, {@code
 * com.hbm.items.armor.ArmorDataComponents}, etc).
 */
public final class MissileDataComponents {

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MainRegistry.MODID);

    /** {@code missile_custom}'s loaded chip/warhead/fuselage/fins/thruster parts - see {@link MissileMultipartComponent}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MissileMultipartComponent>> MISSILE_PARTS =
            DATA_COMPONENT_TYPES.register("missile_parts", () -> DataComponentType.<MissileMultipartComponent>builder()
                    .persistent(MissileMultipartComponent.CODEC)
                    .networkSynchronized(MissileMultipartComponent.STREAM_CODEC)
                    .build());

    private MissileDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus);
    }
}
