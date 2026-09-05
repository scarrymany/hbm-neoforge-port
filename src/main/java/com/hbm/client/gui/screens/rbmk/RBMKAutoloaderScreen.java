package com.hbm.client.gui.screens.rbmk;

import com.hbm.inventory.container.machine.rbmk.RBMKAutoloaderMenu;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.NBTControlPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

/**
 * NeoForge port of CE {@code GUIRBMKAutoloader}.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/GUIRBMKAutoloader.java
 * <p>
 * GUI with inventory, cycle percentage display, and plus/minus buttons.
 */
public class RBMKAutoloaderScreen extends AbstractContainerScreen<RBMKAutoloaderMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/rbmk/gui_rbmk_autoloader.png");

    public RBMKAutoloaderScreen(RBMKAutoloaderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 182;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // CE GUIRBMKAutoloader.java:69-73: simple background blit, no overlays
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // CE GUIRBMKAutoloader.java:59-65: title centered, inventory label, cycle percentage
        graphics.drawString(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 6, 0xFFFFFF, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);

        // Cycle percentage centered at y=23 (CE :64-65)
        String percent = menu.be.cycle + "%";
        graphics.drawString(this.font, percent, (this.imageWidth - this.font.width(percent)) / 2, 23, 0x00FF00, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // CE GUIRBMKAutoloader.java:39-55: minus button (x=74, y=36, 12x12) and plus button (x=90, y=36, 12x12)
        if (mouseX >= leftPos + 74 && mouseX < leftPos + 74 + 12 && mouseY >= topPos + 36 && mouseY < topPos + 36 + 12) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("minus", true);
            PacketDistributor.sendToServer(new NBTControlPacket(menu.be.getBlockPos(), data));
            return true;
        }
        if (mouseX >= leftPos + 90 && mouseX < leftPos + 90 + 12 && mouseY >= topPos + 36 && mouseY < topPos + 36 + 12) {
            CompoundTag data = new CompoundTag();
            data.putBoolean("plus", true);
            PacketDistributor.sendToServer(new NBTControlPacket(menu.be.getBlockPos(), data));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
