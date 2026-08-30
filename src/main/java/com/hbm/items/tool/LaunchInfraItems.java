package com.hbm.items.tool;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ItemBase;
import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 * Item registration for {@code docs/phase3/missile_launch_infra.md}'s designator/sat-interface/
 * silo-hatch-security item family: the three designator items ({@link ItemDesignator}/
 * {@link ItemDesignatorRange}/{@link ItemDesignatorManual}), the two satellite remote items
 * ({@link ItemSatDesignator}, and {@link ItemSatInterface} registered twice as {@code sat_interface}/
 * {@code sat_coord} matching CE's own two-instances-of-one-class shape), and the three plain
 * {@code TileEntityLaunchPadRusted} security items ({@code launch_code_piece}/{@code launch_code}/
 * {@code launch_key}). Registry ids and creative tabs match CE's real {@code ModItems.java}
 * declarations exactly (confirmed by direct read - see {@code missile_launch_infra.md}'s "Key
 * design/API decisions").
 * <p>
 * <b>Not registered here</b> (already real, already registered elsewhere - confirmed by grep before
 * writing this class, see that class's own javadoc): the nine {@code ItemSatChip} legacy satellite
 * items ({@code sat_mapper}/{@code sat_scanner}/{@code sat_radar}/{@code sat_laser}/{@code sat_foeq}/
 * {@code sat_resonator}/{@code sat_miner}/{@code sat_lunar_miner}/{@code sat_gerald}/{@code sat_chip}/
 * {@code sat_relay}) in {@code com.hbm.items.machine.MachineItems}, and the fourteen
 * {@code ItemSatellite} payload-module instances ({@code satellite_*}, one per {@code EnumSatType})
 * in that same class. {@code ItemDesignatorArtyRange} is explicitly out of this package's scope per
 * the research report (turret-targeting content, ported alongside whichever package owns
 * {@code com.hbm.tileentity.turret}).
 */
public final class LaunchInfraItems {

    public static DeferredItem<ItemDesignator> DESIGNATOR;
    public static DeferredItem<ItemDesignatorRange> DESIGNATOR_RANGE;
    public static DeferredItem<ItemDesignatorManual> DESIGNATOR_MANUAL;

    public static DeferredItem<ItemSatDesignator> SAT_DESIGNATOR;
    public static DeferredItem<ItemSatInterface> SAT_INTERFACE;
    public static DeferredItem<ItemSatInterface> SAT_COORD;

    public static DeferredItem<ItemBase> LAUNCH_CODE_PIECE;
    public static DeferredItem<ItemBase> LAUNCH_CODE;
    public static DeferredItem<ItemBase> LAUNCH_KEY;

    private LaunchInfraItems() {
    }

    public static void registerAll() {
        DESIGNATOR = missile("designator", () -> new ItemDesignator(new Item.Properties().stacksTo(1)));
        DESIGNATOR_RANGE = missile("designator_range", () -> new ItemDesignatorRange(new Item.Properties().stacksTo(1)));
        DESIGNATOR_MANUAL = missile("designator_manual", () -> new ItemDesignatorManual(new Item.Properties().stacksTo(1)));

        SAT_DESIGNATOR = missile("sat_designator", () -> new ItemSatDesignator("satchip.designator", new Item.Properties().stacksTo(1)));
        SAT_INTERFACE = missile("sat_interface", () -> new ItemSatInterface("satchip.interface", new Item.Properties().stacksTo(1), false));
        SAT_COORD = missile("sat_coord", () -> new ItemSatInterface("satchip.coord", new Item.Properties().stacksTo(1), true));

        LAUNCH_CODE_PIECE = parts("launch_code_piece", () -> new ItemBase(new Item.Properties().stacksTo(1)));
        LAUNCH_CODE = parts("launch_code", () -> new ItemBase(new Item.Properties().stacksTo(1)));
        LAUNCH_KEY = parts("launch_key", () -> new ItemBase(new Item.Properties().stacksTo(1)));
    }

    private static <T extends Item> DeferredItem<T> missile(String name, Supplier<T> factory) {
        DeferredItem<T> item = ModItems.ITEMS.register(name, factory);
        CreativeTabContents.add(ModCreativeTabs.MISSILE, item);
        return item;
    }

    private static <T extends Item> DeferredItem<T> parts(String name, Supplier<T> factory) {
        DeferredItem<T> item = ModItems.ITEMS.register(name, factory);
        CreativeTabContents.add(ModCreativeTabs.PARTS, item);
        return item;
    }
}
