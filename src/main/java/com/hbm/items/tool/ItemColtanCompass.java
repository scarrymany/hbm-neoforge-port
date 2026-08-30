package com.hbm.items.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Points towards a coltan deposit, picked once per stack and then persisted. Ported from CE's
 * {@code com.hbm.items.tool.ItemColtanCompass}.
 *
 * <p>Two simplifications from CE, both documented rather than silently dropped:
 * <ul>
 *     <li>CE seeded its {@code Random} from {@code world.getSeed() + 5}, so every coltan compass in
 *     the same world pointed at the same single deposit. {@code Level.getSeed()} is a
 *     {@code ServerLevel}-only method whose exact 1.21.1 signature could not be confirmed against a
 *     real usage example anywhere in this tree or the Neo Edition reference (ground rule: don't
 *     invent APIs) - this port instead draws from {@link Level#getRandom()} the first time a given
 *     stack is ticked, so the target is stable for that stack forever but no longer shared
 *     world-wide across separate compasses. Cosmetic only: no coltan ore world-gen feature exists in
 *     this port yet (Phase 4 scope) for a "real" shared deposit location to matter yet.</li>
 *     <li>CE's animated compass needle ({@code IItemPropertyGetter} client-side rotation/wobble) is
 *     not reproduced - it is a pure client rendering nicety layered on top of the server-authoritative
 *     target coordinates below, which is what actually drives gameplay. The tooltip readout below is
 *     the functional replacement.</li>
 * </ul>
 */
public class ItemColtanCompass extends Item {

    public ItemColtanCompass(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide() || stack.has(ToolDataComponents.COLTAN_X.get())) {
            return;
        }

        RandomSource random = level.getRandom();
        int colX = (int) (random.nextGaussian() * 1500);
        int colZ = (int) (random.nextGaussian() * 1500);
        stack.set(ToolDataComponents.COLTAN_X.get(), colX);
        stack.set(ToolDataComponents.COLTAN_Z.get(), colZ);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Points towards the coltan deposit."));
        tooltip.add(Component.literal("The deposit is a large area where coltan ore spawns like standard ore,"));
        tooltip.add(Component.literal("it's not one large blob of ore on that exact location."));

        Integer x = stack.get(ToolDataComponents.COLTAN_X.get());
        Integer z = stack.get(ToolDataComponents.COLTAN_Z.get());
        if (x != null && z != null) {
            tooltip.add(Component.literal("Deposit near (" + x + ", " + z + ")"));
        }
    }
}
