# MCHeli Rework

<img src="docs/images/mcheli-rework-icon.png" alt="MCHeli Rework icon" width="160">

Helicopters, planes, tanks and ground vehicles for **Minecraft 26.3** and **1.21.1**, on **Forge**,
**NeoForge** and **Fabric**.

MCHeli Rework is a ground-up port of MCHeli 1.0.3, a mod that stopped at Minecraft 1.7.10. Every
system is rewritten for the modern game, while the original vehicle definitions, models,
textures and sounds load unchanged.

> **Status:** Phase 3 of 4 (`1.0.0-alpha.3`). Every helicopter, plane, tank, car and ground vehicle
> can be placed, crewed and driven with the original handling, and all 229 weapons fire: guns,
> rockets, bombs, guided missiles, torpedoes, close air support, flares, with damage, armour and
> destroyed wrecks. The original's scripted HUDs and the equipment items follow in phase 4; see the
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

Phase 8 in a development build (`1.1.0-beta.4`): hit boxes that cover the whole vehicle, and shot
down planes that spiral in.

| | |
|---|---|
| ![The MD-90 with F3+B on: cyan boxes from its definition along the fuselage and wings, amber boxes from its model filling the nose, fin and tail](docs/images/phase-8/md-90-hull-boxes.png) | ![An EC665 Tiger seen from above with F3+B on, its hull covered in amber and cyan boxes and none in the rotor disc](docs/images/phase-8/ec665-hull-boxes.png) |
| The MD-90's hull: the definition's boxes (cyan) and the ones made from its model (amber) | The EC665's hull; the rotor disc stays clear |
| ![The S-75 sight with an SA-2 selected, locked onto a plane overhead](docs/images/phase-8/sa-2-lock-on-plane.png) | ![The SA-2 bursting on the plane in a cloud of smoke and debris](docs/images/phase-8/sa-2-hits-plane.png) |
| An S-75 locks onto a plane | The SA-2 bursts on it |
| ![Burning debris trailing down from the sky behind the plane as it spirals towards the ground](docs/images/phase-8/plane-crash-spin.png) | ![The wreck exploding on the ground in a desert village, debris arcing out](docs/images/phase-8/plane-crash-site.png) |
| The plane rolls into a nose-down spiral | It goes up where it comes down |

Phase 6 in a development build (`1.1.0-beta.2`): night glow. Tracers, rocket motors, flares and
explosions light up the dark, and a wreck keeps burning until it disappears.

| | |
|---|---|
| ![An SA-2 closing on a helicopter in the night sky, its motor glowing](docs/images/phase-6/sa-2-night-intercept.png) | ![The shot down helicopter exploding on a hilltop at night in a bright flash](docs/images/phase-6/helicopter-crash-night.png) |
| An SA-2 intercept at night | The helicopter goes up in a flash of light |
| ![Glowing embers and burning debris arcing over the burning wreck at night](docs/images/phase-6/crash-embers-night.png) | ![A torpedo running towards a patrol boat at dusk](docs/images/phase-6/torpedo-run-dusk.png) |
| Burning debris arcs over the wreck | A torpedo run at dusk |
| ![The patrol boat cooking off in a fireball against the sunset](docs/images/phase-6/patrol-boat-cook-off-dusk.png) | ![Glowing debris raining down around the sinking patrol boat](docs/images/phase-6/burning-debris-dusk.png) |
| The patrol boat cooks off | Glowing debris rains down as it sinks |

Phase 5 in a development build (`1.1.0-beta.1`): a patrol boat hit by a torpedo cooks off and
sinks.

| | |
|---|---|
| ![A patrol boat exploding on a river after a torpedo hit, burning debris arcing over the shore](docs/images/phase-5/patrol-boat-cook-off.png) | ![The wreck of the patrol boat resting on the riverbed, listing and still smoking](docs/images/phase-5/patrol-boat-wreck.png) |
| Torpedo hit: fuel and ammunition cook off in three blasts | The wreck settles on the bottom, still smoking |

Phase 4 in a development build: the original's scripted HUDs, pilot gunner mode and lock-on.

