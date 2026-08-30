package com.hbm.blocks.generic;

import com.hbm.blocks.BlockEnums.EnumBasaltOreType;
import net.minecraft.world.level.block.Block;

/**
 * Ported from CE's {@code BlockOreBasalt}: a basalt-hosted decorative ore-type family. CE modelled
 * this as a single {@code BlockEnumMeta<EnumBasaltOreType>} block distinguished by a
 * {@code PropertyInteger} metadata value; Minecraft has had no block metadata since 1.13, so each
 * {@link EnumBasaltOreType} variant is its own registered block (see {@link OreMineralBlocks}).
 *
 * <p>Deliberately not ported: CE's {@code onEntityWalk} spawned {@code ModBlocks.gas_asbestos}
 * above the {@code ASBESTOS} variant on a 1-in-10 tick. The gas-block/fluid system that block
 * belongs to has not been ported yet, so that ambient effect is skipped rather than faked; the
 * block's physical properties (hardness, resistance, sound) are otherwise faithful to CE.
 *
 * <p>CE's per-variant drop ({@link EnumBasaltOreType#getDrop()} /
 * {@link EnumBasaltOreType#getDropCount(int)}) is loot-table content in modern Minecraft (blocks no
 * longer have a Java-side {@code getDrops} hook); wire it into this block's loot table once the
 * datagen loot provider exists.
 */
public class BlockOreBasalt extends Block {

    public final EnumBasaltOreType type;

    public BlockOreBasalt(Properties properties, EnumBasaltOreType type) {
        super(properties);
        this.type = type;
    }
}
