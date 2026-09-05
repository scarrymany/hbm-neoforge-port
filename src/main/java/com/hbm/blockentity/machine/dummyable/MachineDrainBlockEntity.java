package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.machine.dummyable.DrainMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.inventory.fluid.trait.FluidTraitSimple;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.DirPos;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityMachineDrain} — subscribe + spill {@code max(fill/2, 1)}.
 * Amat explodes.
 * {@code FT_Polluting.pollute(SPILL, toSpill)} Exact CE {@code TileEntityMachineDrain.java:69}.
 * {@code oil_spill} raycast / particles stay skipped.
 */
public class MachineDrainBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardReceiverMK2, ITickableBE, MenuProvider {

    public final FluidTankNTM tank;

    public MachineDrainBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1, true, false);
        this.tank = new FluidTankNTM(Fluids.NONE, 2_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm.machine_drain");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof IItemFluidIdentifier;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        ItemStack id = inventory.getStackInSlot(0);
        if (!id.isEmpty() && id.getItem() instanceof IItemFluidIdentifier ident) {
            tank.setTankType(ident.getType(level, worldPosition, id));
        }

        if (level.getGameTime() % 20 == 0) {
            for (DirPos pos : getConPos()) trySubscribe(tank.getTankType(), level, pos);
        }

        if (tank.getFill() > 0) {
            if (tank.getTankType().hasTrait(FluidTraitSimple.FT_Amat.class)) {
                level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                        10.0F, true, Level.ExplosionInteraction.TNT);
                return;
            }
            int toSpill = Math.max(tank.getFill() / 2, 1);
            tank.setFill(tank.getFill() - toSpill);
            // CE TileEntityMachineDrain.java:69
            FT_Polluting.pollute(level, worldPosition, tank.getTankType(),
                    FluidTrait.FluidReleaseType.SPILL, toSpill);
        }

        dataChanged();
        networkPackMK2(50);
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();
        return new DirPos[]{
                new DirPos(worldPosition.relative(dir), dir),
                new DirPos(worldPosition.relative(rot), rot),
                new DirPos(worldPosition.relative(rot.getOpposite()), rot.getOpposite())
        };
    }

    private Direction coreFacing() {
        int meta = getBlockState().getValue(BlockDummyable.META);
        return meta >= 12 ? Direction.from3DDataValue(meta - BlockDummyable.offset) : Direction.NORTH;
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
        tank.writeToNBT(tag, "t");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(tag, "t");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tank.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new DrainMenu(id, inv, this);
    }
}
