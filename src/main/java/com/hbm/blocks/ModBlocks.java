package com.hbm.blocks;

import com.hbm.main.MainRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central block registry, replacing CE's bespoke {@code ModBlocks.ALL_BLOCKS} list (populated by
 * base-class constructors) with a real NeoForge {@link DeferredRegister}.
 * <p>
 * Deliberately empty in Phase 0: every concrete block (~620 CE block files) is Phase 1/2 content
 * and lands here field by field, one {@code public static final DeferredBlock<...>} per block,
 * registered via {@code BLOCKS.register(name, supplier)} (see the confirmed pattern in the Neo
 * Edition reference's {@code NtmBlocks}). Registering a matching {@code BlockItem} for a block is
 * left to whichever phase adds that field, since it depends on the not-yet-existing
 * {@code com.hbm.items.ModItems} item registry and on which {@code BlockItem} subclass (plain,
 * lore, blast-info, ...) a given block needs.
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MainRegistry.MODID);

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
