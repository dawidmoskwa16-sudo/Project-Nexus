package dev.nexus.setting;

import com.google.gson.JsonElement;
import dev.nexus.config.ConfigManager;

public abstract class Setting<T> {
    private final String name;
    protected T value;
    private final T defaultValue;

    protected Setting(String name, T defaultValue) {
        this.name = name;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public String getName() { return name; }
    public T get() { return value; }
    public T getDefault() { return defaultValue; }

    public void set(T newValue) {
        this.value = newValue;
        ConfigManager.markDirty();
    }

    public void reset() { set(defaultValue); }

    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement element);
}
