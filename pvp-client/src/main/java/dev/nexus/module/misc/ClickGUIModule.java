package dev.nexus.module.misc;

import dev.nexus.gui.ClickGUIScreen;
import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.BooleanSetting;
import dev.nexus.setting.ColorSetting;
import dev.nexus.setting.NumberSetting;
import org.lwjgl.glfw.GLFW;

public final class ClickGUIModule extends Module {
    public final ColorSetting accent = add(new ColorSetting("Accent", 0x3B82F6));
    public final NumberSetting opacity = add(new NumberSetting("Opacity", 0.92, 0.4, 1.0, 0.01));
    public final NumberSetting animSpeed = add(new NumberSetting("Anim speed", 6, 1, 15, 0.5));
    public final BooleanSetting dim = add(new BooleanSetting("Dim background", true));

    public ClickGUIModule() {
        super("ClickGUI", "Otwiera menu klienta.", Category.APPEARANCE);
        setDefaultKey(GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    @Override
    public void onBind() {
        if (mc.currentScreen == null) mc.setScreen(new ClickGUIScreen());
    }

    /** ClickGUI nie jest przelacznikiem - klikniecie w GUI otwiera/zamyka jedynie ekran. */
    @Override
    public void toggle() {
        if (mc.currentScreen == null) mc.setScreen(new ClickGUIScreen());
    }
}
