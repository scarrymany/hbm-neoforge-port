package com.hbm.packet;

import com.hbm.main.MainRegistry;
import com.hbm.packet.toclient.BufPacket;
import com.hbm.packet.toclient.ExplosionEffectSyncPacket;
import com.hbm.packet.toclient.ExplosionRemovalSyncPacket;
import com.hbm.packet.toclient.GunAnimationPayload;
import com.hbm.packet.toclient.NukeExplosionRemovalSyncPacket;
import com.hbm.packet.toserver.ItemControlPacket;
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
// bus = Bus.MOD required: RegisterPayloadHandlersEvent implements IModBusEvent and only fires on the
// mod bus - @EventBusSubscriber's bus() defaults to Bus.GAME and does not auto-detect IModBusEvent
// (confirmed against real NeoForge 1.21.1 source and FancyModLoader's EventBusSubscriber javadoc).
@EventBusSubscriber(modid = MainRegistry.MODID, bus = EventBusSubscriber.Bus.MOD)
public class HbmNetwork {

    /**
     * Bumped whenever a wire-incompatible change is made to a registered payload's codec. Independent of
     * Neo Edition's own protocol version - this port tracks its own compatibility history.
     */
    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // Phase 2 (com.hbm.blockentity base framework): the generic ByteBuf block-entity sync payload used by
        // LoadedBaseBlockEntity#networkPackNT/networkPackMK2. Every later phase appends its own
        // playToClient/playToServer registrations here, one per packet, as each owning feature system is ported.
        registrar.playToClient(BufPacket.TYPE, BufPacket.STREAM_CODEC, BufPacket::handleCommon);

        // Phase 3 (explosion_vanillant_core): client-visible explosion removal/effect sync.
        registrar.playToClient(ExplosionRemovalSyncPacket.TYPE, ExplosionRemovalSyncPacket.STREAM_CODEC, ExplosionRemovalSyncPacket::handleClient);
        registrar.playToClient(ExplosionEffectSyncPacket.TYPE, ExplosionEffectSyncPacket.STREAM_CODEC, ExplosionEffectSyncPacket::handleClient);

        // Phase 3 (nuke_explosion_entities): a distinct, separately-registered payload pair from the
        // vanillant pair above - see NukeExplosionRemovalSyncPacket's javadoc for why.
        registrar.playToClient(NukeExplosionRemovalSyncPacket.TYPE, NukeExplosionRemovalSyncPacket.STREAM_CODEC, NukeExplosionRemovalSyncPacket::handleClient);

        // Phase 3 (weapon animation triggers, com.hbm.weapon.anim): S2C "start playing this animation
        // now" hook. Stub client handler - see GunAnimationPayload's class javadoc.
        registrar.playToClient(GunAnimationPayload.TYPE, GunAnimationPayload.STREAM_CODEC, GunAnimationPayload::handleCommon);

        // Phase 3 (scattered_military_items / weapon_animation_hooks): generic C2S "apply this NBT to
        // whatever the player is holding" control packet, dispatching to IItemControlReceiver.
        registrar.playToServer(ItemControlPacket.TYPE, ItemControlPacket.STREAM_CODEC, ItemControlPacket::handleServer);
    }
}
