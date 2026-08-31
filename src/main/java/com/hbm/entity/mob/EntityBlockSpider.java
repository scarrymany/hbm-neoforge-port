package com.hbm.entity.mob;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE: {@code com.hbm.entity.mob.EntityBlockSpider} (58 lines, {@code entity_taintcrawler}).
 * 1.12 id+meta flattened to a synced block registry id (no invented art).
 */
public class EntityBlockSpider extends Monster {

    private static final EntityDataAccessor<String> BLOCK_ID =
            SynchedEntityData.defineId(EntityBlockSpider.class, EntityDataSerializers.STRING);

    public EntityBlockSpider(EntityType<? extends EntityBlockSpider> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BLOCK_ID, BuiltInRegistries.BLOCK.getKey(Blocks.STONE).toString());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.5D));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    public void makeBlock(Block block) {
        this.entityData.set(BLOCK_ID, BuiltInRegistries.BLOCK.getKey(block).toString());
        double health = Math.max(1.0D, block.getExplosionResistance());
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        this.setHealth(this.getMaxHealth());
    }

    public BlockState getRenderState() {
        ResourceLocation id = ResourceLocation.tryParse(this.entityData.get(BLOCK_ID));
        Block block = id == null ? Blocks.STONE : BuiltInRegistries.BLOCK.get(id);
        return block.defaultBlockState();
    }
}
