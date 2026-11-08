package com.ez.analyzer;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Random;

public class AnalyzerScreen extends Screen {
    private static final int PW = 220;
    private static final int PH = 168;
    private static final int GREEN = 0x4ADE80;
    private static final int RED = 0xF87171;
    private static final int ACCENT = 0x7C8CFF;

    private final long openTime = System.nanoTime();
    private long lastFrame = System.nanoTime();

    private float animRate = 0f;
    private float animWins = 0f;
    private float animLosses = 0f;
    private float flashWin = 0f;
    private float flashLoss = 0f;

    // rain
    private static final int DROPS = 50;
    private final Random rng = new Random();
    private final float[] rx = new float[DROPS];
    private final float[] ry = new float[DROPS];
    private final float[] rs = new float[DROPS]; // speed px/s
    private final float[] rd = new float[DROPS]; // depth 0.4..1
    private final float[] splash = new float[DROPS];

    public AnalyzerScreen() {
        super(Text.literal("EZ"));
        for (int i = 0; i < DROPS; i++) {
            respawn(i, true);
        }
    }

    private void respawn(int i, boolean randomY) {
        rx[i] = rng.nextFloat() * (PW + PH * 0.25f);
        ry[i] = randomY ? rng.nextFloat() * PH : -8f;
        rd[i] = 0.4f + rng.nextFloat() * 0.6f;
        rs[i] = 110f + rd[i] * 140f;
        splash[i] = 0f;
    }

