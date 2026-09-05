package com.hbm.blockentity.machine.chem;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.machine.chem.ElectrolyserMenu;
import com.hbm.inventory.container.machine.chem.ElectrolyserMetalMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes;
import com.hbm.inventory.recipes.ElectrolyserMetalRecipes.ElectrolysisMetalRecipe;
import com.hbm.inventory.recipes.chem.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.chem.ElectrolyserFluidRecipes.ElectrolysisRecipe;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.items.tool.ItemFluidContainerInfinite;
import com.hbm.capability.NTMFluidCapabilityHandler;
import com.hbm.lib.DirPos;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import com.hbm.util.CrucibleUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * CE {@code TileEntityElectrolyser}: fluid + metal halves, 21 slots, nitric-acid metal cycle,
 * {@link CrucibleUtil#pourFullStack} left/right pour, fluid-id/canister I/O
 * ({@code TileEntityElectrolyser.java:141-144}).
 * TODO(CE: TileEntityMachineSuperComputer.java:186-194): dropdown / ModuleMachineBase — not this machine.
 */
public class ElectrolyserBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, IPersistentNBT,
        MenuProvider, IControlReceiver {

    private static final int BATTERY_SLOT = 0;
    private static final int UPGRADE_START = 1;
    private static final int UPGRADE_END = 2;
    private static final int FLUID_BYPRODUCT_START = 11;
    private static final int CRYSTAL_SLOT = 14;
    private static final int METAL_BYPRODUCT_START = 15;

    public static final long MAX_POWER = 20_000_000L;
    public static final int USAGE_ORE_BASE = 10_000;
    public static final int USAGE_FLUID_BASE = 10_000;

    public final FluidTankNTM tankIn = new FluidTankNTM(Fluids.WATER, 16_000).withOwner(this);
    public final FluidTankNTM tankOut1 = new FluidTankNTM(Fluids.NONE, 16_000).withOwner(this);
    public final FluidTankNTM tankOut2 = new FluidTankNTM(Fluids.NONE, 16_000).withOwner(this);
    public final FluidTankNTM tankAcid = new FluidTankNTM(Fluids.NITRIC_ACID, 16_000).withOwner(this);

    public final UpgradeManagerNT upgradeManager;
    public long power;
    public int progressFluid;
    public int progressOre;
    public int processFluidTime = 100;
    public int processOreTime = 600;
    public int usageFluid;
    public int usageOre;
    /** Alias of {@link #usageFluid} for older fluid-screen callers. */
    public int usage;
    public Mats.MaterialStack leftStack;
    public Mats.MaterialStack rightStack;
    public final int maxMaterial = MaterialShapes.BLOCK.q(16);
    public int lastSelectedGUI;

    public ElectrolyserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 21, true, true);

        Map<UpgradeType, Integer> maxLevels = new EnumMap<>(UpgradeType.class);
        maxLevels.put(UpgradeType.SPEED, 3);
        maxLevels.put(UpgradeType.POWER, 3);
        maxLevels.put(UpgradeType.OVERDRIVE, 3);
        this.upgradeManager = new UpgradeManagerNT(maxLevels);
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
                // CE TileEntityElectrolyser.java:102-103
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
        return Component.translatable("container.machineElectrolyser");
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemStack) {
        if (i == BATTERY_SLOT) return Library.isBattery(itemStack);
        if (i >= UPGRADE_START && i <= UPGRADE_END) return itemStack.getItem() instanceof ItemMachineUpgrade;
        if (i == 3) return itemStack.getItem() instanceof IItemFluidIdentifier;
        if (i == 5 || i == 7 || i == 9) return isFluidCanister(itemStack);
        if (i == CRYSTAL_SLOT) return ElectrolyserMetalRecipes.getRecipe(itemStack) != null;
        return false;
    }

    /** CE {@code SlotFiltered.fluidHandlerSlot} + {@code IFillableItem} + infinite barrel. */
    public static boolean isFluidCanister(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof IFillableItem) return true;
        if (stack.getItem() instanceof ItemFluidContainerInfinite) return true;
        return NTMFluidCapabilityHandler.isNtmFluidContainer(stack.getItem())
                || NTMFluidCapabilityHandler.isEmptyNtmFluidContainer(stack.getItem());
    }

    @Override
    public boolean canExtractItem(int i, ItemStack itemStack, int j) {
        return i != CRYSTAL_SLOT;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
    }

    public int getCycleCount() {
        return Math.min(1 + upgradeManager.getLevel(UpgradeType.OVERDRIVE) * 2, 7);
    }

    public Direction coreFacing() {
        int meta = getBlockState().getValue(BlockDummyable.META);
        return Direction.from3DDataValue(meta - BlockDummyable.offset);
    }

    private ElectrolysisRecipe currentFluidRecipe() {
        return ElectrolyserFluidRecipes.getRecipe(tankIn.getTankType());
    }

    public boolean canProcessFluid() {
        if (power < usageFluid) return false;
        ElectrolysisRecipe recipe = currentFluidRecipe();
        if (recipe == null) return false;
        if (tankIn.getFill() < recipe.amount) return false;

        if (recipe.output1 != null && recipe.output1.type != Fluids.NONE) {
            if (tankOut1.getTankType() != Fluids.NONE && tankOut1.getTankType() != recipe.output1.type) return false;
            if (tankOut1.getFill() + recipe.output1.fill > tankOut1.getMaxFill()) return false;
        }
        if (recipe.output2 != null && recipe.output2.type != Fluids.NONE) {
            if (tankOut2.getTankType() != Fluids.NONE && tankOut2.getTankType() != recipe.output2.type) return false;
            if (tankOut2.getFill() + recipe.output2.fill > tankOut2.getMaxFill()) return false;
        }
        if (recipe.byproduct != null) {
            for (int i = 0; i < recipe.byproduct.length; i++) {
                ItemStack slot = inventory.getStackInSlot(FLUID_BYPRODUCT_START + i);
                ItemStack byproduct = recipe.byproduct[i];
                if (slot.isEmpty()) continue;
                if (!ItemStack.isSameItem(slot, byproduct)) return false;
                if (slot.getCount() + byproduct.getCount() > slot.getMaxStackSize()) return false;
            }
        }
        return true;
    }

    public void processFluids() {
        ElectrolysisRecipe recipe = currentFluidRecipe();
        tankIn.setFill(tankIn.getFill() - recipe.amount);
        if (recipe.output1 != null && recipe.output1.type != Fluids.NONE) {
            tankOut1.setTankType(recipe.output1.type);
            tankOut1.setFill(tankOut1.getFill() + recipe.output1.fill);
        }
        if (recipe.output2 != null && recipe.output2.type != Fluids.NONE) {
            tankOut2.setTankType(recipe.output2.type);
            tankOut2.setFill(tankOut2.getFill() + recipe.output2.fill);
        }
        if (recipe.byproduct != null) {
            for (int i = 0; i < recipe.byproduct.length; i++) {
                ItemStack slot = inventory.getStackInSlot(FLUID_BYPRODUCT_START + i);
                ItemStack byproduct = recipe.byproduct[i];
                if (slot.isEmpty()) {
                    inventory.setStackInSlot(FLUID_BYPRODUCT_START + i, byproduct.copy());
                } else {
                    slot.grow(byproduct.getCount());
                }
            }
        }
    }

    public boolean canProcessMetal() {
        if (inventory.getStackInSlot(CRYSTAL_SLOT).isEmpty()) return false;
        if (power < usageOre) return false;
        if (tankAcid.getFill() < 100) return false;

        ElectrolysisMetalRecipe recipe = ElectrolyserMetalRecipes.getRecipe(inventory.getStackInSlot(CRYSTAL_SLOT));
        if (recipe == null) return false;

        if (leftStack != null && recipe.output1 != null) {
            if (recipe.output1.material != leftStack.material) return false;
            if (recipe.output1.amount + leftStack.amount > maxMaterial) return false;
        }
        if (rightStack != null && recipe.output2 != null) {
            if (recipe.output2.material != rightStack.material) return false;
            if (recipe.output2.amount + rightStack.amount > maxMaterial) return false;
        }
        if (recipe.byproduct != null) {
            for (int i = 0; i < recipe.byproduct.length; i++) {
                ItemStack slot = inventory.getStackInSlot(METAL_BYPRODUCT_START + i);
                ItemStack byproduct = recipe.byproduct[i];
                if (slot.isEmpty()) continue;
                if (!ItemStack.isSameItem(slot, byproduct)) return false;
                if (slot.getCount() + byproduct.getCount() > slot.getMaxStackSize()) return false;
            }
        }
        return true;
    }

    public void processMetal() {
        ElectrolysisMetalRecipe recipe = ElectrolyserMetalRecipes.getRecipe(inventory.getStackInSlot(CRYSTAL_SLOT));
        if (recipe.output1 != null) {
            if (leftStack == null) {
                leftStack = new Mats.MaterialStack(recipe.output1.material, recipe.output1.amount);
            } else {
                leftStack.amount += recipe.output1.amount;
            }
        }
        if (recipe.output2 != null) {
            if (rightStack == null) {
                rightStack = new Mats.MaterialStack(recipe.output2.material, recipe.output2.amount);
            } else {
                rightStack.amount += recipe.output2.amount;
            }
        }
        if (recipe.byproduct != null) {
            for (int i = 0; i < recipe.byproduct.length; i++) {
                ItemStack slot = inventory.getStackInSlot(METAL_BYPRODUCT_START + i);
                ItemStack byproduct = recipe.byproduct[i];
                if (slot.isEmpty()) {
                    inventory.setStackInSlot(METAL_BYPRODUCT_START + i, byproduct.copy());
                } else {
                    slot.grow(byproduct.getCount());
                }
            }
        }
        tankAcid.setFill(tankAcid.getFill() - 100);
        ItemStack in = inventory.getStackInSlot(CRYSTAL_SLOT);
        in.shrink(1);
        if (in.isEmpty()) inventory.setStackInSlot(CRYSTAL_SLOT, ItemStack.EMPTY);
    }

    public int getDurationMetal() {
        ElectrolysisMetalRecipe result = ElectrolyserMetalRecipes.getRecipe(inventory.getStackInSlot(CRYSTAL_SLOT));
        int base = result != null ? result.duration : 600;
        int speed = upgradeManager.getLevel(UpgradeType.SPEED) - Math.min(upgradeManager.getLevel(UpgradeType.POWER), 1);
        return (int) Math.ceil(base * Math.max(1F - 0.25F * speed, 0.2F));
    }

    public int getDurationFluid() {
        ElectrolysisRecipe result = currentFluidRecipe();
        int base = result != null ? result.duration : 100;
        int speed = upgradeManager.getLevel(UpgradeType.SPEED) - Math.min(upgradeManager.getLevel(UpgradeType.POWER), 1);
        return (int) Math.ceil(base * Math.max(1F - 0.25F * speed, 0.2F));
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition;
        int dx = dir.getStepX();
        int dz = dir.getStepZ();
        int rx = rot.getStepX();
        int rz = rot.getStepZ();
        return new DirPos[]{
                new DirPos(p.getX() - dx * 6, p.getY(), p.getZ() - dz * 6, dir.getOpposite()),
                new DirPos(p.getX() - dx * 6 + rx, p.getY(), p.getZ() - dz * 6 + rz, dir.getOpposite()),
                new DirPos(p.getX() - dx * 6 - rx, p.getY(), p.getZ() - dz * 6 - rz, dir.getOpposite()),
                new DirPos(p.getX() + dx * 6, p.getY(), p.getZ() + dz * 6, dir),
                new DirPos(p.getX() + dx * 6 + rx, p.getY(), p.getZ() + dz * 6 + rz, dir),
                new DirPos(p.getX() + dx * 6 - rx, p.getY(), p.getZ() + dz * 6 - rz, dir)
        };
    }

    private void pourSide(Mats.MaterialStack stack, Direction dir, int speedLevel) {
        List<Mats.MaterialStack> toCast = new ArrayList<>();
        toCast.add(stack);
        int quanta = MaterialShapes.NUGGET.q(3) * Math.max(getCycleCount() * speedLevel, 1);
        CrucibleUtil.pourFullStack(level,
                worldPosition.getX() + 0.5 + dir.getStepX() * 5.875,
                worldPosition.getY() + 2.0,
                worldPosition.getZ() + 0.5 + dir.getStepZ() * 5.875,
                6, true, toCast, quanta);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        power = Library.chargeTEFromItems(inventory, BATTERY_SLOT, power, MAX_POWER);
        tankIn.setType(3, 4, inventory);
        tankIn.loadTank(5, 6, inventory);
        tankOut1.unloadTank(7, 8, inventory);
        tankOut2.unloadTank(9, 10, inventory);

        if (level.getGameTime() % 20 == 0) {
            for (DirPos dp : getConPos()) {
                trySubscribe(level, dp);
                trySubscribe(tankIn.getTankType(), level, dp);
                trySubscribe(tankAcid.getTankType(), level, dp);
                if (tankOut1.getFill() > 0) tryProvide(tankOut1, level, dp);
                if (tankOut2.getFill() > 0) tryProvide(tankOut2, level, dp);
            }
        }

        upgradeManager.checkSlots(inventory, UPGRADE_START, UPGRADE_END);
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);
        usageOre = USAGE_ORE_BASE - USAGE_ORE_BASE * powerLevel / 4 + USAGE_ORE_BASE * speedLevel;
        usageFluid = USAGE_FLUID_BASE - USAGE_FLUID_BASE * powerLevel / 4 + USAGE_FLUID_BASE * speedLevel;
        usage = usageFluid;
        processFluidTime = getDurationFluid();
        processOreTime = getDurationMetal();

        for (int i = 0; i < getCycleCount(); i++) {
            if (canProcessFluid()) {
                progressFluid++;
                power -= usageFluid;
                if (progressFluid >= processFluidTime) {
                    processFluids();
                    progressFluid = 0;
                    setChanged();
                }
            }
            if (canProcessMetal()) {
                progressOre++;
                power -= usageOre;
                if (progressOre >= processOreTime) {
                    processMetal();
                    progressOre = 0;
                    setChanged();
                }
            }
        }

        if (leftStack != null) {
            pourSide(leftStack, coreFacing().getOpposite(), speedLevel);
            if (leftStack.amount <= 0) leftStack = null;
        }
        if (rightStack != null) {
            pourSide(rightStack, coreFacing(), speedLevel);
            if (rightStack.amount <= 0) rightStack = null;
        }

        dataChanged();
        networkPackMK2(50);
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
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tankIn, tankAcid);
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(tankOut1, tankOut2);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tankIn, tankOut1, tankOut2, tankAcid);
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        return dir != null;
    }

    @Override
    public boolean hasPermission(Player player) {
        return isUseableByPlayer(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("sgm")) lastSelectedGUI = 1;
        if (data.contains("sgf")) lastSelectedGUI = 0;
        setChanged();
    }

    @Override
    public void receiveControl(ServerPlayer player, CompoundTag data) {
        if (!hasPermission(player)) return;
        receiveControl(data);
        player.openMenu(new SimpleMenuProvider(this, getDisplayName()), worldPosition);
    }

    private static void writeStack(CompoundTag tag, String typeKey, String amountKey, Mats.MaterialStack stack) {
        if (stack == null || stack.material == null) return;
        tag.putInt(typeKey, stack.material.id);
        tag.putInt(amountKey, stack.amount);
    }

    private static Mats.MaterialStack readStack(CompoundTag tag, String typeKey, String amountKey) {
        if (!tag.contains(typeKey)) return null;
        NTMMaterial mat = Mats.matById.get(tag.getInt(typeKey));
        return mat == null ? null : new Mats.MaterialStack(mat, tag.getInt(amountKey));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("progressFluid", progressFluid);
        tag.putInt("progressOre", progressOre);
        tag.putInt("lastSelectedGUI", lastSelectedGUI);
        writeStack(tag, "leftType", "leftAmount", leftStack);
        writeStack(tag, "rightType", "rightAmount", rightStack);
        tankIn.writeToNBT(tag, "tankIn");
        tankOut1.writeToNBT(tag, "tankOut1");
        tankOut2.writeToNBT(tag, "tankOut2");
        tankAcid.writeToNBT(tag, "tankAcid");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        progressFluid = tag.contains("progressFluid") ? tag.getInt("progressFluid") : tag.getInt("progress");
        progressOre = tag.getInt("progressOre");
        lastSelectedGUI = tag.getInt("lastSelectedGUI");
        leftStack = readStack(tag, "leftType", "leftAmount");
        rightStack = readStack(tag, "rightType", "rightAmount");
        tankIn.readFromNBT(tag, "tankIn");
        tankOut1.readFromNBT(tag, "tankOut1");
        tankOut2.readFromNBT(tag, "tankOut2");
        tankAcid.readFromNBT(tag, "tankAcid");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progressFluid);
        buf.writeInt(progressOre);
        buf.writeInt(usageOre);
        buf.writeInt(usageFluid);
        buf.writeInt(getDurationFluid());
        buf.writeInt(getDurationMetal());
        tankIn.serialize(buf);
        tankOut1.serialize(buf);
        tankOut2.serialize(buf);
        tankAcid.serialize(buf);
        buf.writeBoolean(leftStack != null);
        buf.writeBoolean(rightStack != null);
        if (leftStack != null) {
            buf.writeInt(leftStack.material.id);
            buf.writeInt(leftStack.amount);
        }
        if (rightStack != null) {
            buf.writeInt(rightStack.material.id);
            buf.writeInt(rightStack.amount);
        }
        buf.writeInt(lastSelectedGUI);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progressFluid = buf.readInt();
        progressOre = buf.readInt();
        usageOre = buf.readInt();
        usageFluid = buf.readInt();
        usage = usageFluid;
        processFluidTime = buf.readInt();
        processOreTime = buf.readInt();
        tankIn.deserialize(buf);
        tankOut1.deserialize(buf);
        tankOut2.deserialize(buf);
        tankAcid.deserialize(buf);
        boolean left = buf.readBoolean();
        boolean right = buf.readBoolean();
        if (left) {
            NTMMaterial mat = Mats.matById.get(buf.readInt());
            int amount = buf.readInt();
            leftStack = mat == null ? null : new Mats.MaterialStack(mat, amount);
        } else {
            leftStack = null;
        }
        if (right) {
            NTMMaterial mat = Mats.matById.get(buf.readInt());
            int amount = buf.readInt();
            rightStack = mat == null ? null : new Mats.MaterialStack(mat, amount);
        } else {
            rightStack = null;
        }
        lastSelectedGUI = buf.readInt();
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        tankIn.writeToNBT(nbt, "ni");
        tankOut1.writeToNBT(nbt, "no1");
        tankOut2.writeToNBT(nbt, "no2");
        tankAcid.writeToNBT(nbt, "na");
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        tankIn.readFromNBT(nbt, "ni");
        tankOut1.readFromNBT(nbt, "no1");
        tankOut2.readFromNBT(nbt, "no2");
        tankAcid.readFromNBT(nbt, "na");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (lastSelectedGUI == 1) {
            return new ElectrolyserMetalMenu(containerId, playerInventory, this);
        }
        return new ElectrolyserMenu(containerId, playerInventory, this);
    }
}
