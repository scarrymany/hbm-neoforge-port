package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.FloodlightBeamBlockEntity;
import com.hbm.blockentity.machine.FloodlightBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Floodlight blocks and entities registration (lighting machines).
 * Ported from CE's Floodlight.java and FloodlightBeam.java.
 */
public final class LightingBlocks {

    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(2.0F, 10.0F).sound(SoundType.METAL);

    private LightingBlocks() {
    }

    public static void registerAll() {
        ModBlocks.FLOODLIGHT = ModBlocks.BLOCKS.register("floodlight", () -> new Floodlight());
        ModItems.ITEMS.register("floodlight", () -> new BlockItem(ModBlocks.FLOODLIGHT.get(),
                new Item.Properties().stacksTo(64)));

        ModBlocks.FLOODLIGHT_BEAM = ModBlocks.BLOCKS.register("floodlight_beam", FloodlightBeam::new);

        ModBlocks.FLOODLIGHT_ENTITY = ModBlocks.BLOCK_ENTITY_TYPES.register("floodlight", () ->
                BlockEntityType.Builder.of(FloodlightBlockEntity::new, ModBlocks.FLOODLIGHT.get()).build(null));

        ModBlocks.FLOODLIGHT_BEAM_ENTITY = ModBlocks.BLOCK_ENTITY_TYPES.register("floodlight_beam", () ->
                BlockEntityType.Builder.of(FloodlightBeamBlockEntity::new, ModBlocks.FLOODLIGHT_BEAM.get()).build(null));

        CreativeTabContents.add(ModCreativeTabs.MACHINE, () -> ModBlocks.FLOODLIGHT.get());
    }
}
