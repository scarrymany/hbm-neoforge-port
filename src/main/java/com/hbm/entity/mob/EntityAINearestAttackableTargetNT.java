package com.hbm.entity.mob;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.ai.EntityAINearestAttackableTargetNT} (58 lines, read
 * in full) - see {@code docs/phase4/entities_bosses.md}'s worm-boss table. A thin variant of vanilla's
 * nearest-attackable-target goal: adds a configurable search {@code range} parameter (vanilla derives
 * it from an attribute) and a custom {@link Predicate}{@code <Entity>} selector. Used by both
 * {@link EntityBOTPrimeHead} (targets players, range 128) and {@link EntityBOTPrimeBody} (targets
 * players via a selector excluding same-worm segments, range 128).
 * <p>
 * <b>Real CE bug fixed here, not reproduced</b>: CE's own version sets {@code taskOwner.setAttackTarget
 * (targetEntity)} inside {@code resetTask()} - the lifecycle method vanilla's AI-task scheduler calls
 * when a goal is <em>stopping</em>, not starting (vanilla's own {@code EntityAINearestAttackableTarget}
 * sets it in {@code startExecuting()} instead). Tracing CE's own AI-task scheduler
 * ({@code EntityAITasks.onUpdateTasks}) shows this means CE's version only ever assigns
 * {@code attackTarget} at the exact moment a target search is about to fail (using the stale
 * previous-tick target), leaving the entity's real attack target perpetually null/stale while a
 * target genuinely is being tracked - which would break the worm's entire attack-trigger chain
 * ({@link WormMovementHeadNT}/{@link EntityBOTPrimeBody#customServerAiStep()} both gate firing on
 * {@code getTarget() != null}). This port sets the target in {@link #start()} (matching vanilla's own
 * equivalent goal and CE's evident intent) and clears it in {@link #stop()}.
 */
public class EntityAINearestAttackableTargetNT extends TargetGoal {

    private final Class<? extends LivingEntity> targetClass;
    private final int targetChance;
    @Nullable
    private final Predicate<Entity> selector;
    private final double range;
    @Nullable
    private LivingEntity foundTarget;

    public EntityAINearestAttackableTargetNT(Mob mob, Class<? extends LivingEntity> targetClass, int targetChance,
            boolean mustSee, boolean mustReach, @Nullable Predicate<Entity> selector, double range) {
        super(mob, mustSee, mustReach);
        this.targetClass = targetClass;
        this.targetChance = targetChance;
        this.selector = selector;
        this.range = range;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (this.targetChance > 0 && this.mob.getRandom().nextInt(this.targetChance) != 0) {
            return false;
        }

        this.foundTarget = findNearest(this.targetClass);
        return this.foundTarget != null;
    }

    // Generic helper (rather than inlining Class<? extends LivingEntity> directly into
    // getEntitiesOfClass at the call site) so the wildcard is captured once, cleanly, as a real type
    // parameter - List<T> internally, no List<? extends LivingEntity> variance headaches.
    @Nullable
    private <T extends LivingEntity> T findNearest(Class<T> clazz) {
        List<T> candidates = this.mob.level().getEntitiesOfClass(clazz,
                this.mob.getBoundingBox().inflate(this.range),
                e -> e != this.mob && e.isAlive() && !e.isSpectator() && (this.selector == null || this.selector.test(e)));

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort(Comparator.comparingDouble(this.mob::distanceToSqr));
        return candidates.get(0);
    }

    @Override
    public void start() {
        this.mob.setTarget(this.foundTarget);
        super.start();
    }

    @Override
    public void stop() {
        this.mob.setTarget(null);
        super.stop();
    }
}
