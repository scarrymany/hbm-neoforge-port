package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.ReactorResearchBlockEntity;
import com.hbm.inventory.container.machine.dummyable.ReactorResearchMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * CE {@code GUIReactorResearch} — {@code gui_research_reactor.png} 176×222.
 * TODO(CE: GUIReactorResearch.java:29): NumberDisplay 7-seg widgets — font digits at CE coords.
 */
public class ReactorResearchScreen extends GuiInfoContainer<ReactorResearchMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/reactors/gui_research_reactor.png");
    private static final int GREEN = 0xFF08FF00;

    private EditBox field;
    private int timer;

    public ReactorResearchScreen(ReactorResearchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 121;
    }

    @Override
    protected void init() {
        super.init();
        this.field = new EditBox(this.font, leftPos + 8, topPos + 99, 33, 16, Component.empty());
        this.field.setBordered(false);
        this.field.setMaxLength(3);
        this.field.setValue(String.valueOf((int) (this.getMenu().be.rodLevel * 100)));
        this.addRenderableWidget(this.field);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        ReactorResearchBlockEntity be = this.getMenu().be;
        if (be.rodLevel <= 0.5D) {
            for (int ix = 0; ix < 3; ix++) {
                for (int iy = 0; iy < 3; iy++) {
                    guiGraphics.blit(TEXTURE, x + 81 + 36 * ix, y + 26 + 36 * iy, 176, 0, 8, 8);
                }
            }
            for (int ix = 0; ix < 2; ix++) {
                for (int iy = 0; iy < 2; iy++) {
                    guiGraphics.blit(TEXTURE, x + 99 + 36 * ix, y + 44 + 36 * iy, 176, 0, 8, 8);
                }
            }
        }
        if (timer > 0) {
            guiGraphics.blit(TEXTURE, x + 44, y + 97, 176, 8, 11, 20);
            timer--;
        }
        int[] data = be.getDisplayData();
        drawDigit(guiGraphics, x + 14, y + 25, data[0], 4);
        drawDigit(guiGraphics, x + 12, y + 63, data[1], 3);
        int control;
        try {
            control = Mth.clamp(Integer.parseInt(field.getValue()), 0, 100);
            field.setValue(String.valueOf(control));
        } catch (NumberFormatException e) {
            field.setValue("0");
            control = 0;
        }
        drawDigit(guiGraphics, x + 5, y + 101, control, 3);
        drawInfoPanel(guiGraphics, x - 14, y + 23, 3);
        drawInfoPanel(guiGraphics, x - 14, y + 61, 2);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.getMenu().be.getDisplayName();
        guiGraphics.drawString(this.font, name, 121 - this.font.width(name) / 2, 6, 0xE5E5E5, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
        guiGraphics.drawString(this.font, "Flux", 6, 13, 0xE5E5E5, false);
        guiGraphics.drawString(this.font, "Heat", 6, 51, 0xE5E5E5, false);
        guiGraphics.drawString(this.font, "Control", 6, 89, 0xE5E5E5, false);
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos - 14, topPos + 23, 16, 16,
                leftPos - 6, topPos + 39,
                Component.literal("The reactor has to be submerged"),
                Component.literal("in water on its sides to cool."),
                Component.literal("The neutron flux is provided to"),
                Component.literal("adjacent breeding reactors."));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos - 14, topPos + 61, 16, 16,
                leftPos - 6, topPos + 77,
                Component.literal("This reactor is fueled with plate fuel."),
                Component.literal("The reaction needs a neutron source to start."));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, 44, 97, 11, 20)) {
            try {
                int v = Mth.clamp(Integer.parseInt(field.getValue()), 0, 100);
                field.setValue(String.valueOf(v));
                click();
                this.minecraft.gameMode.handleInventoryButtonClick(this.getMenu().containerId, v);
                timer = 15;
            } catch (NumberFormatException ignored) {
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawDigit(GuiGraphics guiGraphics, int x, int y, int value, int digits) {
        String text = String.format("%0" + digits + "d", Math.max(0, value));
        if (text.length() > digits) text = text.substring(text.length() - digits);
        guiGraphics.drawString(this.font, text, x, y, GREEN, false);
    }
}
