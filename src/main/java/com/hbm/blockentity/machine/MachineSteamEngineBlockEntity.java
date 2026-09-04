package com.hbm.blockentity.machine;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.lib.DirPos;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.tileentity.IConfigurableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.io.IOException;
import java.util.List;

/**
 * Ported from CE's {@code TileEntityMachineSteamEngine} (block {@code MachineSteamEngine}, regname
 * {@code machine_steam_engine}, read in full): a pure fluid-&gt;HE converter with no inventory and
 * no GUI. Multiblock footprint/dimensions ({@code {1,0,5,1,1,1}}, offset 1) live on
 * {@link com.hbm.blocks.machine.MachineSteamEngineBlock}; this class is the core-only block entity.
 * <p>
 * {@code ops}/efficiency math is CE's {@link FT_Coolable} trait read off tank 0's current fluid type
 * (steam -&gt; spent steam by default, {@code amountReq}=100, {@code amountProduced}=1,
 * {@code heatEnergy}=200 per {@link Fluids#STEAM}'s own trait registration), times
 * {@link #efficiency} (CE {@code IConfigurableMachine} {@code steam_engine}). {@code powerBuffer} is reset
 * to 0 and refilled every tick, exactly like CE - it is a same-tick push buffer, not a stored
 * reserve, hence {@link #getMaxPower()} returning the same value as {@link #getPower()}.
 * <p>
 * <b>Simplification vs. CE</b>: CE's {@code getConPos()} targets 3 fixed neighbor positions
 * (one block above each of the two "extra"-flagged dummy positions, plus the corresponding
 * position one block towards/away from the core) computed from the core's own encoded rotation.
 * That exact geometry is reproduced here unchanged (same formula, same offsets) - CE ties the
 * dummy-block "extra" flag purely to a client-side connector-pipe visual anchor
 * ({@code IConnectionAnchors}, not ported - no multiblock connection-anchor package exists in this
 * port yet), which this class does not depend on for the underlying HE/fluid push to function.
 */
public class MachineSteamEngineBlockEntity extends MachineBaseBlockEntity
        implements IEnergyProviderMK2, IFluidStandardTransceiverMK2, ITickableBE, IConfigurableMachine {

    public static int steamCap = 2_000;
    public static int ldsCap = 20;
    public static double efficiency = 0.85D;

    public final FluidTankNTM[] tanks;
    public long powerBuffer;
    public float rotor;
    private float acceleration;

    public MachineSteamEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0, true, true);
        tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.STEAM, steamCap).withOwner(this),
                new FluidTankNTM(Fluids.SPENTSTEAM, ldsCap).withOwner(this)
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.steamEngine");
    }

    private Direction coreDirection() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockDummyable.META)
                ? Direction.from3DDataValue(state.getValue(BlockDummyable.META) - BlockDummyable.offset)
                : Direction.NORTH;
    }

    public DirPos[] getConPos() {
        Direction dir = coreDirection();
        Direction rot = dir.getClockWise(Direction.Axis.Y);
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();

        return new DirPos[]{
                new DirPos(x + rot.getStepX() * 2, y + 1, z + rot.getStepZ() * 2, rot),
                new DirPos(x + rot.getStepX() * 2 + dir.getStepX(), y + 1, z + rot.getStepZ() * 2 + dir.getStepZ(), rot),
                new DirPos(x + rot.getStepX() * 2 - dir.getStepX(), y + 1, z + rot.getStepZ() * 2 - dir.getStepZ(), rot)
        };
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        this.powerBuffer = 0;
        tanks[0].setTankType(Fluids.STEAM);
        tanks[1].setTankType(Fluids.SPENTSTEAM);

        FT_Coolable trait = tanks[0].getTankType().getTrait(FT_Coolable.class);
        if (trait != null) {
            double eff = trait.getEfficiency(FT_Coolable.CoolingType.TURBINE) * efficiency;
            int inputOps = tanks[0].getFill() / trait.amountReq;
            int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / trait.amountProduced;
            int ops = Math.min(inputOps, outputOps);
            tanks[0].setFill(tanks[0].getFill() - ops * trait.amountReq);
            tanks[1].setFill(tanks[1].getFill() + ops * trait.amountProduced);
            this.powerBuffer += (long) (ops * trait.heatEnergy * eff);

            acceleration = Math.max(0F, Math.min(40F, acceleration + (ops > 0 ? 0.1F : -0.1F)));
        }
        rotor += acceleration;

        // CE: TileEntityMachineSteamEngine.update():130-140 - plays once per full rotor revolution
        // (not every tick), pitch driven by the current acceleration so a spun-up engine sounds higher.
        if (rotor >= 360F) {
            rotor -= 360F;
            level.playSound(null, worldPosition, HBMSoundHandler.steamEngineOperate.get(), SoundSource.BLOCKS, 1F, 0.5F + (acceleration / 80F));
        }

        for (DirPos dirPos : getConPos()) {
            BlockPos p = dirPos.getPos();
            if (this.powerBuffer > 0) this.tryProvide(level, p.getX(), p.getY(), p.getZ(), dirPos.getDir());
            this.trySubscribe(tanks[0].getTankType(), level, p.getX(), p.getY(), p.getZ(), dirPos.getDir());
            this.tryProvide(tanks[1], level, p, dirPos.getDir());
        }

        dataChanged();
        networkPackMK2(150);
    }

    @Override
    public String getConfigName() {
        return "steam_engine";
    }

    @Override
    public void readIfPresent(JsonObject obj) {
        readConfig(obj);
    }

    @Override
    public void writeConfig(JsonWriter writer) throws IOException {
        writeConfigStatic(writer);
    }

    static void readConfig(JsonObject obj) {
        // CE TileEntityMachineSteamEngine.java:83-85
        steamCap = IConfigurableMachine.grab(obj, "I:steamCap", steamCap);
        ldsCap = IConfigurableMachine.grab(obj, "I:ldsCap", ldsCap);
        efficiency = IConfigurableMachine.grab(obj, "D:efficiency", efficiency);
    }

    static void writeConfigStatic(JsonWriter writer) throws IOException {
        // CE TileEntityMachineSteamEngine.java:90-92
        writer.name("I:steamCap").value(steamCap);
        writer.name("I:ldsCap").value(ldsCap);
        writer.name("D:efficiency").value(efficiency);
    }

    /** NeoForge BE has no no-arg ctor. MachineDynConfig Exact CE :44-48. */
    public static final class ConfigDummy implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "steam_engine";
        }

        @Override
        public void readIfPresent(JsonObject obj) {
            readConfig(obj);
        }

        @Override
        public void writeConfig(JsonWriter writer) throws IOException {
            writeConfigStatic(writer);
        }
    }

    @Override
    public boolean canConnect(Direction dir) {
        return dir != Direction.UP && dir != Direction.DOWN;
    }

    @Override
    public long getPower() {
        return powerBuffer;
    }

    @Override
    public long getMaxPower() {
        return powerBuffer;
    }

    @Override
    public void setPower(long power) {
        this.powerBuffer = power;
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(tanks[1]);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tanks[0]);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tanks);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("powerBuffer", powerBuffer);
        tag.putFloat("acceleration", acceleration);
        tanks[0].writeToNBT(tag, "s");
        tanks[1].writeToNBT(tag, "w");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        powerBuffer = tag.getLong("powerBuffer");
        acceleration = tag.getFloat("acceleration");
        tanks[0].readFromNBT(tag, "s");
        tanks[1].readFromNBT(tag, "w");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(powerBuffer);
        buf.writeFloat(rotor);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        powerBuffer = buf.readLong();
        rotor = buf.readFloat();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }
}
