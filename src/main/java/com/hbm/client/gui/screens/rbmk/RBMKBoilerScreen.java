package com.hbm.client.gui.screens.rbmk;

import com.hbm.blockentity.machine.rbmk.RBMKBoilerBlockEntity;
import com.hbm.inventory.container.machine.rbmk.RBMKBoilerMenu;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.main.MainRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * NeoForge port of CE {@code GUIRBMKBoiler}.
 * <p>
 * CE: upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/GUIRBMKBoiler.java
 * <p>
 * GUI with feed tank, steam tank, compression button, and steam-type indicator overlay.
 */
public class RBMKBoilerScreen extends AbstractContainerScreen<RBMKBoilerMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/rbmk/gui_rbmk_boiler.png");

    public RBMKBoilerScreen(RBMKBoilerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        RBMKBoilerBlockEntity be = menu.be;

        // CE GUIRBMKBoiler.java:69-70: feed tank (14x58px) at x=126, y=82
        int feedFill = be.feed.getMaxFill() > 0 ? (int) (be.feed.getFill() * 58 / be.feed.getMaxFill()) : 0;
        if (feedFill > 0) {
            graphics.blit(TEXTURE, leftPos + 126, topPos + 82 - feedFill, 176, 58 - feedFill, 14, feedFill);
        }

        // CE GUIRBMKBoiler.java:72-77: steam tank (4x22px) at x=91, y=65, with +1px adjustments
        int steamFill = be.steam.getMaxFill() > 0 ? (int) (be.steam.getFill() * 22 / be.steam.getMaxFill()) : 0;
        if (steamFill > 0) steamFill++;
        if (steamFill > 22) steamFill++;
        if (steamFill > 0) {
            graphics.blit(TEXTURE, leftPos + 91, topPos + 65 - steamFill, 190, 24 - steamFill, 4, steamFill);
        }

        // CE GUIRBMKBoiler.java:79-87: steam-type indicator overlay (14x58px) at x=36, y=24
        if (be.steam.getTankType() == Fluids.STEAM) {
            graphics.blit(TEXTURE, leftPos + 36, topPos + 24, 194, 0, 14, 58);
        } else if (be.steam.getTankType() == Fluids.HOTSTEAM) {
            graphics.blit(TEXTURE, leftPos + 36, topPos + 24, 208, 0, 14, 58);
        } else if (be.steam.getTankType() == Fluids.SUPERHOTSTEAM) {
            graphics.blit(TEXTURE, leftPos + 36, topPos + 24, 222, 0, 14, 58);
        } else if (be.steam.getTankType() == Fluids.ULTRAHOTSTEAM) {
            graphics.blit(TEXTURE, leftPos + 36, topPos + 24, 236, 0, 14, 58);
        }
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        // CE GUIRBMKBoiler.java:56-59: title centered + inventory label
        graphics.drawString(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 6, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // CE GUIRBMKBoiler.java:42-51: compression button click (x=33, y=21, 20x64px)
        // TODO(CE): Port NBTControlPacket for compression cycling (CE TileEntityRBMKBoiler.java:223-225)
        // if (mouseX >= leftPos + 33 && mouseX < leftPos + 33 + 20 && mouseY >= topPos + 21 && mouseY < topPos + 21 + 64) {
        //     send NBTControlPacket with "compression" = true to call BE.cycleCompressor()
        // }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);

        // CE GUIRBMKBoiler.java:36-38: tank tooltips
        RBMKBoilerBlockEntity be = menu.be;
        if (mouseX >= leftPos + 126 && mouseX < leftPos + 126 + 16 && mouseY >= topPos + 24 && mouseY < topPos + 24 + 56) {
            graphics.renderTooltip(this.font, Component.literal("Feed: " + be.feed.getFill() + " / " + be.feed.getMaxFill() + " mB"), (int) mouseX, (int) mouseY);
        }
        if (mouseX >= leftPos + 89 && mouseX < leftPos + 89 + 8 && mouseY >= topPos + 39 && mouseY < topPos + 39 + 28) {
            graphics.renderTooltip(this.font, Component.literal("Steam: " + be.steam.getFill() + " / " + be.steam.getMaxFill() + " mB"), (int) mouseX, (int) mouseY);
        }
    }
}
