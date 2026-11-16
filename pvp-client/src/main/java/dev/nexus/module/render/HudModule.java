package dev.nexus.module.render;

import dev.nexus.gui.Theme;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.BooleanSetting;
import dev.nexus.setting.ColorSetting;
import dev.nexus.setting.NumberSetting;
import dev.nexus.util.Rd;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** Bazowa klasa elementow HUD z konfigurowalna pozycja (edytor HUD pojawi sie w czesci 2). */
public abstract class HudModule extends Module {
    protected final NumberSetting posX;
    protected final NumberSetting posY;
    protected final BooleanSetting background = add(new BooleanSetting("Background", true));
    protected final ColorSetting color = add(new ColorSetting("Text color", 0xFFFFFF));

    protected HudModule(String name, String description, int defX, int defY) {
        this(name, description, Category.HUD, defX, defY);
    }

    protected HudModule(String name, String description, Category category, int defX, int defY) {
        super(name, description, category);
        posX = add(new NumberSetting("X", defX, 0, 2000, 1));
        posY = add(new NumberSetting("Y", defY, 0, 2000, 1));
    }

    protected int lastW = 60, lastH = 12;
    public int getW() { return lastW; }
    public int getH() { return lastH; }
    public void setPos(int x, int y) { posX.set((double) x); posY.set((double) y); }

    public int getX() { return posX.intValue(); }
    public int getY() { return posY.intValue(); }

    @Override
    public final void onRender2D(DrawContext ctx) {
        if (mc.player == null || mc.options.hudHidden) return;
        renderHud(ctx);
    }

    protected abstract void renderHud(DrawContext ctx);

    protected void drawBlock(DrawContext ctx, List<String> lines) {
        int w = 0;
        for (String l : lines) w = Math.max(w, Rd.width(l));
        w += 8;
        int h = lines.size() * 10 + 4;
        lastW = w;
        lastH = h;
        int x = getX(), y = getY();
        if (background.get()) {
            ctx.fill(x, y, x + w, y + h, 0x90000000);
            ctx.fill(x, y, x + 1, y + h, Theme.accent());
        }
        int c = color.argb();
        for (int i = 0; i < lines.size(); i++) Rd.text(ctx, lines.get(i), x + 4, y + 3 + i * 10, c);
    }
}
