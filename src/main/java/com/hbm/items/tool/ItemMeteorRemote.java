package com.hbm.items.tool;

import com.hbm.handler.MeteorStrikeHandler;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemMeteorRemote} (75 lines, read in full) - see
 * {@code docs/phase4/meteor_events.md}. CE's own comment calls this out as a debug/testing item
 * ("mlbv: useful for testing, please don't remove it") - no crafting recipe exists anywhere in CE,
 * confirmed by this report's own search; creative-tab-only here too.
 * <p>
 * CE's {@code stack.damageItem(1, player)} runs <b>unconditionally</b>, before the
 * {@code !world.isRemote} branch. This port mirrors that exact call order with the
 * {@code ItemStack.hurtAndBreak(int, LivingEntity, EquipmentSlot)} convenience overload already in
 * live use elsewhere in this port at the identical call shape (e.g.
 * {@code ItemGunB92}/{@code ItemGunB93}/{@code RedstoneSword}) - that overload resolves the
 * entity's own level internally and only actually applies durability loss on the server, so calling
 * it from both logical sides (as {@code Item#use} naturally is) does not double-damage the stack,
 * addressing this report's own flagged Open question.
 */
public class ItemMeteorRemote extends Item {

    public ItemMeteorRemote(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        components.add(Component.literal("Right click to summon a meteorite!"));
        super.appendHoverText(stack, context, components, flag);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));

        if (!level.isClientSide()) {
            if (player instanceof ServerPlayer serverPlayer) {
                MeteorStrikeHandler.spawnMeteorAtPlayer(serverPlayer, false);
            }
        } else {
            player.displayClientMessage(Component.literal("Watch your head!"), false);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBleep.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);

        player.swing(hand);
        return InteractionResultHolder.success(stack);
    }
}
