package com.hbm.inventory.container.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.FurnaceCombinationBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineAnnihilatorBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineBlastFurnaceBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineFractionTowerBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachinePressBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineRockMillBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineRotaryFurnaceBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineCatalyticCrackerBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineCatalyticReformerBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineCokerBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineCompressorBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineHydrotreaterBlockEntity;
import com.hbm.blockentity.machine.dummyable.WasteDrumBlockEntity;
import com.hbm.inventory.container.ModMenuTypes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class DummyableProcessMenus {

    public static DeferredHolder<MenuType<?>, MenuType<FurnaceCombinationMenu>> FURNACE_COMBINATION;
    public static DeferredHolder<MenuType<?>, MenuType<BlastFurnaceMenu>> MACHINE_BLAST_FURNACE;
    public static DeferredHolder<MenuType<?>, MenuType<RockMillMenu>> MACHINE_ROCK_MILL;
    public static DeferredHolder<MenuType<?>, MenuType<AnnihilatorMenu>> MACHINE_ANNIHILATOR;
    public static DeferredHolder<MenuType<?>, MenuType<PressMenu>> MACHINE_PRESS;
    public static DeferredHolder<MenuType<?>, MenuType<RotaryFurnaceMenu>> MACHINE_ROTARY_FURNACE;
    public static DeferredHolder<MenuType<?>, MenuType<FractionTowerMenu>> MACHINE_FRACTION_TOWER;
    public static DeferredHolder<MenuType<?>, MenuType<WasteDrumMenu>> WASTE_DRUM;
    public static DeferredHolder<MenuType<?>, MenuType<CompressorMenu>> MACHINE_COMPRESSOR;
    public static DeferredHolder<MenuType<?>, MenuType<CokerMenu>> MACHINE_COKER;
    public static DeferredHolder<MenuType<?>, MenuType<CatalyticCrackerMenu>> MACHINE_CATALYTIC_CRACKER;
    public static DeferredHolder<MenuType<?>, MenuType<CatalyticReformerMenu>> MACHINE_CATALYTIC_REFORMER;
    public static DeferredHolder<MenuType<?>, MenuType<HydrotreaterMenu>> MACHINE_HYDROTREATER;

    private DummyableProcessMenus() {
    }

    public static void registerAll() {
        FURNACE_COMBINATION = reg("furnace_combination", (id, inv, buf) ->
                new FurnaceCombinationMenu(id, inv, (FurnaceCombinationBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_BLAST_FURNACE = reg("machine_blast_furnace", (id, inv, buf) ->
                new BlastFurnaceMenu(id, inv, (MachineBlastFurnaceBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_ROCK_MILL = reg("machine_rock_mill", (id, inv, buf) ->
                new RockMillMenu(id, inv, (MachineRockMillBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_ANNIHILATOR = reg("machine_annihilator", (id, inv, buf) ->
                new AnnihilatorMenu(id, inv, (MachineAnnihilatorBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_PRESS = reg("machine_press", (id, inv, buf) ->
                new PressMenu(id, inv, (MachinePressBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_ROTARY_FURNACE = reg("machine_rotary_furnace", (id, inv, buf) ->
                new RotaryFurnaceMenu(id, inv, (MachineRotaryFurnaceBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_FRACTION_TOWER = reg("machine_fraction_tower", (id, inv, buf) ->
                new FractionTowerMenu(id, inv, (MachineFractionTowerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        WASTE_DRUM = reg("machine_waste_drum", (id, inv, buf) ->
                new WasteDrumMenu(id, inv, (WasteDrumBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_COMPRESSOR = reg("machine_compressor", (id, inv, buf) ->
                new CompressorMenu(id, inv, (MachineCompressorBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_COKER = reg("machine_coker", (id, inv, buf) ->
                new CokerMenu(id, inv, (MachineCokerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_CATALYTIC_CRACKER = reg("machine_catalytic_cracker", (id, inv, buf) ->
                new CatalyticCrackerMenu(id, inv, (MachineCatalyticCrackerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_CATALYTIC_REFORMER = reg("machine_catalytic_reformer", (id, inv, buf) ->
                new CatalyticReformerMenu(id, inv, (MachineCatalyticReformerBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        MACHINE_HYDROTREATER = reg("machine_hydrotreater", (id, inv, buf) ->
                new HydrotreaterMenu(id, inv, (MachineHydrotreaterBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> reg(String name, IContainerFactory<T> factory) {
        return ModMenuTypes.MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
