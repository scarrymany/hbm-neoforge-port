package com.hbm.entity.logic;

import com.hbm.config.GeneralConfig;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Port of CE's {@code com.hbm.entity.logic.EntityBomber} (332 lines) - a scripted bombing-run plane,
 * per {@code docs/phase4/entities_vehicles_aircraft.md}'s Deferred scope (flagged there as
 * unclaimed-but-trivial once {@link EntityPlaneBase} exists) and this task's own explicit instruction
 * to build it as a follow-up.
 * <p>
 * 8 static {@code statFacX} spawn-factory methods (Carpet/Napalm/Chlorine/Orange/ABomb/Stinger/
 * Boxcar/PC - one per CE "air-strike type"), each setting a {@code bombStart}/{@code bombStop}/
 * {@code bombRate}/{@code type} tuple before calling the shared {@link #fac} position/heading setup
 * (spawn upwind of the target at Y+50, aim inward). The actual per-tick payload drop ({@link #tick},
 * gated on {@code tickCount} falling inside {@code [bombStart, bombStop)} on a {@code % bombRate}
 * cadence) branches on {@code type}: types 3/7 call {@link ExplosionChaos#spawnChlorine} directly (no
 * submunition entity - already real in this port, called here per this task's explicit instruction);
 * types 0/1/2/4/5 (bomblet-dropping) and 6 (rocket-dropping) need {@code EntityBombletZeta}/{@code
 * EntityBoxcar} respectively - neither exists anywhere in this port (unowned, out of this package's
 * scope per this task's explicit instruction) and are left as documented no-op branches rather than
 * inventing new entity classes for them.
 * <p>
 * <b>CE's {@code STYLE} synced byte (cosmetic bomber-model variant selector) is not ported</b> - purely
 * a client rendering concern with zero gameplay effect, matching this port's established
 * deferred-cosmetic-field precedent (see e.g. {@code LegacyMobBulletConfigs}'s own class javadoc for
 * the identical reasoning applied to a sibling Phase 4 package).
 * <p>
 * <b>{@code isWarDim} gate - a real, deliberate default-true stub</b>, matching this port's own
 * established convention for the identical CE mechanic (see {@code com.hbm.potion.
 * HbmPotionEffects#isWarDim}'s javadoc for the full reasoning: CE's real default has an empty
 * dimension blacklist, so {@code isWarDim} evaluates to {@code true} everywhere until a server
 * operator opts a dimension out - stubbing {@code false} would silently disable real CE-default
 * behavior, not preserve it).
 */
public class EntityBomber extends EntityPlaneBase {

    private int bombStart = 75;
    private int bombStop = 125;
    private int bombRate = 3;
    private int type = 0;

    public EntityBomber(EntityType<? extends EntityBomber> type, Level level) {
        super(type, level);
    }

    public EntityBomber(Level level) {
        this(PlaneEntityTypes.BOMBER.get(), level);
    }

    public static EntityBomber statFacCarpet(Level level, double x, double y, double z) {
        EntityBomber bomber = new EntityBomber(level);
        bomber.timer = 200;
        bomber.bombStart = 50;
        bomber.bombStop = 100;
        bomber.bombRate = 2;
        bomber.fac(x, y, z);
        bomber.type = 0;
        return bomber;
    }

    public static EntityBomber statFacNapalm(Level level, double x, double y, double z) {
        EntityBomber bomber = new EntityBomber(level);
        bomber.timer = 200;
        bomber.bombStart = 50;
        bomber.bombStop = 100;
        bomber.bombRate = 5;
        bomber.fac(x, y, z);
        bomber.type = 1;
        return bomber;
    }

    public static EntityBomber statFacChlorine(Level level, double x, double y, double z) {
        EntityBomber bomber = new EntityBomber(level);
        bomber.timer = 200;
        bomber.bombStart = 50;
        bomber.bombStop = 100;
        bomber.bombRate = 4;
        bomber.fac(x, y, z);
        bomber.type = 2;
        return bomber;
    }

    public static EntityBomber statFacOrange(Level level, double x, double y, double z) {
        EntityBomber bomber = new EntityBomber(level);
        bomber.timer = 200;
        bomber.bombStart = 75;
        bomber.bombStop = 125;
        bomber.bombRate = 1;
        bomber.fac(x, y, z);
        bomber.type = 3;
        return bomber;
    }

    public static EntityBomber statFacABomb(Level level, double x, double y, double z) {
        EntityBomber bomber = new EntityBomber(level);
        bomber.timer = 200;
        bomber.bombStart = 60;
        bomber.bombStop = 70;
        bomber.bombRate = 65;
        bomber.fac(x, y, z);
        bomber.type = 4;
        return bomber;
    }

    public static EntityBomber statFacStinger(Level level, double x, double y, double z) {
        EntityBomber bomber = new EntityBomber(level);
        bomber.timer = 200;
        bomber.bombStart = 50;
        bomber.bombStop = 150;
        bomber.bombRate = 10;
        bomber.fac(x, y, z);
        bomber.type = 5;
        return bomber;
    }

    public static EntityBomber statFacBoxcar(Level level, double x, double y, double z) {
        EntityBomber bomber = new EntityBomber(level);
        bomber.timer = 200;
        bomber.bombStart = 50;
        bomber.bombStop = 150;
        bomber.bombRate = 10;
        bomber.fac(x, y, z);
        bomber.type = 6;
        return bomber;
    }

    public static EntityBomber statFacPC(Level level, double x, double y, double z) {
        EntityBomber bomber = new EntityBomber(level);
        bomber.timer = 200;
        bomber.bombStart = 75;
        bomber.bombStop = 125;
        bomber.bombRate = 1;
        bomber.fac(x, y, z);
        bomber.type = 7;
        return bomber;
    }

    /** CE: {@code fac(World, x, y, z)} - spawns upwind of the target at Y+50, aimed inward. */
    private void fac(double x, double y, double z) {
        Vec3 vector = new Vec3(this.random.nextDouble() - 0.5, 0, this.random.nextDouble() - 0.5).normalize();
        double scale = GeneralConfig.ENABLE_BOMBER_SHORT_MODE.get() ? 1.0D : 2.0D;
        vector = new Vec3(vector.x * scale, 0D, vector.z * scale);

        this.moveTo(x - vector.x * 100, y + 50, z - vector.z * 100, 0F, 0F);
        this.setDeltaMovement(vector.x, 0D, vector.z);
        this.rotation();
    }

    /** See class javadoc - a real, deliberate default-true stub, not a placeholder left broken. */
    private static boolean isWarDim(Level level) {
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        Level level = this.level();
        if (level.isClientSide() || this.isRemoved()) return;
        if (!isWarDim(level)) return;

        if (this.health > 0 && this.tickCount > bombStart && this.tickCount < bombStop && this.tickCount % bombRate == 0) {
            // NOTE, preserved exactly (a real, confirmed CE quirk, not a mis-port): CE's own dispatch
            // keys off `type == 3`/`type == 7` for the two spawnChlorine calls below, but `type == 3`
            // is assigned by statFacOrange, not statFacChlorine (which sets `type = 2` and therefore
            // falls into the bomblet `default` branch below, spawning no chlorine at all). Reproduced
            // verbatim from CE's real source rather than "corrected" to match the method names.
            switch (this.type) {
                // SoundEvents.FIRE_EXTINGUISH is well-established Mojang-mapping knowledge for CE's
                // SoundEvents.BLOCK_FIRE_EXTINGUISH (the "BLOCK_" prefix was dropped) - not
                // independently confirmed against a compiled jar in this sandbox, matching this port's
                // own established disclosure convention for identical unconfirmed-rename risk.
                case 3 -> {
                    level.playSound(null, getX() + 0.5, getY() + 0.5, getZ() + 0.5, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 5.0F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
                    ExplosionChaos.spawnChlorine(level, getX(), getY() - 1D, getZ(), 10, 0.5D, 3);
                }
                case 5 -> {
                    // CE: an intentionally empty branch (Stinger drops nothing via this path).
                }
                case 6 -> {
                    // TODO(unowned-entity, EntityBoxcar): CE spawns a rocket submunition here - see
                    // class javadoc, not built by this package.
                    level.playSound(null, getX() + 0.5, getY() + 0.5, getZ() + 0.5, HBMSoundHandler.missileTakeoff.get(), SoundSource.HOSTILE, 10.0F, 0.9F + random.nextFloat() * 0.2F);
                }
                case 7 -> {
                    level.playSound(null, getX() + 0.5, getY() + 0.5, getZ() + 0.5, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 5.0F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
                    ExplosionChaos.spawnChlorine(level, getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) getX(), (int) getZ()) + 2, getZ(), 10, 1D, 2);
                }
                default -> {
                    // TODO(unowned-entity, EntityBombletZeta): CE spawns a bomblet submunition here for
                    // types 0 (Carpet), 1 (Napalm), 2 (Chlorine - see the NOTE above), and 4 (ABomb) -
                    // see class javadoc, not built by this package.
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.bombStart = tag.getInt("bombStart");
        this.bombStop = tag.getInt("bombStop");
        this.bombRate = tag.getInt("bombRate");
        this.type = tag.getInt("type");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("bombStart", this.bombStart);
        tag.putInt("bombStop", this.bombStop);
        tag.putInt("bombRate", this.bombRate);
        tag.putInt("type", this.type);
    }
}
