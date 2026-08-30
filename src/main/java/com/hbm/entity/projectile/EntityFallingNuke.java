package com.hbm.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Ported from CE's {@code com.hbm.entity.projectile.EntityFallingNuke} (119 lines, read in full) -
 * {@code NukeCustom}'s air-dropped ("falling bomb") mode (see {@code docs/phase3/
 * bomb_blocks_and_detonators.md} §B: {@code TileEntityNukeCustom} spawns this instead of detonating
 * in place when the casing is configured as a dropped, rather than placed, ordnance). CE extends
 * {@code EntityThrowable}, but its {@code onImpact} override is an empty no-op and it drives its
 * own position/motion/ground-check entirely inside {@code onUpdate} rather than relying on {@code
 * EntityThrowable}'s raycast-hit machinery - so this port extends plain {@link Entity} directly
 * instead of a projectile base class, matching what CE's code actually uses rather than what its
 * superclass name suggests. CE's {@code FACING} synced data field is declared but never actually
 * assigned anywhere in the class (confirmed by reading it in full) - genuinely dead state, dropped
 * rather than carried forward.
 * <p>
 * <b>Blocking dependency (documented forward reference, not this pass's to fix)</b>: {@code
 * com.hbm.blocks.bomb.NukeCustom} - the class whose static {@code explodeCustom(...)} this entity
 * calls on landing - does not exist anywhere in this port yet (confirmed; {@code com.hbm.blocks.bomb}
 * has zero files). That call site is a documented {@code TODO} below; everything else (falling
 * physics, rotation animation, ground-contact detection, thrower tracking) is fully ported.
 */
public class EntityFallingNuke extends Entity {

    private float tnt;
    private float nuke;
    private float hydro;
    private float bale;
    private float dirty;
    private float schrab;
    private float sol;
    private float euph;

    @Nullable
    public LivingEntity thrower;

    public EntityFallingNuke(EntityType<? extends EntityFallingNuke> entityType, Level level) {
        super(entityType, level);
    }

    public EntityFallingNuke(Level level, Entity detonator, float tnt, float nuke, float hydro, float bale, float dirty, float schrab, float sol, float euph) {
        this(FallingNukeEntityTypes.FALLING_NUKE.get(), level);

        this.tnt = tnt;
        this.nuke = nuke;
        this.hydro = hydro;
        this.bale = bale;
        this.dirty = dirty;
        this.schrab = schrab;
        this.sol = sol;
        this.euph = euph;
        this.setXRot(90);
        this.setYRot(90);
        if (detonator instanceof LivingEntity livingEntity) this.thrower = livingEntity;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);

        double mx = getDeltaMovement().x * 0.99;
        double mz = getDeltaMovement().z * 0.99;
        double my = getDeltaMovement().y - 0.05D;
        if (my < -1) my = -1;
        setDeltaMovement(mx, my, mz);

        rotation();

        if (!level().getBlockState(BlockPos.containing(getX(), getY(), getZ())).isAir()) {
            if (!level().isClientSide()) {
                // TODO(com.hbm.blocks.bomb.NukeCustom, docs/phase3/bomb_blocks_and_detonators.md
                // §B): CE calls NukeCustom.explodeCustom(world, thrower, posX, posY, posZ, tnt,
                // nuke, hydro, bale, dirty, schrab, sol, euph) here on ground contact. That class
                // doesn't exist in this port yet - the entity still discards itself on landing
                // (preserving "the falling bomb goes away when it hits the ground") but currently
                // detonates nothing until NukeCustom lands.
                this.discard();
            }
        }
    }

    private void rotation() {
        if (getXRot() > -75) setXRot(getXRot() - 2);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        tnt = tag.getFloat("tnt");
        nuke = tag.getFloat("nuke");
        hydro = tag.getFloat("hydro");
        bale = tag.getFloat("bale");
        dirty = tag.getFloat("dirty");
        schrab = tag.getFloat("schrab");
        sol = tag.getFloat("sol");
        euph = tag.getFloat("euph");
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        tag.putFloat("tnt", tnt);
        tag.putFloat("nuke", nuke);
        tag.putFloat("hydro", hydro);
        tag.putFloat("bale", bale);
        tag.putFloat("dirty", dirty);
        tag.putFloat("schrab", schrab);
        tag.putFloat("sol", sol);
        tag.putFloat("euph", euph);
    }
}
