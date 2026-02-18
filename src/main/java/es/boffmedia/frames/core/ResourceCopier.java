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
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
            // Load the embedded generated manifest (if present)
            try (InputStream is = ResourceCopier.class.getResourceAsStream("/manifest_generated.json")) {
                if (is == null) {
                    Frames.LOGGER.atWarning().log("Resource manifest_generated.json not found on classpath; skipping manifest copy/update");
                    return;
                }

                String genManifest = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                // Extract ServerVersion from the generated manifest
                Pattern svPattern = Pattern.compile("\"ServerVersion\"\\s*:\\s*\"([^\"]*)\"");
                Matcher genMatcher = svPattern.matcher(genManifest);
                String genServerVersion = null;
                if (genMatcher.find()) {
                    genServerVersion = genMatcher.group(1);
                }

                Files.createDirectories(manifestOut.getParent());

                if (!Files.exists(manifestOut)) {
                    // If no manifest exists yet, copy the generated one directly
                    Files.copy(new java.io.ByteArrayInputStream(genManifest.getBytes(StandardCharsets.UTF_8)), manifestOut);
                    Frames.LOGGER.atInfo().log("Copied manifest_generated.json -> " + manifestOut);
                    return;
                }

                // Manifest exists: update ServerVersion if present in generated manifest
                if (genServerVersion == null) {
                    Frames.LOGGER.atInfo().log("No ServerVersion found in manifest_generated.json; leaving existing manifest unchanged");
                    return;
                }

                String existing = Files.readString(manifestOut, StandardCharsets.UTF_8);

                Matcher existMatcher = svPattern.matcher(existing);
                String updated;
                if (existMatcher.find()) {
                    // Replace existing ServerVersion value
                    updated = existing.replaceAll("\"ServerVersion\"\\s*:\\s*\"[^\"]*\"", "\"ServerVersion\": \"" + genServerVersion + "\"");
                } else {
                    // Insert ServerVersion before the Dependencies block if possible
                    int depsIdx = existing.indexOf("\"Dependencies\"");
                    if (depsIdx != -1) {
                        String prefix = existing.substring(0, depsIdx);
                        String suffix = existing.substring(depsIdx);
                        // Ensure prefix ends with a newline and proper indentation
                        String insert = "    \"ServerVersion\": \"" + genServerVersion + "\",\n";
                        updated = prefix + insert + suffix;
                    } else {
                        // Fallback: append ServerVersion near the top after the Version field
                        int verIdx = existing.indexOf("\"Version\"");
                        if (verIdx != -1) {
                            // Find end of that line
                            int lineEnd = existing.indexOf('\n', verIdx);
                            if (lineEnd == -1) lineEnd = verIdx + 0;
                            String prefix = existing.substring(0, lineEnd + 1);
                            String suffix = existing.substring(lineEnd + 1);
                            String insert = "    \"ServerVersion\": \"" + genServerVersion + "\",\n";
                            updated = prefix + insert + suffix;
                        } else {
                            // Last resort: append before closing brace
                            int brace = existing.lastIndexOf('}');
                            if (brace == -1) brace = existing.length();
                            String prefix = existing.substring(0, brace);
                            String suffix = existing.substring(brace);
                            String insert = "\n    \"ServerVersion\": \"" + genServerVersion + "\"\n";
                            // Add a trailing comma if necessary
                            if (prefix.trim().endsWith(",")) {
                                updated = prefix + insert + suffix;
                            } else {
                                updated = prefix + "," + insert + suffix;
                            }
                        }
                    }
                }

                Files.writeString(manifestOut, updated, StandardCharsets.UTF_8);
                Frames.LOGGER.atInfo().log("Updated ServerVersion in " + manifestOut + " -> " + genServerVersion);
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
