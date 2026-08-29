package com.hbm.capability;

import com.hbm.config.ServerConfig;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-{@link LivingEntity} NTM status data, ported from CE's {@code HbmLivingCapability}
 * ({@code IEntityHbmProps} / {@code EntityHbmProps}).
 *
 * <p>This is a plain data holder: no config gates, no damage/advancement side effects, no packet
 * dispatch. CE split this exact same way ({@code HbmLivingCapability} for data,
 * {@code HbmLivingProps} for the game-logic facade on top of it); that facade - radiation math
 * with config-driven caps, digamma's health-modifier and instadeath, advancement grants,
 * contamination ticking - is explicitly out of scope for this capability-framework area and
 * belongs to a later status-effect system phase built on top of this attachment.
 *
 * <p>CE additionally versioned its NBT payload ({@code "fmt": "v1"} doubles vs. legacy floats) to
 * migrate older CE saves. There is no equivalent legacy save format for this port to migrate from,
 * so that versioning was dropped in favor of a single current schema; reintroduce it if whoever
 * owns save compatibility later decides old-CE world imports must be supported.
 *
 * <p>Whoever builds the game-logic facade on top of this class must remember NeoForge's attachment
 * contract: mutating an already-fetched instance in place does not by itself notify watching
 * clients. Call {@code entity.setData(ModAttachments.LIVING_ATTACHMENT, props)} after mutating to
 * mark the attachment dirty for sync, exactly as the confirmed-working Neo Edition reference port
 * does for its equivalent attachment.
 */
public final class HbmLivingAttachment {

