package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.api.entity.IRadarDetectableNT.RadarScanParams;
import com.hbm.api.entity.RadarEntry;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.RadarMenu;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code TileEntityMachineRadarNT.java}:85-89 / :220-258 / :422-424 / :457-472
 * and {@code TileEntityMachineRadarLarge.java:16} (range 3000).
 * radarAltitude 55 / radarBuffer 30 Exact CE :88-89 + :422 + :432.
 * SatelliteRayScan.INFO_RADAR + Detector MEDIUM Exact CE :451-454.
 * Scans {@link IRadarDetectableNT} + players. Map GUI skipped — no CE {@code gui_radar_nt.png}.
 */
public class MachineRadarBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 100_000L;
    public static final int CONSUMPTION = 500;
    public static final int RANGE = 1_000;
    public static final int RANGE_LARGE = 3_000;
    public static final int PING_INTERVAL = 80;
    /** CE {@code TileEntityMachineRadarNT.java:88-89}. */
    public static int radarBuffer = 30;
    public static int radarAltitude = 55;
    public static final RadarScanParams SCAN_PARAMS = new RadarScanParams(true, true, true, true);

    public static final int BATTERY_SLOT = 0;

    private final boolean large;
    private long power;
    private int contacts;
    private int redPower;
    private int pingTimer;
    private final List<RadarEntry> entries = new ArrayList<>();

    public MachineRadarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, false);
    }

    public MachineRadarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean large) {
        super(type, pos, state, 1, true, true);
        this.large = large;
    }

    public int getRange() {
        return large ? RANGE_LARGE : RANGE;
    }

    public int getContacts() {
        return contacts;
    }

    public int getRedPower() {
        return redPower;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(large ? "container.machineRadarLarge" : "container.machineRadar");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == BATTERY_SLOT && Library.isBattery(stack);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            BlockPos target = worldPosition.relative(dir);
            trySubscribe(level, target.getX(), target.getY(), target.getZ(), dir);
        }

        power = Library.chargeTEFromItems(inventory, BATTERY_SLOT, power, MAX_POWER);

        int prevRed = redPower;
        // CE TileEntityMachineRadarNT.java:422 — no consume / scan / sat ping below altitude
        if (worldPosition.getY() < radarAltitude) {
            entries.clear();
            contacts = 0;
            redPower = 0;
        } else if (power >= CONSUMPTION) {
            power -= CONSUMPTION;
            allocateTargets();
            pingTimer++;
            if (pingTimer >= PING_INTERVAL) {
                pingTimer = 0;
                level.playSound(null, worldPosition, HBMSoundHandler.sonarPing.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        } else {
            entries.clear();
            contacts = 0;
            redPower = 0;
        }

        if (prevRed != redPower) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }

        dataChanged();
        networkPackMK2(50);
    }

    private void allocateTargets() {
        entries.clear();
        int range = getRange();
        AABB box = new AABB(
                worldPosition.getX() - range, level.getMinBuildHeight(), worldPosition.getZ() - range,
                worldPosition.getX() + range + 1, level.getMaxBuildHeight() + 1, worldPosition.getZ() + range + 1);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) {
            // CE TileEntityMachineRadarNT.java:432 — must be above radar + buffer
            if (entity.getY() - worldPosition.getY() <= radarBuffer) continue;
            if (entity instanceof IRadarDetectableNT radar) {
                if (!radar.paramsApplicable(SCAN_PARAMS) || !radar.canBeSeenBy(this)) continue;
                double dx = entity.getX() - worldPosition.getX();
                double dz = entity.getZ() - worldPosition.getZ();
                if (dx * dx + dz * dz > (double) range * range) continue;
                entries.add(new RadarEntry(radar, entity, radar.suppliesRedstone(SCAN_PARAMS)));
            } else if (entity instanceof Player player && SCAN_PARAMS.scanPlayers && !player.isSpectator()) {
                double dx = player.getX() - worldPosition.getX();
                double dz = player.getZ() - worldPosition.getZ();
                if (dx * dx + dz * dz > (double) range * range) continue;
                entries.add(new RadarEntry(player));
            }
        }
        contacts = entries.size();
        redPower = computeRedPower();

        // CE TileEntityMachineRadarNT.java:451-454 — after a powered scan
        if (level.getGameTime() % 20 == 0) {
            SatelliteDetector.reportEvent(level, SatelliteDetector.DURATION_MEDIUM,
                    SatelliteDetector.BurstIntensity.MEDIUM, worldPosition.getX(), worldPosition.getZ());
            SatelliteRayScan.reportEvent(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    SatelliteRayScan.RayEvent.INFO_RADAR, 200);
        }
    }

    /** CE TileEntityMachineRadarNT.java:457-472 proximity mode. */
    private int computeRedPower() {
        if (entries.isEmpty()) return 0;
        double maxRange = getRange() * Math.sqrt(2D);
        int powerOut = 0;
        for (RadarEntry e : entries) {
            if (!e.redstone) continue;
            double dist = Math.sqrt(Math.pow(e.posX - worldPosition.getX(), 2) + Math.pow(e.posZ - worldPosition.getZ(), 2));
            int p = 15 - (int) Math.floor(dist / maxRange * 15);
            if (p > powerOut) powerOut = p;
        }
        return Math.max(0, Math.min(15, powerOut));
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long i) {
        power = i;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("contacts", contacts);
        tag.putInt("redPower", redPower);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        contacts = tag.getInt("contacts");
        redPower = tag.getInt("redPower");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(contacts);
        buf.writeInt(redPower);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        contacts = buf.readInt();
        redPower = buf.readInt();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RadarMenu(containerId, playerInventory, this);
    }
}
