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
 * Exact CE {@code com.hbm.items.tool.ItemDigammaDiagnostic} {@code :32-39}:
 * {@code onItemRightClick} → {@code ContaminationUtil.printDiagnosticData}. Bauble /
 * {@code playVoices} stay skipped (no Curios; {@code playVoices} is not called from this item).
 */
public class ItemDigammaDiagnostic extends Item {

    public ItemDigammaDiagnostic(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // CE ItemDigammaDiagnostic.java:32-39
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBoop.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            ContaminationUtil.printDiagnosticData(player);
        }
        return InteractionResultHolder.success(stack);
    }
}
