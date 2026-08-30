package com.hbm.items.armor;

import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.armor.JetpackBase} (133 lines) - the shared base every
 * standard-tier jetpack ({@code Jetpack{Regular,Break,Booster,Vectorized}}, {@code com.hbm.items.gear})
 * extends. {@code JetpackGlider} does <b>not</b> extend this class in CE (it extends
 * {@code ItemArmorMod} directly) - see {@code docs/phase3/fsb_armor_and_jetpacks.md} headline
 * finding #4/#5 and that class's own javadoc.
 *
 * <p><b>Dual-mode delivery</b> (CE's real, load-bearing design, not simplified away - see that
 * report's Key design decision #2): a jetpack works both (a) worn directly in the chest slot, and
 * (b) inserted as a mod into slot {@link ArmorModHandler#plate_only} of an <i>unrelated</i>
 * chestplate ({@link ArmorBJJetpack} is one concrete example of a non-jetpack chestplate that
 * delegates its own flight tick into whatever jetpack occupies that slot). Both paths funnel into
 * the exact same {@link #onArmorTick} a leaf overrides:
 * <ul>
 *     <li>{@link #modUpdate} - CE's mod-slot delivery path, called by {@code ArmorDamageHandler}'s
 *     sibling mod-slot-iteration dispatch wherever an armor piece's {@link ArmorModHandler#plate_only}
 *     slot is checked (mirrors {@code ItemArmorMod}'s existing per-tick contract).</li>
 *     <li>{@link #inventoryTick} - this port's confirmed {@code Item#onArmorTick} replacement (see
 *     {@link com.hbm.items.gear.ArmorFSB}'s identical pattern) - fires when this exact stack sits in
 *     the wearer's own {@link EquipmentSlot#CHEST} slot, i.e. standalone wear.</li>
 * </ul>
 *
 * <p>Fuel is stored via {@link ArmorDataComponents#JETPACK_FUEL} (int, CE's raw {@code "fuel"} NBT
 * tag) rather than raw NBT - untagged defaults to {@code 0} (CE: {@code getFuel} initializes an
 * untagged stack's compound and returns {@code 0}, i.e. jetpacks are crafted empty, unlike
 * {@code ArmorFSBFueled}'s "defaults to full" convention).
 *
 * <p><b>Not ported</b> (documented, not silently dropped, per this package's task brief):
 * <ul>
 *     <li><b>Client-side rendering</b> ({@code modRender}/{@code getArmorModel}, CE's custom
 *     {@code ModelJetPack}) - Phase 5, matching every other armor item's model/render hook in this
 *     port (see {@code ArmorGasMask}'s identical scope note); this port's own {@code ItemArmorMod}
 *     does not yet declare a client-render mod-slot hook at all, so there is nothing to stub here.</li>
 *     <li><b>Standalone chest-slot equipping via normal player actions</b> - CE achieves this on a
 *     plain (non-{@code ItemArmor}) {@code Item} via Forge 1.12's {@code isValidArmor}/
 *     {@code getArmorModel} hooks, which have no confirmed NeoForge 1.21.1 replacement verified in
 *     this sandbox (no compiler or decompiled source was reachable to confirm the exact
 *     {@code DataComponents.EQUIPPABLE}/{@code Equippable} builder API surface at this specific
 *     NeoForge version - every other "make a plain Item wearable" case already ported in this repo,
 *     e.g. {@code ArmorGasMask}, sidestepped the question entirely by extending {@code ArmorItem}
 *     instead, which is not available to this class since {@code ArmorModHandler.applyMod}/
 *     {@code isApplicable} require the mod-slot-insert half of dual-mode delivery to be
 *     {@code instanceof ItemArmorMod} - a real Java single-inheritance conflict with also extending
 *     {@code ArmorItem}). <b>{@link #onArmorTick} and {@link #inventoryTick} do not depend on how the
 *     stack got into the chest slot</b> - once a real {@code Equippable} (or equivalent) component is
 *     confirmed and added to this item's {@code Properties} at the registration site, standalone wear
 *     works immediately with no further change here. Until then, the mod-slot-insert delivery mode
 *     (b) is fully functional; only (a) - a player voluntarily equipping the bare jetpack chestplate
 *     via normal click/shift-click actions - is blocked. Named explicitly in this package's
 *     structured report as the one genuine open blocker.</li>
 * </ul>
 */
public abstract class JetpackBase extends ItemArmorMod {

    public JetpackBase(Item.Properties properties) {
        super(properties, ArmorModHandler.plate_only, false, true, false, false);
    }

    public static int getFuel(ItemStack stack) {
        return stack.getOrDefault(ArmorDataComponents.JETPACK_FUEL.get(), 0);
    }

    public static void setFuel(ItemStack stack, int amount) {
        stack.set(ArmorDataComponents.JETPACK_FUEL.get(), Math.max(0, amount));
    }

    /** CE: {@code JetpackBase#useUpFuel(EntityPlayer, ItemStack, int)} - drains one fuel unit every
     * {@code rate} ticks (an integer mB-per-tick-batch drain, not a per-tick float). */
    protected void useUpFuel(Player player, ItemStack stack, int rate) {
        if (player.tickCount % rate == 0) {
            setFuel(stack, getFuel(stack) - 1);
        }
    }

    /** CE: {@code JetpackBase#modUpdate(EntityLivingBase, ItemStack)} - the mod-slot delivery half of
     * dual-mode wear (see class javadoc). */
    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!(entity instanceof Player player)) return;

        ItemStack jetpack = ArmorModHandler.pryMod(armor, ArmorModHandler.plate_only);
        if (jetpack.isEmpty()) return;

        onArmorTick(entity.level(), player, jetpack);
        ArmorUtil.resetFlightTime(player);

        // Writes the (possibly fuel-changed) extracted copy back into the mod slot - this port's
        // component-backed mod storage hands back independent ItemStack copies from pryMod/pryMods,
        // not live references into the parent's stored ItemContainerContents, so mutations made
        // inside onArmorTick must be explicitly re-persisted, exactly mirroring CE's own NBT-compound
        // copy-out/copy-back semantics.
        ArmorModHandler.applyMod(armor, jetpack);
    }

    /** The standalone-wear delivery half of dual-mode wear (see class javadoc) - this port's
     * confirmed {@code Item#onArmorTick} replacement. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!(entity instanceof Player player)) return;
        if (player.getItemBySlot(EquipmentSlot.CHEST) != stack) return;

        onArmorTick(level, player, stack);
        ArmorUtil.resetFlightTime(player);
    }

    /** CE: each leaf's own {@code onArmorTick(World, EntityPlayer, ItemStack)} override (inherited in
     * CE from Forge's {@code Item#onArmorTick} hook - see class javadoc's "Not ported" note for why
     * this port declares it explicitly instead). */
    protected abstract void onArmorTick(Level level, Player player, ItemStack stack);

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        components.add(Component.empty());
        super.appendHoverText(stack, context, components, flag);
        components.add(Component.literal("Can be worn on its own!").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        ItemStack jetpack = ArmorModHandler.pryMod(armor, ArmorModHandler.plate_only);
        if (jetpack.isEmpty()) return;

        list.add(Component.literal("  ").withStyle(ChatFormatting.RED).append(stack.getHoverName()));
    }
}
