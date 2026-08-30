package com.hbm.blocks;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;

/**
 * Implemented by a {@link net.minecraft.world.level.block.Block} subclass that needs blockstate/
 * model generation beyond {@link com.hbm.blocks.datagen.ModBlockStateProvider}'s default
 * {@code simpleCubeAllBlock(...)} call (blockstate properties selecting between models, custom
 * model loaders, multipart builders, ...). Confirmed real pattern, ported from the Neo Edition
 * reference's {@code com.hbm.blocks.ICustomBlockModelRegister}.
 */
public interface ICustomBlockModelRegister {

    void registerModel(BlockStateProvider provider, ResourceLocation modelLocation);
}
