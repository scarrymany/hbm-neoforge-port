package com.hbm.client.render.entity.effect;

import com.hbm.entity.effect.EntityBlackHole;
import com.hbm.entity.effect.EntityQuasar;
import com.hbm.main.MainRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Ported from CE's {@code com.hbm.render.entity.effect.RenderQuasar} ({@code upstream/hbm-ce/.../
 * render/entity/effect/RenderQuasar.java}, 76 lines, read in full) - the purple-white "digamma
 * quasar" reskin of {@link BlackHoleRenderer}, registered against {@link EntityQuasar} only.
 *
 * <h2>Why {@link #render} is not overridden</h2>
 * CE's own {@code RenderQuasar.doRender} override is, byte-for-byte, the exact same body its own
 * {@code RenderBlackHole.doRender} superclass method already produces for a {@code EntityQuasar}
 * instance: {@code EntityQuasar} matches neither the {@code instanceof EntityVortex} nor {@code
 * instanceof EntityRagingVortex} branch of the base class's {@code if}/{@code else if}/{@code else}
 * chain (confirmed - {@code EntityQuasar extends EntityBlackHole} directly, not through either
 * vortex subclass, in both CE and this port), so it always falls into the same {@code else} branch
 * ({@link BlackHoleRenderer#renderDisc}+{@link BlackHoleRenderer#renderJets}) either way. CE's
 * override exists only as a verbatim copy-paste of the inherited method (confirmed by direct
 * side-by-side read of both {@code doRender} bodies - identical apart from the class name in the
 * signature), not because {@code EntityQuasar} needs different top-level render logic - {@code
 * upstream/neo-edition}'s own {@code RenderQuasar.java} independently reached the identical
 * conclusion structurally (its own {@code render} override is likewise a verbatim copy of its
 * superclass's body). Re-declaring that redundant override here would only be a maintenance
 * liability (two copies of the same branch logic to keep in sync); relying on inheritance is the
 * more correct 1.21.1 translation of CE's own effective behavior, not a missed detail. Only the 3
 * genuinely distinct hooks below are overridden, exactly matching CE's real 3 overrides
 * ({@code discTex}/{@code setColorFromIteration}/{@code steps}) minus {@code steps()} (CE's own
 * override returns the same {@code 15} the base class already does - a no-op override, not ported
 * for the same "avoid a redundant copy" reason as {@code render} above).
 */
public class QuasarRenderer<T extends EntityBlackHole> extends BlackHoleRenderer<T> {

    /** CE: {@code RenderQuasar.quasar}, real on-disk name {@code textures/entity/bholed.png} (CE's Java source spells it {@code "bholeD.png"} - see {@link BlackHoleRenderer}'s own "Asset gap" note for why this port uses the real lowercase filename). */
    private static final ResourceLocation QUASAR_DISC =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/entity/bholed.png");

    public QuasarRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected ResourceLocation discTex() {
        return QUASAR_DISC;
    }

    /** CE: {@code RenderQuasar.setColorFromIteration} - {@code r=1}, {@code g=b=(iteration/15)^2}, a white-to-red-tinted ramp (not the base class's orange/gold-to-cyan ramp). */
    @Override
    protected float[] getColorFromIteration(int iteration, float alpha) {
        float g = (float) Math.pow(iteration / 15F, 2);
        return new float[]{1F, g, g, alpha};
    }
}
