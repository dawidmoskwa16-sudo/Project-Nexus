package dev.nexus.module.render;

import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.BooleanSetting;
import dev.nexus.setting.ColorSetting;
import dev.nexus.setting.NumberSetting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.hit.HitResult;

/** Dodatkowy celownik rysowany na srodku ekranu (celownik vanilla ukryjemy mixinem w czesci 2). */
public final class CustomCrosshair extends Module {
    private final NumberSetting size = add(new NumberSetting("Size", 5, 1, 20, 1));
    private final NumberSetting gap = add(new NumberSetting("Gap", 2, 0, 12, 1));
    private final NumberSetting thickness = add(new NumberSetting("Thickness", 1, 1, 4, 1));
    private final BooleanSetting dot = add(new BooleanSetting("Center dot", false));
    private final BooleanSetting targetColor = add(new BooleanSetting("Red on entity", true));
    private final ColorSetting color = add(new ColorSetting("Color", 0x00FF88));

    public CustomCrosshair() {
        super("Celownik", "Własny, konfigurowalny celownik.", Category.APPEARANCE);
    }

    @Override
    public void onRender2D(DrawContext ctx) {
        if (mc.player == null || mc.options.hudHidden || mc.currentScreen != null) return;
        if (!mc.options.getPerspective().isFirstPerson()) return;

        int cx = mc.getWindow().getScaledWidth() / 2;
        int cy = mc.getWindow().getScaledHeight() / 2;
        int col = color.argb();
        if (targetColor.get() && mc.crosshairTarget != null
                && mc.crosshairTarget.getType() == HitResult.Type.ENTITY) col = 0xFFFF3B3B;

        int s = size.intValue(), g = gap.intValue(), t = thickness.intValue();
        int half = t / 2;
        ctx.fill(cx - half, cy - g - s, cx - half + t, cy - g, col);
        ctx.fill(cx - half, cy + g, cx - half + t, cy + g + s, col);
        ctx.fill(cx - g - s, cy - half, cx - g, cy - half + t, col);
        ctx.fill(cx + g, cy - half, cx + g + s, cy - half + t, col);
        if (dot.get()) ctx.fill(cx - half, cy - half, cx - half + t, cy - half + t, col);
    }
}
