package com.hbm.entity.mob;

import com.hbm.lib.HBMSoundHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.EntityDuck} (42 lines, read in full, {@code extends
 * EntityChicken}) - a trivial reskinned-sound vanilla chicken. Needed as {@link EntityQuackos}'s own
 * superclass (see that class and {@code docs/phase4/entities_bosses.md}'s Quackos row) and as
 * {@code ItemChopper}'s {@code spawn_duck} placement target (that item's own doc comment names this
 * exact class as the blocking forward reference - see this task's wiring of {@code ItemChopper}).
 * <p>
 * {@code Chicken.createAttributes()} as the base builder (rather than reimplementing
 * {@code Animal.createAnimalAttributes()} from scratch) is confirmed real 1.21.1 API via Neo Edition's
 * own compiling {@code com.hbm.entity.mob.Duck} (cross-referenced for API shape only, per this task's
 * ground rules - CE is this port's sole behavior/numbers source, and both agree: 4 HP, 0.25 speed).
 * <p>
 * CE's {@code canDespawn() -> false} override (this port: {@link #removeWhenFarAway}) is preserved -
 * Neo Edition's own {@code Duck} omits it, but CE's real 42-line file has it and this report treats
 * CE as the sole behavior source.
 */
public class EntityDuck extends Chicken {

    public EntityDuck(EntityType<? extends Chicken> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Chicken.createAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return HBMSoundHandler.ducc.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return HBMSoundHandler.ducc.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HBMSoundHandler.ducc.get();
    }

    /** CE: {@code canDespawn() { return false; }} */
    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Nullable
    @Override
    public EntityDuck getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        // this.getType() returns the wildcard-erased EntityType<?> - not directly assignable to the
        // EntityType<? extends Chicken>-bound constructor below without an unchecked cast, so this uses
        // the concrete registered holder directly instead (same fix Neo Edition's own confirmed-real
        // Duck.getBreedOffspring applies: `new Duck(NtmEntityTypes.DUCK.get(), level)`).
        return new EntityDuck(Phase4BossEntityTypes2.DUCK.get(), level);
    }
}
