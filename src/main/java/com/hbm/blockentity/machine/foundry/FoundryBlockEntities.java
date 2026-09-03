package com.hbm.blockentity.machine.foundry;

import com.hbm.blocks.machine.foundry.FoundryBlocks;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registry for foundry BlockEntity types.
 */
public class FoundryBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MainRegistry.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoundryTankBlockEntity>> FOUNDRY_TANK_BE_TYPE =
            BLOCK_ENTITY_TYPES.register("foundry_tank", () ->
                    BlockEntityType.Builder.of(FoundryTankBlockEntity::new, FoundryBlocks.FOUNDRY_TANK.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoundryChannelBlockEntity>> FOUNDRY_CHANNEL_BE_TYPE =
            BLOCK_ENTITY_TYPES.register("foundry_channel", () ->
                    BlockEntityType.Builder.of(FoundryChannelBlockEntity::new, FoundryBlocks.FOUNDRY_CHANNEL.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoundryOutletBlockEntity>> FOUNDRY_OUTLET_BE_TYPE =
            BLOCK_ENTITY_TYPES.register("foundry_outlet", () ->
                    BlockEntityType.Builder.of(FoundryOutletBlockEntity::new, FoundryBlocks.FOUNDRY_OUTLET.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FoundryBasinBlockEntity>> FOUNDRY_BASIN_BE_TYPE =
            BLOCK_ENTITY_TYPES.register("foundry_basin", () ->
                    BlockEntityType.Builder.of(FoundryBasinBlockEntity::new, FoundryBlocks.FOUNDRY_BASIN.get())
                            .build(null));

    private FoundryBlockEntities() {
    }
}
