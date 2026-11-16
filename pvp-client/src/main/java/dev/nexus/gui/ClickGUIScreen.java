package dev.nexus.gui;

import dev.nexus.NexusClient;
import dev.nexus.config.ConfigManager;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.module.misc.ClickGUIModule;
import dev.nexus.module.render.HudEditor;
import dev.nexus.setting.Setting;
import dev.nexus.util.Rd;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Glowne menu klienta: zakladki, kategorie, siatka kart modulow i panel ustawien. */
public final class ClickGUIScreen extends Screen {
    private record Card(String title, String status, boolean on, String desc, Module module,
                        Runnable click, Runnable rightClick) {}

    private static final String[] TABS = {"Mody", "Profil", "Ustawienia"};
    private static final int HEADER = 30, CARD_H = 58, GAP = 6, MIN_CARD = 104;

    private int tab;
    private Category filter;
    private Module selected;
    private final Map<Module, List<Rows.Row>> rowCache = new HashMap<>();
    private final Map<Module, Float> switchAnim = new HashMap<>();
    private List<Rows.Row> settingsRows;
    private List<String> profiles = new ArrayList<>();
    private final StringBuilder search = new StringBuilder();

    private float anim, scrollC, scrollR;
    private boolean closing, closed;
    private long last = System.nanoTime();

    // uklad (liczony w layout())
    private int px, py, pw, ph, sideW, rightW, cx, cw, rx;

