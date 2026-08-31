package com.hbm.items.special;

import com.hbm.entity.mob.EntityBOTPrimeHead;
import com.hbm.entity.mob.EntityDuck;
import com.hbm.entity.mob.EntityHunterChopper;
import com.hbm.entity.mob.EntityUFO;
import com.hbm.entity.mob.Phase4BossEntityTypes2;
import com.hbm.entity.mob.WormEntityTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Port of CE's {@code ItemChopper}: 4 already-distinct instances ({@code spawn_chopper},
 * {@code spawn_worm}, {@code spawn_ufo}, {@code spawn_duck}) that place a specific mob entity on
 * right-click/use-on-liquid. Per docs/phase1/items_special.md, CE's own {@code spawnCreature}
 * dispatches on {@code this == ModItems.spawn_x} identity, which the port replaces with a
 * {@link SpawnMob} enum passed directly to the constructor - equivalent behavior, no forward reference
 * to {@code ModItems} needed.
 * <p>
 * <b>All 4 variants are now wired</b> (this task, {@code docs/phase4/entities_bosses.md} +
 * {@code docs/phase4/entities_vehicles_aircraft.md}): {@code spawn_worm} was already wired by a prior
 * package ({@link EntityBOTPrimeHead}, unchanged here); {@code spawn_chopper}/{@code spawn_ufo}/
 * {@code spawn_duck} are wired by this task, since {@link EntityHunterChopper}/{@link EntityUFO}/
 * {@link EntityDuck} now exist. Each places its mob ~3 blocks in front of the user with a random yaw
 * (CE's own {@code spawnCreature}'s {@code MathHelper.wrapDegrees(rand.nextFloat()*360)} - the same
 * simplified "spawn in front of the user" placement the worm variant already established, rather than
 * CE's own block-face-click {@code onItemUse} model). {@code spawn_ufo} additionally pre-sets
 * {@link EntityUFO#initialScanCooldown} to 100 and spawns 35 blocks higher, matching CE's own
 * {@code spawnCreature} branch for that variant exactly.
 */
public class ItemChopper extends Item {

    public enum SpawnMob {
        NONE, WORM, CHOPPER, UFO, DUCK
    }

    private final SpawnMob mob;

    public ItemChopper(Properties properties, SpawnMob mob) {
        super(properties);
        this.mob = mob;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (this.mob == SpawnMob.NONE) {
            return super.use(level, player, hand);
        }

        if (!level.isClientSide()) {
            Vec3 spawnPos = player.position().add(player.getLookAngle().scale(3.0D));
            float yaw = Mth.wrapDegrees(level.random.nextFloat() * 360.0F);

            LivingEntity spawned = switch (this.mob) {
                case WORM -> {
                    EntityBOTPrimeHead head = new EntityBOTPrimeHead(WormEntityTypes.BOTPRIME_HEAD.get(), level);
                    // Bug fix: spawnBody() reads the head's *current* position (via blockPosition())
                    // to place all 74 EntityBOTPrimeBody segments, then re-centers the head on that
                    // same block - it must run AFTER the head is positioned, matching
                    // BlockBallsSpawner's call order (setPos, then spawnBody()). Calling it here before
                    // the common post-switch spawned.setPos(...) below would spawn all 74 body segments
                    // at the head's default construction position (not spawnPos), stranding the entire
                    // body chain away from the head the moment it gets repositioned.
                    head.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                    head.spawnBody();
                    yield head;
                }
                case CHOPPER -> new EntityHunterChopper(Phase4BossEntityTypes2.HUNTER_CHOPPER.get(), level);
                case UFO -> {
                    EntityUFO ufo = new EntityUFO(Phase4BossEntityTypes2.UFO.get(), level);
                    ufo.initialScanCooldown = 100;
                    spawnPos = spawnPos.add(0, 35, 0);
                    yield ufo;
                }
                case DUCK -> new EntityDuck(Phase4BossEntityTypes2.DUCK.get(), level);
                case NONE -> null;
            };

            if (spawned != null) {
                spawned.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                spawned.setYRot(yaw);
                spawned.setYHeadRot(yaw);
                spawned.yBodyRot = yaw;
                if (stack.hasCustomHoverName()) {
                    spawned.setCustomName(stack.getHoverName());
                }
                level.addFreshEntity(spawned);

                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (mob == SpawnMob.WORM) {
            tooltip.add(Component.literal("Without a player in survival mode"));
            tooltip.add(Component.literal("to target, he struggles around a lot."));
            tooltip.add(Component.empty());
            tooltip.add(Component.literal("He's doing his best so please show him"));
            tooltip.add(Component.literal("some consideration."));
        }
    }
}
