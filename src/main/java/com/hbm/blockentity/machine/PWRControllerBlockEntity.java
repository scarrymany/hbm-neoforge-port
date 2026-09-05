package com.hbm.blockentity.machine;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blockentity.network.IConnectionAnchors;
import com.hbm.blocks.machine.PWRBlocks;
import com.hbm.blocks.machine.PWRPhase1Blocks;
import com.hbm.inventory.container.machine.PWRControllerMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.inventory.fluid.trait.FT_PWRModerator;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.machine.ItemPWRFuel;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.items.machine.PWRHotFuelItems;
import com.hbm.lib.DirPos;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import com.hbm.util.EnumUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ported from CE's {@code TileEntityPWRController} (726 lines, read in full; see
 * {@code docs/phase2/reactors_breeding_pwr.md} for the field-level survey this follows). Every heat/
 * flux/coolant formula below is pasted from that class near-verbatim, translated to 1.21.1 types
 * (world -&gt; level, {@code IBlockState} -&gt; {@link BlockState}, {@code ForgeDirection} -&gt;
 * {@link Direction}) - see that report's "pure-math formulas are fully portable" section for the
 * formulas themselves (heat-capacity scaling, the connection-efficiency curve, core/hull heat
 * equalization, the 0.999 per-tick decay).
 *
 * <h2>3-slot inventory</h2>
 * Slot 2 is the coolant {@link IItemFluidIdentifier} — Exact CE {@code TileEntityPWRController.java:183}
 * {@code tanks[0].setType(2, inventory)} when {@code amountLoaded <= 0}.
 *
 * <h2>Deliberate drops from CE, each independently justified</h2>
 * <ul>
 *   <li>The {@code isPrinting}/{@link com.hbm.items.machine.ItemPWRPrinter} sync-channel hijack is
 *   dropped - already flagged Deferred in both {@code docs/phase1/items_machine.md} and this
 *   package's own research report ("port the controller's normal serialize/deserialize path first").
 *   {@link #serialize}/{@link #deserialize} below are the plain, always-real-state path only.</li>
 *   <li>SatelliteRayScan.INFO_NUCLEAR Exact CE {@code TileEntityPWRController.java:265-266}.</li>
 *   <li>OpenComputers (@Callback methods) dropped. ROR: CE {@code TileEntityPWRController.java:609-640}.</li>
 * </ul>
 */
public class PWRControllerBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardTransceiverMK2, IConnectionAnchors, ITickableBE, MenuProvider,
        IRORValueProvider, IRORInteractive {

    public static final long CORE_HEAT_CAPACITY_BASE = 10_000_000L;
    public static final long HULL_HEAT_CAPACITY_BASE = 10_000_000L;
    private static final int MELTDOWN_EXPLOSION_POWER = 15;

    public FluidTankNTM[] tanks;
    public long coreHeat;
    public long coreHeatCapacity = CORE_HEAT_CAPACITY_BASE;
    public long hullHeat;
    public double flux;

    public double rodLevel = 100;
    public double rodTarget = 100;

    public int typeLoaded = -1;
    public int amountLoaded;
    public double progress;
    public double processTime;

    public int rodCount;
    public int connections;
    public int connectionsControlled;
    public int heatexCount;
    public int heatsinkCount;
    public int channelCount;
    public int sourceCount;

    public int unloadDelay;
    public boolean assembled;

    private final List<BlockPos> ports = new ArrayList<>();
    private final List<BlockPos> rods = new ArrayList<>();

    public PWRControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 3, true, false);
        this.tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.COOLANT, 128_000).withOwner(this),
                new FluidTankNTM(Fluids.COOLANT_HOT, 128_000).withOwner(this)
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.pwrController");
    }

    /** The initial creation of the reactor: does all the pre-calculation, ported from CE 1:1. */
    public void setup(Map<BlockPos, BlockState> partMap, Map<BlockPos, BlockState> rodMap) {
        rodCount = 0;
        connections = 0;
        connectionsControlled = 0;
        heatexCount = 0;
        channelCount = 0;
        heatsinkCount = 0;
        sourceCount = 0;
        ports.clear();
        rods.clear();

        int connectionsDouble = 0;
        int connectionsControlledDouble = 0;

        for (Map.Entry<BlockPos, BlockState> entry : partMap.entrySet()) {
            Block block = entry.getValue().getBlock();

            if (block == PWRBlocks.PWR_FUELROD.get()) rodCount++;
            if (block == PWRPhase1Blocks.heatex()) heatexCount++;
            if (block == PWRBlocks.PWR_CHANNEL.get()) channelCount++;
            if (block == PWRPhase1Blocks.heatsink()) heatsinkCount++;
            if (block == PWRPhase1Blocks.neutronSource()) sourceCount++;
            if (block == PWRPhase1Blocks.port()) ports.add(entry.getKey());
        }

        for (Map.Entry<BlockPos, BlockState> entry : rodMap.entrySet()) {
            BlockPos fuelPos = entry.getKey();
            rods.add(fuelPos);

            for (Direction dir : Direction.values()) {
                boolean controlled = false;

                for (int i = 1; i < 16; i++) {
                    BlockPos checkPos = fuelPos.relative(dir, i);
                    BlockState stateAtPos = partMap.get(checkPos);
                    Block atPos = stateAtPos != null ? stateAtPos.getBlock() : null;

                    if (atPos == null || atPos == PWRPhase1Blocks.casing()) break;
                    if (atPos == PWRBlocks.PWR_CONTROL.get()) controlled = true;
                    if (atPos == PWRBlocks.PWR_FUELROD.get()) {
                        if (controlled) connectionsControlledDouble++;
                        else connectionsDouble++;
                        break;
                    }
                    if (atPos == PWRPhase1Blocks.reflector()) {
                        if (controlled) connectionsControlledDouble += 2;
                        else connectionsDouble += 2;
                        break;
                    }
                }
            }
        }

        connections = connectionsDouble / 2;
        connectionsControlled = connectionsControlledDouble / 2;
        heatsinkCount = Math.min(heatsinkCount, 80);

        this.coreHeatCapacity = CORE_HEAT_CAPACITY_BASE + this.heatsinkCount * (CORE_HEAT_CAPACITY_BASE / 20);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        // CE TileEntityPWRController.java:183
        if (this.amountLoaded <= 0) this.tanks[0].setType(2, inventory);
        setupTanks();

        if (unloadDelay > 0) unloadDelay--;

        BlockPos c = worldPosition;
        boolean chunksLoaded = level.isLoaded(c)
                && level.isLoaded(c.offset(32, 0, 32))
                && level.isLoaded(c.offset(32, 0, -32))
                && level.isLoaded(c.offset(-32, 0, 32))
                && level.isLoaded(c.offset(-32, 0, -32));
        if (!chunksLoaded) this.unloadDelay = 60;

        if (this.assembled) {
            for (BlockPos portPos : ports) {
                for (Direction dir : Direction.values()) {
                    BlockPos targetPos = portPos.relative(dir);

                    if (tanks[1].getFill() > 0) this.tryProvide(tanks[1], level, targetPos, dir);
                    if (level.getGameTime() % 20 == 0) this.trySubscribe(tanks[0].getTankType(), level, targetPos, dir);
                }
            }

            if (this.unloadDelay <= 0) {
                ItemStack rodStack = inventory.getStackInSlot(0);
                ItemStack rodHotStack = inventory.getStackInSlot(1);

                EnumPWRFuel freshType = rodStack.getItem() instanceof ItemPWRFuel fuelItem ? fuelItem.getType() : null;

                if ((typeLoaded == -1 || amountLoaded <= 0) && !rodStack.isEmpty() && freshType != null) {
                    typeLoaded = freshType.ordinal();
                    amountLoaded++;
                    rodStack.shrink(1);
                    this.setChanged();
                } else if (!rodStack.isEmpty() && freshType != null && freshType.ordinal() == typeLoaded && amountLoaded < rodCount) {
                    amountLoaded++;
                    rodStack.shrink(1);
                    this.setChanged();
                }

                double diff = this.rodLevel - this.rodTarget;
                if (diff < 1 && diff > -1) this.rodLevel = this.rodTarget;
                if (this.rodTarget > this.rodLevel) this.rodLevel++;
                if (this.rodTarget < this.rodLevel) this.rodLevel--;

                double multiplier = 1D;
                if (tanks[0].getTankType().hasTrait(FT_PWRModerator.class)) {
                    multiplier = tanks[0].getTankType().getTrait(FT_PWRModerator.class).getMultiplier();
                }

                double newFlux = this.sourceCount * 20D;

                if (typeLoaded != -1 && amountLoaded > 0) {
                    EnumPWRFuel fuel = EnumUtil.grabEnumSafely(EnumPWRFuel.VALUES, typeLoaded);
                    double usedRods = getTotalProcessMultiplier();
                    double fluxPerRod = (this.rodCount > 0) ? this.flux / this.rodCount : 0;
                    double outputPerRod = fuel.reactivity(fluxPerRod);
                    double totalOutput = outputPerRod * amountLoaded * usedRods;
                    double totalHeatOutput = totalOutput * fuel.heatEmission;

                    if (tanks[0].getFill() > 0) totalHeatOutput *= multiplier;

                    this.coreHeat += (long) totalHeatOutput;
                    newFlux += totalOutput;

                    this.processTime = fuel.yield;
                    this.progress += totalOutput;

                    if (this.progress >= this.processTime) {
                        this.progress -= this.processTime;

                        DeferredItem<Item> hotItem = PWRHotFuelItems.HOT_FUEL.get(fuel);
                        if (hotItem != null) {
                            if (rodHotStack.isEmpty()) {
                                inventory.setStackInSlot(1, new ItemStack(hotItem.get(), 1));
                            } else if (rodHotStack.getItem() == hotItem.get() && rodHotStack.getCount() < rodHotStack.getMaxStackSize()) {
                                rodHotStack.grow(1);
                            }
                        }

                        this.amountLoaded--;
                        this.setChanged();
                    }

                    // CE TileEntityPWRController.java:265-266
                    if (level.getGameTime() % 100 == 0) {
                        SatelliteRayScan.reportEvent(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                                SatelliteRayScan.RayEvent.INFO_NUCLEAR, 200);
                    }
                }

                if (this.amountLoaded <= 0) this.typeLoaded = -1;
                if (amountLoaded > rodCount) amountLoaded = rodCount;

                /* CORE COOLING */
                double coreCoolingApproachNum = getXOverE((double) this.heatexCount * 5 / getRodCountForCoolant(), 2) / 2D;
                long averageCoreHeat = (this.coreHeat + this.hullHeat) / 2;
                this.coreHeat -= (long) ((coreHeat - averageCoreHeat) * coreCoolingApproachNum);
                this.hullHeat -= (long) ((hullHeat - averageCoreHeat) * coreCoolingApproachNum);

                updateCoolant();

                this.coreHeat = (long) (this.coreHeat * 0.999D);
                this.hullHeat = (long) (this.hullHeat * 0.999D);

                this.flux = newFlux;
                if (tanks[0].getFill() > 0) this.flux *= multiplier;

                // CE: TileEntityPWRController.createAudioLoop() - continuous AudioWrapper loop
                // (HBMSoundHandler.reactorLoop, 10-tick keepAlive) while the reactor core is
                // generating flux. No looped-block-audio bridge ported yet (see
                // ChemPlantBlockEntity's identical note); substituted with a periodic broadcast
                // every 20 ticks while flux is positive.
                if (this.flux > 0 && level.getGameTime() % 20 == 0) {
                    level.playSound(null, worldPosition, HBMSoundHandler.reactorLoop.get(), SoundSource.BLOCKS, 1F, 1.0F);
                }

                if (this.coreHeat > this.coreHeatCapacity) meltDown();
            } else {
                this.hullHeat = 0;
                this.coreHeat = 0;
            }
        }

        dataChanged();
        networkPackNT(150);
    }

    @Override
    public DirPos[] getConPos() {
        if (!this.assembled || ports.isEmpty()) return new DirPos[0];
        DirPos[] result = new DirPos[ports.size() * 6];
        int idx = 0;
        for (BlockPos portPos : ports) {
            for (Direction dir : Direction.values()) {
                result[idx++] = new DirPos(portPos.relative(dir), dir);
            }
        }
        return result;
    }

    /**
     * Ported from CE's {@code meltDown()}. CE's {@code ModBlocks.corium_block} is a finite-spreading
     * fluid block ({@code CoriumFinite extends BlockFluidClassic}); this port has no world-fluid-block
     * system at all yet (Phase 1's own finding, restated in {@code docs/phase2/blockentity_base.md}),
     * so {@link PWRBlocks#CORIUM_BLOCK} is a plain solid block instead - the meltdown still replaces
     * every fuel-rod position and detonates, it just doesn't spread afterward. Documented scope-cut,
     * not a silent behavior change.
     */
    protected void meltDown() {
        if (level == null) return;
        level.removeBlock(this.worldPosition, false);

        if (rods.isEmpty()) return;

        double x = 0, y = 0, z = 0;

        for (BlockPos pos : this.rods) {
            level.setBlock(pos, PWRBlocks.CORIUM_BLOCK.get().defaultBlockState(), 3);
            x += pos.getX() + 0.5;
            y += pos.getY() + 0.5;
            z += pos.getZ() + 0.5;
        }

        x /= rods.size();
        y /= rods.size();
        z /= rods.size();

        level.explode(null, x, y, z, MELTDOWN_EXPLOSION_POWER, true, Level.ExplosionInteraction.TNT);
    }

    private void updateCoolant() {
        FT_Heatable trait = tanks[0].getTankType().getTrait(FT_Heatable.class);
        if (trait == null || trait.getEfficiency(HeatingType.PWR) <= 0) return;

        double coolingEff = (double) this.channelCount / getRodCountForCoolant() * 0.1D; // 10% cooling if numbers match
        if (coolingEff > 1D) coolingEff = 1D;

        int heatToUse = (int) Math.min(Math.min((double) this.hullHeat, this.hullHeat * coolingEff * trait.getEfficiency(HeatingType.PWR)), 2_000_000_000D);
        HeatingStep step = trait.getFirstStep();
        if (step.amountReq <= 0 || step.heatReq <= 0) return; // avoid division by zero
        int coolCycles = tanks[0].getFill() / step.amountReq;
        int hotCycles = (tanks[1].getMaxFill() - tanks[1].getFill()) / step.amountProduced;
        int heatCycles = heatToUse / step.heatReq;
        int cycles = Math.min(coolCycles, Math.min(hotCycles, heatCycles));

        this.hullHeat -= (long) step.heatReq * cycles;
        this.tanks[0].setFill(tanks[0].getFill() - step.amountReq * cycles);
        this.tanks[1].setFill(tanks[1].getFill() + step.amountProduced * cycles);
    }

    private void setupTanks() {
        FT_Heatable trait = tanks[0].getTankType().getTrait(FT_Heatable.class);
        if (trait == null || trait.getEfficiency(HeatingType.PWR) <= 0) {
            tanks[0].setTankType(Fluids.NONE);
            tanks[1].setTankType(Fluids.NONE);
            return;
        }
        tanks[1].setTankType(trait.getFirstStep().typeProduced);
    }

    protected int getRodCountForCoolant() {
        return this.rodCount + (int) Math.ceil(this.heatsinkCount / 4D);
    }

    public double getTotalProcessMultiplier() {
        double totalConnections = this.connections + this.connectionsControlled * (1D - (this.rodLevel / 100D));
        return connectinFunc(totalConnections);
    }

    public double connectinFunc(double connections) {
        return connections / 10D * (1D - getXOverE(connections, 300D)) + connections / 150D * getXOverE(connections, 300D);
    }

    public double getXOverE(double x, double d) {
        return 1 - Math.pow(Math.E, -x / d);
    }

    /** Stepped rod-level control, matching this port's own established slider-replacement convention (see {@link PWRControllerMenu}). */
    public void setRodTarget(double target) {
        this.rodTarget = Mth.clamp(target, 0, 100);
        this.setChanged();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return stack.getItem() instanceof ItemPWRFuel;
        // CE :483-486 returns false for slot 2; without this the ID never lands and setType is dead.
        if (slot == 2) return stack.getItem() instanceof IItemFluidIdentifier;
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        return slot == 1;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1};
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        return type == tanks[0].getTankType() || type == tanks[1].getTankType();
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tanks);
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(tanks[1]);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tanks[0]);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PWRControllerMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tanks[0].writeToNBT(tag, "t0");
        tanks[1].writeToNBT(tag, "t1");
        tag.putBoolean("assembled", assembled);
        tag.putLong("coreHeatL", coreHeat);
        tag.putLong("hullHeatL", hullHeat);
        tag.putDouble("flux", flux);
        tag.putDouble("rodLevel", rodLevel);
        tag.putDouble("rodTarget", rodTarget);
        tag.putInt("typeLoaded", typeLoaded);
        tag.putInt("amountLoaded", amountLoaded);
        tag.putDouble("progress", progress);
        tag.putDouble("processTime", processTime);
        tag.putLong("coreHeatCapacityL", coreHeatCapacity);

        tag.putInt("rodCount", rodCount);
        tag.putInt("connections", connections);
        tag.putInt("connectionsControlled", connectionsControlled);
        tag.putInt("heatexCount", heatexCount);
        tag.putInt("channelCount", channelCount);
        tag.putInt("sourceCount", sourceCount);
        tag.putInt("heatsinkCount", heatsinkCount);

        tag.putInt("portCount", ports.size());
        for (int i = 0; i < ports.size(); i++) {
            BlockPos p = ports.get(i);
            tag.putIntArray("p" + i, new int[]{p.getX(), p.getY(), p.getZ()});
        }

        tag.putInt("rods_list_size", rods.size());
        for (int i = 0; i < rods.size(); i++) {
            BlockPos p = rods.get(i);
            tag.putIntArray("r" + i, new int[]{p.getX(), p.getY(), p.getZ()});
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tanks[0].readFromNBT(tag, "t0");
        tanks[1].readFromNBT(tag, "t1");
        this.assembled = tag.getBoolean("assembled");
        this.coreHeat = tag.getLong("coreHeatL");
        this.hullHeat = tag.getLong("hullHeatL");
        this.flux = tag.getDouble("flux");
        this.rodLevel = tag.getDouble("rodLevel");
        this.rodTarget = tag.getDouble("rodTarget");
        this.typeLoaded = tag.getInt("typeLoaded");
        this.amountLoaded = tag.getInt("amountLoaded");
        this.progress = tag.getDouble("progress");
        this.processTime = tag.getDouble("processTime");
        this.coreHeatCapacity = tag.getLong("coreHeatCapacityL");
        if (this.coreHeatCapacity < CORE_HEAT_CAPACITY_BASE) this.coreHeatCapacity = CORE_HEAT_CAPACITY_BASE;

        this.rodCount = tag.getInt("rodCount");
        this.connections = tag.getInt("connections");
        this.connectionsControlled = tag.getInt("connectionsControlled");
        this.heatexCount = tag.getInt("heatexCount");
        this.channelCount = tag.getInt("channelCount");
        this.sourceCount = tag.getInt("sourceCount");
        this.heatsinkCount = tag.getInt("heatsinkCount");

        ports.clear();
        int portCount = tag.getInt("portCount");
        for (int i = 0; i < portCount; i++) {
            int[] p = tag.getIntArray("p" + i);
            if (p.length == 3) ports.add(new BlockPos(p[0], p[1], p[2]));
        }

        rods.clear();
        int rodListSize = tag.getInt("rods_list_size");
        for (int i = 0; i < rodListSize; i++) {
            if (tag.contains("r" + i)) {
                int[] p = tag.getIntArray("r" + i);
                if (p.length == 3) rods.add(new BlockPos(p[0], p[1], p[2]));
            }
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(this.assembled);
        buf.writeInt(this.rodCount);
        buf.writeLong(this.coreHeat);
        buf.writeLong(this.hullHeat);
        buf.writeDouble(this.flux);
        buf.writeDouble(this.processTime);
        buf.writeDouble(this.progress);
        buf.writeInt(this.typeLoaded);
        buf.writeInt(this.amountLoaded);
        buf.writeDouble(this.rodLevel);
        buf.writeDouble(this.rodTarget);
        buf.writeLong(this.coreHeatCapacity);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.assembled = buf.readBoolean();
        this.rodCount = buf.readInt();
        this.coreHeat = buf.readLong();
        this.hullHeat = buf.readLong();
        this.flux = buf.readDouble();
        this.processTime = buf.readDouble();
        this.progress = buf.readDouble();
        this.typeLoaded = buf.readInt();
        this.amountLoaded = buf.readInt();
        this.rodLevel = buf.readDouble();
        this.rodTarget = buf.readDouble();
        this.coreHeatCapacity = buf.readLong();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    public String[] getFunctionInfo() {
        // CE :609-611
        return new String[]{
                PREFIX_VALUE + "rods",
                PREFIX_VALUE + "coreheat",
                PREFIX_VALUE + "hullheat",
                PREFIX_VALUE + "flux",
                PREFIX_VALUE + "depletion",
                PREFIX_FUNCTION + "setrods" + NAME_SEPARATOR + "percent",
                PREFIX_FUNCTION + "jettison"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :614-620
        if ((PREFIX_VALUE + "rods").equals(name)) return "" + (int) this.rodLevel;
        if ((PREFIX_VALUE + "coreheat").equals(name)) return "" + this.coreHeat;
        if ((PREFIX_VALUE + "hullheat").equals(name)) return "" + this.hullHeat;
        if ((PREFIX_VALUE + "flux").equals(name)) return "" + (int) this.flux;
        if ((PREFIX_VALUE + "depletion").equals(name)) return "" + (int) (this.progress * 100 / this.processTime);
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        // CE :624-640
        if ((PREFIX_FUNCTION + "setrods").equals(name) && params.length > 0) {
            this.rodTarget = IRORInteractive.parseInt(params[0], 0, 100);
            setChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "jettison").equals(name)) {
            this.typeLoaded = -1;
            this.amountLoaded = 0;
            this.progress = 0;
            setChanged();
            return null;
        }
        return null;
    }
}
