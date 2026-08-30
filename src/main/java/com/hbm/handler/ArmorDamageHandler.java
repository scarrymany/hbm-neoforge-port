package com.hbm.handler;

import com.hbm.capability.HbmPlayerAttachment;
import com.hbm.items.armor.IAttackHandler;
import com.hbm.items.armor.IDamageHandler;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.items.gear.ArmorEuphemium;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.main.MainRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Central armor damage-reduction dispatch, ported from CE's {@code com.hbm.main.ModEventHandler}
 * {@code #onEntityHurt(LivingHurtEvent)}/{@code #onEntityAttacked(LivingAttackEvent)}. Per this
 * package's task brief item 6, {@code ISpecialArmor} is <b>not</b> the 1.21.1 replacement here (CE
 * only ever used it for 3 near-total-immunity edge cases, none of which are this package's job -
 * see {@code docs/phase3/armor_equippable_framework.md} headline finding #1); this class is that
 * replacement, as a single self-registering {@code @EventBusSubscriber} (no shared aggregator file
 * to edit, same pattern as {@code handler.neutron.NeutronHandler}).
 *
 * <p>Three dispatch points, confirmed real 1.21.1 events (see the class-level decisions in
 * {@code docs/phase3/armor_equippable_framework.md} Key design decision #2):
 * <ul>
 *     <li>{@link LivingIncomingDamageEvent} - the earliest, fully-cancelable point in the pipeline;
 *     the correct replacement for {@code ISpecialArmor}'s "override the entire damage pipeline"
 *     role. Dispatches to {@link ArmorEuphemium#isFullSetWorn} (the only one of CE's 3
 *     {@code ISpecialArmor} full-immunity sets that is still live - {@code ArmorAsbestos}/
 *     {@code ArmorSchrabidium} are dead code in current CE, migrated onto plain {@code ArmorFSB}
 *     with no damage-absorption mechanism at all - see {@code docs/phase3/armor_special_sets.md}
 *     Headline finding #1).</li>
 *     <li>{@link LivingDamageEvent.Pre} - shield absorb (CE: {@code ModEventHandler#onEntityHurt}'s
 *     first block), armor-mod {@code ItemArmorMod#modDamage} iteration, {@link ArmorFSB#handleHurt},
 *     and {@link IDamageHandler} iteration, in CE's exact order.</li>
 *     <li>{@link LivingIncomingDamageEvent} again (a second, separate listener method below) -
 *     {@link ArmorFSB#handleAttack} and {@link IAttackHandler} iteration. CE's {@code LivingAttackEvent}
 *     does <b>not</b> exist under that name anywhere in real NeoForge 1.21.1 - a prior pass here
 *     incorrectly assumed it "survives unchanged"; confirmed by a source search of the real
 *     {@code neoforged/NeoForge} repository turning up zero matches for {@code LivingAttackEvent} in
 *     the whole org. {@code LivingIncomingDamageEvent} is its actual, confirmed-real successor - it
 *     already fires early enough (before any damage-reduction math) to reproduce CE's
 *     "cancel the attack outright" semantics, and is fully cancelable via {@code setCanceled(boolean)}
 *     exactly like the old event.</li>
 * </ul>
 *
 * <p><b>Now independently confirmed against real NeoForge source</b> (via a targeted GitHub code
 * search of {@code neoforged/NeoForge}, not decompilation, since this sandbox has no reachable
 * NeoForge library jar): {@link LivingDamageEvent.Pre#getNewDamage()}/{@code #setNewDamage(float)}
 * are real, exactly as used below - confirmed by
 * {@code neoforged/NeoForge}'s own {@code LivingDamageEvent.java} (the {@code Pre} inner class
 * delegates both to its {@code DamageContainer}) and {@code CommonHooks#onLivingDamagePre}, which
 * fires {@code new LivingDamageEvent.Pre(entity, container)} and reads back {@code .getNewDamage()}
 * exactly as this class does. {@link LivingIncomingDamageEvent#getOriginalAmount()}/
 * {@code #getEntity()}/{@code #setCanceled(boolean)} are likewise confirmed real via the same search
 * (and already exercised by Neo Edition's {@code DamageResistanceHandler.onEntityAttacked}, which
 * this class's {@code onIncomingDamage}/{@code onAttack} methods below mirror the shape of).
 */
@EventBusSubscriber(modid = MainRegistry.MODID)
public final class ArmorDamageHandler {

    private ArmorDamageHandler() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        // ArmorEuphemium (com.hbm.items.gear.SpecialArmorItems): CE's ArmorEuphemium#getProperties
        // full-set-gated, EntityPlayer-only unconditional damage immunity - see that class's javadoc.
        // Asbestos/Schrabidium's own ISpecialArmor immunity is NOT reproduced here: real, current CE
        // no longer builds those 2 sets from ISpecialArmor-implementing leaf classes at all (both are
        // dead code, migrated onto plain ArmorFSB with zero damage-absorption mechanism - see
        // docs/phase3/armor_special_sets.md Headline finding #1 and SpecialArmorItems' own javadoc),
        // so there is nothing left to cancel for either set.
        if (ArmorEuphemium.isFullSetWorn(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Shield absorb (CE: ModEventHandler#onEntityHurt) - HbmPlayerAttachment is this port's
        // already-ported HbmCapability/IHBMData equivalent.
        HbmPlayerAttachment data = HbmPlayerAttachment.getData(player);
        float shield = data.getShield();
        if (shield > 0) {
            float amount = event.getNewDamage();
            float reduce = Math.min(shield, amount);
            data.setShield(shield - reduce);
            event.setNewDamage(amount - reduce);
        }
        data.setLastDamage(player.tickCount);

        // Armor-mod modDamage iteration - all 4 real armor slots (CE: EnumUtil.
        // ENTITY_EQUIPMENT_SLOTS[2..5], i.e. every slot EquipmentSlot#isArmor() reports true for).
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) continue;
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.isEmpty() || !ArmorModHandler.hasMods(armor)) continue;

            for (ItemStack mod : ArmorModHandler.pryMods(armor)) {
                if (!mod.isEmpty() && mod.getItem() instanceof ItemArmorMod armorMod) {
                    armorMod.modDamage(event, armor);
                }
            }
        }

        // FSB full-set-bonus hurt hook.
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof ArmorFSB fsb) {
            fsb.handleHurt(event);
        }

        // IDamageHandler iteration across all 4 armor slots.
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) continue;
            ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.getItem() instanceof IDamageHandler handler) {
                handler.handleDamage(event, armor);
            }
        }
    }

    @SubscribeEvent
    public static void onAttack(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.getItem() instanceof ArmorFSB fsb) {
            fsb.handleAttack(event);
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) continue;
            ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.getItem() instanceof IAttackHandler handler) {
                handler.handleAttack(event, armor);
            }
        }
    }
}
