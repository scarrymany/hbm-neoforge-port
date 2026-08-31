package com.hbm.inventory.gui.train;

import com.hbm.inventory.container.TrainCargoTramTrailerMenu;
import com.hbm.inventory.gui.GuiInfoContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for {@link TrainCargoTramTrailerMenu} - port of CE's {@code GUITrainCargoTramTrailer}. See
 * {@link TrainCargoTramScreen}'s class javadoc for why this class exists (review-pass fix: the menu
 * was fully wired and entity-reachable but had no bound client {@code Screen}) and
 * {@code com.hbm.main.VehicleCargoClientRegistry} for the registration.
 */
public class TrainCargoTramTrailerScreen extends GuiInfoContainer<TrainCargoTramTrailerMenu> {

    // CE's real asset lives under textures/gui/vehicles/, not .../train/ - see
    // docs/phase5/gui_screens_survey_weapons_storage_special.md Headline finding 2.
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/vehicles/gui_cargo_tram_trailer.png");

    public TrainCargoTramTrailerScreen(TrainCargoTramTrailerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // CE: 5x9 cargo grid, no battery slot, player inventory rows starting at y=140, hotbar at
        // y=198 - see TrainCargoTramTrailerMenu's own slot layout.
        // ySize is CE's real 222, not 224 - see the survey's Headline finding 5.
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
