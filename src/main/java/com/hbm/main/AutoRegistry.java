package com.hbm.main;

import com.hbm.blockentity.machine.dummyable.MachineChungusBlockEntity;
import com.hbm.config.MachineDynConfig;
import com.hbm.tileentity.IConfigurableMachine;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal Exact CE {@code AutoRegistry} slice for {@link MachineDynConfig}.
 * Full annotation-processor registrar is not ported. NeoForge BEs have no no-arg ctor —
 * Chungus registers {@link MachineChungusBlockEntity.ConfigDummy} (same schema as CE
 * {@code TileEntityChungus.java:69-86}).
 */
public final class AutoRegistry {

    public static final List<Class<? extends IConfigurableMachine>> configurableMachineClasses = new ArrayList<>();

    private AutoRegistry() {}

    public static void loadAuxiliaryData() {
        if (configurableMachineClasses.isEmpty()) {
            configurableMachineClasses.add(MachineChungusBlockEntity.ConfigDummy.class);
        }
        MachineDynConfig.initialize();
        MainRegistry.logger.debug("Successfully loaded {} configurable machine classes.", configurableMachineClasses.size());
    }
}
