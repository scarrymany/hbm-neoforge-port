package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blockentity.machine.dummyable.VendingMachineBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPoolsVendingMachine;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * CE {@code BlockVendingMachine} — Dummyable {1,0,0,0,0,0}. No container. Token → pool drop.
 * Dispense sound Exact CE {@code BlockVendingMachine.java:80} ({@code boltOpen} 1.0F/0.75F BLOCKS).
 */
public class VendingMachineBlock extends BlockDummyable {

    public VendingMachineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12
                ? new VendingMachineBlockEntity(DummyableProcessBlockEntities.VENDING_MACHINE.get(), pos, state)
                : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        Item token = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "coin_token"));
        if (token == Items.AIR || stack.isEmpty() || stack.getItem() != token) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        BlockPos core = findCore(level, pos);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (core != null) {
            boolean snacks = level.getBlockState(core.above()).is(this)
                    && level.getBlockState(core.above()).getValue(META) >= extra;
            ItemStack drop = ItemPool.getStack(
                    ItemPool.getPool(snacks ? ItemPoolsVendingMachine.POOL_SNACKS : ItemPoolsVendingMachine.POOL_SODA),
                    level.random);
            if (!drop.isEmpty()) {
                int meta = level.getBlockState(core).getValue(META);
                Direction dir = Direction.from3DDataValue(Math.max(0, meta - offset) % 6);
                ItemEntity item = new ItemEntity(level,
                        pos.getX() + 0.5 + dir.getStepX() * 0.75,
                        core.getY() + 0.25,
                        pos.getZ() + 0.5 + dir.getStepZ() * 0.75,
                        drop);
                level.addFreshEntity(item);
            }
            // Exact CE BlockVendingMachine.java:80
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    HBMSoundHandler.boltOpen.get(), SoundSource.BLOCKS, 1.0F, 0.75F);
        }
        return ItemInteractionResult.CONSUME;
    }
}
