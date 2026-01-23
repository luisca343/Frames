package es.boffmedia.frames.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import java.io.IOException;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Ref;
import es.boffmedia.frames.FileHelper;

import javax.annotation.Nonnull;

public class UpdateFrameUrlCommand extends AbstractPlayerCommand {
    private final RequiredArg<String> urlArg;

    public UpdateFrameUrlCommand() {
        super("url", "Download an image directly from a URL and save it as FRAME_TEST.png");
        this.urlArg = withRequiredArg("url", "Image URL to download", (ArgumentType) ArgTypes.STRING);
    }

    @Override
    protected void execute(@Nonnull CommandContext commandContext, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        Player sender = commandContext.senderAs(Player.class);

        String url = commandContext.get(this.urlArg);
        if (url == null || url.trim().isEmpty()) {
            sender.sendMessage(Message.raw("Debe especificar una URL. Uso: /updateframe url <url>"));
            return;
        }

        sender.sendMessage(Message.raw("Descargando imagen..."));
        try {
            String stateKey = FileHelper.addImageStateFromUrl(url, 32, 32);
            sender.sendMessage(Message.raw("Imagen procesada y añadida como estado: " + stateKey));
        } catch (IOException e) {
            sender.sendMessage(Message.raw("Error al descargar o procesar la imagen: " + e.getMessage()));
        }
    }
}
