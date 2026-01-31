package es.boffmedia.frames.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.hypixel.hytale.protocol.BlockPosition;
import java.io.IOException;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import es.boffmedia.frames.FileHelper;
import es.boffmedia.frames.Frames;
// Permissions check removed; states list deprecated
import es.boffmedia.frames.interactions.UseFrameInteraction;

import javax.annotation.Nonnull;

public class ImageDownloadPage extends InteractiveCustomUIPage<ImageDownloadPage.ImageDownloadData> {

    public static class ImageDownloadData {
        public String action;
        public String url;
        public String name;
        public String sizeXBlocks;
        public String stateKey;

        public static final BuilderCodec<ImageDownloadData> CODEC = ((BuilderCodec.Builder<ImageDownloadData>) ((BuilderCodec.Builder<ImageDownloadData>)
                BuilderCodec.builder(ImageDownloadData.class, ImageDownloadData::new))
                .append(new KeyedCodec<>("Action", Codec.STRING), (ImageDownloadData o, String v) -> o.action = v, (ImageDownloadData o) -> o.action)
                .add()
                .append(new KeyedCodec<>("@UrlInput", Codec.STRING), (ImageDownloadData o, String v) -> o.url = v, (ImageDownloadData o) -> o.url)
                .add()
                .append(new KeyedCodec<>("@NameInput", Codec.STRING), (ImageDownloadData o, String v) -> o.name = v, (ImageDownloadData o) -> o.name)
                .add()
                .append(new KeyedCodec<>("@SizeXInput", Codec.STRING), (ImageDownloadData o, String v) -> o.sizeXBlocks = v, (ImageDownloadData o) -> o.sizeXBlocks)
                .add()
                .append(new KeyedCodec<>("@StateKey", Codec.STRING), (ImageDownloadData o, String v) -> o.stateKey = v, (ImageDownloadData o) -> o.stateKey)
                .add())
                .build();
    }

    private final PlayerRef playerRef;
    private final World targetWorld;
    private final BlockPosition targetBlock;
    private final String initialStateKey;

    public ImageDownloadPage(@Nonnull PlayerRef playerRef, @Nonnull World world, @Nonnull BlockPosition targetBlock) {
        this(playerRef, world, targetBlock, null);
    }

    public ImageDownloadPage(@Nonnull PlayerRef playerRef, @Nonnull World world, @Nonnull BlockPosition targetBlock, String initialStateKey) {
        super(playerRef, CustomPageLifetime.CanDismiss, ImageDownloadData.CODEC);
        this.playerRef = playerRef;
        this.targetWorld = world;
        this.targetBlock = targetBlock;
        this.initialStateKey = initialStateKey;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/ImageDownloadPage.ui");

        // Bind the Upload button; send the Url input's value using the documented @<ControlId> mapping
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#UploadButton",
            new EventData()
                .append("Action", "Upload")
                .append("@UrlInput", "#UrlInput.Value")
                .append("@NameInput", "#NameInput.Value")
                .append("@SizeXInput", "#SizeXInput.Value"),
                false);

