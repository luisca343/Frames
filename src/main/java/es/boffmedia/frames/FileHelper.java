package es.boffmedia.frames;

import com.hypixel.hytale.server.core.util.BsonUtil;
import org.bson.BsonDocument;
import org.bson.BsonArray;
import org.bson.BsonString;
import org.bson.json.JsonWriterSettings;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.net.URL;
import java.net.URISyntaxException;
import java.net.JarURLConnection;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;


public class FileHelper {
        public static final Path MODS_ROOT = Paths.get("mods", "BoffmediaFrames");

        // Define available frame sizes (width x height in frames)
        public static final String[] FRAME_SIZES = new String[]{"1x1", "1x2", "1x3", "2x1", "2x2", "2x3", "3x1", "3x2", "3x3"};

        private static Path frameJsonPathFor(String sizeKey) {
            return MODS_ROOT.resolve(Paths.get("Server", "Item", "Items", "Furniture", "Frames", "Boff_Frame_" + sizeKey + ".json"));
        }

        private static Path textureDirFor(String sizeKey) {
                return MODS_ROOT.resolve(Paths.get("Common", "Blocks", "Frames", "Images"));
        }

        private static Path texturePathFor(String sizeKey, String fileName) {
                return textureDirFor(sizeKey).resolve(fileName);
        }

        // Default JSON is stored in resources/Default Boff_Frame.json

