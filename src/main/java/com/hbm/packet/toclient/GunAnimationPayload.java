package com.hbm.packet.toclient;

import com.hbm.client.render.item.weapon.GunAnimationClientState;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.main.MainRegistry;
import com.hbm.weapon.anim.GunAnimationType;
import com.hbm.weapon.anim.HbmAnimationType;
import com.hbm.render.anim.sedna.BusAnimationSedna;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.BiFunction;
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
 * <b>{@link #gunIndex()}</b> - added by Phase 5 ({@code c6-weapon-gun-rendering}), CE's own
 * {@code GunAnimationPacketSedna}'s third field ({@code itemIndex}, the {@code configs_DNA} index
 * selecting <i>which</i> of a multi-mode gun's {@link com.hbm.items.weapon.sedna.GunConfig}
 * instances the animation belongs to - e.g. the two independent akimbo-pistol configs on one
 * {@code ItemStack}). Phase 3's original 3-field record only carried {@code receiverIndex}
 * (always sent as {@code 0} - see {@link #triggerGunAnimation}'s original 5-arg overload, kept
 * below unchanged) and silently dropped the {@code index} argument
 * {@code ItemGunBaseNT.playAnimation(player, stack, type, index)} already threads through 21 real
 * call sites - confirmed by reading that method's body, which calls the old
 * {@code triggerGunAnimation(player, stack, hand, type, t -> true)} 5-arg overload and discards
 * {@code index} entirely. This is a real, if narrow, bug in the already-committed Phase 3 wiring
 * (single-config guns, the overwhelming majority, were and remain unaffected - {@code index} is
 * always {@code 0} for them) - fixed here by adding a 4th wire field and a new 6-arg
 * {@link #triggerGunAnimation} overload that {@code ItemGunBaseNT.playAnimation} now calls instead
 * (one-line change, see that method). The 3 tool-animation call sites
 * ({@code ItemSwordCutter}/{@code ItemChainsaw}/{@code ItemBoltgun}, none of which have a
 * multi-config concept) keep using the original 5-arg overload unchanged, which now forwards
 * {@code gunIndex=0}.
 * <p>
 * <b>{@link #handleClient}</b> - filled in by Phase 5 ({@code c6-weapon-gun-rendering}), replacing
 * the Phase 3 debug-log stub. Populates {@link GunAnimationClientState}'s hotbar/rail array
 * (the {@code HbmAnimationsSedna.hotbar}-equivalent this port keeps deliberately client-only per
 * {@code docs/phase5/weapon_gun_rendering_animloader.md}'s "Two competing designs" recommendation)
 * and, on a {@link GunAnimationType#CYCLE} animation, stamps
 * {@link ItemGunBaseNT#lastShot}/{@link ItemGunBaseNT#shotRand} - mirroring CE's own
 * {@code GunAnimationPacketSedna.Handler.handleSedna} line-for-line for those two responsibilities
 * (the recoil-callback invocation CE's handler also does on {@code CYCLE} is deliberately not
 * ported here - see this method's own body comment for why).
 */
public record GunAnimationPayload(short animationType, byte hand, int receiverIndex, int gunIndex) implements CustomPacketPayload {

    public static final Type<GunAnimationPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "gun_animation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunAnimationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.SHORT, GunAnimationPayload::animationType,
            ByteBufCodecs.BYTE, GunAnimationPayload::hand,
            ByteBufCodecs.VAR_INT, GunAnimationPayload::receiverIndex,
            ByteBufCodecs.VAR_INT, GunAnimationPayload::gunIndex,
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
        triggerGunAnimation(player, stack, hand, type, 0, isAnimationConfigured);
    }

    /**
     * 6-arg overload carrying the gun/config index - see this class's own javadoc,
     * {@link #gunIndex()}. {@code ItemGunBaseNT.playAnimation} calls this one; the 3 tool-animation
     * call sites keep using the 5-arg overload above (which forwards {@code gunIndex=0}).
     */
    public static void triggerGunAnimation(ServerPlayer player, ItemStack stack, InteractionHand hand,
                                            HbmAnimationType type, int gunIndex, Predicate<HbmAnimationType> isAnimationConfigured) {
        if (!isAnimationConfigured.test(type)) {
            return;
        }

        short ordinal = (short) ((Enum<?>) type).ordinal();
        byte handByte = (byte) (hand == InteractionHand.OFF_HAND ? 1 : 0);
        PacketDistributor.sendToPlayer(player, new GunAnimationPayload(ordinal, handByte, 0, gunIndex));
    }

    public static void handleCommon(GunAnimationPayload packet, IPayloadContext context) {
        handleClient(packet, context);
    }

    /**
     * Filled in by Phase 5 ({@code c6-weapon-gun-rendering}) - see class javadoc. Mirrors CE's own
     * {@code GunAnimationPacketSedna.Handler.handleSedna} for the two responsibilities this port's
     * split design keeps client-side:
     * <ol>
     *   <li>On {@link GunAnimationType#CYCLE}, stamp {@link ItemGunBaseNT#lastShot}/
     *       {@link ItemGunBaseNT#shotRand} (read by every concrete gun renderer's muzzle-flash
     *       helper) - CE: {@code gun.lastShot[gunIndex] = System.currentTimeMillis(); gun.shotRand =
     *       player.world.rand.nextDouble();}. <b>Not ported</b>: CE's handler also invokes the
     *       receiver's {@code onRecoil} callback here
     *       ({@code Receiver.getRecoil(stack).accept(stack, new LambdaContext(...))}) for a
     *       client-only camera view-punch effect. This port's {@code ItemGunBaseNT.LambdaContext}'s
     *       4th constructor argument is named/documented as {@code configIndex} (not CE's
     *       {@code receiverIndex}) throughout the rest of this port's already-committed gun
     *       framework (every other call site passes a config index there, never a receiver index) -
     *       since this payload's own {@link #receiverIndex()} field is itself always {@code 0} today
     *       (no call site populates it with anything else, confirmed by grep), invoking a
     *       receiver-keyed client-only recoil callback here would either be a no-op (index 0 only)
     *       or require guessing at a {@code LambdaContext} semantic this class did not itself
     *       define. Deliberately deferred rather than guessed at - a future pass wiring real
     *       {@code recoil(...)} lambdas for concrete guns should add this call once the
     *       receiver-vs-config index question is resolved.</li>
     *   <li>Look up the gun's registered animation for this trigger
     *       ({@link GunConfig#getAnims(ItemStack)}, added by this same Phase 5 task - see that
     *       class's own javadoc) and, if one exists, stamp
     *       {@link GunAnimationClientState#hotbar}{@code [slot][gunIndex]} with a fresh
     *       {@code Animation(key, System.currentTimeMillis(), animation, type, holdLastFrame)} -
     *       CE: {@code HbmAnimationsSedna.hotbar[slot][gunIndex] = new Animation(...)}. The
     *       {@code RELOAD_EMPTY -> RELOAD} fallback CE performs here does not apply (this port's
     *       {@link GunAnimationType} never sends the deprecated {@code RELOAD_EMPTY} value at all -
     *       see that enum's own javadoc); the {@code ALT_CYCLE -> CYCLE} / {@code CYCLE_EMPTY ->
     *       CYCLE} fallback is ported (this port's vocabulary also lacks {@code CYCLE_EMPTY}, so
     *       only the {@code ALT_CYCLE} half applies).</li>
     * </ol>
     * Tool animations ({@link com.hbm.weapon.anim.ToolAnimationType}) are intentionally left
     * unhandled - no CE gun/tool in the roster this task ported drives {@code BusAnimationSedna}
     * playback off a {@code ToolAnimationType} trigger (CE's melee tools that do animate,
     * e.g. the chainsaw, use the Collada {@code animloader} system instead, explicitly out of this
     * task's scope) - the debug log line the Phase 3 stub had is kept for that branch so a future
     * tool-rendering task has a visible signal rather than a silent no-op.
     */
    @OnlyIn(Dist.CLIENT)
    public static void handleClient(GunAnimationPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty()) return;

            if (!(stack.getItem() instanceof ItemGunBaseNT gun)) {
                MainRegistry.logger.debug(
                        "Received GunAnimationPayload for a non-ItemGunBaseNT held item ({}) - tool " +
                                "animation playback is out of c6-weapon-gun-rendering's scope, ignoring.",
                        stack.getItem());
                return;
            }

            int gunIndex = packet.gunIndex();
            if (gunIndex < 0 || gunIndex >= GunAnimationClientState.hotbar[0].length) return;
            if (gunIndex >= gun.getConfigCount()) return;

            int slot = player.getInventory().selected;
            if (slot < 0 || slot > 8) slot = Math.abs(slot) % 9;

            GunAnimationType[] values = GunAnimationType.values();
            int ordinal = packet.animationType();
            if (ordinal < 0 || ordinal >= values.length) return;
            GunAnimationType type = values[ordinal];

            if (type == GunAnimationType.CYCLE) {
                if (gunIndex < gun.lastShot.length) gun.lastShot[gunIndex] = System.currentTimeMillis();
                gun.shotRand = player.level().random.nextDouble();
            }

            GunConfig config = gun.getConfig(stack, gunIndex);
            BiFunction<ItemStack, HbmAnimationType, BusAnimationSedna> anims = config.getAnims(stack);
            if (anims == null) return;

            BusAnimationSedna animation = anims.apply(stack, type);
            if (animation == null && type == GunAnimationType.ALT_CYCLE) {
                animation = anims.apply(stack, GunAnimationType.CYCLE);
            }

            if (animation != null) {
                boolean isReloadAnimation = type == GunAnimationType.RELOAD || type == GunAnimationType.RELOAD_CYCLE;
                GunAnimationClientState.hotbar[slot][gunIndex] = new GunAnimationClientState.Animation(
                        stack.getItem().getDescriptionId(), System.currentTimeMillis(), animation, type,
                        isReloadAnimation && config.getReloadAnimSequential(stack));
            }
        });
    }

    @Override
    public Type<GunAnimationPayload> type() {
        return TYPE;
    }
}
