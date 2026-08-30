package com.hbm.entity.grenade;

import com.hbm.entity.projectile.EntityThrowableInterp;
import com.hbm.items.weapon.grenade.EnumGrenadeExtra;
import com.hbm.items.weapon.grenade.EnumGrenadeFilling;
import com.hbm.items.weapon.grenade.EnumGrenadeFuze;
import com.hbm.items.weapon.grenade.EnumGrenadeShell;
import com.hbm.items.weapon.grenade.GrenadeDataComponents;
import com.hbm.items.weapon.grenade.GrenadeLoadout;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.util.EnumUtil;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Port of CE's {@code com.hbm.entity.grenade.EntityGrenadeUniversal} (199 lines) - the modern
 * Universal Grenade's flight entity. Extends {@link EntityThrowableInterp} exactly like CE (client-
 * position-interpolating; itself already documents the ballistics/impact-order semantics this class
 * relies on).
 * <p>
 * <b>Deviation from CE's carried-{@code ItemStack} design.</b> CE syncs the entire thrown
 * {@code ItemStack} via a {@code DataParameter<ItemStack>} and re-derives shell/filling/fuze/extra
 * from its NBT every time they're needed. This port instead syncs the 4 loadout values directly as
 * 4 small {@code byte} fields (no carried stack at all) - nothing in the ported scope ever needs the
 * original stack's exact identity/NBT back (not even {@link EnumGrenadeExtra#TRIPLEX}'s child-grenade
 * spawn, which CE itself rebuilds a *fresh* stack for via {@code ItemGrenadeUniversal.make(...)}
 * rather than reusing the carried one) - only the 4 enum values themselves, which this shape
 * provides more directly and with a smaller synced payload.
 * <p>
 * <b>Not ported (see {@code docs/phase3/grenades.md}'s Deferred scope - Phase 5 client rendering):</b>
 * {@code spin}/{@code prevSpin} roll-rate bookkeeping and the {@code TRAIL_TRIPLET} flame-trail
 * particle call are pure render state with no server-side gameplay effect; the {@link #trail} field
 * itself is still carried (for save/network parity and so a future renderer has something to key off)
 * but nothing currently reads it to spawn particles.
 */
public class EntityGrenadeUniversal extends EntityThrowableInterp {

    public static final int TRAIL_NONE = 0;
    public static final int TRAIL_TRIPLET = 1;

    private static final EntityDataAccessor<Byte> DATA_SHELL =
            SynchedEntityData.defineId(EntityGrenadeUniversal.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_FILLING =
            SynchedEntityData.defineId(EntityGrenadeUniversal.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_FUZE =
            SynchedEntityData.defineId(EntityGrenadeUniversal.class, EntityDataSerializers.BYTE);
    /** -1 = no extra installed (CE: {@code KEY_EXTRA} tag absent). */
    private static final EntityDataAccessor<Byte> DATA_EXTRA =
            SynchedEntityData.defineId(EntityGrenadeUniversal.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_BOUNCES =
            SynchedEntityData.defineId(EntityGrenadeUniversal.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TRAIL =
            SynchedEntityData.defineId(EntityGrenadeUniversal.class, EntityDataSerializers.INT);

    public EntityGrenadeUniversal(EntityType<? extends EntityGrenadeUniversal> type, Level level) {
        super(type, level);
    }

    public EntityGrenadeUniversal(Level level, GrenadeLoadout loadout) {
        this(GrenadeEntityTypes.GRENADE_UNIVERSAL.get(), level);
        setLoadout(loadout);
    }

    public EntityGrenadeUniversal(Level level, Player thrower, ItemStack grenadeStack, InteractionHand hand) {
        this(GrenadeEntityTypes.GRENADE_UNIVERSAL.get(), level);
        this.setOwner(thrower);

        GrenadeLoadout loadout = grenadeStack.getOrDefault(GrenadeDataComponents.LOADOUT.get(), GrenadeLoadout.DEFAULT);
        setLoadout(loadout);

        this.moveTo(thrower.getX(), thrower.getY() + thrower.getEyeHeight(), thrower.getZ(), thrower.getYRot(), thrower.getXRot());

        double sideOffset = hand == InteractionHand.OFF_HAND ? -0.25D : 0.25D;
        Vec3 offset = new Vec3(sideOffset, -0.25D, 0D);
        offset = offset.yRot(-this.getYRot() / 180F * (float) Math.PI);
        this.setPos(this.position().add(offset));

        Vec3 look = thrower.getLookAngle();
        this.shoot(look.x, look.y, look.z, (float) loadout.shell().getYeetForce(), 0F);
    }

    private void setLoadout(GrenadeLoadout loadout) {
        this.getEntityData().set(DATA_SHELL, (byte) loadout.shell().ordinal());
        this.getEntityData().set(DATA_FILLING, (byte) loadout.filling().ordinal());
        this.getEntityData().set(DATA_FUZE, (byte) loadout.fuze().ordinal());
        this.getEntityData().set(DATA_EXTRA, (byte) (loadout.extra() == null ? -1 : loadout.extra().ordinal()));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SHELL, (byte) EnumGrenadeShell.FRAG.ordinal());
        builder.define(DATA_FILLING, (byte) EnumGrenadeFilling.HE.ordinal());
        builder.define(DATA_FUZE, (byte) EnumGrenadeFuze.S3.ordinal());
        builder.define(DATA_EXTRA, (byte) -1);
        builder.define(DATA_BOUNCES, 0);
        builder.define(DATA_TRAIL, TRAIL_NONE);
    }

    public EntityGrenadeUniversal setTrail(int trail) {
        this.getEntityData().set(DATA_TRAIL, trail);
        return this;
    }

    /**
     * Small public wrapper around the inherited {@code Projectile#setOwner(Entity)} so
     * {@link EnumGrenadeExtra#TRIPLEX}'s child-grenade spawn (a different class entirely, outside
     * this class's own hierarchy) can copy the parent grenade's owner without this port needing to
     * confirm {@code setOwner}'s exact visibility from an external caller - every other
     * {@code setOwner} call in this port is a same-class/same-hierarchy {@code this.setOwner(...)}.
     */
    public void setOwnerEntity(@Nullable Entity owner) {
        this.setOwner(owner);
    }

    public int getBounces() {
        return this.getEntityData().get(DATA_BOUNCES);
    }

    public int getTrail() {
        return this.getEntityData().get(DATA_TRAIL);
    }

    public EnumGrenadeShell getShell() {
        return EnumUtil.grabEnumSafely(EnumGrenadeShell.VALUES, this.getEntityData().get(DATA_SHELL));
    }

    public EnumGrenadeFilling getFilling() {
        return EnumUtil.grabEnumSafely(EnumGrenadeFilling.VALUES, this.getEntityData().get(DATA_FILLING));
    }

    public EnumGrenadeFuze getFuze() {
        return EnumUtil.grabEnumSafely(EnumGrenadeFuze.VALUES, this.getEntityData().get(DATA_FUZE));
    }

    public EnumGrenadeExtra getExtra() {
        byte ordinal = this.getEntityData().get(DATA_EXTRA);
        return (ordinal < 0 || ordinal >= EnumGrenadeExtra.VALUES.length) ? null : EnumGrenadeExtra.VALUES[ordinal];
    }

    public int getTimer() {
        return this.ticksInAir + this.ticksInGround;
    }

    @Override
    public void tick() {
        super.tick();

        EnumGrenadeFuze fuze = this.getFuze();
        EnumGrenadeExtra extra = this.getExtra();

        if (fuze.updateTick != null) fuze.updateTick.accept(this);
        if (extra != null && extra.updateTick != null) extra.updateTick.accept(this);
    }

    @Override
    protected void onImpact(HitResult mop) {
        EnumGrenadeFuze fuze = this.getFuze();
        EnumGrenadeExtra extra = this.getExtra();

        if (fuze.onImpact != null) fuze.onImpact.accept(this, mop);
        if (extra != null && extra.onImpact != null) extra.onImpact.accept(this, mop);

        if (this.isRemoved()) return; // already exploded via one of the hooks above

        if (mop instanceof BlockHitResult bhr) {
            Direction dir = bhr.getDirection();
            Vec3 hit = bhr.getLocation();
            this.setPos(hit.x + dir.getStepX() * 0.05, hit.y + dir.getStepY() * 0.05, hit.z + dir.getStepZ() * 0.05);

            EnumGrenadeShell shell = this.getShell();
            Vec3 motion = this.getDeltaMovement();
            if (motion.length() > 0.2D) {
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        HBMSoundHandler.grenadeBounce, SoundSource.HOSTILE, 1F, 1F);
            }

            double mx = dir.getStepX() != 0 ? motion.x * -shell.getBounce() : motion.x * 0.8D;
            double my = dir.getStepY() != 0 ? motion.y * -shell.getBounce() : motion.y * 0.8D;
            double mz = dir.getStepZ() != 0 ? motion.z * -shell.getBounce() : motion.z * 0.8D;
            this.setDeltaMovement(mx, my, mz);

            // CE resyncs via TrackerUtil.sendTeleport here (a 1.12 client-authoritative-physics
            // workaround for a server-side velocity flip). 1.21.1's entity tracker already pushes a
            // non-player entity's server-side motion/position changes to observers automatically -
            // see docs/phase3/grenades.md's Open Questions, which flags this exact CE call as "very
            // plausibly dead weight" pending confirmation; no equivalent call is made here.

            this.getEntityData().set(DATA_BOUNCES, this.getBounces() + 1);
        }
    }

    public void explode() {
        this.discard();

        EnumGrenadeFilling filling = this.getFilling();
        if (filling.explode != null) filling.explode.accept(this);

        EnumGrenadeExtra extra = this.getExtra();
        if (extra != null && extra.onExplode != null) extra.onExplode.accept(this);
    }

    @Override
    protected int groundDespawn() {
        return 0;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putByte("shell", this.getEntityData().get(DATA_SHELL));
        compound.putByte("filling", this.getEntityData().get(DATA_FILLING));
        compound.putByte("fuze", this.getEntityData().get(DATA_FUZE));
        compound.putByte("extra", this.getEntityData().get(DATA_EXTRA));
        compound.putInt("bounces", this.getBounces());
        compound.putInt("trail", this.getTrail());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.getEntityData().set(DATA_SHELL, compound.getByte("shell"));
        this.getEntityData().set(DATA_FILLING, compound.getByte("filling"));
        this.getEntityData().set(DATA_FUZE, compound.getByte("fuze"));
        this.getEntityData().set(DATA_EXTRA, compound.getByte("extra"));
        this.getEntityData().set(DATA_BOUNCES, compound.getInt("bounces"));
        this.getEntityData().set(DATA_TRAIL, compound.getInt("trail"));
    }
}
