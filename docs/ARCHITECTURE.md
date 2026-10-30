# Architecture

This document explains how MCHeli Rework is organized and why. It reflects the code as of
`1.1.0-beta.3`.

## Goals

1. **Keep the content.** The 130 vehicles, 229 weapons and their models are the value of MCHeli.
   They load unchanged, and so do content packs written for the original.
2. **Rewrite the engine.** 1.7.10 APIs are gone, so the code is not translated line by line.
   Behavior is ported where players can feel it (flight model, defaults, ranges) and redesigned
   where the platform changed (rendering, networking, registration).
3. **Keep the core testable.** Parsing and model loading do not depend on Minecraft, so they run
   in plain unit tests against every shipped file.

## Layers

```
 ┌────────────────────────────────────────────────────────────────┐
 │ forge / neoforge / fabric   entry point, event wiring, packet  │  loader module
 │                 channel, vehicle ingredient type, mixins       │  (forge-26.3, ...)
 ├────────────────────────────────────────────────────────────────┤
 │ client          renderer, mesh cache, runtime assets, crew     │  client only
 │                 controller, sound, HUD renderer, pack download │
 ├────────────────────────────────────────────────────────────────┤  minecraft-<version>
 │ entity          AircraftEntity and one subclass per kind       │  (minecraft-26.3)
 │ item            AircraftItem, VehicleIngredient                │
 │ network         packets, ModNetwork, PackSync                  │  Minecraft, no loader
 │ particle        GlowParticleOptions                            │
 │ registry        entities, items, components, particles, tabs   │
 │ platform        Platform, Registration                         │
 │ (root)          MCHeli, CommonHooks, configs, McheliCommand    │
 ├────────────────────────────────────────────────────────────────┤
 │ content         Definition, DefinitionReader, ContentRegistry, │  common module
 │                 aircraft/* definitions, recipe conversion,     │
 │                 pack/* content packs                           │
 │ model           Mesh, MqoLoader, ObjLoader                     │  pure Java
 │ hud             HudScript, Expression, HudVariable             │
 │ physics         AircraftRotation, GroundImpact                 │
 │ effect          NightGlow                                      │
 │ config          ConfigSpec (the TOML config format)            │
 └────────────────────────────────────────────────────────────────┘
```

