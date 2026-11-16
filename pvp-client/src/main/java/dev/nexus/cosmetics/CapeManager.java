package dev.nexus.cosmetics;

import dev.nexus.NexusClient;
import dev.nexus.module.render.Capes;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.geom.Path2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Stream;

/** Ladowanie, skladanie i podmiana tekstury peleryny lokalnego gracza. */
public final class CapeManager {
    public static final List<String> BUILTIN = List.of("Nexus", "Eclipse", "Frost", "Inferno", "Void", "Emerald", "Royal", "Galaxy", "Crimson");
    private static final int MAX_FRAMES = 60;
    private static final int MAX_DIM = 2048;

    public record Frame(BufferedImage image, int delayMs) {}

    private static final List<Identifier> ids = new ArrayList<>();
    private static AssetInfo.TextureAssetInfo[] infos = new AssetInfo.TextureAssetInfo[0];
    private static int[] cumulative = new int[0];
    private static long startMs;
    private static float speed = 1f;
    private static int counter;

    private CapeManager() {}

    public static Path folder() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("nexus").resolve("cosmetics");
        try { Files.createDirectories(p); } catch (IOException ignored) { }
        return p;
    }

    public static Path subfolder(String name) {
        Path p = folder().resolve(name);
        try { Files.createDirectories(p); } catch (IOException ignored) { }
        return p;
    }

    public static List<String> availableNames() { return namesWith(BUILTIN, folder()); }

    public static List<String> namesWith(List<String> builtin, Path dir) {
        List<String> out = new ArrayList<>(builtin);
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(Files::isRegularFile).map(x -> x.getFileName().toString()).filter(n -> {
                String l = n.toLowerCase(Locale.ROOT);
                return l.endsWith(".png") || l.endsWith(".jpg") || l.endsWith(".jpeg") || l.endsWith(".gif") || l.endsWith(".bmp");
            }).sorted().forEach(out::add);
        } catch (IOException ignored) { }
        return out;
    }

    public static void setSpeed(float s) { speed = s; }

    /** Musi byc wywolane w watku renderowania (rejestruje tekstury GPU). */
    public static void load(String name, boolean cover) {
        unload();
        try {
            List<Frame> frames = new ArrayList<>();
            if (BUILTIN.contains(name)) {
                frames.add(new Frame(layout(Builtin.panel(name), 8), 100));
            } else {
                List<Frame> raw = decode(folder().resolve(name));
                int scale = raw.size() > 1 ? 4 : 8;
                for (Frame f : raw) frames.add(new Frame(compose(f.image(), cover, scale), f.delayMs()));
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            List<AssetInfo.TextureAssetInfo> list = new ArrayList<>();
            int[] cum = new int[frames.size()];
            int total = 0, i = 0;
            for (Frame f : frames) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ImageIO.write(f.image(), "png", bos);
                NativeImage ni = NativeImage.read(new ByteArrayInputStream(bos.toByteArray()));
                Identifier id = Identifier.of("nexusclient", "cape/" + (counter++));
                mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "nexus " + id, ni));
                ids.add(id);
                list.add(new AssetInfo.TextureAssetInfo(id, id));
                total += Math.max(20, f.delayMs());
                cum[i++] = total;
            }
            cumulative = cum;
            infos = list.toArray(new AssetInfo.TextureAssetInfo[0]);
            startMs = System.currentTimeMillis();
        } catch (Exception e) {
            NexusClient.LOGGER.error("Nie mozna zaladowac peleryny {}", name, e);
            unload();
        }
    }

    public static void unload() {
        MinecraftClient mc = MinecraftClient.getInstance();
        for (Identifier id : ids) mc.getTextureManager().destroyTexture(id);
        ids.clear();
        infos = new AssetInfo.TextureAssetInfo[0];
        cumulative = new int[0];
    }

    private static int frameIndex() {
        if (infos.length <= 1) return 0;
        int total = cumulative[cumulative.length - 1];
        long t = (long) ((System.currentTimeMillis() - startMs) * speed) % total;
        for (int i = 0; i < cumulative.length; i++) if (t < cumulative[i]) return i;
        return 0;
    }

    /** Wywolywane z mixinu: zwraca SkinTextures z nowa peleryna albo null (bez zmian). */
    public static SkinTextures skinFor(AbstractClientPlayerEntity player, SkinTextures original) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (player != mc.player || infos.length == 0) return null;
        Capes m = NexusClient.modules().get(Capes.class);
        if (m == null || !m.isEnabled()) return null;
        return new SkinTextures(original.body(), infos[frameIndex()], original.elytra(), original.model(), original.secure());
    }

    // ---------------------------------------------------------------- dekodowanie

    public static List<Frame> decodeFile(Path file) throws IOException { return decode(file); }

    static List<Frame> decode(Path file) throws IOException {
        List<Frame> out = new ArrayList<>();
        try (ImageInputStream in = ImageIO.createImageInputStream(file.toFile())) {
            Iterator<ImageReader> it = ImageIO.getImageReaders(in);
            if (!it.hasNext()) throw new IOException("Nieobslugiwany format pliku: " + file.getFileName());
            ImageReader reader = it.next();
            try {
                reader.setInput(in, false);
                boolean gif = "gif".equalsIgnoreCase(reader.getFormatName());
                int n = gif ? Math.min(reader.getNumImages(true), MAX_FRAMES) : 1;
                BufferedImage canvas = null;
                for (int i = 0; i < n; i++) {
                    BufferedImage f = reader.read(i);
                    if (f.getWidth() > MAX_DIM || f.getHeight() > MAX_DIM) throw new IOException("Obraz zbyt duzy (max " + MAX_DIM + ")");
                    if (!gif) { out.add(new Frame(f, 100)); break; }

                    int left = 0, top = 0, delay = 100;
                    String disposal = "none";
                    IIOMetadata md = reader.getImageMetadata(i);
                    Node root = md.getAsTree("javax_imageio_gif_image_1.0");
                    for (Node c = root.getFirstChild(); c != null; c = c.getNextSibling()) {
                        NamedNodeMap at = c.getAttributes();
                        if (at == null) continue;
                        if ("ImageDescriptor".equals(c.getNodeName())) {
                            left = Integer.parseInt(at.getNamedItem("imageLeftPosition").getNodeValue());
                            top = Integer.parseInt(at.getNamedItem("imageTopPosition").getNodeValue());
                        } else if ("GraphicControlExtension".equals(c.getNodeName())) {
                            delay = Integer.parseInt(at.getNamedItem("delayTime").getNodeValue()) * 10;
                            disposal = at.getNamedItem("disposalMethod").getNodeValue();
                        }
                    }
                    if (canvas == null) {
                        int cw = f.getWidth() + left, ch = f.getHeight() + top;
                        try {
                            Node sm = reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0");
                            for (Node c = sm.getFirstChild(); c != null; c = c.getNextSibling()) {
                                if ("LogicalScreenDescriptor".equals(c.getNodeName())) {
                                    cw = Integer.parseInt(c.getAttributes().getNamedItem("logicalScreenWidth").getNodeValue());
                                    ch = Integer.parseInt(c.getAttributes().getNamedItem("logicalScreenHeight").getNodeValue());
                                }
                            }
                        } catch (Exception ignored) { }
                        canvas = new BufferedImage(Math.max(1, cw), Math.max(1, ch), BufferedImage.TYPE_INT_ARGB);
                    }
                    Graphics2D g = canvas.createGraphics();
                    g.drawImage(f, left, top, null);
                    g.dispose();
                    BufferedImage copy = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D cg = copy.createGraphics();
                    cg.drawImage(canvas, 0, 0, null);
                    cg.dispose();
                    out.add(new Frame(copy, delay <= 0 ? 100 : delay));
                    if ("restoreToBackgroundColor".equals(disposal)) {
                        Graphics2D d = canvas.createGraphics();
                        d.setComposite(AlphaComposite.Clear);
                        d.fillRect(left, top, f.getWidth(), f.getHeight());
                        d.dispose();
                    }
                }
            } finally {
                reader.dispose();
            }
        }
        if (out.isEmpty()) throw new IOException("Brak klatek w pliku");
        return out;
    }

    // ---------------------------------------------------------------- sklejanie tekstury 64x32

    private static BufferedImage compose(BufferedImage src, boolean cover, int s) {
        int pw = 10 * s, ph = 16 * s;
        BufferedImage panel = new BufferedImage(pw, ph, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = panel.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, pw, ph);
        int sw = src.getWidth(), sh = src.getHeight();
        if (cover) {
            double sc = Math.max((double) pw / sw, (double) ph / sh);
            int dw = (int) Math.round(sw * sc), dh = (int) Math.round(sh * sc);
            g.drawImage(src, (pw - dw) / 2, (ph - dh) / 2, dw, dh, null);
        } else {
            g.drawImage(src, 0, 0, pw, ph, null);
        }
        g.dispose();
        return layout(panel, s);
    }

    /** Uklad tekstury peleryny: zewnetrzny panel (1,1) 10x16, wewnetrzny (12,1), krawedzie wokol. */
    static BufferedImage layout(BufferedImage panel, int s) {
        int pw = panel.getWidth(), ph = panel.getHeight();
        BufferedImage out = new BufferedImage(64 * s, 32 * s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setColor(new Color(0x101010));
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.drawImage(panel, s, s, 10 * s, 16 * s, null);
        g.setColor(new Color(0x161616));
        g.fillRect(12 * s, s, 10 * s, 16 * s);
        g.drawImage(panel.getSubimage(0, 0, pw, 1), s, 0, 10 * s, s, null);
        g.drawImage(panel.getSubimage(0, ph - 1, pw, 1), 11 * s, 0, 10 * s, s, null);
        g.drawImage(panel.getSubimage(0, 0, 1, ph), 0, s, s, 16 * s, null);
        g.drawImage(panel.getSubimage(pw - 1, 0, 1, ph), 11 * s, s, s, 16 * s, null);
        g.dispose();
        return out;
    }

    // ---------------------------------------------------------------- wbudowane wzory (panel 80x128)

    private static final class Builtin {
        static BufferedImage panel(String name) {
            BufferedImage p = new BufferedImage(80, 128, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = p.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            switch (name) {
                case "Eclipse" -> eclipse(g);
                case "Frost" -> frost(g);
                case "Inferno" -> inferno(g);
                case "Void" -> voidCape(g);
                case "Emerald" -> emerald(g);
                case "Royal" -> royal(g);
                case "Galaxy" -> galaxy(g);
                case "Crimson" -> crimson(g);
                default -> nexus(g);
            }
            g.dispose();
            return p;
        }

        private static void background(Graphics2D g, Color top, Color bottom) {
            g.setPaint(new GradientPaint(0, 0, top, 0, 128, bottom));
            g.fillRect(0, 0, 80, 128);
        }

        private static void frame(Graphics2D g, Color color) {
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
            g.setStroke(new BasicStroke(1f));
            g.drawRect(3, 3, 73, 121);
            g.setColor(color);
            g.setStroke(new BasicStroke(2f));
            g.drawLine(5, 5, 5, 123);
            g.drawLine(75, 5, 75, 123);
            g.drawLine(5, 123, 75, 123);
        }

        private static void diamond(Graphics2D g, int cx, int cy, int rx, int ry, Color fill, Color outline) {
            Path2D d = new Path2D.Float();
            d.moveTo(cx, cy - ry);
            d.lineTo(cx + rx, cy);
            d.lineTo(cx, cy + ry);
            d.lineTo(cx - rx, cy);
            d.closePath();
            g.setColor(fill);
            g.fill(d);
            if (outline != null) {
                g.setColor(outline);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(d);
            }
        }

        private static void star(Graphics2D g, int cx, int cy, int r, Color color) {
            Path2D s = new Path2D.Float();
            for (int i = 0; i < 10; i++) {
                double a = -Math.PI / 2 + i * Math.PI / 5;
                double radius = (i % 2 == 0) ? r : r * 0.38;
                double x = cx + Math.cos(a) * radius;
                double y = cy + Math.sin(a) * radius;
                if (i == 0) s.moveTo(x, y); else s.lineTo(x, y);
            }
            s.closePath();
            g.setColor(color);
            g.fill(s);
        }

        private static void nexus(Graphics2D g) {
            background(g, new Color(7, 9, 18), new Color(30, 34, 48));
            Random r = new Random(7);
            g.setColor(new Color(225, 235, 255, 145));
            for (int i = 0; i < 45; i++) g.fillRect(r.nextInt(80), r.nextInt(128), 1, 1);
            frame(g, new Color(210, 220, 242));
            g.setColor(new Color(255, 255, 255, 35));
            g.setStroke(new BasicStroke(1f));
            for (int y = 12; y < 120; y += 12) g.drawLine(10, y, 70, y);
            g.setColor(new Color(0, 0, 0, 210));
            g.fillOval(17, 28, 46, 46);
            g.setColor(new Color(230, 238, 255));
            g.setStroke(new BasicStroke(2.5f));
            g.drawOval(17, 28, 46, 46);
            g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 29));
            g.drawString("N", 28, 63);
            diamond(g, 40, 91, 12, 17, new Color(214, 224, 245), new Color(255, 255, 255));
            g.setColor(new Color(255, 255, 255, 180));
            g.fillRect(7, 119, 66, 3);
        }

        private static void eclipse(Graphics2D g) {
            background(g, new Color(6, 8, 28), new Color(52, 35, 91));
            for (int i = 10; i >= 1; i--) {
                g.setColor(new Color(135, 90, 255, 9 + i * 2));
                g.fillOval(40 - (18 + i * 3), 48 - (18 + i * 3), (18 + i * 3) * 2, (18 + i * 3) * 2);
            }
            g.setColor(new Color(9, 10, 29));
            g.fillOval(20, 28, 40, 40);
            g.setColor(new Color(205, 174, 255));
            g.setStroke(new BasicStroke(2.5f));
            g.drawOval(19, 27, 42, 42);
            g.setColor(new Color(255, 204, 111));
            g.setStroke(new BasicStroke(1.4f));
            g.drawOval(12, 20, 56, 56);
            g.drawArc(8, 45, 64, 38, 190, 160);
            g.setColor(new Color(190, 158, 255, 170));
            g.drawLine(8, 90, 72, 90);
            diamond(g, 40, 105, 8, 13, new Color(169, 125, 255), new Color(241, 220, 255));
            frame(g, new Color(153, 124, 226));
        }

        private static void frost(Graphics2D g) {
            background(g, new Color(225, 247, 255), new Color(59, 114, 167));
            g.setColor(new Color(255, 255, 255, 100));
            for (int x = -100; x < 100; x += 18) {
                Path2D shard = new Path2D.Float();
                shard.moveTo(x, 128);
                shard.lineTo(x + 38, 0);
                shard.lineTo(x + 51, 0);
                shard.lineTo(x + 13, 128);
                shard.closePath();
                g.fill(shard);
            }
            diamond(g, 40, 49, 23, 31, new Color(139, 213, 248, 180), Color.WHITE);
            diamond(g, 40, 49, 12, 22, new Color(239, 254, 255), new Color(81, 156, 205));
            g.setColor(new Color(255, 255, 255, 210));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(40, 16, 40, 82);
            g.drawLine(20, 49, 60, 49);
            frame(g, new Color(239, 252, 255));
        }

        private static void inferno(Graphics2D g) {
            background(g, new Color(48, 8, 13), new Color(161, 35, 18));
            Random r = new Random(19);
            for (int i = 0; i < 18; i++) {
                int x = r.nextInt(80);
                int y = 35 + r.nextInt(88);
                Path2D flame = new Path2D.Float();
                flame.moveTo(x, 128);
                flame.curveTo(x - 13, y + 25, x + 15, y + 10, x + 4, y - 17);
                flame.curveTo(x + 23, y + 2, x + 24, y + 24, x + 14, 128);
                flame.closePath();
                g.setColor(new Color(255, 125 + r.nextInt(100), 35, 70 + r.nextInt(100)));
                g.fill(flame);
            }
            g.setColor(new Color(255, 207, 103));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(8, 16, 8, 116);
            g.drawLine(72, 16, 72, 116);
            diamond(g, 40, 57, 24, 31, new Color(93, 13, 13, 210), new Color(255, 210, 119));
            diamond(g, 40, 57, 12, 19, new Color(255, 102, 23), new Color(255, 234, 162));
            star(g, 40, 57, 10, new Color(255, 245, 185));
            frame(g, new Color(255, 180, 87));
        }

        private static void voidCape(Graphics2D g) {
            background(g, new Color(4, 4, 12), new Color(33, 16, 61));
            Random r = new Random(91);
            for (int i = 0; i < 55; i++) {
                int x = r.nextInt(80), y = r.nextInt(128), s = 1 + r.nextInt(2);
                g.setColor(new Color(193, 143, 255, 90 + r.nextInt(130)));
                g.fillOval(x, y, s, s);
            }
            for (int i = 5; i >= 1; i--) {
                g.setColor(new Color(131, 65, 206, 18));
                g.fillOval(40 - i * 12, 52 - i * 12, i * 24, i * 24);
            }
            g.setColor(new Color(12, 7, 26));
            g.fillOval(17, 29, 46, 46);
            g.setColor(new Color(183, 126, 255));
            g.setStroke(new BasicStroke(2f));
            g.drawOval(17, 29, 46, 46);
            g.setColor(new Color(120, 83, 193));
            g.drawOval(10, 22, 60, 60);
            diamond(g, 40, 53, 12, 22, new Color(115, 73, 181), new Color(227, 190, 255));
            star(g, 40, 53, 7, new Color(245, 225, 255));
            frame(g, new Color(139, 103, 194));
        }

        private static void emerald(Graphics2D g) {
            background(g, new Color(4, 47, 38), new Color(12, 104, 69));
            g.setColor(new Color(75, 225, 157, 45));
            for (int y = -40; y < 170; y += 22) {
                g.drawLine(0, y, 80, y + 40);
                g.drawLine(80, y, 0, y + 40);
            }
            g.setColor(new Color(255, 220, 126));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(9, 8, 9, 120);
            g.drawLine(71, 8, 71, 120);
            diamond(g, 40, 55, 27, 35, new Color(9, 71, 54), new Color(255, 220, 126));
            diamond(g, 40, 55, 17, 25, new Color(39, 197, 124), new Color(220, 255, 221));
            diamond(g, 40, 55, 8, 14, new Color(194, 255, 218), Color.WHITE);
            g.setColor(new Color(255, 222, 139));
            g.fillRect(18, 101, 44, 3);
            frame(g, new Color(109, 224, 164));
        }

        private static void royal(Graphics2D g) {
            background(g, new Color(29, 9, 54), new Color(91, 35, 123));
            g.setColor(new Color(255, 213, 124));
            g.setStroke(new BasicStroke(2f));
            g.drawRect(8, 8, 64, 112);
            g.drawRect(12, 12, 56, 104);
            Path2D crown = new Path2D.Float();
            crown.moveTo(20, 42); crown.lineTo(16, 22); crown.lineTo(31, 32);
            crown.lineTo(40, 14); crown.lineTo(49, 32); crown.lineTo(64, 22);
            crown.lineTo(60, 42); crown.closePath();
            g.setPaint(new GradientPaint(18, 20, new Color(255, 244, 184), 60, 44, new Color(197, 126, 35)));
            g.fill(crown);
            g.setColor(new Color(255, 240, 179));
            g.draw(crown);
            diamond(g, 40, 74, 21, 27, new Color(67, 20, 94), new Color(255, 218, 126));
            diamond(g, 40, 74, 10, 17, new Color(191, 83, 212), new Color(255, 236, 178));
            g.setColor(new Color(255, 219, 137));
            g.fillOval(36, 70, 8, 8);
            frame(g, new Color(237, 190, 104));
        }

        private static void galaxy(Graphics2D g) {
            background(g, new Color(7, 11, 42), new Color(58, 27, 104));
            Random r = new Random(2025);
            for (int i = 0; i < 75; i++) {
                int x = r.nextInt(80), y = r.nextInt(128), s = 1 + r.nextInt(2);
                g.setColor(new Color(205, 205 + r.nextInt(50), 255, 90 + r.nextInt(140)));
                g.fillOval(x, y, s, s);
            }
            g.setColor(new Color(122, 100, 245, 90));
            g.fillOval(9, 26, 62, 62);
            g.setColor(new Color(224, 170, 255));
            g.setStroke(new BasicStroke(3f));
            g.drawOval(13, 31, 54, 54);
            g.setColor(new Color(108, 202, 255));
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(20, 20, 40, 78);
            g.drawOval(3, 43, 74, 32);
            star(g, 40, 57, 15, new Color(255, 245, 205));
            star(g, 18, 23, 4, Color.WHITE);
            star(g, 64, 96, 5, new Color(182, 226, 255));
            frame(g, new Color(168, 144, 255));
        }

        private static void crimson(Graphics2D g) {
            background(g, new Color(18, 5, 10), new Color(111, 10, 29));
            g.setColor(new Color(230, 38, 64, 90));
            for (int i = -100; i < 120; i += 18) {
                Path2D stripe = new Path2D.Float();
                stripe.moveTo(i, 0); stripe.lineTo(i + 10, 0);
                stripe.lineTo(i + 80, 128); stripe.lineTo(i + 70, 128);
                stripe.closePath();
                g.fill(stripe);
            }
            g.setColor(new Color(235, 211, 218));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(9, 10, 9, 118);
            g.drawLine(71, 10, 71, 118);
            diamond(g, 40, 55, 26, 34, new Color(32, 7, 16), new Color(238, 186, 198));
            diamond(g, 40, 55, 15, 23, new Color(173, 15, 48), new Color(255, 223, 226));
            star(g, 40, 55, 10, new Color(255, 226, 228));
            g.setColor(new Color(255, 225, 230));
            g.fillRect(18, 104, 44, 3);
            frame(g, new Color(208, 104, 126));
        }
    }
}
