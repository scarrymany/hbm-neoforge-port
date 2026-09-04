package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.api.rbmk.RBMKDials;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.machine.rbmk.RBMKBaseBlock;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Standalone superheated-steam export pipe — not an RBMK grid column. Exact CE
 * {@code TileEntityRBMKOutlet.java:32-55}: pull {@code reasimSteam} from adjacent cores when
 * {@code getReasimBoilers}, then {@code fillFluidInit} all 6 faces. {@code rbmk_loader} stays skipped.
 */
public class RBMKOutletBlockEntity extends LoadedBaseBlockEntity implements IFluidStandardSenderMK2, ITickableBE {

    /** CE {@code ForgeDirection.getOrientation(2..5)} — N/S/W/E. */
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    public final FluidTankNTM steam;

    public RBMKOutletBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        steam = new FluidTankNTM(Fluids.SUPERHOTSTEAM, 32_000).withOwner(this);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        if (level instanceof ServerLevel serverLevel && RBMKDials.getReasimBoilers(serverLevel)) {
            for (Direction dir : HORIZONTAL) {
                BlockPos npos = worldPosition.relative(dir);
                if (level.getBlockState(npos).getBlock() instanceof RBMKBaseBlock rbmkBlock) {
                    BlockPos core = rbmkBlock.findCore(level, npos);
                    if (core != null && level.getBlockEntity(core) instanceof RBMKBaseBlockEntity rbmk) {
                        int prov = Math.min(steam.getMaxFill() - steam.getFill(), rbmk.reasimSteam);
                        rbmk.reasimSteam -= prov;
                        steam.setFill(steam.getFill() + prov);
                    }
                }
            }
        }

        for (Direction dir : Direction.values()) {
            tryProvide(steam, level, worldPosition.relative(dir), dir);
        }

        dataChanged();
        networkPackMK2(25);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(steam);
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(steam);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        steam.writeToNBT(tag, "steam");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        steam.readFromNBT(tag, "steam");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        steam.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        steam.deserialize(buf);
    }
}
