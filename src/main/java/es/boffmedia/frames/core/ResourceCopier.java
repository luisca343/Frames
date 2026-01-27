package es.boffmedia.frames.core;

import es.boffmedia.frames.Frames;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Small helper responsible for copying embedded resources into the mods folder.
 */
public final class ResourceCopier {
    private ResourceCopier() {}

    public static void ensureManifestExists(Path modsRoot) {
        try {
            Path manifestOut = modsRoot.resolve("manifest.json");
            if (Files.exists(manifestOut)) return;

            try (InputStream is = ResourceCopier.class.getResourceAsStream("/manifest_generated.json")) {
                if (is == null) {
                    Frames.LOGGER.atWarning().log("Resource manifest_generated.json not found on classpath; skipping manifest copy");
                    return;
                }
                Files.createDirectories(manifestOut.getParent());
                Files.copy(is, manifestOut);
                Frames.LOGGER.atInfo().log("Copied manifest_generated.json -> " + manifestOut);
            }
        } catch (Exception e) {
            Frames.LOGGER.atWarning().withCause(e).log("Failed to ensure manifest.json: " + e.getMessage());
        }
    }

    public static void copyResourceDirectory(String resourcePath, Path outDir) throws IOException {
        String rp = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        URL url = ResourceCopier.class.getResource("/" + rp);
        if (url == null) {
            Frames.LOGGER.atWarning().log("Resource " + resourcePath + " not found on classpath; skipping copy");
            return;
        }

        String protocol = url.getProtocol();
        if ("file".equals(protocol)) {
            try {
                Path res = Paths.get(url.toURI());
                Files.walk(res).forEach(p -> {
                    if (Files.isRegularFile(p)) {
                        Path rel = res.relativize(p);
                        Path target = outDir.resolve(rel.toString());
                        try {
                            Files.createDirectories(target.getParent());
                            Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        } else if ("jar".equals(protocol)) {
            JarURLConnection conn = (JarURLConnection) url.openConnection();
            JarFile jar = conn.getJarFile();
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(rp + "/")) continue;
                if (entry.isDirectory()) continue;
                String relName = name.substring(rp.length() + 1);
                Path out = outDir.resolve(relName);
                Files.createDirectories(out.getParent());
                try (InputStream is = jar.getInputStream(entry)) {
                    Files.copy(is, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } else {
            try (InputStream is = ResourceCopier.class.getResourceAsStream("/" + rp)) {
                if (is == null) {
                    Frames.LOGGER.atWarning().log("Unknown resource protocol for " + resourcePath + ": " + protocol);
                    return;
                }
                Files.createDirectories(outDir.getParent());
                Files.copy(is, outDir, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        Frames.LOGGER.atInfo().log("Copied resource folder " + resourcePath + " -> " + outDir);
    }
}
