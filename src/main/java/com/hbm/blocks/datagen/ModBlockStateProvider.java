package com.hbm.blocks.datagen;

import com.hbm.blocks.ICustomBlockModelRegister;
import com.hbm.blocks.ModBlocks;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.Objects;

/**
 * Generates one blockstate/model (plus the matching {@code BlockItem} model) per entry actually
 * present in {@link ModBlocks#BLOCKS} at datagen time - never a hardcoded id list. Every block
 * defaults to a cube-all model shared between the block and its item, covering the overwhelming
 * majority of Phase 1's "simple blocks" (ores, decorative blocks, plain storage blocks). A block
 * class that needs anything else (blockstate properties, custom model loaders, multipart builders)
 * opts out of the default by implementing {@link ICustomBlockModelRegister} and doing its own
 * registration.
 */
public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, MainRegistry.MODID, helper);
    }

    @Override
    protected void registerStatesAndModels() {
        ModBlocks.BLOCKS.getEntries().forEach(holder -> {
            Block block = holder.get();

            if (block instanceof ICustomBlockModelRegister custom) {
                ResourceLocation loc = Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(block));
                custom.registerModel(this, loc);
            } else {
                this.simpleCubeAllBlock(block);
            }
        });
    }

    /** Creates the block with its {@code BlockItem}, both using the same cube-all model. */
    private void simpleCubeAllBlock(Block block) {
        this.simpleBlockWithItem(block, this.cubeAll(block));
    }
}
