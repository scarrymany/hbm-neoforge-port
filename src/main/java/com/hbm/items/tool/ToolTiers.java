package com.hbm.items.tool;

import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.MaterialShapes;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

/**
 * Mining-tool {@link Tier} definitions for every material CE's {@code ItemToolAbility} family is
 * instantiated with (from {@code EnumHelper.addToolMaterial(name, harvestLevel, maxUses,
 * efficiency, attackDamage, enchantability)} calls in CE's {@code MaterialRegistry}/{@code ModItems}).
 *
 * <p>Two deliberate translation choices, both confirmed against the real 1.21.1+NeoForge API via
 * the Neo Edition reference's own {@code NtmTiers} (used to confirm API shape only, not values):
 * <ul>
 *     <li>Post-flattening, Minecraft has no arbitrary numeric "harvest level" any more - block
 *     harvestability is gated by {@code minecraft:incorrect_for_*_tool} block tags. CE's harvest
 *     levels 0-6 (HBM extends past vanilla's own 0-3) are mapped onto the closest vanilla tag:
 *     2 -&gt; {@link BlockTags#INCORRECT_FOR_IRON_TOOL}, 3 -&gt; {@link BlockTags#INCORRECT_FOR_DIAMOND_TOOL},
 *     4/5/6 -&gt; {@link BlockTags#INCORRECT_FOR_NETHERITE_TOOL} (vanilla's strongest tier - there is
 *     no vanilla tag beyond it). A block that CE meant to gate behind cobalt+/schrabidium+
 *     specifically (levels 4-6 are all distinct in CE) will need a bespoke {@code hbm:} block tag
 *     once whichever phase registers such blocks - out of scope here.</li>
 *     <li>Every tier's own {@code attackDamageBonus} is left at {@code 0}: CE's
 *     {@code ItemToolAbility} completely overrides {@code getItemAttributeModifiers} with its own
 *     {@code damage}/{@code movement} constructor arguments instead of using the material's damage
 *     bonus, so the per-item damage passed into {@link com.hbm.items.tool.ItemToolAbility}'s
 *     registration helper is the only source of attack damage, matching CE's real numbers.</li>
 * </ul>
 *
 * <p>Repair ingredients reference {@link MaterialShapes#INGOT} common tags (e.g. {@code c:ingots/titanium})
 * rather than concrete items, so this class has no compile-time dependency on which agent's
 * generation loop ends up registering the concrete ingot item - matching the same forward-compatible
 * pattern the hazard-binding plan recommends. Materials with no equivalent {@link Mats} constant yet
 * (chlorophyte, mese, the legacy "alloy"/"elec" materials) get {@link Ingredient#EMPTY} - anvil
 * repair simply won't match anything until such a material exists, which is a strict improvement
 * over guessing a wrong tag.
 */
public final class ToolTiers {

    private ToolTiers() {
    }

    private static Ingredient repairTag(com.hbm.inventory.material.NTMMaterial mat) {
        return Ingredient.of(MaterialShapes.INGOT.commonTag(mat));
    }

    public static final Tier STEEL = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 750, 8.0F, 0.0F, 10, () -> repairTag(Mats.MAT_STEEL));
    public static final Tier TITANIUM = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1000, 9.0F, 0.0F, 15, () -> repairTag(Mats.MAT_TITANIUM));
    /** CE's legacy generic "alloy" tier - superseded by material-specific tiers, kept only because {@code alloy_pickaxe/axe/shovel} are still registered (CE marks them {@code @Deprecated} but still instantiates them). */
    public static final Tier ALLOY = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 2000, 15.0F, 0.0F, 5, () -> Ingredient.EMPTY);
    public static final Tier ELEC = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 0, 30.0F, 0.0F, 2, () -> Ingredient.EMPTY);
    public static final Tier DESH = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 0, 7.5F, 0.0F, 10, () -> repairTag(Mats.MAT_DESH));
    public static final Tier COBALT = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 750, 9.0F, 0.0F, 15, () -> repairTag(Mats.MAT_COBALT));
    public static final Tier COBALT_DECORATED = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 1000, 15.0F, 0.0F, 25, () -> repairTag(Mats.MAT_COBALT));
    public static final Tier CMB = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 8500, 40.0F, 0.0F, 100, () -> repairTag(Mats.MAT_CMB));
    public static final Tier BISMUTH = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 0, 50.0F, 0.0F, 200, () -> repairTag(Mats.MAT_BISMUTH));
    /** CE repairs volcanic tools with bismuth ingots too - same material tier, same repair tag. */
    public static final Tier VOLCANIC = BISMUTH;
    public static final Tier STARMETAL = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1000, 20.0F, 0.0F, 30, () -> repairTag(Mats.MAT_STAR));
    public static final Tier SCHRABIDIUM = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 10000, 50.0F, 0.0F, 200, () -> repairTag(Mats.MAT_SCHRABIDIUM));
    public static final Tier METEORITE = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 0, 50.0F, 0.0F, 200, () -> Ingredient.EMPTY);
    /** No matching {@link Mats} constant yet for chlorophyte/mese - see class javadoc. */
    public static final Tier CHLOROPHYTE = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 0, 50.0F, 0.0F, 200, () -> Ingredient.EMPTY);
    public static final Tier MESE = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 0, 50.0F, 0.0F, 200, () -> Ingredient.EMPTY);
    public static final Tier DWARVEN = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 0, 4.0F, 0.0F, 10, () -> repairTag(Mats.MAT_COPPER));
    public static final Tier CHAINSAW = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1500, 50.0F, 0.0F, 0, () -> repairTag(Mats.MAT_STEEL));
    public static final Tier MULTITOOL = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 5000, 25.0F, 0.0F, 25, () -> Ingredient.EMPTY);
    public static final Tier ELECTERRA = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 0, 20.0F, 0.0F, 2, () -> Ingredient.EMPTY);
}
