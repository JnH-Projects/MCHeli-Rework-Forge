# Roadmap

The original mod is about 60,000 lines of 1.7.10 code. Nearly all of it touches APIs that no
longer exist (immediate mode OpenGL, numeric ids, the old network and event systems), so the port
happened in four phases toward 1.0.0, and 1.1.0 continues with six more. Each phase ends with a
tagged pre-release that installs and runs on its own.

| Phase | Version | Theme | State |
|-------|---------|-------|-------|
| 1 | `1.0.0-alpha.1` | Foundation and flyable helicopters | Done, tested in game |
| 2 | `1.0.0-alpha.2` | Planes, tanks and ground vehicles | Implemented, in-game test in progress |
| 3 | `1.0.0-alpha.3` | Weapons and combat | Implemented, in-game test in progress |
| 4 | `1.0.0-beta.1` | HUD and polish | Done |
| | `1.0.0` | Release: code-only jar | Released |
| 5-11 | `1.1.0-beta.1` to `beta.7` | Content packs, NeoForge and Fabric, 1.21.1, equipment, workshop, 1.20.1 ([plan](#road-to-110)) | In progress |
| | `1.1.0` | Release on CurseForge | Planned |

## Phase 1: foundation and flyable helicopters

- [x] Gradle project for Forge 66.1 / Minecraft 26.3, CI and tag based releases
- [x] Asset migration to the 26.x layout (textures, sounds, lang, item models)
- [x] Definition parser with the original keys, defaults and ranges
- [x] MQO and OBJ model loaders
- [x] Helicopter entity, flight model, hover mode
- [x] Hull and rotor rendering
- [x] Pilot controls, camera roll, free look, multi-seat boarding
- [x] English translation of the content guides and comments
- [x] In-game test on Minecraft 26.3 with Forge 66.1.0 ([screenshots](../README.md#screenshots))

Known gaps carried into later phases: engine sound, destroyed state and explosion, blade
folding, fuel use, and animated parts other than rotors.

## Phase 2: planes, tanks and ground vehicles

Ports of `MCP_EntityPlane`, `MCH_EntityTank` and `MCH_EntityVehicle` with their definition keys.

- [x] Plane flight model, VTOL, folding and swept wings, nozzles, ejection seats
- [x] Tank and car physics: tracks, road wheels, steering wheels, terrain following, turret
- [x] Ground vehicles (static guns and launchers) with `AddPart` / `AddChildPart`
- [x] Gunner seats, fixed rotation seats, camera positions and switching
- [x] Animated parts for every kind: hatches, canopies, landing gear, weapon bays, throttle, rotation
- [x] Engine sound loop driven by throttle (`Sound`, `SoundRange`, `SoundVolume`, `SoundPitch`)
- [x] Racks for vehicles (`AddRack`, `RideRack`, `ExclusionSeat`)
- [ ] In-game test on Minecraft 26.3 with Forge 66.1.0 ([test plan](TESTING.md#phase-2-checklist)); first pass
      found solid glass and boats floating too low, both fixed ([screenshots](../README.md#screenshots))

Carried into later phases: weapon bays open only once weapons can be selected (phase 3);
containers on racks, parachutes for ejection and rack drops, repelling and troop drops come with
the equipment (phase 4), as do `HideEntity`, search lights, splash and exhaust particles, blade
folding and the pilot gunner mode.

## Phase 3: weapons and combat

Ports of the `weapon` and `flare` packages, about 60 classes.

- [x] Weapon definition loader for all 229 bundled weapons
- [x] Projectiles: machine guns, rockets, bombs, torpedoes, AA/AS/AT/TV missiles, CAS, marker rockets
- [x] Lock-on, proximity fuses, cluster munitions, dispensers, targeting pods (spotting only)
- [x] Damage model: armor min/max and factor, per-class multipliers, extra hit boxes, player damage factor
- [x] Destroyed state, wreck smoke and explosion
- [x] Flares and smoke dischargers
- [x] Ammunition, reloading, heat, weapon groups and resupply from the inventory
- [x] Muzzle flash, cartridges (as particles), trajectory particles, weapon sounds
- [x] Weapon parts: aim, recoil, spinning barrels, missiles hidden while reloading
- [x] Weapon bays opening while their weapon is selected
- [x] Riders tilt with the vehicle (fix carried over from the phase 2 test)
- [ ] In-game test on Minecraft 26.3 with Forge 66.1.0 ([test plan](TESTING.md#phase-3-checklist))

Carried into phase 4: the pilot gunner mode (`EnableGunnerMode`, `ConcurrentGunnerMode`), marking
blocks with targeting pods, mortar distance and zoom (they belong to the HUD), resupply from the
inventory screen and from supply vehicles, and the 3D cartridge model.

## Phase 4: HUD, equipment and polish

Fixed from the alpha.3 test: cars that could not steer (`PivotTurnThrottle`), the view tilting
with the hull in every seat (it also banked the wrong way), and panels flickering between two
textures (back faces are culled again). See the [test plan](TESTING.md#phase-4-checklist).

- [x] HUD script engine: the `hud` package and the subset of the `eval` expression library the
      scripts use, drawing all 19 bundled HUDs (radar blips included)
- [x] Pilot gunner mode (`EnableGunnerMode`, `ConcurrentGunnerMode`): planes circle on autopilot,
      helicopters hover, while the pilot mans the weapons
- [x] Crafting recipes from `AddRecipe`, with 1.7.10 damage values mapped to flattened items, in
      the recipe book from the first login
- [x] Code-only jar: the original content is imported from the player's own copy of the 1.0.3 zip

The rest of the original phase 4 list moved to 1.1.0 (phase 8 below), so 1.0.0 can ship.

## 1.0.0 release criteria

- Every bundled helicopter, plane, tank, vehicle and weapon loads without warnings
- Each vehicle kind has been flown or driven in single player and on a dedicated server
- No known crash, data loss or desync issue
- No original MCHeli file in the jar or the repository; players import their own copy (done)

## Road to 1.1.0

1.1.0 adds three big things on top of 1.0.0: content packs, more Minecraft versions and loaders,
and the vehicle workshop. It also picks up the phase 4 items that were left out of 1.0.0. The work is
split into seven phases, each ending in a beta.

### Release channels

| Build | Private repo | Public repo | CurseForge |
|-------|--------------|-------------|------------|
| `1.1.0-beta.N` | GitHub pre-release | Mirrored, GitHub pre-release | Not uploaded |
| `1.1.0-rc.N` (if needed) | GitHub pre-release | Mirrored, GitHub pre-release | Not uploaded |
| `1.1.0` | GitHub release | Mirrored, GitHub release | Uploaded, one file per Minecraft version |

Every beta goes through the usual flow: set `mod_version`, move the changelog section, merge,
tag `v1.1.0-beta.N` in the private repo, mirror the commits to the public repo and tag the same
content there. The hyphen in the version already marks both GitHub releases as pre-releases. The
CurseForge page stays on 1.0.0 until 1.1.0 ships.

Betas are still public: anyone can download the jar from the
[public repository's releases](https://github.com/JnH-Projects/MCHeli-Rework-Forge/releases), and
the CurseForge description links there for players who want to test early. Treat every beta as
something players will install and keep worlds on, not as an internal build.

Rules that hold for every beta:

- One phase at a time. Once a phase starts, it is finished and its beta jar is shipped (tagged,
  released on GitHub in both repositories) before any work on the next phase begins. Bugs found
  in a shipped beta are fixed in the current phase, not by reopening the old one.
- A world saved by 1.0.0 or an earlier beta loads in the next one. Breaking a save format needs
  a data fixer or a migration, and a line under `Changed` in the changelog.
- The jar still holds no EMB4 file, on every Minecraft version.
- A phase is done when the build is green for every module it builds, its checklist
  in [TESTING.md](TESTING.md) passes in single player and on a dedicated server, and
  `CHANGELOG.md`, the docs and [ARCHITECTURE.md](ARCHITECTURE.md) describe what changed.

| Phase | Version | Theme | State |
|-------|---------|-------|-------|
| 5 | `1.1.0-beta.1` | Groundwork: runtime registry, config, multi-version build | Released |
| 6 | `1.1.0-beta.2` | Content packs and multiplayer sync | Released |
| 7 | `1.1.0-beta.3` | Loaders (Forge, NeoForge, Fabric) and 1.21.1 | Released |
| 8 | `1.1.0-beta.4` | Equipment and the rest of phase 4 (Forge 26.3 only) | In progress |
| 9 | `1.1.0-beta.5` | Workshop: model, parts and tuning (Forge 26.3 only) | Planned |
| 10 | `1.1.0-beta.6` | Workshop: textures, sounds and sharing (Forge 26.3 only) | Planned |
| 11 | `1.1.0-beta.7` | Every loader, 1.21.1 and 1.20.1 | Planned |
| | `1.1.0` | Release on CurseForge | Planned |

The order is deliberate. The registry and the source split come first because everything else
sits on them. Content packs come before the workshop, since the workshop's output is a pack.
The loader split and 1.21.1 come right after packs, while the code is still close to 1.0.0;
from then on each feature is built once in shared code and only its Minecraft glue is written per
version and loader. Betas 4 to 6 build only the Forge 26.3 jar, since six modules on three
toolchains made every build slow; phase 11 writes the glue for the rest, adds 1.20.1 and ships
every jar again.

### Supported versions

MCHeli stopped at Minecraft 1.7.10. 1.1.0 is the bridge back: it runs on the versions most
players and modpacks are on today, so anyone can pick the mod up again, and then follows the
latest Minecraft.

| Minecraft | Also runs on | Java | Forge | NeoForge | Fabric |
|-----------|--------------|------|-------|----------|--------|
| 26.3 | | 25 | yes | yes | yes |
| 1.21.1 | 1.21 | 21 | yes | yes | yes |
| 1.20.1 | 1.20 | 17 | yes | the Forge jar (NeoForge 1.20.1 is a Forge fork) | yes |

Each major version gets its most played minor rather than its last one (1.20.6 and 1.21.11 have
few players). Nothing below 1.20.

**After 1.1.0, the 1.20.1 and 1.21.1 builds are frozen.** They get no further fixes or features;
development continues on the latest Minecraft version only, and players are expected to move
up with it.

## Phase 5: groundwork (`1.1.0-beta.1`)

Engine changes that packs, ports and the workshop all depend on. Players mostly see the config
and the command.

- [x] One generic vehicle item per kind, carrying the definition id in a data component, with a
      migration so 1.0.0 worlds keep their items ([why](#one-generic-vehicle-item-per-kind)): the
      old item ids stay registered and convert themselves in a player's inventory
- [x] `ContentRegistry` can add and replace definitions while the game runs
- [x] Runtime client assets: textures through `DynamicTexture`, sounds through a streaming
      `SoundInstance`, models through `MeshCache`, all without a resource reload
- [x] In-game definition reload, built on the runtime registry
- [x] Server config (`mcheli.cfg` equivalent) and the `/mcheli` command
- [x] Source split: a `common` source set with the version-free code (`content`, `model`, `hud`,
      the expression library, weapon and vehicle definitions, flight and damage maths) and one
      platform module per Minecraft version for rendering, networking, registration and events.
      Minecraft classes stay out of `common`, enforced by a test.
- [x] Multi-version Gradle build: one tag builds every platform module, and the release attaches
      one jar per Minecraft version (`mcheli-<mc>-<version>.jar`). Release workflow and
      [DEVELOPMENT.md](DEVELOPMENT.md#releasing) updated; 26.3 is still the only module.

## Phase 6: content packs and multiplayer sync (`1.1.0-beta.2`)

The external content packs from phase 4, in the package format the workshop will write later
([format](#vehicle-packages)).

- [x] Package format: definition, OBJ or MQO model, PNG textures, OGG sounds and a manifest (id,
      author, version, content hash). Data only, never code.
- [x] Packs loaded from `<game dir>/mcheli/packs`; the 1.0.3 import becomes one more pack
- [x] Load errors reported per pack (log, and chat for operators); a broken pack never stops the
      game from starting
- [x] Server to client sync: id and hash list on join, missing packs sent in chunks and cached in
      `mcheli/cache/<hash>.zip` ([details](#multiplayer))
- [x] Server limits and an allow list in the config; clients cap the cache size
- [x] Pack author guide in `docs/content/packs.md`, with a small example pack in the repo made
      from our own assets (no EMB4 files)
- [x] Night glow: tracers, rocket motors, flares and explosions glow, faint by day and bright at
      night; a crash keeps burning with a glow and the wreck burns for 30 seconds. Client config `effects.nightGlow` turns it off.

## Phase 7: loaders and 1.21.1 (`1.1.0-beta.3`)

1.1.0 runs on three Minecraft versions, each on three loaders (see
[supported versions](#supported-versions)). This phase splits the code so the loaders share
almost everything, adds NeoForge and Fabric on 26.3, then brings all three to 1.21.1.

- [x] Three layers: `common` (pure Java, as now), shared code per Minecraft version (rendering,
      entities, sounds, HUD, which are the same on every loader), and a thin module per loader
      (registration, events, networking, config)
- [x] NeoForge and Fabric on 26.3, with the same features and checklists as Forge
- [x] 1.21.1 (Java 21) on Forge, NeoForge and Fabric: older rendering (no render states), older
      networking, item models from the item JSON instead of `item_model`
- [ ] Content packs and their sync work across loaders on the same Minecraft version (a Fabric
      client on a NeoForge server)
- [x] Legacy importer checked on 1.21.1 (pack format numbers and the asset layout differ)
- [x] Jar names carry the loader and the Minecraft version, such as
      `mcheli-fabric-1.21.1-1.1.0-beta.3.jar`; the release workflow publishes every jar
- [x] CI builds and tests every module on every pull request
- [x] [TESTING.md](TESTING.md) gets a short smoke test per jar: import, fly a helicopter, drive a
      tank, fire a missile, join a dedicated server

## Phase 8: equipment and the rest of phase 4 (`1.1.0-beta.4`)

Gameplay carried over from phase 4, built once in shared code with per-loader glue. Built and
tested on Forge 26.3 only; the other loaders and versions get it in phase 11.

- [x] Glows under Iris shaders: the custom glow pipelines are handed to Iris, so tracers, motors,
      flares and explosions no longer streak to the horizon with a shader pack on
- [x] Crash spin for shot down aircraft (moved from phase 6): planes and jets roll into a
      nose-down spiral instead of just falling, and helicopters spin faster with some wobble. The
      spin direction comes from the entity id so every client sees the same thing.
- [x] Night vision and zoom
- [ ] Thermal view
- [x] Fuel use, fuel trucks, the fuel can, the wrench and the parachute
- [ ] The vehicle inventory, with its fuel slots
- [ ] Equipment: GLTD, rangefinder, UAV station and portable controller, chain towing, container,
      Stinger and Javelin, smoke grenades
- [x] Parachutes for ejection seats
- [ ] Parachutes for rack drops, containers on racks, repelling and troop drops
- [ ] `HideEntity`, search lights, splash and exhaust particles, helicopter blade folding
- [ ] Resupply from the inventory screen and from supply vehicles, marking blocks with targeting
      pods, mortar distance
- [ ] The drafting table and its recipe browser (pack vehicles included)
- [ ] Multiplayer team manager and spotting

## Phase 9: workshop, model, parts and tuning (`1.1.0-beta.5`)

The first half of the [editor](#the-editor). Its output is a pack from phase 6, so anything
built here syncs to other players with no extra work.

- [ ] Workshop block (or the drafting table) opening a 3D editor screen around the model
- [ ] OBJ import from Blockbench or Blender, with clear errors for unsupported files
- [ ] Gizmos for seats, cameras, rotors, wings, flaps, nozzles, wheels, tracks, landing gear,
      canopy and hatches, weapon mounts, hit boxes, engine and fuel tank
- [ ] Flight and drive tuning with a test flight from the editor
- [ ] Save, export and load packages; undo and redo
- [ ] Workshop guide in `docs/content/workshop.md`

## Phase 10: workshop, textures, sounds and sharing (`1.1.0-beta.6`)

- [ ] Texture painting on the model and PNG import per part (cockpit textures included), with
      live preview
- [ ] OGG sounds for engine, rotor and weapons, with pitch curves
- [ ] Package browser, author credits shown in game
- [ ] Operator approval before a player's new vehicle goes live on a server

## Phase 11: every loader, 1.21.1 and 1.20.1 (`1.1.0-beta.7`)

Betas 4 to 6 ship only the Forge 26.3 jar, to keep builds fast. This phase brings the features
added since beta.3 to NeoForge, Fabric and 1.21.1, and adds 1.20.1, the largest modpack version
and the furthest from 26.3. All jars ship again from here on.

- [ ] Every module back in `platforms`; phases 8 to 10 ported to NeoForge and Fabric on 26.3 and
      to all three loaders on 1.21.1
- [ ] `common` compiles for Java 17 (no pattern matching `switch`, no `SequencedCollection`
      methods)
- [ ] 1.20.1 on Forge and Fabric. NeoForge's 1.20.1 is a fork of Forge 47, so the Forge jar is
      tested there instead of built twice
- [ ] Vehicle items keep their vehicle in NBT, since data components only arrived in 1.20.5
- [ ] Rendering, networking and registration for 1.20.1
- [ ] Legacy importer and content packs checked on 1.20.1
- [ ] The phase 7 smoke tests for every jar

## 1.1.0 release

No new features after beta.7; a `1.1.0-rc.N` follows only if beta.7 testing finds problems.

- [ ] Every phase checklist done on every supported Minecraft version
- [ ] A 1.0.0 world and the beta worlds load with their vehicles and items intact
- [ ] A pack made in the workshop on one version loads on every other supported version
- [ ] Tag `v1.1.0` in both repositories; upload one CurseForge file per Minecraft version, with
      the player-facing changelog from `docs/CURSEFORGE.md`

## Open decisions

- **Workshop entry point.** A new workshop block, or a tab in the drafting table.
- **Pack hosting.** Whether there is ever a shared place to publish packs, or only hand sharing
  and server sync.

## Design notes

### One generic vehicle item per kind

Every vehicle currently registers its own item at startup, and Forge freezes the registry before
a world loads, so a vehicle created or received later could never get an item. The item carries
the definition id in a data component instead; the entities already work this way (one entity
type per kind, definition name synced). Existing worlds keep their items through a migration
(see [ARCHITECTURE.md](ARCHITECTURE.md#vehicle-items)).

### Vehicle packages

Players build their own vehicles in game, save them as a file, share them, and fly them on any
server. Someone can build an A350 with its own cockpit texture and engine sound, and everyone on
the server sees and flies it without installing anything.

A **vehicle package** is one zip holding a definition in the existing text format (the same keys
the original vehicles use, so everything the engine supports is available), an OBJ model,
PNG textures and OGG sounds, plus a manifest with id, author, version and a content hash. The
package is data only, never code, so loading one cannot run anything on a player's machine.
Packages live in `<game dir>/mcheli/packs`; the 1.0.3 import becomes one more pack. The format
does not depend on the Minecraft version, so the same zip works on every supported version.

### Multiplayer

The server is the source of truth. On join it sends the list of package ids and hashes; the
client requests only the packages it has not cached, which arrive in chunks over a custom payload
channel and are kept in `mcheli/cache/<hash>.zip`. The server enforces size limits (for example
8 MB per package, 64 MB per server) and an allow list, and can require operator approval before a
player's new vehicle goes live. Packages can also be shared by hand: copy the zip into another
player's `mcheli/packs`.

### The editor

A workshop block (or the drafting table) opens a 3D editor screen around the model:

- **Model:** import an OBJ made in Blockbench or Blender. Modelling a full airliner inside
  Minecraft is not realistic; a simple box builder for small vehicles can come later.
- **Placement with gizmos:** seats (pilot, gunners, passengers), cameras, rotors and their pivot,
  wings, flaps, VTOL nozzles, wheels, tracks, landing gear, canopy and hatch, weapon mounts,
  hit boxes, and the engine and fuel tank positions (which decide where fire and explosions
  start). Each part maps to definition keys such as `AddSeat`, `AddRotor`, `AddWing`, `AddWeapon`.
- **Flight and drive tuning:** speed, lift, mass, turn rates, fuel and hit points, with a test
  flight from the editor.
- **Textures:** paint on the model with a brush, or import PNGs per part (a custom cockpit
  texture included), with live preview.
- **Sounds:** pick OGG files for engine, rotor and weapons, with pitch curves.
- **Save and export** to a package; **load** any package to keep editing.
