package com.hbm.render.misc;

/**
 * Minimal forward-reference stub. CE's {@code com.hbm.render.misc.RenderScreenOverlay} is a large
 * client HUD-drawing class (ammo counters, hunger/thirst/sanity bars, crosshair rendering, etc.) -
 * squarely a "client rendering" package concern, out of scope for this bomb-blocks/detonator-items
 * pass. It is stubbed here, narrowly, only because {@link com.hbm.interfaces.IHoldableWeapon}
 * (already committed by an earlier wave) already references {@code RenderScreenOverlay.Crosshair}
 * in its own {@code getCrosshair()} method signature - without at least this nested enum exisiting,
 * that already-committed interface (and this package's own {@code ItemLaserDetonator}, the first
 * real implementor of it) do not compile.
 *
 * <p>Only the {@link Crosshair} enum is ported here (CE's real values, verbatim - these are inert
 * texture-atlas UV coordinates, not behavior). No actual overlay-drawing logic is implemented -
 * that belongs to whichever future package owns the client HUD rendering pipeline. Until that
 * package lands, {@link Crosshair} has no renderer consuming it; {@code IHoldableWeapon}'s own
 * {@code renderHud}/{@code hasCustomHudElement} default methods are already no-ops, so this stub
 * introduces no fake behavior.
 */
public final class RenderScreenOverlay {

    private RenderScreenOverlay() {
    }

    /** Verbatim port of CE's {@code RenderScreenOverlay.Crosshair} enum (texture atlas x/y/size only). */
    public enum Crosshair {
        NONE(0, 0, 0),
        CROSS(1, 55, 16),
        CIRCLE(19, 55, 16),
        SEMI(37, 55, 16),
        KRUCK(55, 55, 16),
        DUAL(1, 73, 16),
        SPLIT(19, 73, 16),
        CLASSIC(37, 73, 16),
        BOX(55, 73, 16),

        L_CROSS(0, 90, 32),
        L_KRUCK(32, 90, 32),
        L_CLASSIC(64, 90, 32),
        L_CIRCLE(96, 90, 32),
        L_SPLIT(0, 122, 32),
        L_ARROWS(32, 122, 32),
        L_BOX(64, 122, 32),
        L_CIRCUMFLEX(96, 122, 32),
        L_RAD(0, 154, 32),
        L_MODERN(32, 154, 32),
        L_BOX_OUTLINE(64, 154, 32);

        public final int x;
        public final int y;
        public final int size;

        Crosshair(int x, int y, int size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }
    }
}
