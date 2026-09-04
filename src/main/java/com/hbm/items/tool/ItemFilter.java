package com.hbm.items.tool;

import com.hbm.api.item.IGasMask;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Exact CE {@code ItemFilter} ({@code ItemFilter.java:28-81}): right-click screws the held filter
 * onto a worn {@link IGasMask} helmet, or the helmet-only armor-mod if the helmet itself is not a
 * mask. Swap ejects the previous filter.
 */
public class ItemFilter extends Item {

    public ItemFilter(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack stack = player.getItemInHand(hand);
        if (helmet.isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!(helmet.getItem() instanceof IGasMask)) {
            if (ArmorModHandler.hasMods(helmet)) {
                ItemStack[] mods = ArmorModHandler.pryMods(helmet);

                if (!mods[ArmorModHandler.helmet_only].isEmpty()) {
                    ItemStack mask = mods[ArmorModHandler.helmet_only];

                    ItemStack ret = installFilterOn(mask, stack, level, player);
                    ArmorModHandler.applyMod(helmet, mask);
                    return InteractionResultHolder.success(ret);
                }
            }
        }

        return InteractionResultHolder.success(installFilterOn(helmet, stack, level, player));
    }

    private ItemStack installFilterOn(ItemStack helmet, ItemStack filter, Level level, Player player) {
        if (!(helmet.getItem() instanceof IGasMask mask)) {
            return filter;
        }
        if (!mask.isFilterApplicable(helmet, filter)) {
            return filter;
        }

        ItemStack copy = filter.copy();
        copy.setCount(1);
        ItemStack current = ArmorUtil.getGasMaskFilter(helmet);

        filter.shrink(1);
        if (filter.isEmpty()) {
            filter = current;
        } else if (!current.isEmpty()) {
            if (!player.getInventory().add(current)) {
                player.drop(current, true);
            }
        }

        ArmorUtil.installGasMaskFilter(helmet, copy);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                HBMSoundHandler.gasmaskScrew.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

        return filter;
    }
}
