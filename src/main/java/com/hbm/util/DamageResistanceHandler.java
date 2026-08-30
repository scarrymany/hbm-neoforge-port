package com.hbm.util;

/**
 * Minimal forward reference for CE's {@code com.hbm.util.DamageResistanceHandler} (608 lines in CE:
 * a JSON-configured per-item/per-armor-set/per-entity-class damage-resistance-table system consumed
 * by every damage path in the mod, not just guns - melee, explosions, environmental hazards all
 * route through it too via {@code EntityDamageUtil.attackEntityFromNT}).
 * <p>
 * That full table system is out of the gun-framework/ballistics-core package's own scope - see
 * {@code docs/phase3/gun_framework.md}'s "Deferred scope" ("Armor / FSB / hazmat integration" /
 * {@code DamageResistanceHandler} needs its own research pass, likely paired with the armor-sets +
 * FSB modifier system PORT_SPEC also calls out for Phase 3). This class exists only to supply the
 * two pieces the ballistics core actually needs right now:
 * <ol>
 *     <li>{@link DamageClass}, the real 9-value CE enum {@code BulletConfig.dmgClass} switches on
 *     (confirmed by reading CE's {@code DamageResistanceHandler.java} directly - kept whole,
 *     including {@code PLASMA}, unlike the Neo Edition reference's own trimmed 8-value copy, since
 *     {@code PLASMA} is a real, distinct value CE's own resistance-table switch statement still
 *     branches on elsewhere in the mod).</li>
 *     <li>The {@code setup(pierceDT, pierce)}/{@code reset()} static-state contract
 *     {@code EntityDamageUtil.attackEntityFromNT} threads {@code BulletConfig}'s
 *     {@code armorThresholdNegation}/{@code armorPiercingPercent} fields through, preserved
 *     verbatim (plain static fields, not {@code ThreadLocal} - CE's own shape) so a future
 *     resistance-table pass can wire {@link #currentPDT}/{@link #currentPDR} into real
 *     armor-piercing math without having to touch every call site that already threads
 *     {@code pierceDT}/{@code pierce} through. This is the same reentrancy-unsafe
 *     shared-mutable-static-state shape the gun-framework report flagged (mirroring
 *     {@code RBMKNeutronHandler}'s own Phase 2 static-state risk) as a "preserve vs fix" fork for
 *     whoever does the full port - flagged here again, not resolved, since fixing it (e.g. making it
 *     re-entrant) is a design call that belongs with the full resistance-table pass, not this one.</li>
 * </ol>
 */
public class DamageResistanceHandler {

    /** Currently cached damage-threshold pierce amount, set immediately before an attackEntityFromNT call. */
    public static float currentPDT = 0F;
    /** Currently cached armor-piercing percent, set immediately before an attackEntityFromNT call. */
    public static float currentPDR = 0F;

    public static void setup(float dt, float dr) {
        currentPDT = dt;
        currentPDR = dr;
    }

    public static void reset() {
        currentPDT = 0F;
        currentPDR = 0F;
    }

    public enum DamageClass {
        PHYSICAL, FIRE, EXPLOSIVE, ELECTRIC, PLASMA, LASER, MICROWAVE, SUBATOMIC, OTHER
    }
}
