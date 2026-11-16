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
import java.util.stream.Stream;

/** Ladowanie, skladanie i podmiana tekstury peleryny lokalnego gracza. */
public final class CapeManager {
    public static final List<String> BUILTIN = List.of("Nexus", "Eclipse", "Frost", "Royal");
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
                case "Royal" -> royal(g);
                default -> nexus(g);
            }
            g.dispose();
            return p;
        }

        private static void base(Graphics2D g, Color top, Color bottom) {
            g.setPaint(new GradientPaint(0, 0, top, 0, 128, bottom));
            g.fillRect(0, 0, 80, 128);
        }

        private static void sideStitch(Graphics2D g, Color color) {
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 180));
            g.setStroke(new BasicStroke(1f));
            g.drawLine(7, 5, 7, 121);
            g.drawLine(72, 5, 72, 121);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
            g.drawLine(10, 5, 10, 121);
            g.drawLine(69, 5, 69, 121);
        }

        private static void diamond(Graphics2D g, int cx, int cy, int rx, int ry, Color fill, Color edge) {
            Path2D p = new Path2D.Float();
            p.moveTo(cx, cy - ry);
            p.lineTo(cx + rx, cy);
            p.lineTo(cx, cy + ry);
            p.lineTo(cx - rx, cy);
            p.closePath();
            g.setColor(fill);
            g.fill(p);
            g.setColor(edge);
            g.setStroke(new BasicStroke(1.3f));
            g.draw(p);
        }

        private static void nexus(Graphics2D g) {
            base(g, new Color(13, 17, 25), new Color(27, 34, 47));
            sideStitch(g, new Color(166, 180, 199));
            g.setColor(new Color(255, 255, 255, 18));
            for (int y = 12; y < 120; y += 16) g.drawLine(13, y, 67, y);
            g.setColor(new Color(202, 211, 226));
            g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
            g.drawLine(25, 36, 25, 79);
            g.drawLine(25, 36, 55, 79);
            g.drawLine(55, 36, 55, 79);
            g.setColor(new Color(126, 143, 170));
            g.setStroke(new BasicStroke(1f));
            g.drawLine(20, 86, 60, 86);
            g.drawLine(27, 91, 53, 91);
            g.setColor(new Color(211, 219, 233));
            g.fillRect(16, 116, 48, 2);
        }

        private static void eclipse(Graphics2D g) {
            base(g, new Color(12, 12, 21), new Color(34, 26, 43));
            sideStitch(g, new Color(142, 118, 163));
            g.setColor(new Color(9, 9, 16));
            g.fillOval(16, 26, 48, 48);
            g.setColor(new Color(180, 151, 198));
            g.setStroke(new BasicStroke(2.2f));
            g.drawOval(18, 28, 44, 44);
            g.setColor(new Color(226, 204, 164));
            g.setStroke(new BasicStroke(1.4f));
            g.drawArc(11, 21, 58, 58, 205, 225);
            g.setColor(new Color(212, 196, 222, 95));
            g.drawLine(15, 92, 65, 92);
            g.drawLine(21, 98, 59, 98);
            g.setColor(new Color(189, 164, 207));
            g.fillRect(38, 82, 4, 25);
        }

        private static void frost(Graphics2D g) {
            base(g, new Color(17, 31, 47), new Color(43, 75, 93));
            sideStitch(g, new Color(157, 207, 223));
            g.setColor(new Color(191, 229, 236, 42));
            g.setStroke(new BasicStroke(1f));
            g.drawLine(13, 18, 67, 72);
            g.drawLine(67, 18, 13, 72);
            g.setColor(new Color(199, 233, 239));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(40, 24, 40, 92);
            g.drawLine(24, 48, 56, 48);
            g.drawLine(29, 33, 51, 63);
            g.drawLine(51, 33, 29, 63);
            diamond(g, 40, 48, 10, 16, new Color(76, 133, 153), new Color(221, 245, 249));
            g.setColor(new Color(215, 239, 242));
            g.fillRect(18, 105, 44, 2);
            g.fillRect(25, 110, 30, 1);
        }

        private static void royal(Graphics2D g) {
            base(g, new Color(34, 16, 34), new Color(59, 25, 49));
            Color gold = new Color(206, 171, 111);
            g.setColor(new Color(gold.getRed(), gold.getGreen(), gold.getBlue(), 190));
            g.setStroke(new BasicStroke(1.4f));
            g.drawLine(8, 7, 8, 120);
            g.drawLine(71, 7, 71, 120);
            g.drawLine(8, 119, 71, 119);
            Path2D crest = new Path2D.Float();
            crest.moveTo(22, 31);
            crest.lineTo(40, 20);
            crest.lineTo(58, 31);
            crest.lineTo(54, 67);
            crest.lineTo(40, 80);
            crest.lineTo(26, 67);
            crest.closePath();
            g.setColor(new Color(20, 10, 24, 125));
            g.fill(crest);
            g.setColor(gold);
            g.setStroke(new BasicStroke(2f));
            g.draw(crest);
            diamond(g, 40, 48, 8, 14, new Color(133, 72, 100), new Color(244, 216, 165));
            g.setColor(new Color(233, 206, 157));
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine(25, 91, 55, 91);
            g.drawLine(30, 97, 50, 97);
            g.drawLine(35, 103, 45, 103);
        }
    }
}
