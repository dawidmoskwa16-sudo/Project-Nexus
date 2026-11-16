package dev.nexus.module;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.nexus.NexusClient;
import dev.nexus.config.ConfigManager;
import dev.nexus.event.ModuleToggleEvent;
import dev.nexus.module.misc.NotificationsModule;
import dev.nexus.setting.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    private final String name;
    private final String description;
    private final Category category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private int key = -1;
    private boolean enabled;
    boolean keyDown; // uzywane przez ModuleManager

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected <S extends Setting<?>> S add(S setting) {
        settings.add(setting);
        return setting;
    }

    /** Ustawia stan startowy bez wywolywania hookow. */
    protected void enabledByDefault() { this.enabled = true; }

    protected void setDefaultKey(int key) { this.key = key; }

    public void toggle() { setEnabled(!enabled, true); }

    public void setEnabled(boolean state, boolean notify) {
        if (state == enabled) return;
        enabled = state;
        try {
            if (enabled) onEnable(); else onDisable();
        } catch (Exception ex) {
            NexusClient.LOGGER.error("Blad w module {}", name, ex);
        }
        if (notify) {
            NotificationsModule n = NexusClient.modules().get(NotificationsModule.class);
            if (n != null && n.isEnabled()) {
                NexusClient.notifications().push(name + (enabled ? " wlaczony" : " wylaczony"));
            }
        }
        NexusClient.events().post(new ModuleToggleEvent(this, enabled));
        ConfigManager.markDirty();
    }

    /** Wywolywane po wcisnieciu przypisanego klawisza. */
    public void onBind() { toggle(); }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick() {}
    public void onRender2D(DrawContext context) {}

    public String getName() { return name; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public List<Setting<?>> getSettings() { return Collections.unmodifiableList(settings); }
    public boolean isEnabled() { return enabled; }
    public int getKey() { return key; }

    public void setKey(int key) {
        this.key = key;
        ConfigManager.markDirty();
    }

    public JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("enabled", enabled);
        o.addProperty("key", key);
        JsonObject s = new JsonObject();
        for (Setting<?> st : settings) s.add(st.getName(), st.toJson());
        o.add("settings", s);
        return o;
    }

    public void fromJson(JsonObject o) {
        if (o.has("key")) key = o.get("key").getAsInt();
        if (o.has("settings")) {
            JsonObject s = o.getAsJsonObject("settings");
            for (Setting<?> st : settings) {
                JsonElement el = s.get(st.getName());
                if (el == null) continue;
                try {
                    st.fromJson(el);
                } catch (Exception ex) {
                    NexusClient.LOGGER.warn("Nieprawidlowa wartosc ustawienia {}.{}", name, st.getName());
                }
            }
        }
        if (o.has("enabled")) setEnabled(o.get("enabled").getAsBoolean(), false);
    }
}
