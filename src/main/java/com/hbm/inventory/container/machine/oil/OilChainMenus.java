package com.hbm.inventory.container.machine.oil;

import com.hbm.blockentity.machine.oil.MachineRefineryBlockEntity;
import com.hbm.blockentity.machine.oil.OilDrillBaseBlockEntity;
import com.hbm.inventory.container.ModMenuTypes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * {@link MenuType} registrations for the oil chain's two GUI-bearing families - one shared by all
 * three extractors ({@link MachineOilWellMenu}), one for the refinery ({@link MachineRefineryMenu}) -
 * matching {@code docs/phase2/oil_production_chain.md}'s own count ("this area needs exactly two GUI
 * pairs, not four"). Mirrors {@code PowerGenMenus}' established shape (a separate class appending to
 * the shared {@link ModMenuTypes#MENU_TYPES} {@code DeferredRegister} directly, rather than editing
 * that file - see that class's own javadoc for the multi-agent-race rationale).
 */
public final class OilChainMenus {

    public static DeferredHolder<MenuType<?>, MenuType<MachineOilWellMenu>> MACHINE_OIL_WELL;
    public static DeferredHolder<MenuType<?>, MenuType<MachineRefineryMenu>> MACHINE_REFINERY;

    private OilChainMenus() {
    }

    public static void registerAll() {
        MACHINE_OIL_WELL = reg("machine_oil_well", (id, inv, buf) ->
                new MachineOilWellMenu(id, inv, (OilDrillBaseBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_REFINERY = reg("machine_refinery", (id, inv, buf) ->
                new MachineRefineryMenu(id, inv, (MachineRefineryBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> reg(String name, IContainerFactory<T> factory) {
        return ModMenuTypes.MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
