package com.hbm.entity.logic;

import com.hbm.config.BombConfig;
import com.hbm.config.GeneralConfig;
import com.hbm.entity.effect.EntityFalloutRain;
import com.hbm.explosion.ExplosionDrying;
import com.hbm.explosion.ExplosionFleija;
import com.hbm.explosion.ExplosionNukeAdvanced;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.explosion.ExplosionSolinium;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import com.hbm.particle.HbmEffect;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Ported from CE's {@code com.hbm.entity.logic.EntityNukeExplosionMK3} (402 lines, read in full) -
 * the older column-carving nuke-tier driver used by {@code NukeFleija}/{@code NukePrototype}/
 * {@code NukeSolinium}/{@code NukeCustom}'s higher tiers (see {@code docs/phase3/
 * bomb_blocks_and_detonators.md} §B). Drives either a "waste" triple ({@link ExplosionNukeAdvanced}
 * at radii {@code r}/{@code r*1.8}/{@code r*2.5}, modes crater/waste/vapor) or one of 3 {@code
 * extType} alternatives ({@link ExplosionFleija}/{@link ExplosionSolinium}/{@link ExplosionDrying})
 * an accelerating number of times per tick ({@code speed += 1} every tick, CE's own tick-spread
 * optimization for this family - see the research report's "CE's own performance characteristics").
 * <p>
 * <b>Not ported (documented forward references)</b>: {@code AdvancementManager.grantAchievement}
 * (Phase 5). The {@code EntityFalloutRain} spawn on waste-path completion ({@code docs/phase3/
 * explosion_engine.md}'s "Fallout trigger hook") is now wired - see {@code
 * docs/phase4/fallout_rain_and_effects.md}. The anti-nuke
 * "jammer" mechanic ({@link #isJammed}/{@link #statFacFleija}) is ported with its actual gameplay
 * check intact; its particle-burst VFX ({@link #createParticle}) now also broadcasts the real
 * {@code PlasmaBlast} {@link HbmEffect} (matching CE's own r/g/b/scale=7.5 values 1:1,
 * {@code upstream/hbm-ce/.../EntityNukeExplosionMK3.java:250-258,331-336}) - see
 * {@code docs/phase5/particle_engine_and_generic_vfx.md} for the dispatch mechanism this now uses.
 */
public class EntityNukeExplosionMK3 extends EntityExplosionChunkloading {

    public int age = 0;
    public int destructionRange = 0;
    public ExplosionNukeAdvanced exp;
    public ExplosionNukeAdvanced wst;
    public ExplosionNukeAdvanced vap;
    public ExplosionFleija expl;
    public ExplosionSolinium sol;
    public ExplosionDrying dry;
    public int speed = 1;
    public float coefficient = 1;
    public float coefficient2 = 1;
    public boolean did = false;
    public boolean did2 = false;
    public boolean waste = true;
    /** Extended type: 0 = Fleija (antimatter), 1 = Solinium, 2 = Drying. Only consulted when {@link #waste} is false. */
    public int extType = 0;
    public UUID detonator = null;

    public EntityNukeExplosionMK3(EntityType<? extends EntityNukeExplosionMK3> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        markChunkLoaderRestoredFromNBT();
        age = nbt.getInt("age");
        destructionRange = nbt.getInt("destructionRange");
        speed = nbt.getInt("speed");
        coefficient = nbt.getFloat("coefficient");
        coefficient2 = nbt.getFloat("coefficient2");
        did = nbt.getBoolean("did");
        did2 = nbt.getBoolean("did2");
        waste = nbt.getBoolean("waste");
        extType = nbt.getInt("extType");
        if (nbt.hasUUID("detonator")) detonator = nbt.getUUID("detonator");
        long time = nbt.getLong("milliTime");

        if (BombConfig.LIMIT_EXPLOSION_LIFESPAN.get() > 0
                && System.currentTimeMillis() - time > BombConfig.LIMIT_EXPLOSION_LIFESPAN.get() * 1000L) {
            this.discard();
        }

        Level level = level();
        if (this.waste) {
            exp = new ExplosionNukeAdvanced((int) getX(), (int) getY(), (int) getZ(), level, this.destructionRange, this.coefficient, 0);
            exp.readFromNbt(nbt, "exp_");
            exp.detonator = detonator;
            wst = new ExplosionNukeAdvanced((int) getX(), (int) getY(), (int) getZ(), level, (int) (this.destructionRange * 1.8), this.coefficient, 2);
            wst.readFromNbt(nbt, "wst_");
            wst.detonator = detonator;
            vap = new ExplosionNukeAdvanced((int) getX(), (int) getY(), (int) getZ(), level, (int) (this.destructionRange * 2.5), this.coefficient, 1);
            vap.readFromNbt(nbt, "vap_");
            vap.detonator = detonator;
        } else {
            if (extType == 0) {
                expl = new ExplosionFleija((int) getX(), (int) getY(), (int) getZ(), level, this.destructionRange, this.coefficient, this.coefficient2);
                expl.readFromNbt(nbt, "expl_");
                expl.detonator = detonator;
            }
            if (extType == 1) {
                sol = new ExplosionSolinium((int) getX(), (int) getY(), (int) getZ(), level, this.destructionRange, this.coefficient, this.coefficient2);
                sol.readFromNbt(nbt, "sol_");
                sol.detonator = detonator;
            }
            if (extType == 2) {
                dry = new ExplosionDrying((int) getX(), (int) getY(), (int) getZ(), level, this.destructionRange, this.coefficient, this.coefficient2);
                dry.readFromNbt(nbt, "dry_");
                dry.detonator = detonator;
            }
        }

        this.did = true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("age", age);
        nbt.putInt("destructionRange", destructionRange);
        nbt.putInt("speed", speed);
        nbt.putFloat("coefficient", coefficient);
        nbt.putFloat("coefficient2", coefficient2);
        nbt.putBoolean("did", did);
        nbt.putBoolean("did2", did2);
        nbt.putBoolean("waste", waste);
        nbt.putInt("extType", extType);

        nbt.putLong("milliTime", System.currentTimeMillis());
        if (detonator != null) nbt.putUUID("detonator", detonator);
        if (exp != null) exp.saveToNbt(nbt, "exp_");
        if (wst != null) wst.saveToNbt(nbt, "wst_");
        if (vap != null) vap.saveToNbt(nbt, "vap_");
        if (expl != null) expl.saveToNbt(nbt, "expl_");
        if (sol != null) sol.saveToNbt(nbt, "sol_");
        if (dry != null) dry.saveToNbt(nbt, "dry_");
    }

    @Override
    public void tick() {
        super.tick();
        Level level = level();
        if (level.isClientSide()) return;

        loadChunk(chunkPosition().x, chunkPosition().z);

        if (!this.did) {
            // TODO(AdvancementManager, Phase 5): CE grants achManhattan to every player in the level here.

            if (GeneralConfig.ENABLE_EXTENDED_LOGGING.get()) {
                MainRegistry.logger.info("[NUKE] Initialized mk3 explosion at {} / {} / {} with strength {}!", getX(), getY(), getZ(), destructionRange);
            }

            if (this.waste) {
                exp = new ExplosionNukeAdvanced((int) getX(), (int) getY(), (int) getZ(), level, this.destructionRange, this.coefficient, 0);
                wst = new ExplosionNukeAdvanced((int) getX(), (int) getY(), (int) getZ(), level, (int) (this.destructionRange * 1.8), this.coefficient, 2);
                vap = new ExplosionNukeAdvanced((int) getX(), (int) getY(), (int) getZ(), level, (int) (this.destructionRange * 2.5), this.coefficient, 1);
                exp.detonator = detonator;
                wst.detonator = detonator;
                vap.detonator = detonator;
            } else {
                if (extType == 0) {
                    expl = new ExplosionFleija((int) getX(), (int) getY(), (int) getZ(), level, this.destructionRange, this.coefficient, this.coefficient2);
                    expl.detonator = detonator;
                }
                if (extType == 1) {
                    sol = new ExplosionSolinium((int) getX(), (int) getY(), (int) getZ(), level, this.destructionRange, this.coefficient, this.coefficient2);
                    sol.detonator = detonator;
                }
                if (extType == 2) {
                    dry = new ExplosionDrying((int) getX(), (int) getY(), (int) getZ(), level, this.destructionRange, this.coefficient, this.coefficient2);
                    dry.detonator = detonator;
                }
            }

            // TODO(SatelliteDetector, Phase 4): CE reports a HIGH-intensity/HIGH-duration burst event here.
            this.did = true;
        }

        speed += 1; // increase speed to keep up with expansion

        boolean flag = false;
        boolean flag3;

        for (int i = 0; i < this.speed; i++) {
            if (waste) {
                flag = exp.update();
                if (wst != null) {
                    wst.update();
                }
                flag3 = vap.update();

                if (flag3) {
                    this.discard();
                }
            } else {
                if (extType == 0 && expl.update()) this.discard();
                if (extType == 1 && sol.update()) this.discard();
                if (extType == 2 && dry.update()) this.discard();
            }
        }

        if (!flag) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.AMBIENT,
                    10000.0F, 0.8F + this.random.nextFloat() * 0.2F);
            if (waste || extType != 1) {
                double r = this.destructionRange * 2.0D;
                List<Entity> list = level.getEntitiesOfClass(Entity.class,
                        new AABB(getX() - r, getY() - r, getZ() - r, getX() + r, getY() + r, getZ() + r));
                ExplosionNukeGeneric.dealDamage(level, list, getX(), getY(), getZ(), r);
            } else {
                // TODO(ExplosionHurtUtil.doRadiation, ContaminationUtil): CE applies a radial
                // radiation dose here for the Solinium extType instead of blast damage; this
                // pass's scope (docs/phase3/explosion_engine.md) explicitly ports
                // ContaminationUtil.contaminate but ExplosionHurtUtil itself was out of this pass's
                // read set - left as a documented forward reference.
            }
        } else {
            if (!did2 && waste) {
                // CE: EntityFalloutRain(world, (int)(destructionRange*1.8)*10) - the int arg is
                // silently discarded by CE's own 2-arg constructor (see EntityFalloutRain's own
                // javadoc); detonator IS propagated here (unlike MK5's fallout spawn - a real,
                // faithfully-preserved CE asymmetry, see docs/phase4/fallout_rain_and_effects.md's
                // Key design/API decisions).
                EntityFalloutRain rain = new EntityFalloutRain(level, (int) (destructionRange * 1.8) * 10);
                rain.setPos(getX(), getY(), getZ());
                rain.detonator = this.detonator;
                rain.setScale((int) (destructionRange * 1.8));
                level.addFreshEntity(rain);
                did2 = true;
            }
        }
        age++;
    }

    public static final HashMap<ATEntry, Long> at = new HashMap<>();

    private static void createParticle(Level level, ResourceKey<Level> dim, double x, double y, double z, float r, float g, float b) {
        level.playSound(null, x + 0.5D, y + 0.5D, z + 0.5D, HBMSoundHandler.ufoBlast, SoundSource.HOSTILE, 15.0F, 1.0F);

        CompoundTag data = new CompoundTag();
        data.putFloat("r", r);
        data.putFloat("g", g);
        data.putFloat("b", b);
        data.putFloat("scale", 7.5F);
        HbmEffect.sendPacket(level, HbmEffect.PLASMA_BLAST, x + 0.5D, y + 0.5D, z + 0.5D, 150, data);
    }

    public static boolean isJammed(Level level, Entity entity) {
        Iterator<Entry<ATEntry, Long>> it = at.entrySet().iterator();

        while (it.hasNext()) { // checking each jammer if it is in range
            Entry<ATEntry, Long> next = it.next();
            if (next.getValue() < level.getGameTime()) {
                it.remove();
                continue;
            }

            ATEntry jammer = next.getKey();
            if (!jammer.dim.equals(level.dimension())) continue;

            double distance = Math.sqrt(Math.pow(entity.getX() - jammer.x, 2) + Math.pow(entity.getY() - jammer.y, 2) + Math.pow(entity.getZ() - jammer.z, 2));

            if (distance < 300) {
                if (!level.isClientSide()) {
                    createParticle(level, jammer.dim, entity.getX(), entity.getY(), entity.getZ(), 1.0F, 0.5F, 0.0F);
                    createParticle(level, jammer.dim, jammer.x, jammer.y, jammer.z, 0.0F, 0.75F, 1.0F);
                }
                entity.discard();
                return true;
            }
        }
        return false;
    }

    public static EntityNukeExplosionMK3 statFacFleija(Level level, double x, double y, double z, int range) {
        EntityNukeExplosionMK3 entity = new EntityNukeExplosionMK3(NukeEntityTypes.NUKE_MK3.get(), level);
        entity.setPos(x, y, z);
        entity.destructionRange = range;
        entity.speed = BombConfig.BLAST_SPEED.get();
        entity.coefficient = 1.0F;
        entity.waste = false;

        Iterator<Entry<ATEntry, Long>> it = at.entrySet().iterator();

        while (it.hasNext()) {
            Entry<ATEntry, Long> next = it.next();
            if (next.getValue() < level.getGameTime()) {
                it.remove();
                continue;
            }

            ATEntry entry = next.getKey();
            if (!entry.dim.equals(level.dimension())) continue;

            Vec3 vec = new Vec3(x - entry.x, y - entry.y, z - entry.z);

            if (vec.length() < 300) {
                entity.discard();

                if (!level.isClientSide()) {
                    for (int i = 0; i < 2; i++) {
                        double ix = i == 0 ? x : (entry.x + 0.5);
                        double iy = i == 0 ? y : (entry.y + 0.5);
                        double iz = i == 0 ? z : (entry.z + 0.5);

                        level.playSound(null, ix, iy, iz, HBMSoundHandler.ufoBlast, SoundSource.PLAYERS, 15.0F, 0.7F + level.getRandom().nextFloat() * 0.2F);

                        CompoundTag data = new CompoundTag();
                        data.putFloat("r", 0.0F);
                        data.putFloat("g", 0.75F);
                        data.putFloat("b", 1.0F);
                        data.putFloat("scale", 7.5F);
                        HbmEffect.sendPacket(level, HbmEffect.PLASMA_BLAST, ix, iy, iz, 150, data);
                    }
                }

                break;
            }
        }

        return entity;
    }

    public void setDetonator(Entity detonator) {
        if (detonator instanceof ServerPlayer) this.detonator = detonator.getUUID();
    }

    @Override
    public void remove(RemovalReason reason) {
        clearChunkLoader();
        super.remove(reason);
    }

    public static class ATEntry {
        public final ResourceKey<Level> dim;
        public final int x;
        public final int y;
        public final int z;

        public ATEntry(ResourceKey<Level> dim, int x, int y, int z) {
            this.dim = dim;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public int hashCode() {
            final int prime = 27644437;
            int result = 1;
            result = prime * result + dim.hashCode();
            result = prime * result + x;
            result = prime * result + y;
            result = prime * result + z;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ATEntry other = (ATEntry) obj;
            return dim.equals(other.dim) && x == other.x && y == other.y && z == other.z;
        }
    }
}
