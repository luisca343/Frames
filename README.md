
# Boffmedia Frames — Adding Internet Images to Hytale

## Overview

This project enables downloading images from arbitrary URLs, converting them into Hytale-compatible textures, injecting them as new block states in the frame asset JSON, and applying those states to in-world frame blocks via UI or commands.


## Core flow (technical)

1. Request: the UI triggers an image import (URL or upload).
2. Download: the plugin downloads the image bytes from the provided URL.
3. Decode & Resize: the image is decoded and resized (nearest-neighbor) to the frame texture size (32 px per frame unit; scaled by frame dimensions).
4. Save texture: the processed PNG is written into the mod textures directory (under `mods/BoffmediaFrames/` or project `resources` depending on runtime).
5. JSON injection: a new `State.Definitions` entry is inserted into the size-specific frame JSON (the mod's asset manifest) and the JSON is pretty-printed and saved.
6. World refresh: after saving the file, the game automatically reloads the assets, effectively making the new texture/state available.

## Datapack generation

- The plugin programmatically constructs a Hytale datapack (asset folder + manifest) during the image-import flow. This datapack contains the new texture files and updated asset JSON files (`State.Definitions`) for the correct frame sizes, and is written into the `mods/BoffmediaFrames/` (or the server's mod datapack location) so the game can reload and make the new states available without manual editing. 
- This is both the reason the mod works, and the reason for the players to be required to reboot the server after installing the mod, as the first load creates the initial datapack structure, and unfortunately Hytale does not pick it up until a restart. (Workaround ideas welcome!)

## Image processing details

- Download: image fetching handles standard HTTP(S) and reads raw image bytes into a BufferedImage (or equivalent).
- Resize: nearest-neighbor scaling is used to avoid color blending artifacts and preserve pixel art fidelity.
- Format: saved as PNG with an appropriate filename and Hytale asset-friendly naming (capitalized first letter when required).

## JSON & asset injection

- Assets live in size-specific frame JSON files (e.g., `Boff_Frame_1x1.json`, `Boff_Frame_2x2.json`).
- The plugin generates a unique state key and corresponding `Texture` entry, then inserts a new state under `State.Definitions` for the correct size.
- The JSON is written back using a pretty-print routine so it remains human-readable and diff-friendly.

## Applying frames in-world

- The in-world interaction code validates that the chunk is loaded and then sets the block's type/state and rotation accordingly.
- If chunk saving or block update fails, the plugin logs an error and returns a user-friendly message in the UI.

## Naming conventions & sizing

- Texture filenames are generated to be unique and conform to asset naming expectations.
- Frame size keys are derived from the block or explicitly chosen in the UI (e.g., `Boff_Frame_1x1`, `Boff_Frame_2x2`). Ensure the state is added to the correct size file.

---

This README provides a technical overview of the Boffmedia Frames mod for Hytale, detailing its functionality, image processing, JSON injection, and usage instructions. For further development or contributions, please refer to the source code and associated documentation.