package com.hbm.blockentity.turret;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.List;

/**
 * Ported from CE's {@code TileEntityTurretChekhov} - the .50 cal precision multiblock turret.
 * Ammo: CE's {@code XFactory50.bmg50_sp}/{@code fmj}/{@code jhp}/{@code ap}/{@code du} - not
 * ported yet, see {@link TurretBaseBlockEntity#getAmmoList()}'s own javadoc.
 */
public class TurretChekhovBlockEntity extends TurretBaseBlockEntity {

    protected int timer;

    // client-side barrel spin-up cosmetic
    public float spin;
    public float lastSpin;
    private float accel;
    private boolean manual;

    public TurretChekhovBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public long getMaxPower() {
        return 10_000;
    }

    @Override
    protected List<BulletConfig> getAmmoList() {
        // TODO(phase3-gun-content): CE uses XFactory50.bmg50_sp/fmj/jhp/ap/du.
        return Collections.emptyList();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.turretChekhov");
    }

    @Override
    public double getBarrelLength() {
        return 3.5D;
    }

    @Override
    public double getTurretElevation() {
        return 45D;
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 15;
    }

    public int getDelay() {
        return 2;
    }

    @Override
    public void updateFiringTick() {
        timer++;

        if (timer > 20 && timer % getDelay() == 0) {
            BulletConfig conf = getFirstConfigLoaded();

            if (conf != null && level != null) {
                spawnBullet(conf, 10F);
                consumeAmmo(conf.getAmmo());
                level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                        HBMSoundHandler.chekhov_fire.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
                // TODO(phase3-gun-vfx): CE spawns a muzzle-flash particle burst here - deferred.
            }
        }
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        if (level != null && level.isClientSide) {
            if (this.tPos != null || manual) {
                this.accel = Math.min(45F, this.accel += 2);
            } else {
                this.accel = Math.max(0F, this.accel -= 2);
            }

            manual = false;

            this.lastSpin = this.spin;
            this.spin += this.accel;

            if (this.spin >= 360F) {
                this.spin -= 360F;
                this.lastSpin -= 360F;
            }
        } else if (level != null) {
            if (this.tPos == null && !manual) {
                this.timer--;
                if (timer > 20) timer = 20;
                if (timer < 0) timer = 0;
            }
        }
    }

    @Override
    public void manualSetup() {
        manual = true;
    }

    @Override
    public boolean usesCasings() {
        return true;
    }
}
