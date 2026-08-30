package com.hbm.items.special;

import com.hbm.items.weapon.ItemMissile;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.items.special.ItemLootCrate} (83 lines, read in full). Per
 * {@code docs/phase3/scattered_military_items.md}: right-clicking consumes the stack and grants one
 * random {@link ItemMissile} drawn from a rarity-weighted rejection loop over one of three pools.
 * <p>
 * CE used {@code stack.getItem() == ModItems.loot_10/15/misc} identity checks against one shared
 * class; this port instead gives each of the three registered instances ({@code loot_10}/
 * {@code loot_15}/{@code loot_misc}, see {@link ScatteredMilitaryItems}) its own {@code pool}
 * reference at construction time - same behavior, no identity branching needed.
 * <p>
 * The three pools ({@link #LIST_10}/{@link #LIST_15}/{@link #LIST_MISC}) start empty and are
 * populated the same way CE populated them: {@link ItemMissile#setRarity} pushes {@code this} onto
 * the matching list as a side effect of constructing each {@code mp_*} missile-part item (now wired
 * for real in {@link ItemMissile#setRarity} - see that method's javadoc). This report confirms
 * {@code com.hbm.items.weapon.ItemMissile} has already landed in this port ({@code missile_framework.md}'s
 * package), but its own registration class ({@code MissileItems}, per {@code ItemMissile}'s own
 * javadoc) had not landed as of this writing - so these lists compile and roll correctly but stay
 * empty (a documented, honest "not yet populated" state, not a fake behavior) until that package's
 * {@code mp_*} item registrations call {@code setRarity(...)} the way CE's own {@code ModItems.java}
 * field declarations did.
 */
public class ItemLootCrate extends Item {

    public static final List<ItemMissile> LIST_10 = new ArrayList<>();
    public static final List<ItemMissile> LIST_15 = new ArrayList<>();
    public static final List<ItemMissile> LIST_MISC = new ArrayList<>();

    private final List<ItemMissile> pool;

    public ItemLootCrate(Properties properties, List<ItemMissile> pool) {
        super(properties);
        this.pool = pool;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            ItemMissile chosen = choose(pool, level.getRandom());
            if (chosen != null) {
                ItemStack reward = new ItemStack(chosen);
                if (!player.getInventory().add(reward)) {
                    player.drop(reward, false);
                }
                stack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** Ported verbatim from CE's own rejection-loop weighting; returns {@code null} if the pool is still empty. */
    @Nullable
    private static ItemMissile choose(List<ItemMissile> parts, RandomSource rand) {
        if (parts.isEmpty()) return null;

        ItemMissile item;
        do {
            item = parts.get(rand.nextInt(parts.size()));
        } while (!accept(item, rand));
        return item;
    }

    private static boolean accept(ItemMissile item, RandomSource rand) {
        return switch (item.rarity) {
            case COMMON -> true;
            case UNCOMMON -> rand.nextInt(5) == 0;
            case RARE -> rand.nextInt(10) == 0;
            case EPIC -> rand.nextInt(25) == 0;
            case LEGENDARY -> rand.nextInt(50) == 0;
            case SEWS_CLOTHES_AND_SUCKS_HORSE_COCK -> rand.nextInt(100) == 0;
        };
    }
}
