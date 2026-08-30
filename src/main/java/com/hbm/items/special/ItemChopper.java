package com.hbm.items.special;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code ItemChopper}: 4 already-distinct instances ({@code spawn_chopper},
 * {@code spawn_worm}, {@code spawn_ufo}, {@code spawn_duck}) that place a specific mob entity on
 * right-click/use-on-liquid. Per docs/phase1/items_special.md, CE's own {@code spawnCreature}
 * dispatches on {@code this == ModItems.spawn_x} identity, which the port replaces with the mob
 * factory passed directly to the constructor - equivalent behavior, no forward reference to
 * {@code ModItems} needed.
 * <p>
 * Not ported: the actual entity placement ({@code use}/{@code useOn} overrides). No entity system
 * has been ported through Phase 1 (see docs/phase1/items_special.md finding 4's sibling systems), so
 * {@code EntityHunterChopper}/{@code EntityUFO}/{@code EntityBOTPrimeHead}/{@code EntityDuck} do not
 * exist yet to spawn. Registers as a plain item for now; the flavor tooltip for {@code spawn_worm},
 * which carries no such dependency, is kept faithfully.
 */
public class ItemChopper extends Item {

    private final boolean worm;

    public ItemChopper(Properties properties, boolean worm) {
        super(properties);
        this.worm = worm;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (worm) {
            tooltip.add(Component.literal("Without a player in survival mode"));
            tooltip.add(Component.literal("to target, he struggles around a lot."));
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("He's doing his best so please show him"));
            tooltip.add(Component.literal("some consideration."));
        }
    }
}
