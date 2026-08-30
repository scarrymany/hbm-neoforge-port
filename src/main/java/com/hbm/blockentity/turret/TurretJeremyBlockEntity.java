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
 * Ported from CE's {@code TileEntityTurretJeremy} - the heavy artillery-cannon (non-guided)
 * multiblock turret. Ammo: CE's {@code XFactoryTurret.shell_normal}/{@code explosive}/{@code ap}/
 * {@code du}/{@code w9} - not ported yet. CE's own {@code spawnCasing()} override
 * ({@code CasingCreator.composeEffect}, a fancier VFX path than the shared casing-eject default)
 * is not reproduced here - {@code CasingCreator} does not exist in this port either, same deferred
 * gun-VFX substrate as every other turret's muzzle flash.
 */
public class TurretJeremyBlockEntity extends TurretBaseBlockEntity {

    public int timer;
    public int reload;

    public TurretJeremyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected List<BulletConfig> getAmmoList() {
        // TODO(phase3-gun-content): CE uses XFactoryTurret.shell_normal/explosive/ap/du/w9.
        return Collections.emptyList();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.turretJeremy");
    }

    @Override
    public double getDecetorGrace() {
        return 16D;
    }

    @Override
    public double getTurretDepression() {
        return 45D;
    }

    @Override
    public long getMaxPower() {
        return 10_000;
    }

    @Override
    public double getBarrelLength() {
        return 4.25D;
    }

    @Override
    public double getDecetorRange() {
        return 80D;
    }

    @Override
    public void updateEntity() {
        if (reload > 0) reload--;

        if (reload == 1 && level != null) {
            level.playSound(null, worldPosition, HBMSoundHandler.jeremy_reload.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        }

        super.updateEntity();
    }

    @Override
    public void updateFiringTick() {
        timer++;

        if (timer % 40 == 0) {
            BulletConfig conf = getFirstConfigLoaded();

            if (conf != null && level != null) {
                spawnBullet(conf, 50F);
                consumeAmmo(conf.getAmmo());
                level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                        HBMSoundHandler.jeremy_fire.get(), SoundSource.BLOCKS, 4.0F, 1.0F);
                reload = 20;
                // TODO(phase3-gun-vfx): CE spawns a 5-particle muzzle-flash burst here - deferred.
            }
        }
    }

    @Override
    public int casingDelay() {
        return 22;
    }
}
