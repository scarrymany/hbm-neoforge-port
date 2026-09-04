package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.machine.dummyable.FactoryDummyablePorts;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.machine.dummyable.MachineAssemblyFactoryMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.recipes.AssemblerRecipe;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
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
 * CE {@code TileEntityMachineAssemblyFactory}: 60 slots, 4 assembler lanes, tanks 4+4 @ 4000 +
 * water/lps 4000. Upgrade then {@code speed*2D, pow*2D}. Recipes = {@link ProcessingRecipes#ASSEMBLER_TYPE}.
 * Named recipe + {@code receiveControl} index/selection — CE {@code TileEntityMachineAssemblyFactory.java:390-398}.
 * TODO(CE: TileEntityMachineAssemblyFactory.java:213): CE passes blueprint slot {@code 4+i*7} (chem-factory
 * copy-paste); container/isItemValid use {@code 4+i*14} — this port uses 14.
 * TODO(CE: TileEntityMachineAssemblyFactory.java:228-266): AudioWrapper motor loop + AssemfacArm.
 * TODO(CE: TileEntityMachineAssemblyFactory.java:351): NBT key collision {@code "i"+i} for in+out tanks.
 * TODO(CE: TileEntityMachineAssemblyFactory.java:446-461): ProxyDyn coolant delegate / IConnectionAnchors.
 */
public class MachineAssemblyFactoryBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider,
        IControlReceiver, IRORValueProvider {

    public static final int LANES = 4;
    public static final long BASE_MAX_POWER = 1_000_000L;
    public static final int TANK_CAPACITY = 4_000;
    public static final int COOL_PER_LANE = 100;

    private static final int[] HOPPER_SLOTS = {
            5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
            19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
            33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45,
            47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59
    };

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);

    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.POWER, 3);
        VALID_UPGRADES.put(UpgradeType.OVERDRIVE, 3);
    }

    public final FluidTankNTM[] inputTanks = new FluidTankNTM[LANES];
    public final FluidTankNTM[] outputTanks = new FluidTankNTM[LANES];
    public final FluidTankNTM water;
    public final FluidTankNTM lps;
    private final List<FluidTankNTM> allTanks = new ArrayList<>();

    private final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(VALID_UPGRADES);

    public long power;
    public long maxPower = BASE_MAX_POWER;
    public boolean[] didProcess = new boolean[LANES];
    public double[] progress = new double[LANES];
    public String[] recipes = new String[]{"null", "null", "null", "null"};

    public MachineAssemblyFactoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 60, true, true);
        for (int i = 0; i < LANES; i++) {
            inputTanks[i] = new FluidTankNTM(Fluids.NONE, TANK_CAPACITY).withOwner(this);
            outputTanks[i] = new FluidTankNTM(Fluids.NONE, TANK_CAPACITY).withOwner(this);
            allTanks.add(inputTanks[i]);
        }
        for (int i = 0; i < LANES; i++) allTanks.add(outputTanks[i]);
        water = new FluidTankNTM(Fluids.WATER, TANK_CAPACITY).withOwner(this);
        lps = new FluidTankNTM(Fluids.SPENTSTEAM, TANK_CAPACITY).withOwner(this);
        allTanks.add(water);
        allTanks.add(lps);
    }

    public static int blueprintSlot(int lane) {
        return 4 + lane * 14;
    }

    public static int inputStart(int lane) {
        return 5 + lane * 14;
    }

    public static int outputSlot(int lane) {
        return 17 + lane * 14;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineAssemblyFactory");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot >= 1 && slot <= 3) return stack.getItem() instanceof ItemMachineUpgrade;
        for (int i = 0; i < LANES; i++) {
            if (slot == blueprintSlot(i)) return stack.getItem() instanceof ItemBlueprints;
            if (slot >= inputStart(i) && slot < inputStart(i) + 12) return true;
        }
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack itemStack, int amount) {
        for (int i = 0; i < LANES; i++) if (slot == outputSlot(i)) return true;
        return false;
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
            AssemblerRecipe selected = AssemblyMachineRecipes.byName(level, recipes[i]);
            if (selected != null) {
                nextMax += selected.getPower() * 100;
                retargetInputTank(i, selected);
            }
            AssemblerRecipe recipe = findMatchingRecipe(i);
            didProcess[i] = false;
            if (recipe == null) {
                progress[i] = 0;
                continue;
            }
            nextMax += recipe.getPower() * 100;
            setupTanks(i, recipe);
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

        if ((didProcess[0] || didProcess[1] || didProcess[2] || didProcess[3]) && level.getGameTime() % 20 == 0) {
            level.playSound(null, worldPosition, HBMSoundHandler.motor.get(), SoundSource.BLOCKS, 0.5F, 0.75F);
        }

        dataChanged();
        networkPackMK2(100);
    }

    private List<ItemStack> laneInputs(int lane) {
        ItemStack[] items = new ItemStack[12];
        int start = inputStart(lane);
        for (int i = 0; i < 12; i++) items[i] = inventory.getStackInSlot(start + i);
        return List.of(items);
    }

    @Nullable
    private AssemblerRecipe findMatchingRecipe(int lane) {
        AssemblerRecipe recipe = AssemblyMachineRecipes.byName(level, recipes[lane]);
        if (recipe == null) return null;
        AssemblerRecipe.Input input = AssemblerRecipe.Input.of(laneInputs(lane));
        if (!recipe.matches(input, level)) return null;
        if (!matchesFluids(lane, recipe)) return null;
        return recipe;
    }

    private boolean matchesFluids(int lane, AssemblerRecipe recipe) {
        for (FluidStack need : recipe.getInputFluids()) {
            if (need == null || need.type == null || need.type == Fluids.NONE) continue;
            if (inputTanks[lane].getTankType() != need.type || inputTanks[lane].getFill() < need.fill) return false;
        }
        for (FluidStack out : recipe.getOutputFluids()) {
            if (out == null || out.type == null || out.type == Fluids.NONE) continue;
            if (outputTanks[lane].getTankType() != Fluids.NONE && outputTanks[lane].getTankType() != out.type) return false;
            if (outputTanks[lane].getFill() + out.fill > outputTanks[lane].getMaxFill()) return false;
        }
        return true;
    }

    private void retargetInputTank(int lane, AssemblerRecipe recipe) {
        if (inputTanks[lane].getFill() > 0) return;
        for (FluidStack need : recipe.getInputFluids()) {
            if (need != null && need.type != null && need.type != Fluids.NONE) {
                inputTanks[lane].setTankType(need.type);
                return;
            }
        }
    }

    private void setupTanks(int lane, AssemblerRecipe recipe) {
        for (FluidStack need : recipe.getInputFluids()) {
            if (need == null || need.type == Fluids.NONE) continue;
            inputTanks[lane].changeTankSize(Math.max(Math.max(inputTanks[lane].getFill(), need.fill * 2), TANK_CAPACITY));
            return;
        }
        for (FluidStack out : recipe.getOutputFluids()) {
            if (out == null || out.type == Fluids.NONE) continue;
            outputTanks[lane].changeTankSize(Math.max(Math.max(outputTanks[lane].getFill(), out.fill * 2), TANK_CAPACITY));
            return;
        }
    }

    private boolean canFitOutput(int lane, ItemStack output) {
        ItemStack current = inventory.getStackInSlot(outputSlot(lane));
        if (current.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(current, output) && current.getCount() + output.getCount() <= current.getMaxStackSize();
    }

    private boolean canProcess(int lane, AssemblerRecipe recipe, double speed, double pow) {
        long cost = pow == 1 ? recipe.getPower() : (long) (recipe.getPower() * pow);
        if (power < cost) return false;
        if (level == null) return false;
        return canFitOutput(lane, recipe.getResultItem(level.registryAccess()));
    }

    private void process(int lane, AssemblerRecipe recipe, double speed, double pow) {
        long cost = pow == 1 ? recipe.getPower() : (long) (recipe.getPower() * pow);
        power -= cost;
        progress[lane] += Math.min(speed / Math.max(1, recipe.getDuration()), 1D);
        if (progress[lane] < 1D) return;
        consumeInputs(lane, recipe);
        ItemStack output = recipe.getResultItem(level.registryAccess()).copy();
        ItemStack current = inventory.getStackInSlot(outputSlot(lane));
        if (current.isEmpty()) {
            inventory.setStackInSlot(outputSlot(lane), output);
        } else {
            current.grow(output.getCount());
        }
        if (canProcess(lane, recipe, speed, pow) && findMatchingRecipe(lane) != null) {
            progress[lane] -= 1D;
        } else {
            progress[lane] = 0;
        }
    }

    private void consumeInputs(int lane, AssemblerRecipe recipe) {
        int start = inputStart(lane);
        for (AssemblerRecipe.Entry entry : recipe.getInputEntries()) {
            int needed = entry.count();
            for (int slot = start; slot < start + 12 && needed > 0; slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty() || !entry.ingredient().test(stack)) continue;
                int take = Math.min(needed, stack.getCount());
                stack.shrink(take);
                needed -= take;
            }
        }
        for (FluidStack need : recipe.getInputFluids()) {
            if (need == null || need.type == Fluids.NONE) continue;
            if (inputTanks[lane].getTankType() == need.type) {
                inputTanks[lane].setFill(Math.max(0, inputTanks[lane].getFill() - need.fill));
            }
        }
        for (FluidStack out : recipe.getOutputFluids()) {
            if (out == null || out.type == Fluids.NONE) continue;
            if (outputTanks[lane].getTankType() == Fluids.NONE || outputTanks[lane].getTankType() == out.type) {
                outputTanks[lane].setTankType(out.type);
                outputTanks[lane].setFill(outputTanks[lane].getFill() + out.fill);
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
        for (int i = 0; i < LANES; i++) {
            inputTanks[i].writeToNBT(tag, "i" + i);
            outputTanks[i].writeToNBT(tag, "o" + i);
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
        for (int i = 0; i < LANES; i++) {
            inputTanks[i].readFromNBT(tag, "i" + i);
            outputTanks[i].readFromNBT(tag, "o" + i);
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
        // CE :745-751
        return new String[]{
                PREFIX_VALUE + "progress1", PREFIX_VALUE + "progress2", PREFIX_VALUE + "progress3", PREFIX_VALUE + "progress4",
                PREFIX_VALUE + "recipe1", PREFIX_VALUE + "recipe2", PREFIX_VALUE + "recipe3", PREFIX_VALUE + "recipe4",
                PREFIX_VALUE + "anyactive",
                PREFIX_VALUE + "active1", PREFIX_VALUE + "active2", PREFIX_VALUE + "active3", PREFIX_VALUE + "active4"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :755-762 — module.progress / getRecipeName() → port progress[] / recipes[]
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
        return new MachineAssemblyFactoryMenu(containerId, playerInventory, this);
    }
}
