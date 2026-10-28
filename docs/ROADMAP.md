# Roadmap

The original mod is about 60,000 lines of 1.7.10 code. Nearly all of it touches APIs that no
longer exist (immediate mode OpenGL, numeric ids, the old network and event systems), so the port
happens in four phases. Each phase ends with a tagged pre-release that installs and runs on its own.

| Phase | Version | Theme | State |
|-------|---------|-------|-------|
| 1 | `1.0.0-alpha.1` | Foundation and flyable helicopters | Done, tested in game |
| 2 | `1.0.0-alpha.2` | Planes, tanks and ground vehicles | Implemented, in-game test in progress |
| 3 | `1.0.0-alpha.3` | Weapons and combat | Implemented, in-game test in progress |
| 4 | `1.0.0-beta.1` | HUD and polish | Done |
| | `1.0.0` | Release: code-only jar | Releasing |
| 5 | `1.1.0` | Equipment, remaining phase 4 features, vehicle workshop | Planned |

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

The rest of the original phase 4 list moved to 1.1.0 (below), so 1.0.0 can ship.

## 1.0.0 release criteria

- Every bundled helicopter, plane, tank, vehicle and weapon loads without warnings
- Each vehicle kind has been flown or driven in single player and on a dedicated server
- No known crash, data loss or desync issue
- No original MCHeli file in the jar or the repository; players import their own copy (done)

## Phase 5: 1.1.0

### Carried over from phase 4

- [ ] Night vision, thermal view and zoom
- [ ] Equipment: GLTD, rangefinder, UAV station and portable controller, parachute, chain towing,
      container, fuel, wrench, Stinger and Javelin, smoke grenades
- [ ] Parachutes for ejection seats and rack drops, containers on racks, repelling and troop drops
- [ ] `HideEntity`, search lights, splash and exhaust particles, helicopter blade folding
- [ ] The drafting table and its recipe browser
- [ ] Server config (`mcheli.cfg` equivalent) and the `/mcheli` command
- [ ] Multiplayer team manager and spotting
- [ ] In-game definition reload
- [ ] External content packs loaded from the game directory (becomes the vehicle packages below)

### Vehicle workshop

Players build their own vehicles in game, save them as a file, share them, and fly them on any
server. Someone can build an A350 with its own cockpit texture and engine sound, and everyone on
the server sees and flies it without installing anything.

### What a custom vehicle is

A **vehicle package**: one zip holding a definition in the existing text format (the same keys
the original vehicles use, so everything the engine supports is available), an OBJ model,
PNG textures and OGG sounds, plus a manifest with id, author, version and a content hash. The
package is data only, never code, so loading one cannot run anything on a player's machine.
Packages live in `<game dir>/mcheli/packs`; the 1.0.3 import becomes one more pack.

### Design changes the engine needs first

- **One generic vehicle item per kind.** Every vehicle currently registers its own item at
  startup, and Forge freezes the registry before a world loads, so a vehicle created or received
  later could never get an item. The item carries the definition id in a data component instead;
  the entities already work this way (one entity type per kind, definition name synced).
  Existing worlds keep their items through a data fixer.
- **Definitions loadable at runtime.** `ContentRegistry` becomes a registry that can add and
  replace definitions while the game runs (the original vehicles stay as they are now).
- **Runtime client assets.** Models already load from files (`MeshCache`); textures go through
  `DynamicTexture` and sounds through a custom `SoundInstance` streaming the OGG, so nothing needs
  a resource reload.

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

### Order of work

1. Generic items and runtime registry, with the 1.0.3 content moved onto them
2. Package format and loading from `mcheli/packs`, which also covers "external content packs"
   from phase 4
3. Server to client package sync with caching and limits
4. Editor: OBJ import, part placement and tuning, saving
5. Editor: texture painting and sounds
6. Sharing polish: package browser, author credits, operator approval
