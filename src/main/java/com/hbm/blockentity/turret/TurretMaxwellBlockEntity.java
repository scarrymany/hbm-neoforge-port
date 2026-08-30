package com.hbm.blockentity.turret;

import com.hbm.damage.ModDamageTypes;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.List;

/**
 * Ported from CE's {@code TileEntityTurretMaxwell} - the microwave death-ray turret. Direct-damage
 * turret with no projectile at all, using the already-registered {@link ModDamageTypes#MICROWAVE}.
 * <p>
 * <b>Scope trim vs. CE, documented</b>: CE scales this turret's damage/range/consumption/afterburn
 * off a 27-slot upgrade-item inventory scan ({@code ModItems.upgrade_speed_1..3}/{@code effect_1..3}/
 * {@code power_1..3}/{@code afterburn_1..3}/{@code overdrive_1..3}/{@code upgrade_5g}/
 * {@code upgrade_screm}), an {@code IUpgradeInfoProvider} tooltip contract, and the
 * {@code HbmPotion.death} "5G" effect - <b>none of {@code ItemMachineUpgrade}'s concrete upgrade
 * items, {@code IUpgradeInfoProvider}, or {@code HbmPotion} exist anywhere in this port yet</b> (a
 * genuinely blocking dependency this task's report did not name - confirmed by search). Rather than
 * fabricate that item family, this port keeps every upgrade-level field permanently at its
 * zero-upgrade default (CE's own base-case behavior with an empty upgrade inventory: flat 0.25
 * damage, flat 64m range, flat 10,000 HE consumption, no afterburn, no "5G" effect, no scream sound)
 * - the direct-damage attack loop itself is fully ported and correct, only the upgrade-scaling input
 * is unavailable. Fill in the upgrade-item scan loop once that family lands; nothing else here needs
 * to change.
 */
public class TurretMaxwellBlockEntity extends TurretBaseBlockEntity {

    public TurretMaxwellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected List<BulletConfig> getAmmoList() {
        return Collections.emptyList();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.turretMaxwell");
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 2;
    }

    @Override
    public double getDecetorGrace() {
        return 5D;
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
        return 40D;
    }

    @Override
    public double getTurretDepression() {
        return 35D;
    }

    @Override
    public double getDecetorRange() {
        // CE: 64D + greenLevel * 3 (greenLevel is an upgrade-item scan result - see class javadoc).
        return 64D;
    }

    @Override
    public long getMaxPower() {
        return 10_000_000;
    }

    @Override
    public long getConsumption() {
        // CE: _5g ? 10 : 10000 - blueLevel * 300L (both upgrade-gated - see class javadoc).
        return 10_000;
    }

    @Override
    public double getBarrelLength() {
        return 2.125D;
    }

    @Override
    public double getHeightOffset() {
        return 2D;
    }

    @Override
    public void updateFiringTick() {
        if (level == null) return;

        long demand = getConsumption() * 10;

        if (this.target != null && getPower() >= demand) {
            if (this.target instanceof Player player && (player.isCreative() || player.isSpectator())) return;

            // CE: "5G" mode (upgrade-gated, see class javadoc) applies HbmPotion.death instead of direct
            // damage - HbmPotion doesn't exist in this port, so that branch is dropped; the direct-damage
            // path below (redLevel/blackLevel always 0) is CE's own base-case behavior.
            DamageSource source = level.damageSources().source(ModDamageTypes.MICROWAVE);
            EntityDamageUtil.attackEntityFromIgnoreIFrame(this.target, source, 0.25F);

            if (!this.target.isAlive()) {
                // TODO(phase3-gun-vfx): CE spawns a "Giblets" particle burst on kill here - deferred.
                // CE's own "screm" sound branch is upgrade-gated (see class javadoc) and always false
                // here; SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR (1.21.1 Mojang-mapped name for CE's
                // ENTITY_ZOMBIE_BREAK_DOOR_WOOD) is not independently confirmed in this sandbox -
                // double check against the real constant on first build.
                level.playSound(null, this.target.getX(), this.target.getY(), this.target.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 2.0F, 0.95F + level.random.nextFloat() * 0.2F);
            }

            setPower(getPower() - demand);
            networkPackNT(250);
        }
    }
}
