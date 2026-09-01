package com.hbm.packet.toserver;

import com.hbm.inventory.container.AnvilMenu;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe;
import com.hbm.main.MainRegistry;
import com.hbm.util.InventoryUtil;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** CE {@code AnvilCraftPacket.java}:13-73. Construction craft from player inventory. */
public record AnvilCraftPacket(int recipeIndex, int mode) implements CustomPacketPayload {

    public static final Type<AnvilCraftPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "anvil_craft"));

    public static final StreamCodec<ByteBuf, AnvilCraftPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AnvilCraftPacket decode(ByteBuf buf) {
            return new AnvilCraftPacket(buf.readInt(), buf.readInt());
        }

        @Override
        public void encode(ByteBuf buf, AnvilCraftPacket packet) {
            buf.writeInt(packet.recipeIndex);
            buf.writeInt(packet.mode);
        }
    };

    public static void handleServer(AnvilCraftPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.recipeIndex < 0 || packet.recipeIndex >= AnvilRecipes.getConstruction().size()) return;
            Player player = context.player();
            if (!(player.containerMenu instanceof AnvilMenu anvil)) return;
            AnvilConstructionRecipe recipe = AnvilRecipes.getConstruction().get(packet.recipeIndex);
            if (!recipe.isTierValid(anvil.tier)) return;
            int count = packet.mode == 1 ? 64 : 1;
            for (int i = 0; i < count; i++) {
                if (InventoryUtil.doesPlayerHaveAStacks(player, recipe.input, true)) {
                    InventoryUtil.giveChanceStacksToPlayer(player, recipe.output);
                } else {
                    break;
                }
            }
            player.containerMenu.broadcastChanges();
        });
    }

    @Override
    public Type<AnvilCraftPacket> type() {
        return TYPE;
    }
}
