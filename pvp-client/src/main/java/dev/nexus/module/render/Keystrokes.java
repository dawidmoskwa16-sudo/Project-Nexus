package dev.nexus.module.render;

import dev.nexus.gui.Theme;
import dev.nexus.util.Rd;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;

public final class Keystrokes extends HudModule {
    private static final int S = 20, GAP = 2;

    public Keystrokes() {
        super("Klawisze", "Pokazuje wciśnięte klawisze ruchu i myszy.", 4, 80);
    }

    @Override
    protected void renderHud(DrawContext ctx) {
        lastW = 3 * S + 2 * GAP;
        lastH = 2 * (S + GAP) + 30;
        int x = getX(), y = getY();
        key(ctx, "W", mc.options.forwardKey, x + S + GAP, y, S, S);
        key(ctx, "A", mc.options.leftKey, x, y + S + GAP, S, S);
        key(ctx, "S", mc.options.backKey, x + S + GAP, y + S + GAP, S, S);
        key(ctx, "D", mc.options.rightKey, x + 2 * (S + GAP), y + S + GAP, S, S);
        int bw = (3 * S + 2 * GAP - GAP) / 2;
        key(ctx, "LMB", mc.options.attackKey, x, y + 2 * (S + GAP), bw, 16);
        key(ctx, "RMB", mc.options.useKey, x + bw + GAP, y + 2 * (S + GAP), bw, 16);
        key(ctx, "SPACE", mc.options.jumpKey, x, y + 2 * (S + GAP) + 18, 3 * S + 2 * GAP, 12);
    }

    private void key(DrawContext ctx, String label, KeyBinding kb, int x, int y, int w, int h) {
        boolean down = kb.isPressed();
        ctx.fill(x, y, x + w, y + h, down ? Rd.withAlpha(Theme.accent(), 0.85f) : 0x90000000);
        int tx = x + (w - Rd.width(label)) / 2;
        int ty = y + (h - 8) / 2;
        Rd.text(ctx, label, tx, ty, color.argb());
    }
}
