package es.boffmedia.frames.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import es.boffmedia.frames.FileHelper;
import es.boffmedia.frames.PermissionsUtil;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class ListFramesCommand extends AbstractPlayerCommand {

    public ListFramesCommand() {
        super("listframes", "List all created picture-frame metadata files (admin)");
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Player sender = commandContext.senderAs(Player.class);

        if (!PermissionsUtil.canDeleteFrames(sender)) {
            sender.sendMessage(Message.raw("You do not have permission to run this command."));
            return;
        }

        Path metaDir = FileHelper.MODS_ROOT.resolve("Frames");
        if (!Files.exists(metaDir) || !Files.isDirectory(metaDir)) {
            sender.sendMessage(Message.raw("No metadata files found."));
            return;
        }

        java.util.List<String> entries = new java.util.ArrayList<>();
        try (Stream<Path> stream = Files.list(metaDir)) {
            stream.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".json"))
                  .forEach(p -> {
                      try {
                          String txt = Files.readString(p);
                          BsonDocument meta = BsonDocument.parse(txt);
                          String id = meta.containsKey("itemId") ? meta.getString("itemId").getValue() : p.getFileName().toString().replaceFirst("\\.json$", "");
                          String name = meta.containsKey("name") ? meta.getString("name").getValue() : "";
                          String coords = "";
                          if (meta.containsKey("coords")) {
                              BsonDocument c = meta.getDocument("coords");
                              coords = c.getInt32("x").getValue() + "," + c.getInt32("y").getValue() + "," + c.getInt32("z").getValue();
                          }
                          entries.add(id + " — " + name + (coords.isEmpty() ? "" : " @ " + coords));
                      } catch (Exception e) {
                          entries.add("ERROR: " + p.getFileName().toString() + " — " + e.getMessage());
                      }
                  });

            // Open UI page showing entries
            String[] arr = entries.toArray(new String[0]);
            es.boffmedia.frames.ui.ListFramesPage page = new es.boffmedia.frames.ui.ListFramesPage(playerRef, arr);
            sender.getPageManager().openCustomPage(sender.getReference(), sender.getReference().getStore(), page);
        } catch (Exception e) {
            sender.sendMessage(Message.raw("Error listing metadata: " + e.getMessage()));
        }
    }
}
