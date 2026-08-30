package com.hbm.blockentity.machine.rbmk;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.rbmk.IRBMKControlColumn;
import com.hbm.api.rbmk.RBMKDials;
import com.hbm.blocks.machine.rbmk.RBMKControlBlock;
import com.hbm.handler.neutron.RBMKNeutronHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Control rod extraction-level movement, ported from CE's {@code TileEntityRBMKControl} (256 lines).
 * Concrete GUI-facing variants ({@code Manual}/{@code Auto}) subclass this - see those files.
 * ReaSim-powered variants consume HE via {@link IEnergyReceiverMK2}, already ported and confirmed
 * real by this port's own {@code com.hbm.api.energymk2} (Phase 0) - used unchanged from CE.
 * <p>
 * <b>Naming note</b>: CE's field is named {@code level} (the rod's 0-1 extraction fraction) - renamed
 * to {@link #extraction} here because {@code BlockEntity} itself declares a {@code protected Level
 * level} field (the world) in NeoForge 1.21.1, which CE's 1.12 {@code TileEntity} base (field
 * {@code world}) never had. Every other CE name is kept unchanged.
 */
public abstract class RBMKControlBlockEntity extends RBMKSlottedBlockEntity implements IEnergyReceiverMK2, IRBMKControlColumn {

    /** CE field name {@code level} - see class javadoc for the rename. */
    public double extraction;
    public double lastExtraction;
    public static final double SPEED = 0.00277D;
    public double targetLevel;

    public boolean hasPower = false;
    public long power;
    public static final long CONSUMPTION = 5_000;
    public static final long MAX_POWER = CONSUMPTION * 10;

    protected RBMKControlBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0);
    }

    public boolean isPowered() {
        return getBlockState().getBlock() instanceof RBMKControlBlock cb && cb.reasimPowered;
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
        return isPowered() ? MAX_POWER : 0;
    }

    @Override
    public boolean canConnect(Direction dir) {
        return isPowered() && dir == Direction.DOWN;
    }

    @Override
    public ConnectionPriority getPriority() {
        return ConnectionPriority.LOW;
    }

    public boolean isLidRemovable() {
        return false;
    }

    @Override
    public void updateEntity() {
        if (this.level.isClientSide) {
            this.lastExtraction = this.extraction;
        } else {
            this.hasPower = true;

            if (isPowered()) {
                this.trySubscribe(level, worldPosition.below(), Direction.DOWN);
                if (this.power < CONSUMPTION) this.hasPower = false;
            }

            this.lastExtraction = this.extraction;

            if (this.hasPower) {
                double dialSpeed = RBMKDials.getControlSpeed((ServerLevel) this.level);
                if (this.extraction < targetLevel) {
                    this.extraction = Math.min(this.extraction + SPEED * dialSpeed, targetLevel);
                }
                if (this.extraction > targetLevel) {
                    this.extraction = Math.max(this.extraction - SPEED * dialSpeed, targetLevel);
                }

                if (isPowered() && this.extraction != lastExtraction) {
                    this.power -= CONSUMPTION;
                }
            }
        }

        super.updateEntity();
    }

    public void setTarget(double target) {
        this.targetLevel = target;
    }

    // implements IRBMKControlColumn.getLevel() - com.hbm.api.rbmk.IRBMKControlColumn
    @Override
    public double getLevel() {
        return this.extraction;
    }

    /**
     * implements IRBMKControlColumn.getMult() - CE: {@code TileEntityRBMKControl.getMult()}, the raw
     * extraction level with no surge (see {@link RBMKControlManualBlockEntity#getMult()} for the
     * surge override).
     */
    @Override
    public double getMult() {
        return this.extraction;
    }

    public int trackingRange() {
        return 100;
    }

    @Override
    public void onMelt(int reduce) {
        if (isModerated()) {
            int count = 2 + this.level.getRandom().nextInt(2);
            for (int i = 0; i < count; i++) spawnDebris("GRAPHITE");
        }

        int count = 2 + this.level.getRandom().nextInt(2);
        for (int i = 0; i < count; i++) spawnDebris("ROD");

        this.standardMelt(reduce);
    }

    @Override
    public boolean isModerated() {
        return getBlockState().getBlock() instanceof RBMKControlBlock cb && cb.moderated;
    }

    @Override
    public RBMKNeutronHandler.RBMKType getRBMKType() {
        return RBMKNeutronHandler.RBMKType.CONTROL_ROD;
    }

    @Override
    public RBMKColumn getConsoleData() {
        RBMKColumn.ControlColumn data = (RBMKColumn.ControlColumn) super.getConsoleData();
        data.level = this.extraction;
        return data;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("level", this.extraction);
        tag.putDouble("targetLevel", this.targetLevel);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.extraction = tag.getDouble("level");
        this.targetLevel = tag.getDouble("targetLevel");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(this.extraction);
        buf.writeDouble(this.targetLevel);
        buf.writeLong(this.power);
        buf.writeBoolean(this.hasPower);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.extraction = buf.readDouble();
        this.targetLevel = buf.readDouble();
        this.power = buf.readLong();
        this.hasPower = buf.readBoolean();
    }
}
