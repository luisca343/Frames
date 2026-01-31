package es.boffmedia.frames.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import es.boffmedia.frames.FileHelper;
import es.boffmedia.frames.Frames;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ListUserImagesPage extends InteractiveCustomUIPage<ListUserImagesPage.ListData> {

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
    private final PlayerRef playerRef;
    private final World world;
    private final BlockPosition targetBlock;
    private final InteractiveCustomUIPage<?> returnPage;

    public ListUserImagesPage(@Nonnull PlayerRef playerRef, @Nonnull World world, @Nonnull BlockPosition targetBlock, @Nonnull String[] entries, @Nonnull InteractiveCustomUIPage<?> returnPage) {
        super(playerRef, CustomPageLifetime.CanDismiss, ListData.CODEC);
        this.entries = entries != null ? entries : new String[0];
        this.playerRef = playerRef;
        this.world = world;
        this.targetBlock = targetBlock;
        this.returnPage = returnPage;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/ListFramesPage.ui");

        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton",
                new EventData().append("Action", "Close"), false);

        try { uiCommandBuilder.clear("#FramesList"); } catch (Exception ignored) {}

        for (int i = 0; i < entries.length; i++) {
            String entry = entries[i];
            
            Frames.LOGGER.atInfo().log("Adding entry to ListUserImagesPage: " + entry);

            try {
                uiCommandBuilder.append("#FramesList", "Pages/FrameListItem.ui");
                uiCommandBuilder.set("#FramesList[" + i + "] #EntryLabel.Text", entry);

                uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating,
                    "#FramesList[" + i + "] #ApplyButton",
                        new EventData().append("Action", "Apply").append("Index", Integer.toString(i)),
                        false);

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
                        String id = entry.contains(" - ") ? entry.split(" - ", 2)[0] : entry;

                        // If we were passed the original ImageDownloadPage instance, set its state key and reopen it.
                        try {
                            if (returnPage != null && returnPage instanceof ImageDownloadPage) {
                                ((ImageDownloadPage) returnPage).setSelectedStateKey(id);
                                player.getPageManager().openCustomPage(ref, store, returnPage);
                            } else {
                                // Fallback: open a new ImageDownloadPage instance with the selected id
                                player.getPageManager().openCustomPage(player.getReference(), player.getReference().getStore(),
                                        new ImageDownloadPage(playerRef, world, targetBlock, id));
                            }
                        } catch (Exception e) {
                            player.sendMessage(Message.raw("Failed to reopen Image page: " + e.getMessage()));
                        }
                    }
                } catch (Exception ignored) {}
                break;

            case "Delete":
                try {
                    int idx = Integer.parseInt(data.index != null ? data.index : "-1");
                    if (idx >= 0 && idx < entries.length) {
                        if (!es.boffmedia.frames.PermissionsUtil.canDeleteFrames(player)) {
                            player.sendMessage(Message.raw("You do not have permission to delete metadata files."));
                            return;
                        }
                        String entry = entries[idx];
                        String id = entry.contains(" - ") ? entry.split(" - ", 2)[0] : entry;
                        Path meta = FileHelper.MODS_ROOT.resolve("Frames").resolve(id + ".json");
                        try {
                            if (Files.exists(meta)) {
                                Files.delete(meta);
                                player.sendMessage(Message.raw("Metadata file " + id + " has been deleted."));
                            }
                        } catch (Exception e) {
                            player.sendMessage(Message.raw("Error deleting metadata: " + e.getMessage()));
                        }
                    }
                } catch (Exception ignored) {}
                break;

            default:
                break;
        }
    }
}
