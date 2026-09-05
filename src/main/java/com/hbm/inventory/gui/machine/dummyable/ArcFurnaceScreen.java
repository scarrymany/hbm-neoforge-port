package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineArcFurnaceBlockEntity;
import com.hbm.inventory.container.machine.dummyable.ArcFurnaceMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.material.Mats;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.NBTControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Exact CE {@code GUIMachineArcFurnaceLarge} on existing {@code gui_arc_furnace.png} 176×256.
 * Power 8,{@code 106-p} from 176; progress 17,{@code 106-o}; molten 152,106 from 208;
 * liquid click 151,17. Invented {@code Button} + fill-style tooltips removed.
 */
public class ArcFurnaceScreen extends GuiInfoContainer<ArcFurnaceMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_arc_furnace.png");

    public ArcFurnaceScreen(ArcFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 256;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineArcFurnaceBlockEntity be = this.getMenu().be;
        // CE GUIMachineArcFurnaceLarge.java:77-86
        if (be.liquidMode) {
            guiGraphics.blit(TEXTURE, x + 151, y + 17, 190, 18, 18, 18);
        }
        if (be.isProgressing) {
            guiGraphics.blit(TEXTURE, x + 7, y + 17, 190, 0, 18, 18);
        }
        int p = (int) (be.getPower() * 70 / Math.max(1L, be.getMaxPower()));
        if (p > 0) {
            guiGraphics.blit(TEXTURE, x + 8, y + 106 - p, 176, 70 - p, 7, p);
        }
        int o = (int) (be.progress * 70);
        if (o > 0) {
            guiGraphics.blit(TEXTURE, x + 17, y + 106 - o, 183, 70 - o, 7, o);
        }
        drawStack(guiGraphics, be.liquids, MachineArcFurnaceBlockEntity.MAX_LIQUID, x + 152, y + 106);
    }

    /** Exact CE {@code GUIMachineArcFurnaceLarge.drawStack} :98-129. */
    private void drawStack(GuiGraphics guiGraphics, List<Mats.MaterialStack> stack, int capacity, int x, int bottomY) {
        if (stack.isEmpty() || capacity <= 0) return;

        int lastHeight = 0;
        int lastQuant = 0;
        for (Mats.MaterialStack sta : stack) {
            int targetHeight = (lastQuant + sta.amount) * 70 / capacity;
            if (lastHeight == targetHeight) continue;

            int hex = sta.material.moltenColor;
            float r = ((hex >> 16) & 0xFF) / 255F;
            float g = ((hex >> 8) & 0xFF) / 255F;
            float b = (hex & 0xFF) / 255F;
            int h = targetHeight - lastHeight;
            int blitY = bottomY - targetHeight;
            int v = 70 - targetHeight;

            guiGraphics.setColor(r, g, b, 1F);
            guiGraphics.blit(TEXTURE, x, blitY, 208, v, 16, h);
            RenderSystem.enableBlend();
            guiGraphics.setColor(1F, 1F, 1F, 0.3F);
            guiGraphics.blit(TEXTURE, x, blitY, 208, v, 16, h);
            RenderSystem.disableBlend();

            lastQuant += sta.amount;
            lastHeight = targetHeight;
        }
        guiGraphics.setColor(1F, 1F, 1F, 1F);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :66 — title white, centered
        String name = this.title.getString();
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0xffffff, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineArcFurnaceBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 36, 7, 70, be.getPower(), be.getMaxPower());

        List<Component> melt = new ArrayList<>();
        if (be.liquids.isEmpty()) {
            melt.add(Component.literal("Empty").withStyle(ChatFormatting.RED));
        } else {
            for (Mats.MaterialStack sta : be.liquids) {
                melt.add(Component.empty()
                        .append(sta.material.getName())
                        .append(": ")
                        .append(Mats.formatAmount(sta.amount, Screen.hasShiftDown()))
                        .withStyle(ChatFormatting.YELLOW));
            }
        }
        // CE :95 — +16 so JEI recipe tooltip does not overlap
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 152, topPos + 36, 16, 70, mouseX, mouseY + 16, melt);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // CE GUIMachineArcFurnaceLarge.java:54-60
        if (isHovered(mouseX, mouseY, 151, 17, 18, 18)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("liquid", true);
            PacketDistributor.sendToServer(new NBTControlPacket(this.getMenu().be.getBlockPos(), data));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
