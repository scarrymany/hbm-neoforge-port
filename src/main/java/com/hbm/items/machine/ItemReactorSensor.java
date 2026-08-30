package com.hbm.items.machine;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * Classic pile-reactor position marker, ported from CE's
 * {@code com.hbm.items.machine.ItemReactorSensor}.
 * <p>
 * <b>Stubbed pending {@code ModBlocks.reactor_research} and the whole "classic pile reactor"
 * system.</b> Confirmed via repo-wide grep (this port has no {@code reactor_research}/
 * {@code PileSource}/{@code PileVent}/{@code ReactorResearch} content at all) and by
 * {@code docs/phase2/reactors_breeding_pwr.md}'s own independent finding while researching an
 * unrelated PWR method: "none yet covered by any Phase 2 research package listed in docs/phase2/",
 * recommending "a dedicated classic pile reactor Phase 2 research package (parallel in scope to
 * [PWR], RBMK, and the turbine family)." Per the port plan's "stub with a documented TODO rather
 * than blocking" rule, the item is registered (tooltip included) with its use-behavior left a no-op
 * {@link InteractionResult#PASS}.
 */
public class ItemReactorSensor extends Item {

    public ItemReactorSensor(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("No reactor selected!"));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // TODO(cross-area follow-up): once ModBlocks.reactor_research (the classic pile reactor
        // system) exists, port CE's position-marking behavior here.
        return InteractionResult.PASS;
    }
}
