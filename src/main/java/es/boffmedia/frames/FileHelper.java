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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;


public class FileHelper {
        public static final Path MODS_ROOT = Paths.get("mods", "BoffmediaFrames");
        public static final Path FRAME_JSON = MODS_ROOT.resolve(Paths.get("Server", "Item", "Items", "Furniture", "Paintings", "Boff_Frame_1x1.json"));
        public static final Path FRAME_TEXTURE = MODS_ROOT.resolve(Paths.get("Common", "Blocks", "Frames", "FRAME_TEST.png"));

        // Default JSON is stored in resources/DefaultFrame.json

    private static String loadDefaultJsonFromResource() throws IOException {
        try (InputStream is = FileHelper.class.getResourceAsStream("DefaultFrame.json")) {
            if (is == null) throw new IOException("Boff_Frame_1x1.json resource not found on classpath");
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
    public static void loadFiles() {
        Frames.LOGGER.atInfo().log("Mods folder exists: " + Files.exists(MODS_ROOT));

        try {
            ensureDefaultJsonExists();
            BsonDocument doc = readDocument();
            Frames.LOGGER.atInfo().log("Document loaded: " + doc);
        } catch (Exception e) {
            Frames.LOGGER.atSevere().withCause(e).log("Failed to ensure or load frame json: " + e.getMessage());
        }
    }

    public static void ensureDefaultJsonExists() throws IOException {
        if (!Files.exists(FRAME_JSON)) {
            Files.createDirectories(FRAME_JSON.getParent());
            Files.writeString(FRAME_JSON, loadDefaultJsonFromResource());
            Frames.LOGGER.atInfo().log("Wrote default frame json to: " + FRAME_JSON);
        }
    }

    public static BsonDocument readDocument() {
        return BsonUtil.readDocumentNow(FRAME_JSON);
    }

    public static void writeDocument(BsonDocument doc) throws IOException {
        Files.createDirectories(FRAME_JSON.getParent());
        Files.writeString(FRAME_JSON, doc.toJson());
    }

    public static void updateDocument(Consumer<BsonDocument> updater) throws IOException {
        BsonDocument doc = readDocument();
        updater.accept(doc);
        writeDocument(doc);
    }

    private static final SecureRandom RNG = new SecureRandom();
    private static final String NAME_ALPHANUM = "abcdefghijklmnopqrstuvwxyz0123456789";

    private static String generateRandomName(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
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
    public static BufferedImage resizeImage(BufferedImage src, int targetSize) {
        BufferedImage scaled = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.drawImage(src, 0, 0, targetSize, targetSize, null);
        g.dispose();
        return scaled;
    }

    /* Helper: save PNG into mods folder and return the path */
    public static Path saveImageToMods(BufferedImage img, String fileName) throws IOException {
        Path out = MODS_ROOT.resolve(Paths.get("Common", "Blocks", "Frames", fileName));
        Files.createDirectories(out.getParent());
        boolean written = ImageIO.write(img, "png", out.toFile());
        if (!written) throw new IOException("ImageIO.write returned false for: " + out.toString());
        return out;
    }

    /* Helper: ensure default JSON exists and return the BsonDocument */
    public static BsonDocument loadOrCreateDocument() throws IOException {
        ensureDefaultJsonExists();
        return readDocument();
    }

    /* Helper: add a state definition into the provided document (in-memory) */
    public static void addStateToDocument(BsonDocument doc, String key, String texturePath) {
        BsonDocument stateDef = new BsonDocument();
        stateDef.append("InteractionHint", new BsonString("boffmedia.frames.empty"));
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
    public static void prettyPrintAndSave(BsonDocument doc) throws IOException {
        JsonWriterSettings settings = JsonWriterSettings.builder().indent(true).build();
        String pretty = doc.toJson(settings);
        Files.createDirectories(FRAME_JSON.getParent());
        Files.writeString(FRAME_JSON, pretty);
    }

    /**
     * Downloads an image from the URL, scales it to 32x32, saves it under
     * mods/BoffmediaFrames/Common/Blocks/Frames/FRAME_<rand>.png and inserts a new
     * State.Definitions entry into the frame JSON using the same random key.
     * Returns the generated state key.
     */
    public static String addImageStateFromUrl(String urlStr) throws IOException {
        // Download
        BufferedImage image = downloadImage(urlStr);

        // Resize
        BufferedImage scaled = resizeImage(image, 32);

        // Generate name and save image
        String name = generateRandomName(8);
        String fileName = "FRAME_" + name + ".png";
        Path out = saveImageToMods(scaled, fileName);
        String texturePath = "Blocks/Frames/" + fileName;

        // Load or create doc, add state, pretty-save
        BsonDocument doc = loadOrCreateDocument();
        addStateToDocument(doc, name, texturePath);
        prettyPrintAndSave(doc);

        Frames.LOGGER.atInfo().log("Added state " + name + " with texture " + texturePath + " saved to " + out.toString());
        return name;
    }
}
