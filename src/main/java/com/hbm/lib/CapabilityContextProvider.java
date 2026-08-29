package com.hbm.lib;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Context provider for capability lookups.
 * Makes porting 1.7 com.hbm.tileentity.IConditionalInvAccess possible.
 * For now, it only works for NeoForge block capabilities.
 *
 * @author mlbv
 */
public final class CapabilityContextProvider {
    private static final ThreadLocal<BlockPos> accessorPos = new ThreadLocal<>();

    private CapabilityContextProvider() {
    }

    /**
     * Gets the accessor's position from the current thread's context.
     * Returns the core tile's own position if no context is set.
     *
     * @param ownPos The BlockPos of the tile entity asking for the context.
     * @return The accessor's BlockPos, or ownPos if not in a proxy context.
     */
    @NotNull
    public static BlockPos getAccessor(BlockPos ownPos) {
        BlockPos pos = accessorPos.get();
        return pos != null ? pos : ownPos;
    }

    @Nullable
    public static BlockPos pushPos(@NotNull BlockPos pos) {
        BlockPos prev = accessorPos.get();
        accessorPos.set(pos);
        return prev;
    }

    public static void popPos(@Nullable BlockPos prev) {
        if (prev == null) accessorPos.remove();
        else accessorPos.set(prev);
    }
}
