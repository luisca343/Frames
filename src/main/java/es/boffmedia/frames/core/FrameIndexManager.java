package es.boffmedia.frames.core;

import es.boffmedia.frames.Frames;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.json.JsonWriterSettings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the global FramesIndex.json and per-item metadata files.
 */
public final class FrameIndexManager {
    private FrameIndexManager() {}

    public static void registerFrameInstanceInIndex(String itemId, String metaFileName, int x, int y, int z, int blocksX, Path modsRoot) throws IOException {
        Path indexFile = modsRoot.resolve("FramesIndex.json");
        BsonDocument indexDoc;
        if (Files.exists(indexFile)) {
            try {
                String existing = Files.readString(indexFile);
                indexDoc = BsonDocument.parse(existing);
            } catch (Exception e) {
                indexDoc = new BsonDocument();
            }
        } else {
            indexDoc = new BsonDocument();
        }

        if (!indexDoc.containsKey("items")) indexDoc.append("items", new BsonDocument());
        BsonDocument items = indexDoc.getDocument("items");

        if (!items.containsKey(itemId)) items.append(itemId, new BsonArray());
        BsonArray arr = items.getArray(itemId);

        BsonDocument entry = new BsonDocument();
        entry.append("metaFile", new BsonString(metaFileName == null ? "" : metaFileName));
        BsonDocument coords = new BsonDocument();
        coords.append("x", new org.bson.BsonInt32(x));
        coords.append("y", new org.bson.BsonInt32(y));
        coords.append("z", new org.bson.BsonInt32(z));
        entry.append("coords", coords);
        BsonDocument blocks = new BsonDocument();
        blocks.append("x", new org.bson.BsonInt32(blocksX));
        entry.append("blocks", blocks);
        entry.append("createdAt", new BsonString(java.time.Instant.now().toString()));

        arr.add(entry);

        JsonWriterSettings settings = JsonWriterSettings.builder().indent(true).build();
        Files.writeString(indexFile, indexDoc.toJson(settings));
    }

    public static void removeInstancesAtCoords(int x, int y, int z, Path modsRoot) throws IOException {
        Path indexFile = modsRoot.resolve("FramesIndex.json");
        if (!Files.exists(indexFile)) return;

        String existing = Files.readString(indexFile);
        BsonDocument indexDoc = BsonDocument.parse(existing);
        if (!indexDoc.containsKey("items")) return;

        BsonDocument items = indexDoc.getDocument("items");
        List<String> toUpdateMetaFiles = new ArrayList<>();

        for (String itemId : items.keySet()) {
            BsonArray arr = items.getArray(itemId);
            BsonArray newArr = new BsonArray();
            for (int i = 0; i < arr.size(); i++) {
                try {
                    org.bson.BsonDocument inst = arr.get(i).asDocument();
                    boolean match = false;
                    if (inst.containsKey("coords")) {
                        org.bson.BsonDocument c = inst.getDocument("coords");
                        int mx = c.getInt32("x").getValue();
                        int my = c.getInt32("y").getValue();
                        int mz = c.getInt32("z").getValue();
                        if (mx == x && my == y && mz == z) match = true;
                    }
                    if (match) {
                        try { toUpdateMetaFiles.add(inst.getString("metaFile").getValue()); } catch (Exception ignore) {}
                    } else {
                        newArr.add(arr.get(i));
                    }
                } catch (Exception e) {
                    newArr.add(arr.get(i));
                }
            }

            if (newArr.size() == 0) {
                items.remove(itemId);
            } else {
                items.append(itemId, newArr);
            }
        }

        JsonWriterSettings settings = JsonWriterSettings.builder().indent(true).build();
        Files.writeString(indexFile, indexDoc.toJson(settings));

        for (String metaFileName : toUpdateMetaFiles) {
            try {
                Path metaPath = modsRoot.resolve("Frames").resolve(metaFileName);
                if (!Files.exists(metaPath) || !Files.isRegularFile(metaPath)) continue;
                String metaTxt = Files.readString(metaPath);
                BsonDocument metaDoc = BsonDocument.parse(metaTxt);
                if (!metaDoc.containsKey("frames")) continue;
                BsonArray framesArr = metaDoc.getArray("frames");
                BsonArray newFrames = new BsonArray();
                for (int i = 0; i < framesArr.size(); i++) {
                    try {
                        org.bson.BsonDocument fe = framesArr.get(i).asDocument();
                        boolean match = false;
                        if (fe.containsKey("coords")) {
                            org.bson.BsonDocument c = fe.getDocument("coords");
                            int mx = c.getInt32("x").getValue();
                            int my = c.getInt32("y").getValue();
                            int mz = c.getInt32("z").getValue();
                            if (mx == x && my == y && mz == z) match = true;
                        }
                        if (!match) newFrames.add(fe);
                    } catch (Exception e) {
                        newFrames.add(framesArr.get(i));
                    }
                }
                metaDoc.append("frames", newFrames);
                JsonWriterSettings ws = JsonWriterSettings.builder().indent(true).build();
                Files.writeString(metaPath, metaDoc.toJson(ws));
            } catch (Exception e) {
                Frames.LOGGER.atWarning().withCause(e).log("Failed cleaning meta file " + metaFileName + ": " + e.getMessage());
            }
        }
    }

