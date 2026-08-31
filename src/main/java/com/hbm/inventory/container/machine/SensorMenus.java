package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineRadarBlockEntity;
import com.hbm.inventory.container.ModMenuTypes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class SensorMenus {

    public static DeferredHolder<MenuType<?>, MenuType<RadarMenu>> MACHINE_RADAR;

    private SensorMenus() {
    }

    public static void registerAll() {
        MACHINE_RADAR = reg("machine_radar", (id, inv, buf) ->
                new RadarMenu(id, inv, (MachineRadarBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> reg(String name, IContainerFactory<T> factory) {
        return ModMenuTypes.MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
