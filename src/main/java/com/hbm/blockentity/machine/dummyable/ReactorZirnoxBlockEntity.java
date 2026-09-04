package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blockentity.network.IConnectionAnchors;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.inventory.container.machine.dummyable.ReactorZirnoxMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemZirnoxRod;
import com.hbm.items.machine.ItemZirnoxRod.EnumZirnoxType;
import com.hbm.items.machine.ItemZirnoxRodDepleted;
import com.hbm.items.machine.ItemZirnoxRodDepleted.EnumZirnoxTypeDepleted;
import com.hbm.items.machine.MachineItems;
import com.hbm.items.weapon.WeaponMeleeItems;
import com.hbm.lib.DirPos;
import com.hbm.saveddata.satellites.SatelliteRayScan;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * CE {@code TileEntityReactorZirnox}: 28 slots, SHS 8000 / CO2 16000 / water 32000,
 * maxHeat/maxPressure 100000.
 * IConnectionAnchors + checkTilt(CONFIG) / 3×3 floor / standardFloor5x5 Exact CE
 *   TileEntityReactorZirnox.java:229 + :658-659. CE :198-235 = getNeighbouringSlots
 *   + updateConnections (no own FluidNode).
 * SatelliteRayScan.INFO_NUCLEAR Exact CE TileEntityReactorZirnox.java:267-268.
 * TODO(CE: TileEntityReactorZirnox.java:354-431): EntityZirnoxDebris / zirnox_destroyed / AuxParticle / ExplosionNukeGeneric.waste / achZIRNOXBoom / elementals.
 * ROR: CE {@code TileEntityReactorZirnox.java:617-656}.
 * TODO(CE: TileEntityReactorZirnox.java:508-605): OpenComputers callbacks.
 */
