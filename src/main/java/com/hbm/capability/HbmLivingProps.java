package com.hbm.capability;

import com.hbm.config.RadiationConfig;
import com.hbm.damage.ModDamageTypes;
import com.hbm.main.AdvancementManager;
import com.hbm.main.MainRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;

/**
 * Thin static facade over {@link HbmLivingAttachment}, ported from CE's
 * {@code com.hbm.capability.HbmLivingProps}. CE split its per-entity hazard state into two layers
 * (raw data capability + this game-logic facade with config gates, instadeath thresholds, the
 * digamma health-attribute modifier, and player chat feedback); this port already shipped layer 1
 * as {@link HbmLivingAttachment} (its own javadoc explicitly deferred this facade to "a later
 * status-effect system phase built on top of this attachment" - this class is that phase).
 *
 * <p>Kept under CE's exact name rather than folding into {@link HbmLivingAttachment} directly:
 * {@code HazardTypeAsbestos}/{@code HazardTypeCoal}/{@code HazardTypeCold}/{@code HazardTypeToxic}/
 * {@code HazardTypeRadiation} and {@code com.hbm.util.ContaminationUtil} already import
 * {@code com.hbm.capability.HbmLivingProps} by this exact name, live and uncommented.
 *
 * <p>Every mutating method here re-calls {@code entity.setData(ModAttachments.LIVING_ATTACHMENT,
 * props)} after mutating the fetched attachment instance, per {@link HbmLivingAttachment}'s own
 * documented NeoForge attachment-sync contract (in-place mutation alone does not notify watching
 * clients) - matching the Neo Edition reference's identical pattern in its equivalent facade.
 *
 * <p>CE's player-feedback packets ({@code PlayerInformPacketLegacy} on asbestos/blacklung
 * threshold) are UI polish, not logic - ported as {@code player.sendSystemMessage(...)} calls
 * instead, matching the pattern {@code ItemDosimeter}/{@code ItemGeigerCounter} already use. No new
 * {@code CustomPacketPayload} is needed anywhere in this class.
 */
public final class HbmLivingProps {

    private HbmLivingProps() {
    }

