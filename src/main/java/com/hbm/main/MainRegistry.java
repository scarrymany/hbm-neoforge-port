package com.hbm.main;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MainRegistry.MODID)
public class MainRegistry {

    public static final String MODID = "hbm";
    public static final Logger logger = LoggerFactory.getLogger(MODID);

    public MainRegistry(IEventBus modEventBus, ModContainer modContainer) {
        logger.info("HBM's Nuclear Tech - Community Edition (NeoForge port) initializing");
    }
}
