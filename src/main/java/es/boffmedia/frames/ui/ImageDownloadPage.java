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
                .append(new KeyedCodec<>("StateKey", Codec.STRING), (ImageDownloadData o, String v) -> o.stateKey = v, (ImageDownloadData o) -> o.stateKey)
                .add())
                .build();
    }

    private final PlayerRef playerRef;
    private final World targetWorld;
    private final BlockPosition targetBlock;

    public ImageDownloadPage(@Nonnull PlayerRef playerRef, @Nonnull World world, @Nonnull BlockPosition targetBlock) {
        super(playerRef, CustomPageLifetime.CanDismiss, ImageDownloadData.CODEC);
        this.playerRef = playerRef;
        this.targetWorld = world;
        this.targetBlock = targetBlock;
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

        // Prefill inputs from metadata if a frame metadata file exists for this block coords
        try {
            Path metaDir = FileHelper.MODS_ROOT.resolve("Frames");
            if (Files.exists(metaDir) && Files.isDirectory(metaDir)) {
                try (java.util.stream.Stream<Path> stream = Files.list(metaDir)) {
                    java.util.Iterator<Path> it = stream.iterator();
                    while (it.hasNext()) {
                        Path p = it.next();
                        try {
                            if (!Files.isRegularFile(p)) continue;
                            String txt = Files.readString(p);
                            org.bson.BsonDocument meta = org.bson.BsonDocument.parse(txt);
                            if (!meta.containsKey("coords")) continue;
                            org.bson.BsonDocument coords = meta.getDocument("coords");
                            int mx = coords.getInt32("x").getValue();
                            int my = coords.getInt32("y").getValue();
                            int mz = coords.getInt32("z").getValue();
                            if (mx == this.targetBlock.x && my == this.targetBlock.y && mz == this.targetBlock.z) {
                                String mname = meta.containsKey("name") ? meta.getString("name").getValue() : "";
                                String murl = meta.containsKey("url") ? meta.getString("url").getValue() : "";
                                int mbx = 1;
                                if (meta.containsKey("blocks")) {
                                    org.bson.BsonDocument b = meta.getDocument("blocks");
                                    if (b.containsKey("x")) mbx = b.getInt32("x").getValue();
                                }
                                try { uiCommandBuilder.set("#NameInput.Value", mname); } catch (Exception ignore) {}
                                try { uiCommandBuilder.set("#UrlInput.Value", murl); } catch (Exception ignore) {}
                                try { uiCommandBuilder.set("#SizeXInput.Value", Integer.toString(mbx)); } catch (Exception ignore) {}
                                break;
                            }
                        } catch (Exception ignoredMeta) {
                            Frames.LOGGER.atWarning().withCause(ignoredMeta).log("Failed to read metadata file: " + p);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull ImageDownloadData data) {
        Player player = store.getComponent(ref, Player.getComponentType());

        if (data == null || data.action == null) return;

        if ("Upload".equals(data.action)) {
            String url = data.url;
            if (url == null || url.trim().isEmpty()) {
                player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Por favor, introduce una URL válida en el campo."));
                return;
            }

            player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Descargando imagen..."));
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
                        Path metaDir = FileHelper.MODS_ROOT.resolve("Frames");
                        Files.createDirectories(metaDir);
                        Path metaFile = metaDir.resolve(itemId + ".json");
                        String metaJson = "{\n" +
                                "  \"itemId\": \"" + itemId + "\",\n" +
                                "  \"name\": \"" + (data.name == null ? "" : data.name.replace("\"", "\\\"")) + "\",\n" +
                                "  \"url\": \"" + (data.url == null ? "" : data.url.replace("\"", "\\\"")) + "\",\n" +
                                "  \"coords\": { \"x\": " + this.targetBlock.x + ", \"y\": " + this.targetBlock.y + ", \"z\": " + this.targetBlock.z + " },\n" +
                                "  \"blocks\": { \"x\": " + blocksX + " },\n" +
                                "  \"createdAt\": \"" + java.time.Instant.now().toString() + "\"\n" +
                                "}\n";
                        Files.writeString(metaFile, metaJson);
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
                                player.sendMessage(com.hypixel.hytale.server.core.Message.raw("El marco se ha actualizado al nuevo item: " + itemId));
                            } else {
                                player.sendMessage(com.hypixel.hytale.server.core.Message.raw("No se pudo reemplazar el bloque. Asegúrate de que el chunk está cargado y que los assets se han recargado."));
                            }
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            Frames.LOGGER.atWarning().withCause(ie).log("Delayed replacement interrupted");
                        }
                    }).start();
                } catch (IOException ioe) {
                    player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Error al descargar o guardar la imagen: " + ioe.getMessage()));
                }
            } catch (Exception e) {
                player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Error al descargar o procesar la imagen: " + e.getMessage()));
            }
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
                    player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Imagen eliminada y estado removido: " + stateKey));
                    // Refresh UI: close current page and reopen a fresh one so the list updates
                    this.close();
                    ImageDownloadPage page = new ImageDownloadPage(player.getPlayerRef(), this.targetWorld, this.targetBlock);
                    player.getPageManager().openCustomPage(player.getReference(), player.getReference().getStore(), page);
                } else player.sendMessage(com.hypixel.hytale.server.core.Message.raw("No se encontró el estado o no se pudo eliminar: " + stateKey));
            } catch (IOException e) {
                player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Error al eliminar la imagen: " + e.getMessage()));
            }
        }
        else if ("Apply".equals(data.action)) {
            String stateKey = data.stateKey;
            if (stateKey == null || stateKey.isEmpty()) return;
            boolean applied = UseFrameInteraction.applyStateToBlock(this.targetWorld, this.targetBlock, stateKey);
            /*if (applied) player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Marco actualizado al estado: " + stateKey));
            else player.sendMessage(com.hypixel.hytale.server.core.Message.raw("No se pudo aplicar el estado: " + stateKey));*/
        }
    }
}
