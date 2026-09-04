package com.hbm.inventory.gui.machine;

import com.hbm.blockentity.machine.MachineCrucibleBlockEntity;
import com.hbm.inventory.container.machine.MachineCrucibleMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.CrucibleRecipe;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.packet.toserver.CrucibleControlPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported (structurally, from CE's {@code GUICrucible}, canvas 176x214, read in full) as a plain
 * panel with hand-blit bars, following {@code MachineShredderScreen}'s established "no texture asset
 * exists yet for any Phase 2+ machine screen" convention (see that class's own javadoc). Progress
 * bar at {@code (126,82)} 33px, heat bar at {@code (126,91)} 33px, both horizontal fills - CE's exact
 * coordinates. The two vertical material-stack fill bars ({@code (62,97)}/{@code (17,97)}, 34px wide
 * x up to 79px tall, stacked bottom-up per material layer) are hand-blit solid-color rectangles keyed
 * off each material's {@code moltenColor} rather than CE's textured tank-fill sprite (no
 * {@code gui_crucible.png} exists in this port yet either).
 * <p>
 * <b>Recipe picker</b>: CE opens the shared, 331-line {@code GUIScreenRecipeSelector} popup (not
 * ported anywhere in this port yet - see {@code docs/phase7/crucible_core.md}'s "Recommended shape"
 * #7). This screen instead implements the recommended minimal Crucible-specific picker: left-click
 * the recipe-icon zone at {@code (106,80,18,18)} to cycle to the next registered recipe (wrapping),
 * right-click to clear back to "no recipe", both sent server-side via the new
 * {@link CrucibleControlPacket}/{@code IControlReceiver} pair. Hovering (not clicking) the same zone
 * shows the loaded recipe's full input/output printout via {@link CrucibleRecipe#print()}, or a
 * "click to set recipe" hint - matching CE's dual hover/click behavior on that same zone.
 */
public class MachineCrucibleScreen extends GuiInfoContainer<MachineCrucibleMenu> {

    private static final int RECIPE_ZONE_X = 106;
    private static final int RECIPE_ZONE_Y = 80;
    private static final int RECIPE_ZONE_SIZE = 18;

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
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xFFC6C6C6);

        MachineCrucibleBlockEntity be = this.getMenu().be;

        int pGauge = (int) (33L * be.getProgress() / MachineCrucibleBlockEntity.processTime);
        if (pGauge > 0) guiGraphics.fill(x + 126, y + 82, x + 126 + pGauge, y + 87, 0xFF00A000);

        int hGauge = (int) (33L * be.getHeat() / MachineCrucibleBlockEntity.maxHeat);
        if (hGauge > 0) guiGraphics.fill(x + 126, y + 91, x + 126 + hGauge, y + 96, 0xFFFF4500);

        // Recipe-select zone.
        guiGraphics.fill(x + RECIPE_ZONE_X, y + RECIPE_ZONE_Y, x + RECIPE_ZONE_X + RECIPE_ZONE_SIZE, y + RECIPE_ZONE_Y + RECIPE_ZONE_SIZE, 0xFF4A4A4A);

        drawStackBar(guiGraphics, be.getRecipeStack(), MachineCrucibleBlockEntity.recipeZCapacity, x + 62, y + 97);
        drawStackBar(guiGraphics, be.getWasteStack(), MachineCrucibleBlockEntity.wasteZCapacity, x + 17, y + 97);
    }

    /** CE: {@code GUICrucible.drawStack} (textured) - re-expressed as solid-color rectangles per material layer, keyed off {@link NTMMaterial#moltenColor}. */
    private void drawStackBar(GuiGraphics guiGraphics, List<Mats.MaterialStack> stack, int capacity, int x, int bottomY) {
        if (stack.isEmpty() || capacity <= 0) return;

        int lastHeight = 0;
        int lastQuant = 0;

        for (Mats.MaterialStack sta : stack) {
            int targetHeight = (int) (79L * (lastQuant + sta.amount) / capacity);
            if (lastHeight == targetHeight) continue;

            int color = 0xFF000000 | (sta.material.moltenColor & 0xFFFFFF);
            guiGraphics.fill(x, bottomY - targetHeight, x + 34, bottomY - lastHeight, color);

            lastQuant += sta.amount;
            lastHeight = targetHeight;
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        MachineCrucibleBlockEntity be = this.getMenu().be;

        drawCustomInfoStat(guiGraphics, mouseX, mouseY, 16, 17, 36, 81, mouseX, mouseY, stackTooltip(be.getWasteStack()));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, 61, 17, 36, 81, mouseX, mouseY, stackTooltip(be.getRecipeStack()));

        drawCustomInfoStat(guiGraphics, mouseX, mouseY, 126, 82, 33, 5, mouseX, mouseY,
                Component.literal(be.getProgress() + " / " + MachineCrucibleBlockEntity.processTime + " TU"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, 126, 91, 33, 5, mouseX, mouseY,
                Component.literal(be.getHeat() + " / " + MachineCrucibleBlockEntity.maxHeat + " TU"));

        if (RECIPE_ZONE_X <= mouseX && RECIPE_ZONE_X + RECIPE_ZONE_SIZE > mouseX && RECIPE_ZONE_Y < mouseY && RECIPE_ZONE_Y + RECIPE_ZONE_SIZE >= mouseY) {
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
                    .append(Mats.formatAmount(sta.amount, Screen.hasShiftDown())).withStyle(ChatFormatting.YELLOW));
        }
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, RECIPE_ZONE_X, RECIPE_ZONE_Y, RECIPE_ZONE_SIZE, RECIPE_ZONE_SIZE)) {
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
}
