package com.hbm.items.tool;

import com.hbm.items.machine.ItemSatChip;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ported from CE's {@code com.hbm.items.tool.ItemSatDesignator} (52 lines, read in full) -
 * {@code extends ItemSatChip}. Right-click ray-traces 300 blocks, looks up the satellite at this
 * item's stored frequency, and dispatches to {@link Satellite#onCoordAction}/{@link Satellite#onClick}
 * depending on that satellite's {@link Satellite.Interfaces} value. Raytrace idiom matches this
 * port's already-committed {@link ItemDesignatorRange}/{@code ItemLaserDetonator}.
 */
public class ItemSatDesignator extends ItemSatChip {

    private static final double RANGE = 300.0D;

    public ItemSatDesignator(String descKey, Properties properties) {
        super(descKey, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            Satellite sat = SatelliteSavedData.getData(level).getSatFromFreq(this.getFreq(stack));

            if (sat != null) {
                Vec3 eye = player.getEyePosition(1.0F);
                Vec3 look = player.getViewVector(1.0F);
                Vec3 end = eye.add(look.scale(RANGE));
                BlockHitResult ray = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));

                BlockPos rayBlockPos = ray.getBlockPos();
                Direction facing = ray.getDirection();

                int x = rayBlockPos.getX() + facing.getStepX();
                int y = rayBlockPos.getY() + facing.getStepY();
                int z = rayBlockPos.getZ() + facing.getStepZ();

                if (sat.satIface == Satellite.Interfaces.SAT_COORD) {
                    sat.onCoordAction(level, serverPlayer, x, y, z);
                } else if (sat.satIface == Satellite.Interfaces.SAT_PANEL) {
                    sat.onClick(level, serverPlayer, x, z);
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
