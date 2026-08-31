package com.hbm.entity.mob;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * CE: {@code com.hbm.entity.mob.EntityDummy} (63 lines) — armor-stand dummy; name is current HP.
 */
public class EntityDummy extends PathfinderMob {

    public EntityDummy(EntityType<? extends EntityDummy> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(player, hand);
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem armor) {
            if (!this.level().isClientSide) {
                EquipmentSlot slot = armor.getEquipmentSlot();
                ItemStack current = this.getItemBySlot(slot);
                if (!current.isEmpty()) {
                    if (!player.addItem(current.copy())) {
                        this.spawnAtLocation(current.copy());
                    }
                }
                ItemStack equip = stack.copy();
                equip.setCount(1);
                this.setItemSlot(slot, equip);
                this.playSound(SoundEvents.ARMOR_EQUIP_GENERIC.value(), 1.0F, 1.0F);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isCustomNameVisible() {
        return true;
    }

    @Override
    public Component getName() {
        float hp = ((int) (this.getHealth() * 10)) / 10.0F;
        float max = ((int) (this.getMaxHealth() * 10)) / 10.0F;
        return Component.literal(hp + " / " + max);
    }

    @Override
    protected void dropEquipment() {
    }
}
