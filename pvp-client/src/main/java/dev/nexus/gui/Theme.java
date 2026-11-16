package dev.nexus.gui;

import dev.nexus.NexusClient;
import dev.nexus.module.misc.ClickGUIModule;

public final class Theme {
    public static final int HEADER = 0xFF0C0C0C;
    public static final int BODY = 0xFF050505;
    public static final int ROW = 0xFF0E0E0E;
    public static final int TRACK = 0xFF2A2A2A;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DIM = 0xFFA8A8A8;

    private Theme() {}

    private static ClickGUIModule gui() { return NexusClient.modules().get(ClickGUIModule.class); }

    public static int accent() {
        ClickGUIModule g = gui();
        return g == null ? 0xFF3B82F6 : g.accent.argb();
    }

    public static float opacity() {
        ClickGUIModule g = gui();
        return g == null ? 0.92f : g.opacity.floatValue();
    }
}
