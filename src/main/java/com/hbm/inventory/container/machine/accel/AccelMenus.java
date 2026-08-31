package com.hbm.inventory.container.machine.accel;

import com.hbm.blockentity.machine.accel.ExcavatorBlockEntity;
import com.hbm.blockentity.machine.accel.FelBlockEntity;
import com.hbm.blockentity.machine.accel.PaDetectorBlockEntity;
import com.hbm.blockentity.machine.accel.PaPartBlockEntity;
import com.hbm.inventory.container.ModMenuTypes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class AccelMenus {

    public static DeferredHolder<MenuType<?>, MenuType<FelMenu>> MACHINE_FEL;
    public static DeferredHolder<MenuType<?>, MenuType<ExcavatorMenu>> MACHINE_EXCAVATOR;
    public static DeferredHolder<MenuType<?>, MenuType<PaPartMenu>> PA_PART;
    public static DeferredHolder<MenuType<?>, MenuType<PaDetectorMenu>> PA_DETECTOR;

    private AccelMenus() {
    }

    public static void registerAll() {
        MACHINE_FEL = reg("machine_fel", (id, inv, buf) ->
                new FelMenu(id, inv, (FelBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_EXCAVATOR = reg("machine_excavator", (id, inv, buf) ->
                new ExcavatorMenu(id, inv, (ExcavatorBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        PA_PART = reg("pa_part", (id, inv, buf) ->
                new PaPartMenu(id, inv, (PaPartBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        PA_DETECTOR = reg("pa_detector", (id, inv, buf) ->
                new PaDetectorMenu(id, inv, (PaDetectorBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> reg(String name, IContainerFactory<T> factory) {
        return ModMenuTypes.MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
