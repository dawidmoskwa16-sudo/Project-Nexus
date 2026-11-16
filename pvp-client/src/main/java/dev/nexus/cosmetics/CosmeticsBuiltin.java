package dev.nexus.cosmetics;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

/** Wbudowane tekstury skrzydel (128x128, czubek skrzydla po prawej) i aureoli (128x128, pierscien). */
public final class CosmeticsBuiltin {
    public static final List<String> WINGS = List.of("Anielskie", "Nietoperz", "Kryształ");
    public static final List<String> HALOS = List.of("Złota", "Neon", "Srebrna");

    private CosmeticsBuiltin() {}

    private static BufferedImage canvas() { return new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB); }

    private static Graphics2D g(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }

    public static BufferedImage wings(String name) {
        BufferedImage img = canvas();
        Graphics2D g = g(img);
        switch (WINGS.indexOf(name)) {
            case 1 -> bat(g);
            case 2 -> crystal(g);
            default -> angel(g);
        }
        g.dispose();
        return img;
    }

    private static void angel(Graphics2D g) {
        for (int i = 0; i < 9; i++) {
            double ang = Math.toRadians(-38 + i * 10.0);
            float len = 100 - Math.abs(i - 4) * 7;
            AffineTransform old = g.getTransform();
            g.translate(6, 50);
            g.rotate(ang);
            RoundRectangle2D.Float f = new RoundRectangle2D.Float(0, -7, len, 14, 14, 14);
            g.setPaint(new GradientPaint(0, 0, Color.WHITE, len, 0, new Color(196, 208, 232)));
            g.fill(f);
            g.setColor(new Color(0, 0, 0, 45));
            g.setStroke(new BasicStroke(1f));
            g.draw(f);
            g.setTransform(old);
        }
    }

    private static void bat(Graphics2D g) {
        Path2D p = new GeneralPath();
        p.moveTo(6, 40);
        p.lineTo(120, 12);
        p.lineTo(100, 56);
        p.lineTo(88, 48);
        p.lineTo(76, 92);
        p.lineTo(62, 80);
        p.lineTo(46, 122);
        p.lineTo(30, 100);
        p.lineTo(10, 118);
        p.closePath();
        g.setPaint(new GradientPaint(0, 0, new Color(18, 18, 24), 128, 0, new Color(52, 52, 70)));
        g.fill(p);
        g.setColor(new Color(150, 150, 175));
        g.setStroke(new BasicStroke(2f));
        g.draw(p);
        g.setColor(new Color(110, 110, 135));
        g.drawLine(6, 40, 100, 56);
        g.drawLine(6, 40, 76, 92);
        g.drawLine(6, 40, 46, 122);
        g.drawLine(6, 40, 10, 118);
    }

    private static void crystal(Graphics2D g) {
        Random r = new Random(11);
        for (int i = 0; i < 7; i++) {
            double ang = Math.toRadians(-40 + i * 13.0);
            float len = 104 - Math.abs(i - 3) * 9;
            AffineTransform old = g.getTransform();
            g.translate(6, 52);
            g.rotate(ang);
            GeneralPath s = new GeneralPath();
            s.moveTo(0, -6);
            s.lineTo(len * 0.75f, -9);
            s.lineTo(len, 0);
            s.lineTo(len * 0.75f, 9);
            s.lineTo(0, 6);
            s.closePath();
            g.setPaint(new GradientPaint(0, 0, new Color(160, 235, 255, 230), len, 0, new Color(90, 130, 255, 150 + r.nextInt(60))));
            g.fill(s);
            g.setColor(new Color(255, 255, 255, 190));
            g.setStroke(new BasicStroke(1.5f));
            g.draw(s);
            g.setTransform(old);
        }
    }

    public static BufferedImage halo(String name) {
        BufferedImage img = canvas();
        Graphics2D g = g(img);
        Color glow, core;
        switch (HALOS.indexOf(name)) {
            case 1 -> { glow = new Color(70, 220, 255); core = new Color(200, 250, 255); }
            case 2 -> { glow = new Color(200, 205, 225); core = Color.WHITE; }
            default -> { glow = new Color(255, 200, 50); core = new Color(255, 240, 160); }
        }
        for (int k = 0; k < 10; k++) {
            g.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 14));
            g.setStroke(new BasicStroke(8 + k * 2.2f));
            g.drawOval(18, 18, 92, 92);
        }
        g.setColor(glow);
        g.setStroke(new BasicStroke(7f));
        g.drawOval(18, 18, 92, 92);
        g.setColor(core);
        g.setStroke(new BasicStroke(3f));
        g.drawOval(18, 18, 92, 92);
        g.dispose();
        return img;
    }
}
