package dev.nexus;

import com.mojang.brigadier.Command;
import dev.nexus.config.ConfigManager;
import dev.nexus.event.EventBus;
import dev.nexus.event.Render2DEvent;
import dev.nexus.event.TickEvent;
import dev.nexus.gui.ClickGUIScreen;
import dev.nexus.module.ModuleManager;
import dev.nexus.util.NotificationManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NexusClient implements ClientModInitializer {
    public static final String NAME = "PvP Client";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    private static final EventBus EVENTS = new EventBus();
    private static final ModuleManager MODULES = new ModuleManager();
    private static final NotificationManager NOTIFICATIONS = new NotificationManager();

    private static boolean openGuiRequested;

    public static EventBus events() { return EVENTS; }
    public static ModuleManager modules() { return MODULES; }
    public static NotificationManager notifications() { return NOTIFICATIONS; }

    @Override
    public void onInitializeClient() {
        MODULES.init();
        dev.nexus.cosmetics.CosmeticsRenderers.register();
        ConfigManager.init();

        EVENTS.subscribe(TickEvent.class, e -> {
            ConfigManager.tick();
            MinecraftClient cl = MinecraftClient.getInstance();
            if (cl.currentScreen != null && cl.currentScreen.getClass() == net.minecraft.client.gui.screen.TitleScreen.class) {
                cl.setScreen(new dev.nexus.gui.NexusTitleScreen());
            }
            // Menu otwieramy dopiero gdy czat sie zamknie.
            if (openGuiRequested) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.currentScreen == null && mc.player != null) {
                    openGuiRequested = false;
                    mc.setScreen(new ClickGUIScreen());
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> EVENTS.post(TickEvent.INSTANCE));
        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            EVENTS.post(new Render2DEvent(context));
            NOTIFICATIONS.render(context);
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("nexus").executes(ctx -> {
                openGuiRequested = true;
                return Command.SINGLE_SUCCESS;
            }));
            dispatcher.register(ClientCommandManager.literal("gui").executes(ctx -> {
                openGuiRequested = true;
                return Command.SINGLE_SUCCESS;
            }));
        });

        LOGGER.info("{} Client zaladowany ({} modulow)", NAME, MODULES.getAll().size());
    }
}