Arrows only point down. The bottom layer lives in the `common` Gradle module, which is compiled
and tested with nothing from Minecraft or a loader on its classpath (only the JDK, SLF4J,
JSpecify and JOML), so a stray import fails the build; `CommonBoundaryTest` also scans the
sources. The middle layer, `minecraft-<version>`, is everything that touches Minecraft but no
loader. The top layer is a thin module per loader and Minecraft version, which compiles both
lower layers together with its own glue into one jar. See
[DEVELOPMENT.md](DEVELOPMENT.md#three-layers).

The shared code reaches the loader in two ways only:

- **`Platform`**, installed by the loader module before anything else runs: the game and config
  folders, the mod version, whether this is a client, sending a packet, and a creative tab
  builder (vanilla's is not public on Fabric, and only Forge and NeoForge order tabs).
- **`Registration`**, a list of registry entries (`Registered<T>`, like Forge's
  `RegistryObject`) that the shared registry classes declare during `MCHeli.init`. Forge and
  NeoForge register them from their `RegisterEvent`; Fabric registers them straight away.

Everything else goes the other way: the loader module calls `CommonHooks` (server start and stop,
ticks, player join and leave, commands, the imported pack) and `client.ClientHooks` (renderers,
keys, reload listeners, HUD layer, ticks, camera, hand, use and pick block keys, rider pose) from
its own events. Where a loader has no event, the loader module adds a mixin at the place Forge
fires its event, so the shared code sees the same thing on every loader:

| Hook | Forge | NeoForge | Fabric |
|------|-------|----------|--------|
| Camera angles and cockpit eye | `ViewportEvent.ComputeCameraAngles` | mixin on `GameRenderer` (NeoForge's event fires before vanilla sets the camera position) | mixin on `GameRenderer`, plus roll on `Camera` |
| Hide the hand | `RenderHandEvent` | `RenderHandEvent` | mixin on `GameRenderer.renderItemInHand` |
| Use and pick block keys | `InputEvent.InteractionKeyMappingTriggered` | the same | mixin on `Minecraft` |
| Rider tilt | `RenderLivingEvent.Pre` | `RenderLivingEvent.Pre` | mixin on `LivingEntityRenderer.submit` |
| Imported content pack | `AddPackFindersEvent` | `AddPackFindersEvent` | mixin on the `PackRepository` constructor |
| Streamed sounds | `SoundInstance.getStream` | `SoundInstance.getStream` | mixin adding `getAudioStream` to `RuntimeSoundInstance` |

NeoForge rolls the camera the other way round from Forge, so its mixin negates the roll.

### Minecraft 1.21.1

`minecraft-1.21.1` is a copy of `minecraft-26.3` ported back, so the two read side by side. After
1.1.0 it is frozen (see [supported versions](ROADMAP.md#supported-versions)), so the copy is not
kept in step by hand beyond that. What differs:

- **Rendering.** 1.21.1 has no render states or submit step. Each renderer fills the same state
  object itself at the start of `render` and draws straight into the `MultiBufferSource`, so
  `PartRenderer`, `MeshRenderer` and `GlassSplit` are unchanged apart from that. The glow uses a
  render type built from `RenderType`'s public constructor (vanilla keeps `RenderType.create`
  package private) and a custom `ParticleRenderType` for additive particles.
- **Vehicle icons.** There is no `item_model` component. The import writes one item model per kind
  whose `custom_model_data` overrides pick each vehicle's model, and lists the numbers in
  `mcheli/imported/item_icons.properties`; `AircraftItem.stackOf` puts the number on the stack.
- **Recipes** take ingredients as `{"item": ...}` and `{"tag": ...}` objects; the import writes
  them that way for `ImportTarget.V1_21_1`. The marker file records the target, so a folder
  imported for one version is imported again by the other.
- **Entities** extend `Entity` instead of `VehicleEntity`, whose abstract `getDropItem` is package
  private in vanilla, and keep their own hurt wobble data. Save data is `CompoundTag`, damage goes
  through `hurt` (server only), server position updates are eased in by `lerpTo` like vanilla's
  boat, and a click on the hull arrives through `interactAt`.
- **Camera.** Forge 1.21.1 fires its camera event after the camera is placed, so Forge uses the
  event. NeoForge and Fabric hook `GameRenderer.renderLevel` just before the camera rotation turns
  into the view matrix, the spot Forge fires from.

`client` is only referenced from the loader modules behind a physical client check, so a
dedicated server never loads a client class.

Package root: `com.jnhprojects.mcheli`, with the loader glue in `.forge`, `.neoforge` and
`.fabric`. Mod id and asset namespace: `mcheli`, the same as the original, so existing resource
paths and content packs keep working.

## Startup

```
loader entry point (McheliForge, McheliNeoForge, McheliFabric)
  ├─ Platform.install
  ├─ MCHeli.init
  │    ├─ LegacyContent.prepare                       import the original zip into mcheli/imported
  │    ├─ ContentPacks.load(false)                    the import plus every pack in mcheli/packs
  │    ├─ ModDataComponents.init                      mcheli:vehicle
  │    ├─ ModEntities.init                            one entity type per kind
  │    ├─ ModParticles.init                           mcheli:glow
  │    ├─ ModItems.init(content)                      one item per kind, plus the 1.0.0 items
  │    └─ ModCreativeTabs.init
  ├─ register Registration's entries, and the mcheli:vehicle ingredient type
  ├─ register ModNetwork's packets
  ├─ wire events to CommonHooks                       imported pack, server config, packs, commands
  └─ client only: ClientHooks.init and client events  client config, renderers, keys, HUD, input
```

Definitions are still parsed during mod construction, but only because the 1.0.0 items have to be
registered before the loader freezes the registries (see [Vehicle items](#vehicle-items)); nothing
else depends on them existing that early any more. The files are read straight from the imported
folder and the content packs (see [Assets](#assets) and [Content packs](#content-packs)). Models
and textures, by contrast, go through the resource manager, so resource packs can replace them.

## Content definitions

`DefinitionReader` implements the line format: `key = value`, case insensitive keys, `;`, `#` and
`//` comments, repeatable keys. Each `Definition` subclass registers one handler per key in
`registerKeys`, which keeps the ported logic close to the original `loadItemData` branches without
the 1,700 line if/else chain:

```java
keys.put("MaxHp", v -> maxHp = toInt(v, 1, 100000));
keys.putAliased("AddGunnerSeat", this::parseSeat);
```

Defaults and clamping ranges are copied from `MCH_AircraftInfo` and its subclasses. After all
lines are applied, `validate()` fills in derived values (default HUDs, camera, UAV seat) and can
reject the definition.

Deliberate differences from the original loader:

| Original | Rework | Why |
|----------|--------|-----|
| One bad line discarded the whole file | The line is skipped and reported | Easier for content authors |
| Unknown keys were silently ignored | Unknown keys are collected and logged at debug | Shows which keys still need porting |
| Read as UTF-8 only, mangling Shift-JIS | UTF-8 with a Windows-31J fallback | 15 shipped files are Shift-JIS |

`ContentRegistry` holds the loaded definitions, and can change while the game runs: `add`
replaces or adds one definition, `remove` drops one, and `replaceWith` swaps in a whole freshly
loaded set. The maps sit in an immutable snapshot that writers copy and publish in one volatile
write, so readers never lock and the render and server threads of a single player game always
see a consistent set. Every change bumps `generation()`. `AircraftEntity.definition()` caches its
definition together with the generation it was looked up at, and looks it up again when the
number moves, rebuilding seats, parts and weapons while keeping the ammunition it carries.
`MCHeli.content()` is always the same object; reloads change what it holds.

### Vehicle items

1.0.0 registered one item per vehicle at startup, and the loaders freeze the item registry before
a world loads, so a vehicle added later could never get an item. Since 1.1.0 there is one item per
kind (`mcheli:helicopter`, `mcheli:plane`, `mcheli:tank`, `mcheli:vehicle`). A stack names its
vehicle in the `mcheli:vehicle` data component and points its `minecraft:item_model` component at
the item model the import wrote for that vehicle, so it looks and reads as before.
`AircraftItem.stackOf(def)` is the one place that builds such a stack (creative tabs, `/mcheli
give`, pick block, packing a vehicle up).

Existing worlds keep their items through a migration rather than a data fixer: the 1.0.0 ids
(`mcheli:ah-64` and so on) are still registered for the content present at startup, as
`LegacyAircraftItem`. They place vehicles as before, are not listed in any tab, and replace
themselves with the generic stack (count, name and other components kept) as soon as they tick in
a player's inventory. A stack in a chest converts when a player takes it out.

Recipes craft the generic item with both components set. A vehicle used as an ingredient (the
AH-6X recipe takes an AH-6) becomes a `{"type": "mcheli:vehicle", "vehicle": "ah-6"}` ingredient that only matches stacks of
that vehicle. Each loader names custom ingredients under its own key (`type` on Forge,
`neoforge:ingredient_type` on NeoForge, `fabric:type` on Fabric), so the import writes all three
and each loader ignores the other two; a world keeps working when it moves to another loader.
`VehicleIngredient` holds the matching, and each loader module wraps it in its own ingredient
class.

### Reloading content

`/mcheli reload` (operators) runs `ContentReload`: `ContentPacks` scans the import and the packs
again with the server config applied, the result is `replaceWith`ed into the live registry, and
every player gets the new `PackListPacket`. Vehicles in the world pick up their new definition on
their next tick. Each client then rebuilds its own registry the way it does on joining (see
[Content packs](#content-packs)), except in single player, where client and integrated server share
it. `ClientContentReload.refresh` then clears the mesh cache and glass splits, uploads every
vehicle texture again from disk (`RuntimeAssets.refreshTextures`) and rebuilds the creative tabs.

### Content packs

A content pack (`content.pack.ContentPack`) is a zip or folder with a `pack.properties` manifest
and an `assets/mcheli` tree laid out like the import. Opening one walks every file: only
definitions, models, textures and sounds are allowed (so a pack can never carry code), file names
must be lower case, and the file count and unpacked size are capped while reading, not trusted
from the zip. The content hash is SHA-256 over each file's path and its own SHA-256, so it depends
on the files rather than on how they were zipped; servers and clients compare it to decide what
to send. Zips are read in place through a zip file system and stay open while in use.

`ContentRegistry.load` takes a list of `ContentSource`s, each a pack id and an `assets/mcheli`
folder. Weapons load first from every source, so vehicles can use weapons from another pack;
then each source's vehicles in order, a later definition replacing an earlier one of the same
name whatever its kind. Every definition records its pack (`Definition.pack()`). Problems are
collected as `PackProblem`s instead of thrown: a file that cannot be read, a rejected definition,
a bad line, an unknown weapon. Those of the import are only logged, as before.

`ContentPacks` (platform) owns `<game dir>/mcheli/packs`. The constructor loads the import plus
every pack, so the vehicles exist before any world does. `ServerAboutToStartEvent`, the first
moment the per-world server config exists, loads again with the allow list and size limits
applied, and so does `/mcheli reload`. A replaced set of packs is closed one reload later, so the
client of a single player game never reads from a zip that was just closed. Only the import's
vehicles get a 1.0.0 item and an `item_model` component; item models only load with resources, so
pack vehicles show their kind's generic item.

```
 server                                              client
 ──────                                              ──────
 player joins
   ── PackListPacket(id, version, hash, size) ────▶ own mcheli/packs or mcheli/cache/<hash>.zip?
   ◀─────────────── PackRequestPacket(hashes) ──── for the ones it has neither
 PackSync, every tick, per player
   ── PackDataPacket(hash, offset, 128 KiB) ──────▶ assembled in memory, size checked
      (2 chunks a tick, one pack after another)      written as <hash>.download.zip, opened,
                                                     hash checked, renamed to <hash>.zip
                                                   all there: ClientPacks.apply
                                                     import + the server's packs, in its order
                                                     replaceWith, RuntimeAssets roots, refresh
                                                     trim the cache to packs.cacheSize
```

The server only sends packs from its current list, by hash. `ClientPacks` uses exactly the
server's packs while connected, so the vehicle a player simulates has the server's definition; a
pack of the player's own counts as cached when its hash matches. The import is never sent. In
single player and for a LAN host, the client points `RuntimeAssets` at the server's open packs
instead. `RuntimeAssets.setRoots` swaps the folder list in one write, the pack loaded last first.

### Server config and command

`ServerConfig` is `serverconfig/mcheli-server.toml` in each world, read when the world starts and
on `/mcheli reload`: explosions breaking blocks and starting fires, a damage factor for vehicles,
infinite ammunition, and a list of disabled vehicles. Only the server applies these rules; the getters return the
defaults (the 1.0.0 behaviour) when no world is loaded. `McheliCommand` registers `/mcheli`
(content status), `/mcheli list [kind]`, and for operators `/mcheli give` and `/mcheli reload`.

Both configs use `config.ConfigSpec` from `common`, a small TOML reader and writer with the layout
Forge's config spec wrote, so files from 1.0.0 and the first betas keep their values on every
loader. A missing key or bad value falls back to its default, and the file is written back
complete with comments. Unlike Forge's, it does not watch the file: edits apply on the next world
start or `/mcheli reload`.

Recipes are data in 26.x, so the definitions' `AddRecipe` and `AddShapelessRecipe` lines are
turned into `data/mcheli/recipe/<vehicle>.json` during the import by `content.recipe.LegacyRecipe`,
following `MCH_ItemRecipe`. `LegacyItems` maps the 1.7.10 item names and damage values: dyes and
wool by colour, the common `c:` tags where an item became a family (iron, glass, dyes) so other
mods' materials count, vanilla tags for logs, planks and boats. As in 1.7.10, a pattern letter the
key does not define is an empty slot. `RecipeDataTest` converts the whole set and spot checks
the output. `CommonHooks.playerLoggedIn` puts every MCHeli recipe in a player's recipe book on login, standing in
for the drafting table's recipe list.

## Models

`MqoLoader` and `ObjLoader` produce a `Mesh`: an ordered list of `MeshGroup`s, each a flat
triangle list with positions, normals and UVs in parallel float arrays. The loaders reproduce the
original exactly where it shows: MQO coordinates are divided by 100, MQO winding is reversed,
quads are split, OBJ V is flipped, and MQO `shading`/`facet` produce smoothed normals.

MCHeli marks movable parts by object name. `Mesh.part("$blade0")` returns the `$blade0` object and
every following object up to the next `$` object, which is the original `renderPart` rule.
`Mesh.body()` returns `$body`, or everything when there is no `$body`.

Models are files rather than baked Minecraft models because they are large (tens of thousands of
triangles), animated per part and authored outside Minecraft's JSON format.

## Entities

There is one entity type per vehicle kind (`mcheli:helicopter`, `plane`, `tank`, `vehicle`), not
one per vehicle. The
entity stores its definition name in synched data, as the original did. Adding content never
changes the entity registry, and worlds keep loading when a content pack is removed. An entity
whose definition is missing stays inert instead of crashing.

```
VehicleEntity (vanilla)
  └─ AircraftEntity          definition lookup, HP, seat map, part states, racks, authority, save data
       ├─ HelicopterEntity   rotor animation, flight model, mouse handling
       ├─ PlaneEntity        flight model, VTOL nozzles, wings, mouse handling
       ├─ TankEntity         drive model, TerrainFollower
       └─ GroundVehicleEntity  static mounts, optional CanMove
```

### Seats

Vanilla only knows an ordered passenger list, so `AircraftEntity` keeps a seat map: one entity id
per seat, synced as a string. Crew seats come first (seat zero is the pilot and decides
`getControllingPassenger`), then racks, which is also the index space `ExclusionSeat` uses. The
server assigns a seat when a passenger is added, frees it when the passenger leaves, and moves
players between seats without remounting them. Occupants are saved by UUID, so a reloaded world
puts everyone back where they sat.

Racks reuse the passenger mechanism: a carried vehicle is a passenger in a rack seat. While it is
a passenger it does not simulate, and the carrier positions and orients it every tick.

### Moving parts and animation

`PartState` is a port of `MCH_Parts`: canopy, landing gear, hatch, VTOL nozzle and wing each own
one bit of a synced integer, the same bit layout the original used. Only the server flips a bit
(after checking the original's conditions); every side then animates the 0 to 90 degree angle
locally, so no angles are sent.

`AnimationState` holds what is purely visual: wheel roll and steering, track run and rollers,
spinning parts and each seat's view direction relative to the hull. It is derived on every client
from synced throttle, key bits and the crew's own rotations.

### Ground vehicles

`TerrainFollower` replaces the original's invisible wheel entities. Each `SetWheelPos` wheel and
its mirror image is a one block collision box moved with vanilla's collision code; it is dragged
along with the hull, falls onto the terrain and climbs steps up to 1.5 blocks. Each left/right pair
spans a plane with the centre of gravity, and the weighted average normal gives the target pitch
and roll, limited per tick by `OnGroundPitchFactor` and `OnGroundRollFactor`.

### Authority and networking

The rework uses the vanilla vehicle model rather than the original's custom packets:

```
 pilot client                              server                        other clients
 ────────────                              ──────                        ─────────────
 CrewController reads keys + mouse
 <kind>Entity.simulate()
 move(...)
   ── ServerboundMoveVehiclePacket ──▶  validates movement
   ── PilotStatePacket(throttle, ──────▶ acceptPilotState()
         reverse, roll, hover, keys)     synched data ────────────────▶ interpolate pose,
                                         entity tracking ─────────────▶ read throttle/roll/keys
 any crew member
   ── CrewActionPacket(gear, fold, ────▶ checks seat and conditions,
         vtol, eject, seat, racks)       flips part bits / seat map ──▶ animate locally
```

`isLocalInstanceAuthoritative()` decides where `simulate()` runs: on the pilot's client while a
player is at the controls, on the server otherwise (a parked or abandoned helicopter still falls
and its throttle winds down). Position, yaw and pitch travel in the vanilla packet, which the
server already checks for speed and collisions. Roll, throttle, hover and key state have no
vanilla field, so `PilotStatePacket` carries them. The server accepts it only from the controlling
passenger of that exact entity, and ignores non-finite values.

The flying client never waits for a server round trip, which is what makes mouse flying feel
direct.

### Flight model

`HelicopterEntity.updateMotion` is a port of `MCH_EntityHeli.onUpdate_Server`:

* gravity from the definition, separate in water;
* lift `(lift * 0.025 + 0.03) * throttle`, where `lift` falls linearly to zero as pitch plus roll
  approaches 83 degrees;
* forward thrust `0.1 * currentSpeed * pitch^3 / 30000 * throttle` along the yaw;
* a speed cap from `Speed`, with `currentSpeed` spooling up over about 35 ticks;
* ground friction and auto levelling.

Rotation input is applied in the aircraft's own frame (`AircraftRotation.applyLocal`), which
matches the original matrix chain: pulling back while banked turns instead of only raising the nose.

`PlaneEntity` and `TankEntity` port `MCP_EntityPlane` and `MCH_EntityTank` the same way. Their
original attitude rules ran once per rendered frame with a filtered partial tick of about 0.5; they
now run three times per tick with that value, which reproduces a 60 FPS client exactly.

Pilot gunner mode is a synced flag on `AircraftEntity`, switched by the server on a
`CrewActionPacket` and dropped there when its conditions stop holding. As in the original,
`isHovering()` is true in hover mode or gunner mode, so a helicopter whose pilot mans the guns
simply hovers. `PlaneEntity` instead runs the original's autopilot: pitch and roll ease out, the
yaw turns by `AutoPilotRot` each frame, throttle input is ignored and vertical motion is damped
instead of following gravity, the same level off the target drones use. On the client the pilot
stops steering with the mouse, looks around within the seat's pitch limits from the first
`CameraPosition`, and gets the HUD of seat one.

## Weapons

Package `weapon`, with the projectiles in `entity.projectile`. A port of the original's `weapon`
and `flare` packages and the weapon half of `MCH_EntityAircraft`.

* **WeaponDefinition** (`content.weapon`) reads `assets/mcheli/weapons`. `ContentRegistry` loads it
  before the vehicles and drops `AddWeapon` lines naming a weapon that does not exist.
* **WeaponSet** is one selectable weapon of a vehicle: the mounts of consecutive `AddWeapon` lines,
  with the magazine, reserve, reload, heat, mode, recoil and barrel spin they share. Firing goes
  through a `Trigger`, so the same set logic serves both sides.
* **VehicleWeapons** belongs to each `AircraftEntity`: the sets, the weapon each seat has selected
  (synced as a string), which mounts just fired (a synced bit mask that drives muzzle effects,
  recoil and hidden missiles on every client), turret aim, weapon bays and the `FlareDispenser`.
* **ServerFiring** is the server half of each weapon type's `shot`; it spawns the projectile.
  `ProjectileEntity` and its subclasses fly, home and fuse as the original bullet classes did.
* **Explosions** computes entity damage with the original curve and `DamageFactor`, then lets a
  vanilla explosion of the block size break blocks, so game rules and protection mods apply.

```
 crew client                                   server
 ───────────                                   ──────
 WeaponController (use key held)
 VehicleWeapons.use(client trigger)
   ready? loaded? locked? target in sight?
   ── FireWeaponPacket(weapon, option1, ─────▶ checks seat and selection
         option2)                              VehicleWeapons.use(ServerFiring)
                                                 spawns ProjectileEntity ──────▶ all clients
                                               synced selection + fire mask ───▶ effects, aim
 ◀─────────────────── WeaponStatePacket(ammo, reserve), to the crew only
```

Like the original, the client fires first: its weapon copy runs with slightly longer intervals, so
it never asks faster than the server accepts, and it is where lock-on happens. The server trusts
only the two option values (a locked entity id and a fire mode) and repeats every other check.

Damage enters through `AircraftEntity.hurtServer`, a port of `attackEntityFrom`. Projectile rays
are clipped against the main hit box and every `BoundingBox` (`clipHitBoxes`); the box struck
first lends its damage factor to the armour calculation. A vehicle at zero hit points is destroyed:
it burns, spins down or falls, explodes on impact and despawns after 600 ticks (30 seconds; the
original used 500). Until then each client keeps the wreck burning (`wreckFire`): flames, a
flickering `mcheli:glow` and embers, dying down over the last 100 ticks it has seen, and nothing
under water.

A hit neither tints the vehicle red nor calls `markHurt`: the vehicle a player drives is simulated
on their client, and resending the server's motion on every hit snapped it back a step. For the
same reason `Explosions` does not push a driven vehicle, and pushes an empty one at a quarter of
the mob knockback.

Impacts are measured from the position each tick, so they work whichever side simulates the
vehicle. Besides the original's tilt and submersion damage, flying into terrain or another vehicle
at speed is a crash, and tanks and cars (`takesGroundImpacts`) are hurt by walls and long drops by
the curve in `GroundImpact`: a share of their hit points that grows with the square of the speed
or height between a harmless and a fatal value, before armour. A vehicle wrecked by any of these,
or a falling wreck that lands, goes up through `crashExplosion`: the fuel sized explosion of the
original plus `Explosions.crashFireball`, which only draws a bigger fire.

On the way down, a shot down plane or helicopter goes through `crashSpin`, with the rates from
`physics.CrashSpin` in `common`: a plane eases its nose down to a 55 degree dive while its roll and
yaw rates build up, a helicopter spins up to 24 degrees a tick and wobbles in pitch and roll. The
direction and the wobble phase come from the entity id, so every side computes the same spin
without syncing anything; the server simulates the wreck and its rotation reaches clients as usual.

### Fuel and equipment

Fuel lives in the synched `DATA_FUEL`, saved as `Fuel`. The server burns it once a second in
`burnFuel` (the original's `updateFuel`) and fuel trucks share it in `supplyFuel`; whichever side
simulates the vehicle reads `hasFuel()` and lets the throttle run down when the tank is empty, so
the pilot's client and the server agree without another packet. Vehicles saved before fuel
existed load full.

The fuel can, wrench and parachute are plain items (`item.FuelItem`, `WrenchItem`,
`ParachuteItem`). Fuel and wrench act in `AircraftEntity.interact` before boarding, which also
covers clicks on the hull outside the main box (they arrive through `BoardVehiclePacket`).
`ParachuteEntity` is the original's `MCH_EntityParachute`: server simulated, ridden by the
player, steered by the rider's forward input from `ServerPlayer.getLastClientInput`. Its
`Kind` picks the model: the round canopy for the item, the seat for ejection (whose canopy opens
once it falls), the bare canopy for drops.

## Client

**CrewController** samples keys each client tick and the mouse every frame, for whichever seat the
player occupies. For helicopter and plane pilots vanilla turns the view first; the controller
converts the change into aircraft rotation, then snaps the view back onto the nose. Everyone else
looks around freely within their seat's limits (gunner pitch limits, fixed rotation seats, the
tank's free look). Through `ViewportEvent.ComputeCameraAngles` it tilts the view of every seat
with the hull: `AircraftRotation.cameraRoll` keeps the hull's up axis at the top of the screen as
seen along the rider's own view, so a pilot looking ahead gets the hull roll and a passenger
looking out of the side gets the hull pitch. Note that `Camera.setRotation` banks left for a
positive roll, the opposite of the hull's convention. For gunner sights and camera positions the
controller moves the first person eye with `Camera.setPosition`, looked up once through
`ObfuscationReflectionHelper` and switched off with a warning if that ever fails.

**AircraftRenderer** follows the 26.x render state pattern for every kind. `extractRenderState`
copies the interpolated pose, part angles and animation into an `AircraftRenderState`, and
`PartRenderer` submits the hull, the kind's own parts and the common parts in the original order
through `submitCustomGeometry`. Entity render types draw quads, so `MeshRenderer` sends every
triangle as a quad with a repeated last vertex. `GlassSplit` sorts each part's triangles by the
texture alpha under them: opaque ones draw through `entityCutoutCull`, glass draws blended through
`entityTranslucentEmissive`, the one entity type that does not write depth, shaded by hand from the
light at the vehicle. Without that, glass drawn before water hid the water behind it.

Back faces are culled, as in the original. The models build thin panels from back to back faces,
often textured differently on each side, and the 26.x `entityCutout` type draws both, which makes
them fight for the same depth and flicker as the view moves. The glass type cannot cull, so
`MeshRenderer.emitFrontFaces` drops glass triangles that face away from the camera on the CPU:
entities are submitted in world-aligned space with the camera at the origin, so that is a sign
test on each transformed triangle. Both loaders keep the original's counter-clockwise winding.

**ExplosionEffect** is the original's explosion look, played on each client from an
`ExplosionEffectPacket` that every MCHeli explosion sends to players within 256 blocks. Crashes
set the packet's `crash` flag for a larger show of our own: the same smoke shell drawn a little
above the impact, a cluster of fireballs, a climbing plume of signal smoke and burning fuel,
capped at size 12 to keep the particle count in check.

**Night glow** is our own. `Glow` holds two additive pipelines that test depth but never write it
(the lightning blend, so overlapping glows add up and never cut holes in the smoke behind them),
falling back to the vanilla translucent OIT pipelines when Fabulous graphics are on. It draws
camera facing halos and tracer ribbons for `ProjectileRenderer` (rounds and rocket motors) and
`FlareRenderer` (burning flares, including crash debris), and is the layer of the `mcheli:glow`
particle that `ExplosionEffect` spawns for the flash, the embers and the lingering crash fire.
How bright a glow gets comes from `effect.NightGlow` in `common`: the block light, sky light and
the sky darkening at that spot, so it is faint at noon and full at night, in caves and under
water. `ClientConfig` (`config/mcheli-client.toml`) turns it off per player.

Iris replaces every vanilla pipeline with its own shader programs and draws unknown ones with a
program whose vertex layout does not match. `IrisCompat` looks up `IrisApi` by reflection on the
first client tick (Iris is optional, so nothing is compiled against it) and assigns the halo
pipeline to `EMISSIVE_ENTITIES` and the particle pipeline to `PARTICLES_TRANSLUCENT`.

**VehicleCamera** holds the local crew member's zoom and camera mode, the client half of the
original's `MCH_Camera`. Zoom narrows the field of view through `ViewportEvent.ComputeFov`; it is
offered on the fold key whenever the seat looks through a sight, as the original did. Night vision
asks the server through `CrewActionPacket`, and `AircraftEntity` keeps a short ambient night
vision effect topped up on that seat's occupant, removing it again on leaving (unless a longer
effect, such as a potion, was there).

**CrewController** also hides the first person hand through `RenderHandEvent` wherever the original
did: for the pilot, and for anyone looking through a camera or with a weapon selected.

**EngineSoundPlayer** starts one looping `EngineSound` per nearby vehicle. Volume and pitch follow
each kind's original curve; listeners outside the vehicle hear it from a point scaled towards
them, which stretches the normal 16 block range to `SoundRange`.

**WeaponController** turns the use key into firing (cancelling the vanilla item use while the seat
has a weapon), the pick block button and G into weapon switching, and drives lock-on.
**ProjectileRenderer** and **A10Renderer** draw `models/bullets` and the close air support A-10,
tinted and full bright, with back faces culled.

### HUD

The original's HUDs are scripts in `assets/mcheli/hud`: lines such as `DrawLine`, `DrawTexture`
and `DrawString` whose numbers are expressions over about fifty variables (`hp_rto`, `plyr_yaw`,
`reloading` and so on), with `If`/`EndIf`, `Call` and `Exit`. The pure `hud` package parses them:
`HudScript.parse` turns a file into `HudCommand` records, compiling every expression once with
`Expression`. That replaces the `mcheli.eval` library, a copy of a general purpose evaluator that
re-parsed each expression every frame; only the arithmetic the scripts use is kept, with the
library's `evalDouble` semantics (all doubles, shifts as powers of two, `&&` returning an operand).
`HudVariable` lists the variables and `TextArg` the typed values `DrawString` can format. A unit
test parses every bundled script and checks each variable, texture and called script exists.

On the client, `HudScripts` loads the scripts through the resource manager (F3 + T reloads them),
and **WeaponHud**, the HUD layer, picks the script for the player's seat: the definition's `HUD`
line or the kind's default, or `tv_missile` while flying a TV missile. `HudRenderer` runs it
against `HudState`, which ticks the radar sweep (mobs within 64 blocks, as `MCH_Radar` did), the
altitude and the control stick, and fills in the variables each frame. `HudCanvas` draws the
original's GL lines, stippled lines, points and textured quads as unit quads stretched and turned
by the GUI pose, since the 26.x GUI takes whole number coordinates but keeps the pose of each
element. In third person, for a destroyed vehicle or when the seat's script does not exist, the
layer shows a plain text readout instead.

**RiderPose** tilts players riding a vehicle with its hull, about their hip, through
`RenderLivingEvent.Pre`; the camera eye follows the same up axis.

**MeshCache** parses models on first use and is cleared on resource reload (F3 + T).

## Assets

The vehicle content is EMB4's and is All Rights Reserved, so the jar carries none of it. Players
supply the original 1.0.3 zip, and the mod converts it on their machine:

```
<game dir>/mcheli/MC1.7.10_mcheli_1.0.3.zip      downloaded by the player (any *.zip is tried)
        │  LegacyContent.prepare: import when import.properties does not match the zip
        ▼
<game dir>/mcheli/imported/
  assets/mcheli/...                              resource pack half
  data/mcheli/recipe/*.json                      data pack half
  import.properties                              importer format, zip path, size, time
```

`content.LegacyImporter` is pure Java and does the conversion: lower-case paths (26.x identifiers
forbid upper case and `#`), the singular `textures/item` and `textures/block` folders, JSON
language files, `sounds.json` with the original grouping rule, an item model per item texture, and
the recipes. It writes into `imported.tmp` and swaps the folder in only when it succeeds: the
old import is first moved to `imported.old` and deleted last, and the renames are retried for a
few seconds, because on Windows a just-deleted folder keeps its name while a file watcher or
virus scanner still holds a handle in it. Bumping `LegacyImporter.FORMAT` makes every install
convert again (1.1.0-beta.1 bumped it to 2 for the generic item recipes).

### Runtime assets

Vehicle content can change while the game runs (`/mcheli reload`, content packs arriving from a
server, and the workshop later), and a resource reload is slow and does not exist on a server.
The client therefore reads vehicle assets through `client.RuntimeAssets`: the resource manager
first, so resource packs keep the last word, then the runtime roots, the `assets/mcheli` folders
of the content packs in use.

- **Models**: `MeshCache` opens them through `RuntimeAssets.open`; clearing the cache is enough
  for edited or new files to load on the next frame.
- **Textures**: the renderer asks `RuntimeAssets.texture(id)` for every texture it binds. A
  texture only a runtime root has is uploaded as a `DynamicTexture` on first use;
  `refreshTextures` uploads every vehicle texture again from disk. A resource reload releases
  ours so the texture manager takes over again.
- **Sounds**: `RuntimeSoundInstance` (the engine loop extends it) plays through the sound manager
  when a resource pack defines the event, and otherwise streams the OGG file a runtime root holds
  for it through the loader's sound stream hook (`SoundInstance.getStream` on Forge and NeoForge,
  `getAudioStream` on Fabric), with no `sounds.json` entry and no reload.

`CommonHooks.packSource` adds the folder (through `AddPackFindersEvent`, or a mixin on Fabric) for
both pack types, as a required
pack at the bottom of the list: it is enabled in every world, cannot be turned off, and regular
resource packs still override it. Minecraft merges lang files across packs, so the item names
from the import and the rework's own keys in the jar's lang files sit side by side;
`LegacyImporterTest` checks they never share a key.

Without a zip or an earlier import, `ContentRegistry` is empty: the game starts with no vehicles
and `ClientHooks.loggedIn` tells the player in the chat where the zip goes. A zip that fails to import is
logged and reported the same way.

The repository holds none of the original files. The unit tests get them the way players do:
`TestResources` imports the zip from `Mod/` once into `build/legacy-content`, so the tests run
against exactly what the importer produces. Where the zip is missing (CI), the content tests are
skipped and only the tests that build their input in code run. As a guard, the `jar` task still
excludes everything under `assets/mcheli` except `lang` and the generic items' fallback models,
since `build/sourceSets/main` can hold copies from older builds.

## Testing

Unit tests live in `common/src/test` and run with `./gradlew :common:test` (or `build`). Platform
code has no unit tests; it is covered in game. Apart from targeted cases, the tests load
every shipped helicopter definition and model and check that each rotor part referenced by a
definition exists in its model. New content is validated simply by adding it. In-game behavior is
covered by the manual plan in [TESTING.md](TESTING.md).
