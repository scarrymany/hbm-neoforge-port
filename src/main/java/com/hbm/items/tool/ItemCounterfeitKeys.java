package com.hbm.items.tool;

import com.hbm.api.block.ILockable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.function.Supplier;

/**
 * Exact CE {@code ItemCounterfeitKeys} {@code :30-66}: cheesable lock → replace held tool with
 * pin-key then add a second copy (drop if full). {@code !cheesable} → two chat lines.
 */
public class ItemCounterfeitKeys extends Item {

    private final Supplier<? extends Item> fakeKeySupplier;

    public ItemCounterfeitKeys(Properties properties, Supplier<? extends Item> fakeKeySupplier) {
        super(properties);
        this.fakeKeySupplier = fakeKeySupplier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockEntity te = level.getBlockEntity(context.getClickedPos());

        if (!(te instanceof ILockable lockable)) return InteractionResult.PASS;
        if (level.isClientSide || player == null) return InteractionResult.SUCCESS;

        // CE ItemCounterfeitKeys.java:37-56
        if (lockable.isLocked() && lockable.isCheesable()) {
            ItemStack fake = new ItemStack(fakeKeySupplier.get());
            ItemKeyPin.setPins(fake, lockable.getPins());

            player.setItemInHand(context.getHand(), fake.copy());
            if (!player.getInventory().add(fake.copy())) {
                player.drop(fake.copy(), false);
            }
            player.swing(context.getHand());
            return InteractionResult.SUCCESS;
        }

        if (!lockable.isCheesable()) {
            player.displayClientMessage(Component.literal("This lock is too elaborate for a counterfeit key to be made").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            player.displayClientMessage(Component.literal("Perhaps there is another way around here to unlock it").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Use on a locked container to create two counterfeit keys!"));
    }
}
