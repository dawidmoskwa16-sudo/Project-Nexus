package dev.nexus.module.misc;

import dev.nexus.module.Category;
import dev.nexus.module.Module;
import dev.nexus.setting.NumberSetting;

public final class NotificationsModule extends Module {
    private final NumberSetting duration = add(new NumberSetting("Duration (s)", 2.5, 1, 8, 0.5));

    public NotificationsModule() {
        super("Powiadomienia", "Powiadomienia o włączeniu i wyłączeniu modułów.", Category.APPEARANCE);
        enabledByDefault();
    }

    public long durationMillis() { return (long) (duration.get() * 1000); }
}
