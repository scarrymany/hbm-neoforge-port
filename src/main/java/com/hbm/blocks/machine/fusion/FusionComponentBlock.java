package com.hbm.blocks.machine.fusion;

import com.hbm.api.block.IToolable;
import com.hbm.blocks.BlockBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Flattened CE {@code BlockFusionComponent} meta 0. TORCH + {@code steel_plate_triple} → {@code fusion_component_1}.
 * CE {@code NTMToolHandler.java:56}.
 */
public class FusionComponentBlock extends BlockBase implements IToolable {

    public FusionComponentBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onScrew(Level world, Player player, int x, int y, int z, Direction side,
                           float fX, float fY, float fZ, InteractionHand hand, ToolType tool) {
        if (world.isClientSide) return false;
        if (tool != ToolType.TORCH) return false;
        Block welded = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("hbm:fusion_component_1"));
        if (welded == null) return false;
        ItemStack cost = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("hbm:steel_plate_triple")));
        if (cost.isEmpty()) return false;
        if (!player.getAbilities().instabuild) {
            int slot = find(player, cost);
            if (slot < 0) return false;
            player.getInventory().removeItem(slot, 1);
        }
        world.setBlock(new BlockPos(x, y, z), welded.defaultBlockState(), 3);
        return true;
    }

    private static int find(Player player, ItemStack cost) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(cost.getItem())) return i;
        }
        return -1;
    }
}
