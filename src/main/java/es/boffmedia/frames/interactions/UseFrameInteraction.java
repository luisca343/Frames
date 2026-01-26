package es.boffmedia.frames.interactions;


import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import es.boffmedia.frames.Frames;
import es.boffmedia.frames.ui.ImageDownloadPage;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class UseFrameInteraction extends OpenCustomUIInteraction {

    public static final BuilderCodec<UseFrameInteraction> CODEC;

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        Frames.LOGGER.atInfo().log("Frame used by " + context.getEntity().getClass().getName());

        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        Player player = commandBuffer.getComponent(context.getEntity(), Player.getComponentType());
        PlayerRef playerRef = player.getPlayerRef();

        World world = player.getWorld();
        BlockPosition targetBlock = context.getTargetBlock();


        ImageDownloadPage page = new ImageDownloadPage(playerRef, world, targetBlock);
        player.getPageManager().openCustomPage(player.getReference(), player.getReference().getStore(), page);
    }


    public static boolean applyStateToBlock(@Nonnull World world, @Nonnull BlockPosition targetBlock, @Nonnull String stateKey) {
        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(targetBlock.x, targetBlock.z);
            WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
            if (chunk == null) {
                Frames.LOGGER.atWarning().log("Chunk not in memory for block: " + targetBlock);
                return false;
            }

            BlockType current = chunk.getBlockType(targetBlock.x, targetBlock.y, targetBlock.z);
            if (current == null) {
                Frames.LOGGER.atWarning().log("No current block type at: " + targetBlock);
                return false;
            }

            String newBlock = current.getBlockKeyForState(stateKey);
            if (newBlock == null) {
                Frames.LOGGER.atWarning().log("No block key for state '" + stateKey + "' on block " + current.getId());
                return false;
            }

            int newBlockId = BlockType.getAssetMap().getIndex(newBlock);
            BlockType newBlockType = (BlockType) BlockType.getAssetMap().getAsset(newBlockId);
            if (newBlockType == null) {
                Frames.LOGGER.atWarning().log("Failed to resolve BlockType for key: " + newBlock + " (index=" + newBlockId + ")");
                return false;
            }

            int rotation = chunk.getRotationIndex(targetBlock.x, targetBlock.y, targetBlock.z);
            chunk.setBlock(targetBlock.x, targetBlock.y, targetBlock.z, newBlockId, newBlockType, rotation, 0, 256);
            Frames.LOGGER.atInfo().log("Applied state '" + stateKey + "' -> block " + newBlock + " at " + targetBlock);
            return true;
        } catch (Exception e) {
            Frames.LOGGER.atSevere().withCause(e).log("Failed to apply state to block: " + e.getMessage());
            return false;
        }
    }

    /**
     * Replace the block at targetBlock with the block asset identified by itemId.
     * itemId should be the asset key such as "Boff_Frame_<name>".
     */
    public static boolean replaceBlockWithItem(@Nonnull World world, @Nonnull BlockPosition targetBlock, @Nonnull String itemId) {
        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(targetBlock.x, targetBlock.z);
            WorldChunk chunk = world.getChunkIfInMemory(chunkIndex);
            if (chunk == null) {
                Frames.LOGGER.atWarning().log("Chunk not in memory for block: " + targetBlock);
                return false;
            }

            int newBlockId = BlockType.getAssetMap().getIndex(itemId);
            BlockType newBlockType = (BlockType) BlockType.getAssetMap().getAsset(newBlockId);
            if (newBlockType == null) {
                Frames.LOGGER.atWarning().log("Failed to resolve BlockType for itemId: " + itemId + " (index=" + newBlockId + ")");
                return false;
            }

            int rotation = chunk.getRotationIndex(targetBlock.x, targetBlock.y, targetBlock.z);
            chunk.setBlock(targetBlock.x, targetBlock.y, targetBlock.z, newBlockId, newBlockType, rotation, 0, 256);
            Frames.LOGGER.atInfo().log("Replaced block at " + targetBlock + " with " + itemId);
            return true;
        } catch (Exception e) {
            Frames.LOGGER.atSevere().withCause(e).log("Failed to replace block: " + e.getMessage());
            return false;
        }
    }


    static {
        Frames.LOGGER.atInfo().log("Registering UseFrameInteraction codec");
        CODEC = BuilderCodec.builder((Class)UseFrameInteraction.class, (Supplier)UseFrameInteraction::new, UseFrameInteraction.CODEC).build();
    }
}
