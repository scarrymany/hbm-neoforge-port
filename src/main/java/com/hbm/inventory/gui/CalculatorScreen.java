package com.hbm.inventory.gui;

import com.hbm.util.Calculator;
import com.hbm.util.Tuple;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

/**
 * Client-only, containerless GUI for the calculator tool, opened by
 * {@code HbmKeybinds.calculatorKey} from {@code HbmKeybindInputEvents.onClientTick} - ported from
 * CE's {@code com.hbm.inventory.gui.GUICalculator} (100 lines, read in full). Expression evaluation
 * itself lives in {@link Calculator#evaluateExpression(String)} (a byte-for-byte port of CE's
 * {@code com.hbm.util.Calculator}, already committed in Phase 0 and verified identical to CE's real
 * source by this pass - not duplicated here, matching CE's own {@code GUICalculator}/{@code
 * Calculator} package split).
 * <p>
 * <b>CE's real UI has no on-screen buttons</b>: {@code GUICalculator} is a single always-focused
 * {@code GuiTextField} plus a live "={result}" preview line and a scrolling command-history list
 * (up/down arrows browse it, Enter re-evaluates) - there is no button grid anywhere in CE's source.
 * This is confirmed independently by the (incomplete, non-authoritative per this port's ground
 * rules) {@code upstream/neo-edition} port's own {@code CalculatorScreen}, which reaches the same
 * conclusion and also renders text-field-only. This class therefore reproduces that exact CE design
 * (same panel geometry, same live-preview/history mechanics) rather than the button grid the task
 * brief for this file assumed CE has; per this port's ground rule that CE is the sole source of
 * truth for behavior/content (and this codebase's own precedent in {@link SatInterfaceScreen}, whose
 * javadoc removed an earlier invented readout for the same reason), CE's actual, verified design
 * wins over that assumption. Flagged explicitly here and in this task's hand-off notes.
 * <p>
 * <b>1.12.2 to 1.21.1 mechanical translation notes</b> (behavior unchanged from CE):
 * <ul>
 *   <li>CE's single combined {@code keyTyped(char, int)} callback (LWJGL2) is split here into
 *   {@link #keyPressed(int, int, int)} (control/navigation keys, Enter, arrow-key history browsing -
 *   forwarded to {@link #inputField} exactly like CE forwards to its {@code GuiTextField} first) and
 *   {@link #charTyped(char, int)} (printable-character insertion) per vanilla 1.21.1's GLFW-backed
 *   input model. Both paths re-run the same post-keystroke bookkeeping CE runs unconditionally
 *   after every keystroke (history-selection reset/live-preview recompute), so a printable key still
 *   ends up recomputing the preview off the freshly-inserted text exactly once character insertion
 *   has actually happened, matching CE's net observable behavior.</li>
 *   <li>CE's {@code Keyboard.enableRepeatEvents(true/false)} (an LWJGL2-only opt-in for held-key
 *   repeat) has no equivalent call needed here - modern GLFW input already repeat-fires
 *   {@link #keyPressed(int, int, int)} for a held key, which is what {@code EditBox} relies on.</li>
 *   <li>{@code GuiScreen.setClipboardString} to {@code Minecraft.getInstance().keyboardHandler
 *   .setClipboard(String)}; {@code GuiTextField#setCursorPositionEnd()}/{@code setSelectionPos(0)}
 *   (which together select the whole field) to {@link EditBox#moveCursorToEnd(boolean)}/{@link
 *   EditBox#setHighlightPos(int)}.</li>
 *   <li>Panel/text drawing (CE's {@code drawRect}/{@code fontRenderer.drawString}) to {@link
 *   GuiGraphics#fill}/{@link GuiGraphics#drawString} at identical coordinates and colors.</li>
 *   <li>{@link #render(GuiGraphics, int, int, float)} opens with {@code super.render(...)}, mirroring
 *   both CE's own {@code super.drawScreen(...)} call at the top of {@code drawScreen} and this port's
 *   other {@code Screen} subclasses ({@link SatInterfaceScreen}, {@link SatCoordScreen}, {@link
 *   DesignatorManualScreen}, {@link com.hbm.inventory.gui.turret.TurretMobFilterScreen}) - not a
 *   direct {@code renderBackground(...)} call, which no other screen in this port relies on and which
 *   this pass could not confirm still exists with that exact signature on 1.21.1's {@code Screen}
 *   without a buildable jar to check against (this class registers no renderable widgets, so the
 *   base-class call is otherwise a no-op here).</li>
 * </ul>
 */
public class CalculatorScreen extends Screen {

    private static final int SIZE_X = 220;
    private static final int SIZE_Y = 50;
    private static final int BORDER_WIDTH = 2;
    private static final int MAX_HISTORY = 6;

