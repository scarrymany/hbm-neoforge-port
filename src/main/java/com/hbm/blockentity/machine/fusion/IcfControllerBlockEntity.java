package com.hbm.blockentity.machine.fusion;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.LoadedBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.fusion.IcfControllerBlock;
import com.hbm.blocks.machine.fusion.IcfReactorBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Ported from CE's {@code TileEntityICFController}: a single-block laser accumulator that charges
 * from the HE power network ({@link IEnergyReceiverMK2}, exactly like CE's {@code CapabilityEnergy}
 * bridge, but native HE this time per this port's own energy-system rule) and, once charged, fires
 * its accumulated power in a straight line out of its facing side, feeding whichever
 * {@link IcfReactorBlockEntity} it first hits.
 *
 * <h2>Simplification versus CE (documented, not accidental)</h2>
 * CE's controller counts a dedicated sub-multiblock of {@code icf_cell}/{@code icf_emitter}/
 * {@code icf_capacitor}/{@code icf_turbocharger} blocks strung out behind it to derive
 * {@code getMaxPower()} ({@code sqrt(capacitorCount) * capacitorPower + sqrt(turbochargerCount) *
 * turboPower}), and separately requires a fully-assembled reactor-side {@code icf_component}
 * lattice before {@code assembled} goes true at all. This port keeps the real gameplay mechanic -
 * an HE-charged laser that fires down a line, burns through weak blocks, damages/ignites entities in
 * its path, and feeds a reactor multiblock - but with a flat {@link #MAX_POWER} capacity instead of
 * porting the capacitor/turbocharger sub-multiblock counter. Restoring that counter (once a
 * capacitor/turbocharger block family exists for this area to reference) is flagged as a follow-up.
 */
public class IcfControllerBlockEntity extends LoadedBaseBlockEntity implements IEnergyReceiverMK2, ITickableBE, IPersistentNBT {

    public static final long MAX_POWER = 25_000_000L;
    private static final int MAX_RANGE = 48;

    public long power;
    public int laserLength;
    private boolean destroyedByCreativePlayer;

    public IcfControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        Direction dir = getBlockState().getValue(IcfControllerBlock.FACING);

        if (level.getGameTime() % 20 == 0) {
            BlockPos behind = worldPosition.relative(dir.getOpposite());
            trySubscribe(level, behind, dir.getOpposite());
        }

        if (this.power > 0) {
            fireLaser(dir);
            this.power = 0;
        } else {
            this.laserLength = 0;
        }

        dataChanged();
        networkPackMK2(50);
    }

    private void fireLaser(Direction dir) {
        long firedPower = this.getPower();
        long firedMax = this.getMaxPower();

        int hitLength = 0;
        BlockPos endPos = worldPosition;

        for (int i = 1; i <= MAX_RANGE; i++) {
            BlockPos scan = worldPosition.relative(dir, i);
            BlockState state = level.getBlockState(scan);
            hitLength = i;
            endPos = scan;

            if (state.getBlock() instanceof IcfReactorBlock reactorBlock) {
                BlockPos corePos = reactorBlock.findCore(level, scan);
                if (corePos != null && level.getBlockEntity(corePos) instanceof IcfReactorBlockEntity reactor) {
                    reactor.receiveLaser(firedPower, firedMax);
                    break;
                }
            }

            if (!state.isAir()) {
                float resistance = state.getBlock().getExplosionResistance();
                if (resistance < 6000F) {
                    BlockDummyable.safeRem = true;
                    try {
                        level.destroyBlock(scan, false);
                    } finally {
                        BlockDummyable.safeRem = false;
                    }
                } else {
                    break;
                }
            }
        }

        this.laserLength = hitLength;

        AABB beam = new AABB(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                endPos.getX() + 0.5, endPos.getY() + 0.5, endPos.getZ() + 0.5).inflate(0.3);
        List<Entity> hit = level.getEntitiesOfClass(Entity.class, beam);
        for (Entity e : hit) {
            e.hurt(level.damageSources().inFire(), 50F);
            e.igniteForSeconds(5);
        }
    }

    @Override
    public long getPower() {
        return Math.min(power, getMaxPower());
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.power = tag.getLong("power");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(laserLength);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.power = buf.readLong();
        this.laserLength = buf.readInt();
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        nbt.putLong("power", power);
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        this.power = nbt.getLong("power");
    }

    @Override
    public void setDestroyedByCreativePlayer() {
        this.destroyedByCreativePlayer = true;
    }

    @Override
    public boolean isDestroyedByCreativePlayer() {
        return destroyedByCreativePlayer;
    }
}
