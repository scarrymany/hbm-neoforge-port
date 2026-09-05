package com.hbm.inventory.gui.machine;

import com.hbm.blockentity.machine.MachineCrucibleBlockEntity;
import com.hbm.inventory.container.machine.MachineCrucibleMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.CrucibleRecipe;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.CrucibleControlPacket;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Exact CE {@code GUICrucible} on existing {@code gui_crucible.png} 176×214.
 * Progress 126,82 / heat 126,91 / stacks 62,97 + 17,97 / recipe icon 107,81.
 * Selector stays the existing cycle ({@code CrucibleRecipes} is not {@code GenericRecipes}).
 */
public class MachineCrucibleScreen extends GuiInfoContainer<MachineCrucibleMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_crucible.png");

    public MachineCrucibleScreen(MachineCrucibleMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 214;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        MachineCrucibleBlockEntity be = this.getMenu().be;
        // CE GUICrucible.java:83-86
        int pGauge = be.getProgress() * 33 / MachineCrucibleBlockEntity.processTime;
        if (pGauge > 0) {
            guiGraphics.blit(TEXTURE, x + 126, y + 82, 176, 0, pGauge, 5);
        }
        int hGauge = be.getHeat() * 33 / MachineCrucibleBlockEntity.maxHeat;
        if (hGauge > 0) {
            guiGraphics.blit(TEXTURE, x + 126, y + 91, 176, 5, hGauge, 5);
        }

        CrucibleRecipe recipe = CrucibleRecipes.getRecipe(be.getRecipeName());
        ItemStack icon = recipe != null ? recipe.icon() : templateFolder();
        if (!icon.isEmpty()) {
            guiGraphics.renderItem(icon, x + 107, y + 81);
        }

        if (!be.getRecipeStack().isEmpty()) {
            drawStack(guiGraphics, be.getRecipeStack(), MachineCrucibleBlockEntity.recipeZCapacity, x + 62, y + 97);
        }
        if (!be.getWasteStack().isEmpty()) {
            drawStack(guiGraphics, be.getWasteStack(), MachineCrucibleBlockEntity.wasteZCapacity, x + 17, y + 97);
        }
    }

    /** Exact CE {@code GUICrucible.drawStack} :110-142. */
    private void drawStack(GuiGraphics guiGraphics, List<Mats.MaterialStack> stack, int capacity, int x, int bottomY) {
        if (stack.isEmpty() || capacity <= 0) return;

        int lastHeight = 0;
        int lastQuant = 0;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);

        for (Mats.MaterialStack sta : stack) {
            int targetHeight = (lastQuant + sta.amount) * 79 / capacity;
            if (lastHeight == targetHeight) continue;

            int offset = sta.material.smeltable == NTMMaterial.SmeltingBehavior.ADDITIVE ? 34 : 0;
            int hex = sta.material.moltenColor;
            float r = ((hex >> 16) & 0xFF) / 255F;
            float g = ((hex >> 8) & 0xFF) / 255F;
            float b = (hex & 0xFF) / 255F;
            int h = targetHeight - lastHeight;
            int blitY = bottomY - targetHeight;
            int v = 89 - targetHeight;

            guiGraphics.setColor(r, g, b, 1F);
            guiGraphics.blit(TEXTURE, x, blitY, 176 + offset, v, 34, h);
            guiGraphics.setColor(1F, 1F, 1F, 0.3F);
            guiGraphics.blit(TEXTURE, x, blitY, 176 + offset, v, 34, h);

            lastQuant += sta.amount;
            lastHeight = targetHeight;
        }

        guiGraphics.setColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE GUICrucible.java:72-73 — inventory label only
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        MachineCrucibleBlockEntity be = this.getMenu().be;
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 16, topPos + 17, 36, 81, mouseX, mouseY,
                stackTooltip(be.getWasteStack()));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 61, topPos + 17, 36, 81, mouseX, mouseY,
                stackTooltip(be.getRecipeStack()));

        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 125, topPos + 81, 34, 7, mouseX, mouseY,
                Component.literal(String.format(Locale.US, "%,d", be.getProgress())
                        + " / " + String.format(Locale.US, "%,d", MachineCrucibleBlockEntity.processTime) + "TU"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, leftPos + 125, topPos + 90, 34, 7, mouseX, mouseY,
                Component.literal(String.format(Locale.US, "%,d", be.getHeat())
                        + " / " + String.format(Locale.US, "%,d", MachineCrucibleBlockEntity.maxHeat) + "TU"));

        if (isHovered(mouseX, mouseY, 106, 80, 18, 18)) {
            CrucibleRecipe loaded = CrucibleRecipes.getRecipe(be.getRecipeName());
            List<Component> tip = loaded != null
                    ? loaded.print()
                    : List.of(Component.translatable("gui.recipe.setRecipe").withStyle(ChatFormatting.YELLOW));
            guiGraphics.renderComponentTooltip(this.font, tip, mouseX, mouseY);
        }
    }

    private List<Component> stackTooltip(List<Mats.MaterialStack> stack) {
        List<Component> lines = new ArrayList<>();
        if (stack.isEmpty()) {
            lines.add(Component.literal("Empty").withStyle(ChatFormatting.RED));
            return lines;
        }
        for (Mats.MaterialStack sta : stack) {
            lines.add(Component.empty()
                    .append(sta.material.getName())
                    .append(": ")
                    .append(Mats.formatAmount(sta.amount, Screen.hasShiftDown()))
                    .withStyle(ChatFormatting.YELLOW));
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // CE GUICrucible.java:66-68 opens GenericRecipes selector — CrucibleRecipes is the
        // existing Map table, not GenericRecipes. Keep the already-landed cycle + CrucibleControlPacket.
        if (isHovered(mouseX, mouseY, 106, 80, 18, 18)) {
            click();

            List<String> names = CrucibleRecipes.getRecipeNames();
            String next = "";
            if (button != 1 && !names.isEmpty()) {
                int idx = names.indexOf(this.getMenu().be.getRecipeName());
                next = names.get(Math.floorMod(idx + 1, names.size()));
            }

            CompoundTag data = new CompoundTag();
            data.putString("recipe", next);
            PacketDistributor.sendToServer(new CrucibleControlPacket(this.getMenu().be.getBlockPos(), data));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static ItemStack templateFolder() {
        return new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "template_folder")));
    }
}
