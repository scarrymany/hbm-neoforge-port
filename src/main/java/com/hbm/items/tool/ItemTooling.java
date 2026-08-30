package com.hbm.items.tool;

import com.hbm.api.block.IToolable;
import com.hbm.api.block.IToolable.ToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Machine-part screwdriver/hand-drill family, ported from CE's {@code com.hbm.items.tool.ItemTooling}
 * (the {@code screwdriver}/{@code screwdriver_desh}/{@code hand_drill}/{@code hand_drill_desh}
 * registry names, per {@code docs/phase2/items_tool_machine_coupling_and_recipe_system.md}'s Part
 * A.1 table). Dispatches to {@link IToolable#onScrew}, exactly like CE - {@code IToolable} is
 * already ported in this port ({@code com.hbm.api.block.IToolable}, correct {@code ToolType}/
 * {@code onScrew} shape) and compiles today now that {@code com.hbm.inventory.RecipesCommon} (the
 * class backing {@code ToolType.getType}) exists, so no interface work is needed here - only the
 * item itself, which never existed anywhere in this port before this pass.
 * <p>
 * Every concrete machine block's own {@code onScrew} body is that block's own package's job (this
 * class only needs to exist and dispatch); as of this pass several already do
 * ({@code BlockConveyorBase implements IToolable}, confirmed by grep), most others do not yet, and
 * the dispatch call is a no-op {@link InteractionResult#PASS} against any block that isn't
 * {@code IToolable} - not a defect specific to this item.
 */
public class ItemTooling extends Item {

    protected final ToolType type;

    public ItemTooling(ToolType type, Properties properties) {
        super(properties);
        this.type = type;
        // CE: `type.register(new ItemStack(this))` in the constructor - feeds ToolType's own
        // stack -> type lookup map (used by ItemTooling.getType(stack) callers such as a future
        // lock-picking tool check), lazily rebuilt on first IToolable.ToolType.getType() call.
        type.register(new ItemStack(this));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        Block block = context.getLevel().getBlockState(context.getClickedPos()).getBlock();
        if (block instanceof IToolable toolable) {
            ItemStack stack = context.getItemInHand();
            InteractionHand hand = context.getHand();
            BlockPos pos = context.getClickedPos();
            float fX = (float) (context.getClickLocation().x - pos.getX());
            float fY = (float) (context.getClickLocation().y - pos.getY());
            float fZ = (float) (context.getClickLocation().z - pos.getZ());
            boolean handled = toolable.onScrew(context.getLevel(), player, pos, context.getClickedFace(), fX, fY, fZ, hand, this.type);

            if (handled) {
                if (stack.getMaxDamage() > 0) {
                    stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
                }
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (type == ToolType.SCREWDRIVER) {
            tooltip.add(Component.translatable("desc.screwdriver1"));
        } else if (type == ToolType.HAND_DRILL) {
            tooltip.add(Component.translatable("desc.hand_drill1"));
        }
    }

    public ToolType getType() {
        return type;
    }
}
