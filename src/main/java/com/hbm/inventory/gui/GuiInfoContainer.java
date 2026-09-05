package com.hbm.inventory.gui;

import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.util.i18n.I18nUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * Shared {@link AbstractContainerScreen} base for every Phase 2 machine, ported from CE's
 * {@code com.hbm.inventory.gui.GuiInfoContainer} (376 lines, read in full):
 * {@link #drawElectricityInfo}, {@link #drawCustomInfo}/{@link #drawCustomInfoStat} (generic
 * hover-tooltip-on-AABB helpers used by nearly every CE machine GUI for upgrade-slot/power/status
 * tooltips), and {@link #drawInfoPanel} (the 12-icon {@code gui_utility.png} sprite-sheet blit -
 * small/large blue/green/red/yellow/grey I/!/* icons used for machine status hints).
 *
 * <p>Re-expressed against 1.21's {@link GuiGraphics} API rather than CE's raw
 * {@code drawHoveringText}/{@code drawTexturedModalRect} pair, following Neo Edition's real,
 * confirmed-compiling {@code com.hbm.inventory.screens.InfoScreen<T>} line-for-line (method bodies
 * included, not just signatures - this class differs from Neo Edition's only in package/class name
 * and the {@link Library}/{@link I18nUtil} call sites, per this port's own naming and i18n
 * conventions) - see {@code docs/phase2/gui_framework.md} decision 5/"Neo Edition (client-only shape
 * reference)" framing for why that file is trusted for API shape here.
 *
 * <p><b>Package naming</b>: kept as CE's own flat {@code com.hbm.inventory.gui} (this class) /
 * {@code com.hbm.inventory.container} ({@link com.hbm.inventory.container.MenuBase}) rather than Neo
 * Edition's {@code com.hbm.inventory.screens}/{@code .menus} rename - see
 * {@code docs/phase2/gui_framework.md} decision 7: nothing about NeoForge forces the Neo Edition
 * rename, and PORT_SPEC's "preserve {@code com.hbm.*} package layout where legal" rule favors CE's
 * own names.
 *
 * <h2>HE power / fluid-tank fields are read directly off the client-side block entity</h2>
 * See {@link com.hbm.inventory.container.MenuBase}'s class javadoc for the full sync-mechanism
 * writeup (confirmed against this port's already-shipped
 * {@code com.hbm.blockentity.LoadedBaseBlockEntity}/{@code MachineBaseBlockEntity}, not just Neo
 * Edition). The short version, repeated here because it is exactly what a machine {@code Screen}
 * subclass needs to know: a concrete {@code MachineFooScreen extends GuiInfoContainer<MachineFooMenu>}
 * calls {@code drawElectricityInfo(guiGraphics, mouseX, mouseY, x, y, w, h,
 * this.getMenu().be.power, MachineFooBlockEntity.MAX_POWER)} - reading {@code power} straight off
 * {@code this.getMenu().be} (the client-side block entity instance, kept correct by its own NBT sync,
 * not by this Menu/Screen pair) - exactly like CE's
 * {@code GUIMachineElectricFurnace.drawGuiContainerBackgroundLayer} reads {@code furnace.power}
 * directly and Neo Edition's {@code MachineCentrifugeScreen} reads {@code this.be.getPower()}
 * directly. There is no {@code ContainerData} field to bind and no menu-side power field to keep in
 * sync - do not add one.
 *
 * <p>Progress bars and energy bars are a hand-blit <i>convention</i>, not a widget this base class
 * provides (see {@code docs/phase2/gui_framework.md} decision 6 - CE never abstracted this in ~9
 * years across 56+ machines either): a machine {@code Screen} computes
 * {@code scaled = value * pixelSpan / max} (or calls a {@code getFooProgressScaled(pixelSpan)}
 * getter that already does so, matching CE's {@code getProgressScaled}/Neo Edition's
 * {@code getCentrifugeProgressScaled} naming) and blits a sub-rectangle of its own background
 * texture shifted by that many pixels via {@link GuiGraphics#blit}, the same way this class's own
 * {@link #drawInfoPanel} does for its fixed 12-icon sheet. Likewise, once Phase 2's own fluid-tank
 * class exists (deferred - see the GUI-framework report's Deferred scope), it should carry its own
 * {@code renderTank}/{@code renderTankTooltip} methods rather than this class growing a
 * {@code TankWidget}: CE's {@code FluidTankNTM} and Neo Edition's
 * {@code MachineFluidTankBlockEntity.tank} both render themselves, called directly from the owning
 * machine's {@code Screen}.
 */
public abstract class GuiInfoContainer<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    private static final ResourceLocation GUI_UTILITY =
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "textures/gui/gui_utility.png");

    protected GuiInfoContainer(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /**
     * Hover tooltip showing {@code power/maxPower}, formatted through {@link Library#getShortNumber}
     * (both sides confirmed still {@code long}-typed - see this class's own javadoc and
     * {@code docs/phase2/gui_framework.md} decision 3) so an 11-digit HE value doesn't wall the
     * tooltip in raw digits.
     */
    public void drawElectricityInfo(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height, long power, long maxPower) {
        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, x, y, width, height, mouseX, mouseY,
                Component.literal(Library.getShortNumber(power) + "/" + Library.getShortNumber(maxPower) + I18nUtil.resolveKey("he")));
    }

    /** Generic hover-tooltip-on-AABB helper, tooltip anchored at the mouse position. */
    public void drawCustomInfo(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height, Component... text) {
        this.drawCustomInfo(guiGraphics, mouseX, mouseY, x, y, width, height, Arrays.asList(text));
    }

    public void drawCustomInfo(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height, List<Component> text) {
        if (x <= mouseX && x + width > mouseX && y < mouseY && y + height >= mouseY) {
            guiGraphics.renderComponentTooltip(this.font, text, mouseX, mouseY);
        }
    }

    /** Same hover-tooltip-on-AABB check as {@link #drawCustomInfo}, but tooltip anchored at a caller-chosen position rather than the mouse. */
    public void drawCustomInfoStat(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height, int tPosX, int tPosY, Component... text) {
        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, x, y, width, height, tPosX, tPosY, Arrays.asList(text));
    }

    public void drawCustomInfoStat(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int width, int height, int tPosX, int tPosY, List<Component> text) {
        if (x <= mouseX && x + width > mouseX && y < mouseY && y + height >= mouseY) {
            guiGraphics.renderComponentTooltip(this.font, text, tPosX, tPosY);
        }
    }

    /**
     * Blits one of the 12 fixed icons out of {@code textures/gui/gui_utility.png} - small/large,
     * blue/green/red/yellow/grey I/!/* - used across CE's machine GUIs as cheap status hints.
     * <b>Asset note</b>: {@code gui_utility.png} itself has not been copied into this port's
     * resources yet (this pass is code-only, and this port currently has no
     * {@code assets/hbm/textures/**} tree at all - texture porting is a separate, later pass); until
     * it lands, this call renders NeoForge's missing-texture placeholder rather than crashing.
     */
    public void drawInfoPanel(GuiGraphics guiGraphics, int x, int y, int type) {
        switch (type) {
            case 0 -> guiGraphics.blit(GUI_UTILITY, x, y, 0, 0, 8, 8); // Small blue I
            case 1 -> guiGraphics.blit(GUI_UTILITY, x, y, 0, 8, 8, 8); // Small green I
            case 2 -> guiGraphics.blit(GUI_UTILITY, x, y, 8, 0, 16, 16); // Large blue I
            case 3 -> guiGraphics.blit(GUI_UTILITY, x, y, 24, 0, 16, 16); // Large green I
            case 4 -> guiGraphics.blit(GUI_UTILITY, x, y, 0, 16, 8, 8); // Small red !
            case 5 -> guiGraphics.blit(GUI_UTILITY, x, y, 0, 24, 8, 8); // Small yellow !
            case 6 -> guiGraphics.blit(GUI_UTILITY, x, y, 8, 16, 16, 16); // Large red !
            case 7 -> guiGraphics.blit(GUI_UTILITY, x, y, 24, 16, 16, 16); // Large yellow !
            case 8 -> guiGraphics.blit(GUI_UTILITY, x, y, 0, 32, 8, 8); // Small blue *
            case 9 -> guiGraphics.blit(GUI_UTILITY, x, y, 0, 40, 8, 8); // Small grey *
            case 10 -> guiGraphics.blit(GUI_UTILITY, x, y, 8, 32, 16, 16); // Large blue *
            case 11 -> guiGraphics.blit(GUI_UTILITY, x, y, 24, 32, 16, 16); // Large grey *
            default -> {
            }
        }
    }

    /** Hover check against on-screen coordinates (i.e. already offset by {@link #leftPos}/{@link #topPos}), for widgets that track their own screen-relative box. */
    protected boolean isHovered(double mouseX, double mouseY, int left, int top, int sizeX, int sizeY) {
        return leftPos + left <= mouseX && leftPos + left + sizeX > mouseX && topPos + top < mouseY && topPos + top + sizeY >= mouseY;
    }

    /** Plays the standard UI click sound, for machine GUI buttons that don't need a full {@code AbstractWidget}. */
    protected void click() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1F));
    }

    /**
     * CE {@code GUIMachineChemicalFactory.java:113-130} / {@code GuiInfoContainerProcessor.java:127-147}:
     * cycling ghost on empty input slots, then 50% slot-bg blit at z=300.
     */
    protected void renderGhostInputs(GuiGraphics graphics, ResourceLocation texture, GenericRecipe recipe, int[] inputSlots) {
        if (recipe == null || recipe.inputItem == null) return;
        int limit = Math.min(recipe.inputItem.length, inputSlots.length);
        for (int i = 0; i < limit; i++) {
            Slot slot = this.menu.getSlot(inputSlots[i]);
            if (slot.hasItem()) continue;
            ItemStack display = recipe.inputItem[i].extractForCyclingDisplay(20);
            if (display.isEmpty()) continue;
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 10);
            graphics.renderItem(display, this.leftPos + slot.x, this.topPos + slot.y);
            graphics.pose().popPose();
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.setColor(1F, 1F, 1F, 0.5F);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        for (int i = 0; i < limit; i++) {
            Slot slot = this.menu.getSlot(inputSlots[i]);
            if (slot.hasItem()) continue;
            graphics.blit(texture, this.leftPos + slot.x, this.topPos + slot.y, slot.x, slot.y, 16, 16);
        }
        graphics.pose().popPose();
        graphics.setColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }
}
