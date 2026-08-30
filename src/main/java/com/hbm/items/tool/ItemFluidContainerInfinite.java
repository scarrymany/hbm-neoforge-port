package com.hbm.items.tool;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.ItemBase;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Marker item declaring "this stack is an infinite/renewable source of a fixed fluid amount" -
 * consulted by whichever consumer reads {@link #getType()}/{@link #getAmount()}/{@link #getChance()}
 * (a pump/well/pinwheel-style block, per CE's usage). Ported from CE's
 * {@code com.hbm.items.tool.ItemFluidContainerInfinite} ({@code fluid_barrel_infinite},
 * {@code inf_water}, {@code inf_water_mk2}, {@code chlorine_pinwheel}).
 *
 * <p>Unlike {@link ItemCanister}/{@link ItemGasCanister}, this item carries no per-stack fill state
 * at all - CE's own class does not implement any fill/drain interface either, it is a plain
 * constant-configuration source definition read by external code. No consumer of these fields exists
 * in this port yet (the pump/well block(s) that would read them are Phase 2 machine content), so
 * this class is registered ready for that future consumer, exactly mirroring
 * {@link ItemCraftingDegradation}'s "ported ahead of its consumer" precedent in this same package.
 */
public class ItemFluidContainerInfinite extends ItemBase {

    @Nullable
    private final FluidType type;
    private final int amount;
    private final int chance;
    private final boolean unlimitedPressure;

    public ItemFluidContainerInfinite(Properties properties, @Nullable FluidType type, int amount) {
        this(properties, type, amount, 1, false);
    }

    public ItemFluidContainerInfinite(Properties properties, @Nullable FluidType type, int amount, int chance, boolean unlimitedPressure) {
        super(properties);
        this.type = type;
        this.amount = amount;
        this.chance = chance;
        this.unlimitedPressure = unlimitedPressure;
    }

    @Nullable
    public FluidType getType() {
        return this.type;
    }

    public int getAmount() {
        return this.amount;
    }

    public int getChance() {
        return this.chance;
    }

    /** CE: {@code this == ModItems.fluid_barrel_infinite || pressure == 0}. */
    public boolean allowPressure(int pressure) {
        return this.unlimitedPressure || pressure == 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.literal("Provides " + (this.amount * 0.02F) + " mB/t of "
                + (this.type != null ? this.type.getLocalizedName().getString() : "any adjacent fluid") + "."));
    }
}
