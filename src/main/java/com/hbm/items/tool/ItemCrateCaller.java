package com.hbm.items.tool;

import com.hbm.blocks.generic.BlockCrate;
import com.hbm.blocks.generic.GenericCrateBlocks;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Novelty "call in a loot crate" item: randomly drops one of several crate blocks near the player.
 * Ported from CE's {@code com.hbm.items.tool.ItemCrateCaller} (101 lines, read in full).
 *
 * <p><b>Review-pass fix.</b> This class used to be stubbed out ("Block classes for this family
 * already exist... but {@code GenericCrateBlocks.registerAll()} is never called from
 * {@code ModBlocks.register()}") - that gap has since been closed by an earlier phase (it <em>is</em>
 * wired, transitively, via {@code GenericBlocks.registerAll()} - confirmed by a fresh read of
 * {@code ModBlocks.register()}), and {@link GenericCrateBlocks} now exposes lazy accessors for every
 * plain crate variant CE places here (added alongside this fix, matching the {@code crateSupply()}
 * accessor {@code EntityParachuteCrate} already uses). The stale stub is replaced with CE's real
 * behavior below.
 * <p>
 * CE's {@code stack.damageItem(1, player)} runs unconditionally before the {@code !world.isRemote}
 * check; mirrored here with the {@code ItemStack.hurtAndBreak(int, LivingEntity, EquipmentSlot)}
 * convenience overload already in live use at this exact call shape elsewhere in this port (e.g.
 * {@code ItemMeteorRemote}) - it resolves the entity's level internally and only applies durability
 * loss server-side, so calling it from both logical sides (as {@code Item#use} naturally is) does not
 * double-damage the stack. CE's weighted cascade ({@code i<350} weapon, {@code i<100} metal,
 * {@code i<50} lead, {@code i==0} red, else standard - later checks override earlier ones since none
 * {@code break}/{@code return}) and its "only place at y=255 if air" placement rule are reproduced
 * exactly. No {@code data/hbm/lang}/{@code chat.callsp} translation key has been ported anywhere in
 * this port yet (CE's own {@code TextComponentTranslation("chat.callsp")}), so - matching
 * {@code ItemMeteorRemote}'s own already-established fallback for the identical gap - the client
 * message is a literal {@link Component} instead of a translation key lookup.
 */
public class ItemCrateCaller extends Item {

    public ItemCrateCaller(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));

        int x = player.getRandom().nextInt(31) - 15;
        int z = player.getRandom().nextInt(31) - 15;

        DeferredBlock<BlockCrate> crate = GenericCrateBlocks.crateStandard();

        int i = player.getRandom().nextInt(1000);
        if (i < 350) crate = GenericCrateBlocks.crateWeapon();
        if (i < 100) crate = GenericCrateBlocks.crateMetal();
        if (i < 50) crate = GenericCrateBlocks.crateLead();
        if (i == 0) crate = GenericCrateBlocks.crateRed();

        if (!level.isClientSide()) {
            BlockPos pos = new BlockPos(player.getBlockX() + x, 255, player.getBlockZ() + z);
            if (level.getBlockState(pos).isAir()) {
                level.setBlockAndUpdate(pos, crate.get().defaultBlockState());
            }
        } else {
            player.displayClientMessage(Component.literal("The supply plane has heard your call!"), false);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBleep.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);

        player.swing(hand);
        return InteractionResultHolder.success(stack);
    }
}
