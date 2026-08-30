package com.hbm.blocks.generic;

import com.hbm.blocks.ICustomBlockModelRegister;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;

import java.util.List;

/**
 * Ported from CE's {@code WasteLog extends BlockRotatedPillar}: contaminated log reskins
 * ({@code waste_log}/{@code frozen_log}). CE identity-checked {@code this} to pick between a coal
 * drop (with a rare burnt-bark bonus) for {@code waste_log} and a snowball drop for
 * {@code frozen_log}; this port makes that an explicit {@link Kind} instead. CE's
 * {@code com.hbm.items.ModItems.burnt_bark} rare-drop bonus is dropped for now (no such item
 * exists yet in this port - an items-area concern), leaving the plain coal-drop behavior.
 */
public class WasteLog extends RotatedPillarBlock implements ICustomBlockModelRegister {

    public enum Kind {
        WASTE, FROZEN
    }

    public final Kind kind;

    public WasteLog(Properties properties) {
        this(properties, Kind.WASTE);
    }

    public WasteLog(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (kind == Kind.FROZEN) {
            return List.of(new ItemStack(Items.SNOWBALL));
        }
        return List.of(new ItemStack(Items.COAL));
    }

    /** {@code waste_log}/{@code frozen_log} each have real {@code _side}/{@code _top} textures. */
    @Override
    public void registerModel(BlockStateProvider provider, ResourceLocation modelLocation) {
        String name = modelLocation.getPath();
        ResourceLocation side = provider.modLoc("block/" + name + "_side");
        ResourceLocation top = provider.modLoc("block/" + name + "_top");

        provider.simpleBlockWithItem(this, provider.models().cubeColumn(name, side, top));
    }
}
