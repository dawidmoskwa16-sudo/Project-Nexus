package dev.nexus.module.render;

import dev.nexus.NexusClient;
import dev.nexus.module.Module;
import dev.nexus.util.Rd;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ModuleList extends HudModule {
    public ModuleList() { super("Lista modułów", "Lista włączonych modułów.", 4, 120); }

    @Override
    protected void renderHud(DrawContext ctx) {
        List<String> names = new ArrayList<>();
        for (Module m : NexusClient.modules().getAll()) {
            if (m.isEnabled() && m != this && !(m instanceof HudModule)) names.add(m.getName());
        }
        if (names.isEmpty()) { lastW = 0; lastH = 0; return; }
        names.sort(Comparator.comparingInt((String s) -> Rd.width(s)).reversed());
        drawBlock(ctx, names);
    }
}
