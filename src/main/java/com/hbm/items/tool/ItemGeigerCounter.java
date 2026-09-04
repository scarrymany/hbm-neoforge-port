package com.hbm.items.tool;

import com.hbm.items.gear.ArmorFSB;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

/**
 * Handheld ambient radiation-click detector. Exact CE
 * {@code com.hbm.items.tool.ItemGeigerCounter} {@code :34-98}: {@code onUpdate} FSB
 * {@code geigerSound} skip, {@code playGeiger} via {@code getActualPlayerRads},
 * {@code onItemUse} {@code block_red_copper} → {@code survey_scanner},
 * {@code onItemRightClick} → {@code ContaminationUtil.printGeigerData}.
 *
 * <p>Bauble {@code onWornTick} stays dropped (no Curios). Placed geiger TE is not invented.
 */
public class ItemGeigerCounter extends Item {

    public ItemGeigerCounter(Properties properties) {
        super(properties);
    }

    public static void playGeiger(Level level, Player player) {
        if (level.isClientSide()) {
            return;
        }

        double x = ContaminationUtil.getActualPlayerRads(player);

        if (level.getGameTime() % 5 == 0) {
            SoundEvent[] geigerSounds = HBMSoundHandler.geigerSounds();

            if (x > 1e-5) {
                List<Integer> list = new ArrayList<>();
                if (x < 1) list.add(0);
                if (x < 5) list.add(0);
                if (x < 10) list.add(1);
                if (x > 5 && x < 15) list.add(2);
                if (x > 10 && x < 20) list.add(3);
                if (x > 15 && x < 25) list.add(4);
                if (x > 20 && x < 30) list.add(5);
                if (x > 25) list.add(6);

                int r = list.get(level.getRandom().nextInt(list.size()));
                if (r > 0) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(), geigerSounds[r - 1], SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            } else if (level.getRandom().nextInt(100) == 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), geigerSounds[0], SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        // CE ItemGeigerCounter.java:37-46
        if (!(entity instanceof LivingEntity) || level.isClientSide()) {
            return;
        }
        if (entity instanceof Player player) {
            ItemStack plate = player.getItemBySlot(EquipmentSlot.CHEST);
            if (ArmorFSB.hasFSBArmor(player) && plate.getItem() instanceof ArmorFSB fsb && fsb.geigerSound) {
                return;
            }
            playGeiger(level, player);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // CE ItemGeigerCounter.java:75-82
        Level level = context.getLevel();
        Block redCopper = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "block_red_copper"));
        if (level.getBlockState(context.getClickedPos()).getBlock() == redCopper) {
            Player player = context.getPlayer();
            if (player != null && !level.isClientSide()) {
                consumeInventoryItem(player.getInventory(), ToolItems.GEIGER_COUNTER.get());
                if (!player.getInventory().add(new ItemStack(ToolItems.SURVEY_SCANNER.get()))) {
                    player.drop(new ItemStack(ToolItems.SURVEY_SCANNER.get()), false);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // CE ItemGeigerCounter.java:86-93
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), HBMSoundHandler.techBoop.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            ContaminationUtil.printGeigerData(player);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return !ItemStack.isSameItem(oldStack, newStack);
    }

    /** CE {@code Library.consumeInventoryItem} {@code :1033-1041}. */
    private static void consumeInventoryItem(Inventory inventory, Item item) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() == item && !stack.isEmpty()) {
                stack.shrink(1);
                return;
            }
        }
    }
}
