package com.hbm.entity.mob;

import com.hbm.config.MobConfig;
import com.hbm.items.gear.SpecialArmorItems;
import com.hbm.items.weapon.sedna.content.GunPistolItems;
import com.hbm.items.weapon.sedna.content.GunShotgunItems;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * CE: {@code com.hbm.entity.mob.EntityFBI} (196 lines). Door/machine breaker; ranged stub
 * (CE {@code attackEntityWithRangedAttack} is empty). EntityAIBreaking / EntityAI_MLPF not
 * ported — vanilla melee + ranged goals + CE's own ray-break tick.
 */
public class EntityFBI extends Monster implements RangedAttackMob {

    private static final Set<Block> CAN_DESTROY = new HashSet<>();

    static {
        CAN_DESTROY.add(Blocks.ACACIA_DOOR);
        CAN_DESTROY.add(Blocks.BIRCH_DOOR);
        CAN_DESTROY.add(Blocks.DARK_OAK_DOOR);
        CAN_DESTROY.add(Blocks.JUNGLE_DOOR);
        CAN_DESTROY.add(Blocks.OAK_DOOR);
        CAN_DESTROY.add(Blocks.SPRUCE_DOOR);
        CAN_DESTROY.add(Blocks.IRON_DOOR);
        CAN_DESTROY.add(Blocks.OAK_TRAPDOOR);
        CAN_DESTROY.add(Blocks.IRON_TRAPDOOR);
        CAN_DESTROY.add(Blocks.CHEST);
        CAN_DESTROY.add(Blocks.TRAPPED_CHEST);
    }

    public EntityFBI(EntityType<? extends EntityFBI> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 20.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0D, 20, 15.0F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public boolean isNoAi() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof EntityFBI && source.is(DamageTypeTags.IS_PROJECTILE)) {
            return false;
        }
        ItemStack helm = this.getItemBySlot(EquipmentSlot.HEAD);
        if (!helm.isEmpty() && helm.is(Items.GLASS)) {
            if (source.is(DamageTypeTags.IS_FIRE)) return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        if (this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(SpecialArmorItems.GAS_MASK_M65.get()));
        }
        return false;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        if (random.nextInt(2) == 0) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(GunPistolItems.GUN_HEAVY_REVOLVER.get()));
        } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(GunShotgunItems.GUN_SPAS12.get()));
        }
        if (this.level() != null && this.level().dimension() != Level.OVERWORLD) {
            this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.GLASS));
            this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(SpecialArmorItems.HAZMAT_PAA_PLATE.get()));
            this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(SpecialArmorItems.HAZMAT_PAA_LEGS.get()));
            this.setItemSlot(EquipmentSlot.FEET, new ItemStack(SpecialArmorItems.HAZMAT_PAA_BOOTS.get()));
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        // CE body is empty — guns are visual until a fire-gun AI lands.
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || this.getHealth() <= 0) return;

        int delay = MobConfig.RAID_ATTACK_DELAY.get();
        if (delay > 0 && this.tickCount % delay == 0) {
            double reach = MobConfig.RAID_ATTACK_REACH.get();
            float yaw = this.random.nextFloat() * (float) (Math.PI * 2);
            Vec3 dir = new Vec3(Math.cos(yaw) * reach, 0, Math.sin(yaw) * reach);
            Vec3 from = new Vec3(this.getX(), this.getY() + 0.5 + this.random.nextFloat(), this.getZ());
            Vec3 to = from.add(dir);
            HitResult hit = this.level().clip(new net.minecraft.world.level.ClipContext(
                    from, to, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, this));
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockPos pos = ((BlockHitResult) hit).getBlockPos();
                if (CAN_DESTROY.contains(this.level().getBlockState(pos).getBlock())) {
                    this.level().destroyBlock(pos, false);
                }
            }
            AABB box = this.getBoundingBox().inflate(1.5D);
            for (ItemEntity item : this.level().getEntitiesOfClass(ItemEntity.class, box)) {
                item.setRemainingFireTicks(10 * 20);
            }
        }
    }
}
