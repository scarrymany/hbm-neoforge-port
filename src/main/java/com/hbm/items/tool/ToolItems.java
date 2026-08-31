package com.hbm.items.tool;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.handler.ability.IToolAreaAbility;
import com.hbm.handler.ability.IToolHarvestAbility;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemToolAbility.ToolRole;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.UnaryOperator;

/**
 * Registers every Phase-1-safe {@code items/tool} content item: the ~40 material-tiered mining
 * tools built on {@link ItemToolAbility}/{@link ItemToolAbilityFueled}/{@link ItemToolAbilityPower}
 * (upstream hbm-ce {@code ModItems.java} lines ~1439-1801), the {@code multitool_dig}/
 * {@code multitool_silk} rungs of {@link ItemMultitoolTool}, the handful of standalone simple
 * items this package also owns, and the detector/diagnostic, fluid-container, repair-kit, and
 * GUI-shell items rounding out docs/phase1/items_tool.md bucket (a). Several of the latter group
 * are registered with a documented stub (missing world-gen block, missing GUI/menu framework, or
 * missing a cross-cutting system like {@code ConsumableHandler}/{@code PollutionHandler}) rather
 * than faked behavior - see each item class's own javadoc for specifics. See
 * {@code docs/phase1/items_tool.md} for the full area survey.
 *
 * <p>Attack damage/speed parity: CE's {@code ItemToolAbility} overrides
 * {@code getItemAttributeModifiers} to apply the constructor's {@code damage}/{@code attackSpeedIn}
 * arguments as flat {@code ADD_VALUE} modifiers, plus a {@code movement} argument as an
 * {@code ADD_MULTIPLIED_BASE} movement-speed modifier. This port instead builds each item's
 * {@link ItemAttributeModifiers} via the vanilla {@link PickaxeItem#createAttributes}/
 * {@link AxeItem#createAttributes}/{@link ShovelItem#createAttributes} factories - the confirmed
 * real 1.21.1 replacement for that override (verified against the Neo Edition reference's own
 * {@code ToolAbilityItem} registration helpers in {@code NtmItems.java}, which use the identical
 * pattern). Every {@link ToolTiers} tier already declares {@code attackDamageBonus = 0} for exactly
 * this reason (see that class's javadoc), so passing CE's raw {@code damage} value through
 * unchanged reproduces CE's real numbers exactly. CE's separate {@code movement} argument (a small
 * -0.05/-0.1 movement-speed debuff on 4 of the ~40 items) has no equivalent here: Neo Edition's own
 * parallel port of this exact class family drops it too, and it is a minor, non-mining-affecting
 * combat-flavor detail - not reproduced, not silently faked.
 *
 * <p>Creative tab parity: none of CE's {@code ItemToolAbility}/{@code ItemToolAbilityFueled}/
 * {@code ItemToolAbilityPower}/{@code multitool_silk} instances ever receive a
 * {@code setCreativeTab(...)} call in CE's {@code ModItems.java} (confirmed by reading every
 * declaration site) - they are real, functional items in CE, just not creative-menu-browsable
 * ones (obtainable via {@code /give} or NEI/JEI search). This port matches that exactly: none of
 * those items are added to {@link CreativeTabContents}. Only {@code multitool_dig} (CE:
 * {@code consumableTab}) and the standalone items below get tab wiring.
 */
public final class ToolItems {

    private static final float ATTACK_SPEED = -2.8F;

    private ToolItems() {
    }

    // ==================== titanium ====================

    public static final DeferredItem<Item> TITANIUM_PICKAXE = tool("titanium_pickaxe", ToolTiers.TITANIUM, ToolRole.PICKAXE, 4.5F, ATTACK_SPEED);
    public static final DeferredItem<Item> TITANIUM_AXE = tool("titanium_axe", ToolTiers.TITANIUM, ToolRole.AXE, 5.5F, ATTACK_SPEED);
    public static final DeferredItem<Item> TITANIUM_SHOVEL = tool("titanium_shovel", ToolTiers.TITANIUM, ToolRole.SHOVEL, 3.5F, ATTACK_SPEED);

    // ==================== steel ====================

