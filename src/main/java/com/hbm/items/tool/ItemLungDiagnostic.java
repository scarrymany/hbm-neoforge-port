package com.hbm.items.tool;

import com.hbm.lib.HBMSoundHandler;
import com.hbm.util.ContaminationUtil;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Exact CE {@code com.hbm.items.tool.ItemLungDiagnostic} {@code :24-31}:
 * {@code onItemRightClick} → {@code ContaminationUtil.printLungDiagnosticData}.
 */
public class ItemLungDiagnostic extends Item {

    public ItemLungDiagnostic(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // CE ItemLungDiagnostic.java:24-31
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBoop.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            ContaminationUtil.printLungDiagnosticData(player);
        }
        return InteractionResultHolder.success(stack);
    }
}
