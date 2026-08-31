package com.hbm.entity.mob.glyphid;

import com.hbm.api.entity.IResistanceProvider;
import com.hbm.config.MobConfig;
import com.hbm.damage.ModDamageTypes;
import com.hbm.entity.mob.EntityAINearestAttackableTargetNT;
import com.hbm.entity.mob.EntityParasiteMaggot;
import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.entity.mob.Phase9MobEntityTypes;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * CE: {@code com.hbm.entity.mob.glyphid.EntityGlyphid} (674 lines). Combat / armor / subtype /
 * maggot-on-death / IResistanceProvider ported. Hive waypoint + Rampant dig need
 * {@code EntityWaypoint} (not in this port) — TASK_* constants + NBT kept for later.
 */
public class EntityGlyphid extends Monster implements IResistanceProvider {

    public boolean hasHome;
    public int homeX;
    public int homeY;
    public int homeZ;
    protected byte currentTask = 0;

    public static final byte TASK_IDLE = 0;
    public static final byte TASK_RETREAT_FOR_REINFORCEMENTS = 1;
    public static final byte TASK_BUILD_HIVE = 2;
    public static final byte TASK_INITIATE_RETREAT = 3;
    public static final byte TASK_FOLLOW = 4;
    public static final byte TASK_TERRAFORM = 5;
    public static final byte TASK_DIG = 6;

    public static final byte TYPE_NORMAL = 0;
    public static final byte TYPE_INFECTED = 1;
    public static final byte TYPE_RADIOACTIVE = 2;

    public static final EntityDataAccessor<Byte> WALL_CLIMBING =
            SynchedEntityData.defineId(EntityGlyphid.class, EntityDataSerializers.BYTE);
    public static final EntityDataAccessor<Byte> ARMOR =
            SynchedEntityData.defineId(EntityGlyphid.class, EntityDataSerializers.BYTE);
    public static final EntityDataAccessor<Byte> SUBTYPE =
            SynchedEntityData.defineId(EntityGlyphid.class, EntityDataSerializers.BYTE);