    public static void writeFrameMetadata(String itemId, String name, String url, int x, int y, int z, int blocksX, String alignment, Path modsRoot) throws IOException {
        Path metaDir = modsRoot.resolve("Frames");
        Files.createDirectories(metaDir);
        Path metaFile = metaDir.resolve(itemId + ".json");

        try { removeInstancesAtCoords(x, y, z, modsRoot); } catch (Exception e) { Frames.LOGGER.atWarning().withCause(e).log("Failed to remove preexisting instances at coords: " + e.getMessage()); }

        BsonDocument doc;
        if (Files.exists(metaFile)) {
            try {
                String existing = Files.readString(metaFile);
                doc = BsonDocument.parse(existing);
            } catch (Exception e) {
                doc = new BsonDocument();
            }
        } else {
            doc = new BsonDocument();
        }

        if (!doc.containsKey("itemId")) doc.append("itemId", new BsonString(itemId == null ? "" : itemId));
        if (name != null && !name.isEmpty()) doc.append("name", new BsonString(name)); else if (!doc.containsKey("name")) doc.append("name", new BsonString(""));
        if (url != null && !url.isEmpty()) doc.append("url", new BsonString(url)); else if (!doc.containsKey("url")) doc.append("url", new BsonString(""));
        if (alignment != null && !alignment.isEmpty()) doc.append("alignment", new BsonString(alignment));
        if (!doc.containsKey("createdAt")) doc.append("createdAt", new BsonString(java.time.Instant.now().toString()));

        BsonDocument frameEntry = new BsonDocument();
        BsonDocument coords = new BsonDocument();
        coords.append("x", new org.bson.BsonInt32(x));
        coords.append("y", new org.bson.BsonInt32(y));
        coords.append("z", new org.bson.BsonInt32(z));
        frameEntry.append("coords", coords);

        BsonDocument blocks = new BsonDocument();
        blocks.append("x", new org.bson.BsonInt32(blocksX));
        frameEntry.append("blocks", blocks);

        frameEntry.append("createdAt", new BsonString(java.time.Instant.now().toString()));

        if (!doc.containsKey("frames")) doc.append("frames", new BsonArray());
        BsonArray arr = doc.getArray("frames");
        arr.add(frameEntry);

        JsonWriterSettings settings = JsonWriterSettings.builder().indent(true).build();
        Files.writeString(metaFile, doc.toJson(settings));

        try {
            registerFrameInstanceInIndex(itemId, metaFile.getFileName().toString(), x, y, z, blocksX, modsRoot);
        } catch (Exception e) {
            Frames.LOGGER.atWarning().withCause(e).log("Failed to update frames index: " + e.getMessage());
        }
    }
}
