package com.hbm.items.armor;

import com.hbm.damage.ModDamageTypes;
import com.hbm.items.gear.ArmorFSB;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorBJ} (125 lines) - the "Blackjack" power-armor
 * set. Beyond client-model/renderer plumbing (Phase 5), CE's one non-rendering mechanic is a
 * helmet-only failsafe: if the set is worn as a full material match but not fully powered (CE:
 * {@code hasFSBArmorIgnoreCharge && !hasFSBArmor} - i.e. at least one piece has run out of charge),
 * the helmet is forcibly ejected and the wearer takes 1000 lunar-vacuum damage (CE:
 * {@code ModDamageSource.lunar}, this port's {@link ModDamageTypes#LUNAR}). Gated on
 * {@link Type#HELMET} rather than CE's {@code this == ModItems.bj_helmet} identity check, since
 * every slot of this set shares one class in this port.
 */
public class ArmorBJ extends ArmorFSBPowered {

    public ArmorBJ(Holder<ArmorMaterial> material, Type type, Item.Properties properties,
                    long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties, maxPower, chargeRate, consumption, drain);
    }

    @Override
    protected void onArmorTick(Level level, Player player, ItemStack stack) {
        super.onArmorTick(level, player, stack);

        if (this.getType() != Type.HELMET) return;
        if (!ArmorFSB.hasFSBArmorIgnoreCharge(player) || ArmorFSB.hasFSBArmor(player)) return;

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);

        if (!player.getInventory().add(helmet)) {
            player.drop(helmet, false);
        }

        player.hurt(player.damageSources().source(ModDamageTypes.LUNAR), 1000F);
    }
}
