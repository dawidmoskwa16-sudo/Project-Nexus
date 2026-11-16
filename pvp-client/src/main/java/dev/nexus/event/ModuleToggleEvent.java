package dev.nexus.event;

import dev.nexus.module.Module;

public record ModuleToggleEvent(Module module, boolean enabled) {}
