package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.container.machine.dummyable.RotaryFurnaceMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes;
import com.hbm.inventory.recipes.RotaryFurnaceRecipes.RotaryFurnaceRecipe;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.util.CrucibleUtil;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code TileEntityMachineRotaryFurnace}: 3-in + fluid-id + fuel, steam, crucible pour.
 */
public class MachineRotaryFurnaceBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final int MAX_OUTPUT = MaterialShapes.BLOCK.q(16);

    public final FluidTankNTM process;
    public final FluidTankNTM steam;
    public final FluidTankNTM spent;
    public boolean isProgressing;
    public float progress;
    public int burnTime;
    public int maxBurnTime;
    public int steamUsed;
    public Mats.MaterialStack output;

    public MachineRotaryFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5, true, false);
        this.process = new FluidTankNTM(Fluids.NONE, 16_000).withOwner(this);
        this.steam = new FluidTankNTM(Fluids.STEAM, 12_000).withOwner(this);
        this.spent = new FluidTankNTM(Fluids.SPENTSTEAM, 120).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineRotaryFurnace");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 3) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot == 4) return stack.getBurnTime(RecipeType.SMELTING) > 0;
        return slot < 3;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 2, 4};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        ItemStack id = inventory.getStackInSlot(3);
        if (!id.isEmpty() && id.getItem() instanceof IItemFluidIdentifier ident) {
            process.setTankType(ident.getType(level, worldPosition, id));
        }

        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();
        if (level.getGameTime() % 20 == 0) {
            trySubscribe(steam.getTankType(), level, worldPosition.relative(dir.getOpposite()), dir.getOpposite());
            trySubscribe(steam.getTankType(), level, worldPosition.relative(dir.getOpposite()).relative(rot.getOpposite()), dir.getOpposite());
            if (process.getTankType() != Fluids.NONE) {
                trySubscribe(process.getTankType(), level, worldPosition.relative(dir).relative(rot, 2), rot);
            }
            if (spent.getFill() > 0) {
                tryProvide(spent, level, worldPosition.relative(dir.getOpposite()), dir.getOpposite());
            }
        }

        if (output != null) {
            List<Mats.MaterialStack> buf = new ArrayList<>();
            buf.add(output);
            double px = worldPosition.getX() + 0.5 + rot.getStepX() * 2.875;
            double pz = worldPosition.getZ() + 0.5 + rot.getStepZ() * 2.875;
            CrucibleUtil.pourFullStack(level, px, worldPosition.getY() + 1.25, pz, 6, true, buf, MaterialShapes.INGOT.q(1));
            output = buf.isEmpty() || buf.get(0).amount <= 0 ? null : buf.get(0);
        }

        RotaryFurnaceRecipe recipe = RotaryFurnaceRecipes.getRecipe(
                inventory.getStackInSlot(0), inventory.getStackInSlot(1), inventory.getStackInSlot(2));
        isProgressing = false;

        if (recipe != null) {
            if (burnTime <= 0 && !inventory.getStackInSlot(4).isEmpty()) {
                int bt = inventory.getStackInSlot(4).getBurnTime(RecipeType.SMELTING);
                if (bt > 0) {
                    maxBurnTime = burnTime = bt / 2;
                    inventory.extractItem(4, 1, false);
                    setChanged();
                }
            }
            if (canProcess(recipe)) {
                progress += 1F / recipe.duration;
                steam.setFill(steam.getFill() - recipe.steam);
                steamUsed += recipe.steam;
                isProgressing = true;
                if (progress >= 1F) {
                    progress -= 1F;
                    consume(recipe);
                    if (output == null) output = recipe.output.copy();
                    else output.amount += recipe.output.amount;
                    setChanged();
                }
                if (burnTime > 0) burnTime--;
            } else {
                progress = 0;
            }
            if (steamUsed >= 100) {
                int ret = Math.min(steamUsed / 100, spent.getMaxFill() - spent.getFill());
                steamUsed -= ret * 100;
                spent.setFill(spent.getFill() + ret);
            }
        } else {
            progress = 0;
        }

        dataChanged();
        networkPackMK2(50);
    }

    private boolean canProcess(RotaryFurnaceRecipe recipe) {
        if (burnTime <= 0) return false;
        if (steam.getFill() < recipe.steam) return false;
        if (recipe.fluid != null) {
            if (process.getTankType() != recipe.fluid.type || process.getFill() < recipe.fluid.fill) return false;
        }
        if (output != null && output.amount + recipe.output.amount > MAX_OUTPUT) return false;
        return true;
    }

    private void consume(RotaryFurnaceRecipe recipe) {
        for (AStack need : recipe.ingredients) {
            for (int i = 0; i < 3; i++) {
                ItemStack slot = inventory.getStackInSlot(i);
                if (!slot.isEmpty() && need.matchesRecipe(slot, true) && slot.getCount() >= need.stacksize) {
                    inventory.extractItem(i, need.stacksize, false);
                    break;
                }
            }
        }
        if (recipe.fluid != null) process.setFill(process.getFill() - recipe.fluid.fill);
    }

    private Direction coreFacing() {
        int meta = getBlockState().getValue(BlockDummyable.META);
        return meta >= 12 ? Direction.from3DDataValue(meta - BlockDummyable.offset) : Direction.NORTH;
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(process, steam);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(spent);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(process, steam, spent);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        process.writeToNBT(tag, "p");
        steam.writeToNBT(tag, "s");
        spent.writeToNBT(tag, "w");
        tag.putFloat("prog", progress);
        tag.putInt("burn", burnTime);
        tag.putInt("maxBurn", maxBurnTime);
        tag.putInt("steamUsed", steamUsed);
        if (output != null) {
            tag.putInt("omat", output.material.id);
            tag.putInt("oamt", output.amount);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        process.readFromNBT(tag, "p");
        steam.readFromNBT(tag, "s");
        spent.readFromNBT(tag, "w");
        progress = tag.getFloat("prog");
        burnTime = tag.getInt("burn");
        maxBurnTime = tag.getInt("maxBurn");
        steamUsed = tag.getInt("steamUsed");
        if (tag.contains("omat")) {
            NTMMaterial mat = Mats.matById.get(tag.getInt("omat"));
            if (mat != null) output = new Mats.MaterialStack(mat, tag.getInt("oamt"));
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isProgressing);
        buf.writeFloat(progress);
        buf.writeInt(burnTime);
        buf.writeInt(maxBurnTime);
        process.serialize(buf);
        steam.serialize(buf);
        spent.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isProgressing = buf.readBoolean();
        progress = buf.readFloat();
        burnTime = buf.readInt();
        maxBurnTime = buf.readInt();
        process.deserialize(buf);
        steam.deserialize(buf);
        spent.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new RotaryFurnaceMenu(id, inv, this);
    }
}
