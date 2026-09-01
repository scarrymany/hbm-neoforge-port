package com.hbm.blockentity.machine.dummyable;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.container.machine.dummyable.CatalyticCrackerMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CrackingRecipes;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.DirPos;
import com.hbm.util.Tuple.Pair;
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
 * CE {@code TileEntityMachineCatalyticCracker}: 100+200 steam / 5t ×2, 5 tanks. Real menu (CE overlay).
 */
public class MachineCatalyticCrackerBlockEntity extends MachineBaseBlockEntity
        implements IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public final FluidTankNTM oil;
    public final FluidTankNTM steam;
    public final FluidTankNTM left;
    public final FluidTankNTM right;
    public final FluidTankNTM spent;

    public MachineCatalyticCrackerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1, true, false);
        this.oil = new FluidTankNTM(Fluids.BITUMEN, 4_000).withOwner(this);
        this.steam = new FluidTankNTM(Fluids.STEAM, 8_000).withOwner(this);
        this.left = new FluidTankNTM(Fluids.OIL, 4_000).withOwner(this);
        this.right = new FluidTankNTM(Fluids.PETROLEUM, 4_000).withOwner(this);
        this.spent = new FluidTankNTM(Fluids.SPENTSTEAM, 4_000).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm.machine_catalytic_cracker");
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
            oil.setTankType(ident.getType(level, worldPosition, id));
        }

        setupTanks();
        if (level.getGameTime() % 20 == 0) {
            for (DirPos pos : getConPos()) {
                trySubscribe(oil.getTankType(), level, pos);
                trySubscribe(steam.getTankType(), level, pos);
            }
        }
        if (level.getGameTime() % 5 == 0) crack();
        if (level.getGameTime() % 10 == 0) {
            for (DirPos pos : getConPos()) {
                if (left.getFill() > 0) tryProvide(left, level, pos);
                if (right.getFill() > 0) tryProvide(right, level, pos);
                if (spent.getFill() > 0) tryProvide(spent, level, pos);
            }
        }

        dataChanged();
        networkPackMK2(50);
    }

    private void setupTanks() {
        Pair<FluidStack, FluidStack> rec = CrackingRecipes.getCracking(oil.getTankType());
        if (rec != null) {
            steam.setTankType(Fluids.STEAM);
            left.setTankType(rec.getKey().type);
            right.setTankType(rec.getValue().type);
            spent.setTankType(Fluids.SPENTSTEAM);
        }
    }

    private void crack() {
        Pair<FluidStack, FluidStack> rec = CrackingRecipes.getCracking(oil.getTankType());
        if (rec == null) return;
        int l = rec.getKey().fill;
        int r = rec.getValue().fill;
        for (int i = 0; i < 2; i++) {
            if (oil.getFill() >= 100 && steam.getFill() >= 200
                    && left.getFill() + l <= left.getMaxFill()
                    && right.getFill() + r <= right.getMaxFill()
                    && spent.getFill() + 2 <= spent.getMaxFill()) {
                oil.setFill(oil.getFill() - 100);
                steam.setFill(steam.getFill() - 200);
                left.setFill(left.getFill() + l);
                right.setFill(right.getFill() + r);
                spent.setFill(spent.getFill() + 2);
            }
        }
    }

    public DirPos[] getConPos() {
        Direction dir = coreFacing();
        Direction rot = dir.getClockWise();
        return new DirPos[]{
                new DirPos(worldPosition.relative(dir, 4).relative(rot), dir),
                new DirPos(worldPosition.relative(dir, 4).relative(rot.getOpposite(), 2), dir),
                new DirPos(worldPosition.relative(dir.getOpposite(), 4).relative(rot), dir.getOpposite()),
                new DirPos(worldPosition.relative(dir.getOpposite(), 4).relative(rot.getOpposite(), 2), dir.getOpposite()),
                new DirPos(worldPosition.relative(dir, 2).relative(rot, 3), rot),
                new DirPos(worldPosition.relative(dir, 2).relative(rot.getOpposite(), 4), rot.getOpposite()),
                new DirPos(worldPosition.relative(dir.getOpposite(), 2).relative(rot, 3), rot),
                new DirPos(worldPosition.relative(dir.getOpposite(), 2).relative(rot.getOpposite(), 4), rot.getOpposite()),
        };
    }

    private Direction coreFacing() {
        int meta = getBlockState().getValue(BlockDummyable.META);
        return meta >= 12 ? Direction.from3DDataValue(meta - BlockDummyable.offset) : Direction.NORTH;
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(oil, steam);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        return List.of(left, right, spent);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(oil, steam, left, right, spent);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        oil.writeToNBT(tag, "t0");
        steam.writeToNBT(tag, "t1");
        left.writeToNBT(tag, "t2");
        right.writeToNBT(tag, "t3");
        spent.writeToNBT(tag, "t4");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        oil.readFromNBT(tag, "t0");
        steam.readFromNBT(tag, "t1");
        left.readFromNBT(tag, "t2");
        right.readFromNBT(tag, "t3");
        spent.readFromNBT(tag, "t4");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        oil.serialize(buf);
        steam.serialize(buf);
        left.serialize(buf);
        right.serialize(buf);
        spent.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        oil.deserialize(buf);
        steam.deserialize(buf);
        left.deserialize(buf);
        right.deserialize(buf);
        spent.deserialize(buf);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new CatalyticCrackerMenu(id, inv, this);
    }
}
