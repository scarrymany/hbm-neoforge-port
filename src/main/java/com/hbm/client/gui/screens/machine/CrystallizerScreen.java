package com.hbm.client.gui.screens.machine;

import com.hbm.blockentity.machine.MachineCrystallizerBlockEntity;
import com.hbm.inventory.container.machine.MachineCrystallizerMenu;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * NeoForge port of CE {@code GUICrystallizer}.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/GUICrystallizer.java
 * <p>
 * GUI with power bar, progress bar, tank overlay, and upgrade info.
 */
public class CrystallizerScreen extends AbstractContainerScreen<MachineCrystallizerMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_crystallizer_alt.png");

    public CrystallizerScreen(MachineCrystallizerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        MachineCrystallizerBlockEntity be = menu.be;

        // CE GUICrystallizer.java:56-57: power bar (16x52px) at x=152, y=70-i
        int powerFill = (int) be.getPowerScaled(52);
        if (powerFill > 0) {
            graphics.blit(TEXTURE, leftPos + 152, topPos + 70 - powerFill, 176, 64 - powerFill, 16, powerFill);
        }

        // CE GUICrystallizer.java:59-60: progress bar (28x12px) at x=80, y=47
        int progress = be.getProgressScaled(28);
        if (progress > 0) {
            graphics.blit(TEXTURE, leftPos + 80, topPos + 47, 176, 0, progress, 12);
        }

        // CE GUICrystallizer.java:62: upgrade info icon (8x8px) at x=117, y=22
        graphics.blit(TEXTURE, leftPos + 117, topPos + 22, 192, 0, 8, 8);

        // CE GUICrystallizer.java:64: tank overlay (16x52px) at x=35, y=70
        int tankFill = be.tank.getMaxFill() > 0 ? (int) (be.tank.getFill() * 52 / be.tank.getMaxFill()) : 0;
        if (tankFill > 0) {
            graphics.blit(TEXTURE, leftPos + 35, topPos + 70 - tankFill, 200, 52 - tankFill, 16, tankFill);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // CE GUICrystallizer.java:29-31: title centered + inventory label
        graphics.drawString(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // CE GUICrystallizer.java:38-44: tooltips for power, tank, and upgrade info
        MachineCrystallizerBlockEntity be = menu.be;
        if (mouseX >= leftPos + 152 && mouseX < leftPos + 152 + 16 && mouseY >= topPos + 17 && mouseY < topPos + 17 + 52) {
            graphics.renderTooltip(this.font, Component.literal(be.getPower() + " / " + MachineCrystallizerBlockEntity.MAX_POWER + " HE"), mouseX, mouseY);
        }
        if (mouseX >= leftPos + 35 && mouseX < leftPos + 35 + 16 && mouseY >= topPos + 18 && mouseY < topPos + 18 + 52) {
            graphics.renderTooltip(this.font, Component.literal(be.tank.getFill() + " / " + be.tank.getMaxFill() + " mB"), mouseX, mouseY);
        }
        if (mouseX >= leftPos + 117 && mouseX < leftPos + 117 + 8 && mouseY >= topPos + 22 && mouseY < topPos + 22 + 8) {
            graphics.renderTooltip(this.font, Component.literal("Acceptable upgrades:\n -Speed (stacks to level 3)\n -Effectiveness (stacks to level 3)\n -Overdrive (stacks to level 3)"), mouseX, mouseY);
        }
    }
}
