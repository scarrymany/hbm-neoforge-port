package com.hbm.blockentity.machine.rbmk;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Polymorphic column data DTO for the RBMK console's remote display, ported verbatim from CE's
 * {@code com.hbm.tileentity.machine.rbmk.RBMKColumn} (373 lines, read in full) - a pure struct +
 * {@link ByteBuf} (de)serialization, with zero coupling to any concrete column TE beyond the
 * {@link ColumnType} enum. Written directly in this work package (RBMK console/data-model scope, not
 * flux/xenon/meltdown math): every concrete column TE's {@code getConsoleData()} produces one of
 * these, and {@link com.hbm.blockentity.machine.rbmk.RBMKConsoleBlockEntity} is this class's only
 * real consumer.
 * <p>
 * <b>Reconciliation note</b>: this class references
 * {@link RBMKBaseBlockEntity#getConsoleType()}/{@code getConsoleData()} conceptually (every column TE
 * in this package implements those, see each TE's own javadoc) but does not itself import the
 * forward-referenced base class - it is pure data.
 */
public abstract class RBMKColumn {

    public double heat;
    public double maxHeat;
    public boolean moderated;
    public int reasimWater;
    public int reasimSteam;
    public int indicator;

    public final @NotNull ColumnType type;

    protected RBMKColumn(ColumnType type) {
        this.type = type;
    }

    public void serialize(ByteBuf buf) {
        buf.writeDouble(heat);
        buf.writeDouble(maxHeat);
        buf.writeBoolean(moderated);
        buf.writeInt(reasimWater);
        buf.writeInt(reasimSteam);
        buf.writeByte(indicator);
    }

    public void deserialize(ByteBuf buf) {
        heat = buf.readDouble();
        maxHeat = buf.readDouble();
        moderated = buf.readBoolean();
        reasimWater = buf.readInt();
        reasimSteam = buf.readInt();
        indicator = buf.readByte();
    }

    /** Plain-text stat lines for a console screen readout. No i18n/formatting - see class javadoc. */
    public List<String> getFancyStats() {
        List<String> stats = new ArrayList<>();
        stats.add("Heat: " + ((int) (heat * 10D)) / 10D + " C");
        if (moderated) stats.add("Moderated");
        return stats;
    }

    public static RBMKColumn readFromBuf(ByteBuf buf) {
        byte ordinal = buf.readByte();
        if (ordinal == -1) return null;
        ColumnType type = ColumnType.VALUES[ordinal];
        RBMKColumn column = createForType(type);
        column.deserialize(buf);
        return column;
    }

    public static void writeToBuf(ByteBuf buf, RBMKColumn column) {
        if (column == null) {
            buf.writeByte(-1);
        } else {
            buf.writeByte((byte) column.type.ordinal());
            column.serialize(buf);
        }
    }

    public static RBMKColumn createForType(ColumnType type) {
        return switch (type) {
            case FUEL, FUEL_SIM, BREEDER -> new FuelColumn(type);
            case BOILER -> new BoilerColumn();
            case CONTROL, CONTROL_AUTO -> new ControlColumn(type);
            case COOLER -> new CoolerColumn();
            case OUTGASSER -> new OutgasserColumn();
            case HEATEX -> new HeaterColumn();
            default -> new StandardColumn(type);
        };
    }

    /** Canonical list of column kinds a grid position can hold - CE: {@code RBMKColumn.ColumnType}. */
    public enum ColumnType {
        BLANK(0), FUEL(10), FUEL_SIM(90), CONTROL(20), CONTROL_AUTO(30), BOILER(40),
        MODERATOR(50), ABSORBER(60), REFLECTOR(70), OUTGASSER(80), BREEDER(100),
        STORAGE(110), COOLER(120), HEATEX(130);

        public static final ColumnType[] VALUES = values();

        public final int offset;

        ColumnType(int offset) {
            this.offset = offset;
        }
    }

    public static class StandardColumn extends RBMKColumn {
        public StandardColumn(ColumnType type) {
            super(type);
        }
    }

    public static class FuelColumn extends RBMKColumn {
        public double enrichment;
        public double xenon;
        public double c_coreHeat;
        public double c_heat;
        public double c_maxHeat;

        public FuelColumn(ColumnType type) {
            super(type);
        }

        @Override
        public void serialize(ByteBuf buf) {
            super.serialize(buf);
            buf.writeDouble(enrichment);
            buf.writeDouble(xenon);
            buf.writeDouble(c_coreHeat);
            buf.writeDouble(c_heat);
            buf.writeDouble(c_maxHeat);
        }

        @Override
        public void deserialize(ByteBuf buf) {
            super.deserialize(buf);
            enrichment = buf.readDouble();
            xenon = buf.readDouble();
            c_coreHeat = buf.readDouble();
            c_heat = buf.readDouble();
            c_maxHeat = buf.readDouble();
        }

        @Override
        public List<String> getFancyStats() {
            List<String> stats = super.getFancyStats();
            stats.add("Depletion: " + ((int) (((1D - enrichment) * 100000)) / 1000D) + "%");
            stats.add("Xenon: " + ((int) ((xenon * 1000D)) / 1000D) + "%");
            stats.add("Core temp: " + ((int) (c_coreHeat * 10D)) / 10D + " C");
            stats.add("Skin temp: " + ((int) (c_heat * 10D)) / 10D + " / " + ((int) (c_maxHeat * 10D)) / 10D + " C");
            return stats;
        }
    }

    public static class BoilerColumn extends RBMKColumn {
        public int water;
        public int maxWater;
        public int steam;
        public int maxSteam;
        public short steamType;

        public BoilerColumn() {
            super(ColumnType.BOILER);
        }

        @Override
        public void serialize(ByteBuf buf) {
            super.serialize(buf);
            buf.writeInt(water);
            buf.writeInt(maxWater);
            buf.writeInt(steam);
            buf.writeInt(maxSteam);
            buf.writeShort(steamType);
        }

        @Override
        public void deserialize(ByteBuf buf) {
            super.deserialize(buf);
            water = buf.readInt();
            maxWater = buf.readInt();
            steam = buf.readInt();
            maxSteam = buf.readInt();
            steamType = buf.readShort();
        }

        @Override
        public List<String> getFancyStats() {
            List<String> stats = super.getFancyStats();
            stats.add("Water: " + water + "/" + maxWater);
            stats.add("Steam: " + steam + "/" + maxSteam);
            return stats;
        }
    }

    public static class ControlColumn extends RBMKColumn {
        public double level;
        public short color = -1;

        public ControlColumn(ColumnType type) {
            super(type);
        }

        @Override
        public void serialize(ByteBuf buf) {
            super.serialize(buf);
            buf.writeDouble(level);
            buf.writeShort(color);
        }

        @Override
        public void deserialize(ByteBuf buf) {
            super.deserialize(buf);
            level = buf.readDouble();
            color = buf.readShort();
        }

        @Override
        public List<String> getFancyStats() {
            List<String> stats = super.getFancyStats();
            stats.add("Extraction: " + ((int) (level * 100D)) + "%");
            return stats;
        }
    }

    public static class CoolerColumn extends RBMKColumn {
        public int cooled;
        public int cryo;
        public int maxCryo;
        public int hot;
        public int maxHot;
        public short coldType;
        public short hotType;

        public CoolerColumn() {
            super(ColumnType.COOLER);
        }

        @Override
        public void serialize(ByteBuf buf) {
            super.serialize(buf);
            buf.writeInt(cooled);
            buf.writeInt(cryo);
            buf.writeInt(maxCryo);
            buf.writeInt(hot);
            buf.writeInt(maxHot);
            buf.writeShort(coldType);
            buf.writeShort(hotType);
        }

        @Override
        public void deserialize(ByteBuf buf) {
            super.deserialize(buf);
            cooled = buf.readInt();
            cryo = buf.readInt();
            maxCryo = buf.readInt();
            hot = buf.readInt();
            maxHot = buf.readInt();
            coldType = buf.readShort();
            hotType = buf.readShort();
        }

        @Override
        public List<String> getFancyStats() {
            List<String> stats = super.getFancyStats();
            stats.add("Cooling: " + (cooled * 20));
            stats.add("Coolant: " + cryo + "/" + maxCryo + "mB");
            stats.add("Hot output: " + hot + "/" + maxHot + "mB");
            return stats;
        }
    }

    public static class OutgasserColumn extends RBMKColumn {
        public int gas;
        public int maxGas;
        public double progress;
        public double maxProgress;
        public double usedFlux;

        public OutgasserColumn() {
            super(ColumnType.OUTGASSER);
        }

        @Override
        public void serialize(ByteBuf buf) {
            super.serialize(buf);
            buf.writeInt(gas);
            buf.writeInt(maxGas);
            buf.writeDouble(progress);
            buf.writeDouble(maxProgress);
            buf.writeDouble(usedFlux);
        }

        @Override
        public void deserialize(ByteBuf buf) {
            super.deserialize(buf);
            gas = buf.readInt();
            maxGas = buf.readInt();
            progress = buf.readDouble();
            maxProgress = buf.readDouble();
            usedFlux = buf.readDouble();
        }

        @Override
        public List<String> getFancyStats() {
            List<String> stats = super.getFancyStats();
            stats.add("Flux used: " + (long) usedFlux);
            stats.add("Progress: " + (long) progress + "/" + (long) maxProgress);
            stats.add("Gas: " + gas + "/" + maxGas);
            return stats;
        }
    }

    public static class HeaterColumn extends RBMKColumn {
        public int water;
        public int maxWater;
        public int steam;
        public int maxSteam;
        public short coldType;
        public short hotType;

        public HeaterColumn() {
            super(ColumnType.HEATEX);
        }

        @Override
        public void serialize(ByteBuf buf) {
            super.serialize(buf);
            buf.writeInt(water);
            buf.writeInt(maxWater);
            buf.writeInt(steam);
            buf.writeInt(maxSteam);
            buf.writeShort(coldType);
            buf.writeShort(hotType);
        }

        @Override
        public void deserialize(ByteBuf buf) {
            super.deserialize(buf);
            water = buf.readInt();
            maxWater = buf.readInt();
            steam = buf.readInt();
            maxSteam = buf.readInt();
            coldType = buf.readShort();
            hotType = buf.readShort();
        }

        @Override
        public List<String> getFancyStats() {
            List<String> stats = super.getFancyStats();
            stats.add("Feed: " + water + "/" + maxWater + "mB");
            stats.add("Export: " + steam + "/" + maxSteam + "mB");
            return stats;
        }
    }
}
