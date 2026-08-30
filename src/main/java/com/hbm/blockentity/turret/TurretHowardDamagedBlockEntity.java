package com.hbm.blockentity.turret;

import com.hbm.config.WeaponConfig;
import com.hbm.damage.ModDamageTypes;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code TileEntityTurretHowardDamaged} - a "damaged" ruins/loot variant of
 * Howard: always powered/on, slower turn speed, shorter range, targets any {@link LivingEntity}
 * (except creative players), and fires unconditionally on a fixed 4-tick cadence with no ammo/
 * {@code loaded}-counter gating at all (unlike its parent). This needs no ammo content to be
 * fire-complete.
 */
public class TurretHowardDamagedBlockEntity extends TurretHowardBlockEntity {

    public TurretHowardDamagedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
    public double getDecetorRange() {
        return 16D;
    }

    @Override
    public double getDecetorGrace() {
        return 5D;
    }

    @Override
    public boolean hasThermalVision() {
        return false;
    }

    @Override
    public boolean entityAcceptableTarget(Entity e) {
        if (e instanceof Player player && player.isCreative()) return false;
        return e instanceof LivingEntity;
    }

    @Override
    public void updateFiringTick() {
        timer++;

        if (this.tPos != null && level != null) {
            if (timer % 4 == 0) {
                level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                        HBMSoundHandler.howard_fire.get(), SoundSource.BLOCKS, 4.0F, 0.7F + level.random.nextFloat() * 0.3F);

                // TODO(phase3-gun-vfx): CE ejects a DGK casing here - deferred, see the parent class.

                if (level.random.nextInt(100) + 1 <= WeaponConfig.CIWS_ACCURACY.get() * 0.5) {
                    DamageSource source = level.damageSources().source(ModDamageTypes.SHRAPNEL);
                    EntityDamageUtil.attackEntityFromIgnoreIFrame(this.target, source, 2F + level.random.nextInt(2));
                }

                // TODO(phase3-gun-vfx): CE spawns a muzzle-flash particle burst here - deferred.
            }
        }
    }
}
