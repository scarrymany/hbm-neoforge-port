package com.hbm.render.hud;

import com.hbm.blockentity.machine.rbmk.RBMKBaseBlockEntity;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.machine.rbmk.RBMKBaseBlock;
import com.hbm.config.ClientConfig;
import com.hbm.main.MainRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CE {@code ModEventHandlerClient.onOverlayRender} CROSSHAIRS + {@code DODD_RBMK_DIAGNOSTIC}
 * ({@code ModEventHandlerClient.java:839-865}). Held {@link ILookOverlay} item first, else the
 * looked-at block. {@code RenderGuiEvent.Pre} once per frame — {@code printHook} already takes Pre.
 * {@code diagnosticPrintHook} lives here (not on {@link RBMKBaseBlockEntity}) so dedicated
 * {@code runServer} does not classload {@code Minecraft}.
 * TODO(CE: EntityRailCarBase.java:780): entity hit — CE dispatcher is BLOCK-only.
 */
@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)
public final class LookOverlayDispatcher {

    private LookOverlayDispatcher() {
    }

    private static final String DODD_TITLE = "Dump of Ordered Data Diagnostic (DODD)";
    private static final List<String> DODD_EXCEPTIONS = Arrays.asList(
            "x", "y", "z", "items", "id", "muffled", "ForgeCaps");
    private static final List<String> CACHED_DODD_LINES = new ArrayList<>();
    private static long lastDODDUpdate;
    private static BlockPos lastDODDPos;

    @SubscribeEvent
    public static void onRenderGuiPre(RenderGuiEvent.Pre event) {
        if (!ClientConfig.DODD_RBMK_DIAGNOSTIC.get()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level world = mc.level;
        if (player == null || world == null) return;

        HitResult mop = mc.hitResult;
        if (mop != null && mop.getType() == HitResult.Type.BLOCK) {
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

        diagnosticPrintHook(event);
    }

    /**
     * CE {@code TileEntityRBMKBase.diagnosticPrintHook} ({@code TileEntityRBMKBase.java:345-400}).
     * Client-only: common TE must not import {@code Minecraft}.
     */
    public static void diagnosticPrintHook(RenderGuiEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        Level world = mc.level;
        HitResult mop = mc.hitResult;
        if (world == null || mop == null || mop.getType() != HitResult.Type.BLOCK) return;

        BlockPos mopPos = ((BlockHitResult) mop).getBlockPos();
        Block block = world.getBlockState(mopPos).getBlock();
        if (!(block instanceof RBMKBaseBlock rbmk)) return;

        BlockPos currentPos = rbmk.findCore(world, mopPos);
        if (currentPos == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastDODDUpdate > 50 || !currentPos.equals(lastDODDPos)) {
            lastDODDUpdate = currentTime;
            lastDODDPos = currentPos;

            BlockEntity raw = world.getBlockEntity(currentPos);
            if (!(raw instanceof RBMKBaseBlockEntity te)) return;

            CompoundTag flush = new CompoundTag();
            te.getDiagData(flush, world.registryAccess());
            String[] ents = flush.getAllKeys().toArray(new String[0]);
            Arrays.sort(ents);

            CACHED_DODD_LINES.clear();
            for (String key : ents) {
                if (DODD_EXCEPTIONS.contains(key)) continue;
                Tag tag = flush.get(key);
                CACHED_DODD_LINES.add(key + ": " + (tag == null ? "null" : tag));
            }
        }

        int pX = mc.getWindow().getGuiScaledWidth() / 2 + 8;
        int pZ = mc.getWindow().getGuiScaledHeight() / 2;
        Font font = mc.font;
        String name = rbmk.getName().getString();

        event.getGuiGraphics().drawString(font, DODD_TITLE, pX + 1, pZ - 19, 0x006000, false);
        event.getGuiGraphics().drawString(font, DODD_TITLE, pX, pZ - 20, 0x00FF00, false);
        event.getGuiGraphics().drawString(font, name, pX + 1, pZ - 9, 0x606000, false);
        event.getGuiGraphics().drawString(font, name, pX, pZ - 10, 0xFFFF00, false);

        int listPz = pZ;
        for (String line : CACHED_DODD_LINES) {
            event.getGuiGraphics().drawString(font, line, pX, listPz, 0xFFFFFF, false);
            listPz += 10;
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
