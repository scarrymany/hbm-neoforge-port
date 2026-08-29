package com.hbm.capability;

import com.hbm.api.fluidmk2.IFluidProviderMK2;
import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.api.fluidmk2.IFluidUserMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.CapabilityContextProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

import static com.hbm.capability.NTMFluidCapabilityHandler.getFluidType;

/**
 * Wraps NTM's own tile-entity fluid API ({@link IFluidReceiverMK2}/{@link IFluidProviderMK2},
 * backed by {@link FluidTankNTM} tanks) as NeoForge's {@link IFluidHandler}.
 *
 * <p>Ported from CE's {@code NTMFluidHandlerWrapper} with its tank reporting rewritten for modern
 * {@code IFluidHandler}: {@code IFluidTankProperties[]} no longer exists, replaced by an
 * int-indexed {@code getTanks()}/{@code getFluidInTank(int)}/{@code getTankCapacity(int)}/
 * {@code isFluidValid(int, FluidStack)} contract read straight off each {@link FluidTankNTM}
 * (assumed to expose a {@code getFluid()}/{@code getCapacity()}/{@code isFluidValid(FluidStack)}
 * surface mirroring NeoForge's own {@code IFluidTank} convention, once the fluid/inventory area
 * ports it). The pressure-aware fill/drain matching against
 * {@code IFluidReceiverMK2}/{@code IFluidProviderMK2} is unchanged from CE, aside from
 * {@code boolean} becoming {@link FluidAction} and {@code null} "nothing happened" returns
 * becoming {@link FluidStack#EMPTY} (the modern interface never returns null).
 */
public class NTMFluidHandlerWrapper implements IFluidHandler {

    @Nullable
    private final IFluidReceiverMK2 receiver;
    @Nullable
    private final IFluidProviderMK2 provider;
    @NotNull
    private final IFluidUserMK2 user;
    @Nullable
    private final BlockPos accessor;

    /**
     * @param pos The position of the accessor. Null -> Internal access.
     */
    public NTMFluidHandlerWrapper(@NotNull BlockEntity handler, @Nullable BlockPos pos) {
        if (handler instanceof IFluidProviderMK2 providerMK2) this.provider = providerMK2;
        else provider = null;
        if (handler instanceof IFluidReceiverMK2 receiverMK2) this.receiver = receiverMK2;
        else receiver = null;
        if (receiver == null && provider == null)
            throw new IllegalArgumentException("BlockEntity " + handler.getClass().getName() + " must implement IFluidReceiverMK2 or IFluidProviderMK2");
        user = (IFluidUserMK2) handler;
        this.accessor = pos;
    }

    public NTMFluidHandlerWrapper(@NotNull BlockEntity handler) {
        this(handler, null);
    }

    private static int clampToInt(long v) {
        if (v <= 0) return 0;
        return v > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) v;
    }

    private <T> T withAccessor(Supplier<T> action) {
        if (accessor == null) return action.get();
        var prev = CapabilityContextProvider.pushPos(accessor);
        try {
            return action.get();
        } finally {
            CapabilityContextProvider.popPos(prev);
        }
    }

    @Nullable
    private FluidTankNTM tankAt(int index) {
        List<FluidTankNTM> tanks = user.getAllTanks();
        return index >= 0 && index < tanks.size() ? tanks.get(index) : null;
    }

    @Override
    public int getTanks() {
        return withAccessor(() -> user.getAllTanks().size());
    }

    @NotNull
    @Override
    public FluidStack getFluidInTank(int tank) {
        return withAccessor(() -> {
            FluidTankNTM ntmTank = tankAt(tank);
            if (ntmTank == null) return FluidStack.EMPTY;
            FluidStack contents = ntmTank.getFluid();
            return contents == null ? FluidStack.EMPTY : contents;
        });
    }

    @Override
    public int getTankCapacity(int tank) {
        return withAccessor(() -> {
            FluidTankNTM ntmTank = tankAt(tank);
            return ntmTank == null ? 0 : ntmTank.getCapacity();
        });
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return withAccessor(() -> {
            FluidTankNTM ntmTank = tankAt(tank);
            return ntmTank != null && ntmTank.isFluidValid(stack);
        });
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || receiver == null) return 0;
        return withAccessor(() -> fillInternal(resource, action));
    }

    /**
     * NTM tanks match on (type, pressure) as a pair - see IFluidStandardReceiverMK2#getDemand -
     * but a plain FluidStack has no pressure field to carry that information across the
     * capability boundary. So: ask the receiver which pressures it actually has tanks for at this
     * fluid type (getReceivingPressureRange), and try each one in turn, lowest first. Every
     * existing implementer either overrides this properly (IFluidStandardReceiverMK2, scanning
     * its own getReceivingTanks()) or falls back to the {0,0} default.
     */
    private int fillInternal(FluidStack resource, FluidAction action) {
        FluidType type = getFluidType(resource.getFluid());
        if (type == null) return 0;
        int[] range = receiver.getReceivingPressureRange(type);
        int remaining = resource.getAmount();
        int filled = 0;
        for (int p = range[0]; p <= range[1] && remaining > 0; p++) {
            long demand = receiver.getDemand(type, p);
            if (demand <= 0) continue;
            int offer = Math.min(remaining, clampToInt(demand));
            if (offer <= 0) continue;
            if (action.execute()) {
                int remainder = (int) receiver.transferFluid(type, p, offer);
                int accepted = offer - remainder;
                filled += accepted;
                remaining -= accepted;
            } else {
                filled += offer;
                remaining -= offer;
            }
        }
        return filled;
    }

    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || provider == null) return FluidStack.EMPTY;
        return withAccessor(() -> drainInternal(resource, action));
    }

    /** Pressure-aware counterpart to fillInternal, see its javadoc. */
    @NotNull
    private FluidStack drainInternal(FluidStack resource, FluidAction action) {
        FluidType type = getFluidType(resource.getFluid());
        if (type == null) return FluidStack.EMPTY;
        int[] range = provider.getProvidingPressureRange(type);
        int remaining = resource.getAmount();
        int drained = 0;
        for (int p = range[0]; p <= range[1] && remaining > 0; p++) {
            long available = provider.getFluidAvailable(type, p);
            if (available <= 0) continue;
            int toDrain = Math.min(remaining, clampToInt(available));
            if (toDrain <= 0) continue;
            if (action.execute()) provider.useUpFluid(type, p, toDrain);
            drained += toDrain;
            remaining -= toDrain;
        }
        if (drained <= 0) return FluidStack.EMPTY;
        FluidStack out = resource.copy();
        out.setAmount(drained);
        return out;
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0 || provider == null) return FluidStack.EMPTY;
        return withAccessor(() -> drainInternal(maxDrain, action));
    }

    @NotNull
    private FluidStack drainInternal(int maxDrain, FluidAction action) {
        for (FluidTankNTM tank : provider.getAllTanks()) {
            FluidType type = tank.getTankType();
            int pressure = tank.getPressure(); // this IS the actual tank, so no guessing needed here
            long available = provider.getFluidAvailable(type, pressure);
            if (available <= 0) continue;
            int toDrain = Math.min(maxDrain, clampToInt(available));
            if (toDrain <= 0) continue;
            FluidStack exemplar = tank.drain(toDrain, FluidAction.SIMULATE);
            if (exemplar == null || exemplar.isEmpty()) continue;
            exemplar.setAmount(toDrain);
            if (action.execute()) provider.useUpFluid(type, pressure, toDrain);
            return exemplar;
        }
        return FluidStack.EMPTY;
    }
}
