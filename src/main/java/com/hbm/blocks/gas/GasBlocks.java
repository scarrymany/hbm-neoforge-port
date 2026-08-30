package com.hbm.blocks.gas;

import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Table-driven registration for this area's two concrete gas blocks (upstream hbm-ce
 * {@code com.hbm.blocks.gas.BlockGasFlammable}/{@code BlockGasExplosive}), following
 * {@link com.hbm.blocks.OreBlocks}'s exact registration-helper pattern. {@link BlockGasBase} itself
 * is abstract and has no registry entry.
 * <p>
 * The remaining 7 of the 10 CE {@code blocks/gas} classes ({@code BlockGasAsbestos},
 * {@code BlockGasCoal}, {@code BlockGasMonoxide}, {@code BlockGasRadon}/{@code BlockGasRadonDense}/
 * {@code BlockGasRadonTomb}, {@code BlockGasMeltdown}) are deferred - see
 * {@code docs/phase1/DIGEST_REMAINDER.md} ("items_block_fluid_gas.md"): each needs
 * {@code ContaminationUtil}/{@code ArmorUtil}/{@code HbmPotion}, none of which exist in this port yet.
 */
public final class GasBlocks {

    private GasBlocks() {
    }

    public static void registerAll() {
        registerBlock("gas_flammable", () -> new BlockGasFlammable(gasProps()));
        registerBlock("gas_explosive", () -> new BlockGasExplosive(gasProps()));
    }

    private static BlockBehaviour.Properties gasProps() {
        return BlockBehaviour.Properties.of();
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.BLOCKS, block);
        return block;
    }
}
