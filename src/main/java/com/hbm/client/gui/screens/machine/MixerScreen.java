package com.hbm.client.gui.screens.machine;

import com.hbm.blockentity.machine.MachineMixerBlockEntity;
import com.hbm.inventory.container.machine.MachineMixerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.MixerRecipes.MixerRecipe;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.NBTControlPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Exact CE {@code GUIMixer} on existing {@code gui_mixer.png} 176×204.
 * Power 23,{@code 75-i} from 176,{@code 52-i}; progress 62,36 from 192,0; tanks via {@code renderTank}.
 * Invented tank atlas UV 200/207/214 removed.
 */
public class MixerScreen extends GuiInfoContainer<MachineMixerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_mixer.png");

    public MixerScreen(MachineMixerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineMixerBlockEntity be = this.getMenu().be;
        // CE GUIMixer.java:89-98
        int i = (int) (be.getPower() * 53 / Math.max(1L, be.getMaxPower()));
        if (i > 0) {
            guiGraphics.blit(TEXTURE, x + 23, y + 75 - i, 176, 52 - i, 16, i);
        }
        if (be.processTime > 0 && be.progress > 0) {
            int j = be.progress * 53 / be.processTime;
            guiGraphics.blit(TEXTURE, x + 62, y + 36, 192, 0, j, 44);
        }
        be.tanks.get(0).renderTank(x + 43, y + 23 + 52, 0, 7, 52);
        be.tanks.get(1).renderTank(x + 52, y + 23 + 52, 0, 7, 52);
        be.tanks.get(2).renderTank(x + 117, y + 23 + 52, 0, 16, 52);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :78 — title centered
        int nameX = this.imageWidth / 2 - this.font.width(this.title) / 2;
        guiGraphics.drawString(this.font, this.title, nameX, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineMixerBlockEntity be = this.getMenu().be;
        drawElectricityInfo(guiGraphics, mouseX, mouseY, leftPos + 23, topPos + 22, 16, 52, be.getPower(), be.getMaxPower());
        be.tanks.get(0).renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 43, topPos + 23, 7, 52);
        be.tanks.get(1).renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 52, topPos + 23, 7, 52);
        be.tanks.get(2).renderTankTooltip(guiGraphics, mouseX, mouseY, leftPos + 117, topPos + 23, 16, 52);

        MixerRecipe[] recipes = MixerRecipes.getOutput(be.tanks.get(2).getTankType());
        if (recipes != null && recipes.length > 1) {
            List<Component> label = new ArrayList<>();
            label.add(Component.literal("Current recipe (" + (be.recipeIndex + 1) + "/" + recipes.length + "):")
                    .withStyle(ChatFormatting.YELLOW));
            MixerRecipe recipe = recipes[be.recipeIndex % recipes.length];
            if (recipe.input1 != null) {
                label.add(Component.literal("-").append(recipe.input1.type.getLocalizedName()));
            }
            if (recipe.input2 != null) {
                label.add(Component.literal("-").append(recipe.input2.type.getLocalizedName()));
            }
            if (recipe.solidInput != null) {
                ItemStack solid = recipe.solidInput.extractForCyclingDisplay(20);
                if (!solid.isEmpty()) {
                    label.add(Component.literal("-").append(solid.getHoverName()));
                }
            }
            label.add(Component.literal("Click to change!").withStyle(ChatFormatting.RED));
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 62, topPos + 22, 12, 12, mouseX, mouseY, label);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // CE GUIMixer.java:65-70
        if (isHovered(mouseX, mouseY, 62, 22, 12, 12)) {
            click();
            CompoundTag data = new CompoundTag();
            data.putBoolean("toggle", true);
            PacketDistributor.sendToServer(new NBTControlPacket(this.getMenu().be.getBlockPos(), data));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
