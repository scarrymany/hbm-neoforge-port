package com.hbm.blockentity.machine.chem;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.machine.chem.ElectrolyserMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.chem.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.chem.ElectrolyserFluidRecipes.ElectrolysisRecipe;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from CE's {@code TileEntityElectrolyser} - <b>fluid electrolysis half only</b>
 * ({@code docs/phase2/machines_chemical_isotope.md}'s Electrolyser section). CE glues two independent
 * recipe systems into one TE with a dual-GUI toggle; this pass ports the fluid side
 * ({@link ElectrolyserFluidRecipes}, e.g. water -&gt; hydrogen + oxygen) in full and deliberately
 * <b>does not</b> port the ore/crystal electrolysis side - that half pours accumulated molten metal
 * into the world via {@code com.hbm.util.CrucibleUtil.pourFullStack}, a foundry/casting system not
 * ported anywhere in this port yet (the research doc's own words: "flag as a real Phase 2/4 boundary
 * dependency: the electrolyser's ore side cannot function without a foundry/casting target existing
 * downstream"). {@link com.hbm.inventory.recipes.chem.ElectrolyserFluidRecipes} therefore has no
 * {@code ElectrolyserMetalRecipes} sibling in this pass either.
 * <p>
 * <b>Correction (Phase 7 mrec-12-electrolysermetal-misc pass):</b> {@code com.hbm.util.CrucibleUtil}
 * has since landed in this port (the concurrent Phase 7 Crucible-machine pass), so the foundry/casting
 * target named above now exists. That resolves one of {@code ElectrolyserMetalRecipes}'s blockers, but
 * not all of them - two of CE's 18 explicit crystal recipes still need unregistered {@code sulfur}/
 * {@code chunk_ore} items, and CE's bedrock-ore loop (18 more recipes) still needs
 * {@code ItemBedrockOreNew.toFluid}/{@code .extract}-equivalent helpers this port's bedrock-ore item
 * classes don't have. More importantly, <b>this TE still has no ore/crystal-mode branch at all</b> - no
 * item input slot, no dual-GUI toggle, no pour-out tick logic - porting that is real machine-behavior
 * work (new menu/screen state, a molten-material accumulator, a downward {@code CrucibleUtil} pour call)
 * beyond a recipe-data porting task's scope; left for a future pass. See
 * {@code src/main/java/com/hbm/inventory/recipes/chem/ElectrolyserFluidRecipes.java}'s javadoc for the
 * same note.
 * <p>
 * {@link #tankIn} is fixed to CE's own constructor default, {@link Fluids#WATER} (CE's
 * {@code IItemFluidIdentifier}-based tank retyping is not ported, same reasoning as
 * {@code GasCentrifugeBlockEntity}'s tank - a receiving tank must already carry the feed fluid's type
 * for {@code IFluidStandardReceiverMK2}'s demand check to ever report nonzero, so the water
 * electrolysis recipe works fully through pipes; the other {@link ElectrolyserFluidRecipes} entries
 * need the dropped retyping mechanic to ever see fluid arrive).
 * <p>
 * {@code getCycleCount() = min(1 + overdrive*2, 7)} and the SPEED/POWER usage formula are preserved
 * from CE exactly, applied to the one remaining sub-machine.
 */
public class ElectrolyserBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, IPersistentNBT, MenuProvider {

    private static final int BATTERY_SLOT = 0;
    private static final int UPGRADE_START = 1;
    private static final int UPGRADE_END = 2;
    private static final int BYPRODUCT_START = 3;
    private static final int BYPRODUCT_END = 5;

    public static final long MAX_POWER = 20_000_000L;
    public static final int USAGE_BASE = 10_000;

    public final FluidTankNTM tankIn = new FluidTankNTM(Fluids.WATER, 16_000).withOwner(this);
    public final FluidTankNTM tankOut1 = new FluidTankNTM(Fluids.NONE, 16_000).withOwner(this);
    public final FluidTankNTM tankOut2 = new FluidTankNTM(Fluids.NONE, 16_000).withOwner(this);

    public final UpgradeManagerNT upgradeManager;
    public long power;
    public int progress;
    public int usage;

    public ElectrolyserBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 6, true, true);

        Map<UpgradeType, Integer> maxLevels = new EnumMap<>(UpgradeType.class);
        maxLevels.put(UpgradeType.SPEED, 3);
        maxLevels.put(UpgradeType.POWER, 3);
        this.upgradeManager = new UpgradeManagerNT(maxLevels);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineElectrolyser");
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack itemStack) {
        if (i == BATTERY_SLOT) return Library.isBattery(itemStack);
        if (i >= UPGRADE_START && i <= UPGRADE_END) return itemStack.getItem() instanceof ItemMachineUpgrade;
        return false;
    }

    @Override
    public boolean canExtractItem(int i, ItemStack itemStack, int j) {
        return i >= BYPRODUCT_START && i <= BYPRODUCT_END;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{BYPRODUCT_START, BYPRODUCT_START + 1, BYPRODUCT_END};
    }

    public int getCycleCount() {
        return Math.min(1 + upgradeManager.getLevel(UpgradeType.OVERDRIVE) * 2, 7);
    }

    private ElectrolysisRecipe currentRecipe() {
        return ElectrolyserFluidRecipes.getRecipe(tankIn.getTankType());
    }

    private boolean canProcess(ElectrolysisRecipe recipe) {
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
        return true;
    }

    private void process(ElectrolysisRecipe recipe) {
        tankIn.setFill(tankIn.getFill() - recipe.amount);

        if (recipe.output1 != null && recipe.output1.type != Fluids.NONE) {
            tankOut1.setTankType(recipe.output1.type);
            tankOut1.setFill(tankOut1.getFill() + recipe.output1.fill);
        }
        if (recipe.output2 != null && recipe.output2.type != Fluids.NONE) {
            tankOut2.setTankType(recipe.output2.type);
            tankOut2.setFill(tankOut2.getFill() + recipe.output2.fill);
        }
        for (ItemStack byproduct : recipe.byproduct) {
            for (int slot = BYPRODUCT_START; slot <= BYPRODUCT_END; slot++) {
                ItemStack remaining = inventory.insertItem(slot, byproduct.copy(), false);
                if (remaining.isEmpty()) break;
            }
        }
    }

    public DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.getX() + 3, p.getY(), p.getZ(), Direction.EAST),
                new DirPos(p.getX() - 3, p.getY(), p.getZ(), Direction.WEST),
                new DirPos(p.getX(), p.getY(), p.getZ() + 1, Direction.SOUTH),
                new DirPos(p.getX(), p.getY(), p.getZ() - 1, Direction.NORTH)
        };
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        power = Library.chargeTEFromItems(inventory, BATTERY_SLOT, power, MAX_POWER);

        for (DirPos dp : getConPos()) {
            trySubscribe(level, dp);
            trySubscribe(tankIn.getTankType(), level, dp);
            if (tankOut1.getFill() > 0) tryProvide(tankOut1, level, dp);
            if (tankOut2.getFill() > 0) tryProvide(tankOut2, level, dp);
        }

        upgradeManager.checkSlots(inventory, UPGRADE_START, UPGRADE_END);
        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);
        usage = USAGE_BASE - USAGE_BASE * powerLevel / 4 + USAGE_BASE * speedLevel;

        ElectrolysisRecipe recipe = currentRecipe();
        int duration = recipe == null ? 20 : recipe.duration;

        for (int i = 0; i < getCycleCount(); i++) {
            if (power >= usage && canProcess(recipe)) {
                progress++;
                power -= usage;

                if (progress >= duration) {
                    process(recipe);
                    progress = 0;
                    setChanged();
                }
            } else {
                progress = 0;
            }
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
        return List.of(tankIn);
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(tankOut1, tankOut2);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tankIn, tankOut1, tankOut2);
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        return dir != null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("progress", progress);
        tankIn.writeToNBT(tag, "tankIn");
        tankOut1.writeToNBT(tag, "tankOut1");
        tankOut2.writeToNBT(tag, "tankOut2");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        progress = tag.getInt("progress");
        tankIn.readFromNBT(tag, "tankIn");
        tankOut1.readFromNBT(tag, "tankOut1");
        tankOut2.readFromNBT(tag, "tankOut2");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
        tankIn.serialize(buf);
        tankOut1.serialize(buf);
        tankOut2.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
        tankIn.deserialize(buf);
        tankOut1.deserialize(buf);
        tankOut2.deserialize(buf);
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        tankIn.writeToNBT(nbt, "ni");
        tankOut1.writeToNBT(nbt, "no1");
        tankOut2.writeToNBT(nbt, "no2");
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        tankIn.readFromNBT(nbt, "ni");
        tankOut1.readFromNBT(nbt, "no1");
        tankOut2.readFromNBT(nbt, "no2");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectrolyserMenu(containerId, playerInventory, this);
    }
}
