package com.hbm.items.armor;

import com.hbm.capability.HbmPlayerAttachment;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorTrenchmaster} (155 lines) - the Trenchmaster
 * set. Beyond client-model/renderer plumbing (Phase 5), CE's non-rendering behavior, all ported:
 * <ul>
 *     <li>friendly-fire explosion immunity - CE zeroes damage from a player-sourced explosion while
 *     the full set is worn (CE: {@code event.getSource().getTrueSource() instanceof EntityPlayer});</li>
 *     <li>a 1-in-3 chance to dodge (cancel) any attack outright while the full set is worn.</li>
 * </ul>
 * <b>Not ported</b> (documented, not silently dropped): CE's {@code isTrenchMaster}/{@code hasAoS}
 * static helpers reference {@code ModItems.card_aos}, one of the ~35 {@code ItemMod*} armor-insert
 * leaves not yet in this port (per {@code docs/phase3/armor_equippable_framework.md} Open
 * questions #5) - no other class in this package calls either helper, so they are omitted rather
 * than stubbed to a guessed return value; a future package porting {@code ItemModCard}/
 * {@code card_aos} should re-add them.
 */
public class ArmorTrenchmaster extends ArmorFSB {

    public ArmorTrenchmaster(Holder<ArmorMaterial> material, Type type, Item.Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        super.appendHoverText(stack, context, components, flag);
        components.add(Component.literal("§c  " + I18nUtil.resolveKey("armor.moreAmmo")));
    }

    @Override
    public void handleHurt(LivingDamageEvent.Pre event) {
        super.handleHurt(event);
        if (event.getEntity() instanceof Player player && ArmorFSB.hasFSBArmor(player)) {
            if (event.getSource().is(DamageTypeTags.IS_EXPLOSION) && event.getSource().getEntity() instanceof Player) {
                event.setNewDamage(0F);
            }
        }
    }

    @Override
    public void handleAttack(LivingIncomingDamageEvent event) {
        super.handleAttack(event);
        LivingEntity entity = event.getEntity();

        if (entity instanceof Player player && ArmorFSB.hasFSBArmor(player)) {
            if (entity.getRandom().nextInt(3) == 0) {
                SoundEvent breakSound = SoundEvents.ITEM_BREAK;
                HbmPlayerAttachment.plink(player, breakSound, 0.5F, 1.0F + entity.getRandom().nextFloat() * 0.5F);
                event.setCanceled(true);
            }
        }
    }
}
