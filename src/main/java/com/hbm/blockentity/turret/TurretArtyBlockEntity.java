package com.hbm.blockentity.turret;

import com.hbm.entity.projectile.EntityArtilleryShell;
import com.hbm.items.weapon.ArtilleryAmmo;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.util.Vec3dUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * CE {@code TileEntityTurretArty}: modes artillery/cannon/manual, V0 50/20, delays 300/40.
 * TODO(CE: TileEntityTurretArty.java:168): alignTurret debug logger — not copied.
 * TODO(CE: TileEntityTurretArty.java:228-266): AudioWrapper / CasingCreator / AuxParticlePacketNT.
 * TODO(CE: TileEntityTurretArty.java:218-222): cargo NBT shell meta 8 — ammo not registered.
 * TODO(CE: TileEntityTurretArty.java:457-493): OpenComputers addCoords.
 */
public class TurretArtyBlockEntity extends TurretBaseArtilleryBlockEntity {

    public static final short MODE_ARTILLERY = 0;
    public static final short MODE_CANNON = 1;
    public static final short MODE_MANUAL = 2;

    public short mode = MODE_ARTILLERY;
    public double barrelPos;
    public double lastBarrelPos;
    private int timer;
    private boolean didJustShoot;
    private boolean retracting;

    public TurretArtyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.turretArty");
    }

    @Override
    public long getMaxPower() {
        return 100_000;
    }

    @Override
    public double getBarrelLength() {
        return 9D;
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 0;
    }

    @Override
    public double getHeightOffset() {
        return 3D;
    }

    @Override
    public double getDecetorRange() {
        return mode == MODE_CANNON ? 250D : 3000D;
    }

    @Override
    public double getDecetorGrace() {
        return mode == MODE_CANNON ? 32D : 250D;
    }

    @Override
    public double getTurretYawSpeed() {
        return 1D;
    }

    @Override
    public double getTurretPitchSpeed() {
        return 0.5D;
    }

    @Override
    public double getTurretDepression() {
        return 30D;
    }

    @Override
    public double getTurretElevation() {
        return 90D;
    }

    @Override
    public int getDecetorInterval() {
        return mode == MODE_CANNON ? 20 : 200;
    }

    @Override
    public boolean doLOSCheck() {
        return mode == MODE_CANNON;
    }

    public double getV0() {
        return mode == MODE_CANNON ? 20D : 50D;
    }

    @Override
    protected List<BulletConfig> getAmmoList() {
        return Collections.emptyList();
    }

    @Override
    protected void alignTurret() {
        if (tPos == null) return;
        Vec3 pos = getTurretPos();
        Vec3 barrel = new Vec3(getBarrelLength(), 0, 0);
        barrel = Vec3dUtil.rotateRoll(barrel, (float) rotationPitch);
        barrel = barrel.yRot((float) -(rotationYaw + Math.PI * 0.5));
        double newX = pos.x + barrel.x;
        double newY = pos.y + barrel.y;
        double newZ = pos.z + barrel.z;
        Vec3 delta = new Vec3(tPos.x - newX, tPos.y - newY, tPos.z - newZ);
        double targetYaw = -Math.atan2(delta.x, delta.z);
        double x = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double y = delta.y;
        double v0 = getV0();
        double v02 = v0 * v0;
        double g = 9.81 * 0.05;
        double upperLower = mode == MODE_CANNON ? -1 : 1;
        double disc = v02 * v02 - g * (g * x * x + 2 * y * v02);
        double targetPitch = x == 0 || disc < 0
                ? Math.atan2(delta.y, x)
                : Math.atan((v02 + Math.sqrt(disc) * upperLower) / (g * x));
        turnTowardsAngle(targetPitch, targetYaw);
    }

    @Nullable
    public ItemStack getShellLoaded() {
        for (int i = 1; i < 10; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && ArtilleryAmmo.isArtyShell(stack.getItem())) return stack;
        }
        return null;
    }

    public void consumeShell(Item ammo) {
        for (int i = 1; i < 10; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == ammo) {
                stack.shrink(1);
                setChanged();
                return;
            }
        }
    }

    public void spawnShell(ItemStack type) {
        if (level == null || tPos == null) return;
        Vec3 pos = getTurretPos();
        Vec3 vec = new Vec3(getBarrelLength(), 0, 0);
        vec = Vec3dUtil.rotateRoll(vec, (float) -rotationPitch);
        vec = vec.yRot((float) -(rotationYaw + Math.PI * 0.5));
        EntityArtilleryShell proj = new EntityArtilleryShell(
                com.hbm.entity.projectile.Phase9TailEntityTypes.ARTILLERY_SHELL.get(), level);
        proj.setPos(pos.x + vec.x, pos.y + vec.y, pos.z + vec.z);
        proj.shoot(vec.x, vec.y, vec.z, (float) getV0(), 0.0F);
        proj.setTarget(tPos.x, tPos.y, tPos.z);
        proj.setType(ArtilleryAmmo.typeOfArty(type.getItem()));
        if (ArtilleryAmmo.typeOfArty(type.getItem()) == ArtilleryAmmo.ARTY_CARGO) {
            proj.setCargo(ArtilleryAmmo.getCargo(type, level.registryAccess()));
        }
        if (mode != MODE_CANNON) proj.setWhistle(true);
        level.addFreshEntity(proj);
        casingDelay = casingDelay();
    }

    @Override
    public int casingDelay() {
        return 7;
    }

    @Override
    public void updateEntity() {
        if (level == null) return;

        if (level.isClientSide) {
            lastBarrelPos = barrelPos;
            if (retracting) {
                barrelPos += 0.5;
                if (barrelPos >= 1) retracting = false;
            } else {
                barrelPos -= 0.05;
                if (barrelPos < 0) barrelPos = 0;
            }
            lastRotationPitch = rotationPitch;
            lastRotationYaw = rotationYaw;
            rotationPitch = syncRotationPitch;
            rotationYaw = syncRotationYaw;
        }

        if (!level.isClientSide) {
            if (mode == MODE_MANUAL) {
                if (!targetQueue.isEmpty()) tPos = targetQueue.getFirst();
            } else {
                targetQueue.clear();
            }

            aligned = false;
            updateConnections();

            if (target != null && !target.isAlive()) {
                target = null;
                stattrak++;
            }
            if (target != null && mode != MODE_MANUAL && !entityInLOS(target)) {
                target = null;
            }
            if (target != null) {
                tPos = getEntityPos(target);
            } else if (mode != MODE_MANUAL) {
                tPos = null;
            }

            if (isOn() && hasPower()) {
                if (tPos != null) alignTurret();
            } else {
                target = null;
                tPos = null;
            }
            if (!isOn()) targetQueue.clear();

            if (target != null && !target.isAlive()) {
                target = null;
                tPos = null;
                stattrak++;
            }

            if (isOn() && hasPower()) {
                searchTimer--;
                setPower(getPower() - getConsumption());
                if (searchTimer <= 0) {
                    searchTimer = getDecetorInterval();
                    if (target == null && mode != MODE_MANUAL) seekNewTarget();
                }
            } else {
                searchTimer = 0;
            }

            if (aligned) updateFiringTick();

            setPower(Library.chargeTEFromItems(inventory, 10, getPower(), getMaxPower()));
            networkPackNT(250);
            didJustShoot = false;
            if (casingDelay > 0) casingDelay--;
            else spawnCasing();
        } else if (Math.abs(lastRotationYaw - rotationYaw) > Math.PI) {
            if (lastRotationYaw < rotationYaw) lastRotationYaw += Math.PI * 2;
            else lastRotationYaw -= Math.PI * 2;
        }
    }

    @Override
    public void updateFiringTick() {
        timer++;
        int delay = mode == MODE_ARTILLERY ? 300 : 40;
        if (timer % delay != 0) return;

        ItemStack conf = getShellLoaded();
        if (conf != null && level != null) {
            spawnShell(conf);
            consumeShell(conf.getItem());
            level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    HBMSoundHandler.jeremy_fire.get(), SoundSource.BLOCKS, 25.0F, 1.0F);
            didJustShoot = true;
        }
        if (mode == MODE_MANUAL && !targetQueue.isEmpty()) {
            targetQueue.removeFirst();
            tPos = null;
        }
    }

    @Override
    public void handleButtonPacket(int value, int meta) {
        if (meta == 5) {
            mode++;
            if (mode > 2) mode = 0;
            tPos = null;
            targetQueue.clear();
            setChanged();
            return;
        }
        super.handleButtonPacket(value, meta);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeShort(mode);
        buf.writeBoolean(didJustShoot);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        mode = buf.readShort();
        retracting = buf.readBoolean();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mode = tag.getShort("mode");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putShort("mode", mode);
    }
}
