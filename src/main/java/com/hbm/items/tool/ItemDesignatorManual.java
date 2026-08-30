package com.hbm.items.tool;

import com.hbm.api.item.IDesignatorItem;
import com.hbm.items.IItemControlReceiver;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemDesignatorManual} (86 lines, read in full): same
 * {@code (x, z)} storage shape as {@link ItemDesignator}, but set via a client-only, containerless
 * GUI ({@code GUIScreenDesignator}, CE) rather than a world click.
 * <p>
 * <b>GUI framework decision (per {@code docs/phase3/missile_launch_infra.md}'s Key design/API
 * decisions):</b> CE opens this via {@code FMLNetworkHandler.openGui} purely to get a client-side
 * {@code GuiScreen} instantiated with a {@code null} backing {@code Container}. The 1.21.1/NeoForge
 * equivalent is a direct client-side {@link net.minecraft.client.Minecraft#setScreen} call from
 * {@link #use}, exactly mirroring this port's own {@code ItemTurretMobFilter}/
 * {@code TurretMobFilterScreen} precedent (read in full as the confirmed real shape for this exact
 * "bare Screen, no Menu" pattern). The screen's "Save" round trip reuses this port's already-real,
 * already-registered generic {@link com.hbm.packet.toserver.ItemControlPacket}/
 * {@link IItemControlReceiver} mechanism (that packet's own javadoc names "the designator's
 * manual-target GUI" as a planned consumer) rather than inventing a new payload - no new
 * {@code HbmNetwork} wiring is needed for this item.
 */
public class ItemDesignatorManual extends Item implements IDesignatorItem, IItemControlReceiver {

    public ItemDesignatorManual(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos pos = stack.get(ToolDataComponents.DETONATOR_POS.get());
        if (pos == null) {
            tooltip.add(Component.literal("Right-click to set target coordinates").withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.literal("Target: X: " + pos.getX() + " Z: " + pos.getZ()).withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new com.hbm.inventory.gui.DesignatorManualScreen(stack));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** {@link com.hbm.inventory.gui.DesignatorManualScreen}'s "Save" button round trip. */
    @Override
    public void receiveControl(ItemStack stack, CompoundTag data) {
        if (data.contains("designatorX") && data.contains("designatorZ")) {
            stack.set(ToolDataComponents.DETONATOR_POS.get(),
                    new BlockPos(data.getInt("designatorX"), 0, data.getInt("designatorZ")));
        }
    }

    @Override
    public boolean isReady(Level world, ItemStack stack, int x, int y, int z) {
        return stack.get(ToolDataComponents.DETONATOR_POS.get()) != null;
    }

    @Override
    public Vec3 getCoords(Level world, ItemStack stack, int x, int y, int z) {
        BlockPos pos = stack.get(ToolDataComponents.DETONATOR_POS.get());
        return pos == null ? Vec3.ZERO : new Vec3(pos.getX(), 0, pos.getZ());
    }
}
