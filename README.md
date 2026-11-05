# Bloom Client (Stage 1)

Fabric client mod for Minecraft 1.21.11.

## Build it (GitHub, like your other mods)
1. Make a new GitHub repo (for example `bloom-client`).
2. Upload everything in this folder to it (keep the `.github` folder, `src`, `build.gradle`, `gradle.properties`, `settings.gradle`).
3. Open the **Actions** tab. The `build` workflow runs on its own. When it is green, open the run and download the **bloom-client** artifact. The jar is inside.
4. Put the jar in your mods folder (Fabric API is required).

If the build fails while *resolving* `loom_version`, `loader_version` or `fabric_version`, open the working `gradle.properties` from your auctiontracker repo and copy those three lines here.
If it fails with a Java error, copy the red error lines and send them over; they point at the exact line.

## Using it
* **Right Shift** opens the Bloom menu (rebind it in Controls > Bloom Client).
* **Modules** tab: toggle modules, click a module to open its settings. **HUD Editor** lets you drag modules around.
* **Theme** tab: accent colors, **Glass** (first swatch: clear and bubbly, applies to the whole menu and HUD), hex box, favorites, corner roundness.
* **Lyrics** tab: put `song.lrc` (timed lyrics) and optionally `song.wav` in `.minecraft/config/bloom/songs/`, pick the song, press Play, and turn on **Lyrics in the Air** (or use the toggle in that tab).

## Modules in this stage
Watermark, FPS, CPS, Keystrokes, Coordinates, Direction, Clock, Playtime, Memory, Speed, Ping, Server, Held Item, Potion Status, Armor Status, Toggle Sprint, Lyrics in the Air.

## Not in this stage yet
Main menu with the walking character, the aurora sky, glowing glass player outlines, Spotify connection, in-world 3D lyrics.
