package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.rbmk.RBMKDials;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.util.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Active coolant column - no player-facing GUI in CE either (no {@code IGUIProvider} on
 * {@code TileEntityRBMKCooler}). Exact CE {@code TileEntityRBMKCooler.java:51-129}: 50 mB/t
 * {@code PERFLUOROMETHYL_COLD}→{@code PERFLUOROMETHYL}, −200°C on every RBMK column in a 5×5
 * footprint (floor 20°C), subscribe below, hot out at column-top. {@code rbmk_loader} outlet
 * branches stay skipped (block unregistered).
 */
public class RBMKCoolerBlockEntity extends RBMKBaseBlockEntity implements IFluidStandardTransceiverMK2 {

    protected int timer;
    public final FluidTankNTM[] tanks;
    public int lastCooled;
    /** CE {@code TileEntityRBMKCooler.neighborCache} — 5×5, not the base 4-dir heat cache. */
    private final RBMKBaseBlockEntity[] coolNeighborCache = new RBMKBaseBlockEntity[25];

    public RBMKCoolerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.PERFLUOROMETHYL_COLD, 4_000).withOwner(this),
                new FluidTankNTM(Fluids.PERFLUOROMETHYL, 4_000).withOwner(this)
        };
    }

    protected Component getDefaultName() {
        return Component.translatable("container.rbmkCooler");
    }

    @Override
    public void updateEntity() {
        if (level != null && !level.isClientSide) {
            if (timer <= 0) {
                timer = 60;
                for (int i = 0; i < 25; i++) {
                    int x = worldPosition.getX() - 2 + i / 5;
                    int z = worldPosition.getZ() - 2 + i % 5;
                    if (Compat.getBlockEntityStandard(level, new BlockPos(x, worldPosition.getY(), z)) instanceof RBMKBaseBlockEntity tile) {
                        coolNeighborCache[i] = tile;
                    } else {
                        coolNeighborCache[i] = null;
                    }
                }
            } else {
                timer--;
            }

            if (tanks[0].getFill() >= 50 && tanks[1].getMaxFill() - tanks[1].getFill() >= 50) {
                tanks[0].setFill(tanks[0].getFill() - 50);
                tanks[1].setFill(tanks[1].getFill() + 50);

                int cooled = 0;
                for (RBMKBaseBlockEntity neighbor : coolNeighborCache) {
                    if (neighbor != null && !neighbor.isRemoved()) {
                        double before = neighbor.heat;
                        neighbor.heat -= 200;
                        if (neighbor.heat < 20) neighbor.heat = 20;
                        int delta = (int) (before - neighbor.heat);
                        if (delta > 0) {
                            cooled += delta;
                            neighbor.setChanged();
                        }
                    }
                }
                lastCooled = cooled;
            } else {
                lastCooled = 0;
            }

            trySubscribe(tanks[0].getTankType(), level, worldPosition.below(), Direction.DOWN);

            if (tanks[1].getFill() > 0) {
                for (DirPos out : getOutputPos()) {
                    tryProvide(tanks[1], level, out);
                }
            }
        }

        super.updateEntity();
    }

    /** CE {@code :104-129} default (no {@code rbmk_loader}). */
    protected DirPos[] getOutputPos() {
        int height = level instanceof ServerLevel serverLevel ? RBMKDials.getColumnHeight(serverLevel) : 0;
        return new DirPos[]{
                new DirPos(worldPosition.getX(), worldPosition.getY() + height + 1, worldPosition.getZ(), Direction.UP)
        };
    }

    @Override
    public RBMKColumn.ColumnType getConsoleType() {
        return RBMKColumn.ColumnType.COOLER;
    }

    @Override
    public RBMKColumn getConsoleData() {
        RBMKColumn.CoolerColumn data = (RBMKColumn.CoolerColumn) super.getConsoleData();
        data.cooled = lastCooled;
        data.cryo = tanks[0].getFill();
        data.maxCryo = tanks[0].getMaxFill();
        data.hot = tanks[1].getFill();
        data.maxHot = tanks[1].getMaxFill();
        data.coldType = (short) tanks[0].getTankType().getID();
        data.hotType = (short) tanks[1].getTankType().getID();
        return data;
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tanks);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tanks[0]);
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(tanks[1]);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tanks[0].writeToNBT(tag, "cold");
        tanks[1].writeToNBT(tag, "hot");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tanks[0].readFromNBT(tag, "cold");
        tanks[1].readFromNBT(tag, "hot");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }
}
