package com.hbm.blockentity.turret;

import com.hbm.damage.ModDamageTypes;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.List;

/**
 * Ported from CE's {@code TileEntityTurretTauon} - the tauon particle-accelerator turret. Direct-
 * damage turret using the already-registered {@link ModDamageTypes#ELECTRICITY}, but still
 * ammo-gated on {@code XFactoryAccelerator.tau_uranium} exactly like CE's own
 * {@code consumeAmmo(conf.ammo)} call proves ("ammo consumption is decoupled from projectile
 * spawning" - see {@code docs/phase3/turret_system.md} decision 7). Not ported yet (see
 * {@link #getAmmoList()}), so this turret is a correct no-op until Package D lands - the attack
 * logic itself is fully ported.
 */
public class TurretTauonBlockEntity extends TurretBaseBlockEntity {

    protected int timer;

    public TurretTauonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected List<BulletConfig> getAmmoList() {
        // TODO(phase3-gun-content): CE uses XFactoryAccelerator.tau_uranium.
        return Collections.emptyList();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.turretTauon");
    }

    @Override
    public double getDecetorGrace() {
        return 3D;
    }

    @Override
    public double getTurretYawSpeed() {
        return 9D;
    }

    @Override
    public double getTurretPitchSpeed() {
        return 6D;
    }

    @Override
    public double getTurretElevation() {
        return 35D;
    }

    @Override
    public double getTurretDepression() {
        return 35D;
    }

    @Override
    public double getDecetorRange() {
        return 128D;
    }

    @Override
    public double getBarrelLength() {
        return 2.0D - 0.0625D;
    }

    @Override
    public long getMaxPower() {
        return 100_000;
    }

    @Override
    public long getConsumption() {
        return 1_000;
    }

    @Override
    public void updateFiringTick() {
        if (level == null) return;

        timer++;

        if (timer % 5 == 0) {
            BulletConfig conf = getFirstConfigLoaded();

            if (conf != null && this.target != null) {
                DamageSource source = level.damageSources().source(ModDamageTypes.ELECTRICITY);
                this.target.hurt(source, 30F + level.random.nextInt(11));
                consumeAmmo(conf.getAmmo());
                level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                        HBMSoundHandler.tauShoot.get(), SoundSource.BLOCKS, 4.0F, 0.9F + level.random.nextFloat() * 0.3F);
                networkPackNT(250);
                // TODO(phase3-gun-vfx): CE spawns a "Tau" particle beam effect here - deferred.
            }
        }
    }
}