    /**
     * Modifier id for the digamma max-health scaling modifier. CE keyed this by a random
     * {@code UUID} ({@code digamma_UUID}); 1.20.5+ replaced UUID-keyed attribute modifiers with
     * {@link ResourceLocation}-keyed ones - confirmed against the Neo Edition reference's
     * {@code HbmLivingAttachments#setDigamma}, which applies the identical modifier shape against
     * {@link Attributes#MAX_HEALTH}.
     */
    private static final ResourceLocation DIGAMMA_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "digamma");

    // ==================== RADIATION ====================

    public static double getRadiation(LivingEntity entity) {
        if (!RadiationConfig.ENABLE_CONTAMINATION.get()) return 0D;
        return HbmLivingAttachment.getData(entity).getRads();
    }

    public static void setRadiation(LivingEntity entity, double rad) {
        if (!RadiationConfig.ENABLE_CONTAMINATION.get()) return;
        HbmLivingAttachment props = HbmLivingAttachment.getData(entity);
        props.setRads(rad);
        entity.setData(ModAttachments.LIVING_ATTACHMENT, props);
    }

    /**
     * CE clamps the incremented total to {@code [0, 25000000]} here; this port's
     * {@link HbmLivingAttachment#setRads} already clamps every write to the tighter
     * {@code [0, MAX_RADS=2500]} range, so that clamp is left in charge here rather than
     * loosened back out to CE's much higher ceiling.
     */
    public static void incrementRadiation(LivingEntity entity, double rad) {
        if (!RadiationConfig.ENABLE_CONTAMINATION.get()) return;
        HbmLivingAttachment props = HbmLivingAttachment.getData(entity);
        props.increaseRads(rad);
        entity.setData(ModAttachments.LIVING_ATTACHMENT, props);
    }

    // ==================== NEUTRON RADIATION ====================

    public static double getNeutron(LivingEntity entity) {
        return HbmLivingAttachment.getData(entity).getNeutrons();
    }

    public static void setNeutron(LivingEntity entity, double rad) {
        HbmLivingAttachment props = HbmLivingAttachment.getData(entity);
        props.setNeutrons(rad);
        entity.setData(ModAttachments.LIVING_ATTACHMENT, props);
    }

    // ==================== RAD ENV ====================

    public static double getRadEnv(LivingEntity entity) {
        return HbmLivingAttachment.getData(entity).getRadsEnv();
    }

    public static void setRadEnv(LivingEntity entity, double rad) {
        HbmLivingAttachment props = HbmLivingAttachment.getData(entity);
        props.setRadsEnv(rad);
        entity.setData(ModAttachments.LIVING_ATTACHMENT, props);
    }

    // ==================== RAD BUF ====================

    public static double getRadBuf(LivingEntity entity) {
        return HbmLivingAttachment.getData(entity).getRadBuf();
    }

    public static void setRadBuf(LivingEntity entity, double rad) {
        HbmLivingAttachment props = HbmLivingAttachment.getData(entity);
        props.setRadBuf(rad);
        entity.setData(ModAttachments.LIVING_ATTACHMENT, props);
    }

    // ==================== DIGAMMA ====================

    public static double getDigamma(LivingEntity entity) {
        return HbmLivingAttachment.getData(entity).getDigamma();
    }

    /**
     * Sets digamma exposure and re-applies the max-health-scaling attribute modifier
     * ({@code healthMod = 0.5^digamma - 1}, {@link AttributeModifier.Operation#ADD_MULTIPLIED_TOTAL}
     * - see this area's research report, API decision #3), then instakills the entity at
     * {@code digamma >= 10} (or once max health has been scaled down to nothing), matching CE 1:1.
     *
     * <p>CE's soul-sand particle burst on instadeath ({@code AuxParticlePacketNT} via
     * {@code PacketThreading}) is cosmetic polish that depends on pre-1.21 particle-packet plumbing
     * this port doesn't have yet - dropped rather than stubbed, since it doesn't affect the actual
     * hazard logic this facade is responsible for. CE's three digamma-exposure advancement grants
     * ({@link AdvancementManager#digammaSee}/{@link AdvancementManager#digammaFeel}/
     * {@link AdvancementManager#digammaKnow}, re-reading the just-stored value at thresholds
     * {@code >0}/{@code >=2}/{@code >=10}) <b>are</b> wired below - {@code AdvancementManager} landed
     * later in this same Phase 4 wave, after this method was first written.
     */
    public static void setDigamma(LivingEntity entity, double digamma) {
        HbmLivingAttachment props = HbmLivingAttachment.getData(entity);
        props.setDigamma(digamma);
        entity.setData(ModAttachments.LIVING_ATTACHMENT, props);

        double healthMod = Math.pow(0.5, digamma) - 1D;

        AttributeInstance attributeInstance = entity.getAttribute(Attributes.MAX_HEALTH);

        if (attributeInstance != null) {
            attributeInstance.removeModifier(DIGAMMA_MODIFIER_ID);
            attributeInstance.addPermanentModifier(new AttributeModifier(
                    DIGAMMA_MODIFIER_ID, healthMod, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

            if (entity.getHealth() > entity.getMaxHealth()) {
                entity.setHealth(entity.getMaxHealth());
            }
        }

        if ((entity.getMaxHealth() <= 0 || digamma >= 10.0D) && entity.isAlive()) {
            entity.setAbsorptionAmount(0F);
            entity.hurt(entity.damageSources().source(ModDamageTypes.DIGAMMA), 5_000_000F);
            entity.setHealth(0F);
        }

        // CE: HbmLivingProps#setDigamma's own trailing `if (entity instanceof EntityPlayer)` block -
        // re-reads the just-stored value and grants 3 tiered advancements. Was dropped by the Phase 3
        // foundation wave with the documented reason "an AdvancementManager this port doesn't have
        // yet" (see this method's own javadoc) - com.hbm.main.AdvancementManager landed in this same
        // Phase 4 wave with digammaSee/digammaFeel/digammaKnow already declared and loaded but never
        // granted from anywhere; wired here now that the dependency is real.
        if (entity instanceof ServerPlayer serverPlayer) {
            double di = HbmLivingAttachment.getData(entity).getDigamma();

            if (di > 0D) AdvancementManager.grantAchievement(serverPlayer, AdvancementManager.digammaSee);
            if (di >= 2D) AdvancementManager.grantAchievement(serverPlayer, AdvancementManager.digammaFeel);
            if (di >= 10D) AdvancementManager.grantAchievement(serverPlayer, AdvancementManager.digammaKnow);
        }
    }

    /** CE clamps the running total to {@code [0, 10]} here, distinct from the attachment's own wider {@code [0, 1000]} clamp. */
    public static void incrementDigamma(LivingEntity entity, double digamma) {
        double dRad = HbmLivingAttachment.getData(entity).getDigamma() + digamma;

        if (dRad > 10D) dRad = 10D;
        if (dRad < 0D) dRad = 0D;

        setDigamma(entity, dRad);
    }

    // ==================== ASBESTOS ====================

    public static int getAsbestos(LivingEntity entity) {
        return HbmLivingAttachment.getData(entity).getAsbestos();
    }

    public static void setAsbestos(LivingEntity entity, int asbestos) {
        HbmLivingAttachment props = HbmLivingAttachment.getData(entity);
        props.setAsbestos(asbestos);
        entity.setData(ModAttachments.LIVING_ATTACHMENT, props);

        if (asbestos >= HbmLivingAttachment.MAX_ASBESTOS) {
            props.setAsbestos(0);
            entity.setData(ModAttachments.LIVING_ATTACHMENT, props);
            entity.hurt(entity.damageSources().source(ModDamageTypes.ASBESTOS), 1000F);
        }
    }

    public static void incrementAsbestos(LivingEntity entity, int asbestos) {
        setAsbestos(entity, getAsbestos(entity) + asbestos);

        if (entity instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable("info.asbestos").withStyle(ChatFormatting.RED));
        }
    }

    public static void addCont(LivingEntity entity, ContaminationEffect cont) {
        HbmLivingAttachment props = HbmLivingAttachment.getData(entity);
        props.getContaminationEffectList().add(cont);
        entity.setData(ModAttachments.LIVING_ATTACHMENT, props);
    }

    public static List<ContaminationEffect> getCont(LivingEntity entity) {
        return HbmLivingAttachment.getData(entity).getContaminationEffectList();
    }

    // ==================== BLACK LUNG DISEASE ====================

    public static int getBlackLung(LivingEntity entity) {
        return HbmLivingAttachment.getData(entity).getBlacklung();
    }

    public static void setBlackLung(LivingEntity entity, int blacklung) {
        HbmLivingAttachment props = HbmLivingAttachment.getData(entity);
        props.setBlacklung(blacklung);
        entity.setData(ModAttachments.LIVING_ATTACHMENT, props);

        if (blacklung >= HbmLivingAttachment.MAX_BLACKLUNG) {
            props.setBlacklung(0);
            entity.setData(ModAttachments.LIVING_ATTACHMENT, props);
            entity.hurt(entity.damageSources().source(ModDamageTypes.BLACKLUNG), 1000F);
        }
    }

    public static void incrementBlackLung(LivingEntity entity, int blacklung) {
        setBlackLung(entity, getBlackLung(entity) + blacklung);

        if (entity instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable("info.coaldust").withStyle(ChatFormatting.RED));
        }
    }

    // ==================== TIME BOMB ====================

    public static int getTimer(LivingEntity entity) {
        return HbmLivingAttachment.getData(entity).getBombTimer();
    }

    public static void setTimer(LivingEntity entity, int bombTimer) {
        HbmLivingAttachment props = HbmLivingAttachment.getData(entity);
        props.setBombTimer(bombTimer);
        entity.setData(ModAttachments.LIVING_ATTACHMENT, props);
    }

    // ==================== CONTAGION ====================

    /** {@link HbmLivingAttachment#getContagion()} already gates on {@code ServerConfig.ENABLE_MKU}. */
    public static int getContagion(LivingEntity entity) {
        return HbmLivingAttachment.getData(entity).getContagion();
    }

    public static void setContagion(LivingEntity entity, int contagion) {
        HbmLivingAttachment props = HbmLivingAttachment.getData(entity);
        props.setContagion(contagion);
        entity.setData(ModAttachments.LIVING_ATTACHMENT, props);
    }

    // ==================== OIL ====================

    public static int getOil(LivingEntity entity) {
        return HbmLivingAttachment.getData(entity).getOil();
    }

    public static void setOil(LivingEntity entity, int oil) {
        HbmLivingAttachment props = HbmLivingAttachment.getData(entity);
        props.setOil(oil);
        entity.setData(ModAttachments.LIVING_ATTACHMENT, props);
    }
}
