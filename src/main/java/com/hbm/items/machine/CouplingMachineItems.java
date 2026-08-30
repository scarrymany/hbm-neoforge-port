package com.hbm.items.machine;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 * Registration for every {@code com.hbm.items.machine} item this Phase 2 machine-coupling pass adds
 * - the 8 {@code docs/phase1/items_machine.md} "Defer to Phase 2" items
 * {@code docs/phase2/items_tool_machine_coupling_and_recipe_system.md} surveyed
 * ({@code IItemFluidIdentifier} is an interface, not a registered item - see {@link ItemFluidIDMulti},
 * its sole implementor), minus {@code ItemRBMKRod} - already ported this same wave by the RBMK
 * column-blocks package's own {@code com.hbm.items.machine.rbmk.RBMKRods}. Deliberately a new,
 * separate registration class rather than an edit to {@code MachineItems.java} (Phase 1,
 * already-committed/reviewed) - registers straight into the shared {@link ModItems#ITEMS}
 * {@code DeferredRegister}.
 * <p>
 * Wiring: exactly one call from {@code ModItems.register()} -
 * {@code CouplingMachineItems.registerAll();} - is needed (see this task's wiring notes); no other
 * shared file needs a direct edit.
 */
public final class CouplingMachineItems {

    public static DeferredItem<ItemFFFluidDuct> FF_FLUID_DUCT;
    public static DeferredItem<ItemFluidIDMulti> FLUID_ID_MULTI;
    public static DeferredItem<ItemFluidSiphon> FLUID_SIPHON;
    public static DeferredItem<ItemMuffler> MUFFLER;
    public static DeferredItem<ItemPWRPrinter> PWR_PRINTER;
    public static DeferredItem<ItemReactorSensor> REACTOR_SENSOR;

    private CouplingMachineItems() {
    }

    public static void registerAll() {
        FF_FLUID_DUCT = reg("ff_fluid_duct", () -> new ItemFFFluidDuct(new Item.Properties()));
        FLUID_ID_MULTI = reg("fluid_id_multi", () -> new ItemFluidIDMulti(new Item.Properties().stacksTo(1)));
        FLUID_SIPHON = reg("fluid_siphon", () -> new ItemFluidSiphon(new Item.Properties()));
        MUFFLER = reg("muffler", () -> new ItemMuffler(new Item.Properties()));
        PWR_PRINTER = reg("pwr_printer", () -> new ItemPWRPrinter(new Item.Properties()));
        REACTOR_SENSOR = reg("reactor_sensor", () -> new ItemReactorSensor(new Item.Properties()));

        CreativeTabContents.add(ModCreativeTabs.MACHINE, FF_FLUID_DUCT);
        CreativeTabContents.add(ModCreativeTabs.MACHINE, FLUID_ID_MULTI);
        CreativeTabContents.add(ModCreativeTabs.MACHINE, FLUID_SIPHON);
        CreativeTabContents.add(ModCreativeTabs.MACHINE, MUFFLER);
        CreativeTabContents.add(ModCreativeTabs.MACHINE, PWR_PRINTER);
        CreativeTabContents.add(ModCreativeTabs.MACHINE, REACTOR_SENSOR);
    }

    private static <T extends Item> DeferredItem<T> reg(String name, Supplier<T> factory) {
        return ModItems.ITEMS.register(name, factory);
    }
}
