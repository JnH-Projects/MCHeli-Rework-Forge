# MCHeli Rework

<img src="docs/images/mcheli-rework-icon.png" alt="MCHeli Rework icon" width="160">

Helicopters, planes, tanks and ground vehicles for **Minecraft 26.3**, **26.2** and **1.21.1**, on
**Forge**, **NeoForge** and **Fabric**.

MCHeli Rework is a ground-up port of MCHeli 1.0.3, a mod that stopped at Minecraft 1.7.10. Every
system is rewritten for the modern game, while the original vehicle definitions, models,
textures and sounds load unchanged.

> **Status:** on the way to 1.1.0, now at phase 12 (`1.1.0-beta.8.1`). Every helicopter, plane,
> tank, car and ground vehicle can be placed, crewed and driven with the original handling, all
> 229 weapons fire, and the scripted HUDs, fuel, the vehicle screen and all of the original's
> equipment are in, down to UAV stations, the drafting table and the team manager. The vehicle
> workshop builds new vehicles in game from a Blockbench or Blender model, with their own textures
> and sounds, and sends them to a server for its operators to approve. Betas ship the
> Forge 26.3 jar only; 1.1.0 ships on Forge and Fabric for every version from 1.19.4 to 26.3, and
> on NeoForge from 1.20.6 (see [supported versions](docs/ROADMAP.md#supported-versions)). See the
> [roadmap](docs/ROADMAP.md).

## Showcase video

Helicopters, jets and tanks of `1.0.0-alpha.3` in action, on YouTube:

[![MCHeli is back! Helicopters, Jets & Tanks on Minecraft 26.3 (Forge) | MCHeli Rework alpha.3](https://img.youtube.com/vi/IFQBb0iH9dc/hqdefault.jpg)](https://youtu.be/IFQBb0iH9dc)

Phase 4 clips (click a picture to open the video):

| | |
|---|---|
| [![The AC-130 in gunner mode, circling on autopilot](https://img.youtube.com/vi/Uc0Unk2VQRA/hqdefault.jpg)](https://youtu.be/Uc0Unk2VQRA) | [![A helicopter shot down by an SA-2 going up in the crash fireball](https://img.youtube.com/vi/7h6uk0HqH1s/hqdefault.jpg)](https://youtu.be/7h6uk0HqH1s) |
| AC-130 gunner mode: the autopilot circles while the pilot works the guns | S-75 lock-on: an SA-2 brings down a helicopter, which goes up in the crash fireball |

## Screenshots

The vehicle workshop in a development build (`1.1.0-beta.8`): bring a model
from Blockbench or Blender, place its parts on it, tune it and fly it, then give it textures and
sounds of its own. Every earlier phase is in the [screenshot gallery](docs/SCREENSHOTS.md).

| | |
|---|---|
| ![The Paint tab: the model's parts listed on the left, the helicopter in the middle with blue rotor blades, a red cockpit panel and a green hull, and the blade texture previewed on the right](docs/images/phase-12/workshop-paint.png) | ![The Sound tab: Engine, Rotor and a weapon listed on the left, and on the right the engine's sound file, Play and Clear buttons, its volume, and a pitch curve with five points](docs/images/phase-12/workshop-sound.png) |
| A PNG per part, showing on the model straight away | An OGG for the engine, with its pitch following the throttle along a curve |
| ![The workshop editor: a small helicopter on a grid, a hit box outlined around its cockpit, coloured handles on its parts, and the main rotor selected with arrows, its axis and its values in the side panel](docs/images/phase-11/workshop-editor.png) | ![The tuning page beside the helicopter, listing hit points, top speed, gravity, turn rates, fuel and armor](docs/images/phase-11/workshop-tuning.png) |
| Place every part by dragging it on the model | Tune how it flies; values you changed stand out from the defaults |
| ![The workshop's first screen: vehicles in the packs on the left, and a form for a new vehicle with a dropped OBJ file on the right](docs/images/phase-11/workshop-import.png) | ![The view from the helicopter's cockpit in flight over a desert coast, with the instruments on the HUD](docs/images/phase-11/workshop-test-flight.png) |
| Drop an OBJ on the window to start a vehicle | Test flight straight from the editor |
| ![The pack page with the vehicle's name, author, version and description, and a message that the pack was exported to a zip](docs/images/phase-11/workshop-pack.png) | ![The finished helicopter parked on a red desert hill, a CH-47 flying past behind it](docs/images/phase-11/workshop-finished-vehicle.png) |
| Name the pack and export a zip to share | The result is an ordinary content pack |

## Credits

MCHeli was created by **EMB4**, a Japanese mod author who first published it on the
[minecraftuser.jp forum](http://forum.minecraftuser.jp/viewtopic.php?f=13&t=14837). Its English
home is the CurseForge project
[MCHeli Minecraft Helicopter Mod](https://www.curseforge.com/minecraft/mc-mods/mcheli-minecraft-helicopter-mod)
(project 224304), maintained by [**Fureniku**](https://www.curseforge.com/members/fureniku), with
more than 1.5 million downloads. The last release, 1.0.3, came out for Minecraft 1.7.10 and the
project page states it will not be updated further.

All vehicle content (definitions, models, textures, sounds and HUD scripts) is EMB4's work. The
mod does not ship it: on first start it converts the player's own copy of the original 1.0.3 zip
to the modern resource layout. This repository holds none of it either; the unit tests import
the same zip.

The rework is by **JayMar921** ([JnH Projects](https://github.com/JnH-Projects)).

## Licensing note

The original mod is published as **All Rights Reserved**, and its CurseForge page states that EMB4
gave redistribution permission to Fureniku only. The released jar therefore contains none of the
original files: only the new Java code and the rework's own translations. Players download the
original zip from the CurseForge page themselves, and the mod converts it on their machine.

This repository holds no original files either; the unit tests import the developer's own copy
of the zip.

The rework's own code is under the [MCHeli Rework Licence](LICENSE): play it anywhere, include
official releases in free modpacks, read and build the source, and send pull requests.
Redistributing the source, modified builds or forks, and selling the mod need written
permission. Content packs you make are yours. Because the engine is a rework of EMB4's All Rights
Reserved original, the code is source available rather than open source.

## Requirements

| | Minecraft 26.3 | Minecraft 26.2 | Minecraft 1.21.1 |
|---|---|---|---|
| Forge | 66.1.0 or newer in the 66.x line | 65.1.5 or newer in the 65.x line | 52.1.0 or newer |
| NeoForge | 26.3.0.99 or newer | 26.2.0.93 or newer | 21.1.200 or newer |
| Fabric | Loader 0.20 and Fabric API | Loader 0.20 and Fabric API | Loader 0.16 and Fabric API |
| Java | 25 (bundled with the launcher) | 25 (bundled with the launcher) | 21 (bundled with the launcher) |

## Installing

1. Install Forge, NeoForge or Fabric for Minecraft 26.3, 26.2 or 1.21.1. On Fabric, also install
   [Fabric API](https://modrinth.com/mod/fabric-api).
2. Download the jar for your loader from the
   [releases](https://github.com/JnH-Projects/MCHeli-Rework-Forge/releases):
   `mcheli-<loader>-<minecraft>-<version>.jar`, such as `mcheli-fabric-1.21.1-<version>.jar`.
3. Drop it into the `mods` folder of that profile.
4. Download `MC1.7.10_mcheli_1.0.3.zip` from the
   [MCHeli CurseForge page](https://www.curseforge.com/minecraft/mc-mods/mcheli-minecraft-helicopter-mod/files)
   and put it, still zipped, into a folder called `mcheli` next to `mods` (create it if needed):

   ```
   <game directory>/
     mods/mcheli-<loader>-<minecraft>-<version>.jar
     mcheli/MC1.7.10_mcheli_1.0.3.zip
   ```

On the next start the mod converts the zip into `mcheli/imported`, which takes a few seconds once.
That folder is loaded as a resource pack and a data pack (listed as "MCHeli Content") and can't
be switched off. The zip can be deleted afterwards. Dedicated servers need the zip in the same
place. Without it the game still starts, but there are no vehicles, and the chat says where the
zip goes.

Use a separate game directory for each loader and version. Mods built for other loaders or older
versions in a shared `mods` folder will stop the game from starting. Worlds and the `mcheli`
folder can move between loaders on the same Minecraft version: the imported content, content packs
and configs are the same files on all three. Do not open a world in an older Minecraft version than
the one that last saved it.

## Playing

Take a vehicle from one of the MCHeli creative tabs, right-click a block to place it and
right-click the vehicle to board; in survival, craft it with the original's recipe. W and S work
the throttle, the mouse flies or aims, right mouse fires and Shift gets out. Every key can be
rebound under Controls, category MCHeli.

The full controls for each kind of vehicle, fuel and repairs, the equipment (parachute, smoke
grenades, GLTD, rangefinder, Stinger and Javelin, container, UAV station, drafting table, tow
chain), troops, the team manager and the commands and server settings are in
[docs/PLAYING.md](docs/PLAYING.md).

## Content packs

Content packs add vehicles and weapons: drop a pack (a zip or a folder) into
`<game dir>/mcheli/packs`. On a server only the server needs it; players download the server's
packs automatically when they join. [examples/rework-example-pack](examples/rework-example-pack)
adds a small helicopter, the Rework Scout. To make your own, use the
[workshop](docs/content/workshop.md) in game, or write one by hand with
[docs/content/packs.md](docs/content/packs.md).

## Documentation

* [Playing](docs/PLAYING.md): controls, equipment, commands and server settings
* [Screenshots](docs/SCREENSHOTS.md): every phase in pictures
* [Changelog](CHANGELOG.md)
* [Roadmap](docs/ROADMAP.md): what each phase delivered, and what is planned for 1.1.0
* [Architecture](docs/ARCHITECTURE.md): how the mod is put together
* [Development](docs/DEVELOPMENT.md): building, running, testing and releasing
* [In-game test plan](docs/TESTING.md)
* Content authoring: [the workshop](docs/content/workshop.md), [content packs](docs/content/packs.md), [modeling in Blockbench or Blender](docs/content/modeling.md), [vehicles](docs/content/aircraft.md), [weapons](docs/content/weapons.md), [HUD scripts](docs/content/hud.md)
* [Contributing](CONTRIBUTING.md): rules for pull requests and bug reports

## Building from source

```
./gradlew build
```

The jars, one per loader and Minecraft version, end up in `build/release`. Java 25 is required;
Gradle downloads one automatically if it is not installed. `./gradlew :<loader>-26.3:runClient`
and `:<loader>-26.3:runServer` (`forge`, `neoforge` or `fabric`) import the content from
`Mod/MC1.7.10_mcheli_1.0.3.zip`, which you have to download yourself. See
[DEVELOPMENT.md](docs/DEVELOPMENT.md) for details.
