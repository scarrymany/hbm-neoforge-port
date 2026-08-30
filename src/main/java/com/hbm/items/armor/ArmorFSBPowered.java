package com.hbm.items.armor;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.lib.Library;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.function.Supplier;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorFSBPowered} (182 lines) - {@link ArmorFSB} +
 * {@link IBatteryItem}, an intermediate class with zero direct CE instantiations (a CE-wide grep
 * for {@code new ArmorFSBPowered(} finds none - it is extended by ~13 concrete power-armor sets,
 * none of which are this package's job, per this package's task brief item 3).
 *
 * <p>Charge is stored via {@link ArmorDataComponents#ARMOR_CHARGE} instead of CE's raw {@code
 * "charge"} NBT tag, following {@link IBatteryItem}'s own javadoc contract. Only {@link #getCharge}
 * needs overriding from {@link IBatteryItem}'s interface defaults: CE initializes an untagged
 * stack's charge to <i>full</i> ({@link #getMaxCharge}), not empty (the interface's own default
 * defaults to {@code 0L}, correct for every other current {@link IBatteryItem} implementor in this
 * port but wrong for freshly-crafted powered armor); {@code setCharge}/{@code chargeBattery}/
 * {@code dischargeBattery} are inherited unchanged since CE's own overrides of those three are
 * behaviorally identical to the interface's default clamp-and-set logic once the storage backend is
 * swapped from raw NBT to a component.
 *
 * <p>{@code isArmorEnabled(stack) = getCharge(stack) > 0} - CE's real "whole set's bonuses turn off
 * when discharged" gate, consumed by {@link ArmorFSB#hasFSBArmor}.
 *
 * <p><b>Not ported</b> (documented, not silently dropped):
 * <ul>
 *     <li>{@code Item#setDamage(ItemStack, int)} override (CE drains the battery by
 *     {@code damage * consumption} here) - CE's own call graph never actually calls
 *     {@code damageItem} on an {@code ArmorFSBPowered} piece (the real drain path is
 *     {@link #onArmorTick}'s explicit {@code dischargeBattery} call), and this port could not
 *     confirm {@code Item#setDamage(ItemStack, int)} still exists as an overridable hook in
 *     NeoForge 1.21.1 without a compiler in this sandbox - see
 *     {@code docs/phase3/fsb_armor_and_jetpacks.md} Open question #1, which flags this exact
 *     override as likely-dead-code and defers resolving it to whoever implements the concrete
 *     leaves that might call {@code hurtAndBreak} on these pieces.</li>
 *     <li>Enchantment-disabling overrides ({@code getItemEnchantability}/{@code isEnchantable}/
 *     etc.) - 1.21's data-driven enchantment system makes an item simply non-enchantable by
 *     omitting {@code Item.Properties#enchantable(...)} at construction time; no override is needed
 *     on this base class for concrete leaves to get CE's "can't be enchanted" behavior.</li>
 * </ul>
 */
public class ArmorFSBPowered extends ArmorFSB implements IBatteryItem {

    public final long maxPower;
    public final long chargeRate;
    public final long consumption;
    public final long drain;

    public ArmorFSBPowered(Holder<ArmorMaterial> material, Type type, Item.Properties properties,
                            long maxPower, long chargeRate, long consumption, long drain) {
        super(material, type, properties);
        this.maxPower = maxPower;
        this.chargeRate = chargeRate;
        this.consumption = consumption;
        this.drain = drain;
    }

    /** CE: {@code ArmorFSBPowered.getColor(long, long)}, delegating to CE's 1.12 {@code Library.getColor}
     * - this port's {@code com.hbm.lib.Library} deliberately does not carry that method forward (see
     * its own javadoc scope note), so it is reimplemented locally here, matching the exact formula
     * already re-implemented the same way by {@code com.hbm.items.tool.ItemToolAbilityPower}. */
    public static String getColor(long a, long b) {
        float fraction = 100F * a / b;
        if (fraction > 75) return "§a";
        if (fraction > 25) return "§e";
        return "§c";
    }

    @Override
    public Supplier<DataComponentType<Long>> getChargeComponent() {
        return ArmorDataComponents.ARMOR_CHARGE::get;
    }

    @Override
    public long getCharge(ItemStack stack) {
        return stack.getOrDefault(getChargeComponent().get(), getMaxCharge(stack));
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        // TODO(ItemModBattery not yet ported - one of the ~35 com.hbm.items.armor ItemMod* insert
        // leaves, a separate Phase 3 work package per docs/phase3/armor_equippable_framework.md
        // Open questions #5): CE multiplies maxPower by an installed battery-slot mod's `.mod`
        // factor here (ArmorModHandler.pryMod(stack, ArmorModHandler.battery) instanceof
        // ItemModBattery). Stubbed to the unmodified base value until that item exists.
        return maxPower;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return chargeRate;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean isArmorEnabled(ItemStack stack) {
        return getCharge(stack) > 0;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) < getMaxCharge(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getCharge(stack) / getMaxCharge(stack));
    }

    @Override
    protected void onArmorTick(Level level, Player player, ItemStack stack) {
        // CE never calls super.onArmorTick here (the geiger-tick base body is skipped entirely for
        // powered armor) - preserved exactly, not a simplification.
        if (!player.isCreative() && this.drain > 0 && ArmorFSB.hasFSBArmor(player)) {
            // TODO(ItemSelfcharger not yet ported): CE reduces the net drain by a quarter of a
            // held ItemSelfcharger's discharge rate here. That item does not exist in this port
            // yet; stubbed to the full, undiscounted drain until it does.
            this.dischargeBattery(stack, this.drain);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        components.add(Component.literal("Charge: " + Library.getShortNumber(getCharge(stack)) + " / " + Library.getShortNumber(getMaxCharge(stack))));
        super.appendHoverText(stack, context, components, flag);
    }
}
