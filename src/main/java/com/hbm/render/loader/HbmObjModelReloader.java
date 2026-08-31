package com.hbm.render.loader;

import com.hbm.main.MainRegistry;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/**
 * Automatically re-parses every {@link HbmObjModel} obtained through
 * {@link HbmObjModel#get(net.minecraft.resources.ResourceLocation)} whenever the client's
 * resource manager reloads (F3+T, resource pack switch, {@code /reload}) - the modern
 * counterpart to CE's own model-reload support, which its own source comments mark as removed
 * ("{@code mlbv: removed. see comments at HFRModelReloader}" in
 * {@code upstream/hbm-ce/.../render/loader/HFRWavefrontObject.java}) - CE 2.5.0.5 does not
 * actually reload OBJ models live any more, so this is a small improvement over the exact
 * upstream behavior, not a strict port; the shape of the listener (a trivial no-op
 * {@link #prepare}, all real work in {@link #apply}) matches the confirmed-real
 * {@code SimplePreparableReloadListener<Void>} pattern upstream/neo-edition's own (still-present)
 * {@code HFRModelReloader} uses for the same job, cross-checked strictly for the NeoForge/vanilla
 * API shape per this port's ground rules - not for behavior, since Neo Edition's version also
 * reloads a GPU-uploaded {@code VertexBuffer}-per-group cache this port deliberately does not
 * have (see {@link HbmObjModel}'s class javadoc for why).
 *
 * <p>Self-registers via its own {@code @EventBusSubscriber} exactly like this port's existing
 * {@code com.hbm.handler.HbmKeybinds} (see that class's javadoc for the same rationale) instead
 * of needing a line added to the shared {@code ClientModRegistry.onClientSetup}/reload-listener
 * hookup - {@code RegisterClientReloadListenersEvent} implements
 * {@code net.neoforged.fml.event.IModBusEvent} (fires only on the mod bus), hence
 * {@code bus = Bus.MOD} below; confirmed against upstream/neo-edition's own
 * {@code NuclearTechModClient#onRegisterClientReloadListeners}, which subscribes to the same
 * event the same way.
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class HbmObjModelReloader extends SimplePreparableReloadListener<Void> {

    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new HbmObjModelReloader());
    }

    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        // Deliberately trivial, matching upstream/neo-edition's HFRModelReloader.prepare: the
        // actual re-parse happens in #apply, which runs on the main/render thread after the
        // reload barrier - see HbmObjModel#reload's own javadoc for why that matters (no
        // multi-threaded parser safety needed, since parsing never happens off-thread here).
        return null;
    }

    @Override
    protected void apply(Void unused, ResourceManager resourceManager, ProfilerFiller profiler) {
        for (HbmObjModel model : HbmObjModel.allTracked()) {
            model.reload(resourceManager);
        }
    }
}
