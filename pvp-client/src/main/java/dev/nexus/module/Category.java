package dev.nexus.module;

public enum Category {
    HUD("HUD"),
    APPEARANCE("Wygląd"),
    GAMEPLAY("Rozgrywka"),
    PERFORMANCE("Wydajność");

    private final String displayName;

    Category(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
