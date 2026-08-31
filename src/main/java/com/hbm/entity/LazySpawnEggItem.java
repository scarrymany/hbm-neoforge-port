package com.hbm.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Shared lazy spawn-egg — same reason as {@code CreeperVariantEntityTypes.LazySpawnEggItem}:
 * {@code DeferredHolder.get()} is illegal in static/registration-time constructors.
 */
public final class LazySpawnEggItem extends SpawnEggItem {

    private final Supplier<EntityType<?>> realType;

    public LazySpawnEggItem(Supplier<EntityType<?>> realType, int primaryColor, int secondaryColor, Properties properties) {
        super(EntityType.PIG, primaryColor, secondaryColor, properties);
        this.realType = realType;
    }

    @Override
    public EntityType<?> getType(@Nullable ItemStack stack) {
        return this.realType.get();
    }
}
