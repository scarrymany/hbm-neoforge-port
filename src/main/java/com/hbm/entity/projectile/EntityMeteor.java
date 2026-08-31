package com.hbm.entity.projectile;

import com.hbm.blocks.generic.PlantBlocks;
import com.hbm.config.WorldConfig;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.world.MeteoriteGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.entity.projectile.EntityMeteor} (225 lines, read in full) - see
 * {@code docs/phase4/meteor_events.md}. Falls from {@code y=384} toward a target player
 * ({@link com.hbm.handler.MeteorStrikeHandler#spawnMeteorAtPlayer}), clearing a 5-block-radius path
 * of weak/flammable blocks as it descends, then on ground contact fires a vanilla explosion, an
 * optional cosmetic rubble/particle burst, and the real impact via
 * {@link com.hbm.world.MeteoriteGenerator#generate} (with {@code allowSpecials=true,
 * damagingImpact=true} - richer than the sibling ambient {@code MeteoriteFeature} world-gen call,
 * which always passes {@code false, false, false} - per that report's headline finding #1). The
 * shared generator itself is owned by the sibling {@code docs/phase4/
 * worldgen_oil_and_meteor_dungeons.md} package, not this one; it landed mid-task and is called
 * directly here rather than duplicated.
 * <p>
 * <b>API-shape translations</b> (CE 1.12.2 -&gt; NeoForge 1.21.1, confirmed via this port's own
 * already-compiling {@code EntityFallingNuke}): {@code onUpdate()} -&gt; {@link #tick()};
 * {@code setDead()} -&gt; {@link #discard()}; {@code readEntityFromNBT}/{@code writeEntityToNBT} -&gt;
 * {@link #readAdditionalSaveData}/{@link #addAdditionalSaveData}; empty {@code entityInit()} -&gt;
 * empty {@link #defineSynchedData} (CE persists {@code safe} via NBT only, no synced field, and
 * neither does this port). {@code world.createExplosion(entity, x, y, z, power, doesBlockDamage)}
 * -&gt; {@code level.explode(entity, x, y, z, power, interaction)} (the 6-arg, no-fire-flag overload
 * already in live use elsewhere in this port, e.g. {@code IToolAreaAbility}), translating CE's
 * {@code !safe} block-damage flag to {@code Level.ExplosionInteraction.BLOCK}/{@code NONE}.
 * <p>
 * <b>CE's single generic {@code Blocks.LEAVES}/{@code Blocks.LOG} checks</b> (in
 * {@link #damageOrDestroyBlock}) become {@code state.is(BlockTags.LEAVES)}/{@code is(BlockTags.LOGS)}
 * - a direct consequence of Minecraft's own 1.13+ block-ID flattening (one generic leaves/log block
 * split into many per-wood-type blocks unified only by a tag), not a CE behavior change. This port's
 * own {@code FalloutConfigJSON}/{@code RadiationWorldHandler}/{@code PollutionHandler} already make
 * the identical translation for the same reason.
 * <p>
 * <b>Looped falling sound</b>: CE tracks a continuous, position-following {@code AudioWrapper} loop
 * via {@code MainRegistry.proxy.getLoopedSound(...)} on the client. That looped-sound factory has no
 * confirmed 1.21.1 equivalent anywhere in this port yet ({@code LaunchPadLargeBlockEntity}/
 * {@code MachineRefineryBlockEntity} both document the same gap as an open Phase 5 audio-infra
 * dependency). Rather than leave the fall silent, this port substitutes a real, audible, periodic
 * server-broadcast {@code level.playSound(null, ...)} every 10 ticks while falling - not a true
 * continuous loop, but a genuine, functioning stand-in pending Phase 5's real looped-audio port (see
 * this package's knownGaps).
 */
public class EntityMeteor extends Entity {

    /**
     * CE: {@code EntityMeteor.safe}. When {@code true} (the "repel" branch of
     * {@code protection_charm}), path-clearing/block-destruction is suppressed and the ground-impact
     * explosion does no block damage - but the 1000-damage {@code MeteoriteGenerator.generate} AoE still fires
     * unconditionally at the (redirected) landing spot, exactly as CE's real
     * {@code EntityMeteor.java:142} passes a literal {@code true} for {@code damagingImpact}
     * regardless of {@code safe}. Not silently "fixed" - see {@code docs/phase4/meteor_events.md}'s
     * Open questions.
     */
    public boolean safe = false;

    public EntityMeteor(EntityType<? extends EntityMeteor> type, Level level) {
        super(type, level);
    }

    public EntityMeteor(Level level) {
        this(MeteorEntityTypes.METEOR.get(), level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // CE's entityInit() is empty - safe is NBT-only, no synced field needed (see class javadoc).
    }

    private List<BlockPos> getBlocksInRadius(int x, int y, int z, int radius) {
        List<BlockPos> found = new ArrayList<>();
        int rSq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= rSq) {
                        found.add(new BlockPos(x + dx, y + dy, z + dz));
                    }
                }
            }
        }
        return found;
    }

    public void damageOrDestroyBlock(Level level, int blockX, int blockY, int blockZ) {
        if (safe) return;

        BlockPos pos = new BlockPos(blockX, blockY, blockZ);
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (state.isAir()) return;

        float hardness = state.getDestroySpeed(level, pos);

        // CE: block == Blocks.LEAVES || block == Blocks.LOG - see class javadoc for the tag translation.
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || (hardness >= 0 && hardness <= 0.3F)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return;
        }

        if (hardness < 0 || hardness > 5F) return;

        if (random.nextInt(6) == 1) {
            if (block == Blocks.DIRT) {
                level.setBlock(pos, PlantBlocks.DIRT_DEAD.get().defaultBlockState(), 3);
            } else if (block == Blocks.SAND) {
                if (random.nextInt(2) == 1) {
                    level.setBlock(pos, Blocks.SANDSTONE.defaultBlockState(), 3);
                } else {
                    level.setBlock(pos, Blocks.GLASS.defaultBlockState(), 3);
                }
            } else if (block == Blocks.STONE) {
                level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
            } else if (block == Blocks.GRASS_BLOCK) {
                level.setBlock(pos, PlantBlocks.WASTE_EARTH.get().defaultBlockState(), 3);
            }
        }
    }

    private void clearMeteorPath(Level level, int x, int y, int z) {
        for (BlockPos bp : getBlocksInRadius(x, y, z, 5)) {
            damageOrDestroyBlock(level, bp.getX(), bp.getY(), bp.getZ());
        }
    }

    @Override
    public void tick() {
        Level level = level();

        if (!level.isClientSide() && !WorldConfig.ENABLE_METEOR_STRIKES.get()) {
            this.discard();
            return;
        }

        double motionY = getDeltaMovement().y - 0.03D;
        if (motionY < -2.5D) motionY = -2.5D;
        Vec3 motion = new Vec3(getDeltaMovement().x, motionY, getDeltaMovement().z);
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);

        if (!level.isClientSide() && getY() < 260) {
            clearMeteorPath(level, (int) getX(), (int) getY(), (int) getZ());

            if (onGround()) {
                float power = 5F + random.nextFloat();
                level.explode(this, getX(), getY(), getZ(), power,
                        safe ? Level.ExplosionInteraction.NONE : Level.ExplosionInteraction.BLOCK);

                if (WorldConfig.ENABLE_METEOR_TAILS.get()) {
                    ExplosionLarge.spawnRubble(level, getX(), getY(), getZ(), 15);
                    ExplosionLarge.spawnParticles(level, getX(), getY() + 5, getZ(), 75);
                    ExplosionLarge.spawnParticles(level, getX() + 5, getY(), getZ(), 75);
                    ExplosionLarge.spawnParticles(level, getX() - 5, getY(), getZ(), 75);
                    ExplosionLarge.spawnParticles(level, getX(), getY(), getZ() + 5, 75);
                    ExplosionLarge.spawnParticles(level, getX(), getY(), getZ() - 5, 75);
                }

                int spawnPosX = (int) (Math.round(getX() - 0.5D) + (safe ? 0 : (motion.z * 4)));
                int spawnPosY = (int) Math.round(getY() - (safe ? 0 : 4));
                int spawnPosZ = (int) (Math.round(getZ() - 0.5D) + (safe ? 0 : (motion.z * 4)));

                MeteoriteGenerator.generate(level, random, spawnPosX, spawnPosY, spawnPosZ, safe, true, true);
                clearMeteorPath(level, spawnPosX, spawnPosY, spawnPosZ);

                level.playSound(null, getX(), getY(), getZ(), HBMSoundHandler.oldExplosion.get(),
                        SoundSource.HOSTILE, 10000.0F, 0.5F + random.nextFloat() * 0.1F);

                this.discard();
                return;
            }
        }

        // Periodic falling-sound broadcast - see class javadoc's "Looped falling sound" note. CE
        // gates only the cosmetic Exhaust_Meteor particle trail (Phase 5, not ported) on
        // ENABLE_METEOR_TAILS; the falling sound itself is unconditional, so this stand-in is too.
        if (!level.isClientSide() && tickCount % 10 == 0) {
            level.playSound(null, getX(), getY(), getZ(), HBMSoundHandler.meteoriteFallingLoop.get(),
                    SoundSource.HOSTILE, 1.0F, 0.9F + random.nextFloat() * 0.2F);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.safe = tag.getBoolean("safe");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("safe", safe);
    }
}
