package com.hbm.blocks.generic;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Wall-mounted armor recharge station, ported from CE's {@code HEVBattery}. CE gates the actual
 * charging on wearing a specific armor set ({@code ArmorFSB}/{@code ArmorFSBPowered}), neither of
 * which exists in this port's item catalog yet (items-area "gear" content, not part of this pass);
 * per the "no invented items" ground rule, this port charges every worn {@link IBatteryItem} piece
 * unconditionally instead of gating on that specific armor - once {@code ArmorFSB} lands, whoever
 * owns it should re-add the gating check here. CE's bespoke {@code .obj} model has no confirmed
 * NeoForge 1.21 geometry-loader equivalent, so this registers with a plain default model instead of
 * guessing an API (documented rendering gap).
 */
public class HEVBattery extends Block {

    private static final long CHARGE_PER_USE = 150_000L;
    private static final VoxelShape SHAPE = Block.box(6.0, 0.0, 6.0, 10.0, 6.0, 10.0);
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public HEVBattery(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        boolean charged = false;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.getItem() instanceof IBatteryItem battery) {
                battery.chargeBattery(armor, CHARGE_PER_USE);
                charged = true;
            }
        }

        if (charged) {
            level.playSound(null, pos, HBMSoundHandler.battery.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            level.removeBlock(pos, false);
        }
        return InteractionResult.SUCCESS;
    }
}
