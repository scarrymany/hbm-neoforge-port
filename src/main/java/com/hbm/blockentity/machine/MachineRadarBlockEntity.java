package com.hbm.blockentity.machine;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.api.entity.IRadarDetectableNT.RadarScanParams;
import com.hbm.api.entity.RadarEntry;
import com.hbm.blockentity.IRadarCommandReceiver;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blockentity.machine.dummyable.RadarScreenBlockEntity;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.machine.RadarMenu;
import com.hbm.items.ISatChip;
import com.hbm.items.tool.ItemCoordinateBase;
import com.hbm.items.tool.MilitaryC2Items;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.saveddata.satellites.SatelliteHorizons;
import com.hbm.saveddata.satellites.SatelliteLaser;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import com.hbm.saveddata.satellites.SatelliteResonator;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import com.hbm.tileentity.IConfigurableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code TileEntityMachineRadarNT.java}:85-89 / :220-258 / :422-424 / :457-472
 * and {@code TileEntityMachineRadarLarge.java:16} (range 3000).
 * radarAltitude 55 / radarBuffer 30 Exact CE :88-89 + :422 + :432.
 * SatelliteRayScan.INFO_RADAR + Detector MEDIUM Exact CE :451-454.
 * Slot 8 linker → screen entries Exact CE :290-304. Inventory 10 Exact CE :118.
 * Slots 0–7 sat_relay / linker launch Exact CE :536-594.
 * Scan toggles / redMode Exact CE :100-104 + :428 + :457-496 + :525-530.
 * Scans {@link IRadarDetectableNT} + players. Map GUI / showMap / clear skipped — no CE {@code gui_radar_nt.png}.
 * {@link IConfigurableMachine} Exact CE {@code radar}/{@code radar_large}
 * ({@code TileEntityMachineRadarNT.java:190-213}, {@code TileEntityMachineRadarLarge.java:19-31}).
 */
public class MachineRadarBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, ITickableBE, MenuProvider, IControlReceiver, IConfigurableMachine {

    public static int maxPower = 100_000;
    public static int consumption = 500;
    public static int radarRange = 1_000;
    public static int radarLargeRange = 3_000;
    public static boolean generateChunks = false;
    public static final int PING_INTERVAL = 80;
    /** CE {@code TileEntityMachineRadarNT.java:88-89}. */
    public static int radarBuffer = 30;
    public static int radarAltitude = 55;
    /** CE {@code TileEntityMachineRadarNT.java:100-104}. */
    public boolean scanMissiles = true;
    public boolean scanShells = true;
    public boolean scanPlayers = true;
    public boolean smartMode = true;
    public boolean redMode = true;

    /** CE {@code TileEntityMachineRadarNT} {@code super(10)}: linker=8, battery=9. */
    public static final int LINKER_SLOT = 8;
    public static final int BATTERY_SLOT = 9;

    private final boolean large;
    private long power;
    private int contacts;
    private int redPower;
    private int pingTimer;
    public final List<RadarEntry> entries = new ArrayList<>();

