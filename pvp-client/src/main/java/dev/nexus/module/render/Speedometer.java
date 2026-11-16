package dev.nexus.module.render;

import net.minecraft.client.gui.DrawContext;

import java.util.List;
import java.util.Locale;

public final class Speedometer extends HudModule {
    private double lastX, lastZ, speed;

    public Speedometer() { super("Prędkość", "Prędkość w blokach na sekundę.", 4, 270); }

    @Override
    public void onTick() {
        var p = mc.player;
        if (p == null) return;
        speed = Math.hypot(p.getX() - lastX, p.getZ() - lastZ) * 20.0;
        lastX = p.getX();
        lastZ = p.getZ();
    }

    @Override
    protected void renderHud(DrawContext ctx) {
        drawBlock(ctx, List.of(String.format(Locale.ROOT, "Speed: %.2f b/s", speed)));
    }
}
