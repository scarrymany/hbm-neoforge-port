package com.hbm.render.hud;

import com.hbm.blocks.ILookOverlay;
import com.hbm.config.ClientConfig;
import com.hbm.main.MainRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code ModEventHandlerClient.onOverlayRender} CROSSHAIRS + {@code DODD_RBMK_DIAGNOSTIC}
 * ({@code ModEventHandlerClient.java:839-865}). Held {@link ILookOverlay} item first, else the
 * looked-at block. {@code RenderGuiEvent.Pre} once per frame — {@code printHook} already takes Pre.
 * TODO(CE: ModEventHandlerClient.java:865): {@code TileEntityRBMKBase.diagnosticPrintHook}.
 * TODO(CE: EntityRailCarBase.java:780): entity hit — CE dispatcher is BLOCK-only.
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public final class LookOverlayDispatcher {

    private LookOverlayDispatcher() {
    }

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        if (!ClientConfig.DODD_RBMK_DIAGNOSTIC.get()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level world = mc.level;
        if (player == null || world == null) return;

        HitResult mop = mc.hitResult;
        if (mop == null || mop.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = ((BlockHitResult) mop).getBlockPos();
        BlockState stateHit = world.getBlockState(pos);
        Block blockHit = stateHit.getBlock();
        ItemStack held = player.getMainHandItem();

        if (held.getItem() instanceof ILookOverlay overlay) {
            overlay.printHook(event, world, pos);
        } else if (blockHit instanceof ILookOverlay overlay) {
            overlay.printHook(event, world, pos);
        }

        if (ClientConfig.SHOW_BLOCK_META_OVERLAY.get()) {
            List<Component> text = new ArrayList<>();
            text.add(Component.literal(blockHit.getDescriptionId()));
            text.add(Component.literal("Meta: " + metaOf(stateHit)));
            ILookOverlay.printGeneric(event, Component.literal("DEBUG"), 0xffff00, 0x4040000, text);
        }
    }

    private static String metaOf(BlockState state) {
        for (Property<?> prop : state.getProperties()) {
            if ("meta".equals(prop.getName())) {
                return String.valueOf(state.getValue(prop));
            }
        }
        return "0";
    }
}
