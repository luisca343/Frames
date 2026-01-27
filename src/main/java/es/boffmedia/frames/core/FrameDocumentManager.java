package es.boffmedia.frames.core;

import com.hypixel.hytale.server.core.util.BsonUtil;
import es.boffmedia.frames.Frames;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.json.JsonWriterSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * Operations for frame JSON (the "frame JSONs" that define block states).
 */
public final class FrameDocumentManager {
    private FrameDocumentManager() {}

    private static Path frameJsonPathFor(String sizeKey, Path modsRoot) {
        return modsRoot.resolve(Paths.get("Server", "Item", "Items", "Furniture", "Frames", "Boff_Frame_" + sizeKey + ".json"));
    }

    public static BsonDocument readDocument(String sizeKey, Path modsRoot) {
        return BsonUtil.readDocumentNow(frameJsonPathFor(sizeKey, modsRoot));
    }

    public static void writeDocument(BsonDocument doc, String sizeKey, Path modsRoot) throws IOException {
        Path pj = frameJsonPathFor(sizeKey, modsRoot);
        Files.createDirectories(pj.getParent());
        Files.writeString(pj, doc.toJson());
    }

    public static void updateDocument(Consumer<BsonDocument> updater, String sizeKey, Path modsRoot) throws IOException {
        BsonDocument doc = readDocument(sizeKey, modsRoot);
        updater.accept(doc);
        writeDocument(doc, sizeKey, modsRoot);
    }

    public static void ensureDefaultJsonExists(String sizeKey, Path modsRoot) throws IOException {
        Path pj = frameJsonPathFor(sizeKey, modsRoot);
        if (!Files.exists(pj)) {
            Files.createDirectories(pj.getParent());
            Files.writeString(pj, loadDefaultJsonFromResource(sizeKey));
            Frames.LOGGER.atInfo().log("Wrote default frame json to: " + pj);
        }
    }

    private static String loadDefaultJsonFromResource(String sizeKey) throws IOException {
        String specific = "/Boff_Frame_" + sizeKey + ".json";
        try (java.io.InputStream is = FrameDocumentManager.class.getResourceAsStream(specific)) {
            if (is != null) return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        try (java.io.InputStream is = FrameDocumentManager.class.getResourceAsStream("/DefaultFrame.json")) {
            if (is == null) throw new IOException("DefaultFrame.json resource not found on classpath");
            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    public static BsonDocument loadOrCreateDocument(String sizeKey, Path modsRoot) throws IOException {
        ensureDefaultJsonExists(sizeKey, modsRoot);
        return readDocument(sizeKey, modsRoot);
    }

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

    public static void prettyPrintAndSave(BsonDocument doc, String sizeKey, Path modsRoot) throws IOException {
        JsonWriterSettings settings = JsonWriterSettings.builder().indent(true).build();
        String pretty = doc.toJson(settings);
        Path pj = frameJsonPathFor(sizeKey, modsRoot);
        Files.createDirectories(pj.getParent());
        Files.writeString(pj, pretty);
    }

    public static boolean removeImageState(String sizeKey, String stateKey, Path modsRoot) throws IOException {
        BsonDocument doc = loadOrCreateDocument(sizeKey, modsRoot);
        if (doc == null) return false;

        if (!doc.containsKey("BlockType")) return false;
        BsonDocument blockType = doc.getDocument("BlockType");
        if (!blockType.containsKey("State")) return false;
        BsonDocument state = blockType.getDocument("State");
        if (!state.containsKey("Definitions")) return false;
        BsonDocument defs = state.getDocument("Definitions");
        if (!defs.containsKey(stateKey)) return false;

        String texturePath = null;
        try {
            BsonDocument def = defs.getDocument(stateKey);
            if (def.containsKey("CustomModelTexture")) {
                BsonArray arr = def.getArray("CustomModelTexture");
                if (arr.size() > 0) {
                    org.bson.BsonDocument tdoc = arr.get(0).asDocument();
                    if (tdoc.containsKey("Texture")) texturePath = tdoc.getString("Texture").getValue();
                }
            }
        } catch (Exception ignored) {}

        defs.remove(stateKey);
        prettyPrintAndSave(doc, sizeKey, modsRoot);

        if (texturePath != null && texturePath.startsWith("Blocks/Frames/")) {
            String after = texturePath.substring("Blocks/Frames/".length());
            Path p = modsRoot.resolve(Paths.get("Common", "Blocks", "Frames").resolve(after));
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
