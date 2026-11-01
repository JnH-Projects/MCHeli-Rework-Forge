# Playing MCHeli Rework

How to fly, drive and fight, the equipment, and the commands and settings for servers. Installing
is covered in the [README](../README.md#installing).

* [Getting started](#getting-started)
* [Controls](#controls)
* [Fuel, repairs and the vehicle screen](#fuel-repairs-and-the-vehicle-screen)
* [Equipment](#equipment)
* [Commands and server settings](#commands-and-server-settings)

## Getting started

Take a vehicle from one of the MCHeli creative tabs (helicopters, planes, tanks and cars, ground
vehicles), right-click a block to place it, and right-click anywhere on the vehicle to board. The
first player aboard is the pilot; a closed canopy opens on the first click and closes once the
pilot is in. Every key below can be rebound under Controls, category MCHeli.

In survival, vehicles and equipment are crafted on a crafting table with the original's recipes
(mostly iron blocks and ingots, glass, redstone and dyes). They appear in the recipe book as soon
as you join. A few vehicles had no recipe in the original and are still creative only: the
M1129, M26, MXT-MV, fuel truck and ammo box, and the E-767, whose recipe needs the UAV station
that is not ported yet.

## Controls

### Helicopters

| Input | Action |
|-------|--------|
| Mouse | Pitch, bank and turn. The view follows the nose. |
| W / S | Throttle up / down |
| A / D | Bank left / right |
| Space | Toggle hover mode. Hover holds altitude; W/A/S/D then slide the helicopter. |

Lift drops as the helicopter tilts, so pitch the nose down gently to pick up speed and level out
to climb.

### Planes

| Input | Action |
|-------|--------|
| Mouse | Pitch, roll and turn |
| W / S | Throttle up / down; full throttle is needed to climb |
| A / D | Roll in the air, steer on the ground |
| B | Landing gear up / down (it also comes down by itself near the ground) |
| Z | Fold or sweep the wings, or open the hatch. Through a sight, zoom instead. |
| V | VTOL mode, for planes that have it |
| Space, twice | Eject, for planes with ejection seats |

### Tanks and cars

| Input | Action |
|-------|--------|
| Mouse | Look around; the turret follows the view |
| W / S | Throttle; S brakes and then reverses where the vehicle can |
| A / D | Turn |
| Space (hold) | Hand brake |

### Every seat

| Input | Action |
|-------|--------|
| Left Alt (hold) | Free look. On tanks it swaps between looking around and looking along the hull. |
| H | Pilot: next camera position, or gunner mode on vehicles that have it (the autopilot flies while you man the guns). Gunners: toggle the gunner sight. |
| R | Move to the next free seat |
| J | Load nearby vehicles and containers onto your racks, or drive onto a carrier's rack |
| U | Unload the last vehicle or container from your racks, or leave the carrier |
| Z | Through a camera, gunner sight or gunner mode: zoom (2x, 4x and so on up to the vehicle's limit, then back) |
| K | Night vision, then thermal view, then back to normal, in vehicles that have night vision |
| I | The vehicle screen: fuel and parachute slots, reloading, and the cargo chest |
| Shift | Leave the vehicle |

### Weapons

| Input | Action |
|-------|--------|
| Right mouse (hold) | Fire the selected weapon. Seeking missiles lock on while held and fire once locked. |
| Middle mouse or G | Next weapon for your seat; with Shift, the previous one |
| X | Fire mode, for weapons that have one (HE rounds, top attack, TV guidance) |
| C | Release flares (pilot and first crew seat) |

Each seat fires the weapons its vehicle assigns to it; the pilot also fires a gunner's weapons
while that seat has no player. Creative players have unlimited ammunition. A small readout at the
bottom right shows hit points, the weapon, ammunition, reload, heat, lock and flares.

## Fuel, repairs and the vehicle screen

Vehicles burn fuel while the throttle is open; the HUD shows the tank and warns below 10%. With
an empty tank the engine winds down. Refuel with the **fuel can** from the Equipment tab:
right-click a vehicle to pour in up to 100 units, or put cans in the fuel slots of the vehicle
screen (I), which empty into the tank while the vehicle is on the ground. Right-click in the air
with coal or charcoal in your inventory to fill a can again. A fuel truck tops up every vehicle
around it, and an ammunition truck rearms them. The **wrench** repairs 10 hit points per click,
once a second; with Shift it switches the vehicle's paint.

The vehicle screen also reloads weapons from your own items, one supply per click, while the
vehicle is on the ground; closing it loads every magazine. Vehicles with cargo space have a
**Cargo** button there, and Shift + right-click opens their cargo chest from outside. What a
vehicle carries drops when it is destroyed.

## Equipment

Everything here is in the MCHeli Equipment tab and crafted with the original's recipes.

**Parachute.** Use it in mid-air to open it; hold W to glide forward. Ejection seats need a
parachute in the vehicle screen's parachute slots, one per ejection.

**Smoke grenades** (M18, in red, green, violet and yellow). Hold use to draw and let go to throw:
the longer the draw, the further it goes. They bounce, and after two seconds pour out coloured
smoke for half a minute, to mark a landing zone or a target. Dispensers throw them too.

**GLTD target designator.** Place it on a block and right-click to sit behind it; Shift leaves.
The view moves to its sight, which turns up to 70 degrees either way.

| Input | Action |
|-------|--------|
| Right mouse | Mark the spot under the cross: an A-10 makes three gun runs over it |
| Z | Zoom: 2x, 4x, 8x, then back |
| K | Night vision, then thermal view, then back to normal |

The sight shows when the next A-10 can be called, about 22 seconds after the last one.

**Rangefinder.** Hold use to raise it; after a moment the scope shows the range to whatever it
points at, in metres.

| Input (scope up) | Action |
|-------|--------|
| Z / X (hold) | Zoom in / out, from 1.2x to 10x |
| C | What to spot: players and vehicles, monsters and mobs, or a point |
| Left mouse | Spot |

Spotting outlines every target of that kind in a narrow cone out to 400 blocks for 15 seconds
(mobs a minute), never your teammates, and uses one of ten charges. Marking a point sends its
coordinates to you and your team in chat, and is free. Craft the rangefinder with redstone to
charge it again.

**Container.** A crate with a large chest's worth of room: place it on a block, right-click to
open it. It floats and can be pushed. Vehicles whose racks take containers (the CH-47 and others)
load it with J and drop it with U; dropped with no ground in the 20 blocks below, it comes down
under a parachute. Punching it a few times breaks it and drops what is inside.

**Stinger and Javelin.** Shoulder launched missiles: the FIM-92 Stinger locks onto aircraft, the
FGM-148 Javelin onto ground vehicles and players in vehicles. Hold use to raise the sight and keep
the target in the middle until the tone turns steady and the sight reads LOCKED.

| Input (sight up) | Action |
|-------|--------|
| Left mouse | Fire at the locked target |
| Z | Zoom: the Javelin has two steps |
| X | Javelin: direct or top attack. Top attack climbs and dives onto the target, and needs 40 blocks of room. |

Each launcher holds one missile. Lower an empty one with a missile in your inventory and it
reloads in two seconds. Missiles are crafted two at a time.

**Tow chain.** Right-click the two ends with the chain, in either order, 2 to 16 blocks apart.
One end must be a vehicle, which does the pulling: a truck can drag a container or a broken down
car, and a helicopter can lift a container as a sling load. Right-click either end again to take
the chain off. Each step shows a message above the hotbar.

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

