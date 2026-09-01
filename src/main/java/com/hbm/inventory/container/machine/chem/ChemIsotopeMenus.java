package com.hbm.inventory.container.machine.chem;

import com.hbm.blockentity.machine.chem.CentrifugeBlockEntity;
import com.hbm.blockentity.machine.chem.ChemPlantBlockEntity;
import com.hbm.blockentity.machine.chem.CyclotronBlockEntity;
import com.hbm.blockentity.machine.chem.ElectrolyserBlockEntity;
import com.hbm.blockentity.machine.chem.GasCentrifugeBlockEntity;
import com.hbm.blockentity.machine.chem.SilexBlockEntity;
import com.hbm.inventory.container.ModMenuTypes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * {@link MenuType} registrations for the chemical-plant/centrifuge/gas-centrifuge/cyclotron/SILEX/
 * electrolyser family - see {@link ModMenuTypes}'s javadoc for why this is a separate class (avoids
 * every parallel Phase 2 area racing on one shared file) and {@code PowerGenMenus} for the precedent.
 */
public final class ChemIsotopeMenus {

    public static DeferredHolder<MenuType<?>, MenuType<CentrifugeMenu>> CENTRIFUGE;
    public static DeferredHolder<MenuType<?>, MenuType<GasCentrifugeMenu>> GAS_CENTRIFUGE;
    public static DeferredHolder<MenuType<?>, MenuType<SilexMenu>> SILEX;
    public static DeferredHolder<MenuType<?>, MenuType<CyclotronMenu>> CYCLOTRON;
    public static DeferredHolder<MenuType<?>, MenuType<ChemPlantMenu>> CHEM_PLANT;
    public static DeferredHolder<MenuType<?>, MenuType<ElectrolyserMenu>> ELECTROLYSER;
    public static DeferredHolder<MenuType<?>, MenuType<ElectrolyserMetalMenu>> ELECTROLYSER_METAL;

    private ChemIsotopeMenus() {
    }

    public static void registerAll() {
        CENTRIFUGE = reg("machine_centrifuge", (id, inv, buf) ->
                new CentrifugeMenu(id, inv, (CentrifugeBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        GAS_CENTRIFUGE = reg("machine_gascent", (id, inv, buf) ->
                new GasCentrifugeMenu(id, inv, (GasCentrifugeBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        SILEX = reg("machine_silex", (id, inv, buf) ->
                new SilexMenu(id, inv, (SilexBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        CYCLOTRON = reg("machine_cyclotron", (id, inv, buf) ->
                new CyclotronMenu(id, inv, (CyclotronBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        CHEM_PLANT = reg("machine_chemical_plant", (id, inv, buf) ->
                new ChemPlantMenu(id, inv, (ChemPlantBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        ELECTROLYSER = reg("machine_electrolyser", (id, inv, buf) ->
                new ElectrolyserMenu(id, inv, (ElectrolyserBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
        ELECTROLYSER_METAL = reg("machine_electrolyser_metal", (id, inv, buf) ->
                new ElectrolyserMetalMenu(id, inv, (ElectrolyserBlockEntity) inv.player.level().getBlockEntity(buf.readBlockPos())));
    }

    private static <T extends AbstractContainerMenu> DeferredHolder<MenuType<?>, MenuType<T>> reg(String name, IContainerFactory<T> factory) {
        return ModMenuTypes.MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory));
    }
}
