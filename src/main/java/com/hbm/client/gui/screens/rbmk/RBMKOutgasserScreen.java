package com.hbm.client.gui.screens.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKOutgasserBlockEntity;
import com.hbm.inventory.container.machine.rbmk.RBMKOutgasserMenu;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * NeoForge port of CE {@code GUIRBMKOutgasser}.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/GUIRBMKOutgasser.java
 * <p>
 * Simple GUI: item slot, flux progress bar, gas tank overlay.
 */
public class RBMKOutgasserScreen extends AbstractContainerScreen<RBMKOutgasserMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/rbmk/gui_rbmk_outgasser.png");

    public RBMKOutgasserScreen(RBMKOutgasserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        RBMKOutgasserBlockEntity be = menu.be;

        // CE GUIRBMKOutgasser.java:68-69 = flux progress bar (45px wide)
        int progress = be.duration > 0 ? (int) (be.progress * 45 / be.duration) : 0;
        graphics.blit(TEXTURE, leftPos + 66, topPos + 58, 190, 0, progress, 6);

        // CE GUIRBMKOutgasser.java:71-72 = gas tank (14x58 px, filled from bottom up)
        int gas = be.gas.getMaxFill() > 0 ? (int) (be.gas.getFill() * 58 / be.gas.getMaxFill()) : 0;
        graphics.blit(TEXTURE, leftPos + 143, topPos + 82 - gas, 176, 58 - gas, 14, gas);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // CE GUIRBMKOutgasser.java:38-39: title centered
        Component name = this.menu.be.getDisplayName();
        graphics.drawString(this.font, name, (this.imageWidth - this.font.width(name)) / 2, 6, 0x404040, false);

        // CE GUIRBMKOutgasser.java:40: inventory label
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);

        // CE GUIRBMKOutgasser.java:41-44: "Flux" label + progress/duration numbers
        graphics.drawString(this.font, "Flux", 21, 34, 0x404040, false);
        String fluxNumbers = formatNumber((float) menu.be.progress) + "/" + formatNumber((float) menu.be.duration);
        graphics.drawString(this.font, fluxNumbers, 123 - this.font.width(fluxNumbers), 34, 0x46EA00, false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // CE GUIRBMKOutgasser.java:30: gas tank tooltip
        RBMKOutgasserBlockEntity be = menu.be;
        if (mouseX >= leftPos + 143 && mouseX < leftPos + 143 + 14 && mouseY >= topPos + 23 && mouseY < topPos + 23 + 58) {
            graphics.renderTooltip(this.font, Component.literal(be.gas.getFill() + " / " + be.gas.getMaxFill() + " mB"), mouseX, mouseY);
        }
    }

    /**
     * CE GUIRBMKOutgasser.java:47-58: format flux numbers with k/M/G/T suffixes.
     */
    protected String formatNumber(float number) {
        if (number < 1000D) return String.format("%5.1f ", number);
        if (number < 1000000D) return String.format("%5.1fk", number / 1000F);
        if (number < 1000000000D) return String.format("%5.1fM", number / 1000000F);
        if (number < 1000000000000D) return String.format("%5.1fG", number / 1000000000F);
        if (number < 1000000000000000D) return String.format("%5.1fT", number / 1000000000000F);
        return "";
    }
}