    /** Persists for the life of the client JVM, exactly like CE's own static {@code history} field. */
    private static final Deque<Tuple.Pair<String, Double>> history = new ArrayDeque<>();

    private EditBox inputField;
    private int selectedHist = -1;
    private String latestResult = "?";

    public CalculatorScreen() {
        super(Component.literal("Calculator"));
    }

    @Override
    protected void init() {
        int x = (this.width - SIZE_X) / 2;
        int y = (this.height - SIZE_Y) / 2;

        this.inputField = new EditBox(this.font, x + 5, y + 8, 210, 13, Component.literal("Calculator input"));
        this.inputField.setTextColor(-1);
        this.inputField.setCanLoseFocus(false);
        this.inputField.setFocused(true);
        this.inputField.setMaxLength(1000);
        // Not registered via addRenderableWidget/addWidget: rendering and input are dispatched to it
        // explicitly below so this screen's draw order can match CE's exactly (panel background,
        // then the field on top of it, then the result/history text) - see class javadoc.
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.inputField.charTyped(codePoint, modifiers)) {
            this.selectedHist = -1;
            recomputePreview();
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean consumed = this.inputField.keyPressed(keyCode, scanCode, modifiers);

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            onEnter();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_UP) {
            this.selectedHist = Math.max(this.selectedHist - 1, -1);
        } else if (keyCode == GLFW.GLFW_KEY_DOWN) {
            this.selectedHist = Math.min(this.selectedHist + 1, history.size() - 1);
        } else {
            this.selectedHist = -1;
        }

        recomputePreview();
        return consumed || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // CE's GUICalculator never overrides mouseClicked (its field can't be mouse-repositioned at
        // all), but wiring click-to-position here is a harmless, non-content-altering vanilla-widget
        // default this port's other EditBox-based screens already rely on.
        if (this.inputField.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** CE: the {@code typedChar == 13 || typedChar == 10} branch of {@code keyTyped}. */
    private void onEnter() {
        if (this.selectedHist != -1) {
            String input = new ArrayList<>(history).get(this.selectedHist).key;
            this.inputField.setValue(input);
            this.selectedHist = -1;
            recomputePreview();
            return;
        }

        String input = filtered(this.inputField.getValue());
        try {
            double result = Calculator.evaluateExpression(input);
            history.addFirst(new Tuple.Pair<>(input, result));
            if (history.size() > MAX_HISTORY) history.removeLast();

            String plainStringRepresentation = new BigDecimal(result, MathContext.DECIMAL64).toPlainString();
            Minecraft.getInstance().keyboardHandler.setClipboard(plainStringRepresentation);
            this.inputField.setValue(plainStringRepresentation);
            this.inputField.moveCursorToEnd(false);
            this.inputField.setHighlightPos(0);
        } catch (Exception ignored) {
            // CE: caught and silently ignored - invalid expressions simply don't commit.
        }
    }

    /** CE: the tail of {@code keyTyped} that recomputes the live "={result}" preview line. */
    private void recomputePreview() {
        String input = filtered(this.inputField.getValue());
        if (input.isEmpty()) {
            this.latestResult = "?";
            return;
        }
        try {
            this.latestResult = Double.toString(Calculator.evaluateExpression(input));
        } catch (Exception e) {
            this.latestResult = e.toString();
        }
    }

    private static String filtered(String raw) {
        return raw.replaceAll("[^\\d+\\-*/^!.()\\sA-Za-z]+", "");
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int x = (this.width - SIZE_X) / 2;
        int y = (this.height - SIZE_Y) / 2;
        int histHeight = (this.font.lineHeight + 2) * MAX_HISTORY;
        int histStart = y + 30 + this.font.lineHeight + 8;

        guiGraphics.fill(x, y, x + SIZE_X, y + SIZE_Y + histHeight, 0xFF2d2d2d);
        guiGraphics.fill(x + BORDER_WIDTH, y + BORDER_WIDTH, x + SIZE_X - BORDER_WIDTH, y + SIZE_Y - BORDER_WIDTH + histHeight, 0xFF3d3d3d);
        guiGraphics.fill(x, histStart - 5, x + SIZE_X, histStart - 3, 0xFF2d2d2d);

        this.inputField.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawString(this.font, "=" + this.latestResult, x + 5, y + 30, -1);

        int i = 0;
        for (Tuple.Pair<String, Double> prevInput : history) {
            int hy = y + 50 + (this.font.lineHeight + 1) * i;
            if (i == this.selectedHist) {
                guiGraphics.fill(x + 4, hy - 1, x + 4 + SIZE_X - 9, hy + this.font.lineHeight, 0xFF111111);
            }
            guiGraphics.drawString(this.font, prevInput.key + " = " + prevInput.value, x + 5, hy, -1);
            i++;
        }
    }
}
