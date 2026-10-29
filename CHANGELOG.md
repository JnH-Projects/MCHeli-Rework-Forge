# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/). Pre-releases (`1.0.0-alpha.N`) mark the phases on the
way to 1.0.0; see [docs/ROADMAP.md](docs/ROADMAP.md).

## [Unreleased]

Phase 5 of the road to 1.1.0, to be released as `1.1.0-beta.1`: the groundwork that content
packs, older Minecraft versions and the vehicle workshop build on. Betas are not uploaded to
CurseForge; download them from the GitHub releases. Worlds from 1.0.0 load with their vehicles
and items.

### Added

- `/mcheli` shows where the vehicles were loaded from and how many there are; `/mcheli list
  [helicopter|plane|tank|vehicle]` lists them.
- `/mcheli give <players> <vehicle> [count]` for operators, with the vehicle names suggested.
- `/mcheli reload` for operators: reads the vehicle and weapon definitions again without
  restarting. Vehicles already in the world switch to the new values on the spot and keep their
  ammunition; models and textures edited on disk show up straight away, without F3 + T.
- Server config, `serverconfig/mcheli-server.toml` in each world: whether explosions break blocks
  and start fires, a damage multiplier for vehicles, infinite ammunition, and a list of vehicles
  that cannot be placed. Edits apply while the server runs.
- Boats destroyed on the water (by a torpedo or anything else) cook off: three explosions with
  fireballs within about five seconds, the first from the fuel, the second from the ammunition
  still aboard, the last and biggest sized by the fuel load like a crash. Then the burning wreck
  slowly goes under, stern first and listing, and settles on the bottom.
- Content can now bring textures, models and sounds while the game runs, without a resource
  reload. Nothing uses this yet beyond `/mcheli reload`; content packs (beta.2) will.

### Changed

- Vehicle items: there is now one item per vehicle kind (Helicopter, Plane, Tank, Ground Vehicle)
  that remembers which vehicle it places, instead of one item per vehicle. They look and are named
  as before. Items from 1.0.0 still work and turn into the new ones as soon as they are in a
  player's inventory; items in chests change when taken out. To give one by command, use
  `/mcheli give` (plain `/give mcheli:ah-64` no longer exists for new items).
- The imported content is converted again on the first start, since the crafting recipes now
  make the new items. This takes a few seconds, once.
- The source is split into a version-free `common` module and a module per Minecraft version
  (`forge-26.3` for now), and a release attaches one jar per Minecraft version. Nothing changes for
  players.

### Fixed

- Boats and other floating vehicles sat about a third of a block too deep, so the Zodiac was
  almost under water. They now float as high as in the original.
- Static launchers and guns that turn with the gunner's view, such as the Type 93 and Mk 32
  torpedo tubes, only fired straight ahead. They fire where the gunner looks again.
- On Windows, converting the content again could fail with "access denied" when another program
  had the old `mcheli/imported` folder open, leaving the game without vehicles until the next
  start. The old folder is now moved aside first, and the swap is retried.

## [1.0.0] - 2026-10-28

First release. Everything from the alpha and beta versions, shipped as a code-only jar. The
features still open from phase 4 (night vision, equipment, drafting table, config and more) and
the vehicle workshop are planned for 1.1.0; see [docs/ROADMAP.md](docs/ROADMAP.md).

### Installing

This jar contains only the mod's code. The vehicles, models, textures and sounds are EMB4's
original MCHeli content, which you download yourself:

