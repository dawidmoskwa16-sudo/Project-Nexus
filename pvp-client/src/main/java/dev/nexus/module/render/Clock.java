package dev.nexus.module.render;

import dev.nexus.setting.BooleanSetting;
import net.minecraft.client.gui.DrawContext;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class Clock extends HudModule {
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter LONG = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final BooleanSetting seconds = add(new BooleanSetting("Sekundy", false));

    public Clock() { super("Zegar", "Aktualna godzina systemowa.", 4, 180); }

    @Override
    protected void renderHud(DrawContext ctx) {
        drawBlock(ctx, List.of(LocalTime.now().format(seconds.get() ? LONG : SHORT)));
    }
}