public class ReactorZirnoxBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardTransceiverMK2, IConnectionAnchors, ITickableBE, MenuProvider,
        IRORValueProvider, IRORInteractive {

    public static final int MAX_HEAT = 100_000;
    public static final int MAX_PRESSURE = 100_000;
    private static final int[] SLOTS_IO = new int[24];

    static {
        for (int i = 0; i < 24; i++) SLOTS_IO[i] = i;
    }

    public int heat;
    public int pressure;
    public boolean isOn;
    public final FluidTankNTM steam;
    public final FluidTankNTM carbonDioxide;
    public final FluidTankNTM water;
    protected int output;
    private boolean redstonePowered;

    private static Map<Item, Item> fuelMap;

    public ReactorZirnoxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 28, true, false);
        this.steam = new FluidTankNTM(Fluids.SUPERHOTSTEAM, 8000).withOwner(this);
        this.carbonDioxide = new FluidTankNTM(Fluids.CARBONDIOXIDE, 16_000).withOwner(this);
        this.water = new FluidTankNTM(Fluids.WATER, 32_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.zirnox");
    }

    public void setRedstonePowered(boolean powered) {
        if (!powered && this.redstonePowered) {
            isOn = false;
        }
        this.redstonePowered = powered;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return SLOTS_IO;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot < 24 && stack.getItem() instanceof ItemZirnoxRod;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot < 24 && !(stack.getItem() instanceof ItemZirnoxRod);
    }

    public int getGaugeScaled(int i, int type) {
        return switch (type) {
            case 0 -> (steam.getFill() * i) / steam.getMaxFill();
            case 1 -> (carbonDioxide.getFill() * i) / carbonDioxide.getMaxFill();
            case 2 -> (water.getFill() * i) / water.getMaxFill();
            case 3 -> (this.heat * i) / MAX_HEAT;
            case 4 -> (this.pressure * i) / MAX_PRESSURE;
            default -> 1;
        };
    }

    private static int[] getNeighbouringSlots(int id) {
        return switch (id) {
            case 0 -> new int[]{1, 7};
            case 1 -> new int[]{0, 2, 8};
            case 2 -> new int[]{1, 9};
            case 3 -> new int[]{4, 10};
            case 4 -> new int[]{3, 5, 11};
            case 5 -> new int[]{4, 6, 12};
            case 6 -> new int[]{5, 13};
            case 7 -> new int[]{0, 8, 14};
            case 8 -> new int[]{1, 7, 9, 15};
            case 9 -> new int[]{2, 8, 16};
            case 10 -> new int[]{3, 11, 17};
            case 11 -> new int[]{4, 10, 12, 18};
            case 12 -> new int[]{5, 11, 13, 19};
            case 13 -> new int[]{6, 12, 20};
            case 14 -> new int[]{7, 15, 21};
            case 15 -> new int[]{8, 14, 16, 22};
            case 16 -> new int[]{9, 15, 23};
            case 17 -> new int[]{10, 18};
            case 18 -> new int[]{11, 17, 19};
            case 19 -> new int[]{12, 18, 20};
            case 20 -> new int[]{13, 19};
            case 21 -> new int[]{14, 22};
            case 22 -> new int[]{15, 21, 23};
            case 23 -> new int[]{16, 22};
            default -> new int[0];
        };
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        // CE TileEntityReactorZirnox.java:229 — CONFIG gravity before rods / fluid IO
        checkTilt(TiltType.CONFIG, true);

        if (redstonePowered) {
            isOn = true;
        }
        this.output = 0;

        if (!this.tilted && level.getGameTime() % 20 == 0) {
            updateConnections();
        }

        carbonDioxide.loadTank(24, 26, inventory);
        water.loadTank(25, 27, inventory);

        if (isOn) {
            for (int i = 0; i < 24; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    if (stack.getItem() instanceof ItemZirnoxRod) {
                        decay(i);
                    } else if (stack.getItem() == WeaponMeleeItems.METEORITE_SWORD_BRED.get()) {
                        inventory.setStackInSlot(i, new ItemStack(WeaponMeleeItems.METEORITE_SWORD_IRRADIATED.get()));
                    }
                }
            }
        }

        this.pressure = (this.carbonDioxide.getFill() * 2)
                + (int) ((float) this.heat * ((float) this.carbonDioxide.getFill() / (float) this.carbonDioxide.getMaxFill()));

        if (this.heat > 0 && this.heat < MAX_HEAT) {
            if (this.water.getFill() > 0 && this.carbonDioxide.getFill() > 0 && this.steam.getFill() < this.steam.getMaxFill()) {
                generateSteam();
                this.heat -= (int) ((float) this.heat * (float) this.pressure / 1_000_000F);
            } else {
                this.heat -= 10;
            }

            // CE TileEntityReactorZirnox.java:267-268
            if (level.getGameTime() % 100 == 0) {
                SatelliteRayScan.reportEvent(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                        SatelliteRayScan.RayEvent.INFO_NUCLEAR, 200);
            }
        }

        if (!this.tilted) {
            for (DirPos pos : getConPos()) {
                this.tryProvide(steam, level, pos);
            }
        }

        checkIfMeltdown();
        dataChanged();
        networkPackNT(150);
    }

    private void generateSteam() {
        if (this.heat > 10256) {
            int cycle = (int) ((((float) heat - 10256F) / (float) MAX_HEAT)
                    * Math.min(((float) carbonDioxide.getFill() / 14000F), 1F) * 25F * 7.5F);
            this.output = cycle;
            water.setFill(water.getFill() - cycle);
            steam.setFill(steam.getFill() + cycle);
            if (water.getFill() < 0) water.setFill(0);
            if (steam.getFill() > steam.getMaxFill()) steam.setFill(steam.getMaxFill());
        }
    }

    private boolean hasFuelRod(int id) {
        ItemStack stack = inventory.getStackInSlot(id);
        if (stack.getItem() instanceof ItemZirnoxRod rod) {
            return !rod.getType().breeding;
        }
        return false;
    }

    private int getNeighbourCount(int id) {
        int count = 0;
        for (int neighbour : getNeighbouringSlots(id)) {
            if (hasFuelRod(neighbour)) count++;
        }
        return count;
    }

    private void decay(int id) {
        ItemStack stack = inventory.getStackInSlot(id);
        if (!(stack.getItem() instanceof ItemZirnoxRod rod)) return;
        EnumZirnoxType num = rod.getType();
        int decay = getNeighbourCount(id);
        if (!num.breeding) decay++;
        for (int i = 0; i < decay; i++) {
            this.heat += num.heat;
            ItemZirnoxRod.incrementLifeTime(stack);
            if (ItemZirnoxRod.getLifeTime(stack) > num.maxLife) {
                Item out = fuelMap().get(stack.getItem());
                if (out != null) inventory.setStackInSlot(id, new ItemStack(out));
                break;
            }
        }
    }

    private void checkIfMeltdown() {
        if (this.pressure > MAX_PRESSURE || this.heat > MAX_HEAT) {
            meltdown();
        }
    }

    private void meltdown() {
        if (level == null) return;
        for (int i = 0; i < inventory.getSlots(); i++) {
            inventory.setStackInSlot(i, ItemStack.EMPTY);
        }
        level.explode(null, worldPosition.getX(), worldPosition.getY() + 3, worldPosition.getZ(),
                12.0F, true, Level.ExplosionInteraction.TNT);
        ChunkRadiationManager.proxy.incrementRad(level, worldPosition, 50F, 15000F);
    }

    private void updateConnections() {
        if (this.tilted) return;
        for (DirPos pos : getConPos()) {
            this.trySubscribe(water.getTankType(), level, pos);
            this.trySubscribe(carbonDioxide.getTankType(), level, pos);
        }
    }

    @Override
    public int getFloorCount() {
        return 3 * 3;
    }

    @Override
    public BlockPos getFloorPosFromIndex(int index) {
        return standardFloor5x5(index);
    }

    @Override
    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + rot.getStepX() * 3, y + 1, z + rot.getStepZ() * 3, rot),
                new DirPos(x + rot.getStepX() * 3, y + 3, z + rot.getStepZ() * 3, rot),
                new DirPos(x + rot.getStepX() * -3, y + 1, z + rot.getStepZ() * -3, rot.getOpposite()),
                new DirPos(x + rot.getStepX() * -3, y + 3, z + rot.getStepZ() * -3, rot.getOpposite())
        };
    }

    private Direction coreFacing() {
        int meta = getBlockState().getValue(BlockDummyable.META);
        return meta >= 12 ? Direction.from3DDataValue(meta - BlockDummyable.offset) : Direction.NORTH;
    }

    public void toggleControl() {
        if (!redstonePowered) {
            this.isOn = !this.isOn;
            setChanged();
        }
    }

    public void vent() {
        carbonDioxide.setFill(Math.max(carbonDioxide.getFill() - 1000, 0));
        setChanged();
    }

    private static Map<Item, Item> fuelMap() {
        if (fuelMap == null) {
            fuelMap = new IdentityHashMap<>();
            put(EnumZirnoxType.NATURAL_URANIUM_FUEL, EnumZirnoxTypeDepleted.NATURAL_URANIUM_FUEL);
            put(EnumZirnoxType.URANIUM_FUEL, EnumZirnoxTypeDepleted.URANIUM_FUEL);
            fuelMap.put(MachineItems.ZIRNOX_RODS.get(EnumZirnoxType.TH232_FUEL).get(),
                    MachineItems.ZIRNOX_RODS.get(EnumZirnoxType.THORIUM_FUEL).get());
            put(EnumZirnoxType.THORIUM_FUEL, EnumZirnoxTypeDepleted.THORIUM_FUEL);
            put(EnumZirnoxType.MOX_FUEL, EnumZirnoxTypeDepleted.MOX_FUEL);
            put(EnumZirnoxType.PLUTONIUM_FUEL, EnumZirnoxTypeDepleted.PLUTONIUM_FUEL);
            put(EnumZirnoxType.U233_FUEL, EnumZirnoxTypeDepleted.U233_FUEL);
            put(EnumZirnoxType.U235_FUEL, EnumZirnoxTypeDepleted.U235_FUEL);
            put(EnumZirnoxType.LES_FUEL, EnumZirnoxTypeDepleted.LES_FUEL);
            fuelMap.put(MachineItems.ZIRNOX_RODS.get(EnumZirnoxType.LITHIUM_FUEL).get(),
                    MachineItems.ROD_ZIRNOX_TRITIUM.get());
            put(EnumZirnoxType.ZFB_MOX_FUEL, EnumZirnoxTypeDepleted.ZFB_MOX_FUEL);
        }
        return fuelMap;
    }

    private static void put(EnumZirnoxType fresh, EnumZirnoxTypeDepleted depleted) {
        fuelMap.put(MachineItems.ZIRNOX_RODS.get(fresh).get(), MachineItems.ZIRNOX_RODS_DEPLETED.get(depleted).get());
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(steam);
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(water, carbonDioxide);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(water, steam, carbonDioxide);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("heat", heat);
        tag.putInt("pressure", pressure);
        tag.putBoolean("isOn", isOn);
        steam.writeToNBT(tag, "steam");
        carbonDioxide.writeToNBT(tag, "carbondioxide");
        water.writeToNBT(tag, "water");
        tag.putBoolean("redstonePowered", redstonePowered);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heat = tag.getInt("heat");
        pressure = tag.getInt("pressure");
        isOn = tag.getBoolean("isOn");
        steam.readFromNBT(tag, "steam");
        carbonDioxide.readFromNBT(tag, "carbondioxide");
        water.readFromNBT(tag, "water");
        redstonePowered = tag.getBoolean("redstonePowered");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(heat);
        buf.writeInt(pressure);
        buf.writeBoolean(isOn);
        buf.writeBoolean(redstonePowered);
        steam.serialize(buf);
        carbonDioxide.serialize(buf);
        water.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        heat = buf.readInt();
        pressure = buf.readInt();
        isOn = buf.readBoolean();
        redstonePowered = buf.readBoolean();
        steam.deserialize(buf);
        carbonDioxide.deserialize(buf);
        water.deserialize(buf);
    }

    @Override
    public String[] getFunctionInfo() {
        // CE :618-628
        return new String[]{
                PREFIX_VALUE + "heat",
                PREFIX_VALUE + "pressure",
                PREFIX_VALUE + "water",
                PREFIX_VALUE + "steam",
                PREFIX_VALUE + "co2",
                PREFIX_VALUE + "state",
                PREFIX_FUNCTION + "setState" + NAME_SEPARATOR + "active (0 or 1)",
                PREFIX_FUNCTION + "ventCO2"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :632-639
        if ((PREFIX_VALUE + "heat").equals(name)) return "" + (int) Math.round(heat * 1.0E-5D * 780.0D + 20.0D);
        if ((PREFIX_VALUE + "pressure").equals(name)) return "" + (int) Math.round(pressure * 1.0E-5D * 30.0D);
        if ((PREFIX_VALUE + "water").equals(name)) return "" + water.getFill();
        if ((PREFIX_VALUE + "steam").equals(name)) return "" + steam.getFill();
        if ((PREFIX_VALUE + "co2").equals(name)) return "" + carbonDioxide.getFill();
        if ((PREFIX_VALUE + "state").equals(name)) return isOn ? "1" : "0";
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        // CE :643-655
        if ((PREFIX_FUNCTION + "setState").equals(name) && params.length > 0) {
            if (redstonePowered) return null;
            this.isOn = IRORInteractive.parseInt(params[0], 0, 1) == 1;
            setChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "ventCO2").equals(name)) {
            carbonDioxide.setFill(Math.max(carbonDioxide.getFill() - 1000, 0));
            setChanged();
            return null;
        }
        return null;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ReactorZirnoxMenu(id, inv, this);
    }
}
