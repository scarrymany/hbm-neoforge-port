package com.hbm.api.network;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Redesigned for NeoForge 1.21: CE's contract returned the next free sequential packet id
 * ({@code int registerPackets(int nextId)}), which only made sense for Forge's old int-indexed
 * SimpleNetworkWrapper channel. NeoForge registers payloads by ResourceLocation type through a
 * PayloadRegistrar handed out by RegisterPayloadHandlersEvent, so there is no "next id" to hand back;
 * listeners just register their payload types (and handlers) against the shared registrar directly.
 */
public interface IPacketRegisterListener {

    /**
     * Called while the networking area builds its RegisterPayloadHandlersEvent handler, once per listener.
     */
    void registerPackets(PayloadRegistrar registrar);
}
