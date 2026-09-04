package com.hbm.inventory.gui;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.main.MainRegistry;
import com.hbm.packet.toserver.NBTControlPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code GUIScreenRecipeSelector} (331 lines) — Exact CE texture/hitboxes/NBT.
 * {@code onGuiClosed} sends {@code index}+{@code selection} via {@link NBTControlPacket}.
 */
public class GUIScreenRecipeSelector extends Screen {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/processing/gui_recipe_selector.png");

    public static final String NULL_SELECTION = "null";

    protected final int xSize = 176;
    protected final int ySize = 132;
    protected int guiLeft;
    protected int guiTop;

    protected final GenericRecipes recipeSet;
    protected final List<GenericRecipe> recipes = new ArrayList<>();
    protected EditBox search;
    protected int pageIndex;
    protected int size;
    protected String selection;
    protected final int index;
    protected final BlockPos pos;
    protected final Screen previousScreen;
    protected final String installedPool;

    public static void openSelector(GenericRecipes recipeSet, BlockPos pos, String selection, int index,
                                    String installedPool, Screen previousScreen) {
        Minecraft.getInstance().setScreen(
                new GUIScreenRecipeSelector(recipeSet, pos, selection, index, installedPool, previousScreen));
    }

    public GUIScreenRecipeSelector(GenericRecipes recipeSet, BlockPos pos, String selection, int index,
                                   String installedPool, Screen previousScreen) {
        super(Component.empty());
        this.recipeSet = recipeSet;
        this.pos = pos;
        this.selection = selection == null ? NULL_SELECTION : selection;
        this.index = index;
        this.installedPool = installedPool;
        this.previousScreen = previousScreen;
        regenerateRecipes();
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
        this.search = new EditBox(this.font, guiLeft + 28, guiTop + 111, 102, 12, Component.empty());
        this.search.setTextColor(0xFFFFFF);
        this.search.setTextColorUneditable(0xFFFFFF);
        this.search.setBordered(false);
        this.search.setMaxLength(32);
        this.search.setResponder(this::search);
        this.addRenderableWidget(this.search);
    }

    private void regenerateRecipes() {
        this.recipes.clear();
        for (GenericRecipe recipe : recipeSet.recipeOrderedList) {
            if (!recipe.isPooled() || (this.installedPool != null && recipe.isPartOfPool(this.installedPool))) {
                this.recipes.add(recipe);
            }
        }
        resetPaging();
    }

    private void search(String query) {
        this.recipes.clear();
        if (query.isEmpty()) {
            regenerateRecipes();
            return;
        }
        for (GenericRecipe recipe : recipeSet.recipeOrderedList) {
            if (recipe.matchesSearch(query)
                    && (!recipe.isPooled() || (this.installedPool != null && recipe.isPartOfPool(this.installedPool)))) {
                this.recipes.add(recipe);
            }
        }
        resetPaging();
    }

