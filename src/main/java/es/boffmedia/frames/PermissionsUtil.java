package es.boffmedia.frames;

import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

public final class PermissionsUtil {

    // Permission node to allow deleting frame image states
    public static final String PERMISSION_DELETE_FRAME = "boffmedia.frames.delete";
    // Permission node to allow uploading images / creating new frames
    public static final String PERMISSION_UPLOAD_FRAME = "boffmedia.frames.upload";
    // Permission node to allow opening the frame UI (default: allowed for all players)
    public static final String PERMISSION_OPEN_GUI = "boffmedia.frames.open";

    private PermissionsUtil() {}

    public static boolean hasNegatedPermission(@Nonnull final Player player, @Nonnull final String permission) {
        final PermissionsModule perms = PermissionsModule.get();
        final UUID uuid = ((CommandSender) player).getUuid();
        return perms.hasPermission(uuid, "-" + permission);
    }

    public static boolean isAdmin(@Nonnull final Player player) {
        final PermissionsModule perms = PermissionsModule.get();
        final UUID uuid = ((CommandSender) player).getUuid();
        final Set<String> groups = perms.getGroupsForUser(uuid);
        return groups != null && groups.contains("OP");
    }

    public static boolean canDeleteFrames(@Nonnull final Player player) {
        if (isAdmin(player)) return true;
        final PermissionsModule perms = PermissionsModule.get();
        final UUID uuid = ((CommandSender) player).getUuid();
        if (hasNegatedPermission(player, PERMISSION_DELETE_FRAME)) return false;
        return perms.hasPermission(uuid, PERMISSION_DELETE_FRAME);
    }

    public static boolean canUploadFrames(@Nonnull final Player player) {
        if (isAdmin(player)) return true;
        final PermissionsModule perms = PermissionsModule.get();
        final UUID uuid = ((CommandSender) player).getUuid();
        if (hasNegatedPermission(player, PERMISSION_UPLOAD_FRAME)) return false;
        return perms.hasPermission(uuid, PERMISSION_UPLOAD_FRAME);
    }

    public static boolean canOpenGui(@Nonnull final Player player) {
        if (isAdmin(player)) return true;
        final PermissionsModule perms = PermissionsModule.get();
        final UUID uuid = ((CommandSender) player).getUuid();
        if (hasNegatedPermission(player, PERMISSION_OPEN_GUI)) return false;
        // Default: allow opening GUI for all players unless explicitly denied
        return true;
    }
}
