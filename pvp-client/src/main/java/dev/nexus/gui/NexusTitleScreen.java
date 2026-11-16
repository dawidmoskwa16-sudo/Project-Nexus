package dev.nexus.gui;

import dev.nexus.util.Rd;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Menu glowne Nexus: czarna dziura z dyskiem akrecyjnym, biel na czerni. */
public final class NexusTitleScreen extends Screen {
    private record Item(String label, Runnable action) {}

    private static final int BW = 150, BH = 18, GAP = 5;
    private static final int STARS = 200, DISK = 520, LENS = 150, INF = 70, DUST = 70;
    private static final float[] SX = new float[STARS], SY = new float[STARS], SP = new float[STARS], SD = new float[STARS];
    private static final float[] DR = new float[DISK], DA = new float[DISK], DB = new float[DISK];
    private static final float[] LR = new float[LENS], LA = new float[LENS], LB = new float[LENS];
    private static final float[] IA = new float[INF], IO = new float[INF], IS = new float[INF];
    private static final float[] UX = new float[DUST], UY = new float[DUST], US = new float[DUST];

    static {
        Random r = new Random(1337);
        for (int i = 0; i < STARS; i++) { SX[i] = r.nextFloat(); SY[i] = r.nextFloat(); SP[i] = 0.5f + r.nextFloat() * 2f; SD[i] = r.nextFloat(); }
        for (int i = 0; i < DISK; i++) { DR[i] = 1.25f + r.nextFloat() * 2.3f; DA[i] = r.nextFloat() * 6.2832f; DB[i] = 0.4f + r.nextFloat() * 0.6f; }
        for (int i = 0; i < LENS; i++) { LR[i] = r.nextFloat(); LA[i] = r.nextFloat() * 6.2832f; LB[i] = 0.4f + r.nextFloat() * 0.6f; }
        for (int i = 0; i < INF; i++) { IA[i] = r.nextFloat() * 6.2832f; IO[i] = r.nextFloat(); IS[i] = 0.6f + r.nextFloat() * 0.8f; }
        for (int i = 0; i < DUST; i++) { UX[i] = r.nextFloat(); UY[i] = r.nextFloat(); US[i] = 0.3f + r.nextFloat(); }
    }

    private final List<Item> items = new ArrayList<>();
    private final float[] hover = new float[8];
    private final long start = System.nanoTime();
    private long last = System.nanoTime();

    public NexusTitleScreen() {
        super(Text.literal("Nexus"));
        items.add(new Item("SINGLEPLAYER", () -> client.setScreen(new SelectWorldScreen(this))));
        items.add(new Item("MULTIPLAYER", () -> client.setScreen(new MultiplayerScreen(this))));
        items.add(new Item("COSMETICS", () -> client.setScreen(new CosmeticsScreen(this))));
        items.add(new Item("OPTIONS", () -> client.setScreen(new OptionsScreen(this, client.options))));
        items.add(new Item("QUIT", () -> client.scheduleStop()));
    }

    @Override public boolean shouldCloseOnEsc() { return false; }

    private int holeY() { return (int) (height * 0.34f); }
    private int holeR() { return Math.max(18, Math.min(width, height) / 9); }
    private int buttonsTop() {
        return Math.min(holeY() + holeR() + 28, height - 14 - items.size() * (BH + GAP));
    }

