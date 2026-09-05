package com.hbm.blockentity.machine.fusion;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.machine.fusion.FusionKlystronMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.uninos.UniNodespace;
import com.hbm.uninos.networkproviders.KlystronNetwork;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * CE {@code TileEntityFusionKlystron}.
 * TODO(CE: TileEntityFusionKlystron.java:132): AudioWrapper fel loop — VFX last.
 * TODO(CE: TileEntityFusionKlystron.java:340): OpenComputers ntm_fusion_klystron.
 */
public class FusionKlystronBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardReceiverMK2, ITickableBE, MenuProvider, IControlReceiver {

    public static final long MAX_OUTPUT = 1_000_000;
    public static final int AIR_CONSUMPTION = 2_500;

    protected KlystronNetwork.KlystronNode klystronNode;
    public long outputTarget;
    public long output;
    public long power;
    public long maxPower = 1_000_000L;
    public final FluidTankNTM compair;

    public FusionKlystronBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1, true, true);
        this.compair = new FluidTankNTM(Fluids.AIR, AIR_CONSUMPTION * 60).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.fusionKlystron");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && Library.isBattery(stack);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        this.maxPower = Math.max(1_000_000L, this.outputTarget * 100L);
        this.power = Library.chargeTEFromItems(inventory, 0, power, maxPower);

        for (DirPos pos : getConPos()) {
            trySubscribe(level, pos);
            trySubscribe(compair.getTankType(), level, pos);
        }

        this.output = 0;
        double powerFactor = FusionTorusBlockEntity.getSpeedScaled(maxPower, power);
        double airFactor = FusionTorusBlockEntity.getSpeedScaled(compair.getMaxFill(), compair.getFill());
        double factor = Math.min(powerFactor, airFactor);
        long powerReq = (long) Math.ceil(outputTarget * factor);
        int airReq = (int) Math.ceil(AIR_CONSUMPTION * factor);

        if (outputTarget > 0 && power >= powerReq && compair.getFill() >= airReq) {
            this.output = powerReq;
            this.power -= powerReq;
            this.compair.setFill(this.compair.getFill() - airReq);
        }
        if (output < outputTarget / 50) output = 0;

        this.klystronNode = handleKNode(klystronNode, this);
        provideKyU(klystronNode, this.output);
        networkPackNT(100);
    }

    public DirPos[] getConPos() {
        Direction dir = FusionFacing.of(this);
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.getX() + dir.getStepX() * 4, p.getY() + 2, p.getZ() + dir.getStepZ() * 4, dir),
                new DirPos(p.getX() + rot.getStepX() * 3, p.getY(), p.getZ() + rot.getStepZ() * 3, rot),
                new DirPos(p.getX() - rot.getStepX() * 3, p.getY(), p.getZ() - rot.getStepZ() * 3, rot.getOpposite())
        };
    }

    public static KlystronNetwork.KlystronNode handleKNode(KlystronNetwork.KlystronNode klystronNode, BlockEntity that) {
        Level world = that.getLevel();
        BlockPos pos = that.getBlockPos();
        if (world == null) return klystronNode;

        if (klystronNode == null || klystronNode.expired) {
            Direction dir = FusionFacing.of(that).getOpposite();
            BlockPos nodePos = pos.offset(dir.getStepX() * 4, 2, dir.getStepZ() * 4);
            klystronNode = UniNodespace.getNode(world, nodePos, KlystronNetwork.THE_PROVIDER);
            if (klystronNode == null) {
                klystronNode = (KlystronNetwork.KlystronNode) new KlystronNetwork.KlystronNode(KlystronNetwork.THE_PROVIDER, nodePos)
                        .setConnections(new DirPos(pos.getX() + dir.getStepX() * 5, pos.getY() + 2, pos.getZ() + dir.getStepZ() * 5, dir));
                UniNodespace.createNode(world, klystronNode);
            }
        }
        if (klystronNode.net != null) klystronNode.net.addProvider(that);
        return klystronNode;
    }

    public static boolean provideKyU(KlystronNetwork.KlystronNode klystronNode, long output) {
        if (klystronNode == null || klystronNode.net == null) return false;
        for (BlockEntity te : klystronNode.net.receiverEntries.keySet()) {
            if (te instanceof FusionTorusBlockEntity torus && torus.isLoaded() && !torus.isRemoved()) {
                torus.klystronEnergy += output;
                return true;
            }
        }
        return false;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide && klystronNode != null) {
            UniNodespace.destroyNode(level, klystronNode);
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeLong(outputTarget);
        buf.writeLong(output);
        compair.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        outputTarget = buf.readLong();
        output = buf.readLong();
        compair.deserialize(buf);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putLong("maxPower", maxPower);
        tag.putLong("outputTarget", outputTarget);
        compair.writeToNBT(tag, "t");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        maxPower = tag.getLong("maxPower");
        outputTarget = tag.getLong("outputTarget");
        compair.readFromNBT(tag, "t");
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
        return Math.max(maxPower, 1_000_000L);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(compair);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(compair);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 2.5, worldPosition.getZ() + 0.5) < 20 * 20;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("amount")) {
            this.outputTarget = data.getLong("amount");
            if (this.outputTarget < 0) this.outputTarget = 0;
            if (this.outputTarget > MAX_OUTPUT) this.outputTarget = MAX_OUTPUT;
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new FusionKlystronMenu(id, inv, this);
    }
}
