package es.boffmedia.frames;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import es.boffmedia.frames.interactions.UseFrameInteraction;

import javax.annotation.Nonnull;

public class Frames extends JavaPlugin {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public Frames(@Nonnull JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
        this.getCodecRegistry(Interaction.CODEC.register("Frames_UseFrameInteraction", UseFrameInteraction.class, UseFrameInteraction.CODEC));
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());

        FileHelper.loadFiles();
        this.getCommandRegistry().registerCommand(new es.boffmedia.frames.commands.ListFramesCommand());
    }
}