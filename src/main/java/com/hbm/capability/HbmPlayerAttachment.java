package com.hbm.capability;

import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.armor.ItemModShield;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Per-player NTM state, ported from CE's {@code HbmCapability} ({@code IHBMData} / {@code HBMData}).
 *
 * <p>Plain data holder plus the handful of pure derived-data helpers CE kept directly on the data
 * type ({@link #getEffectiveMaxShield}, {@link #isJetpackActive}, {@link #isMagnetActive}) - these
 * have no side effects (no packets, no HUD updates), so they stayed put rather than moving to a
 * separate facade. {@link #setKeyPressed} likewise keeps CE's original key-down-edge toggle
 * behavior for the backpack/HUD/magnet flags, since that is a pure state transition and not a
 * game-logic concern.
 *
 * <p>CE's HUD/packet-driven side effects on these same toggles (seen in the Neo Edition reference
 * port's equivalent class) belong to the input/packet system, not this capability-framework area.
 *
 * <p>Only the fields CE itself persisted to NBT are written by {@link #saveNBTData()}; dash
 * cooldown, stamina, dash count, plink cooldown and last-damage are runtime-only in CE (reset each
 * session) and are likewise excluded here, matching {@code HbmCapability.HBMDataStorage} exactly.
 */
public final class HbmPlayerAttachment {

    public static final float SHIELD_CAP = 100F;
    private static final int PLINK_COOLDOWN_LENGTH = 10;

    /** Dash cooldown length in ticks, ported from CE's {@code HbmCapability.dashCooldownLength}. */
    public static final int DASH_COOLDOWN_LENGTH = 5;

    public static final Codec<HbmPlayerAttachment> CODEC = CompoundTag.CODEC.xmap(
            tag -> {
                HbmPlayerAttachment data = new HbmPlayerAttachment();
                data.loadNBTData(tag);
                return data;
            },
            HbmPlayerAttachment::saveNBTData
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, HbmPlayerAttachment> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> buf.writeNbt(data.saveNBTData()),
            buf -> {
                HbmPlayerAttachment data = new HbmPlayerAttachment();
                data.loadNBTData(buf.readNbt());
                return data;
            }
    );

    private final boolean[] keysPressed = new boolean[EnumKeybind.values().length];

    private boolean enableBackpack = true;
    private boolean enableHUD = true;
    private boolean enableMagnet = true;
    private boolean hasReceivedBook = false;

    private int dashCooldown = 0;
    private int totalDashCount = 0;
    private int stamina = 0;
    private int plinkCooldown = 0;

    private float shield = 0F;
    private float maxShield = 0F;
    private int lastDamage = 0;
    private int reputation = 0;

    public static HbmPlayerAttachment getData(Player player) {
        return player.getData(ModAttachments.PLAYER_ATTACHMENT);
    }

    /**
     * Plays a sound to everyone but {@code player} at most once every {@value #PLINK_COOLDOWN_LENGTH}
     * ticks, gated by this player's stored plink cooldown. Ported from
     * {@code HbmCapability.plink(EntityPlayer, SoundEvent, float, float)}.
     */
    public static void plink(Player player, SoundEvent sound, float volume, float pitch) {
        HbmPlayerAttachment data = getData(player);
        if (data.getPlinkCooldown() <= 0) {
            player.level().playSound(player, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
            data.setPlinkCooldown(PLINK_COOLDOWN_LENGTH);
        }
    }

    public boolean getKeyPressed(EnumKeybind key) {
        return keysPressed[key.ordinal()];
    }

    public void setKeyPressed(EnumKeybind key, boolean pressed) {
        if (!getKeyPressed(key) && pressed) {
            if (key == EnumKeybind.TOGGLE_JETPACK) {
                enableBackpack = !enableBackpack;
            }
            if (key == EnumKeybind.TOGGLE_HEAD) {
                enableHUD = !enableHUD;
            }
            if (key == EnumKeybind.TOGGLE_MAGNET) {
                enableMagnet = !enableMagnet;
            }
        }
        keysPressed[key.ordinal()] = pressed;
    }

    public boolean getEnableBackpack() {
        return enableBackpack;
    }

    public void setEnableBackpack(boolean enableBackpack) {
        this.enableBackpack = enableBackpack;
    }

    public boolean getEnableHUD() {
        return enableHUD;
    }

    public void setEnableHUD(boolean enableHUD) {
        this.enableHUD = enableHUD;
    }

    public boolean getEnableMagnet() {
        return enableMagnet;
    }

    public void setEnableMagnet(boolean enableMagnet) {
        this.enableMagnet = enableMagnet;
    }

    public boolean hasReceivedBook() {
        return hasReceivedBook;
    }

    public void setReceivedBook(boolean hasReceivedBook) {
        this.hasReceivedBook = hasReceivedBook;
    }

    public float getShield() {
        return shield;
    }

    public void setShield(float shield) {
        this.shield = shield;
    }

    public float getMaxShield() {
        return maxShield;
    }

    public void setMaxShield(float maxShield) {
        this.maxShield = maxShield;
    }

    public int getLastDamage() {
        return lastDamage;
    }

    public void setLastDamage(int lastDamage) {
        this.lastDamage = lastDamage;
    }

    public int getDashCooldown() {
        return dashCooldown;
    }

    public void setDashCooldown(int dashCooldown) {
        this.dashCooldown = dashCooldown;
    }

    public int getStamina() {
        return stamina;
    }

    public void setStamina(int stamina) {
        this.stamina = stamina;
    }

    public int getDashCount() {
        return totalDashCount;
    }

    public void setDashCount(int totalDashCount) {
        this.totalDashCount = totalDashCount;
    }

    public int getPlinkCooldown() {
        return plinkCooldown;
    }

    public void setPlinkCooldown(int plinkCooldown) {
        this.plinkCooldown = plinkCooldown;
    }

    public int getReputation() {
        return reputation;
    }

    public void setReputation(int reputation) {
        this.reputation = reputation;
    }

    public float getEffectiveMaxShield(Player player) {
        float max = maxShield;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty()) {
            ItemStack[] mods = ArmorModHandler.pryMods(chest);
            if (mods[ArmorModHandler.kevlar] != null && mods[ArmorModHandler.kevlar].getItem() instanceof ItemModShield mod) {
                max += mod.shield;
            }
        }
        return max;
    }

    public boolean isJetpackActive() {
        return enableBackpack && getKeyPressed(EnumKeybind.JETPACK);
    }

    public boolean isMagnetActive() {
        return enableMagnet;
    }

    private CompoundTag saveNBTData() {
        CompoundTag tag = new CompoundTag();
        for (EnumKeybind key : EnumKeybind.values()) {
            tag.putBoolean(key.name(), getKeyPressed(key));
        }
        tag.putBoolean("hasReceivedBook", hasReceivedBook);
        tag.putFloat("shield", shield);
        tag.putFloat("maxShield", maxShield);
        tag.putBoolean("enableBackpack", enableBackpack);
        tag.putBoolean("enableHUD", enableHUD);
        tag.putBoolean("enableMagnet", enableMagnet);
        tag.putInt("reputation", reputation);
        return tag;
    }

    private void loadNBTData(CompoundTag tag) {
        if (tag == null) return;
        for (EnumKeybind key : EnumKeybind.values()) {
            keysPressed[key.ordinal()] = tag.getBoolean(key.name());
        }
        hasReceivedBook = tag.getBoolean("hasReceivedBook");
        shield = tag.getFloat("shield");
        maxShield = tag.getFloat("maxShield");
        enableBackpack = tag.getBoolean("enableBackpack");
        enableHUD = tag.getBoolean("enableHUD");
        // Pre-magnet-toggle saves have no "enableMagnet" key; default such saves to enabled,
        // matching CE's HBMDataStorage#readNBT exactly.
        enableMagnet = !tag.contains("enableMagnet") || tag.getBoolean("enableMagnet");
        reputation = tag.getInt("reputation");
    }
}