    public ClickGUIScreen() {
        super(Text.literal("PvP Client"));
        Rows.dragging = null;
        Rows.listening = null;
        profiles = ConfigManager.listProfiles();
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void close() { closing = true; }

    @Override
    public void removed() {
        ConfigManager.save();
        Rows.dragging = null;
        Rows.listening = null;
    }

    // ------------------------------------------------------------------ uklad

    private void layout(float slide) {
        pw = Math.min(Math.min(width - 8, 780), Math.max(340, (int) (width * 0.84)));
        ph = Math.min(Math.min(height - 8, 460), Math.max(200, (int) (height * 0.82)));
        px = (width - pw) / 2;
        py = (height - ph) / 2 + Math.round(slide);
        sideW = Math.max(80, pw / 5);
        rightW = Math.max(112, pw * 27 / 100);
        cx = px + sideW;
        cw = pw - sideW - rightW;
        rx = cx + cw;
    }

    private String query() { return search.toString().toLowerCase(Locale.ROOT); }
    private int titleBlock() { return ph >= 300 ? 46 : 36; }
    private int gridTop() { return py + HEADER + titleBlock(); }
    private int gridBottom() { return py + ph - 6; }
    private int cols() { return Math.max(1, (cw - 16 + GAP) / (MIN_CARD + GAP)); }
    private int cardW() { int c = cols(); return (cw - 16 - GAP * (c - 1)) / c; }

    private int tabW(int i) { return Rd.width(TABS[i]) + 28; }
    private int tabX(int i) {
        int total = 0;
        for (int k = 0; k < TABS.length; k++) total += tabW(k);
        int x = px + (pw - total) / 2;
        for (int k = 0; k < i; k++) x += tabW(k);
        return x;
    }

    private List<Module> visibleModules() {
        String q = query();
        List<Module> out = new ArrayList<>();
        for (Module m : NexusClient.modules().getAll()) {
            if (m instanceof ClickGUIModule || m instanceof HudEditor) continue;
            if (filter != null && m.getCategory() != filter) continue;
            if (!q.isEmpty() && !m.getName().toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(m);
        }
        return out;
    }

    private List<Card> cards() {
        List<Card> out = new ArrayList<>();
        if (tab == 0) {
            for (Module m : visibleModules()) {
                out.add(new Card(m.getName(), m.isEnabled() ? "Włączony" : "Wyłączony", m.isEnabled(),
                        m.getDescription(), m, () -> { selected = m; scrollR = 0; }, null));
            }
        } else if (tab == 1) {
            String active = ConfigManager.getActive();
            for (String n : profiles) {
                out.add(new Card(n, n.equals(active) ? "Aktywny" : "Profil", n.equals(active),
                        "Kliknij, aby wczytać. PPM: usuń.", null,
                        () -> { ConfigManager.save(); ConfigManager.load(n); },
                        () -> ConfigManager.deleteProfile(n)));
            }
            out.add(new Card("+ Nowy profil", "Zapisz bieżące", false, "Tworzy kopię obecnych ustawień.", null,
                    () -> ConfigManager.createProfile(ConfigManager.nextProfileName()), null));
        }
        return out;
    }

    private int[] cardRect(int i) {
        int c = cols(), w = cardW();
        return new int[]{cx + 8 + (i % c) * (w + GAP), gridTop() - Math.round(scrollC) + (i / c) * (CARD_H + GAP), w, CARD_H};
    }

    private List<Rows.Row> rowsFor(Module m) {
        return rowCache.computeIfAbsent(m, k -> {
            List<Rows.Row> list = new ArrayList<>();
            list.add(new ModuleToggleRow(k));
            for (Setting<?> s : k.getSettings()) list.add(Rows.of(s));
            list.add(new Rows.BindRow(k));
            return list;
        });
    }

    private List<Rows.Row> settingsTabRows() {
        if (settingsRows == null) {
            settingsRows = new ArrayList<>();
            ClickGUIModule g = NexusClient.modules().get(ClickGUIModule.class);
            if (g != null) for (Setting<?> s : g.getSettings()) settingsRows.add(Rows.of(s));
        }
        return settingsRows;
    }

    private static int rowsHeight(List<Rows.Row> rows) {
        int h = 0;
        for (Rows.Row r : rows) h += r.height();
        return h;
    }

    private List<OrderedText> wrap(String s, int w) {
        List<OrderedText> l = client.textRenderer.wrapLines(Text.literal(s), w);
        return l.size() > 3 ? l.subList(0, 3) : l;
    }

    private int rightRowsTop() {
        int top = py + HEADER + 8 + 16;
        if (tab == 0 && selected != null) top += wrap(selected.getDescription(), rightW - 20).size() * 10 + 8;
        return top;
    }

    private int centerMaxScroll() {
        int view = gridBottom() - gridTop();
        int content = tab == 2 ? rowsHeight(settingsTabRows())
                : ((cards().size() + cols() - 1) / cols()) * (CARD_H + GAP);
        return Math.max(0, content - view);
    }

    // ------------------------------------------------------------------ rysowanie

    private static void bigText(DrawContext ctx, String s, int x, int y, float sc, int color) {
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(sc, sc);
        Rd.text(ctx, s, 0, 0, color);
        m.popMatrix();
    }

    private static void drawSwitch(DrawContext ctx, int x, int y, float t, float e) {
        Rd.rrect(ctx, x, y, 20, 10, 5, Rd.withAlpha(Rd.mix(0xFF30303A, Theme.accent(), t), e));
        Rd.rrect(ctx, x + 1 + Math.round(10 * t), y + 1, 8, 8, 4, Rd.withAlpha(0xFFFFFFFF, e));
    }

    private void renderRows(DrawContext ctx, List<Rows.Row> rows, int x, int y, int w, int top, int bottom, int mx, int my, float e) {
        ctx.enableScissor(x, top, x + w, bottom);
        int yy = y;
        for (Rows.Row r : rows) {
            if (yy + r.height() >= top && yy <= bottom) r.render(ctx, x, yy, w, mx, my, e);
            yy += r.height();
        }
        ctx.disableScissor();
    }

    private boolean clickRows(List<Rows.Row> rows, int x, int y, int w, int top, int bottom, double mx, double my, int btn) {
        if (my < top || my >= bottom || mx < x || mx >= x + w) return false;
        int yy = y;
        for (Rows.Row r : rows) {
            if (r.click(x, yy, w, mx, my, btn)) return true;
            yy += r.height();
        }
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min((now - last) / 1_000_000_000f, 0.1f);
        last = now;
        ClickGUIModule cfg = NexusClient.modules().get(ClickGUIModule.class);
        float speed = cfg == null ? 6f : cfg.animSpeed.floatValue();
        float target = closing ? 0f : 1f;
        if (anim < target) anim = Math.min(target, anim + dt * speed);
        else if (anim > target) anim = Math.max(target, anim - dt * speed);
        if (closing && anim <= 0.001f) {
            if (!closed) { closed = true; client.execute(() -> client.setScreen(null)); }
            return;
        }
        float e = 1f - (float) Math.pow(1f - anim, 3);
        int mx = mouseX, my = mouseY;
        layout((1f - e) * 14f);
        int acc = Theme.accent();

        if (cfg == null || cfg.dim.get()) ctx.fill(0, 0, width, height, Rd.withAlpha(0xFF000000, 0.5f * e));

        // panel
        Rd.rrect(ctx, px - 1, py - 1, pw + 2, ph + 2, 7, Rd.withAlpha(0xFF34343E, e));
        Rd.rrect(ctx, px, py, pw, ph, 6, Rd.withAlpha(0xFF111116, e * Theme.opacity()));
        ctx.fill(px + 1, py + HEADER, px + pw - 1, py + HEADER + 1, Rd.withAlpha(0xFF24242C, e));
        ctx.fill(cx, py + HEADER + 1, cx + 1, py + ph - 1, Rd.withAlpha(0xFF24242C, e));
        ctx.fill(rx, py + HEADER + 1, rx + 1, py + ph - 1, Rd.withAlpha(0xFF24242C, e));

        // naglowek
        Rd.rrect(ctx, px + 10, py + 8, 16, 14, 3, Rd.withAlpha(acc, e));
        Rd.text(ctx, "P", px + 15, py + 11, Rd.withAlpha(0xFFFFFFFF, e));
        bigText(ctx, "PvP Client", px + 32, py + 8, 1.5f, Rd.withAlpha(0xFFFFFFFF, e));
        for (int i = 0; i < TABS.length; i++) {
            int tx = tabX(i), tw = tabW(i);
            boolean on = tab == i;
            boolean hov = Rd.in(mx, my, tx, py, tw, HEADER);
            Rd.text(ctx, TABS[i], tx + 14, py + 11, Rd.withAlpha(on ? 0xFFFFFFFF : hov ? 0xFFCFCFD8 : 0xFF8E8E9A, e));
            if (on) ctx.fill(tx + 4, py + HEADER - 2, tx + tw - 4, py + HEADER, Rd.withAlpha(acc, e));
        }
        boolean hx = Rd.in(mx, my, px + pw - 24, py + 6, 18, 18);
        Rd.text(ctx, "X", px + pw - 19, py + 11, Rd.withAlpha(hx ? 0xFFFFFFFF : 0xFF8E8E9A, e));

        renderSidebar(ctx, mx, my, e);
        renderCenter(ctx, mx, my, e);
        renderRight(ctx, mx, my, e);
    }

    private void renderSidebar(DrawContext ctx, int mx, int my, float e) {
        int acc = Theme.accent();
        int x = px + 8, w = sideW - 16, y = py + HEADER + 8;
        if (tab == 0) {
            Rd.rrect(ctx, x, y, w, 17, 4, Rd.withAlpha(0xFF34343E, e));
            Rd.rrect(ctx, x + 1, y + 1, w - 2, 15, 3, Rd.withAlpha(0xFF18181E, e));
            boolean empty = search.length() == 0;
            String shown = empty ? "Szukaj modułu" : search + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
            Rd.text(ctx, shown, x + 6, y + 5, Rd.withAlpha(empty ? 0xFF7A7A86 : 0xFFFFFFFF, e));

            String[] names = new String[Category.values().length + 1];
            names[0] = "Wszystkie";
            for (int i = 0; i < Category.values().length; i++) names[i + 1] = Category.values()[i].getDisplayName();
            int ey = y + 26;
            for (int i = 0; i < names.length; i++) {
                boolean on = (i == 0 && filter == null) || (i > 0 && filter == Category.values()[i - 1]);
                boolean hov = Rd.in(mx, my, px + 4, ey, sideW - 8, 20);
                if (on) {
                    Rd.rrect(ctx, px + 4, ey, sideW - 8, 20, 4, Rd.withAlpha(Rd.withAlpha(acc, 0.18f), e));
                    ctx.fill(px + 4, ey + 4, px + 6, ey + 16, Rd.withAlpha(acc, e));
                } else if (hov) {
                    Rd.rrect(ctx, px + 4, ey, sideW - 8, 20, 4, Rd.withAlpha(0x14FFFFFF, e));
                }
                Rd.text(ctx, names[i], px + 14, ey + 6, Rd.withAlpha(on ? 0xFFFFFFFF : 0xFF9A9AA6, e));
                ey += 22;
            }
        }
        int by = py + ph - 26;
        boolean hov = Rd.in(mx, my, x, by, w, 18);
        Rd.rrect(ctx, x, by, w, 18, 4, Rd.withAlpha(hov ? 0xFF5A5A68 : 0xFF34343E, e));
        Rd.rrect(ctx, x + 1, by + 1, w - 2, 16, 3, Rd.withAlpha(hov ? 0xFF22222A : 0xFF18181E, e));
        String t = "Edytuj HUD";
        Rd.text(ctx, t, x + (w - Rd.width(t)) / 2, by + 5, Rd.withAlpha(0xFFFFFFFF, e));
    }

    private void renderCenter(DrawContext ctx, int mx, int my, float e) {
        String[] titles = {"Twój interfejs.", "Profile", "Ustawienia"};
        String[] subs = {"Wszystko na swoim miejscu.", "Zapisane konfiguracje modułów i HUD.", "Wygląd i animacje menu."};
        float sc = ph >= 300 ? 2f : 1.5f;
        int ty = py + HEADER + 8;
        bigText(ctx, titles[tab], cx + 10, ty, sc, Rd.withAlpha(0xFFFFFFFF, e));
        Rd.text(ctx, subs[tab], cx + 10, ty + (int) (8 * sc) + 3, Rd.withAlpha(0xFF8E8E9A, e));

        int top = gridTop(), bottom = gridBottom();
        scrollC = Math.max(0, Math.min(centerMaxScroll(), scrollC));
        if (tab == 2) {
            renderRows(ctx, settingsTabRows(), cx + 8, top - Math.round(scrollC), cw - 16, top, bottom, mx, my, e);
            return;
        }
        int acc = Theme.accent();
        List<Card> cards = cards();
        ctx.enableScissor(cx + 1, top, rx, bottom);
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            int[] r = cardRect(i);
            if (r[1] + r[3] < top || r[1] > bottom) continue;
            boolean sel = tab == 0 && c.module() == selected;
            boolean hov = Rd.in(mx, my, r[0], r[1], r[2], r[3]) && my >= top && my < bottom;
            Rd.rrect(ctx, r[0], r[1], r[2], r[3], 5, Rd.withAlpha(sel ? acc : 0xFF30303A, e));
            Rd.rrect(ctx, r[0] + 1, r[1] + 1, r[2] - 2, r[3] - 2, 4, Rd.withAlpha(hov ? 0xFF1E1E26 : 0xFF17171D, e));
            Rd.text(ctx, c.title(), r[0] + 8, r[1] + 8, Rd.withAlpha(0xFFFFFFFF, e));
            Rd.text(ctx, c.status(), r[0] + 8, r[1] + 19, Rd.withAlpha(c.on() ? acc : 0xFF7A7A86, e));
            if (c.module() != null) {
                float cur = switchAnim.getOrDefault(c.module(), c.on() ? 1f : 0f);
                float t = cur + ((c.on() ? 1f : 0f) - cur) * 0.3f;
                if (Math.abs((c.on() ? 1f : 0f) - t) < 0.01f) t = c.on() ? 1f : 0f;
                switchAnim.put(c.module(), t);
                drawSwitch(ctx, r[0] + r[2] - 28, r[1] + 7, t, e);
            }
            List<OrderedText> lines = wrap(c.desc(), r[2] - 16);
            for (int k = 0; k < Math.min(2, lines.size()); k++) {
                ctx.drawTextWithShadow(client.textRenderer, lines.get(k), r[0] + 8, r[1] + 33 + k * 10,
                        Rd.withAlpha(0xFF8E8E9A, e));
            }
        }
        ctx.disableScissor();
    }

    private void renderRight(DrawContext ctx, int mx, int my, float e) {
        int x = rx + 1, w = rightW - 1, y = py + HEADER + 8;
        if (tab != 0 || selected == null) {
            String[] msg = tab == 0 ? new String[]{"Wybierz moduł", "Kliknij kartę, aby zobaczyć", "jego ustawienia."}
                    : tab == 1 ? new String[]{"Profile", "Przechowują ustawienia", "modułów i pozycje HUD."}
                    : new String[]{"Ustawienia", "Zmiany zapisują się", "automatycznie."};
            Rd.text(ctx, msg[0], x + 10, y, Rd.withAlpha(0xFFFFFFFF, e));
            Rd.text(ctx, msg[1], x + 10, y + 14, Rd.withAlpha(0xFF8E8E9A, e));
            Rd.text(ctx, msg[2], x + 10, y + 24, Rd.withAlpha(0xFF8E8E9A, e));
            return;
        }
        bigText(ctx, selected.getName(), x + 10, y, 1.5f, Rd.withAlpha(0xFFFFFFFF, e));
        int dy = y + 16;
        for (OrderedText l : wrap(selected.getDescription(), rightW - 20)) {
            ctx.drawTextWithShadow(client.textRenderer, l, x + 10, dy, Rd.withAlpha(0xFF8E8E9A, e));
            dy += 10;
        }
        List<Rows.Row> rows = rowsFor(selected);
        int top = rightRowsTop(), bottom = py + ph - 6;
        scrollR = Math.max(0, Math.min(Math.max(0, rowsHeight(rows) - (bottom - top)), scrollR));
        renderRows(ctx, rows, x + 4, top - Math.round(scrollR), w - 8, top, bottom, mx, my, e);
    }

    // ------------------------------------------------------------------ wejscie

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (anim < 0.95f) return true;
        layout(0);
        double mx = click.x(), my = click.y();
        int btn = click.button();

        if (Rd.in(mx, my, px + pw - 24, py + 6, 18, 18)) { close(); return true; }
        for (int i = 0; i < TABS.length; i++) {
            if (Rd.in(mx, my, tabX(i), py, tabW(i), HEADER)) {
                tab = i;
                scrollC = 0;
                scrollR = 0;
                if (i == 1) profiles = ConfigManager.listProfiles();
                return true;
            }
        }
        // sidebar
        int x = px + 8, w = sideW - 16;
        if (Rd.in(mx, my, x, py + ph - 26, w, 18)) { client.setScreen(new HudEditorScreen()); return true; }
        if (tab == 0) {
            int ey = py + HEADER + 8 + 26;
            int n = Category.values().length + 1;
            for (int i = 0; i < n; i++, ey += 22) {
                if (Rd.in(mx, my, px + 4, ey, sideW - 8, 20)) {
                    filter = i == 0 ? null : Category.values()[i - 1];
                    scrollC = 0;
                    return true;
                }
            }
        }
        // srodek
        int top = gridTop(), bottom = gridBottom();
        if (tab == 2) {
            if (clickRows(settingsTabRows(), cx + 8, top - Math.round(scrollC), cw - 16, top, bottom, mx, my, btn)) return true;
        } else if (my >= top && my < bottom && mx >= cx && mx < rx) {
            List<Card> cards = cards();
            for (int i = 0; i < cards.size(); i++) {
                int[] r = cardRect(i);
                if (!Rd.in(mx, my, r[0], r[1], r[2], r[3])) continue;
                Card c = cards.get(i);
                if (c.module() != null && btn == 0 && Rd.in(mx, my, r[0] + r[2] - 28, r[1] + 5, 22, 14)) {
                    c.module().toggle();
                } else if (btn == 0) {
                    c.click().run();
                } else if (btn == 1 && c.rightClick() != null) {
                    c.rightClick().run();
                }
                if (tab == 1) profiles = ConfigManager.listProfiles();
                return true;
            }
        }
        // prawy panel
        if (tab == 0 && selected != null) {
            if (clickRows(rowsFor(selected), rx + 5, rightRowsTop() - Math.round(scrollR), rightW - 9,
                    rightRowsTop(), py + ph - 6, mx, my, btn)) return true;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (Rows.dragging != null) {
            Rows.dragging.drag(click.x());
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        Rows.dragging = null;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        layout(0);
        if (mouseX >= rx) {
            scrollR -= (float) (vertical * 14);
        } else if (mouseX >= cx) {
            scrollC -= (float) (vertical * 14);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (Rows.listening != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                // anuluj
            } else if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
                Rows.listening.module().setKey(-1);
            } else {
                Rows.listening.module().setKey(key);
            }
            Rows.listening = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (search.length() > 0) search.setLength(0);
            else close();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (search.length() > 0) search.setLength(search.length() - 1);
            return true;
        }
        ClickGUIModule cfg = NexusClient.modules().get(ClickGUIModule.class);
        if (cfg != null && key == cfg.getKey() && search.length() == 0) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (Rows.listening != null || tab != 0) return true;
        int cp = input.codepoint();
        if (cp >= 32 && cp != 127 && search.length() < 24) {
            search.appendCodePoint(cp);
            scrollC = 0;
            return true;
        }
        return false;
    }
}
