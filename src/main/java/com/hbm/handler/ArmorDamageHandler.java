package com.hbm.handler;

import com.hbm.capability.HbmPlayerAttachment;
import com.hbm.items.armor.IAttackHandler;
import com.hbm.items.armor.IDamageHandler;
import com.hbm.items.armor.ItemArmorMod;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.main.MainRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingAttackEvent;
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
 *     role. No concrete full-immunity set ({@code ArmorAsbestos}/{@code ArmorEuphemium}/
 *     {@code ArmorSchrabidium}-equivalent) exists in this port yet - those are a later Phase 3
 *     content package's job - so this listener currently has nothing to cancel; it exists now so
 *     that package's dispatch point is already in place.</li>
 *     <li>{@link LivingDamageEvent.Pre} - shield absorb (CE: {@code ModEventHandler#onEntityHurt}'s
 *     first block), armor-mod {@code ItemArmorMod#modDamage} iteration, {@link ArmorFSB#handleHurt},
 *     and {@link IDamageHandler} iteration, in CE's exact order.</li>
 *     <li>{@link LivingAttackEvent} (unchanged in NeoForge 1.21, confirmed) -
 *     {@link ArmorFSB#handleAttack} and {@link IAttackHandler} iteration.</li>
 * </ul>
 *
 * <p><b>Not independently verified against a compiler in this sandbox</b> (no NeoForge library jar
 * was reachable to decompile, and neither reference tree contains a real call site beyond an
 * empty-bodied stub): {@link LivingDamageEvent.Pre}'s exact accessor names
 * ({@code getNewDamage()}/{@code setNewDamage(float)}) are this port's best-confidence reading of
 * the real NeoForge 1.21.x {@code DamageContainer}-backed API (distinguishing "damage" naming on
 * the {@code Pre}/{@code Post} events from "amount" naming on the older-lineage
 * {@link LivingIncomingDamageEvent}/{@link LivingAttackEvent}), not a source- or bytecode-confirmed
 * fact. Flagged for a targeted compile-check follow-up.
 */
@EventBusSubscriber(modid = MainRegistry.MODID)
public final class ArmorDamageHandler {

    private ArmorDamageHandler() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        // Forward reference: CE's 3 ISpecialArmor full-immunity pieces (ArmorAsbestos/
        // ArmorEuphemium/ArmorSchrabidium) belong to a later Phase 3 content package (see
        // docs/phase3/armor_equippable_framework.md's Phase-3-safe-scope table). Once ported,
        // their full-cancel checks (event.setCanceled(true), mirroring CE's
        // ArmorUtil.checkArmor(...) + event.setCanceled(true) shape) belong here, at the earliest
        // possible point in the damage pipeline - not on LivingDamageEvent.Pre below, which fires
        // after vanilla's own defense-point math.
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
    public static void onAttack(LivingAttackEvent event) {
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
