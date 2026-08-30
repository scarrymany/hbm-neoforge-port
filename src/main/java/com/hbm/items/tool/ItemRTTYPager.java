package com.hbm.items.tool;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.IItemControlReceiver;
import com.hbm.blockentity.network.RTTYSystem;
import com.hbm.blockentity.network.RTTYSystem.RTTYChannel;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemRTTYPager} (112 lines, read in full). Implements
 * this port's already-real {@link IItemControlReceiver} (unchanged interface, one existing
 * implementer, {@link ItemToolAbility}) to receive its channel name from a future
 * channel-selection GUI via {@code com.hbm.packet.toserver.ItemControlPacket}.
 * <p>
 * Every tick, polls {@link RTTYSystem#listen} for its stored channel; the literal signal
 * {@code "selfdestruct"} fires a real {@link ExplosionVNT} (radius 5, piercing
 * {@link EntityProcessorCrossSmooth}, {@link ExplosionEffectWeapon} SFX) centered on the holding
 * entity and consumes the pager - the one piece of real weapon behavior in this class. Any other
 * signal is reported as a plain chat message (see {@link #inventoryTick} javadoc for why this isn't
 * CE's auto-expiring HUD toast).
 * <p>
 * <b>Deferred</b>: CE's {@code IGUIProvider}/{@code GUIScreenPager} right-click GUI (a bare 1.12
 * {@code GuiScreen} with no server-side {@code Container}) is not ported - per the research report's
 * own open question, whether this needs a real {@code AbstractContainerMenu} at all (vs. a menu-less
 * item-driven client screen) is an open call this port's {@code MenuBase}/{@code GuiInfoContainer}
 * framework doesn't yet address, and building a throwaway GUI just to pick a channel string is out
 * of this package's scope. {@link #receiveControl} (the server-side write path) and the whole
 * polling/selfdestruct/tooltip surface are fully real; only the "pick a channel by right-clicking"
 * convenience UI is stubbed - the channel can already be set by any future caller of
 * {@link #receiveControl} or directly via {@link ToolDataComponents#PAGER_CHANNEL}.
 */
public class ItemRTTYPager extends Item implements IItemControlReceiver {

    public ItemRTTYPager(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        String channelFreq = stack.get(ToolDataComponents.PAGER_CHANNEL.get());
        if (channelFreq == null || channelFreq.isEmpty()) return;
        if (!(entity instanceof ServerPlayer) || level.isClientSide()) return;

        RTTYChannel chan = RTTYSystem.listen(level, channelFreq);
        if (chan == null || chan.timeStamp < level.getGameTime() - 1) return;

        if ("selfdestruct".equals(String.valueOf(chan.signal))) {
            ExplosionVNT vnt = new ExplosionVNT(level, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 5, null);
            vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 50).setupPiercing(5F, 0.5F));
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
            vnt.explode();
            stack.shrink(1);
            return;
        }

        // CE reports this via a self-expiring HUD toast (PlayerInformPacketLegacy); no toclient
        // "inform" packet exists in this port yet (see class javadoc) - a plain chat message is
        // this port's already-established substitute for the same one-off feedback (ItemDetonator/
        // ItemMultiDetonator/ItemRangefinder all use the same idiom).
        int alive = entity.tickCount % 1000;
        Component message = Component.literal("[ " + channelFreq + " (" + alive + ") ] ")
                .withStyle(ChatFormatting.GOLD)
                .append(Component.literal(String.valueOf(chan.signal)).withStyle(ChatFormatting.YELLOW));
        ((ServerPlayer) entity).displayClientMessage(message, true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // TODO(pager channel-select GUI, see class javadoc): CE opens GUIScreenPager here. No
        // menu-less item-driven client screen exists yet in this port's Menu/Screen framework -
        // stubbed until that decision is made; the channel can still be set via receiveControl.
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        String channel = stack.get(ToolDataComponents.PAGER_CHANNEL.get());
        if (channel == null || channel.isEmpty()) {
            list.add(Component.literal("No channel set!").withStyle(ChatFormatting.RED));
        } else {
            list.add(Component.literal("Channel: " + channel).withStyle(ChatFormatting.YELLOW));
        }
    }

    @Override
    public void receiveControl(ItemStack stack, CompoundTag data) {
        if (data.contains("chan")) {
            stack.set(ToolDataComponents.PAGER_CHANNEL.get(), data.getString("chan"));
        }
    }
}
