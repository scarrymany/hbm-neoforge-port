package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.inventory.container.machine.dummyable.WoodBurnerMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.items.machine.IItemFluidIdentifier;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityMachineWoodBurner.java}:72-136 — vanilla burn-time + optional
 * {@code FT_Flammable} tank. Ash ({@code powder_ash}) skipped (unregistered).
 * {@code setType(2)} / {@code loadTank(3,4)} Exact CE {@code :79-80}.
 * Solid {@code incrementPollution(SOOT, SOOT_PER_SECOND)} every 20t Exact CE {@code :117}.
 * Liquid {@code SOOT_PER_SECOND * toBurn / 2F} every 20t Exact CE {@code :132}.
 * Smoke particles stay skipped (VFX).
 */
public class MachineWoodBurnerBlockEntity extends MachineBaseBlockEntity
        implements IEnergyProviderMK2, IFluidStandardReceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 100_000;

    public final FluidTankNTM tank;
    public long power;
    public int burnTime;
    public int maxBurnTime;
    public boolean liquidBurn;
    public boolean isOn;
    public int powerGen;

    public MachineWoodBurnerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 6, true, true);
        this.tank = new FluidTankNTM(Fluids.WOODOIL, 16_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineWoodBurner");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return getBurnTime(stack) > 0;
        if (slot == 2) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot == 5) return Library.isBattery(stack);
        return slot == 3;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 1 || slot == 4 || slot == 5;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{0, 1, 5};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        powerGen = 0;

        // CE TileEntityMachineWoodBurner.java:79-80
        this.tank.setType(2, inventory);
        this.tank.loadTank(3, 4, inventory);

        power = Library.chargeItemsFromTE(inventory, 5, power, MAX_POWER);

        for (DirPos pos : getConPos()) {
            if (power > 0) tryProvide(level, pos.getPos(), pos.getDir());
            if (level.getGameTime() % 20 == 0) trySubscribe(tank.getTankType(), level, pos);
        }

        if (!liquidBurn) {
            if (burnTime <= 0) {
                ItemStack fuel = inventory.getStackInSlot(0);
                int burn = getBurnTime(fuel);
                if (burn > 0) {
                    maxBurnTime = burnTime = burn;
                    inventory.extractItem(0, 1, false);
                    setChanged();
                }
            } else if (power < MAX_POWER && isOn) {
                burnTime--;
                powerGen += 100;
                // CE TileEntityMachineWoodBurner.java:117
                if (level.getGameTime() % 20 == 0) {
                    PollutionHandler.incrementPollution(level, worldPosition, PollutionHandler.PollutionType.SOOT,
                            PollutionHandler.SOOT_PER_SECOND);
                }
            }
        } else if (power < MAX_POWER && tank.getFill() > 0 && isOn) {
            FT_Flammable trait = tank.getTankType().getTrait(FT_Flammable.class);
            if (trait != null) {
                int toBurn = Math.min(tank.getFill(), 2);
                if (toBurn > 0) {
                    powerGen += (int) (trait.getHeatEnergy() * toBurn / 2_000L);
                    tank.setFill(tank.getFill() - toBurn);
                    // CE TileEntityMachineWoodBurner.java:132
                    if (level.getGameTime() % 20 == 0) {
                        PollutionHandler.incrementPollution(level, worldPosition, PollutionHandler.PollutionType.SOOT,
                                PollutionHandler.SOOT_PER_SECOND * toBurn / 2F);
                    }
                }
            }
        }

        power = Math.min(MAX_POWER, power + powerGen);
        dataChanged();
        networkPackMK2(25);
    }

    public static int getBurnTime(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        return stack.getBurnTime(RecipeType.SMELTING);
    }

    public void toggleOn() {
        isOn = !isOn;
        setChanged();
    }

    public void toggleLiquid() {
        liquidBurn = !liquidBurn;
        setChanged();
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();
        return new DirPos[]{
                new DirPos(worldPosition.relative(dir.getOpposite(), 2), rot.getOpposite()),
                new DirPos(worldPosition.relative(dir.getOpposite(), 2).relative(rot), dir.getOpposite()),
        };
    }

    private Direction coreFacing() {
        int meta = getBlockState().getValue(BlockDummyable.META);
        return meta >= 12 ? Direction.from3DDataValue(meta - BlockDummyable.offset) : Direction.NORTH;
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
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(tank);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("burn", burnTime);
        tag.putInt("maxBurn", maxBurnTime);
        tag.putBoolean("on", isOn);
        tag.putBoolean("liq", liquidBurn);
        tank.writeToNBT(tag, "t");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        burnTime = tag.getInt("burn");
        maxBurnTime = tag.getInt("maxBurn");
        isOn = tag.getBoolean("on");
        liquidBurn = tag.getBoolean("liq");
        tank.readFromNBT(tag, "t");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(burnTime);
        buf.writeInt(maxBurnTime);
        buf.writeInt(powerGen);
        buf.writeBoolean(isOn);
        buf.writeBoolean(liquidBurn);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        burnTime = buf.readInt();
        maxBurnTime = buf.readInt();
        powerGen = buf.readInt();
        isOn = buf.readBoolean();
        liquidBurn = buf.readBoolean();
        tank.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new WoodBurnerMenu(id, inv, this);
    }
}
