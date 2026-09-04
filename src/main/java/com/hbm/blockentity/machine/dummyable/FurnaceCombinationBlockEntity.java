package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.capability.NTMFluidCapabilityHandler;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.inventory.container.machine.dummyable.FurnaceCombinationMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CombinationRecipes;
import com.hbm.inventory.recipes.CombinationRecipes.CombinationRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityFurnaceCombination}: heat-driven, processTime 20_000, maxHeat 100_000.
 * {@code unloadTank(2,3)} Exact CE {@code TileEntityFurnaceCombination.java:93}.
 * {@code pollute(SOOT, SOOT_PER_SECOND*3)} every 20t while burning Exact CE {@code :129}.
 * Smoke overflow {@code incrementPollution} Exact CE {@code TileEntityMachinePolluting:39-48}.
 * Audio / particles stay skipped.
 */
public class FurnaceCombinationBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardSenderMK2, ITickableBE, MenuProvider {

    public static final int PROCESS_TIME = 20_000;
    public static final int MAX_HEAT = 100_000;
    public static final double DIFFUSION = 0.25D;

    public static final int SLOT_IN = 0;
    public static final int SLOT_OUT = 1;

    public final FluidTankNTM tank;
    /** CE {@code TileEntityMachinePolluting} buffer 50 from {@code super(4, 50)}. */
    public final FluidTankNTM smoke;
    public final FluidTankNTM smokeLeaded;
    public final FluidTankNTM smokePoison;
    public boolean wasOn;
    public int progress;
    public int heat;

