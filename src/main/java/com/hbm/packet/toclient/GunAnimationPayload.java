package com.hbm.packet.toclient;

import com.hbm.main.MainRegistry;
import com.hbm.weapon.anim.HbmAnimationType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Predicate;

/**
 * S2C "start playing this animation now" trigger, replacing CE's {@code GunAnimationPacket} /
 * {@code GunAnimationPacketSedna} (see {@code docs/phase3/weapon_animation_hooks.md}, read in full).
 * Deliberately built on the newer, wider Sedna wire shape ({@code {short type, int receiverIndex,
 * int itemIndex}}) rather than the legacy packet's narrower {@code {int type, EnumHand hand}}, per
 * that report's own "avoid a second payload type later" recommendation - {@code receiverIndex} is
 * carried now even though nothing populates it with a non-zero value yet, so a future
 * multi-receiver/multi-barrel weapon package doesn't have to bump {@code HbmNetwork}'s
 * {@code PROTOCOL_VERSION} or register a second payload type just to add the field CE itself
 * already needed for that case.
 * <p>
 * Wire-thrifty by design, matching CE's own choice: {@code animationType} travels as a
 * {@code short} ordinal into whichever concrete {@link HbmAnimationType} enum the sending item
 * type implies, rather than a full enum name; {@code hand} travels as a single byte (0 = main,
 * 1 = off - the same {@code EnumHand hand > 0 ? OFF : MAIN} trick CE's legacy packet used, decoded
 * back out via {@link #interactionHand()}) rather than a verbose encoding - because this packet is
 * sent on every single shot of every automatic weapon in the game.
 * <p>
 * The receiving side is expected to resolve the ordinal against the concrete animation enum by
 * checking the actual held item's type first (exactly like CE's own
 * {@code GunAnimationPacketSedna.Handler}, which gates on {@code stack.getItem() instanceof
 * ItemGunBaseNT} before ever calling {@code AnimationEnums.GunAnimation.values()[m.type]}) - no
 * family discriminator travels on the wire, since {@link #animationType()} alone is ambiguous
 * between {@link com.hbm.weapon.anim.GunAnimationType} and
 * {@link com.hbm.weapon.anim.ToolAnimationType} without that context.
 * <p>
 * <b>Phase 3 scope note:</b> {@link #handleClient} is an intentional stub. The client-side per-slot
 * {@code Animation} state array and the renderer that samples it back out
 * ({@code HbmAnimations}/{@code HbmAnimationsSedna}, both of which read
 * {@code Minecraft.getInstance().player} and, for the Sedna version, call
 * {@code GlStateManager} directly) are Phase 5 scope per the research report's "Deferred scope"
 * section. Phase 3 only needs the wire contract (this class) and the server-side send API
 * ({@link #triggerGunAnimation}) to exist, so the future gun-framework package has something real
 * to call without this payload's shape needing to churn once Phase 5 fills the handler body in.
 */
public record GunAnimationPayload(short animationType, byte hand, int receiverIndex) implements CustomPacketPayload {

    public static final Type<GunAnimationPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "gun_animation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunAnimationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.SHORT, GunAnimationPayload::animationType,
            ByteBufCodecs.BYTE, GunAnimationPayload::hand,
            ByteBufCodecs.VAR_INT, GunAnimationPayload::receiverIndex,
            GunAnimationPayload::new
    );

    /** Decodes the wire-thrifty {@link #hand()} byte back into an {@link InteractionHand}. */
    public InteractionHand interactionHand() {
        return hand == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    /**
     * Server-side send API, matching the {@code startAnimation(stack, name)}-shaped hook the
     * research report asked for. Mirrors CE's own {@code ItemGunBase} trigger call sites
     * ({@code spawnProjectile()}'s {@code CYCLE} send, {@code startReloadAction()}'s {@code RELOAD}
     * send), normalized to always guard on "is an animation actually configured for this type" -
     * CE itself only guarded the {@code CYCLE} send that way and sent {@code RELOAD}
     * unconditionally; the report explicitly recommends normalizing both to the same guarded shape
     * rather than porting that asymmetry verbatim, since an unconfigured animation type should
     * never cost a network packet (this fires on every shot of every automatic weapon).
     * <p>
     * The check itself is supplied by the caller as {@code isAnimationConfigured} rather than this
     * method reaching into a {@code GunConfiguration.animations} map directly, because that
     * config class does not exist in this port yet (it belongs to the not-yet-ported
     * {@code gun_framework_core} package). The future gun framework's own {@code fire()}/
     * {@code reload()} logic is expected to call this as e.g.
     * {@code triggerGunAnimation(player, stack, hand, GunAnimationType.CYCLE, mainConfig.animations::containsKey)}
     * - a drop-in replacement for CE's {@code this.mainConfig.animations.containsKey(type)} check.
     *
     * @param stack the gun/tool stack the animation belongs to; not read by this generic check
     *              today, kept in the signature for the future per-stack/per-attachment config
     *              lookups CE's own {@code getConfig(stack, ...)} pattern implies (e.g. Sedna's
     *              multi-receiver weapons resolving a different animation set per attachment).
     */
    public static void triggerGunAnimation(ServerPlayer player, ItemStack stack, InteractionHand hand,
                                            HbmAnimationType type, Predicate<HbmAnimationType> isAnimationConfigured) {
        if (!isAnimationConfigured.test(type)) {
            return;
        }

        short ordinal = (short) ((Enum<?>) type).ordinal();
        byte handByte = (byte) (hand == InteractionHand.OFF_HAND ? 1 : 0);
        PacketDistributor.sendToPlayer(player, new GunAnimationPayload(ordinal, handByte, 0));
    }

    public static void handleCommon(GunAnimationPayload packet, IPayloadContext context) {
        handleClient(packet, context);
    }

    /**
     * Stub handler - see class javadoc's "Phase 3 scope note". Deliberately does nothing beyond a
     * debug log line; Phase 5 replaces this body with the real
     * {@code HbmAnimations.hotbar[slot] = new Animation(...)} (or Sedna equivalent) stamp once the
     * client-side state array and renderer exist. Left un-guarded by {@code @OnlyIn(Dist.CLIENT)}
     * the way {@link BufPacket#handleClient} is, since a debug log line has no client-only
     * dependency yet; add the annotation back once this body starts touching
     * {@code Minecraft.getInstance()}.
     */
    public static void handleClient(GunAnimationPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> MainRegistry.logger.debug(
                "Received GunAnimationPayload (animationType={}, hand={}, receiverIndex={}) - client-side animation " +
                        "playback is Phase 5 scope, ignoring for now.",
                packet.animationType(), packet.interactionHand(), packet.receiverIndex()));
    }

    @Override
    public Type<GunAnimationPayload> type() {
        return TYPE;
    }
}
