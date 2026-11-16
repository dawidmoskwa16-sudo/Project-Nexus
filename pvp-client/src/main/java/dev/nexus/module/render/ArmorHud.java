package dev.nexus.module.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

public final class ArmorHud extends HudModule {
    private static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};

    public ArmorHud() { super("Pancerz", "Status zużycia zbroi i trzymanych przedmiotów.", 4, 240); }

    @Override
    protected void renderHud(DrawContext ctx) {
        lastW = SLOTS.length * 18;
        lastH = 16;
        int x = getX();
        for (EquipmentSlot s : SLOTS) {
            ItemStack st = mc.player.getEquippedStack(s);
            if (!st.isEmpty()) {
                ctx.drawItem(st, x, getY());
                ctx.drawStackOverlay(mc.textRenderer, st, x, getY());
            }
            x += 18;
        }
    }
}