    private static void disk(DrawContext c, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.sqrt((double) r * r - (double) dy * dy);
            c.fill(cx - half, cy + dy, cx + half, cy + dy + 1, color);
        }
    }

    private static void ring(DrawContext c, int cx, int cy, int ro, int ri, int color) {
        for (int dy = -ro; dy <= ro; dy++) {
            int ho = (int) Math.sqrt((double) ro * ro - (double) dy * dy);
            if (Math.abs(dy) >= ri) {
                c.fill(cx - ho, cy + dy, cx + ho, cy + dy + 1, color);
            } else {
                int hi = (int) Math.sqrt((double) ri * ri - (double) dy * dy);
                c.fill(cx - ho, cy + dy, cx - hi, cy + dy + 1, color);
                c.fill(cx + hi, cy + dy, cx + ho, cy + dy + 1, color);
            }
        }
    }

    private static void dot(DrawContext c, int x, int y, int size, int color) {
        c.fill(x, y, x + size, y + size, color);
    }

    private void diskParticles(DrawContext c, int cx, int cy, int R, float t, boolean front) {
        for (int i = 0; i < DISK; i++) {
            float pr = DR[i];
            float w = 0.8f / (pr * (float) Math.sqrt(pr));
            float a = DA[i] + t * w;
            float s = (float) Math.sin(a);
            if ((s >= 0) != front) continue;
            float heat = Math.max(0f, 1f - (pr - 1.25f) / 2.3f);
            float dop = 0.6f + 0.4f * (float) Math.cos(a);
            int col = Rd.mix(0xFF8A8A8A, 0xFFFFFFFF, heat);
            for (int k = 0; k < 3; k++) {
                float ak = a - k * 0.07f / pr;
                int x = cx + Math.round((float) Math.cos(ak) * pr * R);
                int y = cy + Math.round((float) Math.sin(ak) * pr * R * 0.27f);
                float al = DB[i] * dop * (0.35f + 0.65f * heat) * (k == 0 ? 1f : 0.4f / k);
                dot(c, x, y, k == 0 && heat > 0.6f ? 2 : 1, Rd.withAlpha(col, al));
            }
        }
    }

    private void infall(DrawContext c, int cx, int cy, int R, float t, boolean front) {
        for (int i = 0; i < INF; i++) {
            float ph = (t * 0.12f * IS[i] + IO[i]) % 1f;
            float rad = R * (3.7f - 2.65f * ph);
            float ang = IA[i] + ph * ph * 14f;
            float s = (float) Math.sin(ang);
            if ((s >= 0) != front) continue;
            int x = cx + Math.round((float) Math.cos(ang) * rad);
            int y = cy + Math.round(s * rad * 0.27f);
            dot(c, x, y, 1, Rd.withAlpha(0xFFFFFFFF, (float) Math.sin(ph * Math.PI) * 0.9f));
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        long now = System.nanoTime();
        float dt = Math.min((now - last) / 1e9f, 0.1f);
        last = now;
        float t = (now - start) / 1e9f;

        ctx.fill(0, 0, width, height, 0xFF000000);
        float px = (mx - width / 2f) / width, py = (my - height / 2f) / height;
        for (int i = 0; i < STARS; i++) {
            float tw = 0.3f + 0.7f * (0.5f + 0.5f * (float) Math.sin(t * SP[i] + i));
            int x = (int) (SX[i] * width - px * SD[i] * 18f), y = (int) (SY[i] * height - py * SD[i] * 18f);
            dot(ctx, x, y, SD[i] > 0.85f ? 2 : 1, Rd.withAlpha(0xFFFFFFFF, tw * (0.35f + 0.5f * SD[i])));
        }
        for (int i = 0; i < DUST; i++) {
            float fy = ((UY[i] - t * US[i] * 0.02f) % 1f + 1f) % 1f;
            int x = (int) (UX[i] * width + Math.sin(t * 0.3f + i) * 10f), y = (int) (fy * height);
            dot(ctx, x, y, 1, Rd.withAlpha(0xFFFFFFFF, 0.12f + 0.18f * (float) Math.sin(t + i * 1.7f) * (float) Math.sin(t + i * 1.7f)));
        }

        int cx = width / 2, cy = holeY(), R = holeR();
        float pulse = 0.85f + 0.15f * (float) Math.sin(t * 1.3f);
        for (int k = 8; k >= 0; k--) disk(ctx, cx, cy, R + k * 7, Rd.withAlpha(0xFFFFFFFF, 0.03f * pulse));
        diskParticles(ctx, cx, cy, R, t, false);
        infall(ctx, cx, cy, R, t, false);
        disk(ctx, cx, cy, R, 0xFF000000);
        ring(ctx, cx, cy, R + 2, R, Rd.withAlpha(0xFFFFFFFF, 0.55f + 0.45f * pulse));
        ring(ctx, cx, cy, R + 5, R + 2, Rd.withAlpha(0xFFFFFFFF, 0.22f * pulse));
        for (int i = 0; i < LENS; i++) {
            float ang = LA[i] + t * 0.15f;
            float rr = R * (1.05f + LR[i] * 0.55f);
            dot(ctx, cx + Math.round((float) Math.cos(ang) * rr), cy + Math.round((float) Math.sin(ang) * rr), 1,
                    Rd.withAlpha(0xFFFFFFFF, LB[i] * (1f - LR[i]) * 0.8f));
        }
        diskParticles(ctx, cx, cy, R, t, true);
        infall(ctx, cx, cy, R, t, true);

        String title = "P V P   C L I E N T";
        float tsc = Rd.width(title) * 3f > width - 20 ? 2f : 3f;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx, cy - 12f);
        m.scale(tsc, tsc);
        int tx = -Rd.width(title) / 2;
        Rd.text(ctx, title, tx - 1, 0, Rd.withAlpha(0xFFFFFFFF, 0.18f));
        Rd.text(ctx, title, tx + 1, 0, Rd.withAlpha(0xFFFFFFFF, 0.18f));
        Rd.text(ctx, title, tx, 0, 0xFFFFFFFF);
        m.popMatrix();
        String sub = "M I N E C R A F T   1 . 2 1 . 1 1";
        Rd.text(ctx, sub, cx - Rd.width(sub) / 2, cy + R + 14, 0xFF7A7A7A);

        int y = buttonsTop(), x = (width - BW) / 2;
        for (int i = 0; i < items.size(); i++) {
            boolean hov = Rd.in(mx, my, x, y, BW, BH);
            hover[i] += ((hov ? 1f : 0f) - hover[i]) * Math.min(1f, dt * 14f);
            float h = hover[i];
            ctx.fill(x, y, x + BW, y + BH, Rd.mix(0xFF2E2E2E, 0xFFFFFFFF, h));
            ctx.fill(x + 1, y + 1, x + BW - 1, y + BH - 1, Rd.mix(0xFF0A0A0A, 0xFF1C1C1C, h));
            String l = items.get(i).label();
            Rd.text(ctx, l, x + (BW - Rd.width(l)) / 2, y + 5, Rd.mix(0xFFB0B0B0, 0xFFFFFFFF, h));
            int bar = (int) ((BW - 2) * h / 2f);
            ctx.fill(x + (BW / 2) - bar, y + BH - 2, x + (BW / 2) + bar, y + BH - 1, 0xFFFFFFFF);
            y += BH + GAP;
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return false;
        int y = buttonsTop(), x = (width - BW) / 2;
        for (Item it : items) {
            if (Rd.in(click.x(), click.y(), x, y, BW, BH)) {
                it.action().run();
                return true;
            }
            y += BH + GAP;
        }
        return false;
    }
}
