package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.machine.MachineMixerMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.MixerRecipes.MixerRecipe;
import com.hbm.items.machine.IItemFluidIdentifier;
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
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from CE's {@code TileEntityMachineMixer}.
 * {@code tanks[2].setType(2, inventory)} Exact CE {@code TileEntityMachineMixer.java:95}.
 * {@code recipeIndex} cycle Exact CE {@code :184-193}/{@code :351-353}.
 * Slots Exact CE {@code ContainerMixer.java:32-40} (5 slots: battery/solid/ID/upgrades 3-4).
 * {@code upgradePlug} on insert slots 3-4 Exact CE {@code :74-75}.
 */
public class MachineMixerBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider, IControlReceiver {

    public static final long MAX_POWER = 10_000L;
    private static final int TANK_REAGENT_CAPACITY = 16_000;
    private static final int TANK_OUTPUT_CAPACITY = 24_000;

    public static final int BATTERY_SLOT = 0;
    public static final int SOLID_INPUT = 1;
    public static final int SLOT_ID = 2;
    public static final int UPGRADE_START = 3;
    public static final int UPGRADE_END = 4;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);

    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.POWER, 3);
        VALID_UPGRADES.put(UpgradeType.OVERDRIVE, 6);
    }

    private final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(VALID_UPGRADES);
    /** Index 0/1 = reagent input tanks, index 2 = output tank - matches CE's {@code tanks[]} numbering. */
    public final List<FluidTankNTM> tanks = List.of(
            new FluidTankNTM(Fluids.NONE, TANK_REAGENT_CAPACITY).withOwner(this),
            new FluidTankNTM(Fluids.NONE, TANK_REAGENT_CAPACITY).withOwner(this),
            new FluidTankNTM(Fluids.NONE, TANK_OUTPUT_CAPACITY).withOwner(this)
    );

    private long power;
    public int progress;
    public int processTime;
    public int recipeIndex;
    private int consumption = 50;

    public MachineMixerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5, true, true);
    }

    @Override
    protected ItemStackHandler getNewInventory(int scount, int slotlimit) {
        return new ItemStackHandler(scount) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                setChanged();
            }

            @Override
            public void setStackInSlot(int slot, ItemStack stack) {
                super.setStackInSlot(slot, stack);
                // CE TileEntityMachineMixer.java:74-75
                if (!stack.isEmpty() && slot >= UPGRADE_START && slot <= UPGRADE_END
                        && stack.getItem() instanceof ItemMachineUpgrade && level != null && !level.isClientSide) {
                    level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                            HBMSoundHandler.upgradePlug.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
            }

            @Override
            public int getSlotLimit(int slot) {
                return slotlimit;
            }
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineMixer");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack.isEmpty()) return false;
        // CE :253-261 is solid-only (and hopper is {1}). MenuBase.tile is getCheckedInventory(),
        // so battery/ID/upgrade GUI insert dies without this.
        if (slot == BATTERY_SLOT) return Library.isBattery(stack);
        if (slot == SLOT_ID) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot >= UPGRADE_START && slot <= UPGRADE_END) {
            return stack.getItem() instanceof ItemMachineUpgrade;
        }
        if (slot != SOLID_INPUT) return false;
        MixerRecipe[] recipes = MixerRecipes.getOutput(tanks.get(2).getTankType());
        if (recipes == null || recipes.length <= 0) return false;
        MixerRecipe recipe = recipes[this.recipeIndex % recipes.length];
        if (recipe == null || recipe.solidInput == null) return false;
        return recipe.solidInput.matchesRecipe(stack, true);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{SOLID_INPUT};
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x, y - 1, z, Direction.DOWN),
                new DirPos(x + 1, y, z, Direction.EAST),
                new DirPos(x - 1, y, z, Direction.WEST),
                new DirPos(x, y, z + 1, Direction.SOUTH),
                new DirPos(x, y, z - 1, Direction.NORTH),
        };
    }

    public int getConsumption() {
        return consumption;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        this.power = Library.chargeTEFromItems(inventory, BATTERY_SLOT, power, getMaxPower());
        // CE TileEntityMachineMixer.java:95
        tanks.get(2).setType(SLOT_ID, inventory);

        upgradeManager.checkSlots(inventory, UPGRADE_START, UPGRADE_END);
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);
        int overLevel = upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        this.consumption = 50;
        this.consumption += speedLevel * 150;
        this.consumption -= (int) (this.consumption * powerLevel * 0.25);
        this.consumption *= (overLevel * 3 + 1);

        for (DirPos pos : getConPos()) {
            this.trySubscribe(level, pos);
            if (tanks.get(0).getTankType() != Fluids.NONE) {
                this.trySubscribe(tanks.get(0).getTankType(), level, pos);
            }
            if (tanks.get(1).getTankType() != Fluids.NONE) {
                this.trySubscribe(tanks.get(1).getTankType(), level, pos);
            }
        }

        if (this.canProcess()) {
            this.progress++;
            this.power -= this.getConsumption();

            this.processTime -= this.processTime * speedLevel / 4;
            this.processTime /= (overLevel + 1);
            if (processTime <= 0) this.processTime = 1;

            if (this.progress >= this.processTime) {
                this.process();
                this.progress = 0;
            }
        } else {
            this.progress = 0;
        }

        for (DirPos pos : getConPos()) {
            if (tanks.get(2).getFill() > 0) {
                this.tryProvide(tanks.get(2), level, pos);
            }
        }

        dataChanged();
        networkPackMK2(50);
    }

    /** Exact CE {@code TileEntityMachineMixer.canProcess} :184-219. */
    public boolean canProcess() {
        MixerRecipe[] recipes = MixerRecipes.getOutput(tanks.get(2).getTankType());
        if (recipes == null || recipes.length <= 0) {
            this.recipeIndex = 0;
            return false;
        }

        this.recipeIndex = this.recipeIndex % recipes.length;
        MixerRecipe recipe = recipes[this.recipeIndex];
        if (recipe == null) {
            this.recipeIndex = 0;
            return false;
        }

        tanks.get(0).setTankType(recipe.input1 != null ? recipe.input1.type : Fluids.NONE);
        tanks.get(1).setTankType(recipe.input2 != null ? recipe.input2.type : Fluids.NONE);

        if (recipe.input1 != null && tanks.get(0).getFill() < recipe.input1.fill) return false;
        if (recipe.input2 != null && tanks.get(1).getFill() < recipe.input2.fill) return false;
        if (this.power < getConsumption()) return false;
        if (recipe.output + tanks.get(2).getFill() > tanks.get(2).getMaxFill()) return false;

        if (recipe.solidInput != null) {
            if (inventory.getStackInSlot(SOLID_INPUT).isEmpty()) return false;
            if (!recipe.solidInput.matchesRecipe(inventory.getStackInSlot(SOLID_INPUT), true)
                    || recipe.solidInput.getStack().getCount() > inventory.getStackInSlot(SOLID_INPUT).getCount()) {
                return false;
            }
        }

        this.processTime = recipe.processTime;
        return true;
    }

    /** Exact CE {@code TileEntityMachineMixer.process} :222-232. */
    protected void process() {
        MixerRecipe[] recipes = MixerRecipes.getOutput(tanks.get(2).getTankType());
        MixerRecipe recipe = recipes[this.recipeIndex % recipes.length];

        if (recipe.input1 != null) tanks.get(0).setFill(tanks.get(0).getFill() - recipe.input1.fill);
        if (recipe.input2 != null) tanks.get(1).setFill(tanks.get(1).getFill() - recipe.input2.fill);
        if (recipe.solidInput != null) {
            inventory.extractItem(SOLID_INPUT, recipe.solidInput.getStack().getCount(), false);
        }
        tanks.get(2).setFill(tanks.get(2).getFill() + recipe.output);
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
        return MAX_POWER;
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(tanks.get(2));
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tanks.get(0), tanks.get(1));
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return tanks;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("progress", progress);
        tag.putInt("processTime", processTime);
        tag.putInt("recipe", recipeIndex);
        for (int i = 0; i < tanks.size(); i++) tanks.get(i).writeToNBT(tag, "tank" + i);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        progress = tag.getInt("progress");
        processTime = tag.getInt("processTime");
        recipeIndex = tag.getInt("recipe");
        for (int i = 0; i < tanks.size(); i++) tanks.get(i).readFromNBT(tag, "tank" + i);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
        buf.writeInt(processTime);
        buf.writeInt(recipeIndex);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
        processTime = buf.readInt();
        recipeIndex = buf.readInt();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineMixerMenu(containerId, playerInventory, this);
    }

    @Override
    public boolean hasPermission(Player player) {
        // CE TileEntityMachineMixer.java:347
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 256.0;
    }

    /** Exact CE {@code TileEntityMachineMixer.receiveControl} :351-353. */
    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) {
            this.recipeIndex++;
            setChanged();
        }
    }
}