    private void resetPaging() {
        this.pageIndex = 0;
        this.size = Math.max(0, (int) Math.ceil((this.recipes.size() - 40) / 8D));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        drawBackgroundLayer(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawTooltips(graphics, mouseX, mouseY);
    }

    private void drawBackgroundLayer(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.setColor(1F, 1F, 1F, 1F);
        graphics.blit(TEXTURE, guiLeft, guiTop, 0, 0, xSize, ySize);

        if (this.search.isFocused()) {
            graphics.blit(TEXTURE, guiLeft + 26, guiTop + 108, 0, 132, 106, 16);
        }
        if (in(mouseX, mouseY, 152, 18, 16, 16)) {
            graphics.blit(TEXTURE, guiLeft + 152, guiTop + 18, 176, 0, 16, 16);
        }
        if (in(mouseX, mouseY, 152, 36, 16, 16)) {
            graphics.blit(TEXTURE, guiLeft + 152, guiTop + 36, 176, 16, 16, 16);
        }
        if (in(mouseX, mouseY, 152, 90, 16, 16)) {
            graphics.blit(TEXTURE, guiLeft + 152, guiTop + 90, 176, 32, 16, 16);
        }
        if (in(mouseX, mouseY, 134, 108, 16, 16)) {
            graphics.blit(TEXTURE, guiLeft + 134, guiTop + 108, 176, 48, 16, 16);
        }
        if (in(mouseX, mouseY, 8, 108, 16, 16)) {
            graphics.blit(TEXTURE, guiLeft + 8, guiTop + 108, 176, 64, 16, 16);
        }

        for (int i = pageIndex * 8; i < pageIndex * 8 + 40; i++) {
            if (i >= recipes.size()) break;
            int ind = i - pageIndex * 8;
            GenericRecipe recipe = recipes.get(i);
            if (recipe.getInternalName().equals(this.selection)) {
                graphics.blit(TEXTURE, guiLeft + 7 + 18 * (ind % 8), guiTop + 17 + 18 * (ind / 8), 192, 0, 18, 18);
            }
        }
        for (int i = pageIndex * 8; i < pageIndex * 8 + 40; i++) {
            if (i >= recipes.size()) break;
            int ind = i - pageIndex * 8;
            graphics.renderItem(recipes.get(i).getIcon(), guiLeft + 8 + 18 * (ind % 8), guiTop + 18 + 18 * (ind / 8));
        }
        if (this.selection != null && this.recipeSet.recipeNameMap.containsKey(selection)) {
            GenericRecipe recipe = this.recipeSet.recipeNameMap.get(selection);
            graphics.renderItem(recipe.getIcon(), guiLeft + 152, guiTop + 72);
        }
    }

    private void drawTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (in(mouseX, mouseY, 7, 17, 144, 90)) {
            for (int i = pageIndex * 8; i < pageIndex * 8 + 40; i++) {
                if (i >= this.recipes.size()) break;
                int ind = i - pageIndex * 8;
                int ix = 7 + 18 * (ind % 8);
                int iy = 17 + 18 * (ind / 8);
                if (in(mouseX, mouseY, ix, iy, 18, 18)) {
                    graphics.renderComponentTooltip(this.font, recipes.get(i).print(), mouseX, mouseY);
                }
            }
        }
        if (in(mouseX, mouseY, 151, 71, 18, 18)
                && this.selection != null && this.recipeSet.recipeNameMap.containsKey(selection)) {
            graphics.renderComponentTooltip(this.font, this.recipeSet.recipeNameMap.get(selection).print(), mouseX, mouseY);
        }
        if (in(mouseX, mouseY, 152, 90, 16, 16)) {
            graphics.renderTooltip(this.font, Component.literal("Close").withStyle(ChatFormatting.YELLOW), mouseX, mouseY);
        }
        if (in(mouseX, mouseY, 134, 108, 16, 16)) {
            graphics.renderTooltip(this.font, Component.literal("Clear search").withStyle(ChatFormatting.YELLOW), mouseX, mouseY);
        }
        if (in(mouseX, mouseY, 8, 108, 16, 16)) {
            graphics.renderTooltip(this.font, Component.literal("Press ENTER to toggle focus").withStyle(ChatFormatting.ITALIC), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0 && this.pageIndex > 0) {
            this.pageIndex--;
            return true;
        }
        if (scrollY < 0 && this.pageIndex < this.size) {
            this.pageIndex++;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX;
        int y = (int) mouseY;

        if (in(x, y, 152, 18, 16, 16)) {
            playClick();
            if (this.pageIndex > 0) this.pageIndex--;
            return true;
        }
        if (in(x, y, 152, 36, 16, 16)) {
            playClick();
            if (this.pageIndex < this.size) this.pageIndex++;
            return true;
        }
        if (in(x, y, 134, 108, 16, 16)) {
            this.search.setValue("");
            this.search.setFocused(true);
            return true;
        }

        for (int i = pageIndex * 8; i < pageIndex * 8 + 40; i++) {
            if (i >= this.recipes.size()) break;
            int ind = i - pageIndex * 8;
            int ix = 7 + 18 * (ind % 8);
            int iy = 17 + 18 * (ind / 8);
            if (in(x, y, ix, iy, 18, 18)) {
                String next = recipes.get(i).getInternalName();
                this.selection = next.equals(this.selection) ? NULL_SELECTION : next;
                playClick();
                return true;
            }
        }

        if (in(x, y, 151, 71, 18, 18) && !NULL_SELECTION.equals(this.selection)) {
            this.selection = NULL_SELECTION;
            playClick();
            return true;
        }

        if (in(x, y, 152, 90, 16, 16)) {
            this.onClose();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            this.search.setFocused(!this.search.isFocused());
            return true;
        }
        if (this.search.isFocused() && this.search.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE
                || this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.search.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        CompoundTag data = new CompoundTag();
        data.putInt("index", this.index);
        data.putString("selection", this.selection);
        PacketDistributor.sendToServer(new NBTControlPacket(this.pos, data));
        this.minecraft.setScreen(this.previousScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean in(double mouseX, double mouseY, int x, int y, int w, int h) {
        return guiLeft + x <= mouseX && guiLeft + x + w > mouseX && guiTop + y < mouseY && guiTop + y + h >= mouseY;
    }

    private static void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
    }
}
