package com.hbm.items.machine;

import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ItemBase;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Right-click "loot box" that rolls one of {@link GenericRecipes#blueprintPools} matching this
 * folder's {@link Kind} and hands back an {@link ItemBlueprints} stack for it. Three CE metadata
 * variants (base/discover/secret) become three registered instances of this class. The 1.12
 * IModel baking CE used to swap textures per metadata value is dropped - modern item models are
 * data-driven JSON, not baked at runtime.
 */
public class ItemBlueprintFolder extends ItemBase {

    private final Kind kind;

    public ItemBlueprintFolder(Kind kind, Properties properties) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResultHolder.pass(stack);

        List<String> pools = new ArrayList<>();
        for (String pool : GenericRecipes.blueprintPools.keySet()) {
            if (pool.startsWith(this.kind.poolPrefix)) pools.add(pool);
        }

        if (!pools.isEmpty()) {
            stack.shrink(1);
            String chosen = pools.get(level.getRandom().nextInt(pools.size()));
            return InteractionResultHolder.success(ItemBlueprints.make(MachineItems.BLUEPRINTS, chosen));
        }

        return InteractionResultHolder.pass(stack);
    }

    public enum Kind {
        BASE(GenericRecipes.POOL_PREFIX_ALT),
        DISCOVER(GenericRecipes.POOL_PREFIX_DISCOVER),
        SECRET(GenericRecipes.POOL_PREFIX_SECRET);

        public final String poolPrefix;

        Kind(String poolPrefix) {
            this.poolPrefix = poolPrefix;
        }
    }
}
