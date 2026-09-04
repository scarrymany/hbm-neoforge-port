package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.machine.dummyable.FactoryDummyablePorts;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.machine.dummyable.MachineChemicalFactoryMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.chem.ChemPlantRecipes.ChemPlantRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * CE {@code TileEntityMachineChemicalFactory}: 32 slots, 4 chemplant lanes, 12+12 tanks @ 24000 +
 * water/lps 4000. Upgrade then {@code speed*2D, pow*2D}. Recipes = {@link ChemPlantRecipes}.
 * Named recipe + {@code receiveControl} index/selection — CE {@code TileEntityMachineChemicalFactory.java:428-436}.
 * TODO(CE: TileEntityMachineChemicalFactory.java:168-174): loadTank(10,13) leftover chem-plant slot
 * numbers overlap module outputs — factory container has no canister slots. Not copied.
 * TODO(CE: TileEntityMachineChemicalFactory.java:235-250): AudioWrapper chemicalPlant loop.
 * TODO(CE: TileEntityMachineChemicalFactory.java:351): NBT key collision {@code "i"+i} for in+out.
 * TODO(CE: TileEntityMachineChemicalFactory.java:382-416): CapabilityContextProvider coolant accessor.
 * TODO(CE: TileEntityMachineChemicalFactory.java:484-499): ProxyDyn coolant delegate.
 */
