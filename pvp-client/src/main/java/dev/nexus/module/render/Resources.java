package dev.nexus.module.render;

import dev.nexus.module.Category;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** Zuzycie pamieci RAM przez gre. */
public final class Resources extends HudModule {
    public Resources() { super("Zasoby", "Zużycie pamięci RAM.", Category.PERFORMANCE, 4, 200); }

    @Override
    protected void renderHud(DrawContext ctx) {
        Runtime rt = Runtime.getRuntime();
        long used = (rt.totalMemory() - rt.freeMemory()) / 1_048_576L;
        long max = rt.maxMemory() / 1_048_576L;
        drawBlock(ctx, List.of("RAM: " + used + "/" + max + " MB"));
    }
}
