package com.hbm.client.gui.screens.rbmk;

import com.hbm.inventory.container.machine.rbmk.RBMKRodMenu;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * NeoForge port of CE {@code GUIRBMKRod}.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/GUIRBMKRod.java
 * <p>
 * GUI with fuel depletion overlay, xenon bar, and temperature warning icons.
 */
public class RBMKRodScreen extends AbstractContainerScreen<RBMKRodMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/rbmk/gui_rbmk_element.png");

    public RBMKRodScreen(RBMKRodMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        ItemStack rodStack = menu.be.inventory.getStackInSlot(0);

        // CE GUIRBMKRod.java:54-63: fuel rod overlays (depletion + xenon)
        if (rodStack.getItem() instanceof ItemRBMKRod) {
            // Fuel rod outline (18x67px) at x=34, y=21
            graphics.blit(TEXTURE, leftPos + 34, topPos + 21, 176, 0, 18, 67);

            // Depletion overlay (18x67px) at x=34, y=21
            double depletion = 1D - ItemRBMKRod.getEnrichment(rodStack);
            int d = (int) (depletion * 67);
            graphics.blit(TEXTURE, leftPos + 34, topPos + 21, 194, 0, 18, d);

            // Xenon bar (14x58px) at x=126, y=82 (bottom-up fill)
            double xenon = ItemRBMKRod.getPoisonLevel(rodStack);
            int x = (int) (xenon * 58);
            if (x > 0) {
                graphics.blit(TEXTURE, leftPos + 126, topPos + 82 - x, 212, 58 - x, 14, x);
            }
        }

        // CE GUIRBMKRod.java:65-66: temperature warning icons (16x16px)
        if (!menu.be.coldEnoughForAutoloader()) {
            // Warning icon 6 at x=-16, y=20
            graphics.blit(TEXTURE, leftPos - 16, topPos + 20, 226, 0, 16, 16);
        }
        if (!menu.be.coldEnoughForManual()) {
            // Warning icon 7 at x=-16, y=36
            graphics.blit(TEXTURE, leftPos - 16, topPos + 36, 242, 0, 16, 16);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // CE GUIRBMKRod.java:29-32: title centered + inventory label
        graphics.drawString(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // CE GUIRBMKRod.java:41-44: temperature warning tooltips
        if (!menu.be.coldEnoughForAutoloader()) {
            if (mouseX >= leftPos - 16 && mouseX < leftPos && mouseY >= topPos + 20 && mouseY < topPos + 36) {
                graphics.renderTooltip(this.font, Component.literal("Fuel skin temperature has exceeded 1,000°C, autoloaders can no longer cycle fuel!"), mouseX, mouseY);
            }
        }
        if (!menu.be.coldEnoughForManual()) {
            if (mouseX >= leftPos - 16 && mouseX < leftPos && mouseY >= topPos + 36 && mouseY < topPos + 52) {
                graphics.renderTooltip(this.font, Component.literal("Fuel skin temperature has exceeded 200°C, fuel can no longer be removed by hand!"), mouseX, mouseY);
            }
        }
    }
}
