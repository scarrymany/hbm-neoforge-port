package com.hbm.entity.mob;

import com.hbm.items.armor.PoweredArmorItems;
import com.hbm.items.weapon.sedna.content.GunPistolItems;
import com.hbm.items.weapon.sedna.content.GunRifleItems;
import com.hbm.items.weapon.sedna.content.GunShotgunItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * CE: {@code com.hbm.entity.mob.EntityUndeadSoldier} (125 lines) — zombie/skeleton type byte,
 * Taurun armor + random CE gun, no loot.
 */
public class EntityUndeadSoldier extends Monster {

    public static final byte TYPE_ZOMBIE = 0;
    public static final byte TYPE_SKELETON = 1;

    public static final EntityDataAccessor<Byte> DW_TYPE =
            SynchedEntityData.defineId(EntityUndeadSoldier.class, EntityDataSerializers.BYTE);

    public EntityUndeadSoldier(EntityType<? extends EntityUndeadSoldier> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DW_TYPE, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        this.entityData.set(DW_TYPE, (byte) (this.random.nextBoolean() ? TYPE_ZOMBIE : TYPE_SKELETON));
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(PoweredArmorItems.TAURUN_HELMET.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(PoweredArmorItems.TAURUN_PLATE.get()));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(PoweredArmorItems.TAURUN_LEGS.get()));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(PoweredArmorItems.TAURUN_BOOTS.get()));
        int gun = random.nextInt(5);
        if (gun == 0) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(GunPistolItems.GUN_HEAVY_REVOLVER.get()));
        if (gun == 1) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(GunPistolItems.GUN_LIGHT_REVOLVER.get()));
        if (gun == 2) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(GunRifleItems.GUN_CARBINE.get()));
        if (gun == 3) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(GunShotgunItems.GUN_MARESLEG.get()));
        if (gun == 4) this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(GunPistolItems.GUN_GREASEGUN.get()));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        byte type = this.entityData.get(DW_TYPE);
        if (type == TYPE_ZOMBIE) return SoundEvents.ZOMBIE_AMBIENT;
        if (type == TYPE_SKELETON) return SoundEvents.SKELETON_AMBIENT;
        return super.getAmbientSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        byte type = this.entityData.get(DW_TYPE);
        if (type == TYPE_ZOMBIE) return SoundEvents.ZOMBIE_HURT;
        if (type == TYPE_SKELETON) return SoundEvents.SKELETON_HURT;
        return super.getHurtSound(source);
    }

    @Override
    protected SoundEvent getDeathSound() {
        byte type = this.entityData.get(DW_TYPE);
        if (type == TYPE_ZOMBIE) return SoundEvents.ZOMBIE_DEATH;
        if (type == TYPE_SKELETON) return SoundEvents.SKELETON_DEATH;
        return super.getDeathSound();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        byte type = this.entityData.get(DW_TYPE);
        if (type == TYPE_ZOMBIE) this.playSound(SoundEvents.ZOMBIE_STEP, 0.15F, 1.0F);
        if (type == TYPE_SKELETON) this.playSound(SoundEvents.SKELETON_STEP, 0.15F, 1.0F);
    }

    public static boolean checkUndeadSpawn(EntityType<EntityUndeadSoldier> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL
                && Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    protected void dropFromLootTable(DamageSource damageSource, boolean attackedRecently) {
    }

    @Override
    protected void dropEquipment() {
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("dwType", this.entityData.get(DW_TYPE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DW_TYPE, tag.getByte("dwType"));
    }
}
