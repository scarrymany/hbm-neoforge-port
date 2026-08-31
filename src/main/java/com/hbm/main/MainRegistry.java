package com.hbm.main;

import com.hbm.blockentity.machine.StorageBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.capability.ModAttachments;
import com.hbm.capability.ModCapabilities;
import com.hbm.config.HbmConfig;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.entity.ConveyorEntityTypes;
import com.hbm.entity.GunEntityTypes;
import com.hbm.entity.cart.CartEntityTypes;
import com.hbm.entity.effect.EffectEntityTypes;
import com.hbm.entity.effect.GravityWellEntityTypes;
import com.hbm.entity.grenade.GrenadeEntityTypes;
import com.hbm.entity.item.DroneEntityTypes;
import com.hbm.entity.item.ParachuteCrateEntityTypes;
import com.hbm.entity.item.TntPrimedEntityTypes;
import com.hbm.entity.logic.NukeEntityTypes;
import com.hbm.entity.logic.PlaneEntityTypes;
import com.hbm.entity.logic.SatellitePayloadEntityTypes;
import com.hbm.entity.missile.MissileEntityTypes;
import com.hbm.entity.mob.CreeperVariantEntityTypes;
import com.hbm.entity.mob.MaskmanEntityTypes;
import com.hbm.entity.mob.Phase4BossEntityTypes2;
import com.hbm.entity.mob.RadBeastEntityTypes;
import com.hbm.entity.mob.WormEntityTypes;
import com.hbm.entity.projectile.ChopperMineEntityTypes;
import com.hbm.entity.projectile.FallingNukeEntityTypes;
import com.hbm.entity.projectile.MeteorEntityTypes;
import com.hbm.entity.projectile.RubbleEntityTypes;
import com.hbm.entity.train.TrainEntityTypes;
import com.hbm.hazard.HazardComponents;
import com.hbm.inventory.container.ModMenuTypes;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.HbmRecipes;
import com.hbm.items.HbmDataComponents;
import com.hbm.items.ModItems;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.potion.HbmPotionEffects;
import com.hbm.sound.ModSounds;
import com.hbm.world.gen.OilMeteorWorldGenFeatures;
import com.hbm.world.gen.OreWorldGenFeatures;
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
        // Phase 4 (entities_creeper_variants): must run before ModItems.register(modEventBus) below -
        // this registry also adds this package's spawn-egg items into ModItems.ITEMS.
        CreeperVariantEntityTypes.register(modEventBus);
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
        // Phase 4 (World & simulation) entity/feature families.
        WormEntityTypes.register(modEventBus);
        MaskmanEntityTypes.register(modEventBus);
        Phase4BossEntityTypes2.register(modEventBus);
        RadBeastEntityTypes.register(modEventBus);
        ChopperMineEntityTypes.register(modEventBus);
        GravityWellEntityTypes.register(modEventBus);
        SatellitePayloadEntityTypes.register(modEventBus);
        MeteorEntityTypes.register(modEventBus);
        TrainEntityTypes.register(modEventBus);
        PlaneEntityTypes.register(modEventBus);
        ParachuteCrateEntityTypes.register(modEventBus);
        CartEntityTypes.register(modEventBus);
        DroneEntityTypes.register(modEventBus);
        OreWorldGenFeatures.register(modEventBus);
        OilMeteorWorldGenFeatures.register(modEventBus);
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
