package dev.nexus.module.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PotionEffects extends HudModule {
    public PotionEffects() { super("Efekty mikstur", "Własny wygląd efektów mikstur.", 4, 140); }

    @Override
    protected void renderHud(DrawContext ctx) {
        List<String> lines = new ArrayList<>();
        for (StatusEffectInstance e : mc.player.getStatusEffects()) {
            String name = e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1);
            String time;
            if (e.isInfinite()) {
                time = "∞";
            } else {
                int sec = e.getDuration() / 20;
                time = String.format(Locale.ROOT, "%d:%02d", sec / 60, sec % 60);
            }
            lines.add(name + "  " + time);
        }
        if (lines.isEmpty()) { lastW = 0; lastH = 0; return; }
        drawBlock(ctx, lines);
    }
}
