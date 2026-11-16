package dev.nexus.util;

import dev.nexus.NexusClient;
import dev.nexus.gui.Theme;
import dev.nexus.module.misc.NotificationsModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayDeque;
import java.util.Deque;

/** Krotkie powiadomienia w prawym dolnym rogu. Maks. 6 naraz - brak rosnacej pamieci. */
public final class NotificationManager {
    private record Note(String text, long created) {}

    private final Deque<Note> notes = new ArrayDeque<>();

    public void push(String text) {
        notes.addFirst(new Note(text, System.currentTimeMillis()));
        while (notes.size() > 6) notes.removeLast();
    }

    public void render(DrawContext ctx) {
        NotificationsModule m = NexusClient.modules().get(NotificationsModule.class);
        if (m == null || !m.isEnabled()) {
            notes.clear();
            return;
        }
        long now = System.currentTimeMillis();
        long life = m.durationMillis();
        notes.removeIf(n -> now - n.created() > life);

        MinecraftClient mc = MinecraftClient.getInstance();
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        int i = 0;
        for (Note n : notes) {
            long age = now - n.created();
            float in = Math.min(1f, age / 200f);
            float out = Math.min(1f, (life - age) / 300f);
            float a = Math.min(in, out);
            int w = Rd.width(n.text()) + 14;
            int x = sw - w - 6 + (int) ((1f - in) * (w + 6));
            int y = sh - 44 - i * 20;
            ctx.fill(x, y, x + w, y + 16, Rd.withAlpha(0xE0101014, a));
            ctx.fill(x, y, x + 2, y + 16, Rd.withAlpha(Theme.accent(), a));
            Rd.text(ctx, n.text(), x + 8, y + 4, Rd.withAlpha(0xFFFFFFFF, a));
            i++;
        }
    }
}
