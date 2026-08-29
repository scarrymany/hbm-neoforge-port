package com.hbm.packet;

import com.hbm.main.MainRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Central registration point for every {@code CustomPacketPayload} the mod sends, mirroring the role CE's
 * {@code PacketDispatcher} played under the old FML SimpleImpl networking stack.
 * <p>
 * Unlike CE, NeoForge keys packets by {@link net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type}
 * (a namespaced id) rather than a sequential int discriminator, and payloads are sent with
 * {@link net.neoforged.neoforge.network.PacketDistributor} directly instead of through a custom wrapper class -
 * so there is no {@code sendTo} helper here, and none is needed.
 * <p>
 * This class registers zero concrete packets for now. Every later phase that introduces a packet adds one
 * {@code registrar.playToClient(...)} or {@code registrar.playToServer(...)} line here, following the shape
 * described below. Concrete packets themselves live under {@code com.hbm.packet.toclient} / {@code toserver},
 * mirroring CE's package layout, and are added by whichever phase owns the feature that needs them.
 * <p>
 * Per-packet shape for later phases:
 * <pre>
 * public record SomePacket(...) implements CustomPacketPayload {
 *     public static final Type&lt;SomePacket&gt; TYPE =
 *         new Type&lt;&gt;(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "some_packet"));
 *
 *     public static final StreamCodec&lt;RegistryFriendlyByteBuf, SomePacket&gt; STREAM_CODEC = ...;
 *
 *     public static void handleServer(SomePacket packet, IPayloadContext context) {
 *         context.enqueueWork(() -&gt; { ... });
 *     }
 *
 *     &#64;Override public Type&lt;SomePacket&gt; type() { return TYPE; }
 * }
 * </pre>
 */
@EventBusSubscriber(modid = MainRegistry.MODID)
public class HbmNetwork {

    /**
     * Bumped whenever a wire-incompatible change is made to a registered payload's codec. Independent of
     * Neo Edition's own protocol version - this port tracks its own compatibility history.
     */
    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // Phase 0: no concrete packets yet. Later phases append playToClient/playToServer registrations here,
        // one per packet, as each owning feature system is ported.
    }
}
