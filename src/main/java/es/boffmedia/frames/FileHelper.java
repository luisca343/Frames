package es.boffmedia.frames;

import es.boffmedia.frames.core.*;
import org.bson.BsonDocument;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

public final class FileHelper {
    public static final Path MODS_ROOT = Paths.get("mods", "BoffmediaFrames");
    public static final String[] FRAME_SIZES = new String[]{"1x1"};

    private FileHelper() {}

    public static void loadFiles() {
        Frames.LOGGER.atInfo().log("Mods folder exists: " + Files.exists(MODS_ROOT));
        try {
            ResourceCopier.ensureManifestExists(MODS_ROOT);
            ResourceCopier.copyResourceDirectory("/Common", MODS_ROOT.resolve("Common"));
            ResourceCopier.copyResourceDirectory("/Server", MODS_ROOT.resolve("Server"));

            for (String sk : FRAME_SIZES) {
                FrameDocumentManager.ensureDefaultJsonExists(sk, MODS_ROOT);
                BsonDocument doc = FrameDocumentManager.readDocument(sk, MODS_ROOT);
                Frames.LOGGER.atInfo().log("Document loaded for " + sk + ": " + (doc != null));
            }
        } catch (Exception e) {
            Frames.LOGGER.atSevere().withCause(e).log("Failed to ensure or load frame json: " + e.getMessage());
        }
    }

    public static void ensureManifestExists() {
        ResourceCopier.ensureManifestExists(MODS_ROOT);
    }

    public static void copyResourceDirectory(String resourcePath, Path outDir) throws IOException {
        ResourceCopier.copyResourceDirectory(resourcePath, outDir);
    }

    public static void ensureDefaultJsonExists(String sizeKey) throws IOException {
        FrameDocumentManager.ensureDefaultJsonExists(sizeKey, MODS_ROOT);
    }

    public static BsonDocument readDocument(String sizeKey) {
        return FrameDocumentManager.readDocument(sizeKey, MODS_ROOT);
    }

    public static void writeDocument(BsonDocument doc, String sizeKey) throws IOException {
        FrameDocumentManager.writeDocument(doc, sizeKey, MODS_ROOT);
    }

    public static void updateDocument(Consumer<BsonDocument> updater, String sizeKey) throws IOException {
        FrameDocumentManager.updateDocument(updater, sizeKey, MODS_ROOT);
    }

    public static BsonDocument loadOrCreateDocument(String sizeKey) throws IOException {
        return FrameDocumentManager.loadOrCreateDocument(sizeKey, MODS_ROOT);
    }

    public static void addStateToDocument(BsonDocument doc, String key, String texturePath) {
        FrameDocumentManager.addStateToDocument(doc, key, texturePath);
    }

    public static void prettyPrintAndSave(BsonDocument doc, String sizeKey) throws IOException {
        FrameDocumentManager.prettyPrintAndSave(doc, sizeKey, MODS_ROOT);
    }

    public static boolean removeImageState(String sizeKey, String stateKey) throws IOException {
        return FrameDocumentManager.removeImageState(sizeKey, stateKey, MODS_ROOT);
    }

    public static void writeFrameMetadata(String itemId, String name, String url, int x, int y, int z, int blocksX, String alignment) throws IOException {
        FrameIndexManager.writeFrameMetadata(itemId, name, url, x, y, z, blocksX, alignment, MODS_ROOT);
    }

    public static void registerFrameInstanceInIndex(String itemId, String metaFileName, int x, int y, int z, int blocksX) throws IOException {
        FrameIndexManager.registerFrameInstanceInIndex(itemId, metaFileName, x, y, z, blocksX, MODS_ROOT);
    }

    public static void removeInstancesAtCoords(int x, int y, int z) throws IOException {
        FrameIndexManager.removeInstancesAtCoords(x, y, z, MODS_ROOT);
    }

    public static BufferedImage downloadImage(String url) throws IOException {
        return ImageProcessor.downloadImage(url);
    }

    public static BufferedImage resizeImage(BufferedImage src, int targetSizeX, int targetSizeY) {
        return ImageProcessor.resizeImage(src, targetSizeX, targetSizeY);
    }

    public static Path saveImageToMods(BufferedImage img, String fileName, String sizeKey) throws IOException {
        return ImageProcessor.saveImageToMods(img, fileName, sizeKey, MODS_ROOT);
    }

    public static String addImageAsItemFromImage(BufferedImage image, String providedName, int blocksX, int blocksY, String alignment) throws IOException {
        return FrameItemGenerator.addImageAsItemFromImage(image, providedName, blocksX, blocksY, alignment, MODS_ROOT);
    }
}
