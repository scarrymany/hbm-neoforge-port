package com.hbm.blockentity.machine.pile;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.blocks.machine.pile.PileBlocks;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * CE {@code TileEntityPileVent}. AIR tank 4000 @ pressure 1. Pushes into META_AIR_IN channel.
 * TODO(CE: RenderPileVent.java:1): fan OBJ TESR — cube + CE png.
 */
public class PileVentBlockEntity extends PileDeviceBaseBlockEntity implements IFluidStandardReceiverMK2 {

    public final FluidTankNTM compair;
    public boolean isActive = false;
    public float fan;
    public float lastFan;

    public PileVentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.compair = new FluidTankNTM(Fluids.AIR, 4_000).withPressure(1).withOwner(this);
    }

    @Override
    public @NotNull List<FluidTankNTM> getAllTanks() {
        return List.of(compair);
    }

    @Override
    public @NotNull List<FluidTankNTM> getReceivingTanks() {
        return List.of(compair);
    }

    @Override
    public boolean canConnect(FluidType type, Direction dir) {
        if (type != compair.getTankType()) return false;
        return dir == getOrientation();
    }

    @Override
    public void updateEntity() {
        if (level == null) return;

        if (!level.isClientSide) {
            Direction dir = getOrientation();
            this.trySubscribe(compair.getTankType(), level,
                    worldPosition.getX() + dir.getStepX(), worldPosition.getY(), worldPosition.getZ() + dir.getStepZ(), dir);

            this.isActive = false;

            BlockPos inlet = worldPosition.offset(-dir.getStepX(), 0, -dir.getStepZ());
            BlockState inletState = level.getBlockState(inlet);

            if (inletState.getBlock() == PileBlocks.PILE_BLOCK.get()
                    && inletState.getValue(BlockPile.META) == BlockPile.META_AIR_IN) {
                BlockEntity tile = level.getBlockEntity(inlet);
                if (tile instanceof PileBaseBlockEntity pile) {
                    PileCoreBlockEntity core = pile.getCore();
                    if (core != null) {
                        PileCoreBlockEntity.PileChannel ventChan = core.getVentilationChannel(inlet.getX(), inlet.getY(), inlet.getZ());
                        if (ventChan != null) {
                            this.chanNum = core.ventilationChannels.indexOf(ventChan);
                            int toFill = Math.min(compair.getFill(), PileCoreBlockEntity.PileChannel.MAX_AIR - ventChan.air);
                            ventChan.air += toFill;
                            this.compair.setFill(this.compair.getFill() - toFill);
                            this.isActive = toFill > 0;
                        }
                    }
                }
            }

            this.networkPackNT(35);
        } else {
            this.lastFan = fan;
            if (this.isActive) {
                this.fan += 45;
                if (level.random.nextInt(20) == 0) {
                    level.addParticle(ParticleTypes.CLOUD,
                            worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5,
                            0, 0.05, 0);
                }
            }
            if (this.fan >= 360) {
                this.lastFan -= 360;
                this.fan -= 360;
            }
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(this.isActive);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.isActive = buf.readBoolean();
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        compair.readFromNBT(nbt, "t");
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        compair.writeToNBT(nbt, "t");
    }
}
