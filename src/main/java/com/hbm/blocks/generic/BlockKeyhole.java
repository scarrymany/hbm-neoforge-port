package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.tool.CouplingToolItems;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * CE {@code BlockKeyhole} — stone_keyhole hidden in structures. {@code key_red} / {@code key_red_cracked}
 * use opens door + generates red room loot vault (CE {@code generateRoom}). Drops cobblestone.
 */
public class BlockKeyhole extends Block {

    public BlockKeyhole(BlockBehaviour.Properties properties) {
        super(properties);
    }

    public ItemStack getCloneItemStack(Level level, BlockPos pos, BlockState state) {
        return new ItemStack(Blocks.STONE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return InteractionResult.PASS;

        Direction facing = hitResult.getDirection();
        if (facing == Direction.UP || facing == Direction.DOWN) return InteractionResult.PASS;

        boolean cracked = held.is(CouplingToolItems.KEY_RED_CRACKED.get());
        if (!held.is(CouplingToolItems.KEY_RED.get()) && !cracked) return InteractionResult.PASS;

        if (cracked && !level.isClientSide) held.shrink(1);
        if (level.isClientSide) return InteractionResult.SUCCESS;

        Direction dir = facing.getOpposite();
        BlockPos roomOrigin = pos.relative(dir, -4).below(2);
        generateRoom(level, roomOrigin);

        BlockPos doorPos = pos.below();
        level.setBlock(doorPos, ModBlocks.DOOR_RED.get().defaultBlockState(), 3);

        level.playSound(null, pos, HBMSoundHandler.lockOpen.get(), SoundSource.BLOCKS, 1.0F, 1.0F);

        return InteractionResult.SUCCESS;
    }

    /**
     * CE {@code BlockKeyhole.generateRoom} (SHA {@code 293649fc:202-268}) — 9×9×5 red room with brick_red walls,
     * torches, random decor (webs/pillars/fire/circle/lava), 1 in 20 chance spawns NCRPA/Trenchmaster armor
     * (5% via deco_loot) or pedestal loot (95% via ItemPoolsRedRoom.POOL_RED_PEDESTAL).
     */
    protected void generateRoom(Level level, BlockPos origin) {
        int size = 9;
        int height = 5;
        int width = size / 2;
        RandomSource rand = level.random;

        for (int x = -width; x <= width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = -width; z <= width; z++) {
                    BlockPos iPos = origin.offset(x, y, z);
                    if ((x == -width || x == width || z == -width || z == width || y == 0 || y == height - 1)
                            && level.getBlockState(iPos).isSolid()) {
                        level.setBlock(iPos, ModBlocks.BRICK_OBSIDIAN.get().defaultBlockState(), 2);
                    } else {
                        level.setBlock(iPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        int randLoot = rand.nextInt(20);
        BlockPos centerPos = origin.offset(0, 1, 0);

        if (randLoot == 0) {
            level.setBlock(centerPos, GenericCrateBlocks.decoLoot().get().defaultBlockState(), 2);
            if (level.getBlockEntity(centerPos) instanceof BlockLoot.LootBlockEntity loot) {
                if (rand.nextInt(5) == 0) {
                    // CE: 5% NCRPA full set (helmet/plate/legs/boots)
                    loot.addItem(new ItemStack(com.hbm.items.armor.PoweredArmorItems.NCRPA_HELMET.get()), 0, 0, 0);
                    loot.addItem(new ItemStack(com.hbm.items.armor.PoweredArmorItems.NCRPA_PLATE.get()), 0, 0, 0);
                    loot.addItem(new ItemStack(com.hbm.items.armor.PoweredArmorItems.NCRPA_LEGS.get()), 0, 0, 0);
                    loot.addItem(new ItemStack(com.hbm.items.armor.PoweredArmorItems.NCRPA_BOOTS.get()), 0, 0, 0);
                } else {
                    // CE: 95% (of the 5%) Trenchmaster full set
                    loot.addItem(new ItemStack(com.hbm.items.armor.PoweredArmorItems.TRENCHMASTER_HELMET.get()), 0, 0, 0);
                    loot.addItem(new ItemStack(com.hbm.items.armor.PoweredArmorItems.TRENCHMASTER_PLATE.get()), 0, 0, 0);
                    loot.addItem(new ItemStack(com.hbm.items.armor.PoweredArmorItems.TRENCHMASTER_LEGS.get()), 0, 0, 0);
                    loot.addItem(new ItemStack(com.hbm.items.armor.PoweredArmorItems.TRENCHMASTER_BOOTS.get()), 0, 0, 0);
                }
            }
        } else {
            spawnPedestalItem(level, centerPos);
        }
    }

    /**
     * CE {@code BlockKeyhole.spawnPedestalItem} (SHA {@code 293649fc:257-268}) — place pedestal at (x,y,z)
     * with random item from ItemPoolsRedRoom.POOL_RED_PEDESTAL.
     */
    protected void spawnPedestalItem(Level level, BlockPos pos) {
        level.setBlock(pos, GenericCrateBlocks.pedestal().get().defaultBlockState(), 2);
        if (level.getBlockEntity(pos) instanceof BlockPedestal.PedestalBlockEntity pedestal) {
            com.hbm.itempool.ItemPool pool = com.hbm.itempool.ItemPool.getPool(
                    com.hbm.itempool.ItemPoolsRedRoom.POOL_RED_PEDESTAL);
            ItemStack lootItem = com.hbm.itempool.ItemPool.getStack(pool, level.random);
            if (!lootItem.isEmpty()) {
                pedestal.item = lootItem.copy();
                pedestal.setChanged();
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            }
        }
    }
}
