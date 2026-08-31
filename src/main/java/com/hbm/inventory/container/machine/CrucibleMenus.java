package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineCrucibleBlockEntity;
import com.hbm.inventory.container.ModMenuTypes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * {@link MenuType} registration for the Crucible - see {@code ProcessingMenus}' own javadoc for why
 * this appends directly to {@link ModMenuTypes#MENU_TYPES} instead of owning a second
 * {@code DeferredRegister} or editing {@code ModMenuTypes} itself (avoids a shared-file edit while
 * many Phase 7 tasks land in the same wave). {@link #reg(String, IContainerFactory)} mirrors
 * {@code ProcessingMenus}'s own private helper exactly.
 */
public final class CrucibleMenus {

    public static DeferredHolder<MenuType<?>, MenuType<MachineCrucibleMenu>> MACHINE_CRUCIBLE;

    private CrucibleMenus() {
    }

    public static void registerAll() {
        MACHINE_CRUCIBLE = reg("machine_crucible", (id, inv, buf) ->
                new MachineCrucibleMenu(id, inv, (MachineCrucibleBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> reg(String name, IContainerFactory<T> factory) {
        return ModMenuTypes.MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
