package com.hbm.blocks.generic;

import com.hbm.lib.HBMSoundHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Falling loot crate, ported from CE's {@code BlockCrate}. CE registers five distinct blocks
 * ({@code crate}, {@code crate_weapon}, {@code crate_lead}, {@code crate_metal}, {@code crate_red})
 * sharing one class and dispatching to five separate loot pools via {@code this ==
 * ModBlocks.crate_x} identity checks; the port flattens that into an explicit {@link Type} passed
 * to the constructor, one instance per CE registry entry, each with its own mutable weighted pool
 * populated through {@link #addLoot(Type, ItemStack, int)}.
 * <p>
 * <b>Known gap:</b> CE's real pools (see {@code upstream/hbm-ce/.../BlockCrate.java#setDrops}) draw
 * from roughly forty items owned by other, not-yet-ported Phase 1 areas (weapon/grenade/ammo items,
 * machine blocks, fuel-cell items). Registering those references here would mean guessing field
 * names this area has no way to verify, which the porting ground rules forbid. The pools are wired
 * up empty; whichever area finishes registering those items should call {@link #addLoot} to
 * populate them (see the port's known-issues notes for the exact CE source list to carry over).
 */
public class BlockCrate extends FallingBlock {

    /**
     * CE gates crate-breaking on holding {@code ModItems.crowbar}, which does not exist in the
     * items catalog yet (owned by the tools area, not yet registered as of this port pass). Gating
     * on a tag instead of a concrete item reference lets that area add the real crowbar item to
     * this tag via datagen once it lands, without this class guessing its field name.
     */
    public static final TagKey<Item> CROWBAR_TAG = TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hbm.main.MainRegistry.MODID, "crowbars"));

    public enum Type { STANDARD, WEAPON, LEAD, METAL, RED }

    private static final java.util.Map<Type, List<WeightedEntry>> POOLS = new java.util.EnumMap<>(Type.class);

    private final Type type;

    public BlockCrate(Properties properties, Type type) {
        super(properties);
        this.type = type;
    }

    /** Registers one more weighted entry into {@code type}'s loot pool. */
    public static void addLoot(Type type, ItemStack stack, int weight) {
        POOLS.computeIfAbsent(type, t -> new ArrayList<>()).add(new WeightedEntry(stack, weight));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(CROWBAR_TAG)) {
            if (level.isClientSide) {
                player.displayClientMessage(Component.translatable("chat.crate.needcrowbar"), true);
            }
            return InteractionResult.SUCCESS;
        }

        if (!level.isClientSide) {
            dropItems(level, pos);
            level.removeBlock(pos, false);
            level.playSound(null, pos, HBMSoundHandler.crateBreak.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    private void dropItems(Level level, BlockPos pos) {
        List<WeightedEntry> pool = POOLS.get(type);
        if (pool == null || pool.isEmpty()) {
            return;
        }

        RandomSource random = level.getRandom();
        List<ItemStack> toDrop = new ArrayList<>();

        if (type == Type.RED) {
            pool.forEach(entry -> toDrop.add(entry.stack.copy()));
        } else {
            int count = (type == Type.WEAPON)
                    ? (random.nextInt(100) == 34 ? 25 : 1 + random.nextInt(2))
                    : 3 + random.nextInt(3);
            for (int i = 0; i < count; i++) {
                toDrop.add(pickWeighted(pool, random).stack.copy());
            }
        }

        for (ItemStack stack : toDrop) {
            double x = pos.getX() + random.nextFloat() * 0.8 + 0.1;
            double y = pos.getY() + random.nextFloat() * 0.8 + 0.1;
            double z = pos.getZ() + random.nextFloat() * 0.8 + 0.1;
            ItemEntity entity = new ItemEntity(level, x, y, z, stack);
            entity.setDeltaMovement(random.nextGaussian() * 0.05, random.nextGaussian() * 0.05 + 0.2, random.nextGaussian() * 0.05);
            level.addFreshEntity(entity);
        }
    }

    private static WeightedEntry pickWeighted(List<WeightedEntry> pool, RandomSource random) {
        int totalWeight = pool.stream().mapToInt(e -> e.weight).sum();
        int roll = random.nextInt(Math.max(1, totalWeight));
        for (WeightedEntry entry : pool) {
            roll -= entry.weight;
            if (roll < 0) {
                return entry;
            }
        }
        return pool.get(pool.size() - 1);
    }

    private record WeightedEntry(ItemStack stack, int weight) {
    }
}