| | |
|---|---|
| ![A helicopter cockpit in flight with the helicopter HUD and radar](docs/images/phase-4/helicopter-hud.png) | ![The S-75 launcher's sight with the SA-2 selected](docs/images/phase-4/s-75-launcher.png) |
| Helicopter HUD: heading, pitch ladder, weapon, radar | The S-75 launcher with its SA-2 Guideline |
| ![The AC-130 gunner view with another plane in the sight](docs/images/phase-4/ac-130-gunner-mode.png) | ![The AC-130 gunner HUD over the jungle](docs/images/phase-4/ac-130-gunner-hud.png) |
| AC-130 gunner mode: the autopilot circles while the pilot aims the guns | The gunner HUD with the Bofors 40 mm selected |
| ![An SA-2 exploding on a helicopter right above the launcher](docs/images/phase-4/sa-2-strike.png) | ![The smoke and burning debris of an SA-2 hit](docs/images/phase-4/sa-2-burst.png) |
| An SA-2 strikes a helicopter over the launcher | The burst of a hit in mid-air |

Phase 3 in a development build: weapons, damage, crashes and the explosion effect.

| | |
|---|---|
| ![A T-90 burning after a hit from another T-90](docs/images/phase-3/tank-duel.png) | ![A shot down helicopter exploding as it hits the ground](docs/images/phase-3/helicopter-crash.png) |
| Tank duel: the loser burns | A shot down helicopter explodes on impact |
| ![An M61 Vulcan bringing down a helicopter](docs/images/phase-3/vulcan-shoot-down.png) | ![A damaged SH-3 Sea King smoking on the water](docs/images/phase-3/damaged-sea-king.png) |
| An M61 Vulcan bringing down a helicopter | A damaged Sea King smokes, afloat on its floats |
| ![The view from a T-90 turret with the weapon readout](docs/images/phase-3/tank-gunner.png) | ![Water seen through a plane's canopy](docs/images/phase-3/canopy-over-water.png) |
| From the T-90 turret, reloading the main gun | Glass canopies show the water behind them |

Phase 2 in a development build: tanks, cars, ground launchers and planes with their moving parts.

| | |
|---|---|
| ![A tracked tank and an eight-wheeled armoured car](docs/images/phase-2/tank-and-armoured-car.png) | ![Tanks, an armoured car and an S-75 missile launcher](docs/images/phase-2/ground-forces.png) |
| A tracked tank and an eight-wheeled armoured car | Ground forces next to an S-75 launcher |
| ![Tornado with its landing gear down](docs/images/phase-2/tornado.png) | |
| Tornado on the ground, gear down and stores on its pylons | |

Phase 1 running on Minecraft 26.3 with Forge 66.1.0.

| | |
|---|---|
| ![Cockpit view while flying, banked to the right](docs/images/phase-1/cockpit-in-flight.png) | ![SH-3 Sea King hovering](docs/images/phase-1/sh-3-hovering.png) |
| Flying from the cockpit, with camera roll | SH-3 Sea King in the air |
| ![SH-3 Sea King climbing](docs/images/phase-1/sh-3-climbing.png) | ![The MCHeli Helicopters creative tab](docs/images/phase-1/creative-tab.png) |
| Main and tail rotors spinning | All 33 helicopters in the creative tab |

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

| | Minecraft 26.3 | Minecraft 1.21.1 |
|---|---|---|
| Forge | 66.1.0 or newer in the 66.x line | 52.1.0 or newer |
| NeoForge | 26.3.0.99 or newer | 21.1.200 or newer |
| Fabric | Loader 0.20 and Fabric API | Loader 0.16 and Fabric API |
| Java | 25 (bundled with the launcher) | 21 (bundled with the launcher) |

## Installing

1. Install Forge, NeoForge or Fabric for Minecraft 26.3 or 1.21.1. On Fabric, also install
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
and configs are the same files on all three. Do not open a 26.3 world in 1.21.1.

## Controls

Take a vehicle from one of the MCHeli creative tabs (helicopters, planes, tanks and cars,
ground vehicles), right-click a block to place it, and right-click anywhere on the vehicle to
board. The first player aboard is the pilot; a closed canopy opens on the first click and closes once the pilot
is in. Every key below can be rebound under Controls, category MCHeli.

In survival, vehicles are crafted on a crafting table with the original's recipes (mostly iron
blocks and ingots, glass, redstone and dyes). They appear in the recipe book as soon as you join.
A few vehicles had no recipe in the original and are still creative only: the M1129, M26, MXT-MV,
fuel truck and ammo box, and the E-767, whose recipe needs the UAV station that is not ported yet.

**Helicopters**

| Input | Action |
|-------|--------|
| Mouse | Pitch, bank and turn. The view follows the nose. |
| W / S | Throttle up / down |
| A / D | Bank left / right |
| Space | Toggle hover mode. Hover holds altitude; W/A/S/D then slide the helicopter. |

Lift drops as the helicopter tilts, so pitch the nose down gently to pick up speed and level out
to climb.

**Planes**

| Input | Action |
|-------|--------|
| Mouse | Pitch, roll and turn |
| W / S | Throttle up / down; full throttle is needed to climb |
| A / D | Roll in the air, steer on the ground |
| B | Landing gear up / down (it also comes down by itself near the ground) |
| Z | Fold or sweep the wings, or open the hatch. Through a sight, zoom instead. |
| V | VTOL mode, for planes that have it |
| Space, twice | Eject, for planes with ejection seats |

**Tanks and cars**

| Input | Action |
|-------|--------|
| Mouse | Look around; the turret follows the view |
| W / S | Throttle; S brakes and then reverses where the vehicle can |
| A / D | Turn |
| Space (hold) | Hand brake |

**Everyone**

| Input | Action |
|-------|--------|
| Left Alt (hold) | Free look. On tanks it swaps between looking around and looking along the hull. |
| H | Pilot: next camera position, or gunner mode on vehicles that have it (the autopilot flies while you man the guns). Gunners: toggle the gunner sight. |
| R | Move to the next free seat |
| J | Load nearby vehicles onto your racks, or drive onto a carrier's rack |
| U | Unload the last vehicle from your racks, or leave the carrier |
| Z | Through a camera, gunner sight or gunner mode: zoom (2x, 4x and so on up to the vehicle's limit, then back) |
| K | Night vision, then thermal view, then back to normal, in vehicles that have night vision |
| I | The vehicle screen: fuel and parachute slots, reloading, and the cargo chest |
| Shift | Leave the vehicle |

**Weapons**

| Input | Action |
|-------|--------|
| Right mouse (hold) | Fire the selected weapon. Seeking missiles lock on while held and fire once locked. |
| Middle mouse or G | Next weapon for your seat; with Shift, the previous one |
| X | Fire mode, for weapons that have one (HE rounds, top attack, TV guidance) |
| C | Release flares (pilot and first crew seat) |

Each seat fires the weapons its vehicle assigns to it; the pilot also fires a gunner's weapons
while that seat has no player. Creative players have unlimited ammunition. A small readout at the
bottom right shows hit points, the weapon, ammunition, reload, heat, lock and flares.

**Fuel and equipment**

Vehicles burn fuel while the throttle is open; the HUD shows the tank and warns below 10%. With
an empty tank the engine winds down. Refuel with the **fuel can** from the Equipment tab:
right-click a vehicle to pour in up to 100 units, or put cans in the fuel slots of the vehicle
screen (I), which empty into the tank while the vehicle is on the ground. Right-click in the air
with coal or charcoal in your inventory to fill a can again. A fuel truck tops up every vehicle
around it, and an ammunition truck rearms them. The **wrench** repairs 10 hit points per click,
once a second; with Shift it switches the vehicle's paint. The **parachute** opens when used in
mid-air; hold W to glide forward. Ejection seats need a parachute in the vehicle screen's
parachute slots, one per ejection. All three are crafted with the original's recipes.

The vehicle screen also reloads weapons from your own items, one supply per click, while the
vehicle is on the ground; closing it loads every magazine. Vehicles with cargo space have a
**Cargo** button there, and Shift + right-click opens their cargo chest from outside. What a
vehicle carries drops when it is destroyed.

## Commands and server settings

| Command | Who | What it does |
|---------|-----|--------------|
| `/mcheli` | everyone | Where the vehicles were loaded from, and how many of each kind |
| `/mcheli list [helicopter\|plane\|tank\|vehicle]` | everyone | The loaded vehicles by name |
| `/mcheli give <players> <vehicle> [count]` | operators | Hands out vehicles, for example `/mcheli give @s ah-64` |
| `/mcheli packs` | everyone | The content packs in use, and any problems with them |
| `/mcheli reload` | operators | Reads the vehicle and weapon files and the content packs again without a restart, and sends players the new packs |

Each world has `serverconfig/mcheli-server.toml` (created on first start, edits apply while the
server runs):

| Setting | Default | Meaning |
|---------|---------|---------|
| `explosions.breakBlocks` | `true` | Weapons and crashes break blocks (the `mobGriefing` rule applies too) |
| `explosions.startFires` | `true` | Incendiary weapons and crashes start fires |
| `vehicles.damageFactor` | `1.0` | Multiplies the damage vehicles take; `0.5` makes them twice as tough |
| `vehicles.disabled` | `[]` | Vehicles that cannot be placed, by name, such as `["ah-64", "f-35b"]` |
| `vehicles.infiniteFuel` | `false` | Vehicles never run out of fuel |
| `weapons.infiniteAmmo` | `false` | Vehicle weapons never run out of ammunition |
| `packs.allowList` | `[]` | Content packs the server loads, by id; empty loads all of them |
| `packs.maxPackSize` | `8` | Largest content pack the server loads and sends to players, in MB |
| `packs.maxTotalSize` | `64` | Most the server loads and sends in all, in MB |

Each player also has `config/mcheli-client.toml`, which only changes what that player sees:

| Setting | Default | Meaning |
|---------|---------|---------|
| `effects.nightGlow` | `true` | Tracers, rocket motors, flares and explosions glow: faintly by day, brightly at night, in caves and under water |
| `packs.cacheSize` | `256` | How much the content packs downloaded from servers may take up in `mcheli/cache`, in MB |

## Content packs

Content packs add vehicles and weapons: drop a pack (a zip or a folder) into
`<game dir>/mcheli/packs`. On a server only the server needs it; players download the server's
packs automatically when they join. [examples/rework-example-pack](examples/rework-example-pack)
adds a small helicopter, the Rework Scout. To make your own, see
[docs/content/packs.md](docs/content/packs.md).

## Documentation

* [Changelog](CHANGELOG.md)
* [Roadmap](docs/ROADMAP.md): what each phase delivered, and what is planned for 1.1.0
* [Architecture](docs/ARCHITECTURE.md): how the mod is put together
* [Development](docs/DEVELOPMENT.md): building, running, testing and releasing
* [In-game test plan](docs/TESTING.md)
* Content authoring: [content packs](docs/content/packs.md), [modeling in Blockbench or Blender](docs/content/modeling.md), [vehicles](docs/content/aircraft.md), [weapons](docs/content/weapons.md), [HUD scripts](docs/content/hud.md)
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
