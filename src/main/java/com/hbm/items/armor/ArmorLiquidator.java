package com.hbm.items.armor;

import com.hbm.api.item.IGasMask;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.util.ArmorRegistry.HazardClass;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.armor.ArmorLiquidator} (147 lines) - the Liquidator hazmat
 * suit: a plain {@link ArmorFSB} (no charge/fuel gate) that is also a full-hood {@link IGasMask}.
 *
 * <p>CE's real non-rendering behavior, all ported: a fixed +100 knockback-resistance /
 * -10% movement-speed pair on every piece (CE: {@code getItemAttributeModifiers}, an
 * unconditionally-active query - ported as a static {@link ItemAttributeModifiers} component rather
 * than a per-tick recompute, since it has no condition to recompute); a no-restriction gas-mask
 * blacklist (the full hood protects against everything, unlike a half-mask); the helmet-only
 * filter-eject-on-sneak-right-click behavior; and the helmet-only gas-mask tooltip line. CE's
 * {@code renderHelmetOverlay} (motion-blur GL overlay) is Phase 5, not reproduced here.
 *
 * <p>Since 1.21 has no metadata-shared single class the way CE's {@code this ==
 * ModItems.liquidator_helmet} identity check assumed, the "only the helmet drives the gas-mask
 * filter" restriction is instead expressed by only constructing the helmet piece with
 * {@code implements IGasMask} behavior wired - the plate/legs/boots pieces are the same class but
 * simply never have a filter installed on them (harmless, matches CE's real behavior since nothing
 * ever calls {@link IGasMask} methods on a non-helmet slot).
 */
public class ArmorLiquidator extends ArmorFSB implements IGasMask {

    public ArmorLiquidator(Holder<ArmorMaterial> material, Type type, Item.Properties properties) {
        super(material, type, attributeProperties(properties, type));
    }

    private static Item.Properties attributeProperties(Item.Properties properties, Type type) {
        EquipmentSlot slot = switch (type) {
            case HELMET -> EquipmentSlot.HEAD;
            case CHESTPLATE -> EquipmentSlot.CHEST;
            case LEGGINGS -> EquipmentSlot.LEGS;
            case BOOTS -> EquipmentSlot.FEET;
            default -> EquipmentSlot.CHEST;
        };
        ItemAttributeModifiers modifiers = ItemAttributeModifiers.builder()
                .add(Attributes.KNOCKBACK_RESISTANCE,
                        new AttributeModifier(ArmorModHandler.getArmorSlotModifierId(slot), 100D, AttributeModifier.Operation.ADD_VALUE),
                        ArmorModHandler.getArmorSlotGroup(slot))
                .add(Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(ArmorModHandler.getArmorSlotModifierId(slot), -0.1D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                        ArmorModHandler.getArmorSlotGroup(slot))
                .build();
        return properties.attributes(modifiers);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        super.appendHoverText(stack, context, components, flag);
        if (this.getType() == Type.HELMET) {
            ArmorUtil.addGasMaskTooltip(stack, components);
        }
    }

    @Override
    public List<HazardClass> getBlacklist(ItemStack stack) {
        return Collections.emptyList(); // full hood has no restrictions
    }

    @Override
    public ItemStack getFilter(ItemStack stack) {
        return ArmorUtil.getGasMaskFilter(stack);
    }

    @Override
    public void installFilter(ItemStack stack, ItemStack filter) {
        ArmorUtil.installGasMaskFilter(stack, filter);
    }

    @Override
    public void damageFilter(ItemStack stack, int damage) {
        ArmorUtil.damageGasMaskFilter(stack, damage);
    }

    @Override
    public boolean isFilterApplicable(ItemStack stack, ItemStack filter) {
        return true;
    }

    /**
     * CE: {@code ArmorLiquidator#onItemRightClick} - sneak-right-click ejects the helmet's
     * installed filter, restricted to the helmet piece only (CE: {@code this ==
     * ModItems.liquidator_helmet}). Same {@code ArmorItem#use} super-call shape as
     * {@code ArmorGasMask#use} (confirmed real 1.21.1 API - see that class's javadoc).
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (this.getType() == Type.HELMET && player.isShiftKeyDown()) {
            ItemStack filter = getFilter(stack);

            if (!filter.isEmpty()) {
                ArmorUtil.removeFilter(stack);

                if (!player.getInventory().add(filter)) {
                    player.drop(filter, false);
                }
            }
        }

        return super.use(level, player, hand);
    }
}
