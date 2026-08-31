package com.hbm.blocks.generic;

import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.handler.radiation.ChunkRadiationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code blocks/generic/YellowBarrel.java}.
 * <p>
 * {@code toxic_block} (1-in-3 explode replacement) is a fluid block not registered in this port —
 * that branch falls through to the 18.0F blast so the barrel never silently vanishes.
 * {@code ChunkRadiationManager} tick + detonation rad match CE (5/75 idle, 35/1500 on explode).
 */
public class YellowBarrel extends BaseBarrel {

    public YellowBarrel(Properties properties) {
        super(properties);
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (level.isClientSide() || !(level instanceof ServerLevel server)) return;
        server.getServer().execute(() -> explode(level, pos.getX(), pos.getY(), pos.getZ()));
    }

    public void explode(Level level, int x, int y, int z) {
        // CE: 1-in-3 places toxic_block; else createExplosion(..., 18.0F, smoking=true)
        if (level.getRandom().nextInt(3) != 0) {
            level.explode(null, x, y, z, 18.0F, false, Level.ExplosionInteraction.TNT);
        }
        ExplosionNukeGeneric.waste(level, x, y, z, 35);
        ChunkRadiationManager.proxy.incrementRad(level, new BlockPos(x, y, z), 35F, 1500F);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // CE yellow_barrel: incrementRad(world, pos, 5F, 75F) every tickRate=20
        ChunkRadiationManager.proxy.incrementRad(level, pos, 5F, 75F);
        level.scheduleTick(pos, this, 20);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // CE TOWN_AURA → 1.21 MYCELIUM
        level.addParticle(ParticleTypes.MYCELIUM,
                pos.getX() + random.nextFloat() * 0.5F + 0.25F,
                pos.getY() + 1.1F,
                pos.getZ() + random.nextFloat() * 0.5F + 0.25F,
                0.0D, 0.0D, 0.0D);
    }
}
