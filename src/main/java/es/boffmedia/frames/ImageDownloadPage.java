package es.boffmedia.frames;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.protocol.BlockPosition;
import java.io.IOException;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class ImageDownloadPage extends InteractiveCustomUIPage<ImageDownloadPage.ImageDownloadData> {

    public static class ImageDownloadData {
        public String action;
        public String url;

        public static final BuilderCodec<ImageDownloadData> CODEC = ((BuilderCodec.Builder<ImageDownloadData>) ((BuilderCodec.Builder<ImageDownloadData>)
                    BuilderCodec.builder(ImageDownloadData.class, ImageDownloadData::new))
                    .append(new KeyedCodec<>("Action", Codec.STRING), (ImageDownloadData o, String v) -> o.action = v, (ImageDownloadData o) -> o.action)
                    .add()
                    .append(new KeyedCodec<>("@UrlInput", Codec.STRING), (ImageDownloadData o, String v) -> o.url = v, (ImageDownloadData o) -> o.url)
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
            new EventData().append("Action", "Upload").append("@UrlInput", "#UrlInput.Value"),
            false);
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
                String stateKey = FileHelper.addImageStateFromUrl(url);
                player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Imagen procesada y añadida como estado: " + stateKey));

                // Delay applying the state by 10 seconds to allow any asset/IO sync.
                new Thread(() -> {
                    try {
                        Thread.sleep(10_000);
                        Frames.LOGGER.atInfo().log("Attempting to apply state '" + stateKey + "' after delay");
                        boolean applied = UseFrameInteraction.applyStateToBlock(this.targetWorld, this.targetBlock, stateKey);
                        if (applied) {
                            player.sendMessage(com.hypixel.hytale.server.core.Message.raw("El marco se ha actualizado al nuevo estado: " + stateKey));
                        } else {
                            player.sendMessage(com.hypixel.hytale.server.core.Message.raw("No se pudo aplicar el nuevo estado al bloque. Asegúrate de que el chunk está cargado y que el asset se ha recargado."));
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        Frames.LOGGER.atWarning().withCause(ie).log("Delayed state application interrupted");
                    }
                }).start();
            } catch (IOException e) {
                player.sendMessage(com.hypixel.hytale.server.core.Message.raw("Error al descargar o procesar la imagen: " + e.getMessage()));
            }
        }
    }
}
