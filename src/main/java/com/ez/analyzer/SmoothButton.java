package com.ez.analyzer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Supplier;

public class SmoothButton extends ButtonWidget {
    private final int accent;
    private final Supplier<Float> alpha;
    private float hover = 0f;
    private long last = System.nanoTime();

    public SmoothButton(int x, int y, int w, int h, Text label, int accent,
                        Supplier<Float> alpha, PressAction onPress) {
        super(x, y, w, h, label, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.accent = accent;
        this.alpha = alpha;
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1_000_000_000f);
        last = now;
        float target = isHovered() ? 1f : 0f;
        hover += (target - hover) * (1f - (float) Math.exp(-dt * 14f));

        float a = Math.max(0f, Math.min(1f, alpha.get()));
        if (a < 0.02f) return;

        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int base = lerp(0x1E2230, 0x2E3550, hover);

        context.fill(x, y, x + w, y + h, AnalyzerScreen.argb(a, base));
        // accent underline grows on hover
        int lineW = (int) (w * (0.35f + 0.65f * hover));
        int lx = x + (w - lineW) / 2;
        context.fill(lx, y + h - 2, lx + lineW, y + h, AnalyzerScreen.argb(a, accent));

        int textColor = AnalyzerScreen.argb(a, lerp(0xC8CCD8, 0xFFFFFF, hover));
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,
                getMessage(), x + w / 2, y + (h - 8) / 2 - 1, textColor);
    }

    private static int lerp(int c1, int c2, float t) {
        int r = (int) (((c1 >> 16) & 0xFF) + ((((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t));
        int g = (int) (((c1 >> 8) & 0xFF) + ((((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t));
        int b = (int) ((c1 & 0xFF) + (((c2 & 0xFF) - (c1 & 0xFF)) * t));
        return (r << 16) | (g << 8) | b;
    }
}
