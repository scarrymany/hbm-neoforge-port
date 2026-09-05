package com.hbm.items.tool;

import com.hbm.items.gear.ArmorFSB;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.util.ContaminationUtil;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Worn radiation-exposure readout. Exact CE {@code com.hbm.items.tool.ItemDosimeter}
 * {@code :37-83}: {@code onUpdate} FSB {@code geigerSound} skip + dosimeter-tier clicks via
 * {@code getActualPlayerRads}, {@code onItemRightClick} →
 * {@code ContaminationUtil.printDosimeterData}.
 *
 * <p>Bauble {@code onWornTick} stays dropped (no Curios).
 */
public class ItemDosimeter extends Item {

    private final Random rand = new Random();

    public ItemDosimeter(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // CE ItemDosimeter.java:37-72
        if (!(entity instanceof LivingEntity) || level.isClientSide()) {
            return;
        }

        if (entity instanceof Player player) {
            ItemStack plate = player.getItemBySlot(EquipmentSlot.CHEST);
            if (ArmorFSB.hasFSBArmor(player) && plate.getItem() instanceof ArmorFSB fsb && fsb.geigerSound) {
                return;
            }

            double x = ContaminationUtil.getActualPlayerRads(player);

            if (level.getGameTime() % 5 == 0) {
                SoundEvent[] geigerSounds = HBMSoundHandler.geigerSounds();

                if (x > 1e-5) {
                    List<Integer> list = new ArrayList<>();
                    if (x < 0.5) list.add(0);
                    if (x < 1) list.add(1);
                    if (x >= 0.5 && x < 2) list.add(2);
                    if (x >= 1 && x >= 2) list.add(3);

                    int r = list.get(rand.nextInt(list.size()));
                    if (r > 0) {
                        level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), geigerSounds[r - 1], SoundSource.PLAYERS, 1.0F, 1.0F);
                    }
                } else if (rand.nextInt(100) == 0) {
                    level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), geigerSounds[0], SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // CE ItemDosimeter.java:76-83
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBoop.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            ContaminationUtil.printDosimeterData(player);
        }
        return InteractionResultHolder.success(stack);
    }
}
