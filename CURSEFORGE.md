# Frames — Dynamic Picture Frames for Hytale

![Mod Showcase 1](https://i.imgur.com/biPaJ1x.jpeg)

Frames adds in-game picture frames that can display full-resolution images from any URL. No client-side modding required — upload or apply images from the in-game UI and they render at the exact pixel resolution you supply (scaled to the chosen block size).

## Highlights

*   Full-resolution image rendering: supports saving and displaying images at their native pixel dimensions, then scales to the requested block dimensions while preserving aspect ratio.
*   Dynamic assets: when you upload an image the mod generates a dedicated item, block model, and texture automatically.
*   Frame sizes: You can choose the pictures to be any size you want! You just need to specify a width (in blocks) and the height will scale accordingly.
*   Per-instance metadata: every placed frame instance is recorded in per-item metadata (`mods/BoffmediaFrames/Frames/<itemId>.json`) with coordinates and block sizing.
*   Global index: fast coord → item lookup via `mods/BoffmediaFrames/FramesIndex.json` for quick UI prefill and admin tasks.
*   Admin UI & tools: `/listframes` admin command with a list UI, and options to delete generated assets cleanly.

![BIG IMAGE](https://i.imgur.com/mG8kQpz.jpeg)

## How it works

1.  Place a frame and interact with it.
2.  Paste an image URL into the in-game UI and `Upload`, or paste an existing generated `Boff_Frame_<Name>` id and `Apply`.
3.  The mod downloads the image, creates a texture, blockymodel and an item JSON, writes per-item metadata, updates the global index, and (after a short delay) replaces the block so the new item appears in-world.

## Installation

1.  Place the mod in your `mods` folder.
2.  Start the server and allow the mod to copy its default assets into `mods/BoffmediaFrames` (a server restart or a world save may be required on first run).

## Requirements

*   Server restart/save: on first install you may need to restart or save the server once so the mod can initialize and copy its default assets.
*   Experimental: this mod is experimental and leverages the server's asset system — reload timing can affect when generated assets are available for immediate use.

## Permissions

To allow a player to upload images (create new frame items) grant the following permission to the player account:

```
/perm user add [UUID] boffmedia.frames.upload
```

## Alignment

The Picture Frame UI supports alignment options that control how the image is placed within the frame when the image and frame block size do not match exactly. Available alignment values are: `CENTERED`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`, `TOP_LEFT`, and `TOP_RIGHT`.

## Credits & Source

Source and technical details: [https://github.com/luisca343/Frames](https://github.com/luisca343/Frames)