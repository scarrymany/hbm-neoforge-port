package com.hbm.items.machine;

import com.hbm.blockentity.network.PipeBaseBlockEntity;
import com.hbm.blocks.network.FluidDuctBlocks;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.TagsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Pre-set-fluid duct placer, ported from CE's {@code com.hbm.items.machine.ItemFFFluidDuct} (read in
 * full). CE places a bespoke {@code ModBlocks.fluid_duct_neo} block whose metadata damage value
 * encodes the fluid type - no such block exists in this port (only {@link FluidDuctBlocks#DUCT_STANDARD}
 * does), so this port places the real, already-shipped {@code fluid_duct_standard} block and
 * immediately calls its {@link PipeBaseBlockEntity#setType} instead - functionally identical to CE's
 * intent ("place a duct pre-set to a chosen fluid in one click") using this port's real target block
 * rather than a CE-only variant that was never ported. The fluid type to preset is stored on the
 * stack via {@link TagsUtil} (CE: an item-damage-value metadata subtype - post-flattening, this
 * becomes a plain per-stack field, matching {@link ItemFluidIDMulti}'s own choice).
 */
public class ItemFFFluidDuct extends Item {

    public ItemFFFluidDuct(Properties properties) {
        super(properties);
    }

    public static ItemStack getStackFromFluid(Item item, FluidType type) {
        ItemStack stack = new ItemStack(item);
        setFluid(stack, type);
        return stack;
    }

    public static void setFluid(ItemStack stack, FluidType type) {
        CompoundTag tag = TagsUtil.getCustomData(stack);
        tag.putInt("fluid", type.getID());
        TagsUtil.putCustomData(stack, tag);
    }

    public static FluidType getFluid(ItemStack stack) {
        CompoundTag tag = TagsUtil.getCustomData(stack);
        return tag.contains("fluid") ? Fluids.fromID(tag.getInt("fluid")) : Fluids.NONE;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        Direction facing = context.getClickedFace();

        if (!level.getBlockState(pos).canBeReplaced()) {
            pos = pos.relative(facing);
            if (!level.getBlockState(pos).isAir()) return InteractionResult.FAIL;
        }

        ItemStack stack = context.getItemInHand();
        if (player != null && !player.mayUseItemAt(pos, facing, stack)) return InteractionResult.FAIL;

        if (!level.isClientSide) {
            level.setBlock(pos, FluidDuctBlocks.DUCT_STANDARD.get().defaultBlockState(), 3);
            if (level.getBlockEntity(pos) instanceof PipeBaseBlockEntity pipe) {
                pipe.setType(getFluid(stack));
            }
            stack.shrink(1);
            level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.METAL_PLACE, SoundSource.PLAYERS, 1F, 0.8F + level.getRandom().nextFloat() * 0.2F);
        }

        return InteractionResult.SUCCESS;
    }
}
