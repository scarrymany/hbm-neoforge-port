package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * CE {@code TileEntityMachineTeleporter}. Energy + AABB teleport when target set.
 * TODO(CE: ItemTeleLink.java:38-45): ItemTeleLink not ported — target stays null until that item lands.
 */
public class MachineTeleporterBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE {

    public static final int consumption = 100_000_000;
    public static final int maxPower = 1_000_000_000;

    public long power;
    public BlockPos target;
    public boolean linked;
    public boolean prevLinked;
    public byte packageTimer;

    public MachineTeleporterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0, false, true);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineTeleporter");
    }

    @Override
    public void updateEntity() {
        if (level == null) return;
        boolean bounced = false;
        packageTimer++;
        if (!level.isClientSide) {
            for (Direction dir : Direction.values()) {
                trySubscribe(level, worldPosition.relative(dir), dir);
            }
            List<Entity> entities = level.getEntities(null, new AABB(
                    worldPosition.getX() - 0.25, worldPosition.getY(), worldPosition.getZ() - 0.25,
                    worldPosition.getX() + 0.75, worldPosition.getY() + 2, worldPosition.getZ() + 0.75));
            for (Entity e : entities) {
                if (e.tickCount >= 10) {
                    teleport(e);
                    bounced = true;
                }
            }
            networkPack();
            prevLinked = linked;
            dataChanged();
        }
        if (bounced) {
            level.addParticle(ParticleTypes.PORTAL,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5,
                    0.0D, 0.1D, 0.5D);
        }
    }

    private void networkPack() {
        if (linked != prevLinked || packageTimer == 0) {
            networkPackMK2(150);
            packageTimer = 40;
        }
    }

    public void teleport(Entity entity) {
        if (this.power < consumption) return;
        level.playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (target == null) return;

        double x = target.getX() + 0.5D;
        double y = target.getY() + 1.6D;
        double z = target.getZ() + 0.5D;
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(x, y, z);
        } else {
            entity.teleportTo(x, y, z);
        }
        level.playSound(null, target, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);
        this.power -= consumption;
    }

    @Override
    public void setPower(long i) {
        power = i;
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public long getMaxPower() {
        return maxPower;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        if (target != null) {
            tag.putBoolean("hastarget", true);
            tag.putInt("x1", target.getX());
            tag.putInt("y1", target.getY());
            tag.putInt("z1", target.getZ());
        } else {
            tag.putBoolean("hastarget", false);
        }
        tag.putBoolean("linked", linked);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        if (tag.getBoolean("hastarget")) {
            target = new BlockPos(tag.getInt("x1"), tag.getInt("y1"), tag.getInt("z1"));
        }
        linked = tag.getBoolean("linked");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        if (this.target != null) {
            buf.writeBoolean(true);
            buf.writeInt(this.target.getX());
            buf.writeInt(this.target.getY());
            buf.writeInt(this.target.getZ());
        } else {
            buf.writeBoolean(false);
        }
        buf.writeBoolean(this.linked);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        if (buf.readBoolean()) {
            this.target = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        } else {
            this.target = null;
        }
        this.linked = buf.readBoolean();
    }
}
