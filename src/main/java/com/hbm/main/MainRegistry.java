package com.hbm.main;

import com.hbm.blockentity.machine.StorageBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.capability.ModAttachments;
import com.hbm.capability.ModCapabilities;
import com.hbm.config.HbmConfig;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.entity.ConveyorEntityTypes;
import com.hbm.entity.GunEntityTypes;
import com.hbm.entity.effect.EffectEntityTypes;
import com.hbm.entity.grenade.GrenadeEntityTypes;
import com.hbm.entity.item.TntPrimedEntityTypes;
import com.hbm.entity.logic.NukeEntityTypes;
import com.hbm.entity.missile.MissileEntityTypes;
import com.hbm.entity.projectile.FallingNukeEntityTypes;
import com.hbm.entity.projectile.RubbleEntityTypes;
import com.hbm.hazard.HazardComponents;
import com.hbm.inventory.container.ModMenuTypes;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.HbmRecipes;
import com.hbm.items.HbmDataComponents;
import com.hbm.items.ModItems;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.potion.HbmPotionEffects;
import com.hbm.sound.ModSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@Mod(MainRegistry.MODID)
public class MainRegistry {

    public static final String MODID = "hbm";
    public static final Logger logger = LoggerFactory.getLogger(MODID);

    public static ServerProxy proxy;

    public static File configDir;
    public static File configHbmDir;

    public MainRegistry(IEventBus modEventBus, ModContainer modContainer) {
        logger.info("HBM's Nuclear Tech - Community Edition (NeoForge port) initializing");

        proxy = FMLLoader.getDist().isClient() ? new ClientProxy() : new ServerProxy();

        configDir = FMLPaths.CONFIGDIR.get().toFile();
        configHbmDir = new File(configDir, "hbmConfig");
        if(!configHbmDir.exists()) configHbmDir.mkdirs();

        HbmConfig.register(modContainer);

        HBMSoundHandler.register(modEventBus);
        ModSounds.register(modEventBus);
        HbmPotionEffects.register(modEventBus);
        HazardComponents.register(modEventBus);
        ModAttachments.register(modEventBus);
        modEventBus.addListener(ModCapabilities::register);
        // Block-entity capability counterpart to the item-capability listener above - see
        // StorageBlockEntities' own javadoc (Phase 2 storage-machines package).
        modEventBus.addListener(StorageBlockEntities::registerCapabilities);
        ModItems.register(modEventBus);
        HbmDataComponents.register(modEventBus);
        ModBlocks.register(modEventBus);
        ConveyorEntityTypes.register(modEventBus);
        NukeEntityTypes.register(modEventBus);
        EffectEntityTypes.register(modEventBus);
        FallingNukeEntityTypes.register(modEventBus);
        GunEntityTypes.register(modEventBus);
        TntPrimedEntityTypes.register(modEventBus);
        RubbleEntityTypes.register(modEventBus);
        GrenadeEntityTypes.register(modEventBus);
        MissileEntityTypes.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        MaterialRegistry.register(modEventBus);
        HbmRecipes.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        com.hbm.inventory.container.machine.rbmk.RBMKMenuTypes.register(modEventBus);

        Fluids.init();
        // Populates the data-driven Solinium block-swap table (defaults to empty/no-op otherwise).
        com.hbm.explosion.ExplosionNukeGeneric.loadSoliniumFromFile();
    }
}
