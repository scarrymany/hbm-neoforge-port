package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Active coolant column - no player-facing GUI in CE either (no {@code IGUIProvider} on
 * {@code TileEntityRBMKCooler}, confirmed by signature survey), no inventory. Consumes cold coolant,
 * removes column heat, outputs warmed coolant. Ported (simplified consumption-rate approximation,
 * tank sizes/fluid types CE-confirmed) from CE's {@code TileEntityRBMKCooler} (237 lines).
 */
public class RBMKCoolerBlockEntity extends RBMKBaseBlockEntity implements IFluidStandardTransceiverMK2 {

    private static final int COOL_PER_MB = 1;

    public final FluidTankNTM[] tanks;
    public int lastCooled;

    public RBMKCoolerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.PERFLUOROMETHYL_COLD, 4_000).withOwner(this),
                new FluidTankNTM(Fluids.PERFLUOROMETHYL, 4_000).withOwner(this)
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkCooler");
    }

    @Override
    public void updateEntity() {
        if (level != null && !level.isClientSide) {
            int cool = Math.min(tanks[0].getFill(), tanks[1].getMaxFill() - tanks[1].getFill());
            cool = (int) Math.min(cool, Math.max(0, (heat - 20D) / COOL_PER_MB));

            if (cool > 0) {
                tanks[0].setFill(tanks[0].getFill() - cool);
                tanks[1].setFill(tanks[1].getFill() + cool);
                heat -= cool * COOL_PER_MB;
            }
            lastCooled = cool;

            trySubscribe(tanks[0].getTankType(), level, worldPosition.below(), Direction.UP);
            tryProvide(tanks[1], level, worldPosition.above(), Direction.DOWN);
        }

        super.updateEntity();
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
