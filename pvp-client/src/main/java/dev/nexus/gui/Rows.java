package dev.nexus.gui;

import dev.nexus.module.Module;
import dev.nexus.setting.*;
import dev.nexus.util.KeyNames;
import dev.nexus.util.Rd;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

/** Wiersze ustawien: przelaczniki, suwaki z galka, listy, kolory, bindy. */
public final class Rows {
    public static Row dragging;
    public static BindRow listening;

    private Rows() {}

    public static Row of(Setting<?> s) {
        if (s instanceof BooleanSetting b) return new BoolRow(b);
        if (s instanceof NumberSetting n) return new NumberRow(n);
        if (s instanceof ModeSetting m) return new ModeRow(m);
        if (s instanceof ColorSetting c) return new ColorRow(c);
        throw new IllegalArgumentException("Nieobslugiwany typ ustawienia: " + s.getClass());
    }

    private static int a(int c, float e) { return Rd.withAlpha(c, e); }

    public abstract static class Row {
        protected int rx, rw;
        protected float anim, hov;

        public abstract int height();
        public abstract void render(DrawContext ctx, int x, int y, int w, int mx, int my, float e);
        public boolean click(int x, int y, int w, double mx, double my, int btn) { return false; }
        public void drag(double mx) {}

        protected static float approach(float cur, float target) {
            float v = cur + (target - cur) * 0.25f;
            return Math.abs(target - v) < 0.005f ? target : v;
        }

        protected void frame(DrawContext ctx, int x, int y, int w, int mx, int my, float e) {
            hov = approach(hov, Rd.in(mx, my, x, y, w, height()) ? 1f : 0f);
            ctx.fill(x, y, x + w, y + height(), a(Rd.mix(0xFF0C0C12, 0xFF16161E, hov), e * 0.8f));
            ctx.fill(x + 4, y + height() - 1, x + w - 4, y + height(), a(0xFF1E1E26, e));
        }
    }

    public static final class BoolRow extends Row {
        private final BooleanSetting s;
        BoolRow(BooleanSetting s) { this.s = s; anim = s.get() ? 1f : 0f; }

        @Override public int height() { return 15; }

        @Override
        public void render(DrawContext ctx, int x, int y, int w, int mx, int my, float e) {
            frame(ctx, x, y, w, mx, my, e);
            anim = approach(anim, s.get() ? 1f : 0f);
            Rd.text(ctx, s.getName(), x + 6, y + 4, a(Rd.mix(0xFF9A9A9A, 0xFFFFFFFF, Math.max(anim, hov * 0.6f)), e));
            int tw = 18, th = 9, tx = x + w - 8 - tw, ty = y + 3;
            ctx.fill(tx, ty, tx + tw, ty + th, a(Rd.mix(0xFF2A2A2A, Theme.accent(), anim), e));
            int kx = tx + 1 + Math.round((tw - th) * anim);
            ctx.fill(kx, ty + 1, kx + th - 2, ty + th - 1, a(Rd.mix(0xFFB0B0B0, 0xFFFFFFFF, anim), e));
        }

        @Override
        public boolean click(int x, int y, int w, double mx, double my, int btn) {
            if (btn == 0 && Rd.in(mx, my, x, y, w, height())) { s.toggle(); return true; }
            return false;
        }
    }

    public static final class NumberRow extends Row {
        private final NumberSetting s;
        NumberRow(NumberSetting s) { this.s = s; anim = (float) s.percent(); }

        @Override public int height() { return 23; }

        private String fmt() {
            double v = s.get();
            if (s.getStep() >= 1) return Integer.toString((int) Math.round(v));
            return String.format(Locale.ROOT, s.getStep() < 0.1 ? "%.2f" : "%.1f", v);
        }

        @Override
        public void render(DrawContext ctx, int x, int y, int w, int mx, int my, float e) {
            rx = x; rw = w;
            frame(ctx, x, y, w, mx, my, e);
            boolean active = dragging == this;
            anim = approach(anim, (float) s.percent());
            Rd.text(ctx, s.getName(), x + 6, y + 3, a(Rd.mix(0xFF9A9A9A, 0xFFFFFFFF, Math.max(hov, active ? 1f : 0f)), e));
            String v = fmt();
            Rd.text(ctx, v, x + w - 6 - Rd.width(v), y + 3, a(0xFFFFFFFF, e));
            int bx = x + 7, bw = w - 14, by = y + 16;
            int fw = Math.round(bw * anim);
            ctx.fill(bx, by, bx + bw, by + 2, a(0xFF2A2A2A, e));
            ctx.fill(bx, by, bx + fw, by + 2, a(Theme.accent(), e));
            int k = active ? 3 : 2;
            ctx.fill(bx + fw - k, by - k + 1, bx + fw + k, by + k + 1, a(0xFFFFFFFF, e));
        }

        @Override
        public boolean click(int x, int y, int w, double mx, double my, int btn) {
            if (btn == 0 && Rd.in(mx, my, x, y, w, height())) {
                rx = x; rw = w;
                dragging = this;
                drag(mx);
                return true;
            }
            return false;
        }