        // Bind the Apply button: send the selected StateKey (or item id) without downloading
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ApplyButton",
            new EventData()
                .append("Action", "Apply")
                .append("@StateKey", "#StateKeyInput.Value"),
                false);

        // Bind Choose button: open a page to pick an existing image
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ChooseButton",
            new EventData()
                .append("Action", "Choose"),
                false);

        // Bind Remove button: replace with 1x1 frame and remove metadata for this coord
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#RemoveButton",
            new EventData()
                .append("Action", "RemoveReplace1x1"),
                false);

        // Prefill inputs from the global frames index by coords for quick lookup
        try {
            Path indexFile = FileHelper.MODS_ROOT.resolve("FramesIndex.json");
            if (Files.exists(indexFile) && Files.isRegularFile(indexFile)) {
                try {
                    String idxTxt = Files.readString(indexFile);
                    org.bson.BsonDocument idxDoc = org.bson.BsonDocument.parse(idxTxt);
                    if (idxDoc.containsKey("items")) {
                        org.bson.BsonDocument items = idxDoc.getDocument("items");
                        outer: for (String itemId : items.keySet()) {
                            try {
                                org.bson.BsonArray arr = items.getArray(itemId);
                                for (int ai = 0; ai < arr.size(); ai++) {
                                    org.bson.BsonDocument inst = arr.get(ai).asDocument();
                                    if (!inst.containsKey("coords")) continue;
                                    org.bson.BsonDocument c = inst.getDocument("coords");
                                    int mx = c.getInt32("x").getValue();
                                    int my = c.getInt32("y").getValue();
                                    int mz = c.getInt32("z").getValue();
                                    if (mx == this.targetBlock.x && my == this.targetBlock.y && mz == this.targetBlock.z) {
                                        String metaFile = inst.containsKey("metaFile") ? inst.getString("metaFile").getValue() : (itemId + ".json");
                                        Path metaPath = FileHelper.MODS_ROOT.resolve("Frames").resolve(metaFile);
                                        if (Files.exists(metaPath) && Files.isRegularFile(metaPath)) {
                                            try {
                                                String metaTxt = Files.readString(metaPath);
                                                org.bson.BsonDocument meta = org.bson.BsonDocument.parse(metaTxt);
                                                String mname = meta.containsKey("name") ? meta.getString("name").getValue() : "";
                                                String murl = meta.containsKey("url") ? meta.getString("url").getValue() : "";
                                                int mbx = 1;
                                                if (inst.containsKey("blocks")) {
                                                    org.bson.BsonDocument b = inst.getDocument("blocks");
                                                    if (b.containsKey("x")) mbx = b.getInt32("x").getValue();
                                                }
                                                try { uiCommandBuilder.set("#NameInput.Value", mname); } catch (Exception ignore) {}
                                                try { uiCommandBuilder.set("#UrlInput.Value", murl); } catch (Exception ignore) {}
                                                try { uiCommandBuilder.set("#SizeXInput.Value", Integer.toString(mbx)); } catch (Exception ignore) {}
                                                try { uiCommandBuilder.set("#StateKeyInput.Value", itemId); } catch (Exception ignore) {}
                                            } catch (Exception e) {
                                                Frames.LOGGER.atWarning().withCause(e).log("Failed to read referenced metadata: " + metaPath);
                                            }
                                        }
                                        break outer;
                                    }
                                }
                            } catch (Exception ignoreItem) {
                                // ignore per-item errors
                            }
                        }
                    }
                } catch (Exception e) {
                    Frames.LOGGER.atWarning().withCause(e).log("Failed to read frames index: " + e.getMessage());
                }
            }
        } catch (Exception ignored) {}

        // If we were opened with a preselected state key, set the input value
        try {
            if (this.initialStateKey != null && !this.initialStateKey.isEmpty()) {
                uiCommandBuilder.set("#StateKeyInput.Value", this.initialStateKey);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull ImageDownloadData data) {
        Player player = store.getComponent(ref, Player.getComponentType());

        if (data == null || data.action == null) return;

        if ("Choose".equals(data.action)) {
            // Build entries list from metadata files and open ListUserImagesPage
            try {
                Path metaDir = FileHelper.MODS_ROOT.resolve("Frames");
                if (!Files.exists(metaDir) || !Files.isDirectory(metaDir)) {
                    player.sendMessage(com.hypixel.hytale.server.core.Message.raw("No metadata files found."));
                    return;
                }

                java.util.List<String> entries = new java.util.ArrayList<>();
                try (java.util.stream.Stream<Path> stream = Files.list(metaDir)) {
                    stream.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json"))
                          .forEach(p -> {
                              try {
                                  String txt = Files.readString(p);
                                  org.bson.BsonDocument meta = org.bson.BsonDocument.parse(txt);
                                  String id = meta.containsKey("itemId") ? meta.getString("itemId").getValue() : p.getFileName().toString().replaceFirst("\\.json$", "");
                                  String name = meta.containsKey("name") ? meta.getString("name").getValue() : "";
                                  String coords = "";
                                  if (meta.containsKey("coords")) {
                                      org.bson.BsonDocument c = meta.getDocument("coords");
                                      coords = c.getInt32("x").getValue() + "," + c.getInt32("y").getValue() + "," + c.getInt32("z").getValue();
                                  }
                                  entries.add(id + " - " + name + (coords.isEmpty() ? "" : " @ " + coords));
                              } catch (Exception e) {
                                  entries.add("ERROR: " + p.getFileName().toString() + " - " + e.getMessage());
                              }
                          });
                }

                String[] arr = entries.toArray(new String[0]);
                ListUserImagesPage page = new ListUserImagesPage(player.getPlayerRef(), this.targetWorld, this.targetBlock, arr);
                player.getPageManager().openCustomPage(player.getReference(), player.getReference().getStore(), page);
            } catch (Exception e) {
                player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Error opening image chooser: " + e.getMessage()));
            }

            return;
        }

        if ("Upload".equals(data.action)) {
            Frames.LOGGER.atInfo().log("Received Upload action from player " + player.getDisplayName());

            String url = data.url;
                if (url == null || url.trim().isEmpty()) {
                player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Please enter a valid URL in the field."));
                return;
            }

               player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Downloading image..."));
            try {
                int sizeX = 32;
                int sizeY = 32;
                try {
                    long chunkIndex = ChunkUtil.indexChunkFromBlock(this.targetBlock.x, this.targetBlock.z);
                    WorldChunk chunk = this.targetWorld.getChunkIfInMemory(chunkIndex);
                    if (chunk != null) {
                        BlockType current = chunk.getBlockType(this.targetBlock.x, this.targetBlock.y, this.targetBlock.z);
                        if (current != null) {
                            String id = current.getId();
                            if (id != null) {
                                Matcher m = Pattern.compile("(\\d+)x(\\d+)").matcher(id);
                                if (m.find()) {
                                    int w = Integer.parseInt(m.group(1));
                                    int h = Integer.parseInt(m.group(2));
                                    sizeX = 32 * Math.max(1, w);
                                    sizeY = 32 * Math.max(1, h);
                                    Frames.LOGGER.atInfo().log("Determined frame size from block id '" + id + "': " + sizeX + "x" + sizeY);
                                }
                            }
                        }
                    } else {
                        Frames.LOGGER.atInfo().log("Chunk not in memory for block; using default size 32x32");
                    }
                } catch (Exception e) {
                    Frames.LOGGER.atWarning().withCause(e).log("Failed to determine block size, defaulting to 32x32");
                }

                // Download image (no scaling) and create a dedicated item + blockymodel for it
                try {
                    java.awt.image.BufferedImage img = FileHelper.downloadImage(url);

                    int blocksX = 1;
                    try {
                        if (data.sizeXBlocks != null && !data.sizeXBlocks.trim().isEmpty()) blocksX = Math.max(1, Integer.parseInt(data.sizeXBlocks.trim()));
                    } catch (Exception ignored) {}

                    // Maintain aspect ratio: blocksY is derived from image dimensions when saving; pass blocksX only
                    String itemId = FileHelper.addImageAsItemFromImage(img, data.name, blocksX, blocksX);

                    // Write metadata JSON for the created frame into mods/BoffmediaFrames/Frames/<itemId>.json
                    try {
                        FileHelper.writeFrameMetadata(itemId, data.name, data.url, this.targetBlock.x, this.targetBlock.y, this.targetBlock.z, blocksX);
                    } catch (IOException ioe) {
                        Frames.LOGGER.atWarning().withCause(ioe).log("Failed to write frame metadata: " + ioe.getMessage());
                    }

                    // Close UI and delay applying the new block so assets have time to sync
                    this.close();

                    new Thread(() -> {
                        try {
                            Thread.sleep(10_000);
                            Frames.LOGGER.atInfo().log("Attempting to replace block with item '" + itemId + "' after delay");
                            boolean replaced = UseFrameInteraction.replaceBlockWithItem(this.targetWorld, this.targetBlock, itemId);
                            if (replaced) {
                                player.sendMessage(com.hypixel.hytale.server.core.Message.raw("The frame has been updated to the new item: " + itemId));
                            } else {
                                player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Could not replace the block. Ensure the chunk is loaded and assets have been reloaded."));
                            }
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            Frames.LOGGER.atWarning().withCause(ie).log("Delayed replacement interrupted");
                        }
                    }).start();
                } catch (IOException ioe) {
                    player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Error downloading or saving the image: " + ioe.getMessage()));
                }
            } catch (Exception e) {
                player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Error downloading or processing the image: " + e.getMessage()));
            }
        }
        else if ("RemoveReplace1x1".equals(data.action)) {
            try {
                boolean replaced = UseFrameInteraction.replaceBlockWithItem(this.targetWorld, this.targetBlock, "Boff_Frame_1x1");
                if (replaced) {
                    try {
                        FileHelper.removeInstancesAtCoords(this.targetBlock.x, this.targetBlock.y, this.targetBlock.z);
                    } catch (Exception e) {
                        Frames.LOGGER.atWarning().withCause(e).log("Failed to remove frame instances at coords: " + e.getMessage());
                    }
                    player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Frame removed and replaced with Boff_Frame_1x1."));
                } else {
                    player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Could not replace the frame with Boff_Frame_1x1."));
                }
            } catch (Exception e) {
                player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Error removing the frame: " + e.getMessage()));
            }
            this.close();
        }
        else if ("Delete".equals(data.action)) {
            String stateKey = data.stateKey;
            if (stateKey == null || stateKey.isEmpty()) return;

            // Determine sizeKey for the target block (fall back to "1x1")
            String sizeKey = "1x1";
            try {
                long chunkIndex = ChunkUtil.indexChunkFromBlock(this.targetBlock.x, this.targetBlock.z);
                WorldChunk chunk = this.targetWorld.getChunkIfInMemory(chunkIndex);
                if (chunk != null) {
                    BlockType current = chunk.getBlockType(this.targetBlock.x, this.targetBlock.y, this.targetBlock.z);
                    if (current != null) {
                        String id = current.getId();
                        if (id != null) {
                            Matcher m = Pattern.compile("(\\d+)x(\\d+)").matcher(id);
                            if (m.find()) {
                                sizeKey = m.group(1) + "x" + m.group(2);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            try {
                boolean removed = FileHelper.removeImageState(sizeKey, stateKey);
                if (removed) {
                    player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Image removed and state removed: " + stateKey));
                    // Refresh UI: close current page and reopen a fresh one so the list updates
                    this.close();
                    ImageDownloadPage page = new ImageDownloadPage(player.getPlayerRef(), this.targetWorld, this.targetBlock);
                    player.getPageManager().openCustomPage(player.getReference(), player.getReference().getStore(), page);
                } else player.sendMessage(com.hypixel.hytale.server.core.Message.raw("State not found or could not be removed: " + stateKey));
            } catch (IOException e) {
                player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Error removing image: " + e.getMessage()));
            }
        }
        else if ("Apply".equals(data.action)) {
            String stateKey = data.stateKey;
            if (stateKey == null || stateKey.isEmpty()) return;

            // If the provided key is an item id we generated (Boff_Frame_<name>),
            // replace the block with that item type instead of trying to apply
            // it as a state on the base Boff_Frame_1x1 block.
            try {
                if (stateKey.startsWith("Boff_Frame_") || stateKey.startsWith("Boff_Frame")) {
                    boolean replaced = UseFrameInteraction.replaceBlockWithItem(this.targetWorld, this.targetBlock, stateKey);
                    if (replaced) {
                        player.sendMessage(com.hypixel.hytale.server.core.Message.raw("The frame has been updated to the item: " + stateKey));

                        // Preserve name/url/blocks if present in the item's metadata
                        String itemId = stateKey;
                        String name = null;
                        String url = null;
                        int blocksX = 1;
                        try {
                            Path metaPath = FileHelper.MODS_ROOT.resolve("Frames").resolve(itemId + ".json");
                            if (Files.exists(metaPath) && Files.isRegularFile(metaPath)) {
                                String txt = Files.readString(metaPath);
                                org.bson.BsonDocument meta = org.bson.BsonDocument.parse(txt);
                                if (meta.containsKey("name")) name = meta.getString("name").getValue();
                                if (meta.containsKey("url")) url = meta.getString("url").getValue();
                                if (meta.containsKey("frames")) {
                                    org.bson.BsonArray fa = meta.getArray("frames");
                                    if (fa.size() > 0) {
                                        try {
                                            org.bson.BsonDocument last = fa.get(fa.size() - 1).asDocument();
                                            if (last.containsKey("blocks")) {
                                                org.bson.BsonDocument b = last.getDocument("blocks");
                                                if (b.containsKey("x")) blocksX = b.getInt32("x").getValue();
                                            }
                                        } catch (Exception ignore) {}
                                    }
                                }
                            }
                        } catch (Exception ignoredMeta) {}

                        int tx = this.targetBlock.x;
                        int ty = this.targetBlock.y;
                        int tz = this.targetBlock.z;

                        // Remove any existing instances at these coords and then register the new one
                        try {
                            FileHelper.removeInstancesAtCoords(tx, ty, tz);
                        } catch (Exception e) {
                            Frames.LOGGER.atWarning().withCause(e).log("Failed to remove existing instance at coords: " + e.getMessage());
                        }

                        try {
                            FileHelper.writeFrameMetadata(itemId, name, url, tx, ty, tz, blocksX);
                        } catch (Exception e) {
                            Frames.LOGGER.atWarning().withCause(e).log("Failed to write frame metadata: " + e.getMessage());
                        }
                    } else {
                        player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Could not replace the block with the item: " + stateKey));
                    }
                } else {
                    boolean applied = UseFrameInteraction.applyStateToBlock(this.targetWorld, this.targetBlock, stateKey);
                    if (applied) player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Frame updated to state: " + stateKey));
                    else player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Could not apply the state: " + stateKey));
                }
            } catch (Exception e) {
                player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Error applying: " + e.getMessage()));
            }
        }
    }
}
