package com.hbm.inventory.container.machine.reprocess;

import com.hbm.blockentity.machine.reprocess.LiquefactorBlockEntity;
import com.hbm.blockentity.machine.reprocess.PurexBlockEntity;
import com.hbm.inventory.container.ModMenuTypes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ReprocessMenus {

    public static DeferredHolder<MenuType<?>, MenuType<PurexMenu>> MACHINE_PUREX;
    public static DeferredHolder<MenuType<?>, MenuType<LiquefactorMenu>> MACHINE_LIQUEFACTOR;

    private ReprocessMenus() {
    }

    public static void registerAll() {
        MACHINE_PUREX = reg("machine_purex", (id, inv, buf) ->
                new PurexMenu(id, inv, (PurexBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_LIQUEFACTOR = reg("machine_liquefactor", (id, inv, buf) ->
                new LiquefactorMenu(id, inv, (LiquefactorBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> reg(String name, IContainerFactory<T> factory) {
        return ModMenuTypes.MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