1. Install Forge 66.1.0 for Minecraft 26.3 and put `mcheli-26.3-1.0.0.jar` in the `mods` folder.
2. Download `MC1.7.10_mcheli_1.0.3.zip` from the
   [MCHeli CurseForge page, Files tab](https://www.curseforge.com/minecraft/mc-mods/mcheli-minecraft-helicopter-mod/files).
3. Create a folder called `mcheli` next to `mods` and put the zip in it. Do not unzip it.

   ```
   .minecraft/
     mods/mcheli-26.3-1.0.0.jar
     mcheli/MC1.7.10_mcheli_1.0.3.zip
   ```

4. Start the game. The first start converts the zip, which takes a few seconds. If the zip is
   missing, the game still starts and the chat tells you where to put it.

Servers need the same two files. Only the zip goes in the `mcheli` folder, never in `mods`.

### Changed

- The jar no longer contains EMB4's vehicles, models, textures, sounds or HUD scripts; it shrinks
  from 73 MB to under 0.5 MB. Players put the original `MC1.7.10_mcheli_1.0.3.zip` from the MCHeli
  CurseForge page into `<game directory>/mcheli`, and on start the mod converts it into
  `mcheli/imported`, which loads as a required resource pack and data pack ("MCHeli Content").
  The conversion runs again only when the zip changes; once done, the zip can be deleted.
  Dedicated servers need the zip as well.
- Crafting recipes are generated from the definitions during that conversion instead of shipping
  in the jar.
- Item names and achievement texts come from the imported zip. The lang files in the jar hold only
  the rework's own keys (HUD, key bindings, entities, creative tabs), and the bundled `zh_cn`
  file, which had none of those, is gone.
- `tools/AssetMigrator.java` is replaced by the importer inside the mod
  (`content.LegacyImporter`).
- The repository no longer holds any original files: the converted assets, the generated recipe
  copy and `docs/legacy/original` are gone. The unit tests import the zip from `Mod/` into
  `build/legacy-content` instead and are skipped where it is missing, as on CI. The translated
  comments of the converted definitions went with them; the format guides in `docs/content` keep
  the translations that matter. `-PupdateRecipes` is no longer needed.

### Added

- Without the zip the game still starts, with no vehicles, and the chat says where to put it on
  joining a world. A zip that fails to import reports the reason in the chat and the log.
- A guide to modeling vehicles in Blockbench or Blender (`docs/content/modeling.md`): axes,
  scale, part names, textures and export settings.
- A contributing guide (`CONTRIBUTING.md`) and issue and pull request templates.

## [1.0.0-beta.1] - 2026-10-27

Phase 4: HUD, equipment and polish.

### Added

- The original's scripted HUDs, drawn in first person from `assets/mcheli/hud` for each seat (the
  `HUD` line of the definition or the kind's default) and `tv_missile` while flying a TV missile:
  attitude, heading tape, pitch ladders, altitude, speed, hit points, weapon, ammunition, reload,
  heat and lock, flare state, the control stick and the gunner camera frame. Every `HUD` command of
  the original works, with the same variables and text arguments.
- The entity radar of vehicles with `EnableEntityRadar`: a sweep that shows mobs within 64 blocks,
  hostile ones apart, unless they are buried under five or more blocks.
- HUD scripts load through the resource manager, so resource packs can replace or add them, and
  F3 + T reloads them. A script that does not parse is reported in the log with its line and
  left out, where the original crashed.
- Pilot gunner mode (`EnableGunnerMode`, `ConcurrentGunnerMode`), on H: the pilot leaves the
  controls to man the weapons from the camera position, with the gunner HUD. Planes such as the
  AC-130 level off and circle by `AutoPilotRot` while A and D still turn them; helicopters hold a
  hover. A plane needs level flight, more than 60 % throttle and five blocks of air below to
  switch. H again takes back the controls; leaving the pilot seat, or a player boarding the gunner
  seat of a vehicle without `ConcurrentGunnerMode`, ends it.
- Not in the original: tanks and cars take damage from hitting walls faster than about 6 m/s and
  from landing after a drop of more than four blocks. The share of hit points lost grows with the
  square of the speed or height: about a third for a GT-R at full speed into a wall, all of it at
  24 m/s or after a 24 block drop. Armour applies as for any other damage, and a vehicle wrecked
  this way goes up like a crashed aircraft. Landing in water does no harm.
- A crashing helicopter, plane or vehicle now goes up in a much bigger fireball than its fuel
  explosion alone: the smoke shell of a missile hit in mid-air, a cluster of fireballs, a plume of
  smoke that keeps climbing, burning fuel and twice the burning debris.
- Crafting recipes for 106 vehicles, generated from the original's `AddRecipe` lines with the
  1.7.10 items mapped to today's (dyes and wool by colour, common tags for iron, glass and dyes so
  other mods' materials count). They are in the recipe book from the first login. Vehicles the
  original gave no recipe stay creative only, and the E-767 waits for the UAV station.
- Screenshots and two short clips of phase 4 in the readme (gunner mode, SA-2 lock-on and the
  crash fireball), besides the showcase video of `1.0.0-alpha.3`.

### Changed

- The plain text readout of phase 3 now shows only where no HUD script draws: in third person,
  for a destroyed vehicle, or for a seat whose script does not exist. A seat set to `none` (the
  GT-R's passenger) shows nothing in first person, as in the original.
- Network protocol version 4, for the gunner mode crew action and the crash fireball.
- The first person hand is hidden in a vehicle, as in the original: always for the pilot, and for
  anyone looking through a camera or gunner sight or with a weapon to aim.
- The view tilts with the hull in every seat of every vehicle, tanks and cars included. The tilt is
  taken along the way the rider looks, so a passenger looking out of the side sees the hull pitch
  as a bank. Previously only helicopter and plane crews got the hull roll, whatever way they
  looked.

### Fixed

- Cars and wheeled vehicles with `PivotTurnThrottle` (the Skyline GT-R, Centauro, Growler, M1129
  and MXT-MV) could not steer with A and D. Steering scaled with the distance moved since the
  previous tick, which always read zero at that point of the tick.
- The camera banked the opposite way to the hull. Forge 66 passes the roll to
  `Camera.setRotation`, which banks left for a positive value.
- Thin panels made of back to back faces flickered between their two textures as the view moved.
  Vehicles, projectiles and the A-10 now cull back faces as the original did, glass included.
- Seeking missiles fired from a gunner sight or a launcher camera (the S-75's SA-2) would not lock
  on targets close by. The lock cone was measured from the player's feet instead of the point the
  view is seen from, which on the S-75 is several blocks higher.

### Known gaps

- HUD values for systems still to come read as idle: fuel is full, the camera is in normal view at
  zoom 1, the vehicle inventory is empty and the mortar distance is unknown. The control stick
  indicator follows the pilot's mouse look rather than the original's raw mouse input.

## [1.0.0-alpha.3] - 2026-10-27

Phase 3: weapons and combat.

### Added

- All 229 bundled weapons load from `assets/mcheli/weapons` with every key of `MCH_WeaponInfo`,
  its defaults, ranges and `checkData` fix-ups. Vehicles drop `AddWeapon` lines that name a weapon
  that does not exist, as the original did.
- Weapon sets per vehicle: consecutive `AddWeapon` lines of one weapon fire in turn, share a
  magazine, reload, heat and recoil, and wait for each other when they share a `Group`.
- Firing with the use key (right mouse), switching with the pick block button (middle mouse) or G,
  Shift for the previous weapon, X for the fire mode. Each seat gets the weapons its `AddWeapon`
  lines assign to it, and the pilot also gets a gunner's weapons while that seat has no player.
- Every weapon type of the original: machine guns with ball and HE modes, rockets, bombs, cluster
  munitions (`Bomblet`), AA missiles with lock-on, AS missiles on the aimed point, AT missiles with
  direct and top attack modes, TV-guided missiles flown with the view (the first person camera
  rides the missile), torpedoes straight or guided, marker rockets calling in a bombing run, close
  air support by an A-10, dispensers (water drops, bone meal, seeds and so on), targeting pods and
  smoke generators.
- Fuses and ballistics: proximity, time and delay fuses with bouncing rounds, explosion altitude,
  gravity in and out of water, piercing rounds, fuel air explosives, and rounds that shoot through
  glass panes, grass and flowers (`BulletBreakableBlocks`).
- Explosions with the original's separate entity and block sizes, damage curve, `DamageFactor` per
  target kind and burning; blocks break through a vanilla explosion, so `mobGriefing` applies.
  Underwater charges crumble blocks the way the original's did.
- Damage model: `ArmorMinDamage`, `ArmorMaxDamage`, `ArmorDamageFactor`, and the damage factor of
  each `BoundingBox`, which projectiles now hit individually. Rounds ignore invulnerability frames,
  like the original's default `HurtResistantTime`.
- Destroyed state: an explosion, a burning wreck that sets its crew alight, a helicopter that spins
  down, a fireball on impact sized by the vehicle's fuel, ejection for seats that have it, and
  removal after 25 seconds. Damage smoke thickens as hit points drop. The server flies the wreck
  from the moment it is destroyed, so everyone sees the same crash.
- The original's explosion effect: a fireball, dust thrown up from the crater, columns and rings
  of smoke, burning debris arcing away from big blasts, and a dome of spray under water.
- Crash damage from the original: hitting the ground tilted more than 40 degrees (91 for tanks),
  and sinking without floats.
- Not in the original: flying into terrain at more than about 25 m/s, or two vehicles meeting at
  more than about 10 m/s, is a crash. Damage grows with the square of the impact speed, and a
  vehicle destroyed this way goes up in its fuel fireball.
- Flares (`FlareType`, `FlareOption`) on C, with all six release patterns; while they burn, missiles
  homing on the vehicle or its crew are decoyed. Crews hear a warning when locked or tracked.
- Ammunition with magazines, reserve and `MaxAmmo`. Creative players never run dry; others
  resupply the selected weapon with N, paying its `Item` cost from their inventory. Ammunition is
  saved with the vehicle.
- Weapon parts follow their weapon's aim, kick back on firing (`RecoilBufCount`), spin their
  barrels and hide missiles until reloaded. Weapon bays open while their weapon is selected. Big
  guns with `Recoil` kick the whole hull.
- Effects: muzzle flashes and smoke in the weapon's colours, ejected cases, trails, splashes, block
  chips, hit confirmation sound, and every weapon's own sound with `SoundDelay` and pitch spread.
- A plain text readout of hit points, weapon, ammunition, reload, heat, lock and flares, until the
  scripted HUDs arrive in phase 4.
- A `mcheli:bullet` damage type, so deaths read "shot down by".
- Unit tests that load all 229 weapons and check every vehicle's weapons exist.

### Changed

- Network protocol version 3: new fire, ammunition and lock warning packets and five crew actions.
- A survival player punching an empty vehicle packs it up into its item at once, as in the
  original; with crew aboard it takes damage instead. Creative players drop the item only when
  sneaking.

### Known gaps

- Targeting pods spot entities by making them glow; marking a block for the team needs the team
  manager of phase 4. Cases are ejected as particles rather than the 3D case model.
- Mortar distance, zoom and the original HUD scripts come with phase 4, as does resupplying from
  the vehicle's inventory screen and from supply vehicles (`AmmoSupplyRange`).

## [1.0.0-alpha.2] - 2026-10-26

Phase 2: planes, tanks and ground vehicles.

### Added

- Planes (51), tanks and cars (12) and ground vehicles (16) load from their definitions, each
  with its own entity type, item and creative tab. Every shipped file loads without warnings.
- Plane flight model ported from `MCP_EntityPlane`: thrust along the nose, lift from the
  vertical part of that thrust, ground handling, floats, reverse thrust and target drone autopilot.
- VTOL: nozzles and tilt rotors swing down with V, with the original yaw and pitch factors in
  vertical flight. `DefaultVtol` planes stay in vertical mode on the ground.
- Folding wings and variable sweep wings on Z, including the automatic unsweep at low speed and
  the higher top speed with wings swept (`SweepWingSpeed`).
- Retractable landing gear on B with the original automatic lowering near the ground and the
  amphibian rules on water.
- Canopies: the first click on a closed canopy opens it, the next boards, and it closes once the
  pilot is in. Planes cannot take off with the canopy open.
- Ejection seats for seats one and two: double tap Space. Until the parachute item exists the
  crew member gets 20 seconds of slow falling.
- Tank and car drive model ported from `MCH_EntityTank`: throttle, reverse, hand brake on Space,
  pivot turns and `PivotTurnThrottle` for car-like steering.
- Terrain following: the hull pitches and rolls to match the ground under its wheels
  (`SetWheelPos`, `WeightedCenterZ`, `OnGroundPitchFactor`, `OnGroundRollFactor`).
- Ground vehicles ported from `MCH_EntityVehicle`, with the `AddPart` / `AddChildPart` tree turning
  to follow the gunner's view.
- Animated parts for every kind: hatches, canopies, landing gear, weapon bays, throttle levers,
  rotating parts, sensor turrets, road wheels, steering wheels, track rollers and crawler tracks,
  wings, pylons, nozzles and propellers. Part names match the original numbering, and separate
  `<vehicle>_<part>` model files are still accepted.
- Weapon parts, turrets and their children turn with the view of the crew member whose seat
  fires them, within the weapon's traverse limits.
- Explicit seats: every passenger has a synced seat, seat zero flies, R moves to the next free
  seat, `ExclusionSeat` is honoured and seats survive saving and reloading.
- Gunner seats with their sight position and pitch limits, fixed rotation seats, turret seats
  that turn with the turret, and H to cycle the pilot's camera positions or toggle a gunner sight.
- Racks: J loads nearby vehicles onto the carrier's racks or drives this vehicle onto a carrier,
  U unloads or leaves. `AddRack`, `RideRack` and `ExclusionSeat` are supported for vehicles.
- Engine sound loop for every kind, driven by throttle with the original volume and pitch
  curves, `SoundRange` distance scaling and a slight Doppler shift.
- `StepHeight` now applies to every vehicle kind, as it did in the original.

### Fixed

- Riders stayed upright while their vehicle banked or pitched, so a rolling plane left the pilot
  sticking out of the canopy. Players now tilt with the hull about the seat, and the first person
  eye follows the hull's up axis.
- Boarding needed a click on the small default hit box at the vehicle's feet. Right-clicking
  anywhere on the hull's `BoundingBox` boxes now boards, also from inside a cockpit, as it did in
  the original; the server checks the reach against the same boxes.
- Water disappeared behind canopies. Vehicles were drawn entirely translucent, which wrote depth
  before the game drew water. Opaque triangles now draw as cutout and only the glass, found from
  the texture's alpha, draws blended without writing depth.
- Boats and float planes sank to their deck: the water depth check is now the original one, the
  submerged share of the hit box shifted by `FloatOffset`. Floating helicopters benefit as well.
- Canopies and windshields rendered solid; vehicles now draw with a translucent render type so
  their semi-transparent glass shows through, as it did in the original.
- Unit tests for the new definition kinds, and a check that the part names generated from every
  shipped definition resolve in its model. The five that do not are broken in the original
  content as well and are listed in the test.

### Changed

- Helicopters now render all their common animated parts (hatches, landing gear, weapon parts,
  sensor turrets) and have engine sound.
- VTOL moved from F to V, because F now swaps hands in vanilla.
- The original's per-frame attitude rules for planes and tanks run three times per tick with
  the averaged partial tick of 0.5, matching a 60 FPS client without depending on frame rate.
- The tank's invisible wheel entities are replaced by collision probes, which behave the same
  without extra entities to sync.
- Network protocol version 2: the pilot state now carries reverse throttle and key state, and a
  new packet carries crew actions. Clients and servers must both run this version.

### Known gaps

- Weapon bays stay shut until weapons exist in phase 3; they open while their weapon is selected.
- Racks carry vehicles only. Containers, parachutes, repelling and dropping troops arrive with
  the equipment in phase 4.
- `HideEntity` does not hide the crew yet, and search lights, splash and exhaust particles,
  blade folding and the pilot gunner mode are not ported.

## [1.0.0-alpha.1] - 2026-10-26

Phase 1: foundation and flyable helicopters.

### Added

- Forge 66.1 / Minecraft 26.3 project on ForgeGradle 7, targeting Java 25.
- All 1,145 MCHeli 1.0.3 assets migrated to the 26.x resource layout by `tools/AssetMigrator.java`:
  lower-case resource names, `textures/item` and `textures/block` folders, JSON language files,
  a generated `sounds.json` and an item model for every item texture.
- Content loader for the original definition format. Keys, defaults and value ranges match
  MCHeli 1.0.3. A malformed line is now skipped with a warning instead of discarding the file,
  and Shift-JIS files are read as well as UTF-8.
- Metasequoia (`.mqo`) and Wavefront (`.obj`) model loaders, including the `$part` naming
  convention, smoothing angles and the original winding and scale.
- Helicopter entity with the original flight model: throttle driven lift, pitch to speed
  conversion, hover mode and ground handling.
- One item per helicopter (33 in total) and a creative tab.
- Helicopter renderer for hull and rotors, using the 26.x submission based rendering.
- Pilot controls: mouse flying, W/S throttle, A/D bank, Space hover, Left Alt free look,
  camera roll.
- Multi-seat boarding using the seat positions from the definitions.
- Client authoritative flight, the same model vanilla boats use, plus a small packet for roll,
  throttle and hover state.
- Unit tests for the definition parser, the model loaders and the integrity of every shipped
  helicopter.
- GitHub Actions: build and test on every push and pull request, and a release workflow that
  publishes the jar when a `v*` tag is pushed.
- English translations of the original authoring guides in `docs/content`.

### Changed

- Japanese comments in the bundled definitions and HUD scripts are translated to English. The
  untouched originals are kept in `docs/legacy/original`.
- Free look moved from Left Ctrl to Left Alt, because Left Ctrl is now the vanilla sprint key.
- Roll damping runs per tick instead of per frame, so handling no longer depends on frame rate.

[1.0.0]: https://github.com/JnH-Projects/MCHeli-Rework-Forge/releases/tag/v1.0.0
