package com.hbm.saveddata.satellites;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.blockentity.network.RTTYSystem;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.saveddata.satellites.Satellite} (266 lines, read in full) - the
 * satellite addressing/dispatch protocol registry, per {@code docs/phase3/missile_launch_infra.md}'s
 * headline finding #6. A satellite is identified by an arbitrary integer "frequency" chosen by the
 * player when it's launched, stored in {@link SatelliteSavedData} (per-world, keyed by that
 * frequency) rather than per-chunk/per-block state.
 *
 * <p><b>Item registry mapping - adapted, not copied verbatim.</b> CE's {@code itemToClass}/
 * {@code metaToClass} maps distinguish "legacy" {@code ItemSatChip} instances (one Java class per
 * satellite, e.g. {@code ModItems.sat_mapper}) from the newer metadata-based {@code ItemEnumMulti}
 * ({@code ModItems.satellite}, one metadata value per {@link EnumSatType}). This port already
 * flattened that {@code ItemEnumMulti} into distinct {@code ItemSatellite} instances per
 * {@link EnumSatType} (see {@code com.hbm.items.machine.MachineItems}'s "satellite_" + type registrations,
 * confirmed real) - so {@code metaToClass} here is keyed by {@link EnumSatType} directly rather than
 * by an ordinal int, and {@link #getClassFromStack} checks {@code stack.getItem() instanceof
 * ItemSatellite} instead of an identity check against one shared multi-item.
 *
 * <p><b>Registration order is load-bearing</b>: {@link #satellites}' list index is the persisted
 * satellite id (an NBT-stored int, see {@link SatelliteSavedData}) - this order must never change
 * once players have real save data, exactly matching CE's own inline warning.
 */
public abstract class Satellite {

    public static final List<Class<? extends Satellite>> satellites = new ArrayList<>();
    public static final Map<Item, Class<? extends Satellite>> itemToClass = new HashMap<>();
    public static final Map<EnumSatType, Class<? extends Satellite>> typeToClass = new HashMap<>();

    public static final String CHAN_SATLINK = "SAT_LINK";

    public static final String CMD_SETTARGET = "settarget";
    public static final String CMD_GETTARGET = "gettarget";
    public static final String CMD_GETTARGETX = "gettargetx";
    public static final String CMD_GETTARGETZ = "gettargetz";

    public int targetX;
    public int targetZ;

    public String tx = "";

    public enum InterfaceActions {
        HAS_MAP,     // lets the interface display loaded chunks
        CAN_CLICK,   // enables onClick events
        SHOW_COORDS, // enables coordinates as a mouse tooltip
        HAS_RADAR,   // lets the interface display loaded entities
        HAS_ORES     // like HAS_MAP but only shows ores
    }

    public enum CoordActions {
        HAS_Y // enables the Y-coord field which is disabled by default
    }

    public enum Interfaces {
        NONE,      // does not interact with any sat interface (i.e. asteroid miners)
        SAT_PANEL, // allows interaction with the sat interface panel (for graphical applications)
        SAT_COORD  // allows interaction with the sat coord remote (for teleportation or other coord related actions)
    }

    public final List<InterfaceActions> ifaceAcs = new ArrayList<>();
    public final List<CoordActions> coordAcs = new ArrayList<>();
    public Interfaces satIface = Interfaces.NONE;

    /**
     * Registers every concrete {@link Satellite} against the item that launches it, in a fixed,
     * never-to-be-reordered index. Mirrors CE's own {@code register()} exactly - see this class's
     * javadoc for the {@code itemToClass}/{@code typeToClass} adaptation.
     */
    public static void register() {
        // the list index is the persisted satellite id, so this order must never change
        registerSatellite(SatelliteMapper.class, EnumSatType.SPY, null);
        registerSatellite(SatelliteScanner.class, EnumSatType.SCANNER, null);
        registerSatellite(SatelliteRadar.class, EnumSatType.RADAR, null);
        registerSatellite(SatelliteLaser.class, EnumSatType.DEATH_RAY, null);
        registerSatellite(SatelliteResonator.class, EnumSatType.XENIUM_RESONATOR, null);
        registerSatellite(SatelliteRelay.class, EnumSatType.RELAY, null);
        registerSatellite(SatelliteMiner.class, EnumSatType.MINER_ASTRO, null);
        registerSatellite(SatelliteLunarMiner.class, EnumSatType.MINER_LUNAR, null);
        registerSatellite(SatelliteHorizons.class, null, null);
        registerSatellite(SatelliteDetector.class, EnumSatType.DETECTOR, null);
        registerSatellite(SatellitePrecisionLaser.class, EnumSatType.PRECISION_LASER, null);
        registerSatellite(SatelliteRayScan.class, EnumSatType.RAY_SCAN, null);
        registerSatellite(SatelliteScience.class, EnumSatType.SCIENCE, null);
    }

    private static void registerSatellite(Class<? extends Satellite> sat, EnumSatType type, Item legacy) {
        satellites.add(sat);
        if (type != null) typeToClass.put(type, sat);
        if (legacy != null) itemToClass.put(legacy, sat);
    }

    public static Class<? extends Satellite> getClassFromStack(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof com.hbm.items.machine.ItemSatellite satItem) {
            return typeToClass.get(satItem.getType());
        }
        return itemToClass.get(stack.getItem());
    }

    public static void orbit(Level level, int id, int freq, double x, double y, double z) {
        orbit(level, id, ItemStack.EMPTY, freq, x, y, z);
    }

    public static void orbit(Level level, int id, ItemStack part, int freq, double x, double y, double z) {
        if (level.isClientSide()) return;

        SatelliteSavedData data = SatelliteSavedData.getData(level);
        Satellite existing = data.getSatFromFreq(freq);

        if (existing != null) {
            existing.onPartDelivered(level, part);
            data.setDirty();
            return;
        }

        Satellite sat = create(id);
        if (sat != null) {
            data.sats.put(freq, sat);
            sat.setTarget((int) Math.floor(x), (int) Math.floor(z));
            RTTYSystem.broadcast(level, CHAN_SATLINK, "Established connection to " + sat.getType() + " at " + sat.targetX + " / " + sat.targetZ);
            sat.onOrbit(level, x, y, z);
            data.setDirty();
        }
    }

    public static Satellite create(int id) {
        try {
            Class<? extends Satellite> c = satellites.get(id);
            return c.getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            return null;
        }
    }

    public static int getIDFromStack(ItemStack stack) {
        return satellites.indexOf(getClassFromStack(stack));
    }

    public int getID() {
        return satellites.indexOf(this.getClass());
    }

    /** Data currently loaded into the satellite, consumed by the tape drive over a sat link. */
    public EnumDriveType driveInput = null;
    public EnumDriveType driveOutput = null;

    public void writeToNBT(CompoundTag nbt) {
        nbt.putInt("targetX", targetX);
        nbt.putInt("targetZ", targetZ);
        nbt.putString("tx", tx);
        if (driveInput != null) nbt.putInt("driveInput", driveInput.ordinal());
        if (driveOutput != null) nbt.putInt("driveOutput", driveOutput.ordinal());
    }

    public void readFromNBT(CompoundTag nbt) {
        this.targetX = nbt.getInt("targetX");
        this.targetZ = nbt.getInt("targetZ");
        this.tx = nbt.getString("tx");
        this.driveInput = nbt.contains("driveInput") ? safeDrive(nbt.getInt("driveInput")) : null;
        this.driveOutput = nbt.contains("driveOutput") ? safeDrive(nbt.getInt("driveOutput")) : null;
    }

    private static EnumDriveType safeDrive(int ordinal) {
        EnumDriveType[] values = EnumDriveType.VALUES;
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : null;
    }

    public void onCommand(Level level, String... cmd) {
        onCommandTarget(level, cmd);
        onCommandImpl(level, cmd);
    }

    public void onCommandTarget(Level level, String... cmd) {
        if (cmd.length <= 0) return;

        if (cmd[0].equals(CMD_SETTARGET)) {
            if (cmd.length == 3) {
                targetX = IRORInteractive.parseInt(cmd[1], Integer.MIN_VALUE, Integer.MAX_VALUE);
                targetZ = IRORInteractive.parseInt(cmd[2], Integer.MIN_VALUE, Integer.MAX_VALUE);
            }
            if (cmd.length == 4) {
                targetX = IRORInteractive.parseInt(cmd[1], Integer.MIN_VALUE, Integer.MAX_VALUE);
                targetZ = IRORInteractive.parseInt(cmd[3], Integer.MIN_VALUE, Integer.MAX_VALUE);
            }
            return;
        }

        if (cmd[0].equals(CMD_GETTARGET)) {
            this.tx = targetX + ";" + targetZ;
            return;
        }

        if (cmd[0].equals(CMD_GETTARGETX)) {
            this.tx = "" + targetX;
            return;
        }

        if (cmd[0].equals(CMD_GETTARGETZ)) {
            this.tx = "" + targetZ;
        }
    }

    public void onCommandImpl(Level level, String... cmd) {
    }

    public void setTarget(int x, int z) {
        this.targetX = x;
        this.targetZ = z;
    }

    public void onUpdateTick(Level level) {
    }

    /** For subsequent items sent under the same frequency as an existing satellite. */
    public void onPartDelivered(Level level, ItemStack part) {
    }

    public boolean isDirty = false;

    public void markDirty() {
        this.isDirty = true;
    }

    public String getType() {
        return this.getClass().getSimpleName();
    }

    public Component[] getInfo(Level level) {
        return new Component[0];
    }

    /** The check for if there's data available; may also call {@link #produceData} if a cooldown has elapsed. */
    public boolean hasData(Level level) {
        return this.driveInput != null && this.driveOutput != null;
    }

    public EnumDriveType getOutputData(EnumDriveType input) {
        if (input == this.driveInput) return this.driveOutput;
        return null;
    }

    public void produceData(EnumDriveType input, EnumDriveType output) {
        this.driveInput = input;
        this.driveOutput = output;
    }

    public void consumeData() {
        this.driveInput = null;
        this.driveOutput = null;
    }

    /** Called when the satellite reaches space; used to trigger achievements and other one-off effects. */
    public void onOrbit(Level level, double x, double y, double z) {
    }

    /** Called by the sat interface when clicking on the screen; {@code x}/{@code z} are already translated to world coordinates. */
    public void onClick(Level level, ServerPlayer player, int x, int z) {
    }

    /** Called by the coord sat interface. */
    public void onCoordAction(Level level, ServerPlayer player, int x, int y, int z) {
    }

    public abstract float[] getColor();
}
