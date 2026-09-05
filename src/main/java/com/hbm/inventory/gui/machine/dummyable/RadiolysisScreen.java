package com.hbm.inventory.gui.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.MachineRadiolysisBlockEntity;
import com.hbm.inventory.container.machine.dummyable.RadiolysisMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.main.MainRegistry;
import com.hbm.util.i18n.I18nUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Exact CE {@code GUIRadiolysis} on existing {@code gui_radiolysis.png} 230×166.
 * Power 8,{@code 51-i} from 240; input 61,69 8×52; outs 87,33+69 12×16; info -16,16/34/52.
 * Foreign {@code gui_electrolyser_fluid.png} + invented {@code fill()} removed.
 */
public class RadiolysisScreen extends GuiInfoContainer<RadiolysisMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/gui_radiolysis.png");

    public RadiolysisScreen(RadiolysisMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 230;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineRadiolysisBlockEntity be = this.getMenu().be;
        // CE GUIRadiolysis.java:75-86
        int i = (int) (be.getPower() * 34 / Math.max(1L, be.getMaxPower()));
        if (i > 0) {
            guiGraphics.blit(TEXTURE, x + 8, y + 51 - i, 240, 34 - i, 16, i);
        }
        be.input.renderTank(x + 61, y + 69, 0, 8, 52);
        be.out1.renderTank(x + 87, y + 33, 0, 12, 16);
        be.out2.renderTank(x + 87, y + 69, 0, 12, 16);
        drawInfoPanel(guiGraphics, x - 16, y + 16, 10);
        drawInfoPanel(guiGraphics, x - 16, y + 16 + 18, 2);
        drawInfoPanel(guiGraphics, x - 16, y + 16 + 36, 3);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :64 — title centered on x=88
        String name = this.title.getString();
        guiGraphics.drawString(this.font, this.title, 88 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineRadiolysisBlockEntity be = this.getMenu().be;
        be.input.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 61, topPos + 17, 8, 52);
        be.out1.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 87, topPos + 17, 12, 16);
        be.out2.renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 87, topPos + 53, 12, 16);
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 17, 16, 34, be.getPower(), be.getMaxPower());
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos - 16, topPos + 16, 16, 16, leftPos - 8, topPos + 16 + 16,
                toComponents(I18nUtil.resolveKeyArray("desc.gui.radiolysis.desc")));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos - 16, topPos + 16 + 18, 16, 16, leftPos - 8, topPos + 16 + 18 + 16,
                toComponents(I18nUtil.resolveKeyArray("desc.gui.rtg.heat", be.heat)));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos - 16, topPos + 16 + 36, 16, 16, leftPos - 8, topPos + 16 + 36 + 16,
                pelletLines());
    }

    private static List<Component> pelletLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(I18nUtil.resolveKey("desc.gui.rtg.pellets")));
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof ItemRTGPellet pellet) {
                lines.add(Component.literal(I18nUtil.resolveKey("desc.gui.rtg.pelletPower",
                        I18nUtil.resolveKey(pellet.getDescriptionId()), pellet.getHeat() * 10)));
            }
        }
        return lines;
    }

    private static List<Component> toComponents(String[] lines) {
        List<Component> out = new ArrayList<>(lines.length);
        for (String line : lines) {
            out.add(Component.literal(line));
        }
        return out;
    }
}
