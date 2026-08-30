package com.hbm.items.machine;

import com.hbm.items.ItemBase;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;

/**
 * "Add my name to this chip" friend-or-foe biometry item. Self-contained name-list write path; a
 * future turret tile entity will read the list, but writing to it needs no tile entity reference.
 * The joined-name list is stored as one {@code \n}-separated string component (a player name may
 * not legally contain a newline, so no escaping is needed).
 */
public class ItemTurretBiometry extends ItemBase {

    public ItemTurretBiometry(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String[] names = getNames(stack);
        if (names != null) Arrays.stream(names).map(Component::literal).forEach(tooltip::add);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        addName(stack, player.getName().getString());

        if (level.isClientSide()) {
            player.sendSystemMessage(Component.translatable("chat.addpldata"));
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBleep.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.swing(hand);

        return InteractionResultHolder.success(stack);
    }

    public static String[] getNames(ItemStack stack) {
        String joined = stack.get(MachineDataComponents.TURRET_NAMES.get());
        if (joined == null || joined.isEmpty()) return null;
        return joined.split("\n");
    }

    public static void addName(ItemStack stack, String name) {
        String[] existing = getNames(stack);
        if (existing != null && Arrays.asList(existing).contains(name)) return;

        String joined = existing == null ? name : String.join("\n", existing) + "\n" + name;
        stack.set(MachineDataComponents.TURRET_NAMES.get(), joined);
    }

    public static void clearNames(ItemStack stack) {
        stack.remove(MachineDataComponents.TURRET_NAMES.get());
    }
}
