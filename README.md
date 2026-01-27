
# Boffmedia Frames — Dynamic Full-Resolution Picture Frames for Hytale

![Boffmedia Frames image](https://i.imgur.com/biPaJ1x.jpeg)

## Overview

Boffmedia Frames lets players display images from the internet on in-world frames. It now supports full-resolution image rendering (saved at native pixel dimensions and scaled to block sizes), per-instance metadata, and a global index for fast coordinate lookups.

Key features:
- Full-resolution image rendering and pixel-preserving scaling to requested block sizes.
- Automatic asset generation: textures, block models, and server item JSON are created when importing an image.
- Per-item metadata files: every generated item has `mods/BoffmediaFrames/Frames/<itemId>.json` containing a `frames` array of placed instances.
- Global index: `mods/BoffmediaFrames/FramesIndex.json` maps item ids to instances for quick coord → item resolution and UI prefill.
- Admin tools: `/listframes` shows generated frames with a COPY button (copies item id to chat) and delete functionality that removes associated assets and metadata.
- In-UI controls: simplified frame UI with Upload, Apply (by item id or state key), and Remove (drops a normal 1x1 frame and clears metadata for that coordinate).

## How it works (summary)

1. Player interacts with a frame and opens the Picture Frame UI.
2. Upload a URL or paste an existing `Boff_Frame_<Name>` id and use Apply.
3. For uploads: the mod downloads the image, creates a PNG texture, a matching blockymodel and an item JSON, writes per-item metadata and updates the global index.
4. The mod then replaces the block (after a short delay) so the new item/state appears in-world.
5. When applying an existing generated item, the mod removes any prior instance registered at the same coordinates (both from the index and the referenced per-item metadata) and writes the new instance — ensuring a single authoritative mapping per coordinate.

## File layout (runtime)

- `mods/BoffmediaFrames/Frames/<itemId>.json` — per-generated-item metadata (contains `frames` array of instances).
- `mods/BoffmediaFrames/FramesIndex.json` — global index for coord → item lookups.
- `mods/BoffmediaFrames/Common/Blocks/Frames/Images/` — saved PNG textures.
- `mods/BoffmediaFrames/Common/Blocks/Frames/*.blockymodel` — generated models.
- `mods/BoffmediaFrames/Server/Item/Items/Furniture/Frames/Boff_Frame_<Name>.json` — generated server item JSON.

## Admin & UI notes

- `/listframes` opens an admin UI listing generated items. Use COPY to paste the `Boff_Frame_<Name>` id into chat for easy reuse.
- Delete removes the item's metadata, generated item JSON, blockymodel, texture and removes any state definitions referencing the id.
- The Picture Frame UI now includes a Remove button which replaces the frame with a normal `Boff_Frame_1x1` and clears metadata/index entries for that coordinate.

## Development notes

- The plugin writes asset files into `mods/BoffmediaFrames/` so the server/client will load them; a server restart/save may be required on first use for assets to become available.
- Metadata is stored as pretty-printed JSON using `org.bson.BsonDocument` helpers to keep files human-readable.
- The codebase includes helpers in `FileHelper.java` for image download, model/item generation, metadata writes, and index maintenance.

## Troubleshooting

- If a generated asset does not appear immediately, ensure the world chunk is loaded and restart or save the server to force asset reload.
- If coordinates are duplicated, the Apply flow removes prior instances at the same coords before registering a new one.

## Contributing

See the source repository for implementation details and feel free to open issues or PRs for improvements (e.g., client-side caching, index deduping, or UX refinements).

---

For quick reference, see `src/main/java/es/boffmedia/frames/FileHelper.java` for metadata/index behavior and `src/main/java/es/boffmedia/frames/ui/ImageDownloadPage.java` for the in-game UI flow.
