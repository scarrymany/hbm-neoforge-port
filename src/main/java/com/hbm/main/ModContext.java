package com.hbm.main;

import net.minecraft.world.entity.Entity;

/**
 * Ported from CE's {@code com.hbm.main.ModContext} (12 lines) - byte-for-byte the same single-field
 * shape, no adaptation needed. Used by other Phase 3 packages (nuke-casing/launch-pad/bomb-block
 * {@code explode()}/{@code launch()} call chains, per {@code docs/phase3/
 * bomb_blocks_and_detonators.md}'s "Key design/API decisions") to smuggle the triggering
 * {@link Entity} through an internal call that has no detonator parameter of its own.
 *
 * @apiNote Always call {@code remove()} after use to avoid state leaks, wrapped in a
 * {@code try/finally} block - every CE call site does this, and this port's own call sites must
 * preserve that discipline exactly (a leaked {@link ThreadLocal} on a server thread pool would
 * misattribute a later, unrelated detonation to the wrong player).
 */
public class ModContext {

    /**
     * Little hack to provide context. Technically a static field would work either (as the server
     * only has one server thread).
     */
    public static final ThreadLocal<Entity> DETONATOR_CONTEXT = new ThreadLocal<>();
}
