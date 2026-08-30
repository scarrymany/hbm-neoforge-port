package com.hbm.blocks.generic;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.ArrayList;
import java.util.List;

/**
 * Jungle-biome loot crate, ported from CE's {@code BlockJungleCrate}. CE also drops
 * {@code powder_gold}, {@code wire_fine} (gold) and {@code crystal_gold}; this area could not
 * confirm the exact registry field names those three material-shape items land on in this port
 * (owned by the material-generation area, several different generator files), so per the "never
 * guess a field name" ground rule only the two vanilla gold drops are carried over here. Whoever
 * finishes the gold material-shape registration should extend {@link #getDrops} with the other
 * three once their field names are confirmed.
 */
public class BlockJungleCrate extends Block {

    public BlockJungleCrate(Properties properties) {
        super(properties);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        RandomSource random = params.getLevel().getRandom();
        List<ItemStack> drops = new ArrayList<>();

        drops.add(new ItemStack(Items.GOLD_INGOT, 4 + random.nextInt(4)));
        drops.add(new ItemStack(Items.GOLD_NUGGET, 8 + random.nextInt(10)));

        return drops;
    }
}
