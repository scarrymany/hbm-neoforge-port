package com.hbm.client.gui.screens.machine;

import com.hbm.blockentity.machine.MachineShredderBlockEntity;
import com.hbm.inventory.container.machine.MachineShredderMenu;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * NeoForge port of CE {@code GUIMachineShredder}.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/GUIMachineShredder.java
 * <p>
 * GUI with power bar, progress bar, gear status icons, and error indicator.
 */
public class ShredderScreen extends AbstractContainerScreen<MachineShredderMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_shredder.png");

    public ShredderScreen(MachineShredderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 233;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        MachineShredderBlockEntity be = menu.be;

        // CE GUIMachineShredder.java:63-66: power bar (16x88px) at x=8, y=106-i
        if (be.getPower() > 0) {
            int powerFill = (int) be.getPowerScaled(88);
            graphics.blit(TEXTURE, leftPos + 8, topPos + 106 - powerFill, 176, 160 - powerFill, 16, powerFill);
        }

        // CE GUIMachineShredder.java:68-69: progress bar (34x18px) at x=63, y=89
        int progress = be.getDiFurnaceProgressScaled(34);
        graphics.blit(TEXTURE, leftPos + 63, topPos + 89, 176, 54, progress + 1, 18);

        // CE GUIMachineShredder.java:71-90: left gear status (18x18px) at x=43, y=71
        boolean errorFlag = false;
        int gearLeft = be.getGearLeft();
        if (gearLeft != 0) {
            int texY = (gearLeft - 1) * 18;
            graphics.blit(TEXTURE, leftPos + 43, topPos + 71, 176, texY, 18, 18);
            if (gearLeft == 3) {
                errorFlag = true;
            }
        } else {
            errorFlag = true;
        }

        // CE GUIMachineShredder.java:92-110: right gear status (18x18px) at x=79, y=71
        int gearRight = be.getGearRight();
        if (gearRight != 0) {
            int texY = (gearRight - 1) * 18;
            graphics.blit(TEXTURE, leftPos + 79, topPos + 71, 194, texY, 18, 18);
            if (gearRight == 3) {
                errorFlag = true;
            }
        } else {
            errorFlag = true;
        }

        // CE GUIMachineShredder.java:112-113: error icon (16x16px) at x=-16, y=36
        if (errorFlag) {
            graphics.blit(TEXTURE, leftPos - 16, topPos + 36, 212, 0, 16, 16);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // CE GUIMachineShredder.java:49-51: title centered + inventory label
        graphics.drawString(this.font, this.title, 106 - this.font.width(this.title) / 2, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // CE GUIMachineShredder.java:29-43: tooltips for power and error
        MachineShredderBlockEntity be = menu.be;
        if (mouseX >= leftPos + 8 && mouseX < leftPos + 8 + 16 && mouseY >= topPos + 18 && mouseY < topPos + 18 + 88) {
            graphics.renderTooltip(this.font, Component.literal(be.getPower() + " / " + MachineShredderBlockEntity.MAX_POWER + " HE"), mouseX, mouseY);
        }

        boolean errorFlag = (be.getGearLeft() == 0 || be.getGearLeft() == 3) || (be.getGearRight() == 0 || be.getGearRight() == 3);
        if (errorFlag && mouseX >= leftPos - 16 && mouseX < leftPos - 16 + 16 && mouseY >= topPos + 36 && mouseY < topPos + 36 + 16) {
            graphics.renderTooltip(this.font, Component.literal("Error: Shredder blades are broken or missing!"), mouseX, mouseY);
        }
    }
}
