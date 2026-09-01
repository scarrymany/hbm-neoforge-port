package com.hbm.packet;

import com.hbm.main.MainRegistry;
import com.hbm.packet.toclient.BufPacket;
import com.hbm.packet.toclient.ExplosionEffectSyncPacket;
import com.hbm.packet.toclient.ExplosionRemovalSyncPacket;
import com.hbm.packet.toclient.GunAnimationPayload;
import com.hbm.packet.toclient.HbmEffectPacket;
import com.hbm.packet.toclient.NukeExplosionRemovalSyncPacket;
import com.hbm.packet.toclient.RadFogPayload;
import com.hbm.packet.toclient.SatPanelPayload;
import com.hbm.packet.toserver.AnvilCraftPacket;
import com.hbm.packet.toserver.ElectrolyserControlPacket;
import com.hbm.packet.toserver.FusionControlPacket;
import com.hbm.packet.toserver.KeypadServerPacket;
import com.hbm.packet.toserver.ItemControlPacket;
import com.hbm.packet.toserver.KeybindPacket;
import com.hbm.packet.toserver.LaunchPadRustedControlPacket;
import com.hbm.packet.toserver.MassStorageControlPacket;
import com.hbm.packet.toserver.SatPanelActionPayload;
import com.hbm.packet.toserver.TurretControlPacket;
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

        // Phase 3 (gun_framework_core): C2S keybind press/release sync, dispatching to IKeybindReceiver.
        // Fixes a pre-existing gap - HbmKeybindInputEvents/HbmKeybinds already sent/expected this packet
        // shape but com.hbm.packet.KeybindPacket never existed anywhere in the tree before Phase 3.
        registrar.playToServer(KeybindPacket.TYPE, KeybindPacket.STREAM_CODEC, KeybindPacket::handleServer);

        // Phase 3 (turret_system): C2S mob-filter/whitelist mutation for the bare-Screen mob-filter GUI (no backing Menu).
        registrar.playToServer(TurretControlPacket.TYPE, TurretControlPacket.STREAM_CODEC, TurretControlPacket::handleServer);

        // Phase 3 (missile_launch_infra): S2C live satellite-panel stream + C2S panel control round trip.
        registrar.playToClient(SatPanelPayload.TYPE, SatPanelPayload.STREAM_CODEC, SatPanelPayload::handleClient);
        registrar.playToServer(SatPanelActionPayload.TYPE, SatPanelActionPayload.STREAM_CODEC, SatPanelActionPayload::handleServer);

        // Phase 4 (chunk_radiation_system): S2C decorative "radiation fog" particle cue.
        registrar.playToClient(RadFogPayload.TYPE, RadFogPayload.STREAM_CODEC, RadFogPayload::handleClient);

        // Phase 5 (particle_engine_and_generic_vfx): generic S2C "spawn this HbmEffect at (x,y,z)"
        // broadcast, replacing CE's AuxParticlePacketNT/HbmEffectNT dispatch.
        registrar.playToClient(HbmEffectPacket.TYPE, HbmEffectPacket.STREAM_CODEC, HbmEffectPacket::handleClient);

        // Phase 5 (gui_screens_survey_weapons_storage_special): C2S control packet for the rusted
        // launch pad's bare-Screen GUI.
        registrar.playToServer(LaunchPadRustedControlPacket.TYPE, LaunchPadRustedControlPacket.STREAM_CODEC, LaunchPadRustedControlPacket::handleServer);

        registrar.playToServer(MassStorageControlPacket.TYPE, MassStorageControlPacket.STREAM_CODEC, MassStorageControlPacket::handleServer);
        registrar.playToServer(ElectrolyserControlPacket.TYPE, ElectrolyserControlPacket.STREAM_CODEC, ElectrolyserControlPacket::handleServer);
        registrar.playToServer(AnvilCraftPacket.TYPE, AnvilCraftPacket.STREAM_CODEC, AnvilCraftPacket::handleServer);
        registrar.playToServer(FusionControlPacket.TYPE, FusionControlPacket.STREAM_CODEC, FusionControlPacket::handleServer);
        registrar.playToServer(KeypadServerPacket.TYPE, KeypadServerPacket.STREAM_CODEC, KeypadServerPacket::handleServer);
    }
}