    public static final DeferredItem<Item> STEEL_PICKAXE = tool("steel_pickaxe", ToolTiers.STEEL, ToolRole.PICKAXE, 4F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.RECURSION, 0));
    public static final DeferredItem<Item> STEEL_AXE = tool("steel_axe", ToolTiers.STEEL, ToolRole.AXE, 5F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.RECURSION, 0));
    public static final DeferredItem<Item> STEEL_SHOVEL = tool("steel_shovel", ToolTiers.STEEL, ToolRole.SHOVEL, 3F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.RECURSION, 0));

    // ==================== alloy (CE: @Deprecated, still instantiated) ====================

    public static final DeferredItem<Item> ALLOY_PICKAXE = tool("alloy_pickaxe", ToolTiers.ALLOY, ToolRole.PICKAXE, 5F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.RECURSION, 0));
    public static final DeferredItem<Item> ALLOY_AXE = tool("alloy_axe", ToolTiers.ALLOY, ToolRole.AXE, 7F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.RECURSION, 0));
    public static final DeferredItem<Item> ALLOY_SHOVEL = tool("alloy_shovel", ToolTiers.ALLOY, ToolRole.SHOVEL, 4F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.RECURSION, 0));

    // ==================== desh ====================

    public static final DeferredItem<Item> DESH_PICKAXE = tool("desh_pickaxe", ToolTiers.DESH, ToolRole.PICKAXE, 5F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolAreaAbility.RECURSION, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1));
    public static final DeferredItem<Item> DESH_AXE = tool("desh_axe", ToolTiers.DESH, ToolRole.AXE, 6.5F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolAreaAbility.RECURSION, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1));
    public static final DeferredItem<Item> DESH_SHOVEL = tool("desh_shovel", ToolTiers.DESH, ToolRole.SHOVEL, 4F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.HAMMER, 0)
                    .addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolAreaAbility.RECURSION, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0)
                    .addAbility(IToolHarvestAbility.LUCK, 1));

    // ==================== cobalt ====================

    public static final DeferredItem<Item> COBALT_PICKAXE = tool("cobalt_pickaxe", ToolTiers.COBALT, ToolRole.PICKAXE, 4F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.RECURSION, 1).addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 0));
    public static final DeferredItem<Item> COBALT_AXE = tool("cobalt_axe", ToolTiers.COBALT, ToolRole.AXE, 6F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.RECURSION, 1).addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 0));
    public static final DeferredItem<Item> COBALT_SHOVEL = tool("cobalt_shovel", ToolTiers.COBALT, ToolRole.SHOVEL, 3.5F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.RECURSION, 1).addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 0));

    // ==================== misc single tools ====================

    public static final DeferredItem<Item> CENTRI_STICK = tool("centri_stick", ToolTiers.ELEC, ToolRole.MINER, 3F, ATTACK_SPEED, 50,
            item -> item.addAbility(IToolHarvestAbility.CENTRIFUGE, 0));
    public static final DeferredItem<Item> SMASHING_HAMMER = tool("smashing_hammer", ToolTiers.STEEL, ToolRole.MINER, 12F, ATTACK_SPEED, 2500,
            item -> item.addAbility(IToolHarvestAbility.SHREDDER, 0));

    // ==================== cobalt (decorated) ====================

    public static final DeferredItem<Item> COBALT_DECORATED_PICKAXE = tool("cobalt_decorated_pickaxe", ToolTiers.COBALT_DECORATED, ToolRole.PICKAXE, 6F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.RECURSION, 1).addAbility(IToolAreaAbility.HAMMER, 0).addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 2));
    public static final DeferredItem<Item> COBALT_DECORATED_AXE = tool("cobalt_decorated_axe", ToolTiers.COBALT_DECORATED, ToolRole.AXE, 8F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.RECURSION, 1).addAbility(IToolAreaAbility.HAMMER, 0).addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 2));
    public static final DeferredItem<Item> COBALT_DECORATED_SHOVEL = tool("cobalt_decorated_shovel", ToolTiers.COBALT_DECORATED, ToolRole.SHOVEL, 5F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.RECURSION, 1).addAbility(IToolAreaAbility.HAMMER, 0).addAbility(IToolAreaAbility.HAMMER_FLAT, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 2));

    // ==================== starmetal ====================

    public static final DeferredItem<Item> STARMETAL_PICKAXE = tool("starmetal_pickaxe", ToolTiers.STARMETAL, ToolRole.PICKAXE, 8F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.RECURSION, 3).addAbility(IToolAreaAbility.HAMMER, 1).addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 4));
    public static final DeferredItem<Item> STARMETAL_AXE = tool("starmetal_axe", ToolTiers.STARMETAL, ToolRole.AXE, 12F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.RECURSION, 3).addAbility(IToolAreaAbility.HAMMER, 1).addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 4));
    public static final DeferredItem<Item> STARMETAL_SHOVEL = tool("starmetal_shovel", ToolTiers.STARMETAL, ToolRole.SHOVEL, 7F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.RECURSION, 3).addAbility(IToolAreaAbility.HAMMER, 1).addAbility(IToolAreaAbility.HAMMER_FLAT, 1)
                    .addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 4));

    // ==================== cmb ====================

    public static final DeferredItem<Item> CMB_PICKAXE = tool("cmb_pickaxe", ToolTiers.CMB, ToolRole.PICKAXE, 10F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.RECURSION, 2).addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 2));
    public static final DeferredItem<Item> CMB_AXE = tool("cmb_axe", ToolTiers.CMB, ToolRole.AXE, 30F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.RECURSION, 2).addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 2));
    public static final DeferredItem<Item> CMB_SHOVEL = tool("cmb_shovel", ToolTiers.CMB, ToolRole.SHOVEL, 8F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.RECURSION, 2).addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 2));

    // ==================== bismuth ====================

    public static final DeferredItem<Item> BISMUTH_AXE = tool("bismuth_axe", ToolTiers.BISMUTH, ToolRole.AXE, 25F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.HAMMER, 1).addAbility(IToolAreaAbility.HAMMER_FLAT, 1).addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.SHREDDER, 0).addAbility(IToolHarvestAbility.LUCK, 1).addAbility(IToolHarvestAbility.SILK, 0));
    public static final DeferredItem<Item> BISMUTH_PICKAXE = tool("bismuth_pickaxe", ToolTiers.BISMUTH, ToolRole.MINER, 15F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.HAMMER, 1).addAbility(IToolAreaAbility.HAMMER_FLAT, 1).addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.SHREDDER, 0).addAbility(IToolHarvestAbility.LUCK, 1).addAbility(IToolHarvestAbility.SILK, 0)
                    .setDepthRockBreaker());

    // ==================== volcanic ====================

    public static final DeferredItem<Item> VOLCANIC_AXE = tool("volcanic_axe", ToolTiers.VOLCANIC, ToolRole.AXE, 25F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.HAMMER, 1).addAbility(IToolAreaAbility.HAMMER_FLAT, 1).addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.SMELTER, 0).addAbility(IToolHarvestAbility.LUCK, 2).addAbility(IToolHarvestAbility.SILK, 0));
    public static final DeferredItem<Item> VOLCANIC_PICKAXE = tool("volcanic_pickaxe", ToolTiers.VOLCANIC, ToolRole.MINER, 15F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.HAMMER, 1).addAbility(IToolAreaAbility.HAMMER_FLAT, 1).addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.SMELTER, 0).addAbility(IToolHarvestAbility.LUCK, 2).addAbility(IToolHarvestAbility.SILK, 0)
                    .setDepthRockBreaker());

    // ==================== chlorophyte ====================

    public static final DeferredItem<Item> CHLOROPHYTE_AXE = tool("chlorophyte_axe", ToolTiers.CHLOROPHYTE, ToolRole.AXE, 50F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.HAMMER, 1).addAbility(IToolAreaAbility.HAMMER_FLAT, 1).addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.LUCK, 3));
    public static final DeferredItem<Item> CHLOROPHYTE_PICKAXE = tool("chlorophyte_pickaxe", ToolTiers.CHLOROPHYTE, ToolRole.MINER, 20F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.HAMMER, 1).addAbility(IToolAreaAbility.HAMMER_FLAT, 1).addAbility(IToolAreaAbility.RECURSION, 1)
                    .addAbility(IToolHarvestAbility.LUCK, 3).addAbility(IToolHarvestAbility.CENTRIFUGE, 0).addAbility(IToolHarvestAbility.MERCURY, 0)
                    .setDepthRockBreaker());

    // ==================== schrabidium ====================

    public static final DeferredItem<Item> SCHRABIDIUM_PICKAXE = tool("schrabidium_pickaxe", ToolTiers.SCHRABIDIUM, ToolRole.PICKAXE, 20F, ATTACK_SPEED, Rarity.RARE,
            item -> item.addAbility(IToolAreaAbility.HAMMER, 1).addAbility(IToolAreaAbility.HAMMER_FLAT, 1).addAbility(IToolAreaAbility.RECURSION, 6)
                    .addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 4).addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.SHREDDER, 0));
    public static final DeferredItem<Item> SCHRABIDIUM_AXE = tool("schrabidium_axe", ToolTiers.SCHRABIDIUM, ToolRole.AXE, 25F, ATTACK_SPEED, Rarity.RARE,
            item -> item.addAbility(IToolAreaAbility.HAMMER, 1).addAbility(IToolAreaAbility.HAMMER_FLAT, 1).addAbility(IToolAreaAbility.RECURSION, 6)
                    .addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 4).addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.SHREDDER, 0));
    public static final DeferredItem<Item> SCHRABIDIUM_SHOVEL = tool("schrabidium_shovel", ToolTiers.SCHRABIDIUM, ToolRole.SHOVEL, 15F, ATTACK_SPEED, Rarity.RARE,
            item -> item.addAbility(IToolAreaAbility.HAMMER, 1).addAbility(IToolAreaAbility.HAMMER_FLAT, 1).addAbility(IToolAreaAbility.RECURSION, 6)
                    .addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 4).addAbility(IToolHarvestAbility.SMELTER, 0)
                    .addAbility(IToolHarvestAbility.SHREDDER, 0));

    // ==================== mese ====================

    public static final DeferredItem<Item> MESE_PICKAXE = tool("mese_pickaxe", ToolTiers.MESE, ToolRole.MINER, 35F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.HAMMER, 2).addAbility(IToolAreaAbility.HAMMER_FLAT, 2).addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.CRYSTALLIZER, 0).addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 5)
                    .addAbility(IToolAreaAbility.EXPLOSION, 3)
                    .setDepthRockBreaker());
    public static final DeferredItem<Item> MESE_AXE = tool("mese_axe", ToolTiers.MESE, ToolRole.AXE, 75F, ATTACK_SPEED,
            item -> item.addAbility(IToolAreaAbility.HAMMER, 2).addAbility(IToolAreaAbility.HAMMER_FLAT, 2).addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 5)
                    .addAbility(IToolAreaAbility.EXPLOSION, 3));

    // ==================== dwarven ====================

    public static final DeferredItem<Item> DWARVEN_PICKAXE = tool("dwarven_pickaxe", ToolTiers.DWARVEN, ToolRole.MINER, 5F, ATTACK_SPEED, 250,
            item -> item.addAbility(IToolAreaAbility.HAMMER, 0).addAbility(IToolAreaAbility.HAMMER_FLAT, 0));

    // ==================== elec (ItemToolAbilityPower) ====================

    public static final DeferredItem<Item> ELEC_PICKAXE = powerTool("elec_pickaxe", ToolTiers.ELEC, ToolRole.PICKAXE, 10F, ATTACK_SPEED, 500_000L, 1000L, 100L,
            item -> item.addAbility(IToolAreaAbility.HAMMER, 1).addAbility(IToolAreaAbility.HAMMER_FLAT, 1).addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 1));
    public static final DeferredItem<Item> ELEC_AXE = powerTool("elec_axe", ToolTiers.ELEC, ToolRole.AXE, 10F, ATTACK_SPEED, 500_000L, 1000L, 100L,
            item -> item.addAbility(IToolAreaAbility.HAMMER, 1).addAbility(IToolAreaAbility.HAMMER_FLAT, 1).addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 1));
    public static final DeferredItem<Item> ELEC_SHOVEL = powerTool("elec_shovel", ToolTiers.ELEC, ToolRole.SHOVEL, 7.5F, ATTACK_SPEED, 500_000L, 1000L, 100L,
            item -> item.addAbility(IToolAreaAbility.HAMMER, 1).addAbility(IToolAreaAbility.HAMMER_FLAT, 1).addAbility(IToolAreaAbility.RECURSION, 2)
                    .addAbility(IToolHarvestAbility.SILK, 0).addAbility(IToolHarvestAbility.LUCK, 1));

    // ==================== chainsaw (ItemToolAbilityFueled) ====================

    /**
     * CE: {@code new ItemChainsaw("chainsaw", 25, -2.8F, -0.05, ..., 5000, 1, 250, Fluids.DIESEL, ...)}
     * plus {@code .addAbility(IWeaponAbility.CHAINSAW, 1).addAbility(IWeaponAbility.BEHEADER, 0)} -
     * both weapon abilities dropped (Phase 3, see {@link com.hbm.handler.ability.IWeaponAbility}'s
     * own javadoc); silk touch, vein-mining and the always-on shears flag are unabridged.
     */
    public static final DeferredItem<Item> CHAINSAW = ModItems.ITEMS.register("chainsaw", () -> {
        ItemToolAbility item = new ItemChainsaw(
                new Item.Properties().stacksTo(1).durability(1).attributes(attributesFor(ToolRole.AXE, ToolTiers.CHAINSAW, 25F, ATTACK_SPEED)),
                ToolTiers.CHAINSAW, 5000, 1, 250,
                Fluids.DIESEL, Fluids.DIESEL_CRACK, Fluids.KEROSENE, Fluids.BIOFUEL, Fluids.GASOLINE, Fluids.GASOLINE_LEADED,
                Fluids.PETROIL, Fluids.PETROIL_LEADED, Fluids.COALGAS, Fluids.COALGAS_LEADED);
        return item.addAbility(IToolHarvestAbility.SILK, 0)
                .addAbility(IToolAreaAbility.RECURSION, 2)
                .setShears();
    });

    // ==================== multitool (ItemMultitoolTool, dig/silk rungs only) ====================

    public static final DeferredItem<Item> MULTITOOL_DIG = multitool("multitool_dig", false);
    public static final DeferredItem<Item> MULTITOOL_SILK = multitool("multitool_silk", true);

    // ==================== standalone simple items ====================

    public static final DeferredItem<Item> COUPLING_TOOL = ModItems.ITEMS.register("coupling_tool", () -> new ItemCouplingTool(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> ROD_OF_DISCORD = ModItems.ITEMS.register("rod_of_discord", () -> new ItemDiscord(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> MATCHSTICK = ModItems.ITEMS.register("matchstick", () -> new ItemMatch(new Item.Properties()));

    /** CE: {@code chemistry_set}/{@code chemistry_set_boron}, the only two direct instantiations of {@link ItemCraftingDegradation}. */
    public static final DeferredItem<Item> CHEMISTRY_SET = ModItems.ITEMS.register("chemistry_set", () -> new ItemCraftingDegradation(new Item.Properties().stacksTo(1).durability(100)));
    /** Durability 0 (CE parity): never actually damageable, so {@link ItemCraftingDegradation#getCraftingRemainingItem} always returns it unchanged - an infinite-use catalyst. */
    public static final DeferredItem<Item> CHEMISTRY_SET_BORON = ModItems.ITEMS.register("chemistry_set_boron", () -> new ItemCraftingDegradation(new Item.Properties().stacksTo(1).durability(0)));

    // ==================== detectors / diagnostics (docs/phase1/items_tool.md bucket (a)) ====================

    public static final DeferredItem<Item> COLTAN_TOOL = ModItems.ITEMS.register("coltan_tool", () -> new ItemColtanCompass(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> DOSIMETER = ModItems.ITEMS.register("dosimeter", () -> new ItemDosimeter(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> GEIGER_COUNTER = ModItems.ITEMS.register("geiger_counter", () -> new ItemGeigerCounter(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> DIGAMMA_DIAGNOSTIC = ModItems.ITEMS.register("digamma_diagnostic", () -> new ItemDigammaDiagnostic(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> LUNG_DIAGNOSTIC = ModItems.ITEMS.register("lung_diagnostic", () -> new ItemLungDiagnostic(new Item.Properties().stacksTo(1)));
    /** Real per-type density readout; overall tier/fluid summary stubbed - see class javadoc. */
    public static final DeferredItem<Item> ORE_DENSITY_SCANNER = ModItems.ITEMS.register("ore_density_scanner", () -> new ItemOreDensityScanner(new Item.Properties().stacksTo(1)));
    /** Stubbed pending {@code ModBlocks.ore_oil}/{@code ore_bedrock_oil} - see class javadoc. */
    public static final DeferredItem<Item> OIL_DETECTOR = ModItems.ITEMS.register("oil_detector", () -> new ItemOilDetector(new Item.Properties().stacksTo(1)));
    /** Stubbed pending several missing world-gen blocks - see class javadoc. */
    public static final DeferredItem<Item> SURVEY_SCANNER = ModItems.ITEMS.register("survey_scanner", () -> new ItemSurveyScanner(new Item.Properties().stacksTo(1)));
    /** Stubbed pending {@code PollutionHandler} - see class javadoc. */
    public static final DeferredItem<Item> POLLUTION_DETECTOR = ModItems.ITEMS.register("pollution_detector", () -> new ItemPollutionDetector(new Item.Properties().stacksTo(1)));

    // ==================== repair kits (ItemRepairKit; stubbed pending ConsumableHandler) ====================

    public static final DeferredItem<Item> GUN_KIT_1 = ModItems.ITEMS.register("gun_kit_1", () -> new ItemRepairKit(new Item.Properties().stacksTo(1).durability(9)));
    public static final DeferredItem<Item> GUN_KIT_2 = ModItems.ITEMS.register("gun_kit_2", () -> new ItemRepairKit(new Item.Properties().stacksTo(1).durability(99)));

    // ==================== fluid containers (ItemCanister/ItemGasCanister/ItemFluidContainerInfinite/ItemPipette) ====================

    public static final DeferredItem<Item> CANISTER_FUEL = ModItems.ITEMS.register("canister_fuel", () -> new ItemCanister(new Item.Properties(), 1000));
    public static final DeferredItem<Item> GAS_FULL = ModItems.ITEMS.register("gas_full", () -> new ItemGasCanister(new Item.Properties()));
    public static final DeferredItem<Item> FLUID_BARREL_INFINITE = ModItems.ITEMS.register("fluid_barrel_infinite",
            () -> new ItemFluidContainerInfinite(new Item.Properties().stacksTo(1), null, 1_000_000_000, 1, true));
    public static final DeferredItem<Item> INF_WATER = ModItems.ITEMS.register("inf_water",
            () -> new ItemFluidContainerInfinite(new Item.Properties().stacksTo(1), Fluids.WATER, 50));
    public static final DeferredItem<Item> INF_WATER_MK2 = ModItems.ITEMS.register("inf_water_mk2",
            () -> new ItemFluidContainerInfinite(new Item.Properties().stacksTo(1), Fluids.WATER, 500));
    /** CE: partsTab, not controlTab like the other {@link ItemFluidContainerInfinite} instances above. */
    public static final DeferredItem<Item> CHLORINE_PINWHEEL = ModItems.ITEMS.register("chlorine_pinwheel",
            () -> new ItemFluidContainerInfinite(new Item.Properties().stacksTo(1), Fluids.CHLORINE, 1, 2, false));
    public static final DeferredItem<Item> PIPETTE = ModItems.ITEMS.register("pipette", () -> new ItemPipette(new Item.Properties().stacksTo(1), 1000, true));
    public static final DeferredItem<Item> PIPETTE_BORON = ModItems.ITEMS.register("pipette_boron", () -> new ItemPipette(new Item.Properties().stacksTo(1), 1000, false));
    public static final DeferredItem<Item> PIPETTE_LABORATORY = ModItems.ITEMS.register("pipette_laboratory", () -> new ItemPipette(new Item.Properties().stacksTo(1), 50, false));

    // ==================== standalone items with a missing world-gen/paired-block dependency ====================

    /** Stubbed use-behavior pending {@code ModBlocks.balefire} - see class javadoc. */
    public static final DeferredItem<Item> BALEFIRE_AND_STEEL = ModItems.ITEMS.register("balefire_and_steel", () -> new ItemBalefireMatch(new Item.Properties().stacksTo(1).durability(256)));
    /** Stubbed place-behavior pending an accessible registered crate block - see class javadoc. */
    public static final DeferredItem<Item> CRATE_CALLER = ModItems.ITEMS.register("crate_caller", () -> new ItemCrateCaller(new Item.Properties().stacksTo(1).durability(4)));
    /** Stubbed break-behavior pending {@code ModBlocks.ntm_dirt} - see class javadoc. */
    public static final DeferredItem<Item> MYSTERYSHOVEL = ModItems.ITEMS.register("mysteryshovel", () -> new ItemMS(new Item.Properties().stacksTo(1)));

    // ==================== GUI-shell items (ItemBook pattern - menu-opening interaction deferred) ====================

    public static final DeferredItem<Item> BOOK_GUIDE_BOOK = ModItems.ITEMS.register("book_guide_book", () -> new ItemGuideBook(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BOBMAZON = ModItems.ITEMS.register("bobmazon", () -> new ItemCatalog(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BOBMAZON_HIDDEN = ModItems.ITEMS.register("bobmazon_hidden", () -> new ItemCatalog(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> BOOK_LEMEGETON = ModItems.ITEMS.register("book_lemegeton", () -> new ItemBookLemegeton(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> AMMO_BAG = ModItems.ITEMS.register("ammo_bag", () -> new ItemAmmoBag(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> AMMO_BAG_INFINITE = ModItems.ITEMS.register("ammo_bag_infinite", () -> new ItemAmmoBag(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> CASING_BAG = ModItems.ITEMS.register("casing_bag", () -> new ItemCasingBag(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> PLASTIC_BAG = ModItems.ITEMS.register("plastic_bag", () -> new ItemPlasticBag(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> CONTAINMENT_BOX = ModItems.ITEMS.register("containment_box", () -> new ItemLeadBox(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> REACHER = ModItems.ITEMS.register("reacher", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> TOOLBOX = ModItems.ITEMS.register("toolbox", () -> new ItemToolBox(new Item.Properties().stacksTo(1)));

    // ==================== Phase 3 (turret_system) ====================

    /** See {@code com.hbm.items.tool.ItemTurretMobFilter}'s own javadoc. */
    public static final DeferredItem<Item> TURRET_MOB_FILTER = ModItems.ITEMS.register("turret_mob_filter", () -> new ItemTurretMobFilter(new Item.Properties().stacksTo(1)));

    /** No-op beyond forcing this class to load before {@code ModItems.ITEMS.register(modEventBus)}. */
    public static void registerAll() {
    }

    static {
        // Only these get a creative tab - see class javadoc for why the ~44 mining/chainsaw/
        // multitool_silk items above deliberately do not.
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, MULTITOOL_DIG);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, COUPLING_TOOL);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, ROD_OF_DISCORD);
        CreativeTabContents.add(ModCreativeTabs.WEAPON, MATCHSTICK);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, CHEMISTRY_SET);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, CHEMISTRY_SET_BORON);

        // ---- bucket (a) additions: detectors/diagnostics, repair kits, fluid containers,
        // stubbed-dependency standalone items, and GUI-shell items (see docs/phase1/items_tool.md).
        // Tab choices match CE's own setCreativeTab call at each item's ModItems.java declaration.
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, COLTAN_TOOL);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, DOSIMETER);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, GEIGER_COUNTER);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, DIGAMMA_DIAGNOSTIC);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, LUNG_DIAGNOSTIC);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, ORE_DENSITY_SCANNER);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, OIL_DETECTOR);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, SURVEY_SCANNER);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, POLLUTION_DETECTOR);

        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, GUN_KIT_1);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, GUN_KIT_2);

        CreativeTabContents.add(ModCreativeTabs.CONTROL, CANISTER_FUEL);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, GAS_FULL);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, FLUID_BARREL_INFINITE);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, INF_WATER);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, INF_WATER_MK2);
        CreativeTabContents.add(ModCreativeTabs.PARTS, CHLORINE_PINWHEEL);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, PIPETTE);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, PIPETTE_BORON);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, PIPETTE_LABORATORY);

        CreativeTabContents.add(ModCreativeTabs.WEAPON, BALEFIRE_AND_STEEL);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, CRATE_CALLER);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, MYSTERYSHOVEL);

        CreativeTabContents.add(ModCreativeTabs.WEAPON, TURRET_MOB_FILTER);

        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, BOOK_GUIDE_BOOK);
        CreativeTabContents.add(ModCreativeTabs.TEMPLATE, BOBMAZON);
        CreativeTabContents.add(ModCreativeTabs.TEMPLATE, BOBMAZON_HIDDEN);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, BOOK_LEMEGETON);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, AMMO_BAG);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, AMMO_BAG_INFINITE);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, CASING_BAG);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, PLASTIC_BAG);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, CONTAINMENT_BOX);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, TOOLBOX);
    }

    // ==================== construction helpers ====================

    private static ItemAttributeModifiers attributesFor(ToolRole role, Tier tier, float damage, float speed) {
        return switch (role) {
            case AXE -> AxeItem.createAttributes(tier, damage, speed);
            case SHOVEL -> ShovelItem.createAttributes(tier, damage, speed);
            case PICKAXE, MINER -> PickaxeItem.createAttributes(tier, damage, speed);
        };
    }

    private static DeferredItem<Item> tool(String name, Tier tier, ToolRole role, float damage, float speed) {
        return tool(name, tier, role, damage, speed, tier.getUses(), Rarity.COMMON, UnaryOperator.identity());
    }

    private static DeferredItem<Item> tool(String name, Tier tier, ToolRole role, float damage, float speed, UnaryOperator<ItemToolAbility> configure) {
        return tool(name, tier, role, damage, speed, tier.getUses(), Rarity.COMMON, configure);
    }

    private static DeferredItem<Item> tool(String name, Tier tier, ToolRole role, float damage, float speed, int durability, UnaryOperator<ItemToolAbility> configure) {
        return tool(name, tier, role, damage, speed, durability, Rarity.COMMON, configure);
    }

    private static DeferredItem<Item> tool(String name, Tier tier, ToolRole role, float damage, float speed, Rarity rarity, UnaryOperator<ItemToolAbility> configure) {
        return tool(name, tier, role, damage, speed, tier.getUses(), rarity, configure);
    }

    private static DeferredItem<Item> tool(String name, Tier tier, ToolRole role, float damage, float speed, int durability, Rarity rarity, UnaryOperator<ItemToolAbility> configure) {
        return ModItems.ITEMS.register(name, () -> configure.apply(new ItemToolAbility(
                new Item.Properties().stacksTo(1).durability(durability).rarity(rarity).attributes(attributesFor(role, tier, damage, speed)), tier, role)));
    }

    /** Elec tools are forced to durability 1 (CE: {@code setMaxDamage(1)}) - wear drains battery charge instead of vanilla durability. */
    private static DeferredItem<Item> powerTool(String name, Tier tier, ToolRole role, float damage, float speed, long maxPower, long chargeRate, long consumption,
            UnaryOperator<ItemToolAbility> configure) {
        return ModItems.ITEMS.register(name, () -> configure.apply(new ItemToolAbilityPower(
                new Item.Properties().stacksTo(1).durability(1).attributes(attributesFor(role, tier, damage, speed)), tier, role, maxPower, chargeRate, consumption)));
    }

    private static DeferredItem<Item> multitool(String name, boolean silkTouch) {
        return ModItems.ITEMS.register(name, () -> new ItemMultitoolTool(
                new Item.Properties().stacksTo(1).durability(ToolTiers.MULTITOOL.getUses()).attributes(PickaxeItem.createAttributes(ToolTiers.MULTITOOL, 4.0F, 0F)),
                ToolTiers.MULTITOOL, silkTouch));
    }
}
