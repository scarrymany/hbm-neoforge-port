package com.hbm.items.weapon.grenade;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * Port of CE's {@code com.hbm.items.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell} (4 values) -
 * pure data card, no world access. Per {@code docs/phase3/grenades.md}'s metadata-flattening
 * decision, this is no longer a {@code ItemEnumMulti} nested enum: each value now backs its own
 * standalone registered {@code Item} (see {@link GrenadeItems}), and this plain enum only carries
 * the per-shell stats CE's original enum constructor did (stack limit, draw duration, bounce
 * modifier, yeet/throw force).
 */
public enum EnumGrenadeShell implements StringRepresentable {

    /** Bonus fragmentation shell. */
    FRAG(4, 30, 0.5D, 1.0D),
    /** Thrown farther. */
    STICK(4, 43, 0.25D, 1.5D),
    /** Casing with electronics for EMP/plasma fillings. */
    TECH(2, 30, 0.5D, 1.0D),
    /** Nuka-grenade casing for high-yield fillings. */
    NUKE(1, 43, 0.25D, 1.5D);

    public static final EnumGrenadeShell[] VALUES = values();

    public static final Codec<EnumGrenadeShell> CODEC = StringRepresentable.fromEnum(EnumGrenadeShell::values);

    private final int stackLimit;
    private final int drawDuration;
    private final double bounceModifier;
    private final double yeetForce;

    EnumGrenadeShell(int stackLimit, int drawDuration, double bounceModifier, double yeetForce) {
        this.stackLimit = stackLimit;
        this.drawDuration = drawDuration;
        this.bounceModifier = bounceModifier;
        this.yeetForce = yeetForce;
    }

    public int getStackLimit() {
        return stackLimit;
    }

    public int getDrawDuration() {
        return drawDuration;
    }

    public double getBounce() {
        return bounceModifier;
    }

    public double getYeetForce() {
        return yeetForce;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
