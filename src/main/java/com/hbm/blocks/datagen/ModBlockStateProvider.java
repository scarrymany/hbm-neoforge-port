package com.hbm.blocks.datagen;

import com.hbm.blocks.ICustomBlockModelRegister;
import com.hbm.blocks.ModBlocks;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
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

    private final ExistingFileHelper files;

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, MainRegistry.MODID, helper);
        this.files = helper;
    }

    @Override
    protected void registerStatesAndModels() {
        ModBlocks.BLOCKS.getEntries().forEach(holder -> {
            Block block = holder.get();
            ResourceLocation loc = Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(block));

            try {
                if (block instanceof ICustomBlockModelRegister custom) {
                    custom.registerModel(this, loc);
                } else if (hasExistingBlockstate(loc)) {
                    // Phase 10 CE-converted blockstates — don't emit cube-all that shadows them.
                } else if (hasBlockTexture(loc)) {
                    this.simpleCubeAllBlock(block);
                }
            } catch (IllegalArgumentException missing) {
                // Phase 10 owns bulk assets; missing textures must not fail datagen.
            }
        });
    }

    private boolean hasBlockTexture(ResourceLocation loc) {
        return files.exists(loc, PackType.CLIENT_RESOURCES, ".png", "textures/block");
    }

    private boolean hasExistingBlockstate(ResourceLocation loc) {
        return files.exists(loc, PackType.CLIENT_RESOURCES, ".json", "blockstates");
    }

    /** Creates the block with its {@code BlockItem}, both using the same cube-all model. */
    private void simpleCubeAllBlock(Block block) {
        this.simpleBlockWithItem(block, this.cubeAll(block));
    }
}
