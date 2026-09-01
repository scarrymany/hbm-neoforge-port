package com.hbm.items.gear;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import com.hbm.items.armor.WingsMurk;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Registers the 5 concrete jetpack items named by {@code docs/phase3/fsb_armor_and_jetpacks.md} -
 * {@code jetpack_fly}/{@code jetpack_break}/{@code jetpack_vector}/{@code jetpack_boost}/
 * {@code jetpack_glider}, transcribed 1:1 from CE's real {@code ModItems.java} construction site
 * (fluid type, capacity, and registry name all confirmed by grep against
 * {@code upstream/hbm-ce/src/main/java/com/hbm/items/ModItems.java} lines 731-735). Follows the
 * same per-package registrar convention as {@code com.hbm.items.armor.PoweredArmorItems}/
 * {@code com.hbm.items.gear.GearItems} - CE placed every jetpack in {@code CreativeTabs.COMBAT}, so
 * this class owns its own {@link BuildCreativeModeTabContentsEvent} listener rather than touching
 * the shared {@code ModItems}/{@code GearItems} files.
 *
 * <p>None of the 5 sets a vanilla durability bar (CE never calls {@code setMaxDamage} on any
 * {@code Jetpack*} class) - plain {@code new Item.Properties()} beyond the {@code stacksTo(1)}
 * {@code JetpackBase}/{@code ItemArmorMod}'s own constructor already forces.
 */
public final class JetpackItems {

    private JetpackItems() {
    }

    private static final List<Supplier<? extends ItemLike>> COMBAT_TAB = new ArrayList<>();

    public static void registerAll(IEventBus modEventBus) {
        modEventBus.addListener(JetpackItems::onBuildCreativeTab);
    }

    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            COMBAT_TAB.forEach(item -> event.accept(item.get()));
        }
    }

    private static DeferredItem<Item> register(String name, Supplier<? extends Item> factory) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, factory);
        COMBAT_TAB.add(item);
        return item;
    }

    // CE ModItems.java:731 - new JetpackRegular(Fluids.KEROSENE, 12000, "jetpack_fly")
    public static final DeferredItem<Item> JETPACK_FLY = register("jetpack_fly", () ->
            new JetpackRegular(new Item.Properties(), Fluids.KEROSENE, 12000));

    // CE ModItems.java:732 - new JetpackBreak(Fluids.KEROSENE, 12000, "jetpack_break")
    public static final DeferredItem<Item> JETPACK_BREAK = register("jetpack_break", () ->
            new JetpackBreak(new Item.Properties(), Fluids.KEROSENE, 12000));

    // CE ModItems.java:733 - new JetpackVectorized(Fluids.KEROSENE, 16000, "jetpack_vector")
    public static final DeferredItem<Item> JETPACK_VECTOR = register("jetpack_vector", () ->
            new JetpackVectorized(new Item.Properties(), Fluids.KEROSENE, 16000));

    // CE ModItems.java:734 - new JetpackBooster(Fluids.BALEFIRE, 32000, "jetpack_boost")
    public static final DeferredItem<Item> JETPACK_BOOST = register("jetpack_boost", () ->
            new JetpackBooster(new Item.Properties(), Fluids.BALEFIRE, 32000));

    // CE ModItems.java:735 - new JetpackGlider(MaterialRegistry.aMatSteel, -1, EntityEquipmentSlot.CHEST, 20000, "jetpack_glider")
    // The material/-1/CHEST params are dead cruft on the CE side (see JetpackGlider's own javadoc) -
    // only the 20000 capacity carries real behavior.
    public static final DeferredItem<Item> JETPACK_GLIDER = register("jetpack_glider", () ->
            new JetpackGlider(new Item.Properties(), 20000));

    // CE ModItems.java:736-737 — both WingsMurk, CreativeTabs.COMBAT, stacksTo(1).
    // Client model TODO(CE: WingsMurk.java:27-42).
    public static final DeferredItem<Item> WINGS_LIMP = register("wings_limp", () ->
            new WingsMurk(new Item.Properties().stacksTo(1), false));
    public static final DeferredItem<Item> WINGS_MURK = register("wings_murk", () ->
            new WingsMurk(new Item.Properties().stacksTo(1), true));
}
