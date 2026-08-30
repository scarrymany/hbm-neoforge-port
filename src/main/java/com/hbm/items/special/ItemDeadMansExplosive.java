package com.hbm.items.special;

import com.hbm.config.GeneralConfig;
import com.hbm.config.WeaponConfig;
import com.hbm.main.MainRegistry;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.special.ItemDrop} (244 lines, read in full) - just the
 * {@code detonator_de} branch, per {@code docs/phase3/scattered_military_items.md}'s explicit split
 * recommendation (see {@link ItemDeadmanDetonator}'s javadoc for the shared rationale). Unlike
 * {@link ItemDeadmanDetonator}, this has no target-recall behavior at all - it simply explodes
 * (config-gated) the instant it lands on the ground or is otherwise dropped.
 * <p>
 * {@link WeaponConfig#DROP_DEAD_MANS_EXPLOSIVE} is already fully wired in this port (confirmed by
 * direct read, matching CE's {@code 10.05_dropDead} config key) - no new config plumbing needed.
 */
public class ItemDeadMansExplosive extends Item {

    public ItemDeadMansExplosive(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entityItem) {
        Level level = entityItem.level();

        if (!level.isClientSide() && WeaponConfig.DROP_DEAD_MANS_EXPLOSIVE.get()) {
            level.explode(entityItem, entityItem.getX(), entityItem.getY(), entityItem.getZ(), 15.0F, true, Level.ExplosionInteraction.TNT);

            if (GeneralConfig.ENABLE_EXTENDED_LOGGING.get()) {
                MainRegistry.logger.info("[DET] Detonated dead man's explosive at {} / {} / {}!",
                        (int) entityItem.getX(), (int) entityItem.getY(), (int) entityItem.getZ());
            }
        }

        entityItem.discard();
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Explodes when dropped!"));
        tooltip.add(Component.literal("[" + I18nUtil.resolveKey("trait.drop") + "]").withStyle(ChatFormatting.RED));
    }
}
