package com.hbm.api.tile;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ILootContainerModifiable extends RandomizableContainer {
    /**
     * Convenience wiring for CE-style callers that set table and seed together.
     * Delegates to RandomizableContainer's own setLootTable(ResourceKey) and
     * setLootTableSeed(long) so unpackLootTable()'s default logic always sees
     * both values applied together.
     */
    default void setLootTable(@NotNull ResourceKey<LootTable> table, long seed) {
        setLootTable(table);
        setLootTableSeed(seed);
    }

    void fillWithLoot(@Nullable Player player);
}
