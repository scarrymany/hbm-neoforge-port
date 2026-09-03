package com.hbm.client.gui.screens.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKControlAutoBlockEntity;
import com.hbm.inventory.container.machine.rbmk.RBMKControlAutoMenu;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * NeoForge port of CE {@code GUIRBMKControlAuto}.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/GUIRBMKControlAuto.java
 * <p>
 * GUI with control rod level bar, function selector, and 4 text input fields for auto-control parameters.
 */
public class RBMKControlAutoScreen extends AbstractContainerScreen<RBMKControlAutoMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/rbmk/gui_rbmk_control_auto.png");

    public RBMKControlAutoScreen(RBMKControlAutoMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        RBMKControlAutoBlockEntity be = menu.be;

        // CE GUIRBMKControlAuto.java:154-157: rod level bar (8x56px) at x=124, y=29
        int height = (int) (56 * (1D - be.extraction));
        if (height > 0) {
            graphics.blit(TEXTURE, leftPos + 124, topPos + 29, 176, 56 - height, 8, height);
        }

        // CE GUIRBMKControlAuto.java:159-160: function indicator (26x19px) at x=59, y=27
        int f = be.function.ordinal();
        graphics.blit(TEXTURE, leftPos + 59, topPos + 27, 184, f * 19, 26, 19);

        // CE GUIRBMKControlAuto.java:162-164: power indicator (16x16px) at x=136, y=21
        if (be.isPowered()) {
            graphics.blit(TEXTURE, leftPos + 136, topPos + 21, 210, be.hasPower ? 16 : 0, 16, 16);
        }

        // CE GUIRBMKControlAuto.java:166-168: draw text fields
        // TODO(CE): Port text field rendering + input (CE GUIRBMKControlAuto.java:40-60, 90-138, 172-180)
        // 4 fields at x=30, y=27+11*i (26x6px each):
        // - fields[0]: levelUpper (max 3 chars, 0-100)
        // - fields[1]: levelLower (max 3 chars, 0-100)
        // - fields[2]: heatUpper (max 4 chars, 0-9999)
        // - fields[3]: heatLower (max 4 chars, 0-9999)
        // Save button at x=28, y=70, 30x10 sends NBTControlPacket with all 4 values
        // Function buttons at x=61, y=48+k*11 (3 buttons, 22x10) send "function" = k (0=LINEAR, 1=QUAD_UP, 2=QUAD_DOWN)

        // Display current values as static text for now
        graphics.drawString(this.font, String.valueOf((int) be.levelUpper), leftPos + 30, topPos + 27, 0xFFFFFF, false);
        graphics.drawString(this.font, String.valueOf((int) be.levelLower), leftPos + 30, topPos + 38, 0xFFFFFF, false);
        graphics.drawString(this.font, String.valueOf((int) be.heatUpper), leftPos + 30, topPos + 49, 0xFFFFFF, false);
        graphics.drawString(this.font, String.valueOf((int) be.heatLower), leftPos + 30, topPos + 60, 0xFFFFFF, false);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // CE GUIRBMKControlAuto.java:141-144: title centered + inventory label
        graphics.drawString(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // CE GUIRBMKControlAuto.java:64-87: tooltips for various UI elements
        RBMKControlAutoBlockEntity be = menu.be;
        if (mouseX >= leftPos + 124 && mouseX < leftPos + 124 + 16 && mouseY >= topPos + 29 && mouseY < topPos + 29 + 56) {
            graphics.renderTooltip(this.font, Component.literal((int) (be.extraction * 100) + "%"), mouseX, mouseY);
        }
        if (mouseX >= leftPos + 58 && mouseX < leftPos + 58 + 28 && mouseY >= topPos + 26 && mouseY < topPos + 26 + 19) {
            String func = "Function: ";
            switch (be.function) {
                case LINEAR: func += "Linear"; break;
                case QUAD_UP: func += "Quadratic"; break;
                case QUAD_DOWN: func += "Inverse Quadratic"; break;
            }
            graphics.renderTooltip(this.font, Component.literal(func), mouseX, mouseY);
        }
    }
}
