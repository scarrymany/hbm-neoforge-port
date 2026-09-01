package com.hbm.inventory.container.machine.workshop;

import com.hbm.blockentity.machine.workshop.AmmoPressBlockEntity;
import com.hbm.blockentity.machine.workshop.ArcWelderBlockEntity;
import com.hbm.blockentity.machine.workshop.SolderingBlockEntity;
import com.hbm.inventory.container.ModMenuTypes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class WorkshopMenus {

    public static DeferredHolder<MenuType<?>, MenuType<AmmoPressMenu>> MACHINE_AMMO_PRESS;
    public static DeferredHolder<MenuType<?>, MenuType<ArcWelderMenu>> MACHINE_ARC_WELDER;
    public static DeferredHolder<MenuType<?>, MenuType<SolderingMenu>> MACHINE_SOLDERING_STATION;

    private WorkshopMenus() {
    }

    public static void registerAll() {
        MACHINE_AMMO_PRESS = reg("machine_ammo_press", (id, inv, buf) ->
                new AmmoPressMenu(id, inv, (AmmoPressBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_ARC_WELDER = reg("machine_arc_welder", (id, inv, buf) ->
                new ArcWelderMenu(id, inv, (ArcWelderBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_SOLDERING_STATION = reg("machine_soldering_station", (id, inv, buf) ->
                new SolderingMenu(id, inv, (SolderingBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> reg(String name, IContainerFactory<T> factory) {
        return ModMenuTypes.MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
