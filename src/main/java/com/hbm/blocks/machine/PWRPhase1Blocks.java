package com.hbm.blocks.machine;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * Lazy by-id lookup for the six Phase-1-ported {@code com.hbm.blocks.generic.BlockGenericPWR}
 * casing/core visual blocks ({@code pwr_heatex}/{@code pwr_heatsink}/{@code pwr_neutron_source}/
 * {@code pwr_reflector}/{@code pwr_casing}/{@code pwr_port}, registered by
 * {@code com.hbm.blocks.generic.GenericBlocks#registerGenericPwr}) - both
 * {@link MachinePWRControllerBlock}'s flood-fill and
 * {@link com.hbm.blockentity.machine.PWRControllerBlockEntity#setup} need to identify these six
 * blocks by {@code Block} identity (exactly like CE's {@code MachinePWRController.isValidCasing}/
 * {@code isValidCore}/{@code TileEntityPWRController.setup}), but {@code GenericBlocks} keeps no
 * exported {@code DeferredBlock} field for any of the six (each is registered inline, see that
 * class's {@code registerGenericPwr} - only the registry name survives). Rather than edit that
 * already-committed Phase 1 file to add fields (this port's own convention: avoid touching another
 * area's already-shipped file when a same-effect alternative exists), this resolves each block by
 * its known registry id instead - the exact same lazy-{@code BuiltInRegistries.BLOCK.get} idiom this
 * port's own {@code OilDrillBaseBlockEntity} already uses for an identical
 * registered-elsewhere-no-field cross-package lookup (fields resolved and cached on first access,
 * since the registry is not populated yet at this class's own class-load time, only once NeoForge's
 * registry events have fired).
 */
public final class PWRPhase1Blocks {

    private static Block heatexCache;
    private static Block heatsinkCache;
    private static Block neutronSourceCache;
    private static Block reflectorCache;
    private static Block casingCache;
    private static Block portCache;

    private PWRPhase1Blocks() {
    }

    private static Block resolve(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    public static Block heatex() {
        if (heatexCache == null) heatexCache = resolve("pwr_heatex");
        return heatexCache;
    }

    public static Block heatsink() {
        if (heatsinkCache == null) heatsinkCache = resolve("pwr_heatsink");
        return heatsinkCache;
    }

    public static Block neutronSource() {
        if (neutronSourceCache == null) neutronSourceCache = resolve("pwr_neutron_source");
        return neutronSourceCache;
    }

    public static Block reflector() {
        if (reflectorCache == null) reflectorCache = resolve("pwr_reflector");
        return reflectorCache;
    }

    public static Block casing() {
        if (casingCache == null) casingCache = resolve("pwr_casing");
        return casingCache;
    }

    public static Block port() {
        if (portCache == null) portCache = resolve("pwr_port");
        return portCache;
    }
}
