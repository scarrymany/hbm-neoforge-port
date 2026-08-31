package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code BlockNetherCoal extends BlockOutgas} ({@code ore_nether_coal}). CE's real
 * constructor, {@code BlockNetherCoal(false, 5, true, "ore_nether_coal")}, configures
 * {@code BlockOutgas}'s {@code randomTick=false}/{@code rate=5}/{@code onBreak=true} fields - i.e.
 * this ore does <b>not</b> randomly emit gas on its own tick, but releases {@code gas_monoxide}
 * (gated {@code GeneralConfig.enableCarbonMonoxide}) when broken.
 * <p>
 * This port's {@link BlockOutgas} is (per its own javadoc) a deliberately inert placeholder with no
 * gas-emission behavior wired up at all yet, even though the gas-block family now exists -
 * retrofitting that shared placeholder for every outgas ore is out of this narrow registration
 * package's scope, so this class only carries over what has no such dependency: CE's own
 * {@code onEntityWalk} (walking across the ore ignites the entity for 3 seconds - it's smoldering
 * down here) via the modern {@link #stepOn} hook. CE's real {@code coal_infernal} drop item does not
 * exist in this port yet either, so this ore falls back to the ordinary self-drop, following the
 * same "no drop item yet -> null IOreType" convention {@code OreBlocks}'s own class javadoc already
 * documents for {@code sulfur}/{@code niter}/etc. CE's cosmetic flame/smoke-corner
 * {@code randomDisplayTick} particle spray is a client-only Phase 5 concern, not ported here.
 */
public class BlockNetherCoal extends BlockOutgas {

    public BlockNetherCoal(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        entity.igniteForSeconds(3);
        super.stepOn(level, pos, state, entity);
    }
}