        @Override
        public void drag(double mx) {
            double p = Math.max(0, Math.min(1, (mx - (rx + 7)) / (double) (rw - 14)));
            s.set(s.getMin() + p * (s.getMax() - s.getMin()));
        }
    }

    public static final class ModeRow extends Row {
        private final ModeSetting s;
        ModeRow(ModeSetting s) { this.s = s; }

        @Override public int height() { return 15; }

        @Override
        public void render(DrawContext ctx, int x, int y, int w, int mx, int my, float e) {
            frame(ctx, x, y, w, mx, my, e);
            Rd.text(ctx, s.getName(), x + 6, y + 4, a(Rd.mix(0xFF9A9A9A, 0xFFFFFFFF, hov * 0.6f), e));
            String v = s.get();
            int vw = Rd.width(v);
            int rx2 = x + w - 6;
            Rd.text(ctx, ">", rx2 - 5, y + 4, a(0xFF6A6A6A, e));
            Rd.text(ctx, v, rx2 - 9 - vw, y + 4, a(0xFFFFFFFF, e));
            Rd.text(ctx, "<", rx2 - 17 - vw, y + 4, a(0xFF6A6A6A, e));
        }

        @Override
        public boolean click(int x, int y, int w, double mx, double my, int btn) {
            if (!Rd.in(mx, my, x, y, w, height())) return false;
            if (btn == 0) { s.cycle(1); return true; }
            if (btn == 1) { s.cycle(-1); return true; }
            return false;
        }
    }

    public static final class ColorRow extends Row {
        private final ColorSetting s;
        private int channel;
        private static final char[] NAMES = {'R', 'G', 'B'};

        ColorRow(ColorSetting s) { this.s = s; }

        @Override public int height() { return 15 + 27 + 3; }

        @Override
        public void render(DrawContext ctx, int x, int y, int w, int mx, int my, float e) {
            rx = x; rw = w;
            frame(ctx, x, y, w, mx, my, e);
            Rd.text(ctx, s.getName(), x + 6, y + 4, a(0xFFB0B0B0, e));
            ctx.fill(x + w - 22, y + 3, x + w - 6, y + 12, a(0xFFFFFFFF, e));
            ctx.fill(x + w - 21, y + 4, x + w - 7, y + 11, a(s.argb(), e));
            for (int i = 0; i < 3; i++) {
                int shift = 16 - 8 * i;
                int v = (s.get() >> shift) & 0xFF;
                int by = y + 15 + i * 9;
                Rd.text(ctx, String.valueOf(NAMES[i]), x + 6, by, a(0xFF6A6A6A, e));
                int bx = x + 18, bw = w - 26, fw = (int) (bw * (v / 255.0));
                ctx.fill(bx, by + 3, bx + bw, by + 5, a(0xFF2A2A2A, e));
                ctx.fill(bx, by + 3, bx + fw, by + 5, a(0xFFFFFFFF, e));
                ctx.fill(bx + fw - 1, by + 1, bx + fw + 2, by + 7, a(0xFFFFFFFF, e));
            }
        }

        @Override
        public boolean click(int x, int y, int w, double mx, double my, int btn) {
            if (btn != 0 || !Rd.in(mx, my, x, y + 15, w, 27)) return false;
            rx = x; rw = w;
            channel = Math.max(0, Math.min(2, (int) ((my - (y + 15)) / 9)));
            dragging = this;
            drag(mx);
            return true;
        }

        @Override
        public void drag(double mx) {
            double p = (mx - (rx + 18)) / (double) (rw - 26);
            int v = (int) Math.round(Math.max(0, Math.min(1, p)) * 255);
            int shift = 16 - 8 * channel;
            s.set((s.get() & ~(0xFF << shift)) | (v << shift));
        }
    }

    public static final class BindRow extends Row {
        private final Module module;
        BindRow(Module m) { this.module = m; }

        public Module module() { return module; }

        @Override public int height() { return 17; }

        @Override
        public void render(DrawContext ctx, int x, int y, int w, int mx, int my, float e) {
            frame(ctx, x, y, w, mx, my, e);
            Rd.text(ctx, "Bind", x + 6, y + 5, a(0xFF9A9A9A, e));
            boolean on = listening == this;
            String v = on ? "..." : KeyNames.name(module.getKey());
            int bw = Rd.width(v) + 10, bx = x + w - 6 - bw;
            ctx.fill(bx, y + 2, bx + bw, y + 15, a(on ? 0xFFFFFFFF : 0xFF2E2E2E, e));
            ctx.fill(bx + 1, y + 3, bx + bw - 1, y + 14, a(0xFF0A0A0A, e));
            Rd.text(ctx, v, bx + 5, y + 5, a(on ? 0xFFFFFFFF : 0xFFC8C8C8, e));
        }

        @Override
        public boolean click(int x, int y, int w, double mx, double my, int btn) {
            if (btn == 0 && Rd.in(mx, my, x, y, w, height())) {
                listening = listening == this ? null : this;
                return true;
            }
            return false;
        }
    }
}
