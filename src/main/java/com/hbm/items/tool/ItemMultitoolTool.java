package com.hbm.items.tool;

import com.hbm.items.ICustomItemModelRegister;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;

import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemMultitoolTool}, scoped to {@code multitool_dig}
 * and {@code multitool_silk} only. Sneak-right-click Exact CE {@code :37-46}: {@code techBoop}
 * 2.0F/1.0F, {@code multitool_dig} → {@code multitool_silk} + {@code SILK_TOUCH} 3, same damage.
 * silk→ext stay skipped — Passive ladder is a separate chain ({@code ItemMultitoolPassive}).
 *
 * <p>{@code multitool_silk} harvest silk is the real vanilla enchantment (CE swap + fallback
 * around harvest for stacks that arrived without it).
 */
public class ItemMultitoolTool extends TieredItem implements ICustomItemModelRegister {

    private final boolean silkTouch;

    public ItemMultitoolTool(Properties properties, Tier tier, boolean silkTouch) {
        super(tier, properties);
        this.silkTouch = silkTouch;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return getTier().getSpeed();
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isCrouching()) {
            return InteractionResultHolder.pass(stack);
        }
        // CE ItemMultitoolTool.java:39-46
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                HBMSoundHandler.techBoop.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
        if (this == ToolItems.MULTITOOL_DIG.get()) {
            ItemStack item = new ItemStack(ToolItems.MULTITOOL_SILK.get());
            item.setDamageValue(stack.getDamageValue());
            Holder<Enchantment> silk = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(Enchantments.SILK_TOUCH);
            EnchantmentHelper.updateEnchantments(item, mutable -> mutable.set(silk, 3));
            return InteractionResultHolder.success(item);
        }
        // silk→ext stay skipped — Passive ladder is a separate chain (CE :47-50)
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        if (!silkTouch || level.isClientSide()) {
            return super.mineBlock(stack, level, state, pos, miningEntity);
        }

        Holder<Enchantment> silk = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH);
        int prev = EnchantmentHelper.getItemEnchantmentLevel(silk, stack);
        EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(silk, Math.max(prev, 1)));
        try {
            return super.mineBlock(stack, level, state, pos, miningEntity);
        } finally {
            EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(silk, prev));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Breaks blocks extremely fast"));
        tooltipComponents.add(Component.literal(silkTouch ? "Ores will drop themselves via silk touch" : "Extra drops for ores"));
    }

    /** Handheld 3D tool, not a flat icon - see {@link ItemToolAbility#registerItemModel} for the full rationale. */
    @Override
    public void registerItemModel(ItemModelProvider provider, ResourceLocation modelLocation) {
        provider.withExistingParent(modelLocation.getPath(), provider.mcLoc("item/handheld"))
                .texture("layer0", provider.modLoc("item/" + modelLocation.getPath()));
    }
}
