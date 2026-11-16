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

/**
 * Clean, restrained built-in designs. Shapes are deliberately readable at Minecraft texture scale.
 */
public final class CosmeticsBuiltin {
    public static final List<String> WINGS = List.of("Pióra", "Cień", "Kryształ");
    public static final List<String> HALOS = List.of("Klasyczna", "Orbit", "Srebrna");

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
            case "Cień" -> shadowWing(g);
            case "Kryształ" -> crystalWing(g);
            default -> featherWing(g);
        }
        g.dispose();
        return image;
    }

    /** A restrained fan of layered feathers, with a single pale center quill. */
    private static void featherWing(Graphics2D g) {
        Color ivory = new Color(236, 232, 218);
        Color shade = new Color(160, 166, 179);
        Color edge = new Color(255, 250, 235);
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(-47 + i * 13);
            float length = 91 - Math.abs(i - 3.5f) * 5.5f;
            AffineTransform old = g.getTransform();
            g.translate(13, 66);
            g.rotate(angle);

            Path2D feather = new Path2D.Float();
            feather.moveTo(0, -4);
            feather.curveTo(length * 0.22, -11, length * 0.74, -7, length, 0);
            feather.curveTo(length * 0.72, 7, length * 0.22, 10, 0, 4);
            feather.closePath();

            g.setPaint(new GradientPaint(0, -8, edge, length, 4, shade));
            g.fill(feather);
            g.setColor(new Color(87, 94, 111, 150));
            g.setStroke(new BasicStroke(0.8f));
            g.draw(feather);
            g.setColor(new Color(255, 255, 255, 170));
            g.drawLine(6, 0, (int) (length * 0.82f), 0);
            g.setTransform(old);
        }

        g.setPaint(new GradientPaint(6, 52, Color.WHITE, 33, 82, new Color(175, 181, 194)));
        g.fill(new RoundRectangle2D.Float(5, 49, 32, 31, 10, 10));
        g.setColor(new Color(115, 122, 139, 190));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(5, 49, 32, 31, 10, 10);
        g.setColor(new Color(255, 255, 255, 190));
        g.drawLine(13, 55, 29, 74);
    }

    /** Simple dark membrane with deliberate ribs instead of glow/noise. */
    private static void shadowWing(Graphics2D g) {
        Path2D wing = new Path2D.Float();
        wing.moveTo(9, 43);
        wing.lineTo(119, 13);
        wing.lineTo(103, 52);
        wing.lineTo(91, 46);
        wing.lineTo(78, 91);
        wing.lineTo(63, 79);
        wing.lineTo(47, 119);
        wing.lineTo(31, 99);
        wing.lineTo(11, 116);
        wing.closePath();

        g.setPaint(new GradientPaint(10, 24, new Color(66, 54, 83), 95, 112, new Color(18, 17, 27)));
        g.fill(wing);
        g.setColor(new Color(133, 112, 159));
        g.setStroke(new BasicStroke(2f));
        g.draw(wing);
        g.setColor(new Color(174, 154, 197, 185));
        g.setStroke(new BasicStroke(1.4f));
        int[][] tips = {{103, 52}, {78, 91}, {47, 119}, {11, 116}};
        for (int[] tip : tips) g.drawLine(9, 43, tip[0], tip[1]);
        g.setColor(new Color(216, 205, 225, 125));
        g.drawLine(15, 42, 111, 17);
    }

    /** Three clean faceted shards with subtle internal highlights. */
    private static void crystalWing(Graphics2D g) {
        Color[] fills = {
                new Color(112, 189, 216, 225),
                new Color(167, 222, 235, 235),
                new Color(111, 150, 203, 225)
        };
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(-40 + i * 19);
            float length = 98 - Math.abs(i - 2) * 9;
            AffineTransform old = g.getTransform();
            g.translate(10, 65);
            g.rotate(angle);
            Path2D shard = new Path2D.Float();
            shard.moveTo(0, -5);
            shard.lineTo(length * 0.72, -9);
            shard.lineTo(length, 0);
            shard.lineTo(length * 0.72, 9);
            shard.lineTo(0, 5);
            shard.closePath();
            g.setPaint(new GradientPaint(0, -8, new Color(236, 252, 255), length, 6, fills[i % fills.length]));
            g.fill(shard);
            g.setColor(new Color(230, 249, 255, 230));
            g.setStroke(new BasicStroke(1.2f));
            g.draw(shard);
            g.setColor(new Color(58, 111, 157, 150));
            g.drawLine(6, 0, (int) (length * 0.72f), 0);
            g.setTransform(old);
        }
        g.setColor(new Color(210, 240, 248, 230));
        g.fillRoundRect(5, 57, 28, 16, 6, 6);
    }

    public static BufferedImage halo(String name) {
        BufferedImage image = canvas();
        Graphics2D g = graphics(image);

        Color metal;
        Color highlight;
        Color gem;
        switch (name) {
            case "Orbit" -> {
                metal = new Color(111, 157, 190);
                highlight = new Color(221, 241, 250);
                gem = new Color(125, 198, 220);
            }
            case "Srebrna" -> {
                metal = new Color(149, 158, 174);
                highlight = new Color(247, 249, 252);
                gem = new Color(198, 211, 225);
            }
            default -> {
                metal = new Color(173, 128, 53);
                highlight = new Color(250, 218, 145);
                gem = new Color(244, 224, 176);
            }
        }

        // Thin metal band with a second inner line; no oversized bloom.
        g.setColor(new Color(metal.getRed(), metal.getGreen(), metal.getBlue(), 45));
        g.setStroke(new BasicStroke(12f));
        g.drawOval(20, 38, 88, 48);
        g.setPaint(new GradientPaint(24, 42, highlight, 104, 82, metal));
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawOval(20, 38, 88, 48);
        g.setColor(new Color(255, 255, 255, 190));
        g.setStroke(new BasicStroke(1.3f));
        g.drawArc(24, 41, 80, 42, 190, 155);

        if ("Orbit".equals(name)) {
            g.setColor(new Color(gem.getRed(), gem.getGreen(), gem.getBlue(), 225));
            g.setStroke(new BasicStroke(2.2f));
            g.drawOval(30, 25, 68, 74);
            g.setColor(highlight);
            g.fillOval(93, 34, 7, 7);
        } else {
            // Three small inset stones, evenly spaced along the front half.
            stone(g, 38, 70, 4, gem, highlight);
            stone(g, 64, 83, 5, gem, highlight);
            stone(g, 90, 70, 4, gem, highlight);
        }

        g.dispose();
        return image;
    }

    private static void stone(Graphics2D g, int x, int y, int radius, Color fill, Color edge) {
        Path2D p = new Path2D.Float();
        p.moveTo(x, y - radius);
        p.lineTo(x + radius, y);
        p.lineTo(x, y + radius);
        p.lineTo(x - radius, y);
        p.closePath();
        g.setColor(fill);
        g.fill(p);
        g.setColor(edge);
        g.setStroke(new BasicStroke(1f));
        g.draw(p);
    }
}
