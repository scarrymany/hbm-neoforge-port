package com.hbm.inventory.gui;

import com.hbm.blockentity.bomb.LaunchPadBaseBlockEntity;
import com.hbm.inventory.container.LaunchPadMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for {@link LaunchPadMenu} (shared by the small and large launch pad, matching CE's own
 * {@code TileEntityLaunchPadBase.provideGUI} always returning {@code GUILaunchPadLarge}). Ported
 * from CE's {@code GUILaunchPadLarge} (151 lines, read in full for this review pass - see
 * {@code docs/phase5/gui_screens_survey_weapons_storage_special.md} Headline finding 4, which
 * originally surfaced these gaps against the same file at the signature/layout level only).
 * <p>
 * <b>Review-pass fixes</b>, all against CE's exact pixel numbers:
 * <ul>
 *   <li>Texture path corrected from the invented {@code textures/gui/bomb/gui_launch_pad.png} to
 *   CE's real {@code textures/gui/weapon/gui_launch_pad_large.png} (survey Headline finding 2 -
 *   wrong folder <i>and</i> wrong filename, so the asset-copy pass alone would never have fixed
 *   this screen).</li>
 *   <li>{@code imageHeight} corrected from 222 to CE's real {@code ySize} of 236.</li>
 *   <li>The two fuel/oxidizer tanks were rendered at height 48 (bottom at {@code topPos+108}); CE's
 *   real geometry is height 52, bottom at {@code topPos+88} - fixed to match, and the power bar
 *   (previously only an 18x18 tooltip hitbox with no visible fill at all) now occupies the matching
 *   column at {@code x=107} immediately to their left, exactly as CE lays all three out
 *   contiguously (107/125/143).</li>
 *   <li>Added the power-fill bar itself (CE: {@code drawTexturedModalRect(guiLeft+107,
 *   guiTop+88-power, 176, 52-power, 16, power)}) - previously invisible in the GUI entirely.</li>
 *   <li>Added the fuel-present/oxidizer-present/launch-ready 6x8 status icons at
 *   {@link LaunchPadBaseBlockEntity#getFuelState()}/{@link LaunchPadBaseBlockEntity#getOxidizerState()}/
 *   {@link LaunchPadBaseBlockEntity#isMissileValid()} (this report's own open question 5, now
 *   resolved by reading the block entity directly: those exact accessor names already exist).</li>
 *   <li>Added the {@code state} text readout ({@code STATE_MISSING}/{@code STATE_LOADING}/
 *   {@code STATE_READY} -&gt; "Not ready"/"Loading..."/"Ready", CE's own red/orange/green colors) -
 *   drawn at normal scale rather than CE's {@code GlStateManager.scale} shrink, since this port's
 *   {@link GuiGraphics} has no direct matrix-scale equivalent in use elsewhere in this file set.</li>
 * </ul>
 * <p>
 * <b>Not ported</b> (both explicitly named as cross-report/deferred in the survey, not a gap this
 * screen alone can close): the hover-cycling tooltip over the empty designator slot listing the 3
 * compatible designator item types (needs a {@code drawStackText}-equivalent helper this port's
 * {@link GuiInfoContainer} does not have yet - the same gap {@code TurretScreen}'s own javadoc
 * documents for CE's {@code drawAmmo}); and the live 3D missile-preview render
 * ({@code ItemRenderMissileGeneric.renderers}) - depends on whichever Phase 5 area builds the
 * missile/weapon item-renderer registry, not yet present in this port.
 */
public class LaunchPadScreen extends GuiInfoContainer<LaunchPadMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("hbm", "textures/gui/weapon/gui_launch_pad_large.png");

    private final LaunchPadBaseBlockEntity pad;

    public LaunchPadScreen(LaunchPadMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.pad = menu.be;
        this.imageWidth = 176;
        this.imageHeight = 236;
    }

    // FluidTankNTM#renderTank draws via a raw Tesselator quad in absolute screen space (no
    // GuiGraphics pose-stack integration - confirmed by reading its body), so this passes
    // this.leftPos/this.topPos-relative absolute coordinates directly rather than a pushed
    // transform. Per its own javadoc, "y" is the tank's BOTTOM edge, not its top.
    // CE (GUILaunchPadLarge.java): power bar at x=107, tank0 at x=125, tank1 at x=143, all sharing
    // the same bottom edge topPos+88 and max height 52 - contiguous columns.
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 52;
    private static final int TANK0_X = 125;
    private static final int TANK1_X = 143;
    private static final int BAR_BOTTOM_Y = 88;

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        int fuel = pad.getFuelState();
        int oxidizer = pad.getOxidizerState();
        if (fuel == 1) guiGraphics.blit(TEXTURE, x + 130, y + 23, 192, 0, 6, 8);
        if (fuel == -1) guiGraphics.blit(TEXTURE, x + 130, y + 23, 198, 0, 6, 8);
        if (oxidizer == 1) guiGraphics.blit(TEXTURE, x + 148, y + 23, 192, 0, 6, 8);
        if (oxidizer == -1) guiGraphics.blit(TEXTURE, x + 148, y + 23, 198, 0, 6, 8);
        if (pad.isMissileValid()) {
            guiGraphics.blit(TEXTURE, x + 112, y + 23, pad.getPower() >= 75_000 ? 192 : 198, 0, 6, 8);
        }

        long maxPower = pad.getMaxPower();
        int power = maxPower > 0 ? (int) (pad.getPower() * TANK_HEIGHT / maxPower) : 0;
        if (power > 0) {
            guiGraphics.blit(TEXTURE, x + 107, y + BAR_BOTTOM_Y - power, 176, TANK_HEIGHT - power, 16, power);
        }

        pad.tanks[0].renderTank(x + TANK0_X, y + BAR_BOTTOM_Y, 0, TANK_WIDTH, TANK_HEIGHT);
        pad.tanks[1].renderTank(x + TANK1_X, y + BAR_BOTTOM_Y, 0, TANK_WIDTH, TANK_HEIGHT);

        String text;
        int color;
        switch (pad.state) {
            case LaunchPadBaseBlockEntity.STATE_LOADING -> {
                text = "Loading...";
                color = 0xFF8000;
            }
            case LaunchPadBaseBlockEntity.STATE_READY -> {
                text = "Ready";
                color = 0x00FF00;
            }
            default -> {
                text = "Not ready";
                color = 0xFF0000;
            }
        }
        guiGraphics.drawString(this.font, text, x + 34 - this.font.width(text) / 2, y + 103, color, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        this.drawElectricityInfo(guiGraphics, mouseX, mouseY, this.leftPos + 107, this.topPos + BAR_BOTTOM_Y - TANK_HEIGHT, 16, TANK_HEIGHT, pad.getPower(), pad.getMaxPower());
        pad.tanks[0].renderTankTooltip(guiGraphics, mouseX, mouseY, this.leftPos + TANK0_X, this.topPos + BAR_BOTTOM_Y - TANK_HEIGHT, TANK_WIDTH, TANK_HEIGHT);
        pad.tanks[1].renderTankTooltip(guiGraphics, mouseX, mouseY, this.leftPos + TANK1_X, this.topPos + BAR_BOTTOM_Y - TANK_HEIGHT, TANK_WIDTH, TANK_HEIGHT);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 8, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
