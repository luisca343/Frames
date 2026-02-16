package es.boffmedia.frames.core;

import es.boffmedia.frames.AssetJsonBuilder;
import es.boffmedia.frames.Frames;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;

/**
 * Generates blockymodels, textures and item JSON files for dynamic frames.
 */
public final class FrameItemGenerator {
    private FrameItemGenerator() {}

    private static final SecureRandom RNG = new SecureRandom();
    private static final String NAME_ALPHANUM = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final String NAME_FIRST_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static String generateRandomName(int length) {
        if (length <= 0) return "";
        StringBuilder sb = new StringBuilder(length);
        sb.append(NAME_FIRST_LETTERS.charAt(RNG.nextInt(NAME_FIRST_LETTERS.length())));
        for (int i = 1; i < length; i++) sb.append(NAME_ALPHANUM.charAt(RNG.nextInt(NAME_ALPHANUM.length())));
        return sb.toString();
    }

    private static BufferedImage padToMultipleOf32(BufferedImage image) {
        if (image == null) return null;
        int w = image.getWidth();
        int h = image.getHeight();
        int newW = ((w + 31) / 32) * 32;
        int newH = ((h + 31) / 32) * 32;

        Frames.LOGGER.atInfo().log("Padding image from " + w + "x" + h + " to " + newW + "x" + newH);

        if (newW == w && newH == h) return image;
        BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(image, 0, 0, newW, newH, null);
        g.dispose();
        return out;
    }

    public static String addImageAsItemFromImage(BufferedImage image, String providedName, int blocksX, int blocksY, String alignment, Path modsRoot) throws IOException {
        if (image == null) throw new IOException("Provided image is null");

        // We need to pad the image to a multiple of 32 pixels in both dimensions to avoid a bug in Hytale's rendering engine
        image = padToMultipleOf32(image);

        int imgPixelsX = image.getWidth();
        int imgPixelsY = image.getHeight();

        int sizeX = Math.max(1, imgPixelsX);
        int sizeY = Math.max(1, imgPixelsY);

        String baseName = null;
        if (providedName != null) {
            String n = providedName.trim();
            if (!n.isEmpty()) {
                n = n.replaceAll("\\s+", "_");
                String[] parts = n.split("[^a-zA-Z0-9_]");
                if (parts.length > 0) {
                    String candidate = parts[0];
                    if (candidate.length() > 0) {
                        if (candidate.length() == 1) candidate = candidate.toUpperCase();
                        else candidate = candidate.substring(0, 1).toUpperCase() + candidate.substring(1);
                        baseName = candidate;
                    }
                }
            }
        }
        if (baseName == null || baseName.isEmpty()) baseName = generateRandomName(8);

        int w = Math.max(1, sizeX / 32);
        int h = Math.max(1, sizeY / 32);
        String sizeKey = w + "x" + h;

        String fileName = baseName + ".png";
        Path out = ImageProcessor.saveImageToMods(image, fileName, sizeKey, modsRoot);
        String texturePath = "Blocks/Frames/Images/" + fileName;

        Path modelOut = modsRoot.resolve(Paths.get("Common", "Blocks", "Frames", baseName + ".blockymodel"));
        Files.createDirectories(modelOut.getParent());

        float zPosition = ((float) sizeX) / (-blocksX * 2);
        int computedBlocksY = Math.max(1, Math.round((float) blocksX * (float) imgPixelsY / (float) imgPixelsX));
        float yPosition = ((float) sizeY) / ((float) computedBlocksY * 2.0f);

        int offsetX = 0;
        int offsetY = 0;
        int offsetZ = 0;
        if (alignment != null) {
            switch (alignment.toUpperCase()) {
                case "BOTTOM_LEFT":
                    offsetX = Math.round(((float) sizeX / 2.0f) - Math.abs(zPosition));
                    offsetY = Math.round(((float) sizeY / 2.0f) - Math.abs(yPosition));
                    offsetZ = 0;
                    break;
                    
                case "BOTTOM_RIGHT":
                    offsetX = Math.round(-((float) sizeX / 2.0f) + Math.abs(zPosition));
                    offsetY = Math.round(((float) sizeY / 2.0f) - Math.abs(yPosition));
                    offsetZ = 0;
                    break;
                    
                case "BOTTOM_CENTER":
                    offsetX = 0;
                    offsetY = Math.round(((float) sizeY / 2.0f) - Math.abs(yPosition));
                    offsetZ = 0;
                    break;
                    
                case "TOP_LEFT":
                    offsetX = Math.round(((float) sizeX / 2.0f) - Math.abs(zPosition));
                    offsetY = Math.round(-((float) sizeY / 2.0f) + Math.abs(yPosition));
                    offsetZ = 0;
                    break;
                    
                case "TOP_CENTER":
                    offsetX = 0;
                    offsetY = Math.round(-((float) sizeY / 2.0f) + Math.abs(yPosition));
                    offsetZ = 0;
                    break;
                    
                case "TOP_RIGHT":
                    offsetX = Math.round(-((float) sizeX / 2.0f) + Math.abs(zPosition));
                    offsetY = Math.round(-((float) sizeY / 2.0f) + Math.abs(yPosition));
                    offsetZ = 0;
                    break;
                    
                default:
                    offsetX = 0;
                    offsetY = 0;
                    offsetZ = 0;
                    break;
            }
        }

        String modelJson = AssetJsonBuilder.buildBlockymodel(baseName, sizeX, sizeY, (int) yPosition, (int) zPosition, offsetX, offsetY, offsetZ);
        Files.writeString(modelOut, modelJson);

        Path itemOut = modsRoot.resolve(Paths.get("Server", "Item", "Items", "Furniture", "Frames", "Boff_Frame_" + baseName + ".json"));
        Files.createDirectories(itemOut.getParent());

        float scaleFactor = ((float) Math.max(1, blocksX) * 32.0f) / (float) imgPixelsX;
        String itemJson = AssetJsonBuilder.buildItemJson(baseName, texturePath, scaleFactor);
        Files.writeString(itemOut, itemJson);

        String itemId = "Boff_Frame_" + baseName;
        Frames.LOGGER.atInfo().log("Created dynamic item " + itemId + " model=" + modelOut + " image=" + out + " json=" + itemOut);
        return itemId;
    }
}
