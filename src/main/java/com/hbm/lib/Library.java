package com.hbm.lib;

/**
 * CE's {@code Library} is a self-annotated {@code @Spaghetti} ~2500-line god class mixing ray
 * tracing, AABB/cone math, chest-loot rolling, NBT compatibility comparison, energy-transfer glue
 * and number formatting, most of it reaching directly into {@code ModBlocks}, {@code ModItems},
 * capability, entity and tile-entity types that do not exist yet in this port. Porting it wholesale
 * here would either produce dead code (nothing to call the block/item/entity-facing methods) or
 * half-finished stubs, both of which are against the Phase 0 ground rules.
 * <p>
 * This Phase 0 stub carries forward only the one piece of {@code Library} that other in-scope
 * {@code lib}/{@code util} classes call: the busy-wait hint used by the lock-free collectors and
 * queues. CE routed this through a reflective {@code LambdaMetafactory} shim because it still had to
 * run on Java 8, where {@link Thread#onSpinWait()} does not exist; on Java 21 that indirection is
 * unnecessary and the call below is direct.
 * <p>
 * The remaining ~2500 lines of {@code Library} must be triaged method-by-method against the areas
 * that call them (energy/fluid, capability, blocks/items, entity, tileentity) once those areas exist,
 * per the Phase 0 research plan; this class is expected to grow incrementally as later phases port
 * the methods they actually need.
 */
public final class Library {

    private Library() {
    }

    public static void onSpinWait() {
        Thread.onSpinWait();
    }
}
