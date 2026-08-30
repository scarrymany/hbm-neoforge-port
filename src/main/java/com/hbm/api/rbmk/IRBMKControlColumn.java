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
     */
    double getLevel();

    /**
     * The actual flux multiplier applied to a passing stream. Ordinarily equal to
     * {@link #getLevel()}; a manual control rod implementation should override it to add the
     * withdrawal power-surge spike - see {@link RBMKControlMath#getEffectiveMult}, this port's pure
     * extraction of CE's {@code TileEntityRBMKControlManual#getMult()} inline surge math. CE:
     * {@code TileEntityRBMKControl#getMult()}.
     */
    double getMult();
}
