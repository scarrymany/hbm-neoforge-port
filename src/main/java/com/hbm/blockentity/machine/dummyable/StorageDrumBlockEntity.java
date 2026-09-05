package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.hazard.HazardSystem;
import com.hbm.inventory.container.machine.dummyable.StorageDrumMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.StorageDrumRecipes;
import com.hbm.lib.DirPos;
import com.hbm.util.ContaminationUtil;
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
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityStorageDrum.java}:38- — 24 slots, WASTEFLUID/WASTEGAS 16k.
 * Long/short waste siblings + mercury I/O registered — CE table is live.
 * Exact CE overflow {@code incrementRad} {@code :86-96}, ambient {@code radiate} {@code :66-68}/
 * {@code :110-112} (X/Z swapped), neutron decay {@code :80-82}.
 */
public class StorageDrumBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardSenderMK2, ITickableBE, MenuProvider {

    /** Exact CE {@code TileEntityStorageDrum.java}:36 — 10s half-life. */
    private static final float DECAY_RATE = 0.9965402628F;

    public final FluidTankNTM liquid;
    public final FluidTankNTM gas;

    public StorageDrumBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 24, true, false);
        this.liquid = new FluidTankNTM(Fluids.WASTEFLUID, 16_000).withOwner(this);
        this.gas = new FluidTankNTM(Fluids.WASTEGAS, 16_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.storageDrum");
    }

    /** Exact CE {@code TileEntityStorageDrum.java}:133-135 — waste recipe or neutron-contaminated. */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return StorageDrumRecipes.getWaste(stack) != null || ContaminationUtil.isContaminated(stack);
    }

    /** Exact CE {@code TileEntityStorageDrum.java}:143-145 — extract only after decay / recipe done. */
    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return !ContaminationUtil.isContaminated(stack) && StorageDrumRecipes.getWaste(stack) == null;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        int[] slots = new int[24];
        for (int i = 0; i < 24; i++) slots[i] = i;
        return slots;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;
        StorageDrumRecipes.register();

        double rad = 0D;
        int liquidAmt = 0;
        int gasAmt = 0;

        for (int i = 0; i < 24; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            // Exact CE :66-68 — ambient rad sum every 20t
            if (level.getGameTime() % 20 == 0) {
                rad += HazardSystem.getRawRadsFromStack(stack);
            }

            StorageDrumRecipes.WasteData data = StorageDrumRecipes.getWaste(stack);
            if (data != null) {
                // Exact CE :71-78 — convert even when tanks are full; overflow radiates below
                if (level.random.nextInt(Math.max(1, data.chance())) == 0) {
                    liquidAmt += data.liquid();
                    gasAmt += data.gas();
                    inventory.setStackInSlot(i, data.output().copy());
                }
            } else {
                // Exact CE :80-82 — non-recipe stacks decay in place (10s half-life)
                ContaminationUtil.neutronActivateItem(stack, 0.0F, DECAY_RATE);
            }
        }

        // Exact CE :86-96 — overflow * 0.5F then clamp-fill
        int liquidOverflow = Math.max(liquid.getFill() + liquidAmt - liquid.getMaxFill(), 0);
        int gasOverflow = Math.max(gas.getFill() + gasAmt - gas.getMaxFill(), 0);
        if (liquidOverflow > 0) {
            ChunkRadiationManager.proxy.incrementRad(level, worldPosition, liquidOverflow * 0.5F);
        }
        if (gasOverflow > 0) {
            ChunkRadiationManager.proxy.incrementRad(level, worldPosition, gasOverflow * 0.5F);
        }
        liquid.fill(Fluids.WASTEFLUID, liquidAmt, true);
        gas.fill(Fluids.WASTEGAS, gasAmt, true);

        long age = level.getGameTime() % 20;
        for (DirPos pos : getConPos()) {
            if (age == 9 || age == 19) tryProvide(liquid, level, pos);
            if (age == 8 || age == 18) tryProvide(gas, level, pos);
        }

        // Exact CE :110-112 — X/Z swapped on purpose
        if (rad > 0) {
            ContaminationUtil.radiate(level, worldPosition.getZ(), worldPosition.getY(), worldPosition.getX(), 32, (float) rad);
        }

        dataChanged();
        networkPackMK2(25);
    }

    public DirPos[] getConPos() {
        return new DirPos[]{
                new DirPos(worldPosition.above(), Direction.UP),
                new DirPos(worldPosition.below(), Direction.DOWN),
                new DirPos(worldPosition.north(), Direction.NORTH),
                new DirPos(worldPosition.south(), Direction.SOUTH),
                new DirPos(worldPosition.east(), Direction.EAST),
                new DirPos(worldPosition.west(), Direction.WEST),
        };
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(liquid, gas);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(liquid, gas);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        liquid.writeToNBT(tag, "l");
        gas.writeToNBT(tag, "g");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        liquid.readFromNBT(tag, "l");
        gas.readFromNBT(tag, "g");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        liquid.serialize(buf);
        gas.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        liquid.deserialize(buf);
        gas.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new StorageDrumMenu(id, inv, this);
    }
}
