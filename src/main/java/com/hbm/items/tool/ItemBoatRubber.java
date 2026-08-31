package com.hbm.items.tool;

import com.hbm.entity.item.BoatEntityTypes;
import com.hbm.entity.item.EntityBoatRubber;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * CE: {@code com.hbm.items.tool.ItemBoatRubber} (106 lines) — place {@link EntityBoatRubber}.
 */
public class ItemBoatRubber extends Item {

    public ItemBoatRubber(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        Vec3 look = player.getViewVector(1.0F);
        List<Entity> list = level.getEntities(player,
                player.getBoundingBox().expandTowards(look.scale(5.0D)).inflate(1.0D),
                e -> e.isPickable());
        if (!list.isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }

        EntityBoatRubber boat = new EntityBoatRubber(BoatEntityTypes.BOAT_RUBBER.get(), level);
        boat.setPos(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
        boat.setYRot(player.getYRot());
        if (!level.noCollision(boat, boat.getBoundingBox().inflate(-0.1D))) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            level.addFreshEntity(boat);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, hit.getLocation());
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
