package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineReactorBreedingBlockEntity;
import com.hbm.blockentity.machine.PWRControllerBlockEntity;
import com.hbm.inventory.container.ModMenuTypes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * {@link MenuType} registrations for this PWR/breeding-reactor package's two GUI-bearing machines -
 * see {@code PowerGenMenus}'/{@code ProcessingMenus}' own javadoc for why this appends directly to
 * {@link ModMenuTypes#MENU_TYPES} instead of editing that shared file or owning a second
 * {@code DeferredRegister} (many Phase 2 machine areas landing in the same wave). Called from
 * {@code PWRBlocks#registerAll()}, the single call site this whole family needs wired into
 * {@code ModBlocks.register()}.
 */
public final class PWRMenus {

    public static DeferredHolder<MenuType<?>, MenuType<PWRControllerMenu>> PWR_CONTROLLER;
    public static DeferredHolder<MenuType<?>, MenuType<MachineReactorBreedingMenu>> REACTOR_BREEDING;

    private PWRMenus() {
    }

    public static void registerAll() {
        PWR_CONTROLLER = reg("pwr_controller", (id, inv, buf) ->
                new PWRControllerMenu(id, inv, (PWRControllerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        REACTOR_BREEDING = reg("machine_reactor_breeding", (id, inv, buf) ->
                new MachineReactorBreedingMenu(id, inv, (MachineReactorBreedingBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> reg(String name, IContainerFactory<T> factory) {
        return ModMenuTypes.MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
