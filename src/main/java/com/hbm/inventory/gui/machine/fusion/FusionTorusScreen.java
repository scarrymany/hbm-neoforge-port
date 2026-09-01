package com.hbm.inventory.gui.machine.fusion;

import com.hbm.inventory.container.machine.fusion.FusionTorusMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.recipes.FusionRecipe;
import com.hbm.inventory.recipes.FusionRecipes;
import com.hbm.packet.toserver.FusionControlPacket;
import com.hbm.util.BobMathUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/** CE {@code GUIFusionTorus} numbers. No CE png in tree — fill-rect, no invented art. */
public class FusionTorusScreen extends GuiInfoContainer<FusionTorusMenu> {

    public FusionTorusScreen(FusionTorusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 230;
        this.imageHeight = 244;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);
        var be = this.getMenu().be;
        be.tanks[0].renderTank(x + 44, y + 18, 0, 16, 52);
        be.tanks[1].renderTank(x + 62, y + 18, 0, 16, 52);
        be.tanks[2].renderTank(x + 80, y + 18, 0, 16, 52);
        be.tanks[3].renderTank(x + 152, y + 18, 0, 16, 52);
        be.coolantTanks[0].renderTank(x + 188, y + 46, 0, 16, 52);
        be.coolantTanks[1].renderTank(x + 206, y + 46, 0, 16, 52);
        int bar = (int) Math.ceil(70 * be.fusionModule.progress);
        if (bar > 0) guiGraphics.fill(x + 98, y + 81, x + 98 + bar, y + 87, 0xFF3C78C8);
        int bonus = (int) Math.min(Math.ceil(70 * be.fusionModule.bonus), 70);
        if (bonus > 0) guiGraphics.fill(x + 98, y + 91, x + 98 + bonus, y + 97, 0xFF78C83C);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        var be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 8, topPos + 18, 16, 62, be.getPower(), be.getMaxPower());
        FusionRecipe recipe = FusionRecipes.INSTANCE.byName(be.fusionModule.recipe);
        if (recipe != null) {
            drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 43, topPos + 115, 18, 18,
                    Component.literal("-> " + BobMathUtil.getShortNumber(be.klystronEnergy) + "KyU / "
                            + BobMathUtil.getShortNumber(recipe.ignitionTemp) + "KyU"));
            drawCustomInfo(guiGraphics, mouseX, mouseY, leftPos + 79, topPos + 115, 18, 18,
                    Component.literal("<- " + BobMathUtil.getShortNumber(be.plasmaEnergy) + "TU / "
                            + BobMathUtil.getShortNumber(recipe.outputTemp) + "TU"));
        }
        int heat = (int) Math.ceil(be.temperature);
        guiGraphics.drawString(this.font, heat + "K / 123K", 136, 22, heat > 123 ? 0xFF5555 : 0x55FFFF, false);
        int row = 0;
        for (FusionRecipe r : FusionRecipes.INSTANCE.recipeOrderedList) {
            boolean sel = r.getInternalName().equals(be.fusionModule.recipe);
            guiGraphics.drawString(this.font, (sel ? "> " : "  ") + r.getInternalName(), 8, 140 + row * 9,
                    sel ? 0x00FF00 : 0x404040, false);
            row++;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int row = 0;
        for (FusionRecipe r : FusionRecipes.INSTANCE.recipeOrderedList) {
            if (isHovered(mouseX, mouseY, 8, 140 + row * 9, 80, 9)) {
                click();
                CompoundTag data = new CompoundTag();
                data.putInt("index", 0);
                data.putString("selection", r.getInternalName());
                PacketDistributor.sendToServer(new FusionControlPacket(this.getMenu().be.getBlockPos(), data));
                return true;
            }
            row++;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
