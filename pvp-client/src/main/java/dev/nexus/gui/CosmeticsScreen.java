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
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Cosmetic locker with a real player render on the right and the selectable items on the left.
 * The player is rendered by Minecraft's normal entity renderer, so the live skin and feature
 * renderers are reused instead of drawing a fake 2D character.
 */
public final class CosmeticsScreen extends Screen {
    private static final String[] TABS = {"Peleryny", "Skrzydła", "Aureole"};
    private static final int HEADER = 38;
    private static final int CARD_H = 54;
    private static final int CARD_GAP = 6;
    private static final int FOOTER_H = 46;

    private final Screen parent;
    private final List<Cosmetic> cosmetics = new ArrayList<>();
    private int tab;
    private float scroll;
    private int px, py, pw, ph;

    public CosmeticsScreen(Screen parent) {
        super(Text.literal("Cosmetics"));
        this.parent = parent;

        cosmetics.add(NexusClient.modules().get(Capes.class));
        cosmetics.add(NexusClient.modules().get(Wings.class));
        cosmetics.add(NexusClient.modules().get(Halo.class));

        for (Cosmetic cosmetic : cosmetics) {
            if (cosmetic != null) cosmetic.refresh();
        }
    }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void removed() {
        ConfigManager.save();
    }

    private void layout() {
        pw = Math.min(width - 20, Math.max(520, Math.min(840, (int) (width * 0.92f))));
        ph = Math.min(height - 20, Math.max(300, Math.min(520, (int) (height * 0.88f))));
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    private Cosmetic current() { return cosmetics.get(tab); }

    private int listWidth() {
        return Math.max(205, Math.min(300, (int) (pw * 0.39f)));
    }

    private int listX() { return px + 10; }
    private int listTop() { return py + HEADER + 10; }
    private int listBottom() { return py + ph - FOOTER_H; }
    private int previewX() { return px + listWidth() + 20; }
    private int previewY() { return py + HEADER + 10; }
    private int previewW() { return px + pw - 10 - previewX(); }
    private int previewH() { return ph - HEADER - FOOTER_H - 10; }
    private int cardWidth() { return listWidth() - 20; }

    private int tabW(int i) { return Rd.width(TABS[i]) + 28; }

    private int tabX(int i) {
        int total = 0;
        for (int k = 0; k < TABS.length; k++) total += tabW(k);
        int x = px + pw - 12 - total;
        for (int k = 0; k < i; k++) x += tabW(k);
        return x;
    }

    private int[] cardRect(int i) {
        return new int[] {
                listX(),
                listTop() - Math.round(scroll) + i * (CARD_H + CARD_GAP),
                cardWidth(),
                CARD_H
        };
    }

    private int maxScroll() {
        int content = current().options().size() * (CARD_H + CARD_GAP);
        return Math.max(0, content - (listBottom() - listTop()));
    }

    private boolean isEquipped(Cosmetic cosmetic, String name) {
        return cosmetic != null && cosmetic.module().isEnabled() && cosmetic.selected().equals(name);
    }

    private void toggleOption(Cosmetic cosmetic, String name) {
        if (cosmetic == null) return;
        Module module = cosmetic.module();
        if (isEquipped(cosmetic, name)) {
            module.setEnabled(false, true);
        } else {
            cosmetic.select(name);
            if (!module.isEnabled()) module.setEnabled(true, true);
        }
    }

    private static int accentFor(String name) {
        String n = name.toLowerCase(java.util.Locale.ROOT);
        if (n.contains("eclipse") || n.contains("cień")) return 0xFF9A83C4;
        if (n.contains("frost") || n.contains("kryształ")) return 0xFF8FC8DC;
        if (n.contains("royal") || n.contains("klasyczna")) return 0xFFD4B477;
        if (n.contains("orbit")) return 0xFF7DB5D2;
        if (n.contains("srebr")) return 0xFFB8C2D0;
        if (n.contains("pióra") || n.contains("nexus")) return 0xFFCAD0DA;
        return 0xFF8898B5;
    }

    /** A restrained material swatch; the actual item is shown on the 3D player beside it. */
    private void drawSwatch(DrawContext ctx, int x, int y, int size, String name) {
        int accent = accentFor(name);
        Rd.rrect(ctx, x, y, size, size, 5, 0xFF080B11);
        Rd.rrect(ctx, x + 1, y + 1, size - 2, size - 2, 4, 0xFF171D28);
        if (tab == 0) {
            ctx.fill(x + 9, y + 5, x + size - 9, y + size - 5, Rd.mix(accent, 0xFF10151E, 0.28f));
            ctx.fill(x + 11, y + 7, x + size - 11, y + size - 7, accent);
            ctx.fill(x + size / 2 - 1, y + 9, x + size / 2 + 1, y + size - 9, 0xFFE6EAF0);
        } else if (tab == 1) {
            int cx = x + size / 2;
            for (int i = 0; i < 4; i++) {
                int yy = y + 7 + i * 7;
                int len = 8 + i * 3;
                ctx.fill(cx - len, yy, cx - 2, yy + 4, i % 2 == 0 ? accent : Rd.mix(accent, 0xFFFFFFFF, 0.25f));
                ctx.fill(cx + 2, yy, cx + len, yy + 4, i % 2 == 0 ? accent : Rd.mix(accent, 0xFFFFFFFF, 0.25f));
            }
        } else {
            int cx = x + size / 2, cy = y + size / 2;
            for (int i = 0; i < 32; i++) {
                double a = Math.PI * 2.0 * i / 32.0;
                int rx = cx + (int) Math.round(Math.cos(a) * 11);
                int ry = cy + (int) Math.round(Math.sin(a) * 7);
                ctx.fill(rx - 1, ry - 1, rx + 2, ry + 2, accent);
            }
        }
    }

    private void renderPreview(DrawContext ctx, int mx, int my) {
        int x = previewX(), y = previewY(), w = previewW(), h = previewH();
        Rd.rrect(ctx, x, y, w, h, 8, 0xFF343B49);
        Rd.rrect(ctx, x + 1, y + 1, w - 2, h - 2, 7, 0xF20E121A);
        ctx.fillGradient(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF171D29, 0xFF0C1018);

        Rd.text(ctx, "PODGLĄD POSTACI", x + 14, y + 12, 0xFFB7C0CF);
        Cosmetic cosmetic = current();
        String selected = cosmetic == null ? "Brak kosmetyku" : cosmetic.selected();
        Rd.text(ctx, selected, x + 14, y + 29, 0xFFFFFFFF);

        if (client != null && client.player != null) {
            int modelTop = y + 42;
            int modelBottom = y + h - 8;
            int modelSize = Math.max(48, Math.min(92, (modelBottom - modelTop) / 2));
            InventoryScreen.drawEntity(
                    ctx,
                    x + 5, modelTop,
                    x + w - 5, modelBottom,
                    modelSize, 1.0f,
                    mx, my,
                    client.player
            );
            Rd.text(ctx, "Poruszaj kursorem, aby zmienić kąt", x + 14, y + h - 19, 0xFF78869A);
        } else {
            String line1 = "Podgląd 3D jest dostępny";
            String line2 = "po wejściu do świata.";
            Rd.text(ctx, line1, x + 14, y + h / 2 - 5, 0xFFD6DCE6);
            Rd.text(ctx, line2, x + 14, y + h / 2 + 10, 0xFF8995A7);
        }

        if (cosmetic != null && cosmetic.module().isEnabled()) {
            Rd.rrect(ctx, x + 12, y + h - 38, 78, 16, 4, 0xFF263B34);
            Rd.text(ctx, "ZAŁOŻONE", x + 20, y + h - 34, 0xFF8AD5AA);
        }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        layout();
        int acc = Theme.accent();
        ctx.fillGradient(0, 0, width, height, 0xFF06080D, 0xFF111722);

        Rd.rrect(ctx, px - 1, py - 1, pw + 2, ph + 2, 9, 0xFF394251);
        Rd.rrect(ctx, px, py, pw, ph, 8, 0xF211151D);
        ctx.fill(px + 1, py + HEADER, px + pw - 1, py + HEADER + 1, 0xFF29313E);

        Rd.text(ctx, "NEXUS", px + 14, py + 10, 0xFFE7EBF2);
        Rd.text(ctx, "COSMETICS", px + 14, py + 22, 0xFF78869A);

        for (int i = 0; i < TABS.length; i++) {
            int tx = tabX(i), tw = tabW(i);
            boolean active = tab == i;
            boolean hovered = Rd.in(mx, my, tx, py + 4, tw, HEADER - 6);
            Rd.text(ctx, TABS[i], tx + 14, py + 17,
                    active ? 0xFFFFFFFF : hovered ? 0xFFD2D9E4 : 0xFF8793A4);
            if (active) ctx.fill(tx + 5, py + HEADER - 2, tx + tw - 5, py + HEADER, acc);
        }

        Cosmetic cosmetic = current();
        List<String> options = cosmetic == null ? List.of() : cosmetic.options();
        scroll = Math.max(0, Math.min(maxScroll(), scroll));

        ctx.enableScissor(listX(), listTop(), listX() + cardWidth(), listBottom());
        for (int i = 0; i < options.size(); i++) {
            int[] r = cardRect(i);
            if (r[1] + r[3] < listTop() || r[1] > listBottom()) continue;

            String name = options.get(i);
            boolean equipped = isEquipped(cosmetic, name);
            boolean selected = cosmetic != null && cosmetic.selected().equals(name);
            boolean hovered = Rd.in(mx, my, r[0], r[1], r[2], r[3]) && my >= listTop() && my < listBottom();

            int border = equipped ? acc : selected ? 0xFF69788C : 0xFF303947;
            Rd.rrect(ctx, r[0], r[1], r[2], r[3], 6, border);
            Rd.rrect(ctx, r[0] + 1, r[1] + 1, r[2] - 2, r[3] - 2, 5,
                    hovered ? 0xFF202938 : 0xFF171E28);
            drawSwatch(ctx, r[0] + 8, r[1] + 9, 36, name);
            int tx = r[0] + 53;
            Rd.text(ctx, name, tx, r[1] + 11, 0xFFE8ECF3);
            String meta = cosmetic != null && cosmetic.builtins().contains(name) ? "NEXUS ORIGINAL" : "CUSTOM FILE";
            Rd.text(ctx, meta, tx, r[1] + 29, 0xFF7F8B9D);
            if (equipped) Rd.text(ctx, "●", r[0] + r[2] - 16, r[1] + 19, 0xFF8AD5AA);
        }
        ctx.disableScissor();

        renderPreview(ctx, mx, my);

        String customFolder = tab == 0 ? "config/nexus/cosmetics/" : tab == 1 ? "config/nexus/cosmetics/wings/" : "config/nexus/cosmetics/halos/";
        Rd.text(ctx, "WŁASNE: " + customFolder, px + 12, py + ph - 36, 0xFF78869A);
        button(ctx, px + 12, py + ph - 23, 100, 16, "Odśwież pliki", mx, my);
        button(ctx, px + pw - 82, py + ph - 23, 70, 16, "Wstecz", mx, my);
    }

    private static void button(DrawContext ctx, int x, int y, int w, int h, String label, int mx, int my) {
        boolean hovered = Rd.in(mx, my, x, y, w, h);
        Rd.rrect(ctx, x, y, w, h, 4, hovered ? 0xFF536174 : 0xFF35404F);
        Rd.rrect(ctx, x + 1, y + 1, w - 2, h - 2, 3, hovered ? 0xFF263140 : 0xFF1B222D);
        Rd.text(ctx, label, x + (w - Rd.width(label)) / 2, y + 4, 0xFFE9EDF4);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return false;
        layout();
        double mx = click.x(), my = click.y();

        for (int i = 0; i < TABS.length; i++) {
            if (Rd.in(mx, my, tabX(i), py + 4, tabW(i), HEADER - 6)) {
                tab = i;
                scroll = 0;
                return true;
            }
        }

        if (Rd.in(mx, my, px + pw - 82, py + ph - 23, 70, 16)) {
            client.setScreen(parent);
            return true;
        }
        if (Rd.in(mx, my, px + 12, py + ph - 23, 100, 16)) {
            if (current() != null) current().refresh();
            return true;
        }

        if (mx >= listX() && mx < listX() + cardWidth() && my >= listTop() && my < listBottom()) {
            Cosmetic cosmetic = current();
            if (cosmetic != null) {
                List<String> options = cosmetic.options();
                for (int i = 0; i < options.size(); i++) {
                    int[] r = cardRect(i);
                    if (Rd.in(mx, my, r[0], r[1], r[2], r[3])) {
                        toggleOption(cosmetic, options.get(i));
                        return true;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseX >= listX() && mouseX < listX() + cardWidth()
                && mouseY >= listTop() && mouseY < listBottom()) {
            scroll -= (float) (vertical * 18);
            scroll = Math.max(0, Math.min(maxScroll(), scroll));
        }
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
