package com.hbm.items.special;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Port of CE's {@code ItemRag}: a simple state-transition item (dry -> damp when it touches water,
 * damp/dry -> piss when thrown) shared by {@code rag} and {@code mask_rag}. CE hardcoded the
 * companion items via {@code this == ModItems.rag} identity checks; the port takes the two companion
 * item ids in the constructor instead so one class still backs both fields without a hard reference
 * to either.
 * <p>
 * {@code rag_damp}/{@code rag_piss}/{@code mask_damp}/{@code mask_piss} are not owned by this area
 * (a different, not-yet-run Phase 1 area registers the gas-mask/cleaning-item family they belong
 * to), so both companion lookups resolve by registry id at call time via
 * {@link BuiltInRegistries#ITEM} rather than a compile-time field reference, and no-op gracefully
 * until those items exist.
 */
public class ItemRag extends Item {

    private final ResourceLocation dampId;
    private final ResourceLocation pissId;

    public ItemRag(Properties properties, String dampName, String pissName) {
        super(properties);
        this.dampId = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, dampName);
        this.pissId = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, pissName);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (level.isClientSide()) {
            return false;
        }
        boolean wet = entity.isInWater() || level.getFluidState(entity.blockPosition()).is(FluidTags.WATER);
        if (!wet) {
            return false;
        }
        return findCompanion(dampId).map(damp -> {
            entity.setItem(new ItemStack(damp, stack.getCount()));
            return true;
        }).orElse(false);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        findCompanion(pissId).ifPresent(piss -> player.drop(new ItemStack(piss), false));
        stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }

    private static Optional<Item> findCompanion(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id);
    }
}
