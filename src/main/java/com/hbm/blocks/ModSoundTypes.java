package com.hbm.blocks;

import com.hbm.lib.HBMSoundHandler;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SoundType;

/**
 * Ported from CE's {@code ModSoundTypes}. Depends on {@code com.hbm.lib.HBMSoundHandler}'s
 * {@code metalBlock} / {@code pipePlaced} {@link net.neoforged.neoforge.registries.DeferredHolder}s.
 * <p>
 * Deliberately exposed as factory methods rather than eager {@code static final} fields: calling
 * {@code .get()} on a {@code DeferredHolder} at class-load time crashes with
 * {@code IllegalStateException} if {@code ModSoundTypes} loads before the {@code SoundEvent}
 * {@code RegisterEvent} has fired (the same eager-holder pitfall this port has hit repeatedly
 * elsewhere). Callers must invoke these from inside their own block-registration {@code Supplier},
 * never store the result in another eager static field.
 */
public class ModSoundTypes {

    public static ModSoundType grate() {
        return ModSoundType.customStep(SoundType.STONE, HBMSoundHandler.metalBlock.get(), 0.5F, 1.0F);
    }

    public static ModSoundType pipe() {
        return ModSoundType.customDig(SoundType.METAL, HBMSoundHandler.pipePlaced.get(), 0.85F, 0.85F)
                .enveloped(RandomSource.create())
                .pitchFunction((in, rand, type) -> {
                    if (type == ModSoundType.SubType.BREAK) in -= 0.15F;
                    return in + rand.nextFloat() * 0.2F;
                });
    }
}
