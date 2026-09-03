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

    private FoundryBlockEntities() {
    }
}
