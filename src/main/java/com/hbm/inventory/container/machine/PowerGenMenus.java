package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineCombustionEngineBlockEntity;
import com.hbm.blockentity.machine.MachineDieselBlockEntity;
import com.hbm.blockentity.machine.MachineLargeTurbineBlockEntity;
import com.hbm.blockentity.machine.MachineRTGBlockEntity;
import com.hbm.blockentity.machine.MachineTurbineBlockEntity;
import com.hbm.blockentity.machine.MachineTurbineGasBlockEntity;
import com.hbm.inventory.container.ModMenuTypes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * {@link MenuType} registrations for this power-generation package's six GUI-bearing machines
 * (RTG, diesel generator, combustion engine, small/large turbine, gas turbine - the other five
 * machines in this pass have no player-facing inventory, per the research report). Deliberately a
 * <i>separate</i> class from {@code com.hbm.inventory.container.ModMenuTypes} rather than adding
 * fields there directly: that file's own javadoc invites exactly this ("once a machine's
 * {@code MenuBase} subclass exists, register it here"), but with many Phase 2 machine areas landing
 * in the same wave, editing it directly would race with every other area doing the same - this
 * class instead calls the already-public {@link ModMenuTypes#MENU_TYPES} {@code DeferredRegister}
 * field directly (the same "any class may append" contract {@code ModBlocks.BLOCKS}/
 * {@code BLOCK_ENTITY_TYPES} already rely on elsewhere in this port), so zero shared files need a
 * direct edit for this to work - see {@code PowerGenBlocks#registerAll} for the one call that wires
 * this class in.
 */
public final class PowerGenMenus {

    public static DeferredHolder<MenuType<?>, MenuType<MachineRTGMenu>> MACHINE_RTG;
    public static DeferredHolder<MenuType<?>, MenuType<MachineDieselMenu>> MACHINE_DIESEL;
    public static DeferredHolder<MenuType<?>, MenuType<MachineCombustionEngineMenu>> COMBUSTION_ENGINE;
    public static DeferredHolder<MenuType<?>, MenuType<MachineTurbineMenu>> MACHINE_TURBINE;
    public static DeferredHolder<MenuType<?>, MenuType<MachineLargeTurbineMenu>> MACHINE_LARGE_TURBINE;
    public static DeferredHolder<MenuType<?>, MenuType<MachineTurbineGasMenu>> MACHINE_TURBINE_GAS;

    private PowerGenMenus() {
    }

    public static void registerAll() {
        MACHINE_RTG = reg("machine_rtg", (id, inv, buf) ->
                new MachineRTGMenu(id, inv, (MachineRTGBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_DIESEL = reg("machine_diesel", (id, inv, buf) ->
                new MachineDieselMenu(id, inv, (MachineDieselBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        COMBUSTION_ENGINE = reg("machine_combustion_engine", (id, inv, buf) ->
                new MachineCombustionEngineMenu(id, inv, (MachineCombustionEngineBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_TURBINE = reg("machine_turbine", (id, inv, buf) ->
                new MachineTurbineMenu(id, inv, (MachineTurbineBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_LARGE_TURBINE = reg("machine_large_turbine", (id, inv, buf) ->
                new MachineLargeTurbineMenu(id, inv, (MachineLargeTurbineBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_TURBINE_GAS = reg("machine_turbine_gas", (id, inv, buf) ->
                new MachineTurbineGasMenu(id, inv, (MachineTurbineGasBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> reg(String name, IContainerFactory<T> factory) {
        return ModMenuTypes.MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
