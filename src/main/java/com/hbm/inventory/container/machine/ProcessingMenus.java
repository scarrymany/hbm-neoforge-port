package com.hbm.inventory.container.machine;

import com.hbm.blockentity.machine.MachineAssemblyMachineBlockEntity;
import com.hbm.blockentity.machine.MachineCrystallizerBlockEntity;
import com.hbm.blockentity.machine.MachineMixerBlockEntity;
import com.hbm.blockentity.machine.MachineShredderBlockEntity;
import com.hbm.inventory.container.ModMenuTypes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * {@link MenuType} registrations for the shredder/assembler/crystallizer/mixer family - see
 * {@code PowerGenMenus}' own javadoc for why this appends directly to
 * {@link ModMenuTypes#MENU_TYPES} instead of owning a second {@code DeferredRegister} or editing
 * {@code ModMenuTypes} itself (the same "many Phase 2 areas landing in the same wave" race this
 * class avoids identically).
 */
public final class ProcessingMenus {

    public static DeferredHolder<MenuType<?>, MenuType<MachineShredderMenu>> MACHINE_SHREDDER;
    public static DeferredHolder<MenuType<?>, MenuType<MachineAssemblyMachineMenu>> MACHINE_ASSEMBLER;
    public static DeferredHolder<MenuType<?>, MenuType<MachineCrystallizerMenu>> MACHINE_CRYSTALLIZER;
    public static DeferredHolder<MenuType<?>, MenuType<MachineMixerMenu>> MACHINE_MIXER;

    private ProcessingMenus() {
    }

    public static void registerAll() {
        MACHINE_SHREDDER = reg("machine_shredder", (id, inv, buf) ->
                new MachineShredderMenu(id, inv, (MachineShredderBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_ASSEMBLER = reg("machine_assembly_machine", (id, inv, buf) ->
                new MachineAssemblyMachineMenu(id, inv, (MachineAssemblyMachineBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_CRYSTALLIZER = reg("machine_crystallizer", (id, inv, buf) ->
                new MachineCrystallizerMenu(id, inv, (MachineCrystallizerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_MIXER = reg("machine_mixer", (id, inv, buf) ->
                new MachineMixerMenu(id, inv, (MachineMixerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> reg(String name, IContainerFactory<T> factory) {
        return ModMenuTypes.MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
