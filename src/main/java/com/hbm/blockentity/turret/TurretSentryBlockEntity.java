package com.hbm.blockentity.turret;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.particle.HbmEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;

/**
 * Ported from CE's {@code TileEntityTurretSentry} - the small single-block (not {@link
 * com.hbm.blocks.BlockDummyable}) light machine-gun turret. Ammo: CE's
 * {@code XFactory9mm.p9_sp}/{@code p9_fmj}/{@code p9_jhp}/{@code p9_ap} - not ported yet (see
 * {@link TurretBaseBlockEntity#getAmmoList()}'s own javadoc), so {@link #getAmmoList()} is empty
 * until Package D lands.
 */
public class TurretSentryBlockEntity extends TurretBaseBlockEntity {

    // client-side barrel recoil animation - pure cosmetic, no gun-content dependency
    public boolean didJustShootLeft = false;
    public boolean retractingLeft = false;
    public double barrelLeftPos = 0;
    public double lastBarrelLeftPos = 0;
    public boolean didJustShootRight = false;
    public boolean retractingRight = false;
    public double barrelRightPos = 0;
    public double lastBarrelRightPos = 0;

    protected boolean shotSide = false;
    protected int timer;

    public TurretSentryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected List<BulletConfig> getAmmoList() {
        // TODO(phase3-gun-content): CE uses XFactory9mm.p9_sp/p9_fmj/p9_jhp/p9_ap.
        return Collections.emptyList();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.turretSentry");
    }

    @Override
    public double getTurretDepression() {
        return 20D;
    }

    @Override
    public double getTurretElevation() {
        return 20D;
    }

    @Override
    public double getDecetorRange() {
        return 24D;
    }

    @Override
    public double getDecetorGrace() {
        return 2D;
    }

    @Override
    public long getMaxPower() {
        return 1_000;
    }

    @Override
    public long getConsumption() {
        return 5;
    }

    @Override
    public double getBarrelLength() {
        return 1.25D;
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 15;
    }

    @Override
    public boolean hasThermalVision() {
        return false;
    }

    @Override
    public void updateEntity() {
        if (level != null && level.isClientSide) {
            this.lastBarrelLeftPos = this.barrelLeftPos;
            this.lastBarrelRightPos = this.barrelRightPos;

            float retractSpeed = 0.5F;
            float pushSpeed = 0.25F;

            if (this.retractingLeft) {
                this.barrelLeftPos += retractSpeed;
                if (this.barrelLeftPos >= 1) this.retractingLeft = false;
            } else {
                this.barrelLeftPos -= pushSpeed;
                if (this.barrelLeftPos < 0) this.barrelLeftPos = 0;
            }

            if (this.retractingRight) {
                this.barrelRightPos += retractSpeed;
                if (this.barrelRightPos >= 1) this.retractingRight = false;
            } else {
                this.barrelRightPos -= pushSpeed;
                if (this.barrelRightPos < 0) this.barrelRightPos = 0;
            }
        }

        super.updateEntity();
    }

    @Override
    public void updateFiringTick() {
        timer++;

        if (timer % 10 == 0) {
            BulletConfig conf = getFirstConfigLoaded();

            if (conf != null && level != null) {
                spawnBullet(conf, 5F);
                consumeAmmo(conf.getAmmo());
                level.playSound(null, worldPosition, HBMSoundHandler.sentryFire.get(), SoundSource.BLOCKS, 2.0F, 1.0F);

                // CE muzzle-flash burst (AuxParticlePacketNT/HbmEffectNT.VanillaExt_LargeExplode,
                // upstream/hbm-ce/.../TileEntityTurretSentry.java:177-182, size=1F count=1) - simplified
                // to the turret's own getTurretPos() rather than CE's exact barrel-side offset math
                // (shared gun-VFX casing/muzzle substrate is still a documented open item, see class
                // javadoc cross-reference to TurretBaseBlockEntity#spawnCasing).
                CompoundTag flashData = new CompoundTag();
                flashData.putFloat("size", 1F);
                flashData.putInt("count", 1);
                Vec3 muzzle = getTurretPos();
                HbmEffect.sendPacket(level, HbmEffect.VANILLA_EXT_LARGE_EXPLODE, muzzle.x, muzzle.y, muzzle.z, 50, flashData);

                if (shotSide) this.didJustShootLeft = true;
                else this.didJustShootRight = true;
                shotSide = !shotSide;
            }
        }
    }

    /**
     * CE: {@code TileEntityTurretSentry.seekNewTarget()} - plays a lock-on chirp exactly when the
     * turret acquires a NEW target (not on every re-seek while already tracking the same one, and
     * not on losing a target). Only this turret type plays this sound in CE (no other
     * {@code TileEntityTurretBaseNT} subclass overrides {@code seekNewTarget} to add it).
     */
    @Override
    protected void seekNewTarget() {
        Entity previous = this.target;
        super.seekNewTarget();

        if (previous != this.target && this.target != null && level != null) {
            level.playSound(null, worldPosition, HBMSoundHandler.sentryLockon.get(), SoundSource.BLOCKS, 2.0F, 1.5F);
        }
    }

    @Override
    protected void updateConnections() {
        if (level == null) return;
        trySubscribe(level, worldPosition.getX(), worldPosition.getY() - 1, worldPosition.getZ(), Direction.DOWN);
    }

    @Override
    public boolean usesCasings() {
        return true;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(didJustShootLeft);
        buf.writeBoolean(didJustShootRight);
        didJustShootLeft = false;
        didJustShootRight = false;
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.retractingLeft = buf.readBoolean();
        this.retractingRight = buf.readBoolean();
    }
}
