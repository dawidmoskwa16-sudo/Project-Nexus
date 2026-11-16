package dev.nexus.module.render;

import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.NumberSetting;

public final class Zoom extends Module {
    private final NumberSetting fov = add(new NumberSetting("FOV", 30, 30, 70, 1));
    private int previous = -1;

    public Zoom() { super("Zoom", "Przybliżenie obrazu (zmienia FOV).", Category.GAMEPLAY); }

    @Override
    protected void onEnable() { previous = mc.options.getFov().getValue(); }

    @Override
    public void onTick() { if (mc.options.getFov().getValue() != fov.intValue()) mc.options.getFov().setValue(fov.intValue()); }

    @Override
    protected void onDisable() { if (previous != -1) mc.options.getFov().setValue(previous); previous = -1; }
}
