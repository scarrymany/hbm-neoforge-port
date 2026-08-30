package com.hbm.api.rbmk;

import com.hbm.handler.neutron.NeutronStream;
import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKType;

/**
 * Implemented by the two {@link RBMKType} column roles that actually consume a flux stream rather
 * than merely modifying, blocking, or reflecting it in place: fuel rods ({@link RBMKType#ROD}) and
 * outgassers ({@link RBMKType#OUTGASSER}).
 * <p>
 * CE: {@code com.hbm.tileentity.machine.rbmk.IRBMKFluxReceiver} (19 lines, read in full). The
 * nested {@link NType} enum and {@link #receiveFlux} method are unchanged from CE; the two default
 * methods below ({@link #canReceiveFlux()}, {@link #getLastFluxQuantity()},
 * {@link #isReaSimVariant()}) are this port's own generalization of logic CE inlined via
 * {@code instanceof TileEntityRBMKRod}/{@code TileEntityRBMKOutgasser}/{@code TileEntityRBMKRodReaSim}
 * checks against concrete tile entity classes this package does not have (see this package's
 * {@code com.hbm.handler.neutron.RBMKNeutronHandler} javadoc).
 */
public interface IRBMKFluxReceiver extends IRBMKColumn {

    /**
     * The fast/slow neutron-type designation of a fuel, split three ways. See
     * docs/phase2/rbmk_reactor.md's "Fast/slow neutron-type semantics are easy to invert" open
     * question for the exact, easy-to-confuse distinction between this enum, a
     * {@link NeutronStream#fluxRatio} value, and a fuel's "efficiency-weighted input flux" - the
     * three related-but-independent axes CE (and this port) keep separate on purpose.
     */
    enum NType {
        FAST("trait.rbmk.neutron.fast"),
        SLOW("trait.rbmk.neutron.slow"),
        /** not to be used for reactor flux calculation, only for the fuel designation */
        ANY("trait.rbmk.neutron.any");

        public final String unlocalized;

        NType(String loc) {
            this.unlocalized = loc;
        }
    }

    /** CE: {@code IRBMKFluxReceiver#receiveFlux(NeutronStream)}, unchanged. Only ever called by {@code RBMKNeutronHandler} after {@link #canReceiveFlux()} has already been checked. */
    void receiveFlux(NeutronStream stream);

    /**
     * Whether this receiver is currently able to accept a flux stream at all - gates the
     * {@link #receiveFlux} dispatch in {@code RBMKNeutronHandler.RBMKNeutronStream#runStreamInteraction}
     * (a stream that reaches a receiver which returns {@code false} here simply keeps travelling
     * past it, exactly like CE's inlined {@code continue}).
     * <p>
     * CE inlines two different concrete checks at its two dispatch call sites instead of one shared
     * interface method: {@code TileEntityRBMKRod#hasRod} (a fuel item is currently loaded) for
     * {@link RBMKType#ROD}, and {@code TileEntityRBMKOutgasser#canProcess()} (a recipe match is
     * queued and there is tank space for its output) for {@link RBMKType#OUTGASSER}. Rod columns
     * should implement this as {@code hasRod}; outgasser columns as {@code canProcess()}.
     */
    boolean canReceiveFlux();

    /**
     * Last tick's total flux quantity received, consulted only by the RBMK neutron node cache's
     * 20-tick eviction heuristic ({@code RBMKNeutronHandler.RBMKNeutronNode#checkNode}) to decide
     * whether a cold rod's cached streams can be safely dropped. CE:
     * {@code TileEntityRBMKRod#lastFluxQuantity}. Outgasser columns need not override this - the
     * default of {@code 0} is fine, since CE's own {@code checkNode} only ever inspects this field
     * for {@link RBMKType#ROD} columns.
     */
    default double getLastFluxQuantity() {
        return 0D;
    }

    /**
     * Whether this receiver spreads its own outbound flux using the wide ReaSim node-cache diamond
     * ({@code RBMKNeutronHandler.RBMKNeutronNode#getReaSimNodes}) instead of the ordinary
     * 4-cardinal-direction node cache used by every other {@link RBMKType#ROD} column. CE:
     * {@code instanceof TileEntityRBMKRodReaSim} checks inside {@code checkNode()}. Ordinary fuel
     * rods (and outgassers) return {@code false}.
     */
    default boolean isReaSimVariant() {
        return false;
    }
}
