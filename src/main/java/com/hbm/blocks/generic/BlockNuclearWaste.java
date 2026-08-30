package com.hbm.blocks.generic;

/**
 * Ported from CE's {@code BlockNuclearWaste extends BlockHazard} ({@code block_waste},
 * {@code block_waste_painted}, {@code block_waste_vitrified}): on top of {@link BlockHazard}'s
 * contact radiation, CE randomly placed a dense-radon gas block in an adjacent air space on tick.
 * That behavior depends on the gas-block family, which is not ported yet (see {@link BlockOutgas}'s
 * javadoc for the same dependency) - this class currently adds nothing beyond {@link BlockHazard},
 * kept as its own type so the gas-spread behavior has a home once the gas system lands.
 */
public class BlockNuclearWaste extends BlockHazard {

    public BlockNuclearWaste(Properties properties) {
        super(properties);
    }
}
