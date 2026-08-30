package com.hbm.blockentity.turret;

import com.hbm.config.WeaponConfig;
import com.hbm.damage.ModDamageTypes;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.List;

/**
 * Ported from CE's {@code TileEntityTurretHoward} - the CIWS gatling point-defense turret. Ammo:
 * CE's {@code XFactoryTurret.dgk_normal} - not ported yet (see {@link #getAmmoList()}), so
 * {@link #loaded} never becomes positive and {@link #updateFiringTick()}'s shrapnel-damage body
 * (which needs no ammo config at all, only {@code loaded > 0}) stays correctly inert until then -
 * the tick-damage math itself is fully ported, not stubbed, since it is CE's own real behavior for
 * an already-loaded burst.
 */
public class TurretHowardBlockEntity extends TurretBaseBlockEntity {

    protected int loaded;
    protected int timer;

    // client-side gatling spin-up cosmetic
    public float spin;
    public float lastSpin;

    public TurretHowardBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected List<BulletConfig> getAmmoList() {
        // TODO(phase3-gun-content): CE uses XFactoryTurret.dgk_normal.
        return Collections.emptyList();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.turretHoward");
    }

    @Override
    public double getHeightOffset() {
        return 2.25D;
    }

    @Override
    public double getDecetorGrace() {
        return 3D;
    }

    @Override
    public double getTurretYawSpeed() {
        return 12D;
    }

    @Override
    public double getTurretPitchSpeed() {
        return 8D;
    }

    @Override
    public double getTurretElevation() {
        return 90D;
    }

    @Override
    public double getTurretDepression() {
        return 50D;
    }

    @Override
    public double getDecetorRange() {
        return this.targetPlayers ? 48D : 300D;
    }

    @Override
    public double getBarrelLength() {
        return 3.25D;
    }

    @Override
    public long getMaxPower() {
        return 50_000;
    }

    @Override
    public long getConsumption() {
        return 500;
    }

    @Override
    public void updateEntity() {
        if (level != null && level.isClientSide) {
            this.lastSpin = this.spin;
            if (this.tPos != null) this.spin += 45;
            if (this.spin >= 360F) {
                this.spin -= 360F;
                this.lastSpin -= 360F;
            }
        } else if (level != null) {
            if (loaded <= 0) {
                BulletConfig conf = getFirstConfigLoaded();
                if (conf != null) {
                    consumeAmmo(conf.getAmmo());
                    level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                            HBMSoundHandler.howard_reload.get(), SoundSource.BLOCKS, 4.0F, 1F);
                    loaded = 200;
                }
            }
        }
        super.updateEntity();
    }

    @Override
    public void updateFiringTick() {
        timer++;

        if (loaded > 0 && this.target != null && level != null) {
            level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    HBMSoundHandler.howard_fire.get(), SoundSource.BLOCKS, 4.0F, 0.9F + level.random.nextFloat() * 0.3F);
            level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    HBMSoundHandler.howard_fire.get(), SoundSource.BLOCKS, 4.0F, 1F + level.random.nextFloat() * 0.3F);

            // TODO(phase3-gun-vfx): CE ejects two DGK casings here (GunDGKFactory.CASINGDGK via
            // spawnCasing()) - GunDGKFactory doesn't exist yet either, deferred with the rest of the
            // gun-VFX substrate.

            if (timer % 2 == 0) {
                loaded--;

                if (level.random.nextInt(100) + 1 <= WeaponConfig.CIWS_ACCURACY.get()) {
                    DamageSource source = level.damageSources().source(ModDamageTypes.SHRAPNEL);
                    EntityDamageUtil.attackEntityFromIgnoreIFrame(this.target, source, 2F + level.random.nextInt(2));
                }

                // TODO(phase3-gun-vfx): CE spawns two muzzle-flash particle bursts here - deferred.
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.loaded = tag.getInt("loaded");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("loaded", loaded);
    }

    @Override
    public boolean usesCasings() {
        return true;
    }
}
