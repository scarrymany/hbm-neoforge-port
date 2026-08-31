/**
 * The 4 RBMK reactor {@link net.minecraft.client.renderer.blockentity.BlockEntityRenderer}s
 * confirmed "safe to build today with zero new server-side plumbing" by
 * {@code docs/phase5/reactor_and_explosion_visual_effects.md}'s Headline finding 3 and its
 * Phase-5-safe scope items 1-4 (Task {@code c1-rbmk-renderers}):
 * <ul>
 *   <li>{@link com.hbm.client.render.blockentity.rbmk.RBMKControlRodRenderer} - control-rod
 *   extraction depth, backs {@link com.hbm.blockentity.machine.rbmk.RBMKControlManualBlockEntity}
 *   and {@link com.hbm.blockentity.machine.rbmk.RBMKControlAutoBlockEntity}.</li>
 *   <li>{@link com.hbm.client.render.blockentity.rbmk.RBMKFuelColumnRenderer} - fuel-column rod
 *   stack + Cherenkov glow, backs {@link com.hbm.blockentity.machine.rbmk.RBMKRodBlockEntity} and
 *   {@link com.hbm.blockentity.machine.rbmk.RBMKRodReaSimBlockEntity}.</li>
 *   <li>{@link com.hbm.client.render.blockentity.rbmk.RBMKConsoleHeatmapRenderer} - the in-world
 *   15x15 heatmap, backs {@link com.hbm.blockentity.machine.rbmk.RBMKConsoleBlockEntity}.</li>
 *   <li>{@link com.hbm.client.render.blockentity.rbmk.RBMKAutoloaderPistonRenderer} - the
 *   autoloader piston travel, backs
 *   {@link com.hbm.blockentity.machine.rbmk.RBMKAutoloaderBlockEntity}.</li>
 * </ul>
 * All 4 render real, already-networked block-entity fields ({@code extraction}/
 * {@code lastExtraction}, {@code hasRod}/{@code rodColor}/{@code lastFluxQuantity}, {@code columns[]},
 * {@code piston}/{@code lastPiston}) through {@code com.hbm.render.loader.HbmObjModel} (the shared
 * OBJ mesh loader/renderer utility) plus, where CE has no reusable vanilla {@code RenderType}
 * (the Cherenkov glow's additive blend, the console's opaque flat-color grid), a manual
 * {@code Tesselator}/{@code BufferBuilder}/{@code BufferUploader.drawWithShader} draw - see each
 * class's own javadoc for the exact CE source lines ported and the specific 1.21.1 API-shape
 * confirmations used. Registration ({@code BlockEntityRenderers.register}) happens from
 * {@code ClientModRegistry.onClientSetup}, a shared/aggregator file this package's own task may not
 * edit directly - see that task's reported {@code wiringSnippets}.
 * <p>
 * No real {@code .obj}/texture assets exist in {@code src/main/resources} for any of the 4 renderers
 * yet (a known, already-documented gap - see {@code docs/phase5/
 * reactor_and_explosion_visual_effects.md}'s "Texture/model assets" Deferred-scope entry); every
 * class here references the exact resource paths CE ships so each renderer activates the moment
 * those assets land, with zero further code changes.
 */
package com.hbm.client.render.blockentity.rbmk;
