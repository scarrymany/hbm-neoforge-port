package com.hbm.items.machine;

import com.hbm.blockentity.network.PipeBaseBlockEntity;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.TagsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Dual-fluid duct painter/identifier, ported (in reduced scope) from CE's
 * {@code com.hbm.items.machine.ItemFluidIDMulti} (read in full). Right-click a duct
 * ({@link PipeBaseBlockEntity}) to set its fluid type to this stack's "primary" fluid (sneak =
 * recursive flood-fill repaint of every connected duct currently on the "secondary" type, matching
 * CE's {@code spreadType}); right-click air to swap primary/secondary. Implements
 * {@link IItemFluidIdentifier} per {@code docs/phase2/items_tool_machine_coupling_and_recipe_system.md}'s
 * recommendation to port both in the same file.
 * <p>
 * <b>Scope reduction from CE, documented rather than silent</b>: CE's real item is a per-fluid
 * metadata-subtype stack with a baked dual-layer (base+tinted overlay) item model and a
 * {@code GUIScreenFluid} fluid-picker GUI (sneak-right-click in air) for choosing the primary/
 * secondary pair from every registered fluid. Metadata subtypes are gone post-flattening, and
 * {@code gui_framework.md}'s Menu/Screen framework has no fluid-picker screen of its own yet - both
 * are rendering/UX concerns, not the duct-painting mechanic itself. This port's version stores the
 * primary/secondary pair via {@link TagsUtil} (same idiom as {@link ItemFFFluidDuct}) and swaps them
 * on a plain right-click-in-air instead of opening a GUI; the core duct-painting/flood-fill mechanic
 * this item exists for is fully real and functional. Creative-mode variants pre-set to each
 * registered fluid can be added to the creative tab once needed, the same way CE's
 * {@code getSubItems} did.
 */
public class ItemFluidIDMulti extends Item implements IItemFluidIdentifier {

    private static final int SPREAD_LIMIT = 256;

    public ItemFluidIDMulti(Properties properties) {
        super(properties);
    }

    public static void setType(ItemStack stack, FluidType type, boolean primary) {
        CompoundTag tag = TagsUtil.getCustomData(stack);
        tag.putInt(primary ? "fluid1" : "fluid2", type.getID());
        TagsUtil.putCustomData(stack, tag);
    }

    public static FluidType getType(ItemStack stack, boolean primary) {
        CompoundTag tag = TagsUtil.getCustomData(stack);
        return Fluids.fromID(tag.getInt(primary ? "fluid1" : "fluid2"));
    }

    @Override
    public FluidType getType(Level level, BlockPos pos, ItemStack stack) {
        return getType(stack, true);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        if (!(level.getBlockEntity(pos) instanceof PipeBaseBlockEntity duct)) return InteractionResult.PASS;

        FluidType handType = getType(stack, true);
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (handType != duct.getType()) {
            if (player != null && player.isShiftKeyDown()) {
                spreadType(level, pos, handType, duct.getType(), SPREAD_LIMIT);
            } else {
                duct.setType(handType);
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            FluidType primary = getType(stack, true);
            FluidType secondary = getType(stack, false);
            setType(stack, secondary, true);
            setType(stack, primary, false);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, 1.25F);
            player.displayClientMessage(secondary.getLocalizedName(), true);
        }

        return InteractionResultHolder.success(stack);
    }

    private static void spreadType(Level level, BlockPos pos, FluidType hand, FluidType pipe, int remaining) {
        if (remaining <= 0) return;
        if (!(level.getBlockEntity(pos) instanceof PipeBaseBlockEntity duct)) return;
        if (duct.getType() != pipe) return;

        duct.setType(hand);
        duct.setChanged();

        for (var dir : net.minecraft.core.Direction.values()) {
            spreadType(level, pos.relative(dir), hand, pipe, remaining - 1);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Right-click duct: paint primary fluid"));
        tooltip.add(Component.literal("   ").append(getType(stack, true).getLocalizedName()));
        tooltip.add(Component.literal("Shift+right-click duct: flood-fill paint"));
        tooltip.add(Component.literal("Right-click air: swap primary/secondary"));
        tooltip.add(Component.literal("Secondary: ").append(getType(stack, false).getLocalizedName()));
    }
}
