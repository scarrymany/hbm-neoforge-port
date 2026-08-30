package com.hbm.items.weapon.grenade;

import com.hbm.main.MainRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components for {@code com.hbm.items.weapon.grenade}, per this port's per-package
 * {@code DataComponents} convention (see {@code com.hbm.items.weapon.sedna.GunDataComponents} for
 * the closest sibling precedent this class follows).
 */
public final class GrenadeDataComponents {

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MainRegistry.MODID);

    /** {@code ItemGrenadeUniversal}'s shell/filling/fuze/extra loadout - CE's {@code KEY_SHELL}/{@code KEY_FILLING}/{@code KEY_FUZE}/{@code KEY_EXTRA} NBT ints. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GrenadeLoadout>> LOADOUT =
            DATA_COMPONENT_TYPES.register("grenade_loadout", () -> DataComponentType.<GrenadeLoadout>builder()
                    .persistent(GrenadeLoadout.CODEC)
                    .networkSynchronized(GrenadeLoadout.STREAM_CODEC)
                    .build());

    /**
     * {@code ItemGrenadeUniversal}'s held/equipped tracking flag - CE reused
     * {@code ItemGunBaseNT.getIsEquipped}/{@code setIsEquipped} (a gun-package static helper) for
     * this on the grenade item purely as code reuse, not a real gun/grenade coupling; this port gives
     * the grenade package its own component instead of reaching into
     * {@code com.hbm.items.weapon.sedna.GunDataComponents}.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> EQUIPPED =
            DATA_COMPONENT_TYPES.register("grenade_equipped", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    private GrenadeDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus);
    }
}
