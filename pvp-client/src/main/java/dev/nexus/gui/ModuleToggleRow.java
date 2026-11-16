package dev.nexus.gui;

import dev.nexus.module.Module;
import dev.nexus.util.Rd;
import net.minecraft.client.gui.DrawContext;

/** Wiersz "Wlaczony" z przelacznikiem na gorze panelu ustawien modulu. */
final class ModuleToggleRow extends Rows.Row {
    private final Module module;

    ModuleToggleRow(Module m) { this.module = m; anim = m.isEnabled() ? 1f : 0f; }

    @Override public int height() { return 17; }

    @Override
    public void render(DrawContext ctx, int x, int y, int w, int mx, int my, float e) {
        frame(ctx, x, y, w, mx, my, e);
        anim = approach(anim, module.isEnabled() ? 1f : 0f);
        Rd.text(ctx, "Włączony", x + 6, y + 5, Rd.withAlpha(0xFFFFFFFF, e));
        int tw = 20, th = 10, tx = x + w - 8 - tw, ty = y + 3;
        Rd.rrect(ctx, tx, ty, tw, th, 5, Rd.withAlpha(Rd.mix(0xFF2A2A2A, Theme.accent(), anim), e));
        int kx = tx + 1 + Math.round((tw - th) * anim);
        Rd.rrect(ctx, kx, ty + 1, th - 2, th - 2, 4, Rd.withAlpha(0xFFFFFFFF, e));
    }

    @Override
    public boolean click(int x, int y, int w, double mx, double my, int btn) {
        if (btn == 0 && Rd.in(mx, my, x, y, w, height())) { module.toggle(); return true; }
        return false;
    }
}
