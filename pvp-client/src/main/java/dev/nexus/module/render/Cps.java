package dev.nexus.module.render;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** Licznik klikniec na sekunde (LPM | PPM). */
public final class Cps extends HudModule {
    private final Deque<Long> left = new ArrayDeque<>();
    private final Deque<Long> right = new ArrayDeque<>();
    private boolean ld, rd;

    public Cps() { super("CPS", "Liczba klikniec na sekundę.", 4, 100); }

    @Override
    public void onTick() {
        long h = mc.getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();
        if (mc.currentScreen == null) {
            if (l && !ld && left.size() < 100) left.addLast(now);
            if (r && !rd && right.size() < 100) right.addLast(now);
        }
        ld = l;
        rd = r;
    }

    private static void prune(Deque<Long> q, long now) {
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
    }

    @Override
    protected void renderHud(DrawContext ctx) {
        long now = System.currentTimeMillis();
        prune(left, now);
        prune(right, now);
        drawBlock(ctx, List.of("CPS: " + left.size() + " | " + right.size()));
    }
}
