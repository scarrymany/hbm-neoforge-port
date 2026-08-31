package com.hbm.client.render.entity.mob;

import com.hbm.main.MainRegistry;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Not a CE port - CE's 1.12.2 {@code ModelRenderer}-based models needed no separate "layer
 * definition" registration step at all (a {@code ModelBase} subclass just built its {@code
 * ModelRenderer} fields directly in its constructor, every time a renderer instantiated one). 1.21.1
 * requires every reusable box-cuboid {@link net.minecraft.client.model.geom.ModelPart} tree to be
 * baked once from a {@link net.minecraft.client.model.geom.builders.LayerDefinition} registered
 * against a {@link ModelLayerLocation}, via {@code EntityRenderersEvent.RegisterLayerDefinitions}
 * (a mod-bus event, hence the explicit {@code bus = MOD} below - see this port's ground rule 5) -
 * this class is the minimal, self-contained home for that one-time registration for the two
 * genuinely box-cuboid models this task's batch adds ({@link HunterChopperModel}, {@link CrabModel}),
 * matching {@code docs/phase5/boss_and_vehicle_entity_renderers.md}'s own Headline finding #3
 * classification of these two as CE's real, vanilla-box-cuboid-shaped models.
 *
 * <p>Deliberately its own new file rather than an edit to {@code
 * com.hbm.main.ClientModRegistry} - per this task's ground rule 7 (do not directly edit shared
 * aggregator files many parallel agents touch); {@code RegisterLayerDefinitions} is a distinct
 * self-subscribing mod-bus event, so no wiring snippet against that shared file is needed for this
 * piece at all (unlike the per-entity {@code EntityRenderers.register} calls, which this task's
 * structured output reports as {@code wiringSnippets} against {@code ClientEntityRenderers.java}
 * per this task's own explicit instruction).
 */
@EventBusSubscriber(modid = MainRegistry.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BossModelLayers {

    private BossModelLayers() {}

    public static final ModelLayerLocation HUNTER_CHOPPER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "hunter_chopper"), "main");
    public static final ModelLayerLocation CRAB =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "cyber_crab"), "main");
    public static final ModelLayerLocation RAD_BEAST =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "rad_beast"), "main");

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HUNTER_CHOPPER, HunterChopperModel::createBodyLayer);
        event.registerLayerDefinition(CRAB, CrabModel::createBodyLayer);
        event.registerLayerDefinition(RAD_BEAST, RadBeastModel::createBodyLayer);
    }
}
