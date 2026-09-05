package com.hbm.items.tool;

import com.hbm.api.block.IToolable.ToolType;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * Exact CE {@code ItemDefuser.itemInteractionForEntity} {@code :30-32} →
 * {@code ItemModDefuser.defuse} {@code :53-78}. Armor-mod {@code ItemModDefuser} is not
 * registered (IEquipReceiver stay skipped). Glyphid/Confetti branch stays skipped.
 */
public class ItemDefuser extends ItemTooling {

    @SuppressWarnings("unchecked")
    private static final EntityDataAccessor<Boolean> DATA_IS_IGNITED = resolveIgnitedAccessor();

    public ItemDefuser(ToolType type, Properties properties) {
        super(type, properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        // CE ItemDefuser.java:30-32
        if (entity instanceof Creeper creeper) {
            return defuse(creeper, player, true) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return InteractionResult.PASS;
    }

    /**
     * Exact CE {@code ItemModDefuser.defuse} {@code :53-78}. Does not register the armor-mod item.
     */
    public static boolean defuse(Creeper creeper, @Nullable LivingEntity entity, boolean dropItem) {
        creeper.setSwellDir(-1);
        if (DATA_IS_IGNITED != null) {
            creeper.getEntityData().set(DATA_IS_IGNITED, false);
        }

        SwellGoal toRem = null;
        for (WrappedGoal entry : creeper.goalSelector.getAvailableGoals()) {
            if (entry.getGoal() instanceof SwellGoal swell) {
                toRem = swell;
                break;
            }
        }

        if (toRem != null) {
            creeper.goalSelector.removeGoal(toRem);
            if (!creeper.level().isClientSide && dropItem) {
                creeper.level().playSound(null, creeper.getX(), creeper.getY(), creeper.getZ(),
                        HBMSoundHandler.pinBreak.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                Item fuse = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "safety_fuse"));
                creeper.spawnAtLocation(fuse);
                if (entity != null) {
                    creeper.hurt(creeper.damageSources().mobAttack(entity), 1.0F);
                }
                creeper.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
            }
            creeper.getPersistentData().putBoolean("hfr_defused", true);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Boolean> resolveIgnitedAccessor() {
        try {
            Field field = Creeper.class.getDeclaredField("DATA_IS_IGNITED");
            field.setAccessible(true);
            return (EntityDataAccessor<Boolean>) field.get(null);
        } catch (ReflectiveOperationException | SecurityException e) {
            MainRegistry.logger.warn("ItemDefuser: could not resolve Creeper#DATA_IS_IGNITED", e);
            return null;
        }
    }
}
