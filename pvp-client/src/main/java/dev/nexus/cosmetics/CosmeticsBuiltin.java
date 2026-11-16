package dev.nexus.cosmetics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

/** Proceduralne, wbudowane tekstury kosmetykow. */
public final class CosmeticsBuiltin {
    public static final List<String> WINGS = List.of(
            "Anielskie", "Nietoperz", "Kryształ", "Feniks",
            "Cieni", "Cyber", "Lodowe", "Smocze"
    );
    public static final List<String> HALOS = List.of(
            "Złota", "Neon", "Srebrna", "Ognista",
            "Kosmiczna", "Szmaragdowa", "Mroczna", "Różowa"
    );

    private CosmeticsBuiltin() {}

    private static BufferedImage canvas() {
        return new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
    }

    private static Graphics2D graphics(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    public static BufferedImage wings(String name) {
        BufferedImage image = canvas();
        Graphics2D g = graphics(image);
        switch (name) {
            case "Nietoperz" -> batWing(g, new Color(25, 18, 42), new Color(103, 67, 151), new Color(205, 164, 255));
            case "Kryształ" -> crystalWing(g);
            case "Feniks" -> featherWing(g, new Color(255, 91, 35), new Color(255, 205, 74), new Color(255, 239, 174), true);
            case "Cieni" -> featherWing(g, new Color(30, 24, 58), new Color(91, 64, 145), new Color(190, 149, 255), false);
            case "Cyber" -> cyberWing(g);
            case "Lodowe" -> featherWing(g, new Color(94, 190, 235), new Color(204, 246, 255), Color.WHITE, false);
            case "Smocze" -> batWing(g, new Color(20, 62, 48), new Color(65, 148, 105), new Color(190, 255, 197));
            default -> featherWing(g, new Color(204, 214, 237), Color.WHITE, new Color(255, 244, 207), false);
        }
        g.dispose();
        return image;
    }

    /** Warstwowe piorka z obrysami i zylkami. */
    private static void featherWing(Graphics2D g, Color base, Color light, Color edge, boolean embers) {
        for (int i = 0; i < 10; i++) {
            double angle = Math.toRadians(-48 + i * 10.5);
            float len = 91 - Math.abs(i - 4.5f) * 5.5f;
            AffineTransform old = g.getTransform();
            g.translate(8, 64);
            g.rotate(angle);

            Path2D feather = new Path2D.Float();
            feather.moveTo(0, -5);
            feather.curveTo(len * 0.24, -13, len * 0.72, -9, len, 0);
            feather.curveTo(len * 0.72, 9, len * 0.24, 13, 0, 5);
            feather.closePath();

            g.setPaint(new GradientPaint(0, -8, light, len, 5, base));
            g.fill(feather);
            g.setColor(new Color(edge.getRed(), edge.getGreen(), edge.getBlue(), 205));
            g.setStroke(new BasicStroke(1.25f));
            g.draw(feather);
            g.setColor(new Color(255, 255, 255, 115));
            g.setStroke(new BasicStroke(0.8f));
            g.drawLine(5, 0, (int) (len * 0.83f), 0);
            for (int vane = 1; vane <= 3; vane++) {
                int vx = (int) (len * (0.26f + vane * 0.16f));
                g.drawLine(vx, 0, vx - 8, -5);
                g.drawLine(vx, 0, vx - 8, 5);
            }
            g.setTransform(old);
        }

        g.setPaint(new GradientPaint(3, 48, light, 27, 83, base));
        g.fill(new RoundRectangle2D.Float(5, 49, 31, 30, 12, 12));
        g.setColor(new Color(edge.getRed(), edge.getGreen(), edge.getBlue(), 220));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(5, 49, 31, 30, 12, 12);

        if (embers) {
            Random random = new Random(47);
            for (int i = 0; i < 20; i++) {
                int x = 18 + random.nextInt(92);
                int y = 12 + random.nextInt(104);
                int s = 1 + random.nextInt(3);
                g.setColor(new Color(255, 210 + random.nextInt(46), 90, 170));
                g.fillOval(x, y, s, s);
            }
        }
    }

    private static void batWing(Graphics2D g, Color dark, Color mid, Color glow) {
        Path2D membrane = new Path2D.Float();
        membrane.moveTo(8, 46);
        membrane.lineTo(119, 12);
        membrane.lineTo(102, 51);
        membrane.lineTo(91, 45);
        membrane.lineTo(79, 91);
        membrane.lineTo(64, 78);
        membrane.lineTo(48, 119);
        membrane.lineTo(32, 98);
        membrane.lineTo(12, 116);
        membrane.closePath();
        g.setPaint(new GradientPaint(8, 20, mid, 105, 110, dark));
        g.fill(membrane);
        g.setColor(glow);
        g.setStroke(new BasicStroke(2.4f));
        g.draw(membrane);
        g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 190));
        g.setStroke(new BasicStroke(2f));
        int[][] ends = {{102, 51}, {79, 91}, {48, 119}, {12, 116}};
        for (int[] end : ends) g.drawLine(8, 46, end[0], end[1]);
        g.setColor(new Color(255, 255, 255, 90));
        g.setStroke(new BasicStroke(1f));
        g.drawLine(12, 48, 112, 18);
        g.fillOval(5, 41, 10, 10);
    }

    private static void crystalWing(Graphics2D g) {
        Color[] colors = {
                new Color(164, 250, 255, 230), new Color(92, 185, 255, 225),
                new Color(151, 133, 255, 220), new Color(218, 250, 255, 235)
        };
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(-49 + i * 12.5);
            float len = 101 - Math.abs(i - 3.5f) * 8;
            AffineTransform old = g.getTransform();
            g.translate(7, 64);
            g.rotate(angle);
            Path2D shard = new Path2D.Float();
            shard.moveTo(0, -5);
            shard.lineTo(len * 0.68, -10);
            shard.lineTo(len, 0);
            shard.lineTo(len * 0.68, 10);
            shard.lineTo(0, 5);
            shard.closePath();
            Color c = colors[i % colors.length];
            g.setPaint(new GradientPaint(0, -7, Color.WHITE, len, 5, c));
            g.fill(shard);
            g.setColor(new Color(255, 255, 255, 225));
            g.setStroke(new BasicStroke(1.35f));
            g.draw(shard);
            g.setColor(new Color(72, 125, 220, 130));
            g.drawLine(4, 0, (int) (len * 0.72f), 0);
            g.setTransform(old);
        }
        g.setColor(new Color(218, 250, 255, 230));
        g.fillOval(3, 57, 25, 15);
    }

    private static void cyberWing(Graphics2D g) {
        Color cyan = new Color(73, 240, 255);
        Color blue = new Color(48, 103, 255);
        for (int i = 0; i < 7; i++) {
            int y = 19 + i * 13;
            int len = 102 - Math.abs(i - 3) * 9;
            Path2D plate = new Path2D.Float();
            plate.moveTo(8, y + 5);
            plate.lineTo(8 + len * 0.72, y - 4);
            plate.lineTo(8 + len, y + 3);
            plate.lineTo(8 + len * 0.78, y + 13);
            plate.lineTo(8, y + 10);
            plate.closePath();
            g.setPaint(new GradientPaint(8, y, cyan, 112, y + 10, blue));
            g.fill(plate);
            g.setColor(new Color(205, 255, 255, 220));
            g.setStroke(new BasicStroke(1.4f));
            g.draw(plate);
            g.setColor(new Color(255, 255, 255, 170));
            g.drawLine(17, y + 6, 8 + len - 8, y + 4);
        }
        g.setColor(new Color(18, 30, 70, 235));
        g.fillRoundRect(4, 48, 31, 33, 8, 8);
        g.setColor(cyan);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(4, 48, 31, 33, 8, 8);
        g.drawLine(12, 57, 29, 57);
        g.drawLine(12, 72, 29, 72);
    }

    public static BufferedImage halo(String name) {
        BufferedImage image = canvas();
        Graphics2D g = graphics(image);
        Color outer;
        Color inner;
        Color sparkle;
        switch (name) {
            case "Neon" -> { outer = new Color(42, 221, 255); inner = new Color(195, 255, 255); sparkle = new Color(255, 255, 255); }
            case "Srebrna" -> { outer = new Color(173, 190, 218); inner = new Color(250, 253, 255); sparkle = new Color(255, 255, 255); }
            case "Ognista" -> { outer = new Color(255, 91, 35); inner = new Color(255, 219, 104); sparkle = new Color(255, 245, 194); }
            case "Kosmiczna" -> { outer = new Color(145, 93, 255); inner = new Color(92, 225, 255); sparkle = new Color(255, 220, 255); }
            case "Szmaragdowa" -> { outer = new Color(36, 205, 139); inner = new Color(190, 255, 218); sparkle = new Color(238, 255, 245); }
            case "Mroczna" -> { outer = new Color(93, 65, 150); inner = new Color(190, 154, 255); sparkle = new Color(235, 222, 255); }
            case "Różowa" -> { outer = new Color(255, 105, 181); inner = new Color(255, 218, 242); sparkle = new Color(255, 250, 255); }
            default -> { outer = new Color(255, 190, 52); inner = new Color(255, 244, 169); sparkle = new Color(255, 255, 232); }
        }

        // Miękka poświata, podwójna obręcz i jasny rdzeń.
        for (int i = 9; i >= 1; i--) {
            int alpha = 7 + (10 - i) * 2;
            g.setColor(new Color(outer.getRed(), outer.getGreen(), outer.getBlue(), alpha));
            g.setStroke(new BasicStroke(5 + i * 2.1f));
            g.drawOval(20 - i, 27 - i, 88 + i * 2, 70 + i * 2);
        }
        g.setPaint(new GradientPaint(24, 28, inner, 106, 98, outer));
        g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawOval(22, 29, 84, 66);
        g.setColor(sparkle);
        g.setStroke(new BasicStroke(2.5f));
        g.drawOval(25, 32, 78, 60);

        // Małe kryształy rozmieszczone na obręczy.
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2.0 * i / 12.0;
            int cx = 64 + (int) Math.round(Math.cos(a) * 42);
            int cy = 62 + (int) Math.round(Math.sin(a) * 33);
            int r = i % 3 == 0 ? 5 : 3;
            Path2D gem = new Path2D.Float();
            gem.moveTo(cx, cy - r);
            gem.lineTo(cx + r, cy);
            gem.lineTo(cx, cy + r + 2);
            gem.lineTo(cx - r, cy);
            gem.closePath();
            g.setColor(outer);
            g.fill(gem);
            g.setColor(sparkle);
            g.setStroke(new BasicStroke(1f));
            g.draw(gem);
        }

        // Iskry poza pierścieniem dodają głębi i odróżniają wzory.
        Random random = new Random(name.hashCode());
        for (int i = 0; i < 22; i++) {
            int x = 12 + random.nextInt(104);
            int y = 17 + random.nextInt(94);
            int s = 1 + random.nextInt(3);
            g.setColor(new Color(sparkle.getRed(), sparkle.getGreen(), sparkle.getBlue(), 90 + random.nextInt(130)));
            g.fillOval(x, y, s, s);
        }
        g.dispose();
        return image;
    }
}
