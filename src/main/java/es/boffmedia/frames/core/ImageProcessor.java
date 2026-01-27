package es.boffmedia.frames.core;

import es.boffmedia.frames.Frames;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Image download / resize / save helpers.
 */
public final class ImageProcessor {
    private ImageProcessor() {}

    public static BufferedImage downloadImage(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        Frames.LOGGER.atInfo().log("Downloading image from: " + urlStr);
        BufferedImage image = ImageIO.read(url);
        if (image == null) throw new IOException("Failed to decode image from URL: " + urlStr);
        return image;
    }

    public static BufferedImage resizeImage(BufferedImage src, int targetSizeX, int targetSizeY) {
        BufferedImage scaled = new BufferedImage(targetSizeX, targetSizeY, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.drawImage(src, 0, 0, targetSizeX, targetSizeY, null);
        g.dispose();
        return scaled;
    }

    public static Path saveImageToMods(BufferedImage img, String fileName, String sizeKey, Path modsRoot) throws IOException {
        Path out = modsRoot.resolve(Path.of("Common", "Blocks", "Frames", "Images")).resolve(fileName);
        Files.createDirectories(out.getParent());
        boolean written = ImageIO.write(img, "png", out.toFile());
        if (!written) throw new IOException("ImageIO.write returned false for: " + out.toString());
        return out;
    }
}
