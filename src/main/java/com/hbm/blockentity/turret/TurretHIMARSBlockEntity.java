package com.hbm.blockentity.turret;

import com.hbm.entity.projectile.EntityArtilleryRocket;
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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * CE {@code TileEntityTurretHIMARS}: AUTO/MANUAL, crane reload, 40t delay, V0 25.
 * TODO(CE: TileEntityTurretHIMARS.java:382-388): OpenComputers addCoords.
 * TODO(CE: RenderTurretHIMARS.java:1): crane/tube OBJ TESR.
 */
public class TurretHIMARSBlockEntity extends TurretBaseArtilleryBlockEntity {

    public enum FiringMode {
        AUTO,
        MANUAL;
        public static final FiringMode[] VALUES = values();
    }

    private static final int FIRE_DELAY_TICKS = 40;

    public FiringMode mode = FiringMode.AUTO;
    public int typeLoaded = -1;
    public int ammo;
    public float crane;
    public float lastCrane;
    private int firingTimer;

    public TurretHIMARSBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.turretHIMARS");
    }

    @Override
    public long getMaxPower() {
        return 1_000_000;
    }

    @Override
    public double getBarrelLength() {
        return 0.5D;
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 5D;
    }

    @Override
    public double getHeightOffset() {
        return 5D;
    }

    @Override
    public double getDecetorRange() {
        return 5000D;
    }

    @Override
    public double getDecetorGrace() {
        return 250D;
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
    public boolean doLOSCheck() {
        return false;
    }

    @Override
    protected List<BulletConfig> getAmmoList() {
        return Collections.emptyList();
    }

    @Override
    protected void alignTurret() {
        if (tPos == null) return;
        Vec3 pos = getTurretPos();
        Vec3 delta = new Vec3(tPos.x - pos.x, tPos.y - pos.y, tPos.z - pos.z);
        turnTowardsAngle(Math.PI / 4D, -Math.atan2(delta.x, delta.z));
    }

    @Nullable
    private ItemStack getSpareRocket() {
        for (int i = 1; i < 10; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && ArtilleryAmmo.isHimarsRocket(stack.getItem())) return stack;
        }
        return null;
    }

    public boolean hasAmmo() {
        return typeLoaded >= 0 && ammo > 0;
    }

    @Override
    public void updateEntity() {
        if (level == null) return;

        if (level.isClientSide) {
            lastRotationPitch = rotationPitch;
            lastRotationYaw = rotationYaw;
            lastCrane = crane;
            rotationPitch = syncRotationPitch;
            rotationYaw = syncRotationYaw;
        }

        if (!level.isClientSide) {
            if (mode == FiringMode.MANUAL) {
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
            if (target != null && mode != FiringMode.MANUAL && !entityInLOS(target)) {
                target = null;
            }
            if (target != null) {
                tPos = getEntityPos(target);
            } else if (mode != FiringMode.MANUAL) {
                tPos = null;
            }

            if (isOn() && hasPower()) {
                if (!hasAmmo() || crane > 0) {
                    turnTowardsAngle(0, rotationYaw);
                    if (aligned) {
                        if (hasAmmo()) {
                            crane -= 0.0125F;
                        } else {
                            crane += 0.0125F;
                            if (crane >= 1F) {
                                ItemStack available = getSpareRocket();
                                if (available != null) {
                                    typeLoaded = ArtilleryAmmo.typeOfHimars(available.getItem());
                                    ammo = ArtilleryAmmo.himarsAmount(typeLoaded);
                                    available.shrink(1);
                                    setChanged();
                                }
                            }
                        }
                    }
                    crane = Mth.clamp(crane, 0F, 1F);
                } else if (tPos != null) {
                    alignTurret();
                }
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
                    if (target == null && mode != FiringMode.MANUAL) seekNewTarget();
                }
            } else {
                searchTimer = 0;
            }

            if (aligned && crane <= 0) updateFiringTick();

            setPower(Library.chargeTEFromItems(inventory, 10, getPower(), getMaxPower()));
            networkPackNT(250);
        } else if (Math.abs(lastRotationYaw - rotationYaw) > Math.PI) {
            if (lastRotationYaw < rotationYaw) lastRotationYaw += Math.PI * 2;
            else lastRotationYaw -= Math.PI * 2;
        }
    }

    @Override
    public void updateFiringTick() {
        if (++firingTimer % FIRE_DELAY_TICKS != 0) return;
        if (hasAmmo() && tPos != null && level != null) {
            spawnShell(typeLoaded);
            ammo--;
            level.playSound(null, worldPosition, HBMSoundHandler.rocketFlame.get(), SoundSource.BLOCKS, 25.0F, 1.0F);
        }
        if (mode == FiringMode.MANUAL && !targetQueue.isEmpty()) {
            targetQueue.removeFirst();
            tPos = null;
        }
    }

    public void spawnShell(int type) {
        if (level == null) return;
        Vec3 pos = getTurretPos();
        Vec3 vec = new Vec3(getBarrelLength(), 0, 0);
        vec = Vec3dUtil.rotateRoll(vec, (float) -rotationPitch);
        vec = vec.yRot((float) -(rotationYaw + Math.PI * 0.5));
        EntityArtilleryRocket proj = new EntityArtilleryRocket(
                com.hbm.entity.projectile.Phase9TailEntityTypes.ARTILLERY_ROCKET.get(), level);
        proj.setPos(pos.x + vec.x, pos.y + vec.y, pos.z + vec.z);
        proj.shoot(vec.x, vec.y, vec.z, 25F, 0.0F);
        if (target != null) proj.setTarget(target);
        else if (tPos != null) proj.setTarget(tPos.x, tPos.y, tPos.z);
        proj.setType(type);
        level.addFreshEntity(proj);
    }

    @Override
    public void handleButtonPacket(int value, int meta) {
        if (meta == 5) {
            mode = FiringMode.VALUES[(mode.ordinal() + 1) % FiringMode.VALUES.length];
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
        buf.writeShort(mode.ordinal());
        buf.writeShort(typeLoaded);
        buf.writeInt(ammo);
        buf.writeFloat(crane);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        mode = FiringMode.VALUES[buf.readShort()];
        typeLoaded = buf.readShort();
        ammo = buf.readInt();
        crane = buf.readFloat();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mode = FiringMode.VALUES[tag.getShort("mode")];
        typeLoaded = tag.getInt("type");
        ammo = tag.getInt("ammo");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putShort("mode", (short) mode.ordinal());
        tag.putInt("type", typeLoaded);
        tag.putInt("ammo", ammo);
    }
}
