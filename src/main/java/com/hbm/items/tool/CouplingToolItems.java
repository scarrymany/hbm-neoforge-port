package com.hbm.items.tool;

import com.hbm.api.block.IToolable.ToolType;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemDrone.DroneType;
import com.hbm.blocks.network.ConveyorBlocks;
import com.hbm.main.MainRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 * Registration for every {@code com.hbm.items.tool} item this Phase 2 machine-coupling pass adds
 * (the 19 {@code docs/phase1/items_tool.md} bucket-(c) items {@code docs/phase2/items_tool_machine_coupling_and_recipe_system.md}
 * surveyed, minus {@code ItemWiring} and {@code ItemRBMKRod} - both already ported by sibling
 * packages this same wave, see this class's own package-level note below). Deliberately a new,
 * separate registration class rather than an edit to {@code ToolItems.java} (Phase 1,
 * already-committed/reviewed) - registers straight into the shared {@link ModItems#ITEMS}
 * {@code DeferredRegister}, the same pattern {@link NetworkToolItems}/{@code RBMKItems} already use
 * this same wave.
 * <p>
 * <b>Not registered here (already done by sibling packages this wave, confirmed by reading their
 * source before writing this class)</b>:
 * <ul>
 *   <li>{@code ItemWiring} - {@link NetworkToolItems#WIRING_TOOL}, the energy cable/pylon package's
 *   own required item.</li>
 *   <li>{@code ItemRBMKRod} - {@code com.hbm.items.machine.rbmk.RBMKRods}, the RBMK column-blocks
 *   package's own fuel-rod item family.</li>
 * </ul>
 * <p>
 * Wiring: exactly one call from {@code ModItems.register()} - {@code CouplingToolItems.registerAll();}
 * - is needed (see this task's wiring notes); no other shared file needs a direct edit.
 */
public final class CouplingToolItems {

    public static DeferredItem<ItemTooling> SCREWDRIVER;
    public static DeferredItem<ItemTooling> SCREWDRIVER_DESH;
    public static DeferredItem<ItemTooling> HAND_DRILL;
    public static DeferredItem<ItemTooling> HAND_DRILL_DESH;
    public static DeferredItem<ItemToolingWeapon> WRENCH_ARCHINEER;
    public static DeferredItem<ItemBlowtorch> BLOWTORCH;
    public static DeferredItem<ItemWrench> WRENCH;
    public static DeferredItem<ItemAnalyzer> ANALYZER;
    public static DeferredItem<ItemAnalysisTool> ANALYSIS_TOOL;
    public static DeferredItem<ItemMirrorTool> MIRROR_TOOL;
    public static DeferredItem<ItemPowerNetTool> POWER_NET_TOOL;
    public static DeferredItem<ItemConveyorWand> CONVEYOR_WAND;
    public static DeferredItem<ItemConveyorWand> CONVEYOR_WAND_EXPRESS;
    public static DeferredItem<ItemConveyorWand> CONVEYOR_WAND_DOUBLE;
    public static DeferredItem<ItemConveyorWand> CONVEYOR_WAND_TRIPLE;
    public static DeferredItem<ItemSettingsTool> SETTINGS_TOOL;
    public static DeferredItem<ItemKeyPin> KEY_PIN;
    public static DeferredItem<ItemKey> KEY_RED;
    public static DeferredItem<ItemKey> KEY_RED_CRACKED;
    public static DeferredItem<ItemLock> LOCK;
    public static DeferredItem<ItemLock> PADLOCK_RUSTY;
    public static DeferredItem<ItemLock> PADLOCK;
    public static DeferredItem<ItemLock> PADLOCK_REINFORCED;
    public static DeferredItem<ItemLock> PADLOCK_UNBREAKABLE;
    public static DeferredItem<ItemCounterfeitKeys> COUNTERFEIT_KEYS;
    public static DeferredItem<ItemRBMKTool> RBMK_TOOL;
    public static DeferredItem<ItemDyatlov> DYATLOV;
    public static DeferredItem<ItemRebarPlacer> REBAR_PLACER;
    public static DeferredItem<ItemAnchorRemote> ANCHOR_REMOTE;
    public static DeferredItem<ItemDrone> DRONE_PATROL;
    public static DeferredItem<ItemDrone> DRONE_PATROL_CHUNKLOADING;
    public static DeferredItem<ItemDrone> DRONE_PATROL_EXPRESS;
    public static DeferredItem<ItemDrone> DRONE_PATROL_EXPRESS_CHUNKLOADING;
    public static DeferredItem<ItemDrone> DRONE_REQUEST;
    public static DeferredItem<ItemDroneLinker> DRONE_LINKER;

    private CouplingToolItems() {
    }

    public static void registerAll() {
        SCREWDRIVER = reg("screwdriver", () -> new ItemTooling(ToolType.SCREWDRIVER, new Item.Properties().durability(256)));
        SCREWDRIVER_DESH = reg("screwdriver_desh", () -> new ItemTooling(ToolType.SCREWDRIVER, new Item.Properties().durability(1024)));
        HAND_DRILL = reg("hand_drill", () -> new ItemTooling(ToolType.HAND_DRILL, new Item.Properties().durability(256)));
        HAND_DRILL_DESH = reg("hand_drill_desh", () -> new ItemTooling(ToolType.HAND_DRILL, new Item.Properties().durability(1024)));
        WRENCH_ARCHINEER = reg("wrench_archineer", () -> new ItemToolingWeapon(ToolType.SCREWDRIVER,
                new Item.Properties().durability(512).attributes(attackDamage(4.0))));
        BLOWTORCH = reg("blowtorch", () -> new ItemBlowtorch(new Item.Properties().stacksTo(1)));
        WRENCH = reg("wrench", () -> new ItemWrench(ToolTiers.STEEL,
                new Item.Properties().stacksTo(1).attributes(wrenchAttributes())));

        ANALYZER = reg("analyzer", () -> new ItemAnalyzer(new Item.Properties()));
        ANALYSIS_TOOL = reg("analysis_tool", () -> new ItemAnalysisTool(new Item.Properties()));
        MIRROR_TOOL = reg("mirror_tool", () -> new ItemMirrorTool(new Item.Properties().stacksTo(1)));
        POWER_NET_TOOL = reg("power_net_tool", () -> new ItemPowerNetTool(new Item.Properties()));

        CONVEYOR_WAND = reg("conveyor_wand", () -> new ItemConveyorWand(new Item.Properties(), () -> ConveyorBlocks.CONVEYOR.get()));
        CONVEYOR_WAND_EXPRESS = reg("conveyor_wand_express", () -> new ItemConveyorWand(new Item.Properties(), () -> ConveyorBlocks.CONVEYOR_EXPRESS.get()));
        CONVEYOR_WAND_DOUBLE = reg("conveyor_wand_double", () -> new ItemConveyorWand(new Item.Properties(), () -> ConveyorBlocks.CONVEYOR_DOUBLE.get()));
        CONVEYOR_WAND_TRIPLE = reg("conveyor_wand_triple", () -> new ItemConveyorWand(new Item.Properties(), () -> ConveyorBlocks.CONVEYOR_TRIPLE.get()));

        SETTINGS_TOOL = reg("settings_tool", () -> new ItemSettingsTool(new Item.Properties().stacksTo(1)));

        KEY_PIN = reg("key_pin", () -> new ItemKeyPin(new Item.Properties()));
        KEY_RED = reg("key_red", () -> new ItemKey(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
        // Registered under this exact name to close a documented Phase 1 gap: BlockForgottenLock
        // (com.hbm.blocks.generic, already committed) does a lazy BuiltInRegistries.ITEM lookup for
        // "hbm:key_red"/"hbm:key_red_cracked" by name specifically because "those key items belong
        // to a different Phase 1 area and have not landed yet" - see that class's own javadoc. Both
        // registrations below make its vault-unlock interaction work with no further changes there.
        KEY_RED_CRACKED = reg("key_red_cracked", () -> new ItemKey(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
        LOCK = reg("lock", () -> new ItemLock(0.1D, new Item.Properties()));
        // CE ModItems.java:2344-2347 ItemLock lockMod / consumable tab. Models+lang already in tree.
        PADLOCK_RUSTY = reg("padlock_rusty", () -> new ItemLock(1.0D, new Item.Properties().stacksTo(1)));
        PADLOCK = reg("padlock", () -> new ItemLock(0.1D, new Item.Properties().stacksTo(1)));
        PADLOCK_REINFORCED = reg("padlock_reinforced", () -> new ItemLock(0.02D, new Item.Properties().stacksTo(1)));
        PADLOCK_UNBREAKABLE = reg("padlock_unbreakable", () -> new ItemLock(0.0D, new Item.Properties().stacksTo(1)));
        COUNTERFEIT_KEYS = reg("counterfeit_keys", () -> new ItemCounterfeitKeys(new Item.Properties(), () -> KEY_PIN.get()));

        RBMK_TOOL = reg("rbmk_tool", () -> new ItemRBMKTool(new Item.Properties().stacksTo(1)));
        DYATLOV = reg("dyatlov", () -> new ItemDyatlov(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

        REBAR_PLACER = reg("rebar_placer", () -> new ItemRebarPlacer(new Item.Properties()));
        ANCHOR_REMOTE = reg("anchor_remote", () -> new ItemAnchorRemote(new Item.Properties().stacksTo(1)));

        DRONE_PATROL = reg("drone_patrol", () -> new ItemDrone(DroneType.PATROL, new Item.Properties()));
        DRONE_PATROL_CHUNKLOADING = reg("drone_patrol_chunkloading", () -> new ItemDrone(DroneType.PATROL_CHUNKLOADING, new Item.Properties()));
        DRONE_PATROL_EXPRESS = reg("drone_patrol_express", () -> new ItemDrone(DroneType.PATROL_EXPRESS, new Item.Properties()));
        DRONE_PATROL_EXPRESS_CHUNKLOADING = reg("drone_patrol_express_chunkloading", () -> new ItemDrone(DroneType.PATROL_EXPRESS_CHUNKLOADING, new Item.Properties()));
        DRONE_REQUEST = reg("drone_request", () -> new ItemDrone(DroneType.REQUEST, new Item.Properties()));
        DRONE_LINKER = reg("drone_linker", () -> new ItemDroneLinker(new Item.Properties()));

        CreativeTabContents.add(ModCreativeTabs.CONTROL, SCREWDRIVER);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, SCREWDRIVER_DESH);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, HAND_DRILL);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, HAND_DRILL_DESH);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, WRENCH_ARCHINEER);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, BLOWTORCH);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, WRENCH);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, ANALYZER);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, ANALYSIS_TOOL);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, MIRROR_TOOL);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, POWER_NET_TOOL);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, CONVEYOR_WAND);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, CONVEYOR_WAND_EXPRESS);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, CONVEYOR_WAND_DOUBLE);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, CONVEYOR_WAND_TRIPLE);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, SETTINGS_TOOL);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, KEY_PIN);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, KEY_RED);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, KEY_RED_CRACKED);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, LOCK);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, PADLOCK_RUSTY);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, PADLOCK);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, PADLOCK_REINFORCED);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, PADLOCK_UNBREAKABLE);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, COUNTERFEIT_KEYS);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, RBMK_TOOL);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, DYATLOV);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, REBAR_PLACER);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, ANCHOR_REMOTE);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, DRONE_PATROL);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, DRONE_PATROL_CHUNKLOADING);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, DRONE_PATROL_EXPRESS);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, DRONE_PATROL_EXPRESS_CHUNKLOADING);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, DRONE_REQUEST);
        CreativeTabContents.add(ModCreativeTabs.CONTROL, DRONE_LINKER);
    }

    private static ItemAttributeModifiers attackDamage(double amount) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, amount, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    /** CE's {@code ItemWrench}: +4 attack damage, -0.1 movement speed while held (matches its own {@code getAttributeModifiers}/{@code hitEntity} pair). */
    private static ItemAttributeModifiers wrenchAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.MOVEMENT_SPEED, new AttributeModifier(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "wrench_modifier"), -0.1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    private static <T extends Item> DeferredItem<T> reg(String name, Supplier<T> factory) {
        return ModItems.ITEMS.register(name, factory);
    }
}