    public static final Codec<HbmLivingAttachment> CODEC = CompoundTag.CODEC.xmap(
            tag -> {
                HbmLivingAttachment props = new HbmLivingAttachment();
                props.loadNBTData(tag);
                return props;
            },
            HbmLivingAttachment::saveNBTData
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, HbmLivingAttachment> STREAM_CODEC = StreamCodec.of(
            (buf, props) -> buf.writeNbt(props.saveNBTData()),
            buf -> {
                HbmLivingAttachment props = new HbmLivingAttachment();
                props.loadNBTData(buf.readNbt());
                return props;
            }
    );

    public static final int MAX_ASBESTOS = 60 * 60 * 20;
    public static final int MAX_BLACKLUNG = 60 * 60 * 20;
    private static final double MAX_RADS = 2500D;
    private static final double MAX_DIGAMMA = 1000D;

    private double rads;
    private double neutrons;
    private double radsEnv;
    private double radBuf;
    private double digamma;
    private int asbestos;
    private int blacklung;
    private int bombTimer;
    private int contagion;
    private int oil;
    private int phosphorus;
    private int fire;
    private int balefire;
    private int grenadeDeployment;
    private final List<ContaminationEffect> contamination = new ArrayList<>();

    public static HbmLivingAttachment getData(LivingEntity entity) {
        return entity.getData(ModAttachments.LIVING_ATTACHMENT);
    }

    public double getRads() {
        return rads;
    }

    public void setRads(double rads) {
        this.rads = clamp(rads, 0D, MAX_RADS);
    }

    public void increaseRads(double rads) {
        setRads(this.rads + rads);
    }

    public void decreaseRads(double rads) {
        setRads(this.rads - rads);
    }

    public double getNeutrons() {
        return neutrons;
    }

    public void setNeutrons(double neutrons) {
        this.neutrons = Math.max(neutrons, 0D);
    }

    public double getRadsEnv() {
        return radsEnv;
    }

    public void setRadsEnv(double radsEnv) {
        this.radsEnv = radsEnv;
    }

    public double getRadBuf() {
        return radBuf;
    }

    public void setRadBuf(double radBuf) {
        this.radBuf = radBuf;
    }

    public double getDigamma() {
        return digamma;
    }

    public void setDigamma(double digamma) {
        this.digamma = digamma;
    }

    public void increaseDigamma(double digamma) {
        this.digamma = clamp(this.digamma + digamma, 0D, MAX_DIGAMMA);
    }

    public void decreaseDigamma(double digamma) {
        this.digamma = clamp(this.digamma - digamma, 0D, MAX_DIGAMMA);
    }

    public int getAsbestos() {
        return asbestos;
    }

    public void setAsbestos(int asbestos) {
        this.asbestos = asbestos;
    }

    public int getBlacklung() {
        return blacklung;
    }

    public void setBlacklung(int blacklung) {
        this.blacklung = blacklung;
    }

    public int getBombTimer() {
        return bombTimer;
    }

    public void setBombTimer(int bombTimer) {
        this.bombTimer = bombTimer;
    }

    public int getContagion() {
        // Ported from CE's HbmLivingCapability.EntityHbmProps#getContagion: reports 0 while the
        // MKU system is disabled server-side, matching CE's config gate.
        if (!ServerConfig.ENABLE_MKU.get()) return 0;
        return contagion;
    }

    public void setContagion(int contagion) {
        this.contagion = contagion;
    }

    public int getOil() {
        return oil;
    }

    public void setOil(int oil) {
        this.oil = oil;
    }

    public int getPhosphorus() {
        return phosphorus;
    }

    public void setPhosphorus(int phosphorus) {
        this.phosphorus = phosphorus;
    }

    public int getFire() {
        return fire;
    }

    public void setFire(int fire) {
        this.fire = fire;
    }

    public int getBalefire() {
        return balefire;
    }

    public void setBalefire(int balefire) {
        this.balefire = balefire;
    }

    public int getGrenadeDeployment() {
        return grenadeDeployment;
    }

    public void setGrenadeDeployment(int grenadeDeployment) {
        this.grenadeDeployment = grenadeDeployment;
    }

    public List<ContaminationEffect> getContaminationEffectList() {
        return contamination;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private CompoundTag saveNBTData() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("rads", rads);
        tag.putDouble("neutrons", neutrons);
        tag.putDouble("envRads", radsEnv);
        tag.putDouble("radBuf", radBuf);
        tag.putDouble("digamma", digamma);
        tag.putInt("asbestos", asbestos);
        tag.putInt("blacklung", blacklung);
        tag.putInt("bombtimer", bombTimer);
        // Matches CE's HbmLivingCapability.EntityHbmProps#saveNBTData: contagion is only persisted
        // while the MKU system is enabled.
        if (ServerConfig.ENABLE_MKU.get()) tag.putInt("contagion", contagion);
        tag.putInt("oil", oil);
        tag.putInt("fire", fire);
        tag.putInt("phosphorus", phosphorus);
        tag.putInt("balefire", balefire);
        // grenadeDeployment is deliberately not persisted here, matching CE's EntityHbmProps,
        // which keeps it as pure runtime/session state (never written to NBT or network).
        tag.putInt("conteffectsize", contamination.size());
        for (int i = 0; i < contamination.size(); i++) {
            contamination.get(i).save(tag, i);
        }
        return tag;
    }

    private void loadNBTData(CompoundTag tag) {
        if (tag == null) return;
        rads = tag.getDouble("rads");
        neutrons = tag.getDouble("neutrons");
        radsEnv = tag.getDouble("envRads");
        radBuf = tag.getDouble("radBuf");
        digamma = tag.getDouble("digamma");
        asbestos = tag.getInt("asbestos");
        blacklung = tag.getInt("blacklung");
        bombTimer = tag.getInt("bombtimer");
        if (ServerConfig.ENABLE_MKU.get()) contagion = tag.getInt("contagion");
        oil = tag.getInt("oil");
        fire = tag.getInt("fire");
        phosphorus = tag.getInt("phosphorus");
        balefire = tag.getInt("balefire");

        contamination.clear();
        int contCount = tag.getInt("conteffectsize");
        for (int i = 0; i < contCount; i++) {
            contamination.add(ContaminationEffect.load(tag, i));
        }
    }
}
