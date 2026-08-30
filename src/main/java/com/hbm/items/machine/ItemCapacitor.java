package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Redcoil capacitor: right-clicking a block spends one charge, striking that block with lightning
 * and a small explosion, up to a fixed maximum charge count. Needed for Schrabidium synthesis.
 */
public class ItemCapacitor extends ItemBase {

    private final int maxDura;

    public ItemCapacitor(int maxDura, Properties properties) {
        super(properties);
        this.maxDura = maxDura;
    }

    public static String getColorPrefix(long a, long b) {
        float fraction = 100F * a / b;
        if (fraction > 75) return "§a";
        if (fraction > 25) return "§e";
        return "§c";
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click a block to negate positive charge."));
        tooltip.add(Component.literal("[Needed for Schrabidium Synthesis]").withStyle(ChatFormatting.AQUA));
        int dura = getDura(stack);
        tooltip.add(Component.literal(getColorPrefix(dura, this.maxDura) + dura + " §2/ " + this.maxDura));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        if (getDura(stack) >= this.maxDura) return InteractionResult.PASS;

        setDura(stack, getDura(stack) + 1);
        Level level = context.getLevel();
        double x = context.getClickedPos().getX() + 0.5;
        double y = context.getClickedPos().getY() + 0.5;
        double z = context.getClickedPos().getZ() + 0.5;

        if (!level.isClientSide()) {
            level.explode(null, x, y, z, 2.5F, Level.ExplosionInteraction.BLOCK);
        }
        LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(x, context.getClickedPos().getY(), z);
            level.addFreshEntity(bolt);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getDura(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * (this.maxDura - getDura(stack)) / (float) this.maxDura);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00A0FF;
    }

    public static int getDura(ItemStack stack) {
        return stack.getOrDefault(MachineDataComponents.CAPACITOR_CHARGE.get(), 0);
    }

    public static void setDura(ItemStack stack, int dura) {
        stack.set(MachineDataComponents.CAPACITOR_CHARGE.get(), dura);
    }
}