    public MachineRadarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, false);
    }

    public MachineRadarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean large) {
        super(type, pos, state, 10, true, true);
        this.large = large;
    }

    public int getRange() {
        return large ? radarLargeRange : radarRange;
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
        if (slot == BATTERY_SLOT) return Library.isBattery(stack);
        return true;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            BlockPos target = worldPosition.relative(dir);
            trySubscribe(level, target.getX(), target.getY(), target.getZ(), dir);
        }

        // CE TileEntityMachineRadarNT.java:231 + :239
        power = Library.chargeTEFromItems(inventory, BATTERY_SLOT, power, maxPower);
        power = Library.chargeTEFromItems(inventory, 0, power, maxPower);

        int prevRed = redPower;
        // CE TileEntityMachineRadarNT.java:422 — no consume / scan / sat ping below altitude
        if (worldPosition.getY() < radarAltitude) {
            entries.clear();
            contacts = 0;
            redPower = 0;
        } else if (power >= consumption) {
            power -= consumption;
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

        // CE TileEntityMachineRadarNT.java:290-304
        pushEntriesToScreen();

        dataChanged();
        networkPackMK2(50);
    }

    private void pushEntriesToScreen() {
        ItemStack link = inventory.getStackInSlot(LINKER_SLOT);
        if (link.isEmpty() || link.getItem() != MilitaryC2Items.RADAR_LINKER.get()) return;
        BlockPos target = ItemCoordinateBase.getPosition(link);
        if (target == null) return;
        if (level.getBlockEntity(target) instanceof RadarScreenBlockEntity screen) {
            screen.entries.clear();
            screen.entries.addAll(this.entries);
            screen.refX = worldPosition.getX();
            screen.refY = worldPosition.getY();
            screen.refZ = worldPosition.getZ();
            screen.range = this.getRange();
            screen.linked = true;
            screen.dataChanged();
            screen.networkPackMK2(25);
        }
    }

    private void allocateTargets() {
        entries.clear();
        int range = getRange();
        AABB box = new AABB(
                worldPosition.getX() - range, level.getMinBuildHeight(), worldPosition.getZ() - range,
                worldPosition.getX() + range + 1, level.getMaxBuildHeight() + 1, worldPosition.getZ() + range + 1);
        RadarScanParams params = getScanParams();
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) {
            // CE TileEntityMachineRadarNT.java:432 — must be above radar + buffer
            if (entity.getY() - worldPosition.getY() <= radarBuffer) continue;
            if (entity instanceof IRadarDetectableNT radar) {
                if (!radar.paramsApplicable(params) || !radar.canBeSeenBy(this)) continue;
                double dx = entity.getX() - worldPosition.getX();
                double dz = entity.getZ() - worldPosition.getZ();
                if (dx * dx + dz * dz > (double) range * range) continue;
                entries.add(new RadarEntry(radar, entity, radar.suppliesRedstone(params)));
            } else if (entity instanceof Player player && params.scanPlayers && !player.isSpectator()) {
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

    /** CE {@code TileEntityMachineRadarNT.java:428}. */
    public RadarScanParams getScanParams() {
        return new RadarScanParams(scanMissiles, scanShells, scanPlayers, smartMode);
    }

    /** CE {@code TileEntityMachineRadarNT.java:457-496} proximity vs tier. */
    private int computeRedPower() {
        if (entries.isEmpty()) return 0;
        int powerOut = 0;
        if (redMode) {
            double maxRange = getRange() * Math.sqrt(2D);
            for (RadarEntry e : entries) {
                if (!e.redstone) continue;
                double dist = Math.sqrt(Math.pow(e.posX - worldPosition.getX(), 2) + Math.pow(e.posZ - worldPosition.getZ(), 2));
                int p = 15 - (int) Math.floor(dist / maxRange * 15);
                if (p > powerOut) powerOut = p;
            }
        } else {
            for (RadarEntry e : entries) {
                if (!e.redstone) continue;
                if (e.blipLevel + 1 > powerOut) powerOut = e.blipLevel + 1;
            }
        }
        return powerOut;
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
        return maxPower;
    }

    @Override
    public String getConfigName() {
        return large ? "radar_large" : "radar";
    }

    @Override
    public void readIfPresent(JsonObject obj) {
        if (large) readLarge(obj);
        else readNt(obj);
    }

    @Override
    public void writeConfig(JsonWriter writer) throws IOException {
        if (large) writeLarge(writer);
        else writeNt(writer);
    }

    static void readNt(JsonObject obj) {
        // CE TileEntityMachineRadarNT.java:196-202 — writeConfig omits chunkLoadCap
        maxPower = IConfigurableMachine.grab(obj, "L:powerCap", maxPower);
        consumption = IConfigurableMachine.grab(obj, "L:consumption", consumption);
        radarRange = IConfigurableMachine.grab(obj, "I:radarRange", radarRange);
        radarBuffer = IConfigurableMachine.grab(obj, "I:radarBuffer", radarBuffer);
        radarAltitude = IConfigurableMachine.grab(obj, "I:radarAltitude", radarAltitude);
        generateChunks = IConfigurableMachine.grab(obj, "B:generateChunks", generateChunks);
    }

    static void writeNt(JsonWriter writer) throws IOException {
        // CE TileEntityMachineRadarNT.java:207-212
        writer.name("L:powerCap").value(maxPower);
        writer.name("L:consumption").value(consumption);
        writer.name("I:radarRange").value(radarRange);
        writer.name("I:radarBuffer").value(radarBuffer);
        writer.name("I:radarAltitude").value(radarAltitude);
        writer.name("B:generateChunks").value(generateChunks);
    }

    static void readLarge(JsonObject obj) {
        // CE TileEntityMachineRadarLarge.java:25
        radarLargeRange = IConfigurableMachine.grab(obj, "I:radarLargeRange", radarLargeRange);
    }

    static void writeLarge(JsonWriter writer) throws IOException {
        // CE TileEntityMachineRadarLarge.java:30
        writer.name("I:radarLargeRange").value(radarLargeRange);
    }

    public static final class ConfigDummy implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "radar";
        }

        @Override
        public void readIfPresent(JsonObject obj) {
            readNt(obj);
        }

        @Override
        public void writeConfig(JsonWriter writer) throws IOException {
            writeNt(writer);
        }
    }

    public static final class ConfigDummyLarge implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "radar_large";
        }

        @Override
        public void readIfPresent(JsonObject obj) {
            readLarge(obj);
        }

        @Override
        public void writeConfig(JsonWriter writer) throws IOException {
            writeLarge(writer);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("contacts", contacts);
        tag.putInt("redPower", redPower);
        tag.putBoolean("scanMissiles", scanMissiles);
        tag.putBoolean("scanShells", scanShells);
        tag.putBoolean("scanPlayers", scanPlayers);
        tag.putBoolean("smartMode", smartMode);
        tag.putBoolean("redMode", redMode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        contacts = tag.getInt("contacts");
        redPower = tag.getInt("redPower");
        // hasKey so pre-toggle worlds keep CE defaults (true); CE :397-401 is getBoolean
        if (tag.contains("scanMissiles")) scanMissiles = tag.getBoolean("scanMissiles");
        if (tag.contains("scanShells")) scanShells = tag.getBoolean("scanShells");
        if (tag.contains("scanPlayers")) scanPlayers = tag.getBoolean("scanPlayers");
        if (tag.contains("smartMode")) smartMode = tag.getBoolean("smartMode");
        if (tag.contains("redMode")) redMode = tag.getBoolean("redMode");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(contacts);
        buf.writeInt(redPower);
        buf.writeBoolean(scanMissiles);
        buf.writeBoolean(scanShells);
        buf.writeBoolean(scanPlayers);
        buf.writeBoolean(smartMode);
        buf.writeBoolean(redMode);
        buf.writeInt(entries.size());
        for (RadarEntry entry : entries) entry.toBytes(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        contacts = buf.readInt();
        redPower = buf.readInt();
        scanMissiles = buf.readBoolean();
        scanShells = buf.readBoolean();
        scanPlayers = buf.readBoolean();
        smartMode = buf.readBoolean();
        redMode = buf.readBoolean();
        int count = buf.readInt();
        entries.clear();
        for (int i = 0; i < count; i++) {
            RadarEntry entry = new RadarEntry();
            entry.fromBytes(buf);
            entries.add(entry);
        }
    }

    @Override
    public boolean hasPermission(Player player) {
        return isUseableByPlayer(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        // CE TileEntityMachineRadarNT.java:519 — empty; launch is player overload
    }

    @Override
    public void receiveControl(ServerPlayer player, CompoundTag data) {
        // CE TileEntityMachineRadarNT.java:525-530. map/clear/gui1 skipped — no map png / slots already here.
        if (data.contains("missiles")) this.scanMissiles = !this.scanMissiles;
        if (data.contains("shells")) this.scanShells = !this.scanShells;
        if (data.contains("players")) this.scanPlayers = !this.scanPlayers;
        if (data.contains("smart")) this.smartMode = !this.smartMode;
        if (data.contains("red")) this.redMode = !this.redMode;

        if (data.contains("link") && level != null) {
            int id = data.getInt("link");
            if (id < 0 || id > 7) return;
            ItemStack link = inventory.getStackInSlot(id);

            if (!link.isEmpty() && link.getItem() == satRelayItem()) {
                Satellite sat = SatelliteSavedData.getData(level).getSatFromFreq(ISatChip.getFreqS(link));
                if (sat instanceof SatelliteLaser && data.contains("launchPosX")) {
                    int x = data.getInt("launchPosX");
                    int z = data.getInt("launchPosZ");
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            HBMSoundHandler.techBleep.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
                    sat.onClick(level, player, x, z);
                }
                if (sat instanceof SatelliteHorizons && data.contains("launchPosX")) {
                    int x = data.getInt("launchPosX");
                    int z = data.getInt("launchPosZ");
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            HBMSoundHandler.techBleep.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
                    sat.onCoordAction(level, player, x, 60, z);
                }
                if (sat instanceof SatelliteResonator && data.contains("launchPosX")) {
                    int x = data.getInt("launchPosX");
                    int z = data.getInt("launchPosZ");
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            HBMSoundHandler.techBleep.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
                    sat.onCoordAction(level, player, x, y, z);
                }
            }
            if (!link.isEmpty() && link.getItem() == MilitaryC2Items.RADAR_LINKER.get()) {
                BlockPos target = ItemCoordinateBase.getPosition(link);
                if (target != null) {
                    BlockEntity tile = level.getBlockEntity(target);
                    if (tile instanceof IRadarCommandReceiver rec) {
                        if (data.contains("launchEntity")) {
                            Entity entity = level.getEntity(data.getInt("launchEntity"));
                            if (entity != null && rec.sendCommandEntity(entity)) {
                                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                        HBMSoundHandler.techBleep.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                            }
                        } else if (data.contains("launchPosX")) {
                            int x = data.getInt("launchPosX");
                            int z = data.getInt("launchPosZ");
                            if (rec.sendCommandPosition(x, target.getY(), z)) {
                                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                        HBMSoundHandler.techBleep.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                            }
                        }
                    }
                }
            }
        }
    }

    private static Item satRelayItem() {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "sat_relay"));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RadarMenu(containerId, playerInventory, this);
    }
}
