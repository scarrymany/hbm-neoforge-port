package com.hbm.items.weapon;

import com.hbm.main.MainRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Data components backing ItemStack state for the {@code items/weapon} melee family
 * ({@link ItemSwordCutter}, {@link ItemCrucible}). Follows the same per-package
 * {@code DataComponents} convention as {@code com.hbm.items.tool.ToolDataComponents} (see that
 * class's javadoc) rather than adding entries there, since that file is concurrently owned by the
 * mining-tool ability framework area.
 */
public final class WeaponDataComponents {

    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MainRegistry.MODID);

    /**
     * {@link ItemSwordCutter}'s "was this the equipped/selected stack last tick" flag - mirrors
     * {@code com.hbm.items.weapon.sedna.GunDataComponents.EQUIPPED}'s exact role and shape, used the
     * same way ({@code ItemGunBaseNT#inventoryTick}'s equip-edge detection), just package-local to
     * this melee family instead of shared with the gun framework.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> EQUIPPED =
            DATA_COMPONENT_TYPES.register("weapon_melee_equipped", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    /**
     * {@link ItemCrucible}'s remaining "full damage" charge count (CE NBT key {@code "charges"}).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CRUCIBLE_CHARGES =
            DATA_COMPONENT_TYPES.register("crucible_charges", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    private WeaponDataComponents() {
    }

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus);
    }
}
