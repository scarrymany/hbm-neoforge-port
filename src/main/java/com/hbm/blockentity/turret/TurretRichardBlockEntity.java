package com.hbm.blockentity.turret;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;

/**
 * Ported from CE's {@code TileEntityTurretRichard} - the lock-on rocket-pod turret. Ammo: CE's
 * {@code XFactoryRocket.rocket_ml} list - not ported yet. {@link #spawnBullet} overrides the base
 * implementation to set {@link EntityBulletBaseMK4#lockonTarget}, matching CE exactly (the one
 * turret whose projectiles home in on the current target after launch).
 */
public class TurretRichardBlockEntity extends TurretBaseBlockEntity {

    protected int timer;
    public int loaded;
    protected int reload;

    public TurretRichardBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected List<BulletConfig> getAmmoList() {
        // TODO(phase3-gun-content): CE uses every entry of XFactoryRocket.rocket_ml.
        return Collections.emptyList();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.turretRichard");
    }

    @Override
    public double getTurretDepression() {
        return 25D;
    }

    @Override
    public double getTurretElevation() {
        return 25D;
    }

    @Override
    public double getBarrelLength() {
        return 1.25D;
    }

    @Override
    public long getMaxPower() {
        return 10_000;
    }

    @Override
    public double getDecetorGrace() {
        return 8D;
    }

    @Override
    public double getDecetorRange() {
        return 64D;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        if (level != null && !level.isClientSide) {
            if (reload > 0) {
                reload--;
                if (reload == 0) this.loaded = 17;
            }

            if (loaded <= 0 && reload <= 0 && getFirstConfigLoaded() != null) {
                reload = 100;
            }

            if (getFirstConfigLoaded() == null) {
                this.loaded = 0;
            }

            networkPackNT(250);
        }
    }

    @Override
    public void updateFiringTick() {
        if (reload > 0) return;

        timer++;

        if (timer > 0 && timer % 10 == 0) {
            BulletConfig conf = getFirstConfigLoaded();

            if (conf != null && level != null) {
                spawnBullet(conf, 30F);
                consumeAmmo(conf.getAmmo());
                level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                        HBMSoundHandler.richard_fire.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
                this.loaded--;
            } else {
                this.loaded = 0;
            }
        }
    }

    @Override
    public void spawnBullet(BulletConfig bullet, float baseDamage) {
        if (level == null) return;

        Vec3 pos = getTurretPos();
        Vec3 vec = new Vec3(getBarrelLength(), 0, 0);
        vec = com.hbm.util.Vec3dUtil.rotateRoll(vec, (float) -this.rotationPitch);
        vec = vec.yRot((float) -(this.rotationYaw + Math.PI * 0.5));

        EntityBulletBaseMK4 proj = new EntityBulletBaseMK4(level, bullet, baseDamage, bullet.spread, (float) rotationYaw, (float) rotationPitch);
        proj.moveTo(pos.x + vec.x, pos.y + vec.y, pos.z + vec.z, proj.getYRot(), proj.getXRot());
        proj.lockonTarget = this.target;
        level.addFreshEntity(proj);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeVarInt(this.loaded);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.loaded = buf.readVarInt();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.loaded = tag.getInt("loaded");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("loaded", this.loaded);
    }
}
