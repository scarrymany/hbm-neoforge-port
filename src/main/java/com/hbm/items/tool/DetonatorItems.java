package com.hbm.items.tool;

import com.hbm.api.block.IToolable.ToolType;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 * Item registration for the {@code docs/phase3/bomb_blocks_and_detonators.md} Section C detonator/
 * defuser family: {@link ItemDetonator}, {@link ItemMultiDetonator}, {@link ItemLaserDetonator},
 * {@link ItemDefuser} (registered twice, CE's {@code defuser}/{@code defuser_desh}). New,
 * separate registration class - same one-call-from-{@code ModItems.register()} pattern as
 * {@code NetworkToolItems}/{@code CouplingToolItems}.
 *
 * <p>CE's {@code ModItems.java} puts all five of these on {@code MainRegistry.nukeTab} (each
 * constructor's own {@code setCreativeTab(MainRegistry.controlTab)} call is overridden by a later
 * {@code .setCreativeTab(MainRegistry.nukeTab)} at the declaration site - confirmed by reading both
 * the item source and the real {@code ModItems.java} declarations) - mapped here onto
 * {@link ModCreativeTabs#NUKE}, not {@link ModCreativeTabs#CONTROL}.
 *
 * <p>Not registered here (CE names them in the same block of {@code ModItems.java} but they are
 * unrelated {@code ItemDrop} crafting/quest items, not detonator behavior, and are outside this
 * task's named scope): {@code detonator_deadman}, {@code detonator_de}.
 */
public final class DetonatorItems {

    public static DeferredItem<ItemDetonator> DETONATOR;
    public static DeferredItem<ItemMultiDetonator> DETONATOR_MULTI;
    public static DeferredItem<ItemLaserDetonator> DETONATOR_LASER;
    public static DeferredItem<ItemDefuser> DEFUSER;
    public static DeferredItem<ItemDefuser> DEFUSER_DESH;

    private DetonatorItems() {
    }

    public static void registerAll() {
        DETONATOR = reg("detonator", () -> new ItemDetonator(new Item.Properties().stacksTo(1)));
        DETONATOR_MULTI = reg("detonator_multi", () -> new ItemMultiDetonator(new Item.Properties().stacksTo(1)));
        DETONATOR_LASER = reg("detonator_laser", () -> new ItemLaserDetonator(new Item.Properties().stacksTo(1)));

        // CE: `new ItemDefuser(ToolType.DEFUSER, 100, "defuser")` / `(..., -1, "defuser_desh")`.
        // -1 ("infinite") maps onto this port's own established 1024-durability convention - see
        // CouplingToolItems' screwdriver_desh/hand_drill_desh, the precedent this follows.
        DEFUSER = reg("defuser", () -> new ItemDefuser(ToolType.DEFUSER, new Item.Properties().stacksTo(1).durability(100)));
        DEFUSER_DESH = reg("defuser_desh", () -> new ItemDefuser(ToolType.DEFUSER, new Item.Properties().stacksTo(1).durability(1024)));

        CreativeTabContents.add(ModCreativeTabs.NUKE, DETONATOR);
        CreativeTabContents.add(ModCreativeTabs.NUKE, DETONATOR_MULTI);
        CreativeTabContents.add(ModCreativeTabs.NUKE, DETONATOR_LASER);
        CreativeTabContents.add(ModCreativeTabs.NUKE, DEFUSER);
        CreativeTabContents.add(ModCreativeTabs.NUKE, DEFUSER_DESH);
    }

    private static <T extends Item> DeferredItem<T> reg(String name, Supplier<T> factory) {
        return ModItems.ITEMS.register(name, factory);
    }
}
