package com.hbm.client.render.item;

import com.hbm.main.MainRegistry;
import com.hbm.render.loader.HbmObjModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Minimal, compiling worked example of {@link HbmItemBEWLR}, proving this task's plumbing lines
 * up end-to-end against a real (if placeholder) {@link HbmObjModel} reference - per this task's
 * own instructions ("prove your plumbing compiles-shape-correct with one minimal worked example
 * (any placeholder mesh reference is fine)").
 *
 * <h2>Deliberately NOT wired to any registered item</h2>
 * This class is never passed to {@link HbmItemRendererRegistry#register} anywhere in this port's
 * real client-setup code, and {@link #PLACEHOLDER_MODEL}/{@link #PLACEHOLDER_TEXTURE} do not
 * correspond to any real asset shipped in {@code src/main/resources} - porting the actual OBJ
 * meshes and textures for CE's ~54-65 guns is explicitly out of this task's scope (Content-wave
 * task {@code c6}'s job; see also this port's ground rule that content agents own the actual
 * assets, not the rendering framework). {@link HbmObjModel#get} eagerly parses its resource the
 * first time it is called and throws {@link com.hbm.render.loader.ModelFormatException} if the
 * resource does not exist - so this class keeps that call strictly lazy (inside
 * {@link #model()}, only reachable from {@link #renderModel}, only reachable from
 * {@link #renderByItem}) and never lets it run from a static field initializer or any other path
 * that could execute during normal client bootstrap. Since nothing in this port's real
 * registration code ever constructs or renders this class, that lazy path is never actually
 * exercised - it exists purely so this file compiles against {@link HbmObjModel}'s real API and
 * demonstrates the intended call shape for {@code c6} to copy from, not so it is safe to
 * accidentally wire up as-is. <b>Do not register this class to a real item</b> - copy its shape
 * into a new, gun-specific subclass instead, pointing at that gun's real ported {@code .obj}/
 * texture resources.
 *
 * <h2>What this demonstrates</h2>
 * <ul>
 *   <li>A concrete {@link HbmItemBEWLR} subclass compiles and its abstract {@link #renderModel}
 *       hook has the exact signature the base class's {@link HbmItemBEWLR#renderByItem} dispatch
 *       calls it with.</li>
 *   <li>Overriding one {@code setupXxx} hook (here, {@link #setupFirstPerson}) without needing to
 *       touch the others - the base class's per-context dispatch does the rest.</li>
 *   <li>Reading a piece of live {@link ItemStack} state (here, damage value, standing in for a
 *       real gun's "is the magazine present" check - see
 *       {@code docs/phase5/weapon_gun_rendering_animloader.md}'s "What CE actually renders for
 *       reload/ammo visual state" section for the real pattern this stands in for) to decide
 *       which named OBJ part(s) to draw this frame, via this class's inherited
 *       {@link #renderPart} convenience.</li>
 * </ul>
 */
public final class ExamplePlaceholderBEWLR extends HbmItemBEWLR {

    // Deliberately non-existent placeholder paths - see class javadoc. A real gun subclass would
    // point these at its actual ported assets, e.g.
    // ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/weapons/spas12.obj").
    private static final ResourceLocation PLACEHOLDER_MODEL =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "models/weapons/example/placeholder.obj");
    private static final ResourceLocation PLACEHOLDER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/models/weapons/example/placeholder.png");

    // Lazily populated - see class javadoc for why this must never run eagerly for a resource that
    // does not exist on disk. HbmObjModel.get(...) itself caches by ResourceLocation, so this field
    // is really just avoiding a repeated map lookup, not the only cache layer.
    private HbmObjModel cachedModel;

    private HbmObjModel model() {
        if (cachedModel == null) {
            cachedModel = HbmObjModel.get(PLACEHOLDER_MODEL);
        }
        return cachedModel;
    }

    @Override
    protected void setupFirstPerson(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack) {
        // Stand-in for the per-gun view-bob/sway/recoil pose math a real subclass would apply here
        // (see HbmItemBEWLR's own "OBJ pivot/orientation note" javadoc, and
        // docs/phase5/weapon_gun_rendering_animloader.md's recommended client-local animation-start
        // timestamp approach for driving this from BusAnimation state once that lands) - kept as a
        // trivial fixed offset here since real pose math is explicitly out of this task's scope.
        poseStack.translate(0.0, 0.1, -0.2);
    }

    @Override
    protected void renderModel(ItemStack stack, ItemDisplayContext ctx, PoseStack poseStack,
                                MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderPart(model(), poseStack, bufferSource, PLACEHOLDER_TEXTURE, packedLight, packedOverlay, "Body");

        // Stand-in for a real gun's "is a magazine/round visually present" check - see this class's
        // own javadoc for the CE pattern this mirrors (a plain per-frame `if`, no model-predicate
        // system involved).
        if (stack.getDamageValue() < stack.getMaxDamage()) {
            renderPart(model(), poseStack, bufferSource, PLACEHOLDER_TEXTURE, packedLight, packedOverlay, "Magazine");
        }
    }
}
