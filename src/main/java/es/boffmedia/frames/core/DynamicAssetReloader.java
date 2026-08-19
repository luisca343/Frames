package es.boffmedia.frames.core;

import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.AssetUpdateQuery;
import com.hypixel.hytale.server.core.asset.common.CommonAssetModule;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import com.hypixel.hytale.server.core.asset.common.asset.FileCommonAsset;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import es.boffmedia.frames.Frames;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Reloads dynamic frame JSON assets and registers their raw model/texture files as CommonAssets. */
public final class DynamicAssetReloader {
    private static final String PACK_KEY = "Boffmedia:Frames";

    private DynamicAssetReloader() {}

    private static FileCommonAsset registerCommonFile(Path file, String assetName) throws Exception {
        byte[] bytes = Files.readAllBytes(file);
        FileCommonAsset asset = new FileCommonAsset(file, assetName, bytes);
        CommonAssetRegistry.addCommonAsset(PACK_KEY, asset);
        Frames.LOGGER.atInfo().log("Registered common asset " + assetName + " (" + bytes.length + " bytes)");
        return asset;
    }

    public static boolean reloadGeneratedFrame(Path modsRoot, String itemId, Path modelPath, Path itemPath) {
        try {
            if (!Files.isRegularFile(modelPath) || !Files.isRegularFile(itemPath)) {
                Frames.LOGGER.atWarning().log("Dynamic frame files are missing: model=" + modelPath + ", item=" + itemPath);
                return false;
            }

            Path texturePath = modelPath.resolveSibling(modelPath.getFileName().toString().replaceFirst("\\.blockymodel$", ".png"));
            if (!Files.isRegularFile(texturePath)) {
                Frames.LOGGER.atWarning().log("Dynamic frame texture is missing: " + texturePath);
                return false;
            }

            // .blockymodel and .png are CommonAssets in Hytale's asset system.
            // Loading only BlockType/Item JSON (or ModelAsset) does not make these
            // raw files available to the client, which is what caused the magenta cube.
            String modelName = "Blocks/Frames/" + modelPath.getFileName();
            String textureName = "Blocks/Frames/" + texturePath.getFileName();

            // Register the common assets first, but do NOT send them yet.
            // Loading the Item/BlockType can trigger an asset rebuild. If the common
            // assets are sent before that rebuild, the rebuild may discard the dynamic
            // model/texture on the client.
            FileCommonAsset modelAsset = registerCommonFile(modelPath, modelName);
            FileCommonAsset textureAsset = registerCommonFile(texturePath, textureName);

            AssetUpdateQuery query = new AssetUpdateQuery(
                    new AssetUpdateQuery.RebuildCache(true, true, true, true, true, true));

            // The Item JSON owns the embedded BlockType. Reload it first.
            AssetStore itemStore = Item.getAssetStore();
            itemStore.loadAssetsFromPaths(PACK_KEY, List.of(itemPath), query, true);

            // Only after the server-side asset rebuild has completed do we send the
            // model + texture to the client. This avoids the second rebuild wiping
            // out a texture that was briefly visible.
            CommonAssetModule.get().sendAssets(List.of(modelAsset, textureAsset), true);
            Frames.LOGGER.atInfo().log("Sent dynamic model + texture after BlockType reload for " + itemId);

            Frames.LOGGER.atInfo().log("Dynamically loaded frame asset and common resources: " + itemId);
            return true;
        } catch (Exception e) {
            Frames.LOGGER.atWarning().withCause(e).log("Failed to dynamically reload frame assets for " + itemId + ": " + e.getMessage());
            return false;
        }
    }
}
