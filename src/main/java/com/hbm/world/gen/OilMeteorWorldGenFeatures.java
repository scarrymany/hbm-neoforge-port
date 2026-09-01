package com.hbm.world.gen;

import com.hbm.main.MainRegistry;
import com.hbm.world.feature.AntennaFeature;
import com.hbm.world.feature.BedrockOilDepositFeature;
import com.hbm.world.feature.BunkerFeature;
import com.hbm.world.feature.LandmineFeature;
import com.hbm.world.feature.MeteoriteFeature;
import com.hbm.world.feature.NitanChestFeature;
import com.hbm.world.feature.OilBubbleFeature;
import com.hbm.world.feature.OilSandBubbleFeature;
import com.hbm.world.feature.RadioFeature;
import com.hbm.world.feature.SellafieldFeature;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link DeferredRegister}{@code <Feature<?>>} for
 * docs/phase4/worldgen_oil_and_meteor_dungeons.md's Part 1 (oil-deposit) and Part 2a (ambient
 * meteorite) world-gen - {@link OilBubbleFeature}, {@link BedrockOilDepositFeature},
 * {@link OilSandBubbleFeature}, {@link MeteoriteFeature}. Follows this port's per-family
 * {@code DeferredRegister} template (see {@code com.hbm.entity.effect.EffectEntityTypes}, and this
 * same package's sibling {@code com.hbm.world.gen.OreWorldGenFeatures}).
 * <p>
 * Registered from {@code MainRegistry}'s constructor via {@link #register(IEventBus)} - see this
 * package's own wiringSnippets (protected file, not edited directly).
 * <p>
 * Leftover CE {@code enableDungeons} structures (no generator in this port):
 * TODO(CE: HbmWorldGen.java:347-358) hive — {@code GlyphidHive.generate}, {@code hiveSpawn} 256;
 * TODO(CE: HbmWorldGen.java:367-368) desert-atom — {@code DesertAtom001}, {@code atomStructure} 0:500, {@code !canRain && temp>=2};
 * TODO(CE: HbmWorldGen.java:370-371) barrel — {@code Barrel}, {@code barrelStructure} 0:5000, {@code temp>1.8};
 * TODO(CE: HbmWorldGen.java:373-374) satellite dish — {@code Satellite}, {@code satelliteStructure} 0:500, {@code temp<1 || temp>1.8};
 * TODO(CE: HbmWorldGen.java:377) spaceship — {@code Spaceship}, {@code spaceshipStructure} 0:1000;
 * TODO(CE: HbmWorldGen.java:379) dud — {@code Dud}, {@code dudStructure} 0:500.
 */
public final class OilMeteorWorldGenFeatures {

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(BuiltInRegistries.FEATURE, MainRegistry.MODID);

    public static final DeferredHolder<Feature<?>, OilBubbleFeature> OIL_BUBBLE =
            FEATURES.register("oil_bubble", () -> new OilBubbleFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, BedrockOilDepositFeature> BEDROCK_OIL_DEPOSIT =
            FEATURES.register("bedrock_oil_deposit", () -> new BedrockOilDepositFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, OilSandBubbleFeature> OIL_SAND_BUBBLE =
            FEATURES.register("oil_sand_bubble", () -> new OilSandBubbleFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, MeteoriteFeature> METEORITE =
            FEATURES.register("meteorite", () -> new MeteoriteFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, AntennaFeature> ANTENNA =
            FEATURES.register("antenna", () -> new AntennaFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, BunkerFeature> BUNKER =
            FEATURES.register("bunker", () -> new BunkerFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, RadioFeature> RADIO =
            FEATURES.register("radio", () -> new RadioFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, SellafieldFeature> SELLAFIELD =
            FEATURES.register("sellafield", () -> new SellafieldFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, LandmineFeature> LANDMINE =
            FEATURES.register("landmine", () -> new LandmineFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, NitanChestFeature> NITAN_CHEST =
            FEATURES.register("nitan_chest", () -> new NitanChestFeature(NoneFeatureConfiguration.CODEC));

    private OilMeteorWorldGenFeatures() {
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
