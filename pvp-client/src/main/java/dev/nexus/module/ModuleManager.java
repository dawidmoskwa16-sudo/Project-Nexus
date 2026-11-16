package dev.nexus.module;

import dev.nexus.NexusClient;
import dev.nexus.event.Render2DEvent;
import dev.nexus.event.TickEvent;
import dev.nexus.module.misc.*;
import dev.nexus.module.movement.*;
import dev.nexus.module.render.*;
import dev.nexus.module.world.*;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public void init() {
        // HUD
        register(new Coordinates());
        register(new Keystrokes());
        register(new Speedometer());
        register(new ArmorHud());
        register(new Cps());
        register(new PotionEffects());
        register(new Clock());
        register(new ModuleList());
        register(new WorldInformation());
        // Wyglad
        register(new CustomCrosshair());
        register(new Capes());
        register(new Wings());
        register(new Halo());
        register(new NotificationsModule());
        // Rozgrywka
        register(new Sprint());
        register(new Zoom());
        register(new Fullbright());
        // Wydajnosc
        register(new FpsCounter());
        register(new Resources());
        // Ukryte (otwieraja ekrany)
        register(new ClickGUIModule());
        register(new HudEditor());

        NexusClient.events().subscribe(TickEvent.class, e -> tick());
        NexusClient.events().subscribe(Render2DEvent.class, this::render);
    }

    private void register(Module m) { modules.add(m); }

    public List<Module> getAll() { return Collections.unmodifiableList(modules); }

    public List<Module> byCategory(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : modules) if (m.getCategory() == c) out.add(m);
        return out;
    }

    public <T extends Module> T get(Class<T> type) {
        for (Module m : modules) if (type.isInstance(m)) return type.cast(m);
        return null;
    }

    private void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        long handle = mc.getWindow().getHandle();
        boolean canFire = mc.currentScreen == null;
        for (Module m : modules) {
            int k = m.getKey();
            if (k <= 0) { m.keyDown = false; continue; }
            boolean down = GLFW.glfwGetKey(handle, k) == GLFW.GLFW_PRESS;
            if (down && !m.keyDown && canFire) safe(m, m::onBind);
            m.keyDown = down;
        }
        for (Module m : modules) {
            if (m.isEnabled()) safe(m, m::onTick);
        }
    }

    private void render(Render2DEvent e) {
        for (Module m : modules) {
            if (m.isEnabled()) safe(m, () -> m.onRender2D(e.context()));
        }
    }

    private void safe(Module m, Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            NexusClient.LOGGER.error("Modul {} zglosil blad i zostal wylaczony", m.getName(), t);
            m.setEnabled(false, false);
        }
    }
}
