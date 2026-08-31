package com.hbm.client.render.armor;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import com.hbm.main.MainRegistry;

/**
 * {@link ModelLayerLocation} registrar for this package's hand-modeled (no OBJ), plain-box
 * armor-model leaves - {@link GasMaskArmorModel} (CE: {@code render/model/ModelGasMask.java}, 6
 * boxes), {@link M65ArmorModel} (CE: {@code render/model/ModelM65.java}, 9 boxes across a
 * {@code mask}/{@code filter} pair), and {@link JetpackWornModel} (CE: {@code render/model/
 * ModelJetPack.java}, 8 boxes) - CE's "bucket (b)" leaves per {@code
 * docs/phase5/armor_humanoidmodel_rendering.md} finding 3. Mirrors {@code com.hbm.client.render.
 * entity.mob.BossModelLayers}' own established pattern exactly (same class-javadoc reasoning
 * applies here: 1.21.1 requires every reusable box-cuboid {@link net.minecraft.client.model.geom.
 * ModelPart} tree to be baked once from a {@link net.minecraft.client.model.geom.builders.
 * LayerDefinition} registered against a {@link ModelLayerLocation}, via {@code
 * EntityRenderersEvent.RegisterLayerDefinitions} - a mod-bus event, hence the explicit {@code bus =
 * MOD} below per this port's ground rule 5) - deliberately its own new file, not an edit to
 * {@code com.hbm.main.ClientModRegistry} or {@code BossModelLayers} (both shared/owned-elsewhere
 * per this task's ground rule 7/8).
 *
 * <p><b>Why armor Path A leaves need this at all, unlike an ordinary entity renderer</b>: {@code
 * IClientItemExtensions#getGenericArmorModel} hands a leaf no {@code EntityRendererProvider.
 * Context} (the usual place {@code Context#bakeLayer(ModelLayerLocation)} is called from) - only a
 * {@code LivingEntity}/{@code ItemStack}/{@code EquipmentSlot}/{@code HumanoidModel}. Every leaf in
 * this file instead lazily bakes its {@link net.minecraft.client.model.geom.ModelPart} tree via
 * {@code net.minecraft.client.Minecraft.getInstance().getEntityModels().bakeLayer(...)} the first
 * time it is actually constructed (i.e. the first time a player equips the piece) - confirmed real
 * by a directly analogous, real, compiling call site in {@code upstream/neo-edition}: {@code
 * particle/SkeletonParticle.java:69}, {@code new SkeletonModel(Minecraft.getInstance().
 * getEntityModels().bakeLayer(SkeletonModel.SKELETON_PART_LAYER))} - a non-entity-renderer call
 * site lazily baking a {@code ModelLayerLocation} registered via the identical {@code
 * EntityRenderersEvent.RegisterLayerDefinitions} mechanism ({@code main/NuclearTechModClient.java:
 * 656-660}), exactly this class's own shape. Safe to call this late: {@code
 * EntityRenderersEvent.RegisterLayerDefinitions} fires during client setup, long before any player
 * has a chance to equip one of these items.
 */
@EventBusSubscriber(modid = MainRegistry.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ArmorPartLayers {

    private ArmorPartLayers() {
    }

    public static final ModelLayerLocation GAS_MASK =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "gas_mask"), "main");
    public static final ModelLayerLocation M65_MASK =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "m65_mask"), "main");
    public static final ModelLayerLocation JETPACK_WORN =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "jetpack_worn"), "main");

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GAS_MASK, GasMaskArmorModel::createBodyLayer);
        event.registerLayerDefinition(M65_MASK, M65ArmorModel::createBodyLayer);
        event.registerLayerDefinition(JETPACK_WORN, JetpackWornModel::createBodyLayer);
    }
}
