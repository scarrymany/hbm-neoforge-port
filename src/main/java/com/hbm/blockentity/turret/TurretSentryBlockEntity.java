package com.hbm.blockentity.turret;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

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

                // TODO(phase3-gun-vfx): CE spawns a muzzle-flash particle burst here (AuxParticlePacketNT
                // / HbmEffectNT.VanillaExt_LargeExplode) - deferred shared gun-VFX substrate.

                if (shotSide) this.didJustShootLeft = true;
                else this.didJustShootRight = true;
                shotSide = !shotSide;
            }
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
