package com.hbm.entity.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.EntityTeslaCrab} (extends {@link EntityCyberCrab}, 44
 * lines, read in full) - see {@code docs/phase4/entities_vehicles_aircraft.md}'s crab-family row.
 * Taller 0.75x1.25 hitbox, 10 HP, otherwise identical to the base crab (same ranged-attack cadence,
 * same tau bullet, same 0.1F death explosion - none of those are overridden here, matching CE).
 * <p>
 * <b>Tesla-arc zap not reproduced</b> - CE's {@code onLivingUpdate} calls
 * {@code TileEntityTesla.zap(world, posX, posY+1, posZ, 3, this)}; {@code TileEntityTesla} does not
 * exist in this port yet (same documented gap as {@link EntityTaintCrab} - see that class's javadoc).
 * Unlike {@link EntityTaintCrab}, this class's {@code onLivingUpdate} in CE has <b>no other</b>
 * mechanic alongside the zap call, so nothing else is lost by skipping it.
 * <p>
 * <b>{@code coil_copper} drop (1-in-200) not reproduced</b> - not registered in this port yet, same
 * documented items-scope gap as {@link EntityTaintCrab}.
 */
public class EntityTeslaCrab extends EntityCyberCrab {

    public EntityTeslaCrab(EntityType<? extends EntityCyberCrab> type, Level level) {
        super(type, level);
    }

    /** CE: {@code applyEntityAttributes} - {@code MAX_HEALTH = 10}, {@code MOVEMENT_SPEED = 0.5}. */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        // coil_copper (1-in-200) not registered in this port yet - see class javadoc.
    }
}
