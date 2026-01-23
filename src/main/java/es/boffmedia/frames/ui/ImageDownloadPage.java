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
import es.boffmedia.frames.FileHelper;
import es.boffmedia.frames.Frames;
import es.boffmedia.frames.interactions.UseFrameInteraction;

import javax.annotation.Nonnull;

public class ImageDownloadPage extends InteractiveCustomUIPage<ImageDownloadPage.ImageDownloadData> {

    public static class ImageDownloadData {
        public String action;
        public String url;
        public String name;
        public String stateKey;

        public static final BuilderCodec<ImageDownloadData> CODEC = ((BuilderCodec.Builder<ImageDownloadData>) ((BuilderCodec.Builder<ImageDownloadData>)
                BuilderCodec.builder(ImageDownloadData.class, ImageDownloadData::new))
                .append(new KeyedCodec<>("Action", Codec.STRING), (ImageDownloadData o, String v) -> o.action = v, (ImageDownloadData o) -> o.action)
                .add()
                .append(new KeyedCodec<>("@UrlInput", Codec.STRING), (ImageDownloadData o, String v) -> o.url = v, (ImageDownloadData o) -> o.url)
                .add()
                .append(new KeyedCodec<>("@NameInput", Codec.STRING), (ImageDownloadData o, String v) -> o.name = v, (ImageDownloadData o) -> o.name)
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
            new EventData().append("Action", "Upload").append("@UrlInput", "#UrlInput.Value").append("@NameInput", "#NameInput.Value"),
                false);

        // Populate existing image states into the UI list (if present in JSON)
        final String statesListRef = "#StatesList";
        try {
            // Determine sizeKey for the target block (fall back to "1x1")
            String sizeKey = "1x1";
            try {
                long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(this.targetBlock.x, this.targetBlock.z);
                WorldChunk chunk = this.targetWorld.getChunkIfInMemory(chunkIndex);
                if (chunk != null) {
                    BlockType current = chunk.getBlockType(this.targetBlock.x, this.targetBlock.y, this.targetBlock.z);
                    if (current != null) {
                        String id = current.getId();
                        if (id != null) {
                            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)x(\\d+)").matcher(id);
                            if (m.find()) {
                                int w = Integer.parseInt(m.group(1));
                                int h = Integer.parseInt(m.group(2));
                                sizeKey = (Math.max(1, w)) + "x" + (Math.max(1, h));
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}

            org.bson.BsonDocument doc = FileHelper.loadOrCreateDocument(sizeKey);
            if (doc != null && doc.containsKey("BlockType")) {
                org.bson.BsonDocument blockType = doc.getDocument("BlockType");
                if (blockType.containsKey("State")) {
                    org.bson.BsonDocument state = blockType.getDocument("State");
                    if (state.containsKey("Definitions")) {
                        org.bson.BsonDocument defs = state.getDocument("Definitions");
                        java.util.List<String> keys = new java.util.ArrayList<>(defs.keySet());
                        uiCommandBuilder.clear(statesListRef);
                        for (int i = 0; i < keys.size(); i++) {
                            String key = keys.get(i);
                            String texture = "";
                            try {
                                org.bson.BsonDocument def = defs.getDocument(key);
                                if (def.containsKey("CustomModelTexture")) {
                                    org.bson.BsonArray texArr = def.getArray("CustomModelTexture");
                                    if (texArr.size() > 0 && texArr.get(0).isDocument()) {
                                        org.bson.BsonDocument texDoc = texArr.get(0).asDocument();
                                        if (texDoc.containsKey("Texture")) texture = texDoc.getString("Texture").getValue();
                                    }
                                }
                            } catch (Exception ignored) {}

                            uiCommandBuilder.append(statesListRef, "Pages/FrameStateItem.ui");
                            String instancePrefix = statesListRef + "[" + i + "]";

                            String escapedTex = texture.replace("\"", "\\\"");

                            // Extract filename only from texture path (e.g. Blocks/Frames/1x1/FRAME_x.png -> FRAME_x.png)
                            String fileNameOnly = escapedTex;
                            int slash = fileNameOnly.lastIndexOf('/');
                            if (slash >= 0 && slash + 1 < fileNameOnly.length()) fileNameOnly = fileNameOnly.substring(slash + 1);

                            // Set filename label on the appended instance
                            uiCommandBuilder.set(instancePrefix + " #FileNameLabel.Text", fileNameOnly);

                                // Bind the Apply button for this specific instance
                                uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, instancePrefix + " #ApplyButton",
                                    new EventData().append("Action", "Apply").append("StateKey", key), false);
                                // Bind the Delete button for this specific instance
                                uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, instancePrefix + " #DeleteButton",
                                    new EventData().append("Action", "Delete").append("StateKey", key), false);
                        }
                    }
                }
            }
        } catch (java.io.IOException ioe) {
            Frames.LOGGER.atWarning().withCause(ioe).log("Failed to read frame JSON for UI: " + ioe.getMessage());
        }
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

                String stateKey = FileHelper.addImageStateFromUrl(url, sizeX, sizeY, data.name);
                // Lo ideal sería poder aplicar justo después de que termine de actualizar, pero no veo la forma
                // Así que por ahora cierro y que se actualice manualmente
                this.close();
                // Delay applying the state by 10 seconds to allow any asset/IO sync.
                /*
                new Thread(() -> {
                    try {
                        Thread.sleep(5_000);
                        Frames.LOGGER.atInfo().log("Attempting to apply state '" + stateKey + "' after delay");
                        boolean applied = UseFrameInteraction.applyStateToBlock(this.targetWorld, this.targetBlock, stateKey);
                        if (applied) {
                            //player.sendMessage(com.hypixel.hytale.server.core.Message.raw("El marco se ha actualizado al nuevo estado: " + stateKey));
                        } else {
                            player.sendMessage(com.hypixel.hytale.server.core.Message.raw("No se pudo aplicar el nuevo estado al bloque. Asegúrate de que el chunk está cargado y que el asset se ha recargado."));
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        Frames.LOGGER.atWarning().withCause(ie).log("Delayed state application interrupted");
                    }
                }).start();*/
            } catch (IOException e) {
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
                if (removed) player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Imagen eliminada y estado removido: " + stateKey));
                else player.sendMessage(com.hypixel.hytale.server.core.Message.raw("No se encontró el estado o no se pudo eliminar: " + stateKey));
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