    public static int argb(float alpha, int rgb) {
        int a = Math.max(4, Math.min(255, (int) (alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    private float openProgress() {
        float t = Math.min(1f, (System.nanoTime() - openTime) / 280_000_000f);
        float inv = 1f - t;
        return 1f - inv * inv * inv; // ease-out cubic
    }

    private int px() { return this.width / 2 - PW / 2; }
    private int py() { return this.height / 2 - PH / 2; }

    @Override
    protected void init() {
        int x = px();
        int y = py() + 108;

        addDrawableChild(new SmoothButton(x + 15, y, 90, 20, Text.literal("Record WIN"), GREEN,
                this::openProgress, b -> { GameTracker.add(GameTracker.Result.WIN); flashWin = 1f; }));
        addDrawableChild(new SmoothButton(x + 115, y, 90, 20, Text.literal("Record LOSS"), RED,
                this::openProgress, b -> { GameTracker.add(GameTracker.Result.LOSS); flashLoss = 1f; }));
        addDrawableChild(new SmoothButton(x + 15, y + 26, 90, 20, Text.literal("Clear history"), ACCENT,
                this::openProgress, b -> GameTracker.clear()));
        addDrawableChild(new SmoothButton(x + 115, y + 26, 90, 20, Text.literal("Close"), ACCENT,
                this::openProgress, b -> close()));
    }

    private void tick(float dt) {
        float k = 1f - (float) Math.exp(-dt * 9f);
        animRate += ((float) GameTracker.winRate() - animRate) * k;
        animWins += (GameTracker.wins() - animWins) * k;
        animLosses += (GameTracker.losses() - animLosses) * k;
        flashWin = Math.max(0f, flashWin - dt * 3f);
        flashLoss = Math.max(0f, flashLoss - dt * 3f);

        for (int i = 0; i < DROPS; i++) {
            if (splash[i] > 0f) {
                splash[i] -= dt * 4f;
                if (splash[i] <= 0f) respawn(i, false);
                continue;
            }
            ry[i] += rs[i] * dt;
            rx[i] -= rs[i] * dt * 0.25f; // slight slant
            if (ry[i] >= PH - 2) splash[i] = 1f;
            if (rx[i] < -8f) rx[i] += PW + 16f;
        }
    }

    private void drawRain(DrawContext context, int x, int y, float p) {
        context.enableScissor(x, y, x + PW, y + PH);
        for (int i = 0; i < DROPS; i++) {
            int dx = x + (int) rx[i];
            if (dx < x || dx >= x + PW) continue;
            if (splash[i] > 0f) {
                float s = 1f - splash[i];
                int w = 2 + (int) (s * 5f);
                context.fill(dx - w, y + PH - 2, dx + w, y + PH - 1, argb(p * 0.35f * splash[i], 0xA8C8FF));
                continue;
            }
            int dy = y + (int) ry[i];
            float a = p * 0.38f * rd[i];
            context.fill(dx, dy, dx + 1, dy + 3, argb(a, 0xA8C8FF));
            context.fill(dx + 1, dy + 3, dx + 2, dy + 6, argb(a * 0.6f, 0xA8C8FF));
        }
        context.disableScissor();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);

        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1_000_000_000f);
        lastFrame = now;
        tick(dt);

        float p = openProgress();
        int x = px();
        int y = py() + (int) ((1f - p) * 14f); // slide up

        // panel + border
        context.fill(x - 1, y - 1, x + PW + 1, y + PH + 1, argb(p, ACCENT));
        context.fill(x, y, x + PW, y + PH, argb(p * 0.96f, 0x12141C));
        context.fill(x, y, x + PW, y + 2, argb(p, ACCENT));

        drawRain(context, x, y, p);

        // win-rate bar
        int bx = x + 20, bw = PW - 40, by = y + 36;
        boolean any = GameTracker.total() > 0;
        context.fill(bx, by, bx + bw, by + 8, argb(p, any ? 0x5A2A2E : 0x262A38));
        int gw = (int) (bw * Math.max(0f, Math.min(100f, animRate)) / 100f);
        if (gw > 0) context.fill(bx, by, bx + gw, by + 8, argb(p, GREEN));
        context.fill(bx, by + 8, bx + bw, by + 9, argb(p * 0.4f, 0x000000));

        // history strip (last 20)
        List<GameTracker.Result> hist = GameTracker.history();
        int start = Math.max(0, hist.size() - 20);
        int sx = x + 20, sy = y + 88;
        for (int i = start; i < hist.size(); i++) {
            int col = hist.get(i) == GameTracker.Result.WIN ? GREEN : RED;
            int cx = sx + (i - start) * 9;
            boolean newest = i == hist.size() - 1;
            float glow = newest ? Math.max(flashWin, flashLoss) : 0f;
            if (glow > 0f) context.fill(cx - 1, sy - 1, cx + 7, sy + 7, argb(p * glow * 0.6f, col));
            context.fill(cx, sy, cx + 6, sy + 6, argb(p, col));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        float p = openProgress();
        if (p < 0.05f) return;
        int x = px();
        int y = py() + (int) ((1f - p) * 14f);
        int cx = this.width / 2;

        context.drawCenteredTextWithShadow(textRenderer, title, cx, y + 10, argb(p, 0xFFFFFF));
        context.drawCenteredTextWithShadow(textRenderer,
                String.format("%.1f%%", animRate), cx, y + 50, argb(p, 0xE6E8F0));

        int winCol = blend(0xB8E8C8, 0xFFFFFF, flashWin, GREEN);
        int lossCol = blend(0xF0C0C0, 0xFFFFFF, flashLoss, RED);
        context.drawTextWithShadow(textRenderer, "Wins " + Math.round(animWins), x + 20, y + 68,
                argb(p, flashWin > 0 ? winCol : GREEN));
        String ls = "Losses " + Math.round(animLosses);
        context.drawTextWithShadow(textRenderer, ls, x + PW - 20 - textRenderer.getWidth(ls), y + 68,
                argb(p, flashLoss > 0 ? lossCol : RED));
        context.drawCenteredTextWithShadow(textRenderer, "Games " + GameTracker.total(), cx, y + 68,
                argb(p, 0xAAAAAA));

        context.drawCenteredTextWithShadow(textRenderer,
                "Manual tracking only — does not change server outcomes.",
                cx, this.height - 16, argb(p * 0.7f, 0xAAAAAA));
    }

    private static int blend(int a, int b, float t, int fallback) {
        return t > 0f ? fallback : a;
    }
          }
