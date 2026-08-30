package com.hbm.items.gear;

import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.MaterialShapes;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

/**
 * {@link Tier} definitions for the melee weapons/hoes in {@code items/gear}, ported from CE's
 * {@code MaterialRegistry.enumToolMaterial*} fields ({@code EnumHelper.addToolMaterial(name,
 * harvestLevel, maxUses, efficiency, damageVsEntity, enchantability)} - upstream hbm-ce
 * {@code main/MaterialRegistry.java:107-124}).
 * <p>
 * Deliberately separate from {@code com.hbm.items.tool.ToolTiers} (a different concurrent Phase 1
 * area's file): that class zeroes every tier's {@code attackDamageBonus} because its own
 * {@code ItemToolAbility} family fully overrides attack-damage attribute modifiers itself, which
 * would silently drop CE's real melee damage for these plain {@code SwordItem}/{@code HoeItem}
 * ports (none of which override attack-damage attributes). This class keeps each material's real
 * {@code damageVsEntity} as {@link Tier#getAttackDamageBonus()} instead.
 * <p>
 * Harvest-level buckets reuse {@code ToolTiers}' own convention (2 -&gt;
 * {@code INCORRECT_FOR_IRON_TOOL}, 3 -&gt; {@code INCORRECT_FOR_DIAMOND_TOOL}, 4+ -&gt;
 * {@code INCORRECT_FOR_NETHERITE_TOOL}); CE harvest levels 0-1 (bat/bat_nail/sledge) have no
 * meaningful vanilla tag below iron and these are melee weapons, not mining tools, so they are
 * bucketed at {@code INCORRECT_FOR_IRON_TOOL} too - harmless, since {@code SwordItem} never mines.
 * <p>
 * CE's {@code enumToolMaterialHammer}/{@code Sledge}/{@code Elec} all declare {@code maxUses = 0}.
 * In 1.12, a 0-max-damage tool is treated as non-damageable (unbreakable) while still stacking
 * normally. In 1.21, {@code Item.Properties#durability(int)} unconditionally marks the item
 * damageable (forcing max stack size to 1) regardless of the value passed, including 0 - and
 * {@code SwordItem} damages itself by 1 on every hit via {@code postHurtEnemy}, so a literal 0
 * would make the weapon destroy itself on its very first swing. These three tiers use
 * {@link Integer#MAX_VALUE} uses instead to preserve CE's "never breaks in practice" intent without
 * hitting that instant-break edge case; each affected weapon is a unique single-stack item already; this changes
 * durability-bar display but not gameplay balance.
 */
final class GearTiers {

    private GearTiers() {
    }

    private static Ingredient repairIngot(com.hbm.inventory.material.NTMMaterial mat) {
        return Ingredient.of(MaterialShapes.INGOT.commonTag(mat));
    }

    public static final Tier STEEL = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 750, 8.0F, 2.0F, 10, () -> repairIngot(Mats.MAT_STEEL));
    public static final Tier TITANIUM = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1000, 9.0F, 2.5F, 15, () -> repairIngot(Mats.MAT_TITANIUM));
    public static final Tier ALLOY = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 2000, 15.0F, 5.0F, 5, () -> Ingredient.EMPTY);
    public static final Tier ELEC = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, Integer.MAX_VALUE, 30.0F, 12.0F, 2, () -> Ingredient.EMPTY);
    public static final Tier DESH = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 0, 7.5F, 2.0F, 10, () -> repairIngot(Mats.MAT_DESH));
    public static final Tier COBALT = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 750, 9.0F, 2.5F, 15, () -> Ingredient.EMPTY);
    public static final Tier SCHRABIDIUM = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 10000, 50.0F, 100.0F, 200, () -> repairIngot(Mats.MAT_SCHRABIDIUM));
    public static final Tier HAMMER = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, Integer.MAX_VALUE, 50.0F, 999999996F, 200,
            () -> Ingredient.of(MaterialShapes.BLOCK.commonTag(Mats.MAT_SCHRABIDIUM)));
    public static final Tier SLEDGE = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, Integer.MAX_VALUE, 25.0F, 26F, 200, () -> Ingredient.EMPTY);
    public static final Tier SAW = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 750, 2.0F, 3.5F, 25, () -> Ingredient.EMPTY);
    public static final Tier BAT = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 500, 1.5F, 3F, 25, () -> Ingredient.EMPTY);
    public static final Tier BAT_NAIL = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 450, 1.0F, 4F, 25, () -> Ingredient.EMPTY);
    public static final Tier GOLF_CLUB = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 1000, 2.0F, 5F, 25, () -> Ingredient.EMPTY);
    public static final Tier PIPE_RUSTY = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 350, 1.5F, 4.5F, 25, () -> Ingredient.EMPTY);
    public static final Tier PIPE_LEAD = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 250, 1.5F, 5.5F, 25, () -> Ingredient.EMPTY);
    public static final Tier BOTTLE_OPENER = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 250, 1.5F, 0.5F, 200,
            () -> Ingredient.of(MaterialShapes.PLATE.commonTag(Mats.MAT_STEEL)));

    /**
     * CE's {@code cobalt_decorated_hoe}/{@code starmetal_hoe} reuse
     * {@code com.hbm.items.tool.ToolTiers}' constants of the same name directly (no gear-specific
     * damage override exists in CE for those two hoes) - see {@link GearItems}.
     */
}
