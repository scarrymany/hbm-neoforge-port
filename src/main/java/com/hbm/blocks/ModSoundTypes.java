package com.hbm.blocks;

import com.hbm.lib.HBMSoundHandler;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SoundType;

/**
 * Ported from CE's {@code ModSoundTypes}. Depends on {@code com.hbm.lib.HBMSoundHandler} (its
 * {@code metalBlock} / {@code pipePlaced} sound events), which does not exist yet in this port tree;
 * this class will not compile until that class lands.
 */
public class ModSoundTypes {

    public static final ModSoundType grate = ModSoundType.customStep(SoundType.STONE, HBMSoundHandler.metalBlock, 0.5F, 1.0F);

    public static final ModSoundType pipe = ModSoundType.customDig(SoundType.METAL, HBMSoundHandler.pipePlaced, 0.85F, 0.85F)
            .enveloped(RandomSource.create())
            .pitchFunction((in, rand, type) -> {
                if (type == ModSoundType.SubType.BREAK) in -= 0.15F;
                return in + rand.nextFloat() * 0.2F;
            });
}
