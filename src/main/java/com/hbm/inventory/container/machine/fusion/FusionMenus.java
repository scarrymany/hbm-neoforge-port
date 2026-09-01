package com.hbm.inventory.container.machine.fusion;

import com.hbm.blockentity.machine.fusion.IcfPressBlockEntity;
import com.hbm.blockentity.machine.fusion.IcfReactorBlockEntity;
import com.hbm.blockentity.machine.fusion.PlasmaForgeBlockEntity;
import com.hbm.blockentity.machine.fusion.WatzReactorBlockEntity;
import com.hbm.inventory.container.ModMenuTypes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * {@link MenuType} registrations for the ICF/Watz fusion-reactor family - see
 * {@code ModMenuTypes}'s javadoc for why this is a separate class (avoids every parallel Phase 2
 * area racing on one shared file) and {@code ChemIsotopeMenus} for the precedent.
 */
public final class FusionMenus {

    public static DeferredHolder<MenuType<?>, MenuType<IcfReactorMenu>> ICF_REACTOR;
    public static DeferredHolder<MenuType<?>, MenuType<IcfPressMenu>> ICF_PRESS;
    public static DeferredHolder<MenuType<?>, MenuType<WatzReactorMenu>> WATZ_REACTOR;
    public static DeferredHolder<MenuType<?>, MenuType<PlasmaForgeMenu>> FUSION_PLASMA_FORGE;

    private FusionMenus() {
    }

    public static void registerAll() {
        ICF_REACTOR = reg("machine_icf_reactor", (id, inv, buf) ->
                new IcfReactorMenu(id, inv, (IcfReactorBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        ICF_PRESS = reg("machine_icf_press", (id, inv, buf) ->
                new IcfPressMenu(id, inv, (IcfPressBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        WATZ_REACTOR = reg("machine_watz_reactor", (id, inv, buf) ->
                new WatzReactorMenu(id, inv, (WatzReactorBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        FUSION_PLASMA_FORGE = reg("fusion_plasma_forge", (id, inv, buf) ->
                new PlasmaForgeMenu(id, inv, (PlasmaForgeBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> reg(String name, IContainerFactory<T> factory) {
        return ModMenuTypes.MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