    public FurnaceCombinationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 4, true, false);
        this.tank = new FluidTankNTM(Fluids.NONE, 24_000).withOwner(this);
        this.smoke = new FluidTankNTM(Fluids.SMOKE, 50).withOwner(this);
        this.smokeLeaded = new FluidTankNTM(Fluids.SMOKE_LEADED, 50).withOwner(this);
        this.smokePoison = new FluidTankNTM(Fluids.SMOKE_POISON, 50).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.furnaceCombination");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_IN) return CombinationRecipes.getOutput(stack) != null;
        // CE :213 returns false for slot 2; without this the empty canister never lands and unloadTank is dead.
        if (slot == 2) {
            return NTMFluidCapabilityHandler.isEmptyNtmFluidContainer(stack.getItem())
                    || stack.getItem() instanceof IFillableItem;
        }
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == SLOT_OUT;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{SLOT_IN, SLOT_OUT};
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        tryPullHeat();
        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                Direction rot = dir.getClockWise();
                for (int y = 0; y <= 1; y++) {
                    for (int j = -1; j <= 1; j++) {
                        BlockPos p = worldPosition.relative(dir, 2).relative(rot, j).above(y);
                        if (tank.getFill() > 0) tryProvide(tank, level, p, dir);
                        // CE TileEntityFurnaceCombination.java:78
                        sendSmoke(p, dir);
                    }
                }
            }
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos up = worldPosition.offset(x, 2, z);
                    if (tank.getFill() > 0) tryProvide(tank, level, up, Direction.UP);
                    // CE TileEntityFurnaceCombination.java:86
                    sendSmoke(up, Direction.UP);
                }
            }
        }

        wasOn = false;
        // CE TileEntityFurnaceCombination.java:93
        tank.unloadTank(2, 3, inventory);
        if (canSmelt()) {
            int burn = heat / 100;
            if (burn > 0) {
                wasOn = true;
                progress += burn;
                heat -= burn;
                if (progress >= PROCESS_TIME) {
                    progress -= PROCESS_TIME;
                    CombinationRecipe rec = CombinationRecipes.getOutput(inventory.getStackInSlot(SLOT_IN));
                    if (rec != null) {
                        if (!rec.output.isEmpty()) inventory.insertItem(SLOT_OUT, rec.output.copy(), false);
                        if (rec.fluid != null) {
                            if (tank.getTankType() != rec.fluid.type) tank.setTankType(rec.fluid.type);
                            tank.setFill(tank.getFill() + rec.fluid.fill);
                        }
                        inventory.extractItem(SLOT_IN, 1, false);
                    }
                }
                AABB box = new AABB(worldPosition.getX() - 0.5, worldPosition.getY() + 2, worldPosition.getZ() - 0.5,
                        worldPosition.getX() + 1.5, worldPosition.getY() + 4, worldPosition.getZ() + 1.5);
                for (Entity e : level.getEntitiesOfClass(Entity.class, box)) e.igniteForSeconds(5);
                // CE TileEntityFurnaceCombination.java:129
                if (level.getGameTime() % 20 == 0) {
                    pollute(PollutionHandler.PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * 3);
                }
            }
        } else {
            progress = 0;
        }
        dataChanged();
        networkPackMK2(50);
    }

    /** CE {@code TileEntityMachinePolluting#sendSmoke}. */
    private void sendSmoke(BlockPos pos, Direction dir) {
        if (smoke.getFill() > 0) tryProvide(smoke, level, pos, dir);
        if (smokeLeaded.getFill() > 0) tryProvide(smokeLeaded, level, pos, dir);
        if (smokePoison.getFill() > 0) tryProvide(smokePoison, level, pos, dir);
    }

    /** Exact CE {@code TileEntityMachinePolluting#pollute(PollutionType, float)} {@code :39-48}. */
    public void pollute(PollutionHandler.PollutionType type, float amount) {
        FluidTankNTM dest = type == PollutionHandler.PollutionType.SOOT ? smoke
                : type == PollutionHandler.PollutionType.HEAVYMETAL ? smokeLeaded : smokePoison;
        int fluidAmount = (int) Math.ceil(amount * 100);
        dest.setFill(dest.getFill() + fluidAmount);
        if (dest.getFill() > dest.getMaxFill()) {
            int overflow = dest.getFill() - dest.getMaxFill();
            dest.setFill(dest.getMaxFill());
            PollutionHandler.incrementPollution(level, worldPosition, type, overflow / 100F);
        }
    }

    private boolean canSmelt() {
        ItemStack in = inventory.getStackInSlot(SLOT_IN);
        if (in.isEmpty()) return false;
        CombinationRecipe rec = CombinationRecipes.getOutput(in);
        if (rec == null || rec.output.isEmpty()) return false;
        if (!inventory.insertItem(SLOT_OUT, rec.output.copy(), true).isEmpty()) return false;
        if (rec.fluid != null) {
            if (tank.getTankType() != rec.fluid.type && tank.getFill() > 0) return false;
            return tank.getTankType() != rec.fluid.type || tank.getFill() + rec.fluid.fill <= tank.getMaxFill();
        }
        return true;
    }

    private void tryPullHeat() {
        if (heat >= MAX_HEAT) return;
        if (level.getBlockEntity(worldPosition.below()) instanceof IHeatSource source) {
            int diff = source.getHeatStored() - heat;
            if (diff > 0) {
                diff = (int) Math.ceil(diff * DIFFUSION);
                source.useUpHeat(diff);
                heat = Math.min(heat + diff, MAX_HEAT);
                return;
            }
        }
        heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        // CE TileEntityFurnaceCombination.java:273-274
        return List.of(tank, smoke, smokeLeaded, smokePoison);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tank.writeToNBT(tag, "tank");
        smoke.writeToNBT(tag, "smoke0");
        smokeLeaded.writeToNBT(tag, "smoke1");
        smokePoison.writeToNBT(tag, "smoke2");
        tag.putInt("prog", progress);
        tag.putInt("heat", heat);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(tag, "tank");
        smoke.readFromNBT(tag, "smoke0");
        smokeLeaded.readFromNBT(tag, "smoke1");
        smokePoison.readFromNBT(tag, "smoke2");
        progress = tag.getInt("prog");
        heat = tag.getInt("heat");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(wasOn);
        buf.writeInt(heat);
        buf.writeInt(progress);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        wasOn = buf.readBoolean();
        heat = buf.readInt();
        progress = buf.readInt();
        tank.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FurnaceCombinationMenu(id, inv, this);
    }
}
