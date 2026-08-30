package com.hbm.items.gear;

import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.projectile.EntityRubble;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Random;

/**
 * Port of CE's {@code WeaponSpecial} - ~13 named special melee weapons sharing one class, dispatched
 * by registry name (post-flattening equivalent of CE's {@code this == ModItems.X} identity checks).
 * <p>
 * Deliberately not ported (per docs/phase1/items_food_gear.md): the CE {@code wrench} branches in
 * {@link #hurtEnemy}/attribute logic - the real {@code wrench} item is CE's {@code ItemWrench}, a
 * different class entirely (upstream hbm-ce {@code ModItems.java:748}), so that branch is dead code
 * in CE itself and is not reproduced here; only {@code wrench_flipped} (a genuine
 * {@code WeaponSpecial} instance) keeps its branch.
 * <p>
 * The {@code memespoon} big-fall-nuke branch ({@code EntityNukeExplosionMK5}/{@code EntityNukeTorex})
 * and {@code shimmer_sledge}'s rubble-entity spawn ({@code EntityRubble}) were completed in this
 * package's own pass once the Phase 3 foundation wave landed those three entity classes - see
 * {@link #hurtEnemy}/{@link #useOn} respectively. Still deferred, each flagged inline where it
 * would go: {@code onUpdate}'s advancement grants ({@code ArmorUtil.checkForFiend}/
 * {@code AdvancementManager}, neither ported - see {@code docs/phase3/melee_weapons.md}'s Deferred
 * scope) - CE's {@code onUpdate} hook itself has no other content, so no stub method is added here
 * purely to hold a TODO comment - and {@code lead_gavel}'s {@code HbmPotion.lead} effect (unported
 * potion registry).
 */
public class WeaponSpecial extends SwordItem {

    private static final Random RANDOM = new Random();

