package com.hbm.api.rbmk;

import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKType;

/**
 * The {@link RBMKType#CONTROL_ROD}-specific surface that {@code RBMKNeutronHandler}'s stream
 * dispatch needs. Split out from {@link IRBMKColumn}/{@link IRBMKFluxReceiver} because a control
 * rod does not consume a flux stream the way a fuel rod or outgasser does - it attenuates a
 * passing stream in place and lets it continue (or blocks it outright at zero extraction).
 * <p>
 * CE: the relevant subset of {@code com.hbm.tileentity.machine.rbmk.TileEntityRBMKControl}
 * ({@code level} field, {@code getMult()} method).
 */
public interface IRBMKControlColumn extends IRBMKColumn {

    /**
     * Raw extraction level, {@code [0;1]}: {@code 0} = fully inserted (blocks all flux), {@code 1}
     * = fully withdrawn. CE: {@code TileEntityRBMKControl#level}. Checked directly (not through
     * {@link #getMult()}) by {@code RBMKNeutronHandler}'s "is this rod even open at all" gate and
     * by its "control rod immediately after another control rod" fallback branch.
     * <p>
     * Named {@code getExtraction()} rather than CE's own {@code getLevel()} because
     * {@code BlockEntity} itself declares a {@code @Nullable Level getLevel()} accessor (the world)
     * in NeoForge 1.21.1; every implementor of this interface is a {@code BlockEntity} subclass
     * ({@link com.hbm.blockentity.machine.rbmk.RBMKControlBlockEntity}), so keeping CE's name here
     * would collide with that inherited method (same name, incompatible return type - not a valid
     * override). See that class's own javadoc for the matching {@code level} field -> {@code
     * extraction} rename this mirrors.
     */
    double getExtraction();

    /**
     * The actual flux multiplier applied to a passing stream. Ordinarily equal to
     * {@link #getExtraction()}; a manual control rod implementation should override it to add the
     * withdrawal power-surge spike - see {@link RBMKControlMath#getEffectiveMult}, this port's pure
     * extraction of CE's {@code TileEntityRBMKControlManual#getMult()} inline surge math. CE:
     * {@code TileEntityRBMKControl#getMult()}.
     */
    double getMult();
}
