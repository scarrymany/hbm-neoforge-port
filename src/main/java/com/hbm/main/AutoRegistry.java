package com.hbm.main;

import com.hbm.blockentity.machine.MachineDieselBlockEntity;
import com.hbm.blockentity.machine.MachineIndustrialTurbineBlockEntity;
import com.hbm.blockentity.machine.MachineSteamEngineBlockEntity;
import com.hbm.blockentity.machine.chem.CentrifugeBlockEntity;
import com.hbm.blockentity.machine.dummyable.CondenserBlockEntity;
import com.hbm.blockentity.machine.dummyable.CondenserPoweredBlockEntity;
import com.hbm.blockentity.machine.dummyable.MachineChungusBlockEntity;
import com.hbm.config.MachineDynConfig;
import com.hbm.tileentity.IConfigurableMachine;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal Exact CE {@code AutoRegistry} slice for {@link MachineDynConfig}.
 * Full annotation-processor registrar is not ported. NeoForge BEs have no no-arg ctor —
 * each machine registers a {@code ConfigDummy} with Exact CE {@code writeConfig} keys.
 */
public final class AutoRegistry {

    public static final List<Class<? extends IConfigurableMachine>> configurableMachineClasses = new ArrayList<>();

    private AutoRegistry() {}

    public static void loadAuxiliaryData() {
        if (configurableMachineClasses.isEmpty()) {
            configurableMachineClasses.add(MachineChungusBlockEntity.ConfigDummy.class);
            configurableMachineClasses.add(CentrifugeBlockEntity.ConfigDummy.class);
            configurableMachineClasses.add(MachineDieselBlockEntity.ConfigDummy.class);
            configurableMachineClasses.add(MachineSteamEngineBlockEntity.ConfigDummy.class);
            configurableMachineClasses.add(MachineIndustrialTurbineBlockEntity.ConfigDummy.class);
            configurableMachineClasses.add(CondenserBlockEntity.ConfigDummy.class);
            configurableMachineClasses.add(CondenserBlockEntity.ConfigDummyTowerSmall.class);
            configurableMachineClasses.add(CondenserBlockEntity.ConfigDummyTowerLarge.class);
            configurableMachineClasses.add(CondenserPoweredBlockEntity.ConfigDummy.class);
        }
        MachineDynConfig.initialize();
        MainRegistry.logger.debug("Successfully loaded {} configurable machine classes.", configurableMachineClasses.size());
    }
}
