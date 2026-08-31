package com.hbm.entity.effect;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Ported from CE's {@code com.hbm.entity.effect.EntityQuasar} (23 lines, read in full) - a trivial
 * {@link EntityBlackHole} subclass. Zero behavioral override beyond the two constructors matching
 * {@link EntityVortex}'s shape and a {@link #tick()} whose entire body is {@code super.tick()} - this
 * class exists purely to give {@code ItemDigamma}'s dropped-item spawn its own registry name
 * ({@code entity_digamma_quasar}) and its own client renderer (Phase 5), not to add any new mechanic.
 * Kept as its own class (matching CE's real structure and this port's "one CE class = one port class"
 * convention) rather than folded into {@link EntityBlackHole} the way Neo Edition's own
 * {@code NtmEntityTypes} does - see {@link GravityWellEntityTypes}'s own javadoc for why that fold is
 * not copied here.
 */
public class EntityQuasar extends EntityBlackHole {

    public EntityQuasar(EntityType<? extends EntityQuasar> type, Level level) {
        super(type, level);
    }

    public EntityQuasar(Level level, float size) {
        this(GravityWellEntityTypes.QUASAR.get(), level);
        this.entityData.set(SIZE, size);
    }

    @Override
    public void tick() {
        super.tick();
    }
}