public class MachineChemicalFactoryBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider,
        IControlReceiver, IRORValueProvider {

    public static final int LANES = 4;
    public static final long BASE_MAX_POWER = 1_000_000L;
    public static final int TANK_CAPACITY = 24_000;
    public static final int COOL_CAPACITY = 4_000;
    public static final int COOL_PER_LANE = 100;

    private static final int[] HOPPER_SLOTS = {
            5, 6, 7, 8, 9, 10,
            12, 13, 14, 15, 16, 17,
            19, 20, 21, 22, 23, 24,
            26, 27, 28, 29, 30, 31
    };

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);

    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.POWER, 3);
        VALID_UPGRADES.put(UpgradeType.OVERDRIVE, 3);
    }

    public final FluidTankNTM[] inputTanks = new FluidTankNTM[12];
    public final FluidTankNTM[] outputTanks = new FluidTankNTM[12];
    public final FluidTankNTM water;
    public final FluidTankNTM lps;
    private final List<FluidTankNTM> allTanks = new ArrayList<>();

    private final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(VALID_UPGRADES);

    public long power;
    public long maxPower = BASE_MAX_POWER;
    public boolean[] didProcess = new boolean[LANES];
    public double[] progress = new double[LANES];
    public String[] recipes = new String[]{"null", "null", "null", "null"};

    public MachineChemicalFactoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 32, true, true);
        for (int i = 0; i < 12; i++) {
            inputTanks[i] = new FluidTankNTM(Fluids.NONE, TANK_CAPACITY).withOwner(this);
            outputTanks[i] = new FluidTankNTM(Fluids.NONE, TANK_CAPACITY).withOwner(this);
            allTanks.add(inputTanks[i]);
        }
        for (int i = 0; i < 12; i++) allTanks.add(outputTanks[i]);
        water = new FluidTankNTM(Fluids.WATER, COOL_CAPACITY).withOwner(this);
        lps = new FluidTankNTM(Fluids.SPENTSTEAM, COOL_CAPACITY).withOwner(this);
        allTanks.add(water);
        allTanks.add(lps);
    }

    public static int blueprintSlot(int lane) {
        return 4 + lane * 7;
    }

    public static int itemIn(int lane, int n) {
        return 5 + lane * 7 + n;
    }

    public static int itemOut(int lane, int n) {
        return 8 + lane * 7 + n;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineChemicalFactory");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot >= 1 && slot <= 3) return stack.getItem() instanceof ItemMachineUpgrade;
        for (int i = 0; i < LANES; i++) {
            if (slot == blueprintSlot(i)) return stack.getItem() instanceof ItemBlueprints;
            if (slot >= itemIn(i, 0) && slot <= itemIn(i, 2)) return true;
        }
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        if (slot >= 8 && slot <= 10) return true;
        if (slot >= 15 && slot <= 17) return true;
        if (slot >= 22 && slot <= 24) return true;
        return slot >= 29 && slot <= 31;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return HOPPER_SLOTS;
    }

    public boolean canCool() {
        return water.getFill() >= COOL_PER_LANE && lps.getFill() <= lps.getMaxFill() - COOL_PER_LANE;
    }

    public DirPos[] getConPos() {
        return FactoryDummyablePorts.getConPos(worldPosition, FactoryDummyablePorts.coreFacing(getBlockState()));
    }

    public DirPos[] getCoolPos() {
        return FactoryDummyablePorts.getCoolPos(worldPosition, FactoryDummyablePorts.coreFacing(getBlockState()));
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        if (maxPower <= 0) maxPower = 10_000_000;

        upgradeManager.checkSlots(inventory, 1, 3);
        power = Library.chargeTEFromItems(inventory, 0, power, maxPower);

        for (DirPos pos : getConPos()) {
            trySubscribe(level, pos);
            for (FluidTankNTM tank : inputTanks) {
                if (tank.getTankType() != Fluids.NONE) trySubscribe(tank.getTankType(), level, pos);
            }
            for (FluidTankNTM tank : outputTanks) {
                if (tank.getFill() > 0) tryProvide(tank, level, pos);
            }
        }
        for (DirPos pos : getCoolPos()) {
            trySubscribe(level, pos);
            trySubscribe(water.getTankType(), level, pos);
            if (lps.getFill() > 0) tryProvide(lps, level, pos);
        }

        double speed = 1D;
        double pow = 1D;
        speed += Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3) / 3D;
        speed += Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);
        pow -= Math.min(upgradeManager.getLevel(UpgradeType.POWER), 3) * 0.25D;
        pow += Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
        pow += Math.min(upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3) * 10D / 3D;
        speed *= 2D;
        pow *= 2D;

        long nextMax = 0;
        for (int i = 0; i < LANES; i++) {
            ChemPlantRecipe selected = ChemicalPlantRecipes.byName(recipes[i]);
            if (selected != null) {
                nextMax += selected.power * 100;
                retargetEmptyTanks(i, selected);
            }
            ChemPlantRecipe recipe = findRecipe(i);
            didProcess[i] = false;
            if (recipe == null) {
                progress[i] = 0;
                continue;
            }
            nextMax += recipe.power * 100;
            if (canCool() && canProcess(i, recipe, speed, pow)) {
                process(i, recipe, speed, pow);
                didProcess[i] = true;
                water.setFill(water.getFill() - COOL_PER_LANE);
                lps.setFill(lps.getFill() + COOL_PER_LANE);
            } else {
                progress[i] = 0;
            }
        }
        maxPower = Math.max(Math.max(power, nextMax), BASE_MAX_POWER);

        for (FluidTankNTM in : inputTanks) {
            if (in.getTankType() == Fluids.NONE) continue;
            for (FluidTankNTM out : outputTanks) {
                if (out.getTankType() != in.getTankType()) continue;
                if (out.getPressure() != in.getPressure()) continue;
                int toMove = Math.min(Math.min(in.getMaxFill() - in.getFill(), out.getFill()), 50);
                if (toMove > 0) {
                    in.setFill(in.getFill() + toMove);
                    out.setFill(out.getFill() - toMove);
                }
            }
        }

        if ((didProcess[0] || didProcess[1] || didProcess[2] || didProcess[3]) && level.getGameTime() % 20 == 0) {
            level.playSound(null, worldPosition, HBMSoundHandler.chemicalPlant.get(), SoundSource.BLOCKS, 1F, 1.0F);
        }

        dataChanged();
        networkPackMK2(100);
    }

    @Nullable
    private ChemPlantRecipe findRecipe(int lane) {
        ChemPlantRecipe recipe = ChemicalPlantRecipes.byName(recipes[lane]);
        if (recipe == null) return null;
        if (matchesItems(lane, recipe) && matchesFluids(lane, recipe)) return recipe;
        return null;
    }

    private boolean matchesItems(int lane, ChemPlantRecipe recipe) {
        boolean[] used = new boolean[3];
        for (AStack key : recipe.inputItems) {
            boolean found = false;
            for (int i = 0; i < 3; i++) {
                if (used[i]) continue;
                if (key.matchesRecipe(inventory.getStackInSlot(itemIn(lane, i)), false)) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    @Nullable
    private FluidTankNTM findInputTank(int lane, FluidType type) {
        for (int i = 0; i < 3; i++) {
            FluidTankNTM tank = inputTanks[lane * 3 + i];
            if (tank.getTankType() == type) return tank;
        }
        return null;
    }

    private boolean matchesFluids(int lane, ChemPlantRecipe recipe) {
        for (FluidStack need : recipe.inputFluids) {
            FluidTankNTM tank = findInputTank(lane, need.type);
            if (tank == null || tank.getFill() < need.fill) return false;
        }
        return true;
    }

    private void retargetEmptyTanks(int lane, ChemPlantRecipe recipe) {
        for (FluidStack need : recipe.inputFluids) {
            if (findInputTank(lane, need.type) != null) continue;
            for (int i = 0; i < 3; i++) {
                FluidTankNTM tank = inputTanks[lane * 3 + i];
                if (tank.getFill() == 0) {
                    tank.setTankType(need.type);
                    break;
                }
            }
        }
    }

    private boolean hasOutputSpace(int lane, ChemPlantRecipe recipe) {
        for (int i = 0; i < recipe.outputItems.length && i < 3; i++) {
            if (!inventory.insertItem(itemOut(lane, i), recipe.outputItems[i], true).isEmpty()) return false;
        }
        for (FluidStack out : recipe.outputFluids) {
            boolean placed = false;
            for (int i = 0; i < 3; i++) {
                FluidTankNTM tank = outputTanks[lane * 3 + i];
                if ((tank.getTankType() == out.type || tank.getTankType() == Fluids.NONE)
                        && tank.getFill() + out.fill <= tank.getMaxFill()) {
                    placed = true;
                    break;
                }
            }
            if (!placed) return false;
        }
        return true;
    }

    private boolean canProcess(int lane, ChemPlantRecipe recipe, double speed, double pow) {
        long cost = pow == 1 ? recipe.power : (long) (recipe.power * pow);
        return power >= cost && hasOutputSpace(lane, recipe);
    }

    private void process(int lane, ChemPlantRecipe recipe, double speed, double pow) {
        long cost = pow == 1 ? recipe.power : (long) (recipe.power * pow);
        power -= cost;
        progress[lane] += Math.min(speed / Math.max(1, recipe.duration), 1D);
        if (progress[lane] < 1D) return;
        consume(lane, recipe);
        if (canProcess(lane, recipe, speed, pow) && findRecipe(lane) != null) {
            progress[lane] -= 1D;
        } else {
            progress[lane] = 0;
        }
    }

    private void consume(int lane, ChemPlantRecipe recipe) {
        boolean[] used = new boolean[3];
        for (AStack key : recipe.inputItems) {
            for (int i = 0; i < 3; i++) {
                if (used[i]) continue;
                if (key.matchesRecipe(inventory.getStackInSlot(itemIn(lane, i)), false)) {
                    inventory.extractItem(itemIn(lane, i), key.count(), false);
                    used[i] = true;
                    break;
                }
            }
        }
        for (FluidStack need : recipe.inputFluids) {
            FluidTankNTM tank = findInputTank(lane, need.type);
            if (tank != null) tank.setFill(tank.getFill() - need.fill);
        }
        for (int i = 0; i < recipe.outputItems.length && i < 3; i++) {
            inventory.insertItem(itemOut(lane, i), recipe.outputItems[i].copy(), false);
        }
        for (FluidStack out : recipe.outputFluids) {
            for (int i = 0; i < 3; i++) {
                FluidTankNTM tank = outputTanks[lane * 3 + i];
                if (tank.getTankType() == out.type || tank.getTankType() == Fluids.NONE) {
                    tank.setTankType(out.type);
                    tank.setFill(tank.getFill() + out.fill);
                    break;
                }
            }
        }
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
    public List<FluidTankNTM> getReceivingTanks() {
        List<FluidTankNTM> tanks = new ArrayList<>(List.of(inputTanks));
        tanks.add(water);
        return tanks;
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        List<FluidTankNTM> tanks = new ArrayList<>(List.of(outputTanks));
        tanks.add(lps);
        return tanks;
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return allTanks;
    }

    @Override
    public long getDemand(FluidType type, int pressure) {
        long amount = 0;
        for (FluidTankNTM tank : inputTanks) {
            if (tank.getPressure() != pressure) continue;
            if (tank.getTankType() == type || (tank.getTankType() == Fluids.NONE && tank.getFill() == 0)) {
                amount += tank.getMaxFill() - tank.getFill();
            }
        }
        if (type == water.getTankType() && water.getPressure() == pressure) {
            amount += water.getMaxFill() - water.getFill();
        }
        return amount;
    }

    @Override
    public long transferFluid(FluidType type, int pressure, long amount) {
        if (type == water.getTankType() && water.getPressure() == pressure) {
            int toAdd = (int) Math.min(amount, water.getMaxFill() - water.getFill());
            water.setFill(water.getFill() + toAdd);
            amount -= toAdd;
        }
        for (FluidTankNTM tank : inputTanks) {
            if (amount <= 0) break;
            if (tank.getPressure() != pressure) continue;
            if (tank.getTankType() == Fluids.NONE && tank.getFill() == 0) tank.setTankType(type);
            if (tank.getTankType() != type) continue;
            int toAdd = (int) Math.min(amount, tank.getMaxFill() - tank.getFill());
            tank.setFill(tank.getFill() + toAdd);
            amount -= toAdd;
        }
        return amount;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < 12; i++) {
            inputTanks[i].writeToNBT(tag, "i" + i);
            outputTanks[i].writeToNBT(tag, "o" + i);
        }
        for (int i = 0; i < LANES; i++) {
            tag.putDouble("progress" + i, progress[i]);
            tag.putString("recipe" + i, recipes[i]);
        }
        water.writeToNBT(tag, "w");
        lps.writeToNBT(tag, "s");
        tag.putLong("power", power);
        tag.putLong("maxPower", maxPower);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < 12; i++) {
            inputTanks[i].readFromNBT(tag, "i" + i);
            outputTanks[i].readFromNBT(tag, "o" + i);
        }
        for (int i = 0; i < LANES; i++) {
            progress[i] = tag.getDouble("progress" + i);
            recipes[i] = tag.contains("recipe" + i) ? tag.getString("recipe" + i) : "null";
        }
        water.readFromNBT(tag, "w");
        lps.readFromNBT(tag, "s");
        power = tag.getLong("power");
        maxPower = Math.max(tag.getLong("maxPower"), BASE_MAX_POWER);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.serialize(buf);
        for (FluidTankNTM tank : outputTanks) tank.serialize(buf);
        water.serialize(buf);
        lps.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        for (int i = 0; i < LANES; i++) {
            buf.writeBoolean(didProcess[i]);
            buf.writeDouble(progress[i]);
            buf.writeUtf(recipes[i]);
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.deserialize(buf);
        for (FluidTankNTM tank : outputTanks) tank.deserialize(buf);
        water.deserialize(buf);
        lps.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        for (int i = 0; i < LANES; i++) {
            didProcess[i] = buf.readBoolean();
            progress[i] = buf.readDouble();
            recipes[i] = buf.readUtf();
        }
    }

    @Override
    public boolean hasPermission(Player player) {
        return isUseableByPlayer(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("index") && data.contains("selection")) {
            int index = data.getInt("index");
            String selection = data.getString("selection");
            if (index >= 0 && index < LANES) {
                this.recipes[index] = selection;
                setChanged();
            }
        }
    }

    @Override
    public String[] getFunctionInfo() {
        // CE :526-532
        return new String[]{
                PREFIX_VALUE + "progress1", PREFIX_VALUE + "progress2", PREFIX_VALUE + "progress3", PREFIX_VALUE + "progress4",
                PREFIX_VALUE + "recipe1", PREFIX_VALUE + "recipe2", PREFIX_VALUE + "recipe3", PREFIX_VALUE + "recipe4",
                PREFIX_VALUE + "anyactive",
                PREFIX_VALUE + "active1", PREFIX_VALUE + "active2", PREFIX_VALUE + "active3", PREFIX_VALUE + "active4"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :536-543 — chemplantModule.progress / getRecipeName() → port progress[] / recipes[]
        if ((PREFIX_VALUE + "anyactive").equals(name)) {
            return "" + ((this.didProcess[0] || this.didProcess[1] || this.didProcess[2] || this.didProcess[3]) ? 1 : 0);
        }
        for (int i = 0; i < 4; i++) {
            if ((PREFIX_VALUE + "progress" + (i + 1)).equals(name)) return "" + (int) Math.round(this.progress[i] * 100);
            if ((PREFIX_VALUE + "recipe" + (i + 1)).equals(name)) return this.recipes[i];
            if ((PREFIX_VALUE + "active" + (i + 1)).equals(name)) return "" + (this.didProcess[i] ? 1 : 0);
        }
        return null;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineChemicalFactoryMenu(containerId, playerInventory, this);
    }
}
