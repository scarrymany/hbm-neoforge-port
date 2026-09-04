package com.hbm.items.weapon.sedna.content;

import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.GunStateDecider;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.impl.ItemGunDrill;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineFluid;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.MainRegistry;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import com.hbm.util.EntityDamageUtil;
import com.hbm.weapon.anim.GunAnimationType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Port of CE's {@code XFactoryDrill}. Block harvest is Exact CE {@code :62-130}:
 * {@code getMouseOver} + sneak-gated AoE + {@code tryHarvestBlock} →
 * {@code ServerPlayer.gameMode.destroyBlock}. Weapon-mod reach/DT/pierce/AoE via
 * {@link XWeaponModManager#eval}. Electric engine burns 1000. Client AoE highlight skipped.
 */
public final class XFactoryDrill {

    public static final String D_REACH = "D_REACH";
    public static final String F_DTNEG = "F_DTNEG";
    public static final String F_PIERCE = "F_PIERCE";
    public static final String I_AOE = "I_AOE";
    public static final String I_HARVEST = "I_HARVEST";

    private static final ResourceLocation ENGINE_ELECTRIC =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "engine_electric");

    private XFactoryDrill() {
    }

    public static ItemGunBaseNT gun_drill() {
        return new ItemGunDrill(new Item.Properties(), WeaponQuality.UTILITY,
                new GunConfig()
                        .dura(3_000).draw(10).inspect(55).hideCrosshair(false).crosshair(Crosshair.L_CIRCUMFLEX)
                        .rec(new Receiver(0)
                                .dmg(10F).delay(20).auto(true).jam(0)
                                .mag(new MagazineFluid(0, 4_000, Fluids.GASOLINE, Fluids.GASOLINE_LEADED, Fluids.COALGAS, Fluids.COALGAS_LEADED))
                                .offset(1, -0.15625, -0.25D)
                                .canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(XFactoryDrill::drillFire))
                        .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD).decider(GunStateDecider.LAMBDA_STANDARD_DECIDER));
    }

    /** Exact CE {@code XFactoryDrill.java:62-102} {@code doStandardFire}. */
    private static void drillFire(ItemStack stack, ItemGunBaseNT.LambdaContext ctx) {
        Player player = ctx.getPlayer();
        int index = ctx.configIndex;
        if (player == null) return;

        ItemGunBaseNT.playAnimation(player, stack, GunAnimationType.CYCLE, index);

        Receiver primary = ctx.config.getReceivers(stack)[0];
        @SuppressWarnings("unchecked")
        IMagazine<Object> mag = (IMagazine<Object>) primary.getMagazine(stack);

        HitResult mop = EntityDamageUtil.getMouseOver(player, getModdableReach(stack, 5.0D));
        if (mop != null) {
            if (mop.getType() == HitResult.Type.ENTITY && mop instanceof EntityHitResult ehr) {
                float damage = primary.getBaseDamage(stack);
                DamageSource source = player.damageSources().playerAttack(player);
                if (ehr.getEntity() instanceof LivingEntity living) {
                    EntityDamageUtil.attackEntityFromNT(living, source, damage, true, true, 0.1D,
                            getModdableDTNegation(stack, 2F), getModdablePiercing(stack, 0.15F));
                } else {
                    ehr.getEntity().hurt(source, damage);
                }
            }
            if (mop.getType() == HitResult.Type.BLOCK && mop instanceof BlockHitResult bhr) {
                int aoe = player.isShiftKeyDown() ? 0 : getModdableAoE(stack, 1);
                boolean didPlink = false;
                BlockPos origin = bhr.getBlockPos();
                for (int i = -aoe; i <= aoe; i++) {
                    for (int j = -aoe; j <= aoe; j++) {
                        for (int k = -aoe; k <= aoe; k++) {
                            didPlink = breakExtraBlock(player.level(), origin.offset(i, j, k), player, didPlink);
                        }
                    }
                }
            }
        }

        int ammoToUse = 10;
        if (XWeaponModManager.hasUpgrade(stack, 0, ENGINE_ELECTRIC)) ammoToUse = 1_000;
        mag.useUpAmmo(stack, ctx.inventory, ammoToUse);
        ItemGunBaseNT.setWear(stack, index, Math.min(ItemGunBaseNT.getWear(stack, index), ctx.config.getDurability(stack)));
    }

    /** Exact CE {@code XFactoryDrill.java:105-130} {@code breakExtraBlock}. */
    public static boolean breakExtraBlock(Level level, BlockPos pos, Player playerEntity, boolean didPlink) {
        if (level.isEmptyBlock(pos) || !(playerEntity instanceof ServerPlayer player)) return didPlink;

        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);

        if (!state.getBlock().canHarvestBlock(state, level, pos, player)
                || hardness == -1.0F
                || hardness == 0.0F) {
            if (!didPlink) {
                level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.5F, 0.8F + level.getRandom().nextFloat() * 0.6F);
                return true;
            }
            return didPlink;
        }

        player.gameMode.destroyBlock(pos);

        if (level.isEmptyBlock(pos)) {
            player.connection.send(new ClientboundLevelEventPacket(2001, pos, Block.getId(state), false));
        }

        return didPlink;
    }

    public static double getModdableReach(ItemStack stack, double base) {
        return XWeaponModManager.eval(base, stack, D_REACH, stack.getItem(), 0);
    }

    public static float getModdableDTNegation(ItemStack stack, float base) {
        return XWeaponModManager.eval(base, stack, F_DTNEG, stack.getItem(), 0);
    }

    public static float getModdablePiercing(ItemStack stack, float base) {
        return XWeaponModManager.eval(base, stack, F_PIERCE, stack.getItem(), 0);
    }

    public static int getModdableAoE(ItemStack stack, int base) {
        return XWeaponModManager.eval(base, stack, I_AOE, stack.getItem(), 0);
    }

    public static int getModdableHarvestLevel(ItemStack stack, int base) {
        return XWeaponModManager.eval(base, stack, I_HARVEST, stack.getItem(), 0);
    }
}
