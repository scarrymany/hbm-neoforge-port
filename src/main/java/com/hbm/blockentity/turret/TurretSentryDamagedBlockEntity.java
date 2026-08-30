package com.hbm.blockentity.turret;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code TileEntityTurretSentryDamaged} - a "damaged" ruins/loot variant of
 * Sentry: always powered/on, slower turn speed, targets any {@link LivingEntity} (except creative
 * players) with no filter/whitelist logic at all, and fires a hardcoded ammo type rather than
 * reading the ammo inventory via {@link #getFirstConfigLoaded()}.
 */
public class TurretSentryDamagedBlockEntity extends TurretSentryBlockEntity {

    public TurretSentryDamagedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean hasPower() {
        return true;
    }

    @Override
    public boolean isOn() {
        return true;
    }

    @Override
    public double getTurretYawSpeed() {
        return 3D;
    }

    @Override
    public double getTurretPitchSpeed() {
        return 2D;
    }

    @Override
    public boolean entityAcceptableTarget(Entity e) {
        if (e instanceof Player player && player.isCreative()) return false;
        return e instanceof LivingEntity;
    }

    @Override
    public void updateFiringTick() {
        timer++;

        if (timer % 10 == 0) {
            // TODO(phase3-gun-content): CE hardcodes XFactory9mm.p9_fmj here (not read from
            // getFirstConfigLoaded() at all) - not ported yet, see getAmmoList()'s own javadoc.
            BulletConfig conf = null;

            if (conf != null && level != null) {
                if (shotSide) {
                    level.playSound(null, worldPosition, HBMSoundHandler.sentryFire.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
                    spawnBullet(conf, 5F);
                } else {
                    level.playSound(null, worldPosition, HBMSoundHandler.sentryFire.get(), SoundSource.BLOCKS, 2.0F, 0.75F);
                    if (usesCasings()) {
                        if (casingDelay() == 0) spawnCasing();
                        else casingDelay = casingDelay();
                    }
                }

                if (shotSide) this.didJustShootLeft = true;
                else this.didJustShootRight = true;
                shotSide = !shotSide;
            }
        }
    }
}
