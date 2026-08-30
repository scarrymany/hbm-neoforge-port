package com.hbm.entity.projectile;

import com.hbm.util.Tuple;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Port of CE's {@code com.hbm.entity.projectile.IBulletBase} (12 lines) - a trivial tracer-render
 * contract (previous-tick position + a list of render "nodes", each a world position paired with a
 * timestamp/weight) implemented by CE's older {@code EntityBulletBaseNT} for multi-segment tracer
 * rendering. Ported here as CE has it (a marker/callback interface, not consumed by
 * {@link EntityBulletBaseMK4}/{@link EntityBulletBeamBase} in CE either - only the separate,
 * out-of-scope {@code EntityBulletBaseNT} implements it, per {@code docs/phase3/gun_framework.md}'s
 * research; see that class's own Phase 4/legacy-system notes for where it would actually be
 * consumed).
 */
public interface IBulletBase {
    double prevX();
    double prevY();
    double prevZ();

    void prevX(double d);
    void prevY(double d);
    void prevZ(double d);

    List<Tuple.Pair<Vec3, Double>> nodes();
}
