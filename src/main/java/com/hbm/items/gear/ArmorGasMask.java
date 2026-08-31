package com.hbm.items.gear;

import com.hbm.api.item.IGasMask;
import com.hbm.client.render.armor.ArmorModelBase;
import com.hbm.client.render.armor.GasMaskArmorModel;
import com.hbm.client.render.armor.M65ArmorModel;
import com.hbm.handler.ArmorUtil;
import com.hbm.util.ArmorRegistry.HazardClass;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

/**
 * Ported from CE's {@code com.hbm.items.gear.ArmorGasMask} - 4 live items ({@code gas_mask},
 * {@code gas_mask_m65}, {@code gas_mask_mono}, {@code gas_mask_olde},
 * {@link com.hbm.items.gear.SpecialArmorItems}), the concrete {@link IGasMask} implementation for
 * CE's 4 dedicated gas masks. All 4 are constructed with {@link Type#HELMET} directly rather than
 * CE's shared-class-plus-{@code isValidArmor} override (CE reused one class across a nominally
 * generic {@code ArmorMaterial.IRON} constructor param but only ever built HELMET-slot instances) -
 * with the equipment slot now baked into the item at construction time, {@code isValidArmor} has
 * nothing left to restrict and is dropped entirely, not merely stubbed.
 *
 * <p>{@link #blacklist} replaces CE's {@code this == ModItems.gas_mask_mono} identity-chain in
 * {@code getBlacklist} with a plain constructor field - each of the 4
 * {@link com.hbm.items.gear.SpecialArmorItems} registrations passes its own literal list, which is
 * behaviorally identical to the identity chain (every item is still a distinct instance with its
 * own fixed blacklist) without needing the leaf class to reference its own not-yet-registered
 * sibling fields.
 *
 * <p>Filter storage ({@link #getFilter}/{@link #installFilter}/{@link #damageFilter}) delegates to
 * {@code ArmorUtil}'s already-real {@code GAS_MASK_FILTER} data-component-backed implementation
 * (ported by the sibling {@code hazmat_protection_integration} package) - no NBT anywhere.
 *
 * <p>The custom armor model dispatch ({@link #initializeClient}) is filled in by task {@code
 * c7-armor-model-rendering}: {@link #modelKind} + {@link #modelTexture} (constructor parameters,
 * one per concrete item - see {@code SpecialArmorItems#gasMask}) select between {@link
 * GasMaskArmorModel} ({@code gas_mask}, CE: {@code ModelGasMask}) and the shared {@link
 * M65ArmorModel} ({@code gas_mask_m65}/{@code _mono}/{@code _olde}, CE: a shared {@code ModelM65}
 * instance) exactly matching CE's own {@code ArmorGasMask#getArmorModel}'s {@code this ==
 * ModItems.gas_mask} / {@code this == ModItems.gas_mask_m65 || ...} identity dispatch (translated
 * into a constructor-supplied enum since concrete leaves are no longer distinguished by identity
 * comparison against not-yet-registered sibling fields in this port - see this class's own
 * "{@link #blacklist}" note for the identical reasoning already established for the hazard list).
 * The per-item durability-scaled blur overlay ({@code renderHelmetOverlay}) is pure GL-immediate-
 * mode rendering with no confirmed 1.21 client-item-extension equivalent surveyed in this pass (see
 * {@code com.hbm.items.gear.ArmorModel}'s javadoc, which skips the same CE mechanism for the same
 * reason) - not attempted here; the mask's own wear level still uses vanilla's ordinary damage bar
 * (this item is constructed with real durability, see {@code SpecialArmorItems}), so nothing is
 * lost mechanically, only the cosmetic fog-up render.
 */
public class ArmorGasMask extends ArmorItem implements IGasMask {

    /** Which CE render/model class this concrete gas mask item uses - see class javadoc. */
    public enum ModelKind { GAS_MASK, M65 }

    private final List<HazardClass> blacklist;
    private final ModelKind modelKind;
    private final ResourceLocation modelTexture;

    private GasMaskArmorModel gasMaskModel;
    private M65ArmorModel m65Model;

    public ArmorGasMask(Holder<ArmorMaterial> material, Type type, Item.Properties properties, List<HazardClass> blacklist,
                         ModelKind modelKind, ResourceLocation modelTexture) {
        super(material, type, properties);
        this.blacklist = blacklist;
        this.modelKind = modelKind;
        this.modelTexture = modelTexture;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public Model getGenericArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (equipmentSlot != EquipmentSlot.HEAD) return original;

                ArmorModelBase replacement = switch (modelKind) {
                    case GAS_MASK -> {
                        if (gasMaskModel == null) gasMaskModel = new GasMaskArmorModel(equipmentSlot);
                        yield gasMaskModel;
                    }
                    case M65 -> {
                        if (m65Model == null) m65Model = new M65ArmorModel(equipmentSlot, modelTexture);
                        yield m65Model;
                    }
                };
                replacement.getPropertiesFrom(original, livingEntity);
                return replacement;
            }
        });
    }

    @Override
    public List<HazardClass> getBlacklist(ItemStack stack) {
        return blacklist;
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
     * CE: {@code ArmorGasMask#onItemRightClick} - sneak-right-click ejects the installed filter,
     * then unconditionally falls through to {@code super.onItemRightClick} (CE calls this
     * regardless of sneak state - equip-on-right-click and the sneak-eject are not mutually
     * exclusive in CE, preserved here as-is). {@code ArmorItem#use} (1.21.1 vanilla, pre-
     * {@code Equippable}-data-component era) is the confirmed real replacement for that
     * {@code super} call - it performs the actual right-click equip-swap, so it must still run
     * after the eject, not be replaced by a locally-fabricated result.
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

        if (!blacklist.isEmpty()) {
            components.add(Component.literal("§c" + I18nUtil.resolveKey("hazard.neverProtects")));

            for (HazardClass clazz : blacklist) {
                components.add(Component.literal("§4 -" + I18nUtil.resolveKey(clazz.lang)));
            }
        }
    }
}
