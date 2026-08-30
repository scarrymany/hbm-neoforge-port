package com.hbm.blocks.bomb;

import com.hbm.entity.item.EntityTNTPrimedBase;
import com.hbm.explosion.ExplosionNukeSmall;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Ported from CE's {@code com.hbm.blocks.bomb.BlockFissureBomb} (50 lines, read in full). The
 * explosion itself ({@link ExplosionNukeSmall#explode} with {@code PARAMS_MEDIUM}, already
 * confirmed real in this port's foundation wave) is fully wired; the 5-block-radius
 * bedrock-ore -&gt; volcano/crater conversion half is a documented Phase-4 world-gen forward
 * reference per this task's explicit instruction, since it depends on {@code ModBlocks.
 * ore_bedrock_block}/{@code ore_bedrock_oil}/{@code ore_volcano} and a {@code BiomeGenCraterBase}
 * equivalent, none of which exist in this port yet (per
 * {@code docs/phase3/bomb_blocks_and_detonators.md}'s own Deferred-scope entry for this class).
 */
public class BlockFissureBomb extends BlockTNTBase {

    public BlockFissureBomb(Properties properties) {
        super(properties);
    }

    @Override
    public void explodeEntity(Level level, double x, double y, double z, @Nullable EntityTNTPrimedBase entity) {
        ExplosionNukeSmall.explode(level, x, y, z, ExplosionNukeSmall.PARAMS_MEDIUM);

        // TODO(Phase 4, world-gen): CE additionally sweeps a 5-block-radius cube around the blast
        // converting ModBlocks.ore_bedrock_block -> ore_volcano (with a BiomeGenCraterBase-derived
        // CRATER flag) and ore_bedrock_oil -> Blocks.BEDROCK. None of ore_bedrock_block/
        // ore_bedrock_oil/ore_volcano/BiomeGenCraterBase exist in this port yet - see
        // docs/phase3/bomb_blocks_and_detonators.md's Deferred scope for this exact class.
    }
}
