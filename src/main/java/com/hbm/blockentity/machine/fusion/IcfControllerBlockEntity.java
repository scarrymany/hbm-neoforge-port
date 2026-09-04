package com.hbm.blockentity.machine.fusion;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.fusion.IcfControllerBlock;
import com.hbm.blocks.machine.fusion.IcfReactorBlock;
import com.hbm.tileentity.IConfigurableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * CE {@code TileEntityICFController}. Capacitor/turbo count Exact CE {@code :49-104}/{@code :264-265}.
 * {@link IConfigurableMachine} Exact CE {@code :269-282} ({@code icfLaser}).
 * Assemble replaces parts with {@link com.hbm.blocks.machine.fusion.IcfBlock}.
 * Break of any ICF proxy restores the original and sets {@code assembled=false}.
 */
public class IcfControllerBlockEntity extends LoadedBaseBlockEntity implements IEnergyReceiverMK2, ITickableBE, IPersistentNBT {

    // CE TileEntityICFController.java:35-36
    public static int capacitorPower = 2_500_000;
    public static int turboPower = 5_000_000;

    private static final int MAX_RANGE = 48;

    private final List<BlockPos> ports = new ArrayList<>();
    public long power;
    public int laserLength;
    public boolean assembled;
    private int cellCount;
    private int emitterCount;
    private int capacitorCount;
    private int turbochargerCount;
    private boolean destroyedByCreativePlayer;

    public IcfControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void setup(HashSet<BlockPos> ports, HashSet<BlockPos> cells, HashSet<BlockPos> emitters,
                      HashSet<BlockPos> capacitors, HashSet<BlockPos> turbochargers) {
        // CE TileEntityICFController.java:49-103
        this.cellCount = 0;
        this.emitterCount = 0;
        this.capacitorCount = 0;
        this.turbochargerCount = 0;

        if (level == null) return;
        BlockState controllerState = level.getBlockState(worldPosition);
        if (!(controllerState.getBlock() instanceof IcfControllerBlock)) return;
        Direction structureDirection = controllerState.getValue(IcfControllerBlock.FACING).getOpposite();
        HashSet<BlockPos> validCells = new HashSet<>();
        HashSet<BlockPos> validEmitters = new HashSet<>();
        HashSet<BlockPos> validCapacitors = new HashSet<>();

        for (int i = 1; i <= cells.size(); i++) {
            BlockPos currentCellPos = worldPosition.relative(structureDirection, i);
            if (cells.contains(currentCellPos)) {
                this.cellCount++;
                validCells.add(currentCellPos);
            } else {
                break;
            }
        }

        for (BlockPos emitterPos : emitters) {
            for (Direction facing : Direction.values()) {
                if (validCells.contains(emitterPos.relative(facing))) {
                    this.emitterCount++;
                    validEmitters.add(emitterPos);
                    break;
                }
            }
        }
        for (BlockPos capacitorPos : capacitors) {
            for (Direction facing : Direction.values()) {
                if (validEmitters.contains(capacitorPos.relative(facing))) {
                    this.capacitorCount++;
                    validCapacitors.add(capacitorPos);
                    break;
                }
            }
        }
        for (BlockPos turboPos : turbochargers) {
            for (Direction facing : Direction.values()) {
                if (validCapacitors.contains(turboPos.relative(facing))) {
                    this.turbochargerCount++;
                    break;
                }
            }
        }
        this.ports.clear();
        this.ports.addAll(ports);
        setChanged();
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        Direction dir = getBlockState().getValue(IcfControllerBlock.FACING);

        if (this.assembled) {
            // CE TileEntityICFController.java:118-124
            for (BlockPos port : ports) {
                for (Direction face : Direction.values()) {
                    if (this.getMaxPower() > 0) {
                        trySubscribe(level, port.relative(face), face);
                    }
                }
            }

            if (this.power > 0) {
                fireLaser(dir);
                this.power = 0;
            } else {
                this.laserLength = 0;
            }
        } else {
            this.laserLength = 0;
        }

        dataChanged();
        networkPackMK2(50);
    }

