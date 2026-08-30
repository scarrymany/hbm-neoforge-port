package com.hbm.items.tool;

import com.hbm.config.GeneralConfig;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IHoldableWeapon;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import com.hbm.render.misc.RenderScreenOverlay;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Stateless raytracing remote detonator, ported from CE's
 * {@code com.hbm.items.tool.ItemLaserDetonator} (read in full). Unlike {@link ItemDetonator}/
 * {@link ItemMultiDetonator}, this item stores nothing on the stack - every right-click raytraces
 * live from the player's look vector and detonates whatever {@link IBomb} the ray lands on.
 *
 * <p>CE's {@code Library.rayTrace(player, 500, 1)} (a 500-block block-only raycast that ignores
 * liquids, respects blocks without a full collision box, and always returns a result even on a
 * total miss) maps onto {@link Level#clip(ClipContext)} with
 * {@link ClipContext.Block#OUTLINE}/{@link ClipContext.Fluid#NONE} - the same raytrace shape this
 * port's already-committed {@code ItemDiscord} uses in place of {@code Library.rayTrace}.
 * {@code Level#clip} likewise never returns {@code null} (a miss still yields a
 * {@link BlockHitResult} at the ray's end), matching CE's own {@code returnLastUncollidableBlock}
 * flag - so, exactly like CE, no null-check is needed before reading the hit position.
 */
public class ItemLaserDetonator extends Item implements IHoldableWeapon {

    private static final double RANGE = 500.0D;

    public ItemLaserDetonator(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(I18nUtil.resolveKey("item.detonator_laser.desc")));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(RANGE));
        BlockHitResult ray = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        BlockPos pos = ray.getBlockPos();

        if (!level.isClientSide()) {
            if (level.getBlockState(pos).getBlock() instanceof IBomb bomb) {
                bomb.explode(level, pos, player);

                if (GeneralConfig.ENABLE_EXTENDED_LOGGING.get()) {
                    MainRegistry.logger.info("[DET] Tried to detonate block at {} / {} / {} by {}!",
                            pos.getX(), pos.getY(), pos.getZ(), player.getDisplayName().getString());
                }

                player.displayClientMessage(Component.literal("[" + I18nUtil.resolveKey("chat.detonated") + "]")
                        .withStyle(ChatFormatting.DARK_GREEN), false);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBleep.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
            } else {
                player.displayClientMessage(Component.literal(I18nUtil.resolveKey("chat.posbadrror"))
                        .withStyle(ChatFormatting.RED), false);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBleep.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
            }
        }

        return super.use(level, player, hand);
    }

    @Override
    public RenderScreenOverlay.Crosshair getCrosshair() {
        return RenderScreenOverlay.Crosshair.L_ARROWS;
    }
}
