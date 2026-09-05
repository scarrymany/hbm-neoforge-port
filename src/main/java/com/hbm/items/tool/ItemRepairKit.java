package com.hbm.items.tool;

import com.hbm.items.ItemBase;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Exact CE {@code ItemRepairKit} + {@code ConsumableHandler.handleGunKit}
 * ({@code ConsumableHandler.java:197-230}): hotbar 0-8 + offhand, {@code maxDura * 0.25F} per
 * worn Sedna config. {@code repairFactor} is unused in CE too. Other ConsumableHandler items stay
 * on {@link com.hbm.items.special.ItemConsumable}.
 */
public class ItemRepairKit extends ItemBase {

    private final Holder<SoundEvent> sound;

    public ItemRepairKit(Properties properties, Holder<SoundEvent> sound) {
        super(properties);
        this.sound = sound;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack kit = player.getItemInHand(hand);
        boolean didSomething = false;

        for (int i = 0; i < 10; i++) {
            ItemStack item = (i == 9) ? player.getOffhandItem() : player.getInventory().items.get(i);
            if (!item.isEmpty() && item.getItem() instanceof ItemGunBaseNT gun) {
                int configs = gun.getConfigCount();
                for (int j = 0; j < configs; j++) {
                    GunConfig cfg = gun.getConfig(item, j);
                    float maxDura = cfg.getDurability(item);
                    float wear = Math.min(ItemGunBaseNT.getWear(item, j), maxDura);
                    if (wear > 0) {
                        ItemGunBaseNT.setWear(item, j, Math.max(0F, ItemGunBaseNT.getWear(item, j) - maxDura * 0.25F));
                        didSomething = true;
                    }
                }
            }
        }

        if (!didSomething) {
            return InteractionResultHolder.fail(kit);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), sound.value(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        kit.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        return InteractionResultHolder.success(kit);
    }
}