    private void fireLaser(Direction dir) {
        long firedPower = this.getPower();
        long firedMax = this.getMaxPower();

        int hitLength = 0;
        BlockPos endPos = worldPosition;

        for (int i = 1; i <= MAX_RANGE; i++) {
            BlockPos scan = worldPosition.relative(dir, i);
            BlockState state = level.getBlockState(scan);
            hitLength = i;
            endPos = scan;

            if (state.getBlock() instanceof IcfReactorBlock reactorBlock) {
                BlockPos corePos = reactorBlock.findCore(level, scan);
                if (corePos != null && level.getBlockEntity(corePos) instanceof IcfReactorBlockEntity reactor) {
                    reactor.receiveLaser(firedPower, firedMax);
                    break;
                }
            }

            if (!state.isAir()) {
                float resistance = state.getBlock().getExplosionResistance();
                if (resistance < 6000F) {
                    BlockDummyable.safeRem = true;
                    try {
                        level.destroyBlock(scan, false);
                    } finally {
                        BlockDummyable.safeRem = false;
                    }
                } else {
                    break;
                }
            }
        }

        this.laserLength = hitLength;

        AABB beam = new AABB(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                endPos.getX() + 0.5, endPos.getY() + 0.5, endPos.getZ() + 0.5).inflate(0.3);
        List<Entity> hit = level.getEntitiesOfClass(Entity.class, beam);
        for (Entity e : hit) {
            e.hurt(level.damageSources().inFire(), 50F);
            e.igniteForSeconds(5);
        }
    }

    @Override
    public long getPower() {
        return Math.min(power, getMaxPower());
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        // CE TileEntityICFController.java:265
        return (long) (Math.sqrt(capacitorCount) * capacitorPower
                + Math.sqrt(Math.min(turbochargerCount, capacitorCount)) * turboPower);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeNBT(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readNBT(tag);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(capacitorCount);
        buf.writeInt(turbochargerCount);
        buf.writeInt(laserLength);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.power = buf.readLong();
        this.capacitorCount = buf.readInt();
        this.turbochargerCount = buf.readInt();
        this.laserLength = buf.readInt();
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        // CE TileEntityICFController.java:234-249
        nbt.putLong("power", power);
        nbt.putBoolean("assembled", assembled);
        nbt.putInt("cellCount", cellCount);
        nbt.putInt("emitterCount", emitterCount);
        nbt.putInt("capacitorCount", capacitorCount);
        nbt.putInt("turbochargerCount", turbochargerCount);
        nbt.putInt("portCount", ports.size());
        for (int i = 0; i < ports.size(); i++) {
            BlockPos p = ports.get(i);
            nbt.putIntArray("p" + i, new int[]{p.getX(), p.getY(), p.getZ()});
        }
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        this.power = nbt.getLong("power");
        this.assembled = nbt.getBoolean("assembled");
        this.cellCount = nbt.getInt("cellCount");
        this.emitterCount = nbt.getInt("emitterCount");
        this.capacitorCount = nbt.getInt("capacitorCount");
        this.turbochargerCount = nbt.getInt("turbochargerCount");
        ports.clear();
        int portCount = nbt.getInt("portCount");
        for (int i = 0; i < portCount; i++) {
            int[] port = nbt.getIntArray("p" + i);
            if (port.length >= 3) ports.add(new BlockPos(port[0], port[1], port[2]));
        }
    }

    @Override
    public void setDestroyedByCreativePlayer() {
        this.destroyedByCreativePlayer = true;
    }

    @Override
    public boolean isDestroyedByCreativePlayer() {
        return destroyedByCreativePlayer;
    }

    static void readLaser(JsonObject obj) {
        // CE TileEntityICFController.java:275-276
        capacitorPower = IConfigurableMachine.grab(obj, "I:capacitorPower", capacitorPower);
        turboPower = IConfigurableMachine.grab(obj, "I:turboPower", turboPower);
    }

    static void writeLaser(JsonWriter writer) throws IOException {
        // CE TileEntityICFController.java:281-282
        writer.name("I:capacitorPower").value(capacitorPower);
        writer.name("I:turboPower").value(turboPower);
    }

    public static final class ConfigDummy implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "icfLaser";
        }

        @Override
        public void readIfPresent(JsonObject obj) {
            readLaser(obj);
        }

        @Override
        public void writeConfig(JsonWriter writer) throws IOException {
            writeLaser(writer);
        }
    }
}
