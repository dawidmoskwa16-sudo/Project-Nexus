package dev.nexus.gui;

import dev.nexus.NexusClient;
import dev.nexus.config.ConfigManager;
import dev.nexus.cosmetics.Cosmetic;
import dev.nexus.module.Module;
import dev.nexus.module.render.Capes;
import dev.nexus.module.render.Halo;
import dev.nexus.module.render.Wings;
import dev.nexus.util.Rd;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Menu Cosmetics: zaklada peleryny, skrzydla i aureole (te same ustawienia co w ClickGUI). */
public final class CosmeticsScreen extends Screen {
    private static final String[] TABS = {"Peleryny", "Skrzydła", "Aureole"};
    private static final int CARD_H = 72, GAP = 7, MIN_CARD = 150, HEADER = 30;
    private static final int PREVIEW = 50;

    private final Screen parent;
    private final List<Cosmetic> cosmetics = new ArrayList<>();
    private int tab;
    private float scroll;
    private int px, py, pw, ph;

    public CosmeticsScreen(Screen parent) {
        super(Text.literal("Cosmetics"));
        this.parent = parent;
        Capes c = NexusClient.modules().get(Capes.class);
        Wings w = NexusClient.modules().get(Wings.class);
        Halo h = NexusClient.modules().get(Halo.class);
        cosmetics.add(c);
        cosmetics.add(w);
        cosmetics.add(h);
        for (Cosmetic k : cosmetics) k.refresh();
    }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void removed() { ConfigManager.save(); }

