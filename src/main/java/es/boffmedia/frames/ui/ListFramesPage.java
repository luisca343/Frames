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
import java.nio.file.Paths;
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
                        
                        player.sendMessage(Message.raw(id));
                    }
                } catch (Exception ignored) {}
                this.close();
                break;

            case "Delete":
                try {
                    int idx = Integer.parseInt(data.index != null ? data.index : "-1");
                    if (idx >= 0 && idx < entries.length) {
                        if (!PermissionsUtil.canDeleteFrames(player)) {
                            player.sendMessage(Message.raw("You do not have permission to delete metadata files."));
                            return;
                        }
                        String entry = entries[idx];
                        String id = entry.contains(" — ") ? entry.split(" — ", 2)[0] : entry;
                        // Attempt to delete all files related to this generated frame
                        String baseName = id.startsWith("Boff_Frame_") ? id.substring("Boff_Frame_".length()) : null;

                        // 1) Delete metadata file under mods/BoffmediaFrames/Frames/<id>.json
                        Path meta = FileHelper.MODS_ROOT.resolve("Frames").resolve(id + ".json");
                        try {
                                if (Files.exists(meta)) {
                                Files.delete(meta);
                                player.sendMessage(Message.raw("Metadata file " + id + " has been deleted."));
                            }
                        } catch (Exception e) {
                            player.sendMessage(Message.raw("Error deleting metadata: " + e.getMessage()));
                        }

                        // 2) Delete generated item JSON
                        try {
                            Path item = FileHelper.MODS_ROOT.resolve(Paths.get("Server", "Item", "Items", "Furniture", "Frames", id + ".json"));
                            if (Files.exists(item)) {
                                Files.delete(item);
                                player.sendMessage(Message.raw("Item file deleted: " + item.getFileName().toString()));
                            }
                        } catch (Exception e) {
                            player.sendMessage(Message.raw("Error deleting item JSON: " + e.getMessage()));
                        }

                        // 3) Delete blockymodel and image files if we can derive the base name
                        if (baseName != null) {
                            try {
                                Path model = FileHelper.MODS_ROOT.resolve(Paths.get("Common", "Blocks", "Frames", baseName + ".blockymodel"));
                                if (Files.exists(model)) {
                                    Files.delete(model);
                                    player.sendMessage(Message.raw("Model deleted: " + model.getFileName().toString()));
                                }
                            } catch (Exception e) {
                                player.sendMessage(Message.raw("Error deleting blockymodel: " + e.getMessage()));
                            }

                            try {
                                Path img = FileHelper.MODS_ROOT.resolve(Paths.get("Common", "Blocks", "Frames", "Images", baseName + ".png"));
                                if (Files.exists(img)) {
                                    Files.delete(img);
                                    player.sendMessage(Message.raw("Image deleted: " + img.getFileName().toString()));
                                }
                            } catch (Exception e) {
                                player.sendMessage(Message.raw("Error deleting image: " + e.getMessage()));
                            }
                        }

                        // 4) Remove any state definitions referencing this id across all frame JSON sizes
                        try {
                            boolean anyRemoved = false;
                            for (String sk : FileHelper.FRAME_SIZES) {
                                try {
                                    boolean removed = FileHelper.removeImageState(sk, id);
                                    if (removed) {
                                            anyRemoved = true;
                                            player.sendMessage(Message.raw("Removed state " + id + " from frame json: " + sk));
                                        }
                                } catch (Exception inner) {
                                    // non-fatal per-size failure
                                }
                            }
                            if (!anyRemoved) {
                                // inform optionally that nothing was found in state definitions
                                // (but avoid spamming if we've already deleted files above)
                            }
                        } catch (Exception e) {
                            player.sendMessage(Message.raw("Error removing state definitions: " + e.getMessage()));
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
