package com.hbm.items.tool;

import com.hbm.blockentity.network.PipelineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.util.TagsUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Long-distance pipeline anchor linker, ported from CE's {@code com.hbm.items.tool.ItemWrench} (read
 * in full). Mark a first {@link PipelineBaseBlockEntity} anchor (stores its position via
 * {@link TagsUtil}'s {@code minecraft:custom_data}, mirroring CE's own per-stack
 * {@code NBTTagCompound}), then right-click a second one to link them via the exact TE-side surface
 * {@code docs/phase2/network_fluid_ducts.md} says it left ready for this item:
 * {@link PipelineBaseBlockEntity#addConnection}/{@code static canConnect(...)} (that report's own
 * javadoc on {@code PipelineBaseBlockEntity} names this class by name as the intended caller).
 * <p>
 * <b>Not the pylon-linking wrench</b>: {@code docs/phase2/energy_cable_pylon_network.md} originally
 * guessed CE's {@code ItemWrench} also touches {@code TileEntityPylonBase}, but CE's real source
 * (read directly for this pass) shows it only ever touches {@code TileEntityPipelineBase} - the
 * already-ported {@link ItemWiring} (a separate sibling-package item) is what links pylons. No
 * behavior is missing by keeping this item pipeline-only: it matches CE exactly.
 */
public class ItemWrench extends SwordItem {

    public ItemWrench(Tier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        if (level.getBlockState(pos).getBlock() instanceof BlockDummyable dummy) {
            BlockPos core = dummy.findCore(level, pos);
            if (core != null) pos = core;
        }

        BlockEntity te = level.getBlockEntity(pos);
        if (!(te instanceof PipelineBaseBlockEntity second)) return InteractionResult.PASS;

        if (level.isClientSide) {
            player.swing(context.getHand());
            return InteractionResult.SUCCESS;
        }

        CompoundTag tag = TagsUtil.getCustomData(stack);
        if (!tag.contains("x")) {
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            TagsUtil.putCustomData(stack, tag);
            player.displayClientMessage(Component.literal("Pipe start"), true);
        } else {
            BlockPos firstPos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
            BlockEntity firstTe = level.getBlockEntity(firstPos);

            if (firstTe instanceof PipelineBaseBlockEntity first) {
                switch (PipelineBaseBlockEntity.canConnect(first, second)) {
                    case 0 -> {
                        first.addConnection(pos);
                        second.addConnection(firstPos);
                        player.displayClientMessage(Component.literal("Pipe end"), true);
                    }
                    case 1 -> player.displayClientMessage(Component.literal("Pipe error - Pipes are not the same type"), true);
                    case 2 -> player.displayClientMessage(Component.literal("Pipe error - Cannot connect to the same pipe anchor"), true);
                    case 3 -> player.displayClientMessage(Component.literal("Pipe error - Pipe anchor is too far away"), true);
                    case 4 -> player.displayClientMessage(Component.literal("Pipe error - Pipe anchor fluid types do not match"), true);
                    default -> {
                    }
                }
            } else {
                player.displayClientMessage(Component.literal("Pipe error"), true);
            }

            stack.remove(DataComponents.CUSTOM_DATA);
        }

        player.swing(context.getHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Level level = target.level();
        Vec3 look = attacker.getLookAngle();
        target.setDeltaMovement(target.getDeltaMovement().add(look.x * 0.5, look.y * 0.5, look.z * 0.5));
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 3.0F, 0.75F);
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag tag = TagsUtil.getCustomData(stack);
        if (tag.contains("x")) {
            tooltip.add(Component.literal("Pipe start x: " + tag.getInt("x")));
            tooltip.add(Component.literal("Pipe start y: " + tag.getInt("y")));
            tooltip.add(Component.literal("Pipe start z: " + tag.getInt("z")));
        } else {
            tooltip.add(Component.literal("Right-click anchor to connect"));
        }
    }
}
