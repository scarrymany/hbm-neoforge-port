package com.hbm.items.weapon;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

/**
 * {@link Tier} definitions for the {@code items/weapon} melee family, mirroring
 * {@code com.hbm.items.gear.GearTiers}' pattern exactly (that class is package-private to
 * {@code items/gear}, so this package needs its own copy of the convention rather than reusing it).
 * Ported from CE's {@code MaterialRegistry.matCrucible}
 * ({@code EnumHelper.addToolMaterial("CRUCIBLE", 3, 10000, 50.0F, 100.0F, 200)} - upstream hbm-ce
 * {@code MaterialRegistry.java:127}).
 * <p>
 * The {@code attackDamageBonus} CE's {@code EnumHelper} call encodes (100.0F) is <b>not</b>
 * reproduced as this tier's {@link Tier#getAttackDamageBonus()} - {@link ItemCrucible} (via
 * {@code ItemSwordAbility#createAttributes}) bakes its own {@code damage} constructor argument
 * (500, CE {@code ModItems.java:771}) directly into the {@code ATTACK_DAMAGE} modifier instead,
 * exactly like every other {@code ItemSwordAbility} family member - the tier's own attack-damage
 * value is simply never read for this item family (same reasoning
 * {@code com.hbm.items.tool.ToolTiers}' javadoc already documents for {@code ItemToolAbility}).
 */
final class WeaponTiers {

    private WeaponTiers() {
    }

    public static final Tier CRUCIBLE = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 10000, 50.0F, 0F, 200, () -> Ingredient.EMPTY);
}
