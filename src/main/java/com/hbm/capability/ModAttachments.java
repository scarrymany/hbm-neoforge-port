package com.hbm.capability;

import com.hbm.main.MainRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Registers the NeoForge data attachments that replace CE's {@code HbmCapability} and
 * {@code HbmLivingCapability} Forge capabilities. Attachments are the direct successor to those
 * capabilities: {@link AttachmentType#builder} supplies the default instance itself, so unlike
 * the old Forge setup no {@code ICapabilitySerializable} provider or DUMMY default instance is
 * needed anymore.
 */
public final class ModAttachments {

    private ModAttachments() {}

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MainRegistry.MODID);

    /**
     * Per-player state (keybind toggles, shield, dash/stamina, reputation). Carried across death,
     * matching the confirmed-working precedent in the Neo Edition reference port.
     */
    public static final Supplier<AttachmentType<HbmPlayerAttachment>> PLAYER_ATTACHMENT = ATTACHMENTS.register(
            "player_data",
            () -> AttachmentType.builder(HbmPlayerAttachment::new)
                    .serialize(HbmPlayerAttachment.CODEC)
                    .sync(HbmPlayerAttachment.STREAM_CODEC)
                    .copyOnDeath()
                    .build()
    );

    /**
     * Per-living-entity NTM status data (radiation, digamma, hazard timers). Intentionally not
     * carried across death: a fresh entity/respawn starts clean, matching CE's original behavior
     * of the capability being reattached per-entity-instance.
     */
    public static final Supplier<AttachmentType<HbmLivingAttachment>> LIVING_ATTACHMENT = ATTACHMENTS.register(
            "living_props",
            () -> AttachmentType.builder(HbmLivingAttachment::new)
                    .serialize(HbmLivingAttachment.CODEC)
                    .sync(HbmLivingAttachment.STREAM_CODEC)
                    .build()
    );

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
