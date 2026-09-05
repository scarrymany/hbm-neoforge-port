package com.hbm.inventory.gui.machine.workshop;

import com.hbm.blockentity.machine.workshop.AmmoPressBlockEntity;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.container.machine.workshop.AmmoPressMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.inventory.recipes.AmmoPressRecipes.AmmoPressRecipe;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.NBTControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Exact CE {@code GUIMachineAmmoPress} on existing {@code gui_ammo_press.png} 176×200.
 * Recipe page 12 tiles, search 10,75, selection NBT. Invented Idle/Pressing tooltip removed.
 */
public class AmmoPressScreen extends GuiInfoContainer<AmmoPressMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_ammo_press.png");

    private final ArrayList<AmmoPressRecipe> recipes = new ArrayList<>();
    private int index;
    private int size;
    private int selection;
    private EditBox search;

    public AmmoPressScreen(AmmoPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
        this.selection = menu.be.selectedRecipe;
        regenerateRecipes();
    }

    @Override
    protected void init() {
        super.init();
        this.search = new EditBox(this.font, this.leftPos + 10, this.topPos + 75, 66, 12, Component.empty());
        this.search.setTextColor(0xFFFFFF);
        this.search.setTextColorUneditable(0xFFFFFF);
        this.search.setBordered(false);
        this.search.setMaxLength(25);
        this.addRenderableWidget(this.search);
    }

    private void regenerateRecipes() {
        this.recipes.clear();
        this.recipes.addAll(AmmoPressRecipes.getAllRecipes());
        resetPaging();
    }

    private void search(String query) {
        String needle = query.toLowerCase(Locale.US);
        this.recipes.clear();
        if (needle.isEmpty()) {
            this.recipes.addAll(AmmoPressRecipes.getAllRecipes());
        } else {
            for (AmmoPressRecipe recipe : AmmoPressRecipes.getAllRecipes()) {
                if (recipe.output.getHoverName().getString().toLowerCase(Locale.US).contains(needle)) {
                    this.recipes.add(recipe);
                }
            }
        }
        resetPaging();
    }

    private void resetPaging() {
        this.index = 0;
        this.size = Math.max(0, (int) Math.ceil((this.recipes.size() - 12) / 3D));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        // CE GUIMachineAmmoPress.java:196-205
        if (isHovered(mouseX, mouseY, 7, 17, 9, 54)) {
            guiGraphics.blit(TEXTURE, x + 7, y + 17, 176, 0, 9, 54);
        }
        if (isHovered(mouseX, mouseY, 88, 17, 9, 54)) {
            guiGraphics.blit(TEXTURE, x + 88, y + 17, 185, 0, 9, 54);
        }
        if (this.search.isFocused()) {
            guiGraphics.blit(TEXTURE, x + 8, y + 72, 176, 54, 70, 16);
        }

        AmmoPressBlockEntity be = this.getMenu().be;
        List<AmmoPressRecipe> all = AmmoPressRecipes.getAllRecipes();
        for (int i = index * 3; i < index * 3 + 12; i++) {
            if (i >= this.recipes.size()) break;
            int ind = i - index * 3;
            int col = ind / 3;
            int row = ind % 3;
            AmmoPressRecipe recipe = this.recipes.get(i);
            int ix = x + 17 + 18 * col;
            int iy = y + 18 + 18 * row;
            guiGraphics.renderItem(recipe.output, ix, iy);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(ix + 8, iy + 8, 200);
            guiGraphics.pose().scale(0.5F, 0.5F, 1F);
            guiGraphics.renderItemDecorations(this.font, recipe.output, 0, 0, recipe.output.getCount() + "");
            guiGraphics.pose().popPose();
            int sel = all.indexOf(recipe);
            if (selection == sel) {
                guiGraphics.blit(TEXTURE, x + 16 + 18 * col, y + 17 + 18 * row, 194, 0, 18, 18);
            } else {
                guiGraphics.blit(TEXTURE, x + 16 + 18 * col, y + 17 + 18 * row, 212, 0, 18, 18);
            }
        }

        if (selection >= 0 && selection < all.size()) {
            AmmoPressRecipe recipe = all.get(selection);
            for (int i = 0; i < 9; i++) {
                AStack stack = recipe.input(i);
                if (stack == null) continue;
                if (!be.inventory.getStackInSlot(i).isEmpty()) continue;
                List<ItemStack> inputs = stack.extractForJEI();
                ItemStack input = inputs.isEmpty() ? ItemStack.EMPTY
                        : inputs.get((int) (Math.abs(System.currentTimeMillis() / 1000) % inputs.size()));
                if (input.isEmpty()) continue;
                int gx = x + 116 + 18 * (i % 3);
                int gy = y + 18 + 18 * (i / 3);
                guiGraphics.renderItem(input, gx, gy);
                if (input.getCount() > 1) {
                    guiGraphics.renderItemDecorations(this.font, input, gx, gy, input.getCount() + "");
                }
                RenderSystem.enableBlend();
                guiGraphics.setColor(1F, 1F, 1F, 0.5F);
                guiGraphics.blit(TEXTURE, gx, gy, 116 + 18 * (i % 3), 18 + 18 * (i / 3), 18, 18);
                guiGraphics.setColor(1F, 1F, 1F, 1F);
                RenderSystem.disableBlend();
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // CE :186 — inventory label only
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        for (Slot slot : this.menu.slots) {
            if (isHovered(mouseX, mouseY, slot.x, slot.y, 16, 16) && slot.hasItem()) {
                return;
            }
        }
        for (int i = index * 3; i < index * 3 + 12; i++) {
            if (i >= this.recipes.size()) break;
            int ind = i - index * 3;
            int ix = 16 + 18 * (ind / 3);
            int iy = 17 + 18 * (ind % 3);
            if (isHovered(mouseX, mouseY, ix, iy, 18, 18)) {
                guiGraphics.renderTooltip(this.font, this.recipes.get(i).output, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, 7, 17, 9, 54)) {
            click();
            if (this.index > 0) this.index--;
            return true;
        }
        if (isHovered(mouseX, mouseY, 88, 17, 9, 54)) {
            click();
            if (this.index < this.size) this.index++;
            return true;
        }
        List<AmmoPressRecipe> all = AmmoPressRecipes.getAllRecipes();
        for (int i = index * 3; i < index * 3 + 12; i++) {
            if (i >= this.recipes.size()) break;
            int ind = i - index * 3;
            int ix = 16 + 18 * (ind / 3);
            int iy = 17 + 18 * (ind % 3);
            if (isHovered(mouseX, mouseY, ix, iy, 18, 18)) {
                int newSelection = all.indexOf(this.recipes.get(i));
                this.selection = this.selection != newSelection ? newSelection : -1;
                CompoundTag data = new CompoundTag();
                data.putInt("selection", this.selection);
                PacketDistributor.sendToServer(new NBTControlPacket(this.getMenu().be.getBlockPos(), data));
                click();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= leftPos && mouseX < leftPos + imageWidth && mouseY >= topPos && mouseY < topPos + imageHeight
                && this.hoveredSlot == null) {
            if (scrollY > 0 && this.index > 0) {
                this.index--;
                return true;
            }
            if (scrollY < 0 && this.index < this.size) {
                this.index++;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.search.keyPressed(keyCode, scanCode, modifiers) || this.search.canConsumeInput()) {
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                search(this.search.getValue());
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.search.charTyped(codePoint, modifiers)) {
            search(this.search.getValue());
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }
}