    public EntityGlyphid(EntityType<? extends EntityGlyphid> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        StatBundle stats = GlyphidStats.getStats().getGrunt();
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, stats.health())
                .add(Attributes.MOVEMENT_SPEED, stats.speed())
                .add(Attributes.ATTACK_DAMAGE, stats.damage())
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }

    public ResourceLocation getSkin() {
        return ResourceLocation.fromNamespaceAndPath("hbm", "textures/entity/glyphid.png");
    }

    public double getGlyphidScale() {
        return 1.0D;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WALL_CLIMBING, (byte) 0);
        builder.define(ARMOR, (byte) 0b11111);
        builder.define(SUBTYPE, (byte) 0);
    }

    public StatBundle getStats() {
        return GlyphidStats.getStats().statsGrunt;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8D) {
            @Override
            public boolean canUse() {
                return getCurrentTask() == TASK_IDLE && super.canUse();
            }
        });
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        double range = useExtendedTargeting() ? 128.0 : 16.0;
        this.targetSelector.addGoal(1, new EntityAINearestAttackableTargetNT(this, Player.class, 10, true, false,
                player -> !isBlind() && player != null && distanceTo(player) <= range, range));
    }

    @Override
    public float[] getCurrentDTDR(DamageSource damage, float amount, float pierceDT, float pierce) {
        if (damage.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || damage.is(DamageTypeTags.BYPASSES_ARMOR)) {
            return new float[]{0F, 0F};
        }
        StatBundle stats = this.getStats();
        float threshold = stats.thresholdMultForArmor() * getGlyphidArmor() / 5F;
        if (damage.is(ModDamageTypes.NUCLEAR_BLAST)) return new float[]{threshold * 0.25F, 0F};
        String key = damage.type().msgId();
        if (DamageClass.LASER.name().toLowerCase(Locale.US).equals(key) || damage.is(ModDamageTypes.LASER)) {
            return new float[]{threshold * 0.5F, stats.resistanceMult() * 0.5F};
        }
        if (damage.is(DamageTypeTags.IS_FIRE)) return new float[]{0F, stats.resistanceMult() * 0.2F};
        if (damage.is(DamageTypeTags.IS_EXPLOSION)) return new float[]{threshold * 0.5F, stats.resistanceMult() * 0.35F};
        return new float[]{threshold, stats.resistanceMult()};
    }

    @Override
    public void onDamageDealt(DamageSource damage, float amount) {
        if (this.isArmorBroken(amount)) this.breakOffArmor();
    }

    public boolean isBlind() {
        return this.hasEffect(MobEffects.BLINDNESS);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (!hasHome) {
                homeX = (int) this.getX();
                homeY = (int) this.getY();
                homeZ = (int) this.getZ();
                hasHome = true;
            }
            if (isBlind()) {
                this.setTarget(null);
                this.getNavigation().stop();
            }
            this.setBesideClimbableBlock(this.horizontalCollision);
        }
    }

    @Override
    protected void dropFromLootTable(DamageSource damageSource, boolean attackedRecently) {
        Item drop = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("hbm",
                this.isOnFire() ? "glyphid_meat_grilled" : "glyphid_meat"));
        if (this.random.nextInt(2) == 0) {
            this.spawnAtLocation(new ItemStack(drop, ((int) getGlyphidScale() * 2) + (attackedRecently ? 1 : 0)));
        }
    }

    @Nullable
    @Override
    public LivingEntity getTarget() {
        return isBlind() ? null : super.getTarget();
    }

    public boolean useExtendedTargeting() {
        try {
            return MobConfig.RAMPANT_EXTENDED_TARGETING.get()
                    || PollutionHandler.getPollution(this.level(), this.blockPosition(),
                    PollutionHandler.PollutionType.SOOT) >= MobConfig.TARGETING_THRESHOLD.get();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return getTarget() == null && getCurrentTask() == TASK_IDLE && this.tickCount > 100;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide && doesInfectedSpawnMaggots() && this.entityData.get(SUBTYPE) == TYPE_INFECTED) {
            int j = 2 + this.random.nextInt(3);
            for (int k = 0; k < j; ++k) {
                float f = ((float) (k % 2) - 0.5F) * 0.5F;
                float f1 = ((float) (k / 2) - 0.5F) * 0.5F;
                EntityParasiteMaggot maggot = new EntityParasiteMaggot(Phase9MobEntityTypes.PARASITE_MAGGOT.get(), this.level());
                maggot.moveTo(this.getX() + f, this.getY() + 0.5D, this.getZ() + f1, this.random.nextFloat() * 360.0F, 0.0F);
                maggot.setDeltaMovement(f, 0, f1);
                maggot.hasImpulse = true;
                this.level().addFreshEntity(maggot);
            }
            this.level().playSound(null, this.blockPosition(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                    SoundSource.HOSTILE, 2.0F, 0.95F + this.random.nextFloat() * 0.2F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof EntityGlyphid) return false;
        return GlyphidStats.getStats().handleAttack(this, source, amount);
    }

    public boolean attackSuperclass(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    public boolean doesInfectedSpawnMaggots() {
        return true;
    }

    public boolean isArmorBroken(float amount) {
        return this.random.nextInt(100) <= Math.min(Math.pow(amount * 0.6, 2), 100);
    }

    public void breakOffArmor() {
        byte armor = this.entityData.get(ARMOR);
        List<Integer> indices = Arrays.asList(0, 1, 2, 3, 4);
        Collections.shuffle(indices);
        for (Integer i : indices) {
            byte bit = (byte) (1 << i);
            if ((armor & bit) > 0) {
                armor &= (byte) ~bit;
                armor = (byte) (armor & 0b11111);
                this.entityData.set(ARMOR, armor);
                this.level().playSound(null, this.blockPosition(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                        SoundSource.HOSTILE, 1.0F, 1.25F);
                break;
            }
        }
    }

    public byte getArmorBits() {
        return this.entityData.get(ARMOR);
    }

    public int getGlyphidArmor() {
        int total = 0;
        byte armor = this.entityData.get(ARMOR);
        for (int i = 0; i < 5; i++) {
            if ((armor & (1 << i)) != 0) total++;
        }
        return total;
    }

    @Override
    public boolean isNoGravity() {
        return false;
    }

    @Override
    public boolean onClimbable() {
        return this.isBesideClimbableBlock();
    }

    public boolean isBesideClimbableBlock() {
        return (this.entityData.get(WALL_CLIMBING) & 1) != 0;
    }

    public void setBesideClimbableBlock(boolean climbable) {
        byte watchable = this.entityData.get(WALL_CLIMBING);
        if (climbable) {
            watchable = (byte) (watchable | 1);
        } else {
            watchable &= -2;
        }
        this.entityData.set(WALL_CLIMBING, watchable);
    }

    @Override
    public boolean doHurtTarget(Entity victim) {
        if (this.entityData.get(SUBTYPE) == TYPE_INFECTED && victim instanceof LivingEntity living) {
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.POISON, 100, 2));
            living.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.CONFUSION, 100, 0));
        }
        return super.doHurtTarget(victim);
    }

    public byte getCurrentTask() {
        return currentTask;
    }

    public void setCurrentTask(byte task) {
        this.currentTask = task;
    }

    public void communicate(byte task) {
        var box = this.getBoundingBox().inflate(4);
        for (Entity e : this.level().getEntities(this, box)) {
            if (e instanceof EntityGlyphid bug && !(e instanceof EntityGlyphidScout)
                    && bug.getCurrentTask() != task) {
                bug.setCurrentTask(task);
            }
        }
    }

    public boolean isAtDestination(int x, int y, int z, int radiusSq) {
        return this.distanceToSqr(x, y, z) <= radiusSq;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putByte("armor", this.entityData.get(ARMOR));
        nbt.putByte("subtype", this.entityData.get(SUBTYPE));
        nbt.putBoolean("hasHome", hasHome);
        nbt.putInt("homeX", homeX);
        nbt.putInt("homeY", homeY);
        nbt.putInt("homeZ", homeZ);
        nbt.putByte("task", currentTask);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.entityData.set(ARMOR, nbt.getByte("armor"));
        this.entityData.set(SUBTYPE, nbt.getByte("subtype"));
        this.hasHome = nbt.getBoolean("hasHome");
        this.homeX = nbt.getInt("homeX");
        this.homeY = nbt.getInt("homeY");
        this.homeZ = nbt.getInt("homeZ");
        this.currentTask = nbt.getByte("task");
    }

    public static boolean checkGlyphidSpawn(EntityType<? extends EntityGlyphid> type, ServerLevelAccessor level,
            net.minecraft.world.entity.MobSpawnType spawnType, net.minecraft.core.BlockPos pos,
            net.minecraft.util.RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
    }
}
