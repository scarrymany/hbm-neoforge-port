package com.hbm.capability;

import com.hbm.inventory.FluidContainerRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adapts an NTM fluid-container {@link ItemStack} (see {@link FluidContainerRegistry}) to
 * NeoForge's item fluid-handler capability.
 *
 * <p>Ported from the {@code Wrapper} inner class of CE's {@code NTMFluidCapabilityHandler} and
 * promoted to a top-level class, with its tank reporting rewritten: modern
 * {@code IFluidHandler} dropped {@code IFluidTankProperties[]} entirely in favor of an int-indexed
 * {@code getTanks()}/{@code getFluidInTank(int)}/{@code getTankCapacity(int)}/{@code isFluidValid(int, FluidStack)}
 * contract, and {@code fill}/{@code drain} now take a {@link FluidAction} instead of a
 * {@code boolean}. Every NTM container is still modeled as exactly one tank (index 0): a known
 * full container reports its fixed content, an empty candidate reports its precomputed maximum
 * fill capacity - the same fast-path CE used for {@code getTankProperties()}.
 *
 * <p>{@code FluidStack} no longer carries an NBT {@code tag} field; CE's "reject stacks carrying
 * extra NBT" guard becomes {@link FluidStack#isComponentsPatchEmpty()} (data components replaced
 * NBT tags on stacks). {@code null} returns for "nothing filled/drained" become
 * {@link FluidStack#EMPTY}, matching the modern {@code IFluidHandler} contract of never returning
 * null.
 */
public final class NTMFluidContainerWrapper implements IFluidHandlerItem {

    private static final int TANK_COUNT = 1;

    private ItemStack container;

    public NTMFluidContainerWrapper(@NotNull ItemStack container) {
        this.container = container;
    }

    @Override
    public int getTanks() {
        return TANK_COUNT;
    }

    @NotNull
    @Override
    public FluidStack getFluidInTank(int tank) {
        FluidStack contents = getContentsInternal();
        return contents == null ? FluidStack.EMPTY : contents;
    }

    @Override
    public int getTankCapacity(int tank) {
        FluidContainerRegistry.FluidContainer full = FluidContainerRegistry.getFluidContainer(this.container);
        if (full != null) {
            return full.content();
        }
        return Math.max(0, FluidContainerRegistry.getMaxFillCapacity(this.container));
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return getContentsInternal() == null
                && stack.isComponentsPatchEmpty()
                && FluidContainerRegistry.getFillRecipe(this.container, stack.getFluid()) != null;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || getContentsInternal() != null || !resource.isComponentsPatchEmpty()) return 0;
        FluidContainerRegistry.FluidContainer fillRecipe = FluidContainerRegistry.getFillRecipe(this.container, resource.getFluid());
        if (fillRecipe == null) return 0;
        int needed = fillRecipe.content();
        if (resource.getAmount() < needed) return 0;
        if (action.execute()) this.container = fillRecipe.fullContainer().copy();
        return needed;
    }

    @NotNull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !resource.isComponentsPatchEmpty()) return FluidStack.EMPTY;
        FluidStack contents = getContentsInternal();
        if (contents == null || !contents.isFluidEqual(resource) || resource.getAmount() < contents.getAmount()) return FluidStack.EMPTY;
        if (action.execute()) {
            FluidContainerRegistry.FluidContainer fc = FluidContainerRegistry.getFluidContainer(this.container);
            if (fc == null) return FluidStack.EMPTY;
            this.container = fc.emptyContainer() != null ? fc.emptyContainer().copy() : ItemStack.EMPTY;
        }
        return contents.copy();
    }

    @NotNull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack contents = getContentsInternal();
        if (contents == null || maxDrain < contents.getAmount()) return FluidStack.EMPTY;
        return drain(contents, action);
    }

    @NotNull
    @Override
    public ItemStack getContainer() {
        return this.container;
    }

    @Nullable
    private FluidStack getContentsInternal() {
        FluidContainerRegistry.FluidContainer fc = FluidContainerRegistry.getFluidContainer(this.container);
        if (fc == null) return null;
        Fluid vanillaFluid = fc.type().getFF();
        return vanillaFluid == null ? null : new FluidStack(vanillaFluid, fc.content());
    }
}
