package es.boffmedia.frames.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import java.nio.file.Files;
import java.nio.file.Path;
import es.boffmedia.frames.FileHelper;
import es.boffmedia.frames.Frames;
import es.boffmedia.frames.PermissionsUtil;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public class ListFramesPage extends InteractiveCustomUIPage<ListFramesPage.ListData> {

    public static class ListData {
        public String action;
        public String index;

        public static final BuilderCodec<ListData> CODEC = ((BuilderCodec.Builder<ListData>) BuilderCodec.builder(ListData.class, ListData::new))
                .append(new KeyedCodec<>("Action", Codec.STRING), (ListData o, String v) -> o.action = v, (ListData o) -> o.action)
                .add()
                .append(new KeyedCodec<>("Index", Codec.STRING), (ListData o, String v) -> o.index = v, (ListData o) -> o.index)
                .add()
                .build();
    }

    private final String[] entries;

    public ListFramesPage(@Nonnull PlayerRef playerRef, @Nonnull String[] entries) {
        super(playerRef, CustomPageLifetime.CanDismiss, ListData.CODEC);
        this.entries = entries != null ? entries : new String[0];
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/ListFramesPage.ui");

        // Wire Close button to send Action=Close so we can optionally handle it server-side
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                new EventData().append("Action", "Close"), false);

        // Clear any existing children in the scrolling list container
        try { uiCommandBuilder.clear("#FramesList"); } catch (Exception ignored) {}

        // Append one `FrameListItem.ui` per entry and bind its buttons
        for (int i = 0; i < entries.length; i++) {
            String entry = entries[i];
            try {
                uiCommandBuilder.append("#FramesList", "Pages/FrameListItem.ui");
                // Set the label text for the newly appended item (indexed selector)

                Frames.LOGGER.atInfo().log("Adding frame entry to UI: " + entry);

                uiCommandBuilder.set("#FramesList[" + i + "] #EntryLabel.Text", entry);

                // Bind Apply button for this item; include index so handler knows which entry
                uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    "#FramesList[" + i + "] #ApplyButton",
                        new EventData().append("Action", "Apply").append("Index", Integer.toString(i)),
                        false);

                // Bind Delete button for this item
                uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    "#FramesList[" + i + "] #DeleteButton",
                        new EventData().append("Action", "Delete").append("Index", Integer.toString(i)),
                        false);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull ListData data) {
        if (data == null || data.action == null) return;

        Player player = store.getComponent(ref, Player.getComponentType());

        switch (data.action) {
            case "Close":
                this.close();
                break;

            case "Apply":
                try {
                    int idx = Integer.parseInt(data.index != null ? data.index : "-1");
                    if (idx >= 0 && idx < entries.length) {
                        String entry = entries[idx];
                        String id = entry.contains(" — ") ? entry.split(" — ", 2)[0] : entry;
                        player.sendMessage(Message.raw("Apply selected: " + id));
                    }
                } catch (Exception ignored) {}
                this.close();
                break;

            case "Delete":
                try {
                    int idx = Integer.parseInt(data.index != null ? data.index : "-1");
                    if (idx >= 0 && idx < entries.length) {
                        if (!PermissionsUtil.canDeleteFrames(player)) {
                            player.sendMessage(Message.raw("No tienes permiso para eliminar archivos de metadatos."));
                            return;
                        }
                        String entry = entries[idx];
                        String id = entry.contains(" — ") ? entry.split(" — ", 2)[0] : entry;
                        Path meta = FileHelper.MODS_ROOT.resolve("Frames").resolve(id + ".json");
                        try {
                            if (Files.exists(meta)) {
                                Files.delete(meta);
                                player.sendMessage(Message.raw("El archivo de metadatos " + id + " ha sido eliminado."));
                            } else {
                                player.sendMessage(Message.raw("Archivo no encontrado: " + meta.toString()));
                            }
                        } catch (Exception e) {
                            player.sendMessage(Message.raw("Error eliminando: " + e.getMessage()));
                        }
                    }
                } catch (Exception ignored) {}
                this.close();
                break;

            default:
                break;
        }
    }
}
