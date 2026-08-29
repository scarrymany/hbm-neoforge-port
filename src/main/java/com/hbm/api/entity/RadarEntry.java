package com.hbm.api.entity;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Plain data holder for a single radar blip. The CE original carried a manual ByteBuf codec
 * (fromBytes/toBytes via Forge's ByteBufUtils); that wire format is a packet concern and is
 * intentionally left out here, to be defined as a StreamCodec&lt;RegistryFriendlyByteBuf, RadarEntry&gt;
 * alongside whichever packet the networking area builds around this DTO.
 */
public class RadarEntry {

    /** Name use for radar display, uses I18n for lookup */
    public String unlocalizedName;
    /** The type of dot to show on the radar as well as the redstone level in tier mode */
    public int blipLevel;
    public int posX;
    public int posY;
    public int posZ;
    public ResourceKey<Level> dim;
    public int entityID;
    /** Whether this radar entry should be counted for the redstone output */
    public boolean redstone;

    public RadarEntry() { } //blank ctor for packets

    public RadarEntry(String name, int level, int x, int y, int z, ResourceKey<Level> dim, int entityID, boolean redstone) {
        this.unlocalizedName = name;
        this.blipLevel = level;
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.dim = dim;
        this.entityID = entityID;
        this.redstone = redstone;
    }

    public RadarEntry(IRadarDetectableNT detectable, Entity entity, boolean redstone) {
        this(detectable.getTranslationKey(), detectable.getBlipLevel(), (int) Math.floor(entity.getX()), (int) Math.floor(entity.getY()), (int) Math.floor(entity.getZ()), entity.level().dimension(), entity.getId(), redstone);
    }

    public RadarEntry(IRadarDetectable detectable, Entity entity) {
        this(detectable.getTargetType().name, detectable.getTargetType().ordinal(), (int) Math.floor(entity.getX()), (int) Math.floor(entity.getY()), (int) Math.floor(entity.getZ()), entity.level().dimension(), entity.getId(), entity.getDeltaMovement().y < 0);
    }

    public RadarEntry(Player player) {
        this(player.getDisplayName().getString(), IRadarDetectableNT.PLAYER, (int) Math.floor(player.getX()), (int) Math.floor(player.getY()), (int) Math.floor(player.getZ()), player.level().dimension(), player.getId(), true);
    }
}