    public WeaponSpecial(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = target.level();
        String path = BuiltInRegistries.ITEM.getKey(this).getPath();

        switch (path) {
            case "schrabidium_hammer" -> {
                if (!level.isClientSide()) target.setHealth(0.0F);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), HBMSoundHandler.bonk.get(), SoundSource.PLAYERS, 3.0F, 0.1F);
            }
            case "bottle_opener" -> {
                if (!level.isClientSide()) {
                    int i = RANDOM.nextInt(7);
                    if (i == 0) target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5 * 60 * 20, 0));
                    if (i == 1) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5 * 60 * 20, 2));
                    if (i == 2) target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 5 * 60 * 20, 2));
                    if (i == 3) target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60 * 20, 0));
                }
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 3.0F, 1.0F);
            }
            case "shimmer_sledge" -> {
                Vec3 look = attacker.getLookAngle().scale(5);
                target.setDeltaMovement(target.getDeltaMovement().add(look));
                level.playSound(null, target.getX(), target.getY(), target.getZ(), HBMSoundHandler.bang.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
            }
            case "shimmer_axe" -> {
                target.setHealth(target.getHealth() / 2);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), HBMSoundHandler.slice.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
            }
            case "wrench_flipped" -> {
                Vec3 look = attacker.getLookAngle().scale(0.5);
                target.setDeltaMovement(target.getDeltaMovement().add(look));
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 3.0F, 0.75F);
            }
            case "ullapool_caber" -> {
                if (!level.isClientSide()) {
                    level.explode(attacker, target.getX(), target.getY(), target.getZ(), 7.5F, false, Level.ExplosionInteraction.TNT);
                }
                stack.hurtAndBreak(505, attacker, LivingEntity.getSlotForHand(InteractionHand.MAIN_HAND));
            }
            case "memespoon" -> {
                if (attacker.fallDistance >= 2) {
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), HBMSoundHandler.bang.get(), SoundSource.PLAYERS, 3.0F, 0.75F);
                    target.setHealth(0);
                }
                if (!(attacker instanceof Player player)) return false;
                if (attacker.fallDistance >= 20 && !player.isCreative() && !level.isClientSide()) {
                    // CE: EntityNukeExplosionMK5.statFac(world, 100, target.posX/Y/Z).setDetonator(attacker),
                    // plus a config-gated EntityNukeTorex (upstream hbm-ce WeaponSpecial.java:143-147).
                    // Both entities landed in the Phase 3 foundation wave - unblocked, ported here.
                    level.addFreshEntity(EntityNukeExplosionMK5.statFac(level, 100, target.getX(), target.getY(), target.getZ()).setDetonator(attacker));
                    if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
                        EntityNukeTorex.statFac(level, target.getX(), target.getY(), target.getZ(), 100);
                    }
                }
            }
            case "stopsign", "sopsign" ->
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), HBMSoundHandler.stop.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            case "wood_gavel" ->
                    level.playSound(null, target.getX(), target.getY(), target.getZ(), HBMSoundHandler.whack.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
            case "lead_gavel" -> {
                level.playSound(null, target.getX(), target.getY(), target.getZ(), HBMSoundHandler.whack.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
                // Deferred: CE also applies HbmPotion.lead for 15s at amplifier 4 (unported potion
                // registry, upstream hbm-ce WeaponSpecial.java:159).
            }
            case "diamond_gavel" -> {
                target.setHealth(target.getHealth() - target.getMaxHealth() / 3);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), HBMSoundHandler.whack.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
            }
            default -> {
            }
        }

        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        String path = BuiltInRegistries.ITEM.getKey(this).getPath();
        var pos = context.getClickedPos();

        if (path.equals("shimmer_sledge")) {
            // CE: spawns an EntityRubble carrying the destroyed block's identity, flung along the
            // player's look vector, instead of a bare destroyBlock (upstream hbm-ce
            // WeaponSpecial.java:176-201). EntityRubble landed in the Phase 3 foundation wave -
            // unblocked, ported here.
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() != Blocks.AIR && state.getBlock().getExplosionResistance() < 6000) {
                level.playSound(null, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, HBMSoundHandler.bang.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
                if (!level.isClientSide() && context.getPlayer() != null) {
                    EntityRubble rubble = new EntityRubble(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                    rubble.setBlockState(state);
                    rubble.setDeltaMovement(context.getPlayer().getLookAngle().scale(5));
                    level.addFreshEntity(rubble);
                    level.destroyBlock(pos, false);
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (path.equals("shimmer_axe")) {
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, HBMSoundHandler.kaping.get(), SoundSource.PLAYERS, 3.0F, 1.0F);
            if (!level.isClientSide()) {
                if (level.getBlockState(pos).getBlock() != Blocks.AIR && level.getBlockState(pos).getBlock().getExplosionResistance() < 6000) {
                    level.destroyBlock(pos, false);
                }
                if (level.getBlockState(pos.above()).getBlock() != Blocks.AIR && level.getBlockState(pos.above()).getBlock().getExplosionResistance() < 6000) {
                    level.destroyBlock(pos.above(), false);
                }
                if (level.getBlockState(pos.below()).getBlock() != Blocks.AIR && level.getBlockState(pos.below()).getBlock().getExplosionResistance() < 6000) {
                    level.destroyBlock(pos.below(), false);
                }
            }
            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String path = BuiltInRegistries.ITEM.getKey(this).getPath();
        switch (path) {
            case "schrabidium_hammer" -> {
                tooltip.add(Component.literal("Even though it says \"+1000000000"));
                tooltip.add(Component.literal("damage\", it's actually \"onehit anything\""));
            }
            case "bottle_opener" -> {
                tooltip.add(Component.literal("My very own bottle opener."));
                tooltip.add(Component.literal("Use with caution!"));
            }
            case "ullapool_caber" -> {
                tooltip.add(Component.literal("High-yield Scottish face removal."));
                tooltip.add(Component.literal("A sober person would throw it..."));
            }
            case "wrench_flipped" -> tooltip.add(Component.literal("Wrench 2: The Wrenchening"));
            case "memespoon" -> {
                tooltip.add(Component.literal("Level 10 Shovel").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.literal("Deals crits while the wielder is rocket jumping").withStyle(ChatFormatting.AQUA));
                tooltip.add(Component.literal("20% slower firing speed").withStyle(ChatFormatting.RED));
                tooltip.add(Component.literal("No random critical hits").withStyle(ChatFormatting.RED));
            }
            case "shimmer_sledge" -> tooltip.add(Component.literal("Breaks everything, even portals."));
            case "shimmer_axe" -> tooltip.add(Component.literal("Timber!"));
            case "wood_gavel" -> tooltip.add(Component.literal("Thunk!"));
            case "lead_gavel" -> tooltip.add(Component.literal("You are hereby sentenced to lead poisoning."));
            case "diamond_gavel" -> {
                tooltip.add(Component.literal("The joke! It makes sense now!!"));
                tooltip.add(Component.literal(""));
                tooltip.add(Component.literal("Deals as much damage as it needs to.").withStyle(ChatFormatting.BLUE));
            }
            default -> {
            }
        }
    }
}