    private void layout() {
        pw = Math.min(width - 8, Math.max(300, Math.min(620, (int) (width * 0.8))));
        ph = Math.min(height - 8, Math.max(190, Math.min(380, (int) (height * 0.82))));
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    private Cosmetic current() { return cosmetics.get(tab); }
    private int cols() { return Math.max(1, (pw - 16 + GAP) / (MIN_CARD + GAP)); }
    private int cardW() { int c = cols(); return (pw - 16 - GAP * (c - 1)) / c; }
    private int gridTop() { return py + HEADER + 10; }
    private int gridBottom() { return py + ph - 44; }
    private int tabW(int i) { return Rd.width(TABS[i]) + 28; }
    private int tabX(int i) {
        int total = 0;
        for (int k = 0; k < TABS.length; k++) total += tabW(k);
        int x = px + (pw - total) / 2;
        for (int k = 0; k < i; k++) x += tabW(k);
        return x;
    }

    private int[] cardRect(int i) {
        int c = cols(), w = cardW();
        return new int[]{px + 8 + (i % c) * (w + GAP), gridTop() - Math.round(scroll) + (i / c) * (CARD_H + GAP), w, CARD_H};
    }

    private int maxScroll() {
        int rows = (current().options().size() + cols() - 1) / cols();
        return Math.max(0, rows * (CARD_H + GAP) - (gridBottom() - gridTop()));
    }

    private boolean isEquipped(Cosmetic c, String name) {
        return c.module().isEnabled() && c.selected().equals(name);
    }

    private void toggleOption(Cosmetic c, String name) {
        Module m = c.module();
        if (isEquipped(c, name)) {
            m.setEnabled(false, true);
        } else {
            c.select(name);
            if (!m.isEnabled()) m.setEnabled(true, true);
        }
    }

    /** Rysuje mały, czytelny podgląd rodzaju kosmetyku bez potrzeby zakładania go. */
    private void drawPreview(DrawContext ctx, int x, int y, int size, int type, String name) {
        Rd.rrect(ctx, x, y, size, size, 5, 0xFF090A10);
        Rd.rrect(ctx, x + 1, y + 1, size - 2, size - 2, 4, 0xFF171923);
        int accent = previewAccent(name);

        if (type == 0) {
            // Peleryna: zwężający się materiał z pasami i emblematem.
            int cx = x + size / 2;
            int top = y + 7;
            ctx.fill(cx - 10, top, cx + 10, top + 5, 0xFFB9BBC8);
            for (int row = 0; row < 7; row++) {
                int half = 12 - row;
                int yy = top + 5 + row * 5;
                ctx.fill(cx - half, yy, cx + half, yy + 5, row % 2 == 0 ? accent : Rd.mix(accent, 0xFF080A12, 0.3f));
            }
            ctx.fill(cx - 1, top + 14, cx + 2, top + 27, 0xFFE8EAF2);
        } else if (type == 1) {
            // Skrzydla: symetryczne piorka po obu stronach.
            int cy = y + size / 2;
            for (int i = 0; i < 5; i++) {
                int yy = cy - 18 + i * 8;
                int len = 11 + i * 3;
                int col = i % 2 == 0 ? accent : Rd.mix(accent, 0xFFFFFFFF, 0.28f);
                ctx.fill(x + size / 2 - len, yy, x + size / 2 - 2, yy + 5, col);
                ctx.fill(x + size / 2 + 2, yy, x + size / 2 + len, yy + 5, col);
            }
            ctx.fill(x + size / 2 - 2, cy - 15, x + size / 2 + 2, cy + 19, 0xFFE6E8F0);
        } else {
            // Aureola: świetlisty pierścień nad małą sylwetką głowy.
            int cx = x + size / 2, cy = y + size / 2 - 2;
            for (int i = 0; i < 48; i++) {
                double a = Math.PI * 2.0 * i / 48.0;
                int rx = cx + (int) Math.round(Math.cos(a) * 16);
                int ry = cy + (int) Math.round(Math.sin(a) * 9);
                ctx.fill(rx - 1, ry - 1, rx + 2, ry + 2, Rd.withAlpha(accent, 150));
            }
            ctx.fill(cx - 6, cy + 7, cx + 6, cy + 19, 0xFFBFC4D2);
            ctx.fill(cx - 9, cy + 17, cx + 9, cy + 21, 0xFF8A91A3);
        }
    }

    private static int previewAccent(String name) {
        String n = name.toLowerCase(java.util.Locale.ROOT);
        if (n.contains("neon")) return 0xFF42DDF5;
        if (n.contains("srebr") || n.contains("frost")) return 0xFFD5DCEB;
        if (n.contains("złot") || n.contains("gold") || n.contains("nexus")) return 0xFFFFC94A;
        if (n.contains("eclipse") || n.contains("nietoperz")) return 0xFF9A83E8;
        if (n.contains("kryształ") || n.contains("crystal")) return 0xFF79CFFF;
        return Rd.mix(0xFF55A7FF, 0xFFFF68B5, (name.hashCode() & 255) / 255f);
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        layout();
        int acc = Theme.accent();
        ctx.fillGradient(0, 0, width, height, 0xFF050508, 0xFF10101A);

        Rd.rrect(ctx, px - 1, py - 1, pw + 2, ph + 2, 7, 0xFF34343E);
        Rd.rrect(ctx, px, py, pw, ph, 6, 0xF2111116);
        ctx.fill(px + 1, py + HEADER, px + pw - 1, py + HEADER + 1, 0xFF24242C);
        Rd.text(ctx, "Cosmetics", px + 10, py + 11, 0xFFFFFFFF);
        for (int i = 0; i < TABS.length; i++) {
            int tx = tabX(i), tw = tabW(i);
            boolean on = tab == i;
            boolean hov = Rd.in(mx, my, tx, py, tw, HEADER);
            Rd.text(ctx, TABS[i], tx + 14, py + 11, on ? 0xFFFFFFFF : hov ? 0xFFCFCFD8 : 0xFF8E8E9A);
            if (on) ctx.fill(tx + 4, py + HEADER - 2, tx + tw - 4, py + HEADER, acc);
        }

        Cosmetic c = current();
        List<String> opts = c.options();
        scroll = Math.max(0, Math.min(maxScroll(), scroll));
        int top = gridTop(), bottom = gridBottom();
        ctx.enableScissor(px + 1, top, px + pw - 1, bottom);
        for (int i = 0; i < opts.size(); i++) {
            int[] r = cardRect(i);
            if (r[1] + r[3] < top || r[1] > bottom) continue;
            String name = opts.get(i);
            boolean eq = isEquipped(c, name);
            boolean hov = Rd.in(mx, my, r[0], r[1], r[2], r[3]) && my >= top && my < bottom;
            Rd.rrect(ctx, r[0], r[1], r[2], r[3], 5, eq ? acc : 0xFF30303A);
            Rd.rrect(ctx, r[0] + 1, r[1] + 1, r[2] - 2, r[3] - 2, 4, hov ? 0xFF1E1E26 : 0xFF17171D);

            drawPreview(ctx, r[0] + 8, r[1] + 10, PREVIEW, tab, name);
            int tx = r[0] + PREVIEW + 18;
            Rd.text(ctx, name, tx, r[1] + 12, 0xFFFFFFFF);
            Rd.text(ctx, c.builtins().contains(name) ? "Wbudowana" : "Własny plik", tx, r[1] + 27, 0xFF7A7A86);
            Rd.text(ctx, eq ? "Założona – kliknij, aby zdjąć" : "Kliknij, aby założyć", tx, r[1] + 44,
                    eq ? acc : 0xFF8E8E9A);
        }
        ctx.disableScissor();

        Rd.text(ctx, "Własne pliki: " + c.folderHint(), px + 10, py + ph - 38, 0xFF7A7A86);
        button(ctx, px + 8, py + ph - 24, 96, 16, "Odśwież pliki", mx, my);
        button(ctx, px + pw - 8 - 70, py + ph - 24, 70, 16, "Wstecz", mx, my);
    }

    private static void button(DrawContext ctx, int x, int y, int w, int h, String label, int mx, int my) {
        boolean hov = Rd.in(mx, my, x, y, w, h);
        Rd.rrect(ctx, x, y, w, h, 4, hov ? 0xFF5A5A68 : 0xFF34343E);
        Rd.rrect(ctx, x + 1, y + 1, w - 2, h - 2, 3, hov ? 0xFF22222A : 0xFF18181E);
        Rd.text(ctx, label, x + (w - Rd.width(label)) / 2, y + 4, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return false;
        layout();
        double mx = click.x(), my = click.y();
        for (int i = 0; i < TABS.length; i++) {
            if (Rd.in(mx, my, tabX(i), py, tabW(i), HEADER)) { tab = i; scroll = 0; return true; }
        }
        if (Rd.in(mx, my, px + pw - 78, py + ph - 24, 70, 16)) { client.setScreen(parent); return true; }
        if (Rd.in(mx, my, px + 8, py + ph - 24, 96, 16)) { current().refresh(); return true; }
        if (my >= gridTop() && my < gridBottom()) {
            List<String> opts = current().options();
            for (int i = 0; i < opts.size(); i++) {
                int[] r = cardRect(i);
                if (Rd.in(mx, my, r[0], r[1], r[2], r[3])) {
                    toggleOption(current(), opts.get(i));
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll -= (float) (vertical * 14);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            client.setScreen(parent);
            return true;
        }
        return super.keyPressed(input);
    }
}
