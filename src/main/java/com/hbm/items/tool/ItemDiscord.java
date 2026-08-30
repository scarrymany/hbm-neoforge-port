package com.hbm.items.tool;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Short-range "blink" teleport item - particle and sound only, no persisted state. Ported from
 * CE's {@code com.hbm.items.tool.ItemDiscord}, retargeted at the 1.21 raytrace API
 * ({@link Level#clip(ClipContext)} replaces CE's {@code Library.rayTrace}).
 */
public class ItemDiscord extends Item {

    private static final double RANGE = 100.0D;

    public ItemDiscord(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(RANGE));
        HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));

        if (hit.getType() == HitResult.Type.BLOCK) {
            if (!level.isClientSide()) {
                if (player.isPassenger()) {
                    player.stopRiding();
                }

                Direction dir = ((BlockHitResult) hit).getDirection();
                Vec3 pos = hit.getLocation();

                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.teleportTo(pos.x + dir.getStepX(), pos.y + dir.getStepY() - 1, pos.z + dir.getStepZ());
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                player.fallDistance = 0.0F;
            }

            for (int i = 0; i < 32; i++) {
                level.addParticle(ParticleTypes.PORTAL, player.getX(), player.getY() + player.getRandom().nextDouble() * 2.0D, player.getZ(),
                        player.getRandom().nextGaussian(), 0.0D, player.getRandom().nextGaussian());
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("I've seen the Rod of Discord and honestly").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        tooltipComponents.add(Component.literal("it's not as amazing as people say.").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.literal("Rod of Discord is crucial in so many boss fights.").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        tooltipComponents.add(Component.literal("Imagine getting coiled by worm bosses.").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.literal("Oh, you mean the Terraria item.").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
        tooltipComponents.add(Component.literal("Idk about that thing.").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
    }
}
