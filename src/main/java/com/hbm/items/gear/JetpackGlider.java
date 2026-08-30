package com.hbm.items.gear;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.handler.ArmorModHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.armor.ArmorDataComponents;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.items.armor.JetpackTankState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.gear.JetpackGlider} (125 lines) - {@code jetpack_glider}.
 * Unlike {@link JetpackRegular}/{@link JetpackBreak}/{@link JetpackBooster}/{@link JetpackVectorized},
 * this extends {@link ItemArmorMod} <b>directly</b> (not {@code com.hbm.items.armor.JetpackBase}) and
 * has <b>no {@code onArmorTick} of its own</b> in CE - confirmed by reading the full 125-line file;
 * its actual flight logic lives entirely in CE's {@code JetpackHandler} (1,075 lines: client-side
 * hover/direct-thrust input interception, a server-side per-player state map, its own sync packet,
 * and HUD/particle rendering), per {@code docs/phase3/fsb_armor_and_jetpacks.md} headline finding #5.
 *
 * <p><b>Deferred to a later package</b> (documented, not silently dropped - matches that report's
 * Deferred scope item 1 exactly): {@code JetpackHandler}'s entire flight/HUD/particle engine is
 * out of this package's scope - it is almost entirely client input handling and rendering (Phase 5
 * per this port's phase boundary), and even its server-authoritative half (a per-player drain-tank
 * tick loop keyed on {@code hasJetpack}/{@code getSpeed}/{@code getDrain}) has no caller without the
 * client half existing. What <b>is</b> ported here, fully functional today: the item shell itself -
 * {@link ArmorModHandler}-slot registration ({@code type = plate_only}), and the complete
 * {@link IFillableItem} fuel-tank contract ({@link #getTank}/{@link #setTank}/{@code tryFill}/
 * {@code tryEmpty}/{@code acceptsFluid}), so a fluid siphon/pump/canister can already fill or drain
 * this item exactly as CE intends, independent of whether {@code JetpackHandler} exists yet. Without
 * that follow-up package, wearing/inserting this item does not yet grant any flight - a real, named
 * gap, not a silent one.
 *
 * <p>Tank state is backed by {@link JetpackTankState} ({@link ArmorDataComponents#JETPACK_GLIDER_TANK})
 * rather than CE's raw {@code "fuelTank"} NBT compound - see that record's own javadoc for why this
 * is a narrow, single-consumer component rather than a general "item fluid tank" shape. CE's
 * constructor also takes an unused {@code int i} parameter (dead cruft per this area's own research
 * report's Open question #4, read but never stored) - dropped entirely here rather than carried
 * forward as a meaningless parameter.
 */
public class JetpackGlider extends ItemArmorMod implements IFillableItem {

    public final int capacity;

    public JetpackGlider(Item.Properties properties, int capacity) {
        super(properties, ArmorModHandler.plate_only, false, true, false, false);
        this.capacity = capacity;
    }

    public FluidTankNTM getTank(ItemStack stack) {
        JetpackTankState state = stack.getOrDefault(ArmorDataComponents.JETPACK_GLIDER_TANK.get(), JetpackTankState.EMPTY);
        FluidTankNTM tank = new FluidTankNTM(state.type(), capacity);
        tank.setFill(state.fill());
        return tank;
    }

    public void setTank(ItemStack stack, FluidTankNTM tank) {
        stack.set(ArmorDataComponents.JETPACK_GLIDER_TANK.get(), new JetpackTankState(tank.getTankType(), tank.getFill()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        FluidTankNTM tank = getTank(stack);

        if (tank.getTankType() == Fluids.NONE) {
            components.add(Component.literal("    Fuel Type: None").withStyle(ChatFormatting.RED));
        } else {
            components.add(Component.empty().withStyle(ChatFormatting.RED)
                    .append(Component.literal("    Fuel Type: ")).append(tank.getTankType().getLocalizedName()));
        }
        // TODO(JetpackHandler not yet ported - see class javadoc): CE also prints
        // "Fuel Speed: " + JetpackHandler.getSpeed(tank.getTankType()) here.

        int percent = tank.getCapacity() > 0 ? (int) (100F * tank.getFill() / tank.getCapacity()) : 0;
        components.add(Component.literal("    Fuel Amount: " + tank.getFill() + "/" + tank.getCapacity() + " (" + percent + "%)").withStyle(ChatFormatting.RED));

        super.appendHoverText(stack, context, components, flag);
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        super.addDesc(list, stack, armor);

        FluidTankNTM tank = getTank(stack);
        String fuelName = tank.getTankType() == Fluids.NONE ? "None" : tank.getTankType().getLocalizedName().getString();
        list.add(Component.literal("    Fuel: " + fuelName + " " + tank.getFill() + "/" + tank.getCapacity()).withStyle(ChatFormatting.RED));
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        FluidTankNTM tank = getTank(stack);

        if (tank.getTankType() == Fluids.NONE || tank.getFill() == 0) {
            return type == Fluids.KEROSENE || type == Fluids.KEROSENE_REFORM || type == Fluids.BALEFIRE || type == Fluids.NITAN;
        }
        return type == tank.getTankType();
    }

    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (stack.getCount() > 1 || !acceptsFluid(type, stack)) return amount;

        FluidTankNTM tank = getTank(stack);
        int filled = Math.min(capacity - tank.getFill(), amount);
        if (filled <= 0) return amount;

        if (tank.getFill() == 0) {
            tank.setTankType(type);
        }
        tank.setFill(tank.getFill() + filled);
        setTank(stack, tank);
        return amount - filled;
    }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        return getTank(stack).getTankType() == type;
    }

    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        if (stack.getCount() > 1) return 0;

        FluidTankNTM tank = getTank(stack);
        if (tank.getTankType() != type) return 0;

        int drained = Math.min(tank.getFill(), amount);
        if (drained <= 0) return 0;

        tank.setFill(tank.getFill() - drained);
        if (tank.getFill() == 0) tank.setTankType(Fluids.NONE);
        setTank(stack, tank);
        return drained;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        FluidType type = getTank(stack).getTankType();
        return type == Fluids.NONE ? null : type;
    }

    @Override
    public int getFill(ItemStack stack) {
        return getTank(stack).getFill();
    }
}
