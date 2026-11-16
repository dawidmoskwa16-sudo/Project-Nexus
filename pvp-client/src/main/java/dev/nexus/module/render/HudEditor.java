package dev.nexus.module.render;

import dev.nexus.gui.HudEditorScreen;
import dev.nexus.module.Category;
import dev.nexus.module.Module;

public final class HudEditor extends Module {
    public HudEditor() {
        super("HudEditor", "Przesuwanie elementow HUD myszka.", Category.APPEARANCE);
    }

    @Override
    public void toggle() { if (mc.currentScreen == null) mc.setScreen(new HudEditorScreen()); }

    @Override
    public void onBind() { toggle(); }
}
