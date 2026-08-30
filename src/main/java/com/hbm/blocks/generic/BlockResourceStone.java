package com.hbm.blocks.generic;

import com.hbm.blocks.BlockEnums.EnumStoneType;
import net.minecraft.world.level.block.Block;

/**
 * Ported from CE's {@code BlockResourceStone}: a generic resource-stone family, one
 * {@link EnumStoneType} per variant.
 *
 * <p>Deliberately not ported: CE's {@code MALACHITE} variant dropped bonus
 * {@code ModItems.chunk_ore} stacks (an {@code EnumChunkType}-keyed, data-component multi-variant
 * item) via {@code OreDictManager.DictFrame}. Neither that item family nor {@code OreDictManager}
 * exist in the port yet, and block drops are loot-table content in modern Minecraft regardless
 * (there is no Java {@code getDrops} hook to hang this on) - the special drop is left for whoever
 * builds the {@code chunk_ore} item and this block's loot table.
 */
public class BlockResourceStone extends Block {

    public final EnumStoneType type;

    public BlockResourceStone(Properties properties, EnumStoneType type) {
        super(properties);
        this.type = type;
    }
}
