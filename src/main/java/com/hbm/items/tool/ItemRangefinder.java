package com.hbm.items.tool;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemRangefinder} (63 lines, read in full) - plain
 * {@link Item} (not an {@link ItemCoordinateBase}): right-click ray-traces up to 200 blocks and
 * reports the hit distance to the player.
 * <p>
 * CE's {@code World#rayTraceBlocks} is replaced by {@link Level#clip(ClipContext)} with
 * {@link ClipContext.Block#COLLIDER}/{@link ClipContext.Fluid#NONE} - the confirmed real 1.21.1
 * replacement already used this exact way by {@code IToolAreaAbility}'s own raytrace helper.
 * <p>
 * CE sent the distance readout via a one-shot {@code PlayerInformPacket} toclient payload; no
 * equivalent toclient "inform" packet exists anywhere in this port yet (confirmed by directory
 * listing of {@code com.hbm.packet.toclient}), and this single-item, single-message need does not
 * justify building one (the report flagged this as a five-minute check, not a real blocker). This
 * uses the same plain {@code player.displayClientMessage(...)} idiom this port's own
 * {@link ItemDetonator}/{@link ItemMultiDetonator} already use for identical one-off server-to-player
 * chat feedback - functionally identical to CE's toast, minus the auto-expiring HUD placement.
 * <p>
 * {@code META_POLARIZED} (CE damage value 1, a light-purple tint on both the message and the display
 * name with no other behavior difference and no confirmed crafting/loot reference anywhere in CE)
 * is flattened into a second registry entry ({@code hbm:rangefinder_polarized}) per this port's
 * metadata-flattening convention, carried here as a constructor-time {@code boolean} rather than a
 * runtime damage-value check.
 */
public class ItemRangefinder extends Item {

    private static final double RANGE = 200D;

    private final boolean polarized;

    public ItemRangefinder(Properties properties, boolean polarized) {
        super(properties);
        this.polarized = polarized;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            Vec3 start = player.getEyePosition(1.0F);
            Vec3 end = start.add(player.getViewVector(1.0F).scale(RANGE));
            BlockHitResult result = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

            if (result.getType() == HitResult.Type.BLOCK) {
                double dist = start.distanceTo(result.getLocation());
                String msg = ((int) (dist * 10D)) / 10D + "m";

                Component component = polarized
                        ? Component.literal(msg).withStyle(ChatFormatting.LIGHT_PURPLE)
                        : Component.literal(msg);
                player.displayClientMessage(component, true);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = super.getName(stack);
        return polarized ? name.copy().withStyle(ChatFormatting.LIGHT_PURPLE) : name;
    }
}
