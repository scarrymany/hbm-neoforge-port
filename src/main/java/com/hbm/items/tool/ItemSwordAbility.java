package com.hbm.items.tool;

import com.hbm.handler.ability.AvailableAbilities;
import com.hbm.handler.ability.IBaseAbility;
import com.hbm.main.MainRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * Ability-hook variant of the sword system. Ported from CE's
 * {@code com.hbm.items.tool.ItemSwordAbility}, retargeted at the 1.21 tool API the same way
 * {@link ItemToolAbility} (its mining-tool sibling) was.
 * <p>
 * Not ported from CE's original (documented, not silently dropped):
 * <ul>
 *     <li>Dynamic model baking ({@code IDynamicModels}/{@code IClaimedModelLocation}) - a 1.12-era
 *     workaround with no 1.21 equivalent need, exactly as {@link ItemToolAbility}'s javadoc explains
 *     for the same reason.</li>
 *     <li>The {@code this == ModItems.mese_gavel} identity special-case in CE's {@code hitEntity}
 *     (an extra hit sound cue) - {@code mese_gavel} is not registered anywhere in this port yet;
 *     whoever registers it should add the same check here if wanted.</li>
 * </ul>
 */
public class ItemSwordAbility extends SwordItem {

    /**
     * CE hardcodes this exact UUID ({@code 91AEAA56-376B-4498-935B-2F7F68070635}, "Tool modifier")
     * as the movement-speed modifier id shared by every {@code ItemSwordAbility} instance
     * (upstream hbm-ce {@code ItemSwordAbility.getItemAttributeModifiers}). 1.21's
     * {@link AttributeModifier} keys off a {@link ResourceLocation} instead of a UUID+name pair;
     * this is that id's modern equivalent, still shared by every instance exactly as CE's literal
     * UUID was (harmless: only one sword can occupy the mainhand slot at a time).
     */
    public static final ResourceLocation MOVEMENT_MODIFIER_ID = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "sword_ability_movement");

    /**
     * CE stores these as plain instance fields ({@code protected float damage; protected double
     * attackSpeed; protected double movement;}) so subclasses (e.g. {@code ItemCrucible}) can read
     * the "unboosted" values back out at runtime to compute a charge-dependent delta - kept here for
     * the same reason, even though {@link #createAttributes} already bakes them into this instance's
     * static {@link ItemAttributeModifiers} at construction time.
     */
    protected final float damage;
    protected final double attackSpeed;
    protected final double movement;

    protected final AvailableAbilities availableAbilities = new AvailableAbilities();

    public ItemSwordAbility(float damage, double attackSpeed, double movement, Tier tier, Properties properties) {
        super(tier, properties.attributes(createAttributes(damage, attackSpeed, movement)));
        this.damage = damage;
        this.attackSpeed = attackSpeed;
        this.movement = movement;
    }

    /** CE's convenience constructor - always uses vanilla's standard {@code -2.4} sword attack-speed penalty. */
    public ItemSwordAbility(float damage, double movement, Tier tier, Properties properties) {
        this(damage, -2.4, movement, tier, properties);
    }

    /**
     * Builds the dynamic per-instance {@link ItemAttributeModifiers} CE's ability-sword constructor
     * computed from its {@code (float damage, double attackSpeed, double movement)} triple
     * (upstream hbm-ce {@code ItemSwordAbility.java:53-65,163-171}) - independent of the sword's
     * {@link Tier}, unlike vanilla's {@code SwordItem.createAttributes(Tier, int, float)}. Callers
     * building a concrete ability sword's {@code Item.Properties} should pass this to
     * {@code .attributes(...)} instead.
     *
     * @param attackSpeed CE's convenience constructor (no explicit attack speed) always passed
     *                    {@code -2.4} here, vanilla's own standard sword attack-speed penalty.
     */
    public static ItemAttributeModifiers createAttributes(float damage, double attackSpeed, double movement) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, attackSpeed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                // CE's movement modifier uses 1.12 AttributeModifier.Operation 1 (MULTIPLY_BASE),
                // which maps 1:1 onto modern ADD_MULTIPLIED_BASE - not a flat ADD_VALUE.
                .add(Attributes.MOVEMENT_SPEED, new AttributeModifier(MOVEMENT_MODIFIER_ID, movement, AttributeModifier.Operation.ADD_MULTIPLIED_BASE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public static ItemAttributeModifiers createAttributes(float damage, double movement) {
        return createAttributes(damage, -2.4, movement);
    }

    public ItemSwordAbility addAbility(IBaseAbility ability, int level) {
        this.availableAbilities.addAbility(ability, level);
        return this;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide() && attacker instanceof Player player && canOperate(stack)) {
            this.availableAbilities.getWeaponAbilities().forEach((ability, level) -> ability.onHit(level, attacker.level(), player, target, this));
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    protected boolean canOperate(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        availableAbilities.appendHoverText(tooltipComponents);
    }
}
