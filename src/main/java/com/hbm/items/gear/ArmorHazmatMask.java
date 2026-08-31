package com.hbm.items.gear;

import com.hbm.api.item.IGasMask;
import com.hbm.client.render.armor.M65ArmorModel;
import com.hbm.handler.ArmorUtil;
import com.hbm.util.ArmorRegistry.HazardClass;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Ported from CE's {@code com.hbm.items.gear.ArmorHazmatMask} - 4 live items ({@code hazmat_helmet},
 * {@code hazmat_helmet_red}, {@code hazmat_helmet_grey}, {@code hazmat_paa_helmet},
 * {@link com.hbm.items.gear.SpecialArmorItems}), the hazmat family's helmet variant: extends
 * {@link ArmorHazmat} and additionally implements {@link IGasMask}.
 *
 * <p>{@link #modelTexture} replaces CE's {@code this == ModItems.hazmat_helmet_red ||
 * this == ModItems.hazmat_helmet_grey} identity check in {@code getArmorModel} - only those 2 of
 * the 4 items get the shared {@link M65ArmorModel} swap in CE (real textures {@code
 * ModelHazRed.png}/{@code ModelHazGrey.png}, normalized to this port's snake_case asset convention
 * by task {@code c7-armor-model-rendering} - see {@code SpecialArmorItems#hazmatMask}); {@code
 * hazmat_helmet}/{@code hazmat_paa_helmet} pass {@code null} and get no custom model (plain texture
 * dispatch, already handled by this item's material - see {@link ArmorHazmat}'s javadoc). {@code
 * getBlacklist} is a constant empty list for all 4 items in CE (full protection, no hazard the mask
 * itself refuses to filter) - unlike sibling {@code ArmorGasMask}, no per-item field is needed
 * there.
 */
public class ArmorHazmatMask extends ArmorHazmat implements IGasMask {

    @Nullable
    private final ResourceLocation modelTexture;

    private M65ArmorModel model;

    public ArmorHazmatMask(Holder<ArmorMaterial> material, Type type, Item.Properties properties,
                            @Nullable ResourceLocation modelTexture) {
        super(material, type, properties);
        this.modelTexture = modelTexture;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        if (modelTexture == null) return;

        consumer.accept(new IClientItemExtensions() {
            @Override
            public Model getGenericArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (equipmentSlot != EquipmentSlot.HEAD) return original;
                if (model == null) model = new M65ArmorModel(equipmentSlot, modelTexture);
                model.getPropertiesFrom(original, livingEntity);
                return model;
            }
        });
    }

    @Override
    public List<HazardClass> getBlacklist(ItemStack stack) {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getFilter(ItemStack stack) {
        return ArmorUtil.getGasMaskFilter(stack);
    }

    @Override
    public boolean isFilterApplicable(ItemStack stack, ItemStack filter) {
        return true;
    }

    @Override
    public void installFilter(ItemStack stack, ItemStack filter) {
        ArmorUtil.installGasMaskFilter(stack, filter);
    }

    @Override
    public void damageFilter(ItemStack stack, int damage) {
        ArmorUtil.damageGasMaskFilter(stack, damage);
    }

    /**
     * CE: {@code ArmorHazmatMask#onItemRightClick} - identical sneak-eject-then-fall-through shape
     * as {@code ArmorGasMask#use} (see that method's javadoc for why {@code super.use} - not a
     * locally-fabricated result - is the correct replacement for CE's {@code super.onItemRightClick}).
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> components, TooltipFlag flag) {
        super.appendHoverText(stack, context, components, flag);
        ArmorUtil.addGasMaskTooltip(stack, components);
    }
}
