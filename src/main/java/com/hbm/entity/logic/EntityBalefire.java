package com.hbm.entity.logic;

import com.hbm.config.GeneralConfig;
import com.hbm.explosion.ExplosionBalefire;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.main.MainRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Ported from CE's {@code com.hbm.entity.logic.EntityBalefire} (106 lines, read in full) - a
 * simpler standalone wrapper around {@link ExplosionBalefire} (no multi-instance triple, no
 * fallout spawn at all, confirmed by reading the entity in full - unlike {@code
 * EntityNukeExplosionMK3}'s "waste" path).
 */
public class EntityBalefire extends EntityExplosionChunkloading {

    public int age = 0;
    public int destructionRange = 0;
    public ExplosionBalefire exp;
    public int speed = 1;
    public boolean did = false;
    @Nullable
    public UUID detonator = null;

    public EntityBalefire(EntityType<? extends EntityBalefire> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        markChunkLoaderRestoredFromNBT();
        age = nbt.getInt("age");
        destructionRange = nbt.getInt("destructionRange");
        speed = nbt.getInt("speed");
        did = nbt.getBoolean("did");

        exp = new ExplosionBalefire((int) getX(), (int) getY(), (int) getZ(), level(), this.destructionRange);
        exp.readFromNbt(nbt, "exp_");

        this.did = true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("age", age);
        nbt.putInt("destructionRange", destructionRange);
        nbt.putInt("speed", speed);
        nbt.putBoolean("did", did);

        if (exp != null) exp.saveToNbt(nbt, "exp_");
    }

    @Override
    public void tick() {
        super.tick();
        Level level = level();
        if (level.isClientSide()) return;

        loadChunk(chunkPosition().x, chunkPosition().z);

        if (!this.did) {
            if (GeneralConfig.ENABLE_EXTENDED_LOGGING.get()) {
                MainRegistry.logger.info("[NUKE] Initialized BF explosion at {} / {} / {} with strength {}!", getX(), getY(), getZ(), destructionRange);
            }

            exp = new ExplosionBalefire((int) getX(), (int) getY(), (int) getZ(), level, this.destructionRange);
            exp.detonator = detonator;
            this.did = true;
        }

        speed += 1; // increase speed to keep up with expansion

        boolean flag = false;
        for (int i = 0; i < this.speed; i++) {
            flag = exp.update();
            if (flag) {
                this.discard();
            }
        }

        if (!flag) {
            double r = this.destructionRange * 2.0D;
            List<Entity> list = level.getEntitiesOfClass(Entity.class,
                    new AABB(getX() - r, getY() - r, getZ() - r, getX() + r, getY() + r, getZ() + r));
            ExplosionNukeGeneric.dealDamage(level, list, getX(), getY(), getZ(), r);
        }

        age++;
    }

    public void setDetonator(Entity detonator) {
        if (detonator instanceof ServerPlayer) this.detonator = detonator.getUUID();
    }

    @Override
    public void remove(RemovalReason reason) {
        // mlbv (CE): original upstream released the chunk loader after setDead(), which CE's own
        // comment calls brittle - CE moved it to run first, preserved in that same order here.
        clearChunkLoader();
        super.remove(reason);
    }
}
