package com.hbm.blockentity.machine.fusion;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.machine.fusion.FusionTorusMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.FusionRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.modules.machine.ModuleMachineFusion;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import com.hbm.uninos.INetworkProvider;
import com.hbm.uninos.UniNodespace;
import com.hbm.uninos.networkproviders.KlystronNetwork;
import com.hbm.uninos.networkproviders.PlasmaNetwork;
import com.hbm.util.BobMathUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * CE {@code TileEntityFusionTorus}. CooledBase inlined.
 * SatelliteRayScan.INFO_PARTICLE Exact CE TileEntityFusionTorus.java:193-195.
 * TODO(CE: TileEntityFusionTorus.java:239): AudioWrapper fusionReactorRunning loop — VFX last.
 * TODO(CE: TileEntityFusionTorus.java:520): OpenComputers ntm_fusion_torus.
 * TODO(CE: MachineFusionTorus.java:87): TileEntityFusionTorusAE2 / ProxyCombo META≥6.
 */
public class FusionTorusBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider,
        IControlReceiver, IRORValueProvider {

    public static final float KELVIN = 273F;
    public static final float TEMPERATURE_TARGET = KELVIN - 150F;
    public static final float TEMP_CHANGE_PER_MB = 0.5F;
    public static final float TEMP_PASSIVE_HEATING = 2.5F;
    public static final float TEMP_CHANGE_MAX = 5F + TEMP_PASSIVE_HEATING;
    public static final long MAX_POWER = 10_000_000L;

    public final FluidTankNTM[] coolantTanks = new FluidTankNTM[2];
    public final FluidTankNTM[] tanks = new FluidTankNTM[4];
    public final ModuleMachineFusion fusionModule;

    protected KlystronNetwork.KlystronNode[] klystronNodes = new KlystronNetwork.KlystronNode[4];
    protected PlasmaNetwork.PlasmaNode[] plasmaNodes = new PlasmaNetwork.PlasmaNode[4];
    public boolean[] connections = new boolean[4];

    public boolean didProcess;
    public long power;
    public float temperature = KELVIN + 20;
    public long klystronEnergy;
    private long klystronEnergySync;
    public long plasmaEnergy;
    public double fuelConsumption;

    public FusionTorusBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 3, true, true);
        coolantTanks[0] = new FluidTankNTM(Fluids.PERFLUOROMETHYL_COLD, 4_000).withOwner(this);
        coolantTanks[1] = new FluidTankNTM(Fluids.PERFLUOROMETHYL, 4_000).withOwner(this);
        for (int i = 0; i < 4; i++) tanks[i] = new FluidTankNTM(Fluids.NONE, 4_000).withOwner(this);
        this.fusionModule = new ModuleMachineFusion(0, this, inventory)
                .fluidInput(tanks[0], tanks[1], tanks[2])
                .fluidOutput(tanks[3])
                .itemOutput(2);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fusionTorus");
    }

    @Override
    public boolean isUseableByPlayer(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 32 * 32;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        return slot == 1 && stack.getItem() instanceof ItemBlueprints;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        return slot == 2;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{2};
    }

    public boolean isCool() {
        return this.temperature <= TEMPERATURE_TARGET;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        checkTilt(TiltType.CONFIG, true);

        for (int i = 0; i < 4; i++) {
            Direction dir = Direction.from3DDataValue(i + 2);
            if (klystronNodes[i] == null || klystronNodes[i].expired) {
                klystronNodes[i] = createKlystronNode(KlystronNetwork.THE_PROVIDER, dir);
            }
            if (plasmaNodes[i] == null || plasmaNodes[i].expired) {
                plasmaNodes[i] = createPlasmaNode(PlasmaNetwork.THE_PROVIDER, dir);
            }
            if (klystronNodes[i].net != null) klystronNodes[i].net.addReceiver(this);
            if (plasmaNodes[i].net != null) plasmaNodes[i].net.addProvider(this);
        }

        this.temperature += TEMP_PASSIVE_HEATING;
        if (this.temperature > KELVIN + 20) this.temperature = KELVIN + 20;
        if (this.temperature > TEMPERATURE_TARGET) {
            int cyclesTemp = (int) Math.ceil((Math.min(this.temperature - TEMPERATURE_TARGET, TEMP_CHANGE_MAX)) / TEMP_CHANGE_PER_MB);
            int cyclesCool = coolantTanks[0].getFill();
            int cyclesHot = coolantTanks[1].getMaxFill() - coolantTanks[1].getFill();
            int cycles = BobMathUtil.min(cyclesTemp, cyclesCool, cyclesHot);
            coolantTanks[0].setFill(coolantTanks[0].getFill() - cycles);
            coolantTanks[1].setFill(coolantTanks[1].getFill() + cycles);
            this.temperature -= TEMP_CHANGE_PER_MB * cycles;
        }

        for (DirPos pos : getConPos()) {
            if (level.getGameTime() % 20 == 0) {
                trySubscribe(level, pos);
                trySubscribe(coolantTanks[0].getTankType(), level, pos);
                if (tanks[0].getTankType() != Fluids.NONE) trySubscribe(tanks[0].getTankType(), level, pos);
                if (tanks[1].getTankType() != Fluids.NONE) trySubscribe(tanks[1].getTankType(), level, pos);
                if (tanks[2].getTankType() != Fluids.NONE) trySubscribe(tanks[2].getTankType(), level, pos);
            }
            if (coolantTanks[1].getFill() > 0) tryProvide(coolantTanks[1], level, pos);
            if (tanks[3].getFill() > 0) tryProvide(tanks[3], level, pos);
        }

        this.power = Library.chargeTEFromItems(inventory, 0, power, getMaxPower());

        int receiverCount = 0;
        int collectors = 0;
        for (int i = 0; i < 4; i++) {
            connections[i] = klystronNodes[i] != null && klystronNodes[i].hasValidNet() && !klystronNodes[i].net.providerEntries.isEmpty();
            if (!connections[i] && plasmaNodes[i] != null && plasmaNodes[i].hasValidNet() && !plasmaNodes[i].net.receiverEntries.isEmpty()) {
                connections[i] = true;
            }
            if (plasmaNodes[i] != null && plasmaNodes[i].hasValidNet() && !plasmaNodes[i].net.receiverEntries.isEmpty()) {
                for (BlockEntity thing : plasmaNodes[i].net.receiverEntries.keySet()) {
                    if (thing.isRemoved()) continue;
                    if (thing instanceof LoadedBaseBlockEntity loaded && !loaded.isLoaded()) continue;
                    if (thing instanceof IFusionPowerReceiver recv && recv.receivesFusionPower()) receiverCount++;
                    if (thing instanceof FusionCollectorBlockEntity) collectors++;
                    break;
                }
            }
        }

        FusionRecipe recipe = (FusionRecipe) this.fusionModule.getRecipe();
        double powerFactor = getSpeedScaled(getMaxPower(), power);
        double fuel0Factor = recipe != null && recipe.inputFluid != null && recipe.inputFluid.length > 0
                ? getSpeedScaled(tanks[0].getMaxFill(), tanks[0].getFill()) : 1D;
        double fuel1Factor = recipe != null && recipe.inputFluid != null && recipe.inputFluid.length > 1
                ? getSpeedScaled(tanks[1].getMaxFill(), tanks[1].getFill()) : 1D;
        double fuel2Factor = recipe != null && recipe.inputFluid != null && recipe.inputFluid.length > 2
                ? getSpeedScaled(tanks[2].getMaxFill(), tanks[2].getFill()) : 1D;
        double factor = BobMathUtil.min(powerFactor, fuel0Factor, fuel1Factor, fuel2Factor);
        boolean ignition = recipe == null || recipe.ignitionTemp <= this.klystronEnergy;

        this.plasmaEnergy = 0;
        this.fuelConsumption = 0;
        this.fusionModule.preUpdate(factor, collectors * 0.5D);
        this.fusionModule.update(1D, 1D, !this.tilted && this.isCool() && ignition, inventory.getStackInSlot(1));
        this.didProcess = this.fusionModule.didProcess;
        if (this.fusionModule.markDirty) setChanged();
        if (didProcess && recipe != null) {
            this.plasmaEnergy = (long) Math.ceil(recipe.outputTemp * factor);
            this.fuelConsumption = factor;
            // CE TileEntityFusionTorus.java:193-195
            if (level.getGameTime() % 20 == 15) {
                SatelliteRayScan.reportEvent(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                        SatelliteRayScan.RayEvent.INFO_PARTICLE, 200);
            }
        }

        double outputIntensity = getOuputIntensity(receiverCount);
        double outputFlux = recipe != null ? recipe.neutronFlux * factor : 0D;
        float r = recipe != null ? recipe.r : 0F;
        float g = recipe != null ? recipe.g : 0F;
        float b = recipe != null ? recipe.b : 0F;

        if (this.plasmaEnergy > 0) {
            for (int i = 0; i < 4; i++) {
                if (plasmaNodes[i] != null && plasmaNodes[i].hasValidNet() && !plasmaNodes[i].net.receiverEntries.isEmpty()) {
                    for (BlockEntity te : plasmaNodes[i].net.receiverEntries.keySet()) {
                        if (te instanceof IFusionPowerReceiver receiver) {
                            receiver.receiveFusionPower((long) Math.ceil(this.plasmaEnergy * outputIntensity), outputFlux, r, g, b);
                        }
                    }
                }
            }
        }

        this.klystronEnergySync = this.klystronEnergy;
        networkPackNT(150);
        this.klystronEnergy = 0;
    }

    public static double getOuputIntensity(int receiverCount) {
        if (receiverCount == 1) return 1D;
        if (receiverCount == 2) return 0.625D;
        if (receiverCount == 3) return 0.5D;
        return 0.4375D;
    }

    public static double getSpeedScaled(double max, double level) {
        if (max == 0) return 0D;
        if (level >= max * 0.5) return 1D;
        return level / max * 2D;
    }

    public PlasmaNetwork.PlasmaNode createPlasmaNode(INetworkProvider<PlasmaNetwork> provider, Direction dir) {
        BlockPos nodePos = worldPosition.offset(dir.getStepX() * 7, 2, dir.getStepZ() * 7);
        PlasmaNetwork.PlasmaNode node = UniNodespace.getNode(level, nodePos, provider);
        if (node != null) return node;
        node = (PlasmaNetwork.PlasmaNode) new PlasmaNetwork.PlasmaNode(provider, nodePos)
                .setConnections(new DirPos(worldPosition.getX() + dir.getStepX() * 8, worldPosition.getY() + 2,
                        worldPosition.getZ() + dir.getStepZ() * 8, dir));
        UniNodespace.createNode(level, node);
        return node;
    }

    public KlystronNetwork.KlystronNode createKlystronNode(INetworkProvider<KlystronNetwork> provider, Direction dir) {
        BlockPos nodePos = worldPosition.offset(dir.getStepX() * 7, 2, dir.getStepZ() * 7);
        KlystronNetwork.KlystronNode node = UniNodespace.getNode(level, nodePos, provider);
        if (node != null) return node;
        node = (KlystronNetwork.KlystronNode) new KlystronNetwork.KlystronNode(provider, nodePos)
                .setConnections(new DirPos(worldPosition.getX() + dir.getStepX() * 8, worldPosition.getY() + 2,
                        worldPosition.getZ() + dir.getStepZ() * 8, dir));
        UniNodespace.createNode(level, node);
        return node;
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x, y - 1, z, Direction.DOWN),
                new DirPos(x, y + 5, z, Direction.UP),
                new DirPos(x + 6, y - 1, z, Direction.DOWN),
                new DirPos(x + 6, y + 5, z, Direction.UP),
                new DirPos(x + 6, y - 1, z + 2, Direction.DOWN),
                new DirPos(x + 6, y + 5, z + 2, Direction.UP),
                new DirPos(x + 6, y - 1, z - 2, Direction.DOWN),
                new DirPos(x + 6, y + 5, z - 2, Direction.UP),
                new DirPos(x - 6, y - 1, z, Direction.DOWN),
                new DirPos(x - 6, y + 5, z, Direction.UP),
                new DirPos(x - 6, y - 1, z + 2, Direction.DOWN),
                new DirPos(x - 6, y + 5, z + 2, Direction.UP),
                new DirPos(x - 6, y - 1, z - 2, Direction.DOWN),
                new DirPos(x - 6, y + 5, z - 2, Direction.UP),
                new DirPos(x, y - 1, z + 6, Direction.DOWN),
                new DirPos(x, y + 5, z + 6, Direction.UP),
                new DirPos(x + 2, y - 1, z + 6, Direction.DOWN),
                new DirPos(x + 2, y + 5, z + 6, Direction.UP),
                new DirPos(x - 2, y - 1, z + 6, Direction.DOWN),
                new DirPos(x - 2, y + 5, z + 6, Direction.UP),
                new DirPos(x, y - 1, z - 6, Direction.DOWN),
                new DirPos(x, y + 5, z - 6, Direction.UP),
                new DirPos(x + 2, y - 1, z - 6, Direction.DOWN),
                new DirPos(x + 2, y + 5, z - 6, Direction.UP),
                new DirPos(x - 2, y - 1, z - 6, Direction.DOWN),
                new DirPos(x - 2, y + 5, z - 6, Direction.UP),
        };
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            for (KlystronNetwork.KlystronNode node : klystronNodes) {
                if (node != null) UniNodespace.destroyNode(level, node);
            }
            for (PlasmaNetwork.PlasmaNode node : plasmaNodes) {
                if (node != null) UniNodespace.destroyNode(level, node);
            }
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        coolantTanks[0].serialize(buf);
        coolantTanks[1].serialize(buf);
        buf.writeFloat(temperature);
        buf.writeLong(power);
        buf.writeBoolean(didProcess);
        buf.writeLong(klystronEnergySync);
        buf.writeLong(plasmaEnergy);
        buf.writeDouble(fuelConsumption);
        fusionModule.serialize(buf);
        for (int i = 0; i < 4; i++) tanks[i].serialize(buf);
        for (int i = 0; i < 4; i++) buf.writeBoolean(connections[i]);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        coolantTanks[0].deserialize(buf);
        coolantTanks[1].deserialize(buf);
        temperature = buf.readFloat();
        power = buf.readLong();
        didProcess = buf.readBoolean();
        klystronEnergy = buf.readLong();
        plasmaEnergy = buf.readLong();
        fuelConsumption = buf.readDouble();
        fusionModule.deserialize(buf);
        for (int i = 0; i < 4; i++) tanks[i].deserialize(buf);
        for (int i = 0; i < 4; i++) connections[i] = buf.readBoolean();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        coolantTanks[0].writeToNBT(tag, "t0");
        coolantTanks[1].writeToNBT(tag, "t1");
        tag.putFloat("temperature", temperature);
        tag.putLong("power", power);
        for (int i = 0; i < 4; i++) tanks[i].writeToNBT(tag, "ft" + i);
        fusionModule.writeToNBT(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        coolantTanks[0].readFromNBT(tag, "t0");
        coolantTanks[1].readFromNBT(tag, "t1");
        temperature = tag.getFloat("temperature");
        power = tag.getLong("power");
        for (int i = 0; i < 4; i++) tanks[i].readFromNBT(tag, "ft" + i);
        fusionModule.readFromNBT(tag);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(coolantTanks[1], tanks[3]);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(coolantTanks[0], tanks[0], tanks[1], tanks[2]);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(coolantTanks[0], coolantTanks[1], tanks[0], tanks[1], tanks[2], tanks[3]);
    }

    @Override
    public int getFloorCount() {
        return 6 * 6;
    }

    @Override
    public BlockPos getFloorPosFromIndex(int index) {
        return new BlockPos(
                worldPosition.getX() - 5 + (index / 6) * 2,
                worldPosition.getY() - 1,
                worldPosition.getZ() - 5 + (index % 6) * 2
        );
    }

    @Override
    public boolean hasPermission(Player player) {
        return isUseableByPlayer(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("index") && data.contains("selection")) {
            if (data.getInt("index") == 0) {
                this.fusionModule.setRecipe(data.getString("selection"), false);
                setChanged();
            }
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FusionTorusMenu(id, inv, this);
    }

    @Override
    public String[] getFunctionInfo() {
        // CE TileEntityFusionTorus.java:496-500
        return new String[]{
                PREFIX_VALUE + "plasma",
                PREFIX_VALUE + "consumption"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :504-507
        if ((PREFIX_VALUE + "plasma").equals(name)) return "" + this.plasmaEnergy;
        if ((PREFIX_VALUE + "consumption").equals(name)) return "" + (int) (this.fuelConsumption * 100);
        return null;
    }

}
