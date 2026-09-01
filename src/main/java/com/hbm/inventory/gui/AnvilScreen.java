package com.hbm.inventory.gui;

import com.hbm.inventory.container.AnvilMenu;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilConstructionRecipe;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.AnvilCraftPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** CE {@code GUIAnvil.java}:38-216. 176×222, search + 2×5 construction page + craft packet. */
public class AnvilScreen extends AbstractContainerScreen<AnvilMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_anvil.png");

    private final int tier;
    private final List<AnvilConstructionRecipe> origin = new ArrayList<>();
    private final List<AnvilConstructionRecipe> recipes = new ArrayList<>();
    private int index;
    private int size;
    private int selection = -1;
    private EditBox search;

    public AnvilScreen(AnvilMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.tier = menu.tier;
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 94;
        for (AnvilConstructionRecipe recipe : AnvilRecipes.getConstruction()) {
            if (recipe.isTierValid(this.tier)) this.origin.add(recipe);
        }
        regenerate();
    }

    @Override
    protected void init() {
        super.init();
        this.search = new EditBox(this.font, this.leftPos + 10, this.topPos + 111, 84, 12, Component.literal("Search"));
        this.search.setBordered(false);
        this.search.setMaxLength(25);
        this.search.setTextColor(0xFFFFFF);
        this.addRenderableWidget(this.search);
    }

    private void regenerate() {
        this.recipes.clear();
        this.recipes.addAll(this.origin);
        resetPaging();
    }

    private void search(String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        this.recipes.clear();
        if (needle.isEmpty()) {
            this.recipes.addAll(this.origin);
        } else {
            for (AnvilConstructionRecipe recipe : this.origin) {
                ItemStack display = recipe.getDisplay();
                String name = display.getHoverName().getString().toLowerCase(Locale.ROOT);
                String id = display.getItem().toString().toLowerCase(Locale.ROOT);
                if (name.contains(needle) || id.contains(needle)) this.recipes.add(recipe);
            }
        }
        resetPaging();
    }

    private void resetPaging() {
        this.index = 0;
        this.selection = -1;
        this.size = Math.max(0, (int) Math.ceil((this.recipes.size() - 10) / 2.0D));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX;
        int y = (int) mouseY;
        if (in(x, y, 7, 71, 9, 36)) {
            if (this.index > 0) this.index--;
            return true;
        }
        if (in(x, y, 106, 71, 9, 36)) {
            if (this.index < this.size) this.index++;
            return true;
        }
        if (in(x, y, 52, 53, 18, 18)) {
            if (this.selection >= 0 && this.selection < this.recipes.size()) {
                int mode = hasShiftDown() ? 1 : 0;
                int recipeIndex = AnvilRecipes.getConstruction().indexOf(this.recipes.get(this.selection));
                PacketDistributor.sendToServer(new AnvilCraftPacket(recipeIndex, mode));
            }
            return true;
        }
        if (in(x, y, 97, 107, 18, 18)) {
            search(this.search.getValue());
            return true;
        }
        for (int i = index * 2; i < index * 2 + 10; i++) {
            if (i >= this.recipes.size()) break;
            int ind = i - index * 2;
            int ix = 16 + 18 * (ind / 2);
            int iy = 71 + 18 * (ind % 2);
            if (in(x, y, ix, iy, 18, 18)) {
                this.selection = this.selection == i ? -1 : i;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0 && this.index > 0) {
            this.index--;
            return true;
        }
        if (scrollY < 0 && this.index < this.size) {
            this.index++;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.search.isFocused() && keyCode == GLFW.GLFW_KEY_ENTER) {
            search(this.search.getValue());
            return true;
        }
        if (this.search.keyPressed(keyCode, scanCode, modifiers) || this.search.canConsumeInput()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
        for (int i = index * 2; i < index * 2 + 10; i++) {
            if (i >= this.recipes.size()) break;
            int ind = i - index * 2;
            int ix = x + 16 + 18 * (ind / 2);
            int iy = y + 71 + 18 * (ind % 2);
            ItemStack display = this.recipes.get(i).getDisplay();
            guiGraphics.renderItem(display, ix + 1, iy + 1);
            guiGraphics.renderItemDecorations(this.font, display, ix + 1, iy + 1);
            if (this.selection == i) {
                guiGraphics.fill(ix, iy, ix + 18, iy + 18, 0x80FFFFFF);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        for (int i = index * 2; i < index * 2 + 10; i++) {
            if (i >= this.recipes.size()) break;
            int ind = i - index * 2;
            int ix = 16 + 18 * (ind / 2);
            int iy = 71 + 18 * (ind % 2);
            if (in(mouseX, mouseY, ix, iy, 18, 18)) {
                guiGraphics.renderTooltip(this.font, this.recipes.get(i).getDisplay(), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, Component.translatable("container.hbm.anvil", this.tier),
                61 - this.font.width(Component.translatable("container.hbm.anvil", this.tier)) / 2, 8, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    private boolean in(double mouseX, double mouseY, int left, int top, int w, int h) {
        return this.leftPos + left <= mouseX && mouseX < this.leftPos + left + w
                && this.topPos + top <= mouseY && mouseY < this.topPos + top + h;
    }
}
