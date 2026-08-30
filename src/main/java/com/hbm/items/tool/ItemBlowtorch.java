package com.hbm.items.tool;

import com.hbm.api.block.IToolable;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.TagsUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Locale;

/**
 * Fluid-fueled welding torch, ported from CE's {@code com.hbm.items.tool.ItemBlowtorch}. Same
 * {@link IToolable} dispatch shape as {@link ItemTooling} (this class does not extend it only
 * because CE's own {@code ItemBlowtorch} doesn't either - it implements {@link IFillableItem}
 * instead, which {@link ItemTooling} does not), gated on the tool holding at least 250mB of
 * {@link Fluids#GAS}.
 * <p>
 * Only the single-fluid {@code blowtorch} variant is ported (CE's second variant,
 * {@code acetylene_torch}, runs on a two-tank unsaturated-hydrocarbons/oxygen mix and is not one of
 * the 19 items_tool bucket-(c) items this pass owns - left to whichever pass ports that item, which
 * can extend this class the same way CE's two variants shared one Java class via
 * {@code this == ModItems.X} identity checks).
 * <p>
 * Fuel is stored via {@link com.hbm.util.TagsUtil}'s {@code minecraft:custom_data} helper (an
 * {@code int} under key {@code "fill"}) rather than a bespoke {@link net.minecraft.core.component.DataComponentType}
 * - this port's own established "not yet migrated off ad-hoc NBT" idiom (see {@code TagsUtil}'s own
 * javadoc, and {@link ItemWiring}'s identical choice for its own per-stack state) - simpler than
 * standing up a new component registry for one {@code int} field.
 */
public class ItemBlowtorch extends Item implements IFillableItem {

    public static final int MAX_FILL = 4_000;
    private static final int COST_PER_USE = 250;

    public ItemBlowtorch(Properties properties) {
        super(properties);
        IToolable.ToolType.TORCH.register(new ItemStack(this));
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        return type == Fluids.GAS;
    }

    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (!acceptsFluid(type, stack)) return amount;
        int toFill = Math.min(amount, MAX_FILL - getFill(stack));
        setFillAmount(stack, getFill(stack) + toFill);
        return amount - toFill;
    }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        return false;
    }

    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        return amount;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        return getFill(stack) > 0 ? Fluids.GAS : null;
    }

    @Override
    public int getFill(ItemStack stack) {
        return getFill(stack, Fluids.GAS);
    }

    public int getFill(ItemStack stack, FluidType type) {
        if (type != Fluids.GAS) return 0;
        CompoundTag tag = TagsUtil.getCustomData(stack);
        return tag.contains("fill") ? tag.getInt("fill") : MAX_FILL;
    }

    public void setFillAmount(ItemStack stack, int fill) {
        CompoundTag tag = TagsUtil.getCustomData(stack);
        tag.putInt("fill", Math.clamp(fill, 0, MAX_FILL));
        TagsUtil.putCustomData(stack, tag);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getLevel().isClientSide()) return InteractionResult.SUCCESS;

        Block block = context.getLevel().getBlockState(context.getClickedPos()).getBlock();
        if (!(block instanceof IToolable toolable)) return InteractionResult.SUCCESS;

        ItemStack stack = context.getItemInHand();
        if (getFill(stack) < COST_PER_USE) return InteractionResult.FAIL;

        var pos = context.getClickedPos();
        float fX = (float) (context.getClickLocation().x - pos.getX());
        float fY = (float) (context.getClickLocation().y - pos.getY());
        float fZ = (float) (context.getClickLocation().z - pos.getZ());
        boolean handled = toolable.onScrew(context.getLevel(), player, pos, context.getClickedFace(), fX, fY, fZ,
                context.getHand(), IToolable.ToolType.TORCH);

        if (handled) {
            setFillAmount(stack, getFill(stack) - COST_PER_USE);
        }

        // CE always returns SUCCESS here ("due to minecraft being stupid i have to always return success").
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getFill(stack) < MAX_FILL;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getFill(stack) / MAX_FILL);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Fluids.GAS.getLocalizedName().copy()
                .append(": " + String.format(Locale.US, "%,d", getFill(stack)) + " / " + String.format(Locale.US, "%,d", MAX_FILL))
                .withStyle(ChatFormatting.YELLOW));
    }
}
