package dev.nexus.gui;

import dev.nexus.NexusClient;
import dev.nexus.config.ConfigManager;
import dev.nexus.module.Module;
import dev.nexus.module.render.HudModule;
import dev.nexus.util.Rd;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Edytor HUD: przeciagaj ramki wlaczonych elementow myszka. Esc zapisuje i wychodzi. */
public final class HudEditorScreen extends Screen {
    private HudModule dragged;
    private double offX, offY;

    public HudEditorScreen() { super(Text.literal("HUD Editor")); }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void removed() { ConfigManager.save(); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0x55000000);
        Rd.text(ctx, "HUD Editor - przeciagaj elementy, Esc = zapisz", 6, 6, 0xFFFFFFFF);
        for (Module m : NexusClient.modules().getAll()) {
            if (!(m instanceof HudModule h) || !h.isEnabled()) continue;
            int x = h.getX(), y = h.getY(), w = h.getW(), hh = h.getH();
            int c = h == dragged ? Theme.accent() : Rd.withAlpha(Theme.accent(), 0.6f);
            ctx.fill(x - 1, y - 1, x + w + 1, y, c);
            ctx.fill(x - 1, y + hh, x + w + 1, y + hh + 1, c);
            ctx.fill(x - 1, y, x, y + hh, c);
            ctx.fill(x + w, y, x + w + 1, y + hh, c);
            Rd.text(ctx, h.getName(), x, y - 10, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return false;
        for (Module m : NexusClient.modules().getAll()) {
            if (m instanceof HudModule h && h.isEnabled()
                    && Rd.in(click.x(), click.y(), h.getX(), h.getY(), h.getW(), h.getH())) {
                dragged = h;
                offX = click.x() - h.getX();
                offY = click.y() - h.getY();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragged == null) return false;
        int nx = (int) Math.max(0, Math.min(width - 10, click.x() - offX));
        int ny = (int) Math.max(0, Math.min(height - 10, click.y() - offY));
        dragged.setPos(nx, ny);
        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragged = null;
        return true;
    }
}