    private static String loadDefaultJsonFromResource(String sizeKey) throws IOException {
        // Prefer size-specific default (e.g. Boff_Frame_1x1.json), fallback to Default Boff_Frame.json
        String specific = "/Boff_Frame_" + sizeKey + ".json";
        try (InputStream is = FileHelper.class.getResourceAsStream(specific)) {
            if (is != null) return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        try (InputStream is = FileHelper.class.getResourceAsStream("/DefaultFrame.json")) {
            if (is == null) throw new IOException("DefaultFrame.json resource not found on classpath");
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    public static void loadFiles() {
        Frames.LOGGER.atInfo().log("Mods folder exists: " + Files.exists(MODS_ROOT));

        try {
            // Copy embedded resource folders into the mods folder first
            copyResourceDirectory("/Common", MODS_ROOT.resolve("Common"));
            copyResourceDirectory("/Server", MODS_ROOT.resolve("Server"));

            // Ensure defaults exist for all declared sizes
            for (String sk : FRAME_SIZES) {
                ensureDefaultJsonExists(sk);
                BsonDocument doc = readDocument(sk);
                Frames.LOGGER.atInfo().log("Document loaded for " + sk + ": " + (doc != null));
            }
            // Ensure top-level manifest exists in mods root (copy from resource manifest_generated.json)
            ensureManifestExists();
        } catch (Exception e) {
            Frames.LOGGER.atSevere().withCause(e).log("Failed to ensure or load frame json: " + e.getMessage());
        }
    }

    /**
     * Ensure that mods/BoffmediaFrames/manifest.json exists. If not, copy
     * the embedded resource /manifest_generated.json into that location.
     */
    public static void ensureManifestExists() {
        try {
            Path manifestOut = MODS_ROOT.resolve("manifest.json");
            if (Files.exists(manifestOut)) return;

            try (InputStream is = FileHelper.class.getResourceAsStream("/manifest_generated.json")) {
                if (is == null) {
                    Frames.LOGGER.atWarning().log("Resource manifest_generated.json not found on classpath; skipping manifest copy");
                    return;
                }
                Files.createDirectories(manifestOut.getParent());
                Files.copy(is, manifestOut);
                Frames.LOGGER.atInfo().log("Copied manifest_generated.json -> " + manifestOut);
            }
        } catch (Exception e) {
            Frames.LOGGER.atWarning().withCause(e).log("Failed to ensure manifest.json: " + e.getMessage());
        }
    }

    /**
     * Copy a resource directory (from classpath) into the given output directory.
     * Handles both running from the filesystem (IDE) and from within a jar.
     */
    public static void copyResourceDirectory(String resourcePath, Path outDir) throws IOException {
        String rp = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        URL url = FileHelper.class.getResource("/" + rp);
        if (url == null) {
            Frames.LOGGER.atWarning().log("Resource " + resourcePath + " not found on classpath; skipping copy");
            return;
        }

        String protocol = url.getProtocol();
        if ("file".equals(protocol)) {
            try {
                Path res = Paths.get(url.toURI());
                Files.walk(res).forEach(p -> {
                    if (Files.isRegularFile(p)) {
                        Path rel = res.relativize(p);
                        Path target = outDir.resolve(rel.toString());
                        try {
                            Files.createDirectories(target.getParent());
                            Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        } else if ("jar".equals(protocol)) {
            JarURLConnection conn = (JarURLConnection) url.openConnection();
            JarFile jar = conn.getJarFile();
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(rp + "/")) continue;
                if (entry.isDirectory()) continue;
                String relName = name.substring(rp.length() + 1);
                Path out = outDir.resolve(relName);
                Files.createDirectories(out.getParent());
                try (InputStream is = jar.getInputStream(entry)) {
                    Files.copy(is, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } else {
            // Fallback: attempt to treat resourcePath as a single resource
            try (InputStream is = FileHelper.class.getResourceAsStream("/" + rp)) {
                if (is == null) {
                    Frames.LOGGER.atWarning().log("Unknown resource protocol for " + resourcePath + ": " + protocol);
                    return;
                }
                Files.createDirectories(outDir.getParent());
                Files.copy(is, outDir, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        Frames.LOGGER.atInfo().log("Copied resource folder " + resourcePath + " -> " + outDir);
    }

    public static void ensureDefaultJsonExists(String sizeKey) throws IOException {
        Path pj = frameJsonPathFor(sizeKey);
        if (!Files.exists(pj)) {
            Files.createDirectories(pj.getParent());
            Files.writeString(pj, loadDefaultJsonFromResource(sizeKey));
            Frames.LOGGER.atInfo().log("Wrote default frame json to: " + pj);
        }
    }

    public static BsonDocument readDocument(String sizeKey) {
        return BsonUtil.readDocumentNow(frameJsonPathFor(sizeKey));
    }

    public static void writeDocument(BsonDocument doc, String sizeKey) throws IOException {
        Path pj = frameJsonPathFor(sizeKey);
        Files.createDirectories(pj.getParent());
        Files.writeString(pj, doc.toJson());
    }

    public static void updateDocument(Consumer<BsonDocument> updater, String sizeKey) throws IOException {
        BsonDocument doc = readDocument(sizeKey);
        updater.accept(doc);
        writeDocument(doc, sizeKey);
    }

    private static final SecureRandom RNG = new SecureRandom();
    private static final String NAME_ALPHANUM = "abcdefghijklmnopqrstuvwxyz0123456789";
    // Need first to be a uppercase letter to comply witth Hytale's asset naming rules
    private static final String NAME_FIRST_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static String generateRandomName(int length) {
        if (length <= 0) return "";
        StringBuilder sb = new StringBuilder(length);
        // First character must be an uppercase letter
        sb.append(NAME_FIRST_LETTERS.charAt(RNG.nextInt(NAME_FIRST_LETTERS.length())));
        for (int i = 1; i < length; i++) {
            sb.append(NAME_ALPHANUM.charAt(RNG.nextInt(NAME_ALPHANUM.length())));
        }
        return sb.toString();
    }

    /* Helper: download an image from a URL and return a BufferedImage */
    public static BufferedImage downloadImage(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        Frames.LOGGER.atInfo().log("Downloading image from: " + urlStr);
        BufferedImage image = ImageIO.read(url);
        if (image == null) throw new IOException("Failed to decode image from URL: " + urlStr);
        return image;
    }

    /* Helper: resize a BufferedImage to a square of given size (nearest-neighbor) */
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

    /* Helper: save PNG into mods folder and return the path */
    public static Path saveImageToMods(BufferedImage img, String fileName, String sizeKey) throws IOException {
        Path out = texturePathFor(sizeKey, fileName);
        Files.createDirectories(out.getParent());
        boolean written = ImageIO.write(img, "png", out.toFile());
        if (!written) throw new IOException("ImageIO.write returned false for: " + out.toString());
        return out;
    }

    /* Helper: ensure default JSON exists and return the BsonDocument */
    public static BsonDocument loadOrCreateDocument(String sizeKey) throws IOException {
        ensureDefaultJsonExists(sizeKey);
        return readDocument(sizeKey);
    }

    /* Helper: add a state definition into the provided document (in-memory) */
    public static void addStateToDocument(BsonDocument doc, String key, String texturePath) {
        BsonDocument stateDef = new BsonDocument();
        stateDef.append("InteractionHint", new BsonString("frames.use_hint"));
        BsonArray texArr = new BsonArray();
        texArr.add(new BsonDocument().append("Texture", new BsonString(texturePath)));
        stateDef.append("CustomModelTexture", texArr);

        if (!doc.containsKey("BlockType")) doc.append("BlockType", new BsonDocument());
        BsonDocument blockType = doc.getDocument("BlockType");

        if (!blockType.containsKey("State")) blockType.append("State", new BsonDocument());
        BsonDocument state = blockType.getDocument("State");

        if (!state.containsKey("Definitions")) state.append("Definitions", new BsonDocument());
        BsonDocument defs = state.getDocument("Definitions");

        defs.append(key, stateDef);
    }

    /* Helper: pretty-print JSON and save to mods FRAME_JSON */
    public static void prettyPrintAndSave(BsonDocument doc, String sizeKey) throws IOException {
        JsonWriterSettings settings = JsonWriterSettings.builder().indent(true).build();
        String pretty = doc.toJson(settings);
        Path pj = frameJsonPathFor(sizeKey);
        Files.createDirectories(pj.getParent());
        Files.writeString(pj, pretty);
    }
    
    /**
     * Save the provided image without scaling and create a blockymodel + item JSON
     * matching the image's exact pixel dimensions. Returns the generated item id.
     */
    public static String addImageAsItemFromImage(BufferedImage image, String providedName, int blocksX, int blocksY) throws IOException {
        if (image == null) throw new IOException("Provided image is null");

        int imgPixelsX = image.getWidth();
        int imgPixelsY = image.getHeight();

        // Model size in pixels = image pixel dimensions (no cropping)
        int sizeX = Math.max(1, imgPixelsX);
        int sizeY = Math.max(1, imgPixelsY);

        // Normalize base name same as previous logic
        String baseName = null;
        if (providedName != null) {
            String n = providedName.trim();
            if (!n.isEmpty()) {
                n = n.replaceAll("\\s+", "_");
                if (n.length() == 1) n = n.toUpperCase();
                else n = n.substring(0, 1).toUpperCase() + n.substring(1).toLowerCase();
                baseName = n;
            }
        }
        if (baseName == null || baseName.isEmpty()) baseName = generateRandomName(8);

        // sizeKey based on 32px steps (folder grouping)
        int w = Math.max(1, sizeX / 32);
        int h = Math.max(1, sizeY / 32);
        String sizeKey = w + "x" + h;

        String fileName = baseName + ".png";
        // Save image as-is
        Path out = saveImageToMods(image, fileName, sizeKey);
        String texturePath = "Blocks/Frames/Images/" + fileName;

        // Create blockymodel matching exact pixel size
        Path modelOut = MODS_ROOT.resolve(Paths.get("Common", "Blocks", "Frames", baseName + ".blockymodel"));
        Files.createDirectories(modelOut.getParent());
        // We adjust the position to be exactly in the wall
        float zOffset = ((float) sizeX) / (-blocksX * 2);

        // Compute render scale so the model (which is sized to image pixels) is displayed
        // at the requested block dimensions. Use horizontal size to compute scale
        // and preserve aspect ratio by applying the same scale on both axes.
        float scaleFactor = ((float) Math.max(1, blocksX) * 32.0f) / (float) imgPixelsX;

        // Derive the vertical block count from the horizontal blocks and image aspect ratio
        int computedBlocksY = Math.max(1, Math.round((float) blocksX * (float) imgPixelsY / (float) imgPixelsX));

        // Calculate a Y position offset for the model similar to the Z offset calculation.
        float yOffset = ((float) sizeY) / ((float) computedBlocksY * 2.0f);

        String modelJson = AssetJsonBuilder.buildBlockymodel(baseName, sizeX, sizeY, (int) yOffset, (int) zOffset);
        Files.writeString(modelOut, modelJson);

        // Create minimal item JSON (no recipe). Ensure it drops 1x1 on break via a drop hint field.
        Path itemOut = MODS_ROOT.resolve(Paths.get("Server", "Item", "Items", "Furniture", "Frames", "Boff_Frame_" + baseName + ".json"));
        Files.createDirectories(itemOut.getParent());
        String itemJson = AssetJsonBuilder.buildItemJson(baseName, texturePath, scaleFactor);
        Files.writeString(itemOut, itemJson);

        String itemId = "Boff_Frame_" + baseName;
        Frames.LOGGER.atInfo().log("Created dynamic item " + itemId + " model=" + modelOut + " image=" + out + " json=" + itemOut);
        return itemId;
    }

    // Legacy compatibility helpers removed.

    /**
     * Remove an image state from the frame JSON and delete the texture file on disk if present.
     * @param sizeKey size key like "1x1", "2x3"
     * @param stateKey the state key to remove
     * @return true if the state existed and was removed (file deletion may still fail silently)
     */
    public static boolean removeImageState(String sizeKey, String stateKey) throws IOException {
        BsonDocument doc = loadOrCreateDocument(sizeKey);
        if (doc == null) return false;

        if (!doc.containsKey("BlockType")) return false;
        BsonDocument blockType = doc.getDocument("BlockType");
        if (!blockType.containsKey("State")) return false;
        BsonDocument state = blockType.getDocument("State");
        if (!state.containsKey("Definitions")) return false;
        BsonDocument defs = state.getDocument("Definitions");
        if (!defs.containsKey(stateKey)) return false;

        // Try to locate texture path from the definition (first CustomModelTexture entry)
        String texturePath = null;
        try {
            BsonDocument def = defs.getDocument(stateKey);
            if (def.containsKey("CustomModelTexture")) {
                BsonArray arr = def.getArray("CustomModelTexture");
                if (arr.size() > 0) {
                    BsonDocument tdoc = arr.get(0).asDocument();
                    if (tdoc.containsKey("Texture")) texturePath = tdoc.getString("Texture").getValue();
                }
            }
        } catch (Exception ignored) {}

        // Remove definition and save JSON
        defs.remove(stateKey);
        prettyPrintAndSave(doc, sizeKey);

        // Delete texture file if it was under Blocks/Frames/<sizeKey>/...
        if (texturePath != null && texturePath.startsWith("Blocks/Frames/")) {
            String after = texturePath.substring("Blocks/Frames/".length());
            Path p = MODS_ROOT.resolve(Paths.get("Common", "Blocks", "Frames").resolve(after));
            try {
                Files.deleteIfExists(p);
                Frames.LOGGER.atInfo().log("Deleted texture file: " + p);
            } catch (IOException e) {
                Frames.LOGGER.atWarning().withCause(e).log("Failed to delete texture file: " + p + " -> " + e.getMessage());
            }
        }

        return true;
    }
}
