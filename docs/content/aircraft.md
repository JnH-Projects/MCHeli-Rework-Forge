# Vehicle definition format

English translation of `Readme_Aircraft.txt` (2016/04/17) from MCHeli 1.0.3 by EMB4. The
untranslated original ships in the 1.0.3 zip as `mcheli/assets/mcheli/Readme_Aircraft.txt`.

Vehicle definitions are plain text files with one `Key = value` entry per line. Keys are case
insensitive, lines starting with `;` are comments, and keys such as `AddSeat` can repeat. Files
should be saved as UTF-8. The Rework also reads the Shift-JIS files the original shipped with.

Not every key is implemented in the Rework yet. [`ROADMAP.md`](../ROADMAP.md) lists which phase
brings which feature. Unknown keys are ignored and reported in the debug log, so a definition
written for the original always loads.

> The original could reload definitions and models in game through
> *R (supply screen) > MOD Option > Development > Reload aircraft setting*. Textures and sounds
> reload through the vanilla resource pack screen. The in-game reload is not ported yet; F3 + T
> reloads models in the Rework.

## Common keys

These apply to helicopters, planes, tanks and ground vehicles.

### Identity

| Key | Example | Meaning |
|-----|---------|---------|
| `DisplayName` | `AH-6 Killer Egg` | English item name. ASCII only. |
| `AddDisplayName` | `ja_JP, AH-6 ...` | Item name for another language. The file must be UTF-8 when it contains non-ASCII text. |
| `ItemID` | `28801` | Numeric item id. Ignored since Minecraft 1.7.2, accepted for old files. |
| `AddTexture` | `sh60-us-1` | Extra paint job, file name without extension. Repeatable. The default texture has the same name as the definition file. |
| `Category` | `W.A` | Sort key for the creative tab, for example `M.A` for military attacker. |

### Cameras and HUD

```
CameraPosition = 0.0, 1.1, 3.7
CameraPosition = 0.0, 1.1, 3.7, false
CameraPosition = 0.0, 1.1, 3.7, true, 30, 45
```

| Parameter | Meaning |
|-----------|---------|
| 1 to 3 | Camera position X, Y, Z. Several entries can be cycled with H. |
| 4 | `true` to always view from this camera. |
| 5 | Horizontal camera angle. |
| 6 | Vertical camera angle. |

| Key | Meaning |
|-----|---------|
| `CameraZoom = 3` | Maximum camera zoom. |
| `HUD = heli, heli_gnr, none, gunner` | HUD file per seat (`hud/<name>.txt`). `none` hides the HUD. Missing seats use the defaults below. |
| `EnableGunnerMode = true` | Allow switching to gunner mode. |
| `EnableNightVision = true` | Allow switching night vision and thermal view. |
| `EnableEntityRadar = false` | Fit a radar. |
| `DefaultFreelook = true` | Start in free look right after boarding. Mostly used for tanks. |
| `CameraRotationSpeed = 25` | Camera rotation speed. Tanks use it to limit turret traverse speed. |

Default HUDs when `HUD` is not set:

| Kind | Default |
|------|---------|
| Helicopter | `heli, heli_gnr, gunner, gunner, ...` |
| Plane | `plane, plane, gunner, gunner, ...` |
| Ground vehicle | `vehicle` only |

In gunner mode the pilot uses the HUD of seat 2. A single-seat aircraft with gunner mode should
therefore still list two HUDs, for example `HUD = heli, heli_gnr`.

### Movement

| Key | Meaning |
|-----|---------|
| `Speed = 0.6` | Top speed. Larger is faster. |
| `MotionFactor = 0.96` | Speed decay, 0.0 to 1.0. Smaller values slow the vehicle down harder. Small changes already have a visible effect. |
| `Gravity = -0.04` | Gravity on the vehicle. Negative values fall. |
| `GravityInWater = -0.04` | Gravity while in water. |
| `StepHeight = 2.5` | Height of blocks the vehicle can climb. |
| `MobilityYaw`, `MobilityYawOnGround` | Turn rate. `MobilityYawOnGround` only applies on land, not on water. |
| `MobilityRoll` | Roll rate. |
| `MobilityPitch` | Pitch rate. For ground vehicles this sets the elevation limits. |
| `MinRotationPitch` (-80 to 0), `MaxRotationPitch` (0 to 80) | Pitch limits. For helicopters and planes this also limits roll. |
| `MinRotationRoll` (-80 to 0), `MaxRotationRoll` (0 to 80) | Roll limits. For helicopters and planes this also limits pitch. |
| `ThrottleUpDown = 1.0` | How fast the throttle moves. Smaller values make take-off slower. |
| `ThrottleUpDownOnEntity = 2.0` | Throttle rate while the vehicle rides another entity: `ThrottleUpDown * carrier speed * ThrottleUpDownOnEntity`. On a minecart (top speed about 1.7) a factor of 2.0 gives 3.4, so take-off needs about a third of the distance. |
| `AutoPilotRot = -0.4` | Turn angle during auto pilot. 0 flies straight, negative turns left, positive turns right. |
| `OnGroundPitch = <angle>` | Pitch while parked. Tail draggers such as the A6M Zero sit nose up. |
| `CanMoveOnGround = false` | Forbid moving on the ground. |
| `CanRotOnGround = false` | Forbid turning on the ground. |
| `UnmountPosition = 3.0, 1.0, -2.0` | Where passengers are placed when they leave. |

### Seats

```
AddSeat =-0.45,  0.80,  1.20
AddSeat = 0.90, -0.50,  0.20, true
```

Every vehicle except UAVs needs at least one seat. The first seat is the pilot. Parameters are the
position X, Y, Z. The optional fourth parameter moves the seat with the pilot's view, which is
mainly used for tank turrets.

```
AddGunnerSeat = -0.45, 0.80, 1.20,   0.0, 2.00, -1.01,   true
AddGunnerSeat = -0.45, 0.80, 1.20,   0.0, 2.00, -1.01,   true, -60, 78, true
```

`AddGunnerSeat = seat X, Y, Z, camera X, Y, Z, can switch view, camera upper limit (-90 to 0), camera lower limit (0 to 90), follows turret`

A player in a gunner seat views through the camera. On the pilot seat the camera view is not
used. The camera position can be omitted, in which case `CameraPosition` applies. Without the
view switch flag (or with `false`) only the camera view is available; with `true`, H switches to
the player view. The tenth parameter moves the seat with the turret.

```
AddFixRotSeat = -0.45, 0.80, 1.20,   0.0, 2.00, -1.01,   true, -50, 40
```

`AddFixRotSeat = seat X, Y, Z, camera X, Y, Z, can switch view, fixed yaw, fixed pitch`

Like `AddGunnerSeat`, but the camera points in a fixed direction. The mouse does not move it.
Free look (Ctrl in the original) still works.

### Carrying other vehicles

Loading works if either side declares it: the carrier lists what it accepts with `AddRack`, or
the passenger names its carrier with `RideRack`.

```
AddRack = container,                 0.0, 1.4, -4.7,  0.0, 1.0, -16.1
AddRack = container / ah-64,         0.0, 1.4, -4.7,  0.0, 1.0, -16.1,  5.0, 20
AddRack = helicopter/vehicle / t-4,  0.0, 1.4, -4.7,  0.0, 1.0, -16.1,  5.0, 100000,  0.0, 0.0
```

| Parameter | Meaning |
|-----------|---------|
| 1 | Accepted entities, separated by `/`: `container`, `helicopter` (all helicopters), `plane` (all planes), `vehicle` (all ground vehicles) or a vehicle name such as `ah-64`, `t-4`, `s-75`. |
| 2 to 4 | Rack position X, Y, Z on the carrier. |
| 5 to 7 | Rack entrance X, Y, Z. Entities near this point can be loaded, and are unloaded here. On the AC-130 the rack sits inside the cabin and the entrance at the rear ramp. |
| 8 | Entrance radius. Optional from here on. |
| 9 | Altitude at which the parachute opens. A very large value means never. |
| 10 | Yaw of the carried entity. |
| 11 | Pitch of the carried entity. |

`RideRack = c5, 1` is set on the passenger: carrier name and rack number (starting at 1).

`ExclusionSeat = 15, 17` makes seats or racks mutually exclusive. Any number of indices can be
listed. With `ExclusionSeat = 3, 4, 5`, a mob in seat 3 blocks seats 4 and 5, a mob in seat 4
blocks 3 and 5, and so on. Racks count too, so a rack holding a container can keep a vehicle
off another rack.

Numbering assigns every seat first, in file order, and then the racks:

```
AddSeat        <- 1
AddRack        <- 4
AddGunnerSeat  <- 2
AddRack        <- 5
AddSeat        <- 3
AddRack        <- 6
```

Listing all seats before the racks keeps the numbers easy to follow.

### Weapons

```
AddWeapon = m230,     0.00, 0.90, 2.54,   0.0, 0.0, true, 2
AddWeapon = hydra70,  0.00, 0.90, 2.54,   0.0, 0.0, true, 1, 0,-60,60, 0,25
AddWeapon = m134,     1.48, 0.40, 1.54,   1.0, 0.0
AddWeapon = m134,    -1.48, 0.40, 1.54,  -1.0, 0.0
AddTurretWeapon = hydra70,  0.00, 0.90, 2.54,   0.0, 0.0, true, 1, 0,-60,60, 0,25
```

`AddWeapon = weapon file, X, Y, Z, yaw, pitch, usable by pilot, seat, DefaultYaw, MinYaw, MaxYaw, MinPitch, MaxPitch`

The weapon name is a file in `weapons/` without extension. Consecutive lines with the same weapon,
like the two `m134` lines, form one weapon that fires from each position in turn.

| Pilot flag, seat | Who can fire |
|------------------|--------------|
| `true, 2` | The player in seat 2, or the pilot while seat 2 is empty. |
| `false, 2` | Only the player in seat 2. |
| `false, 1` | Only the pilot (not recommended). |
| `true, 1` | Only the pilot. This is the default. |

`DefaultYaw` is the resting direction and should match the model. Seen from above, `MinYaw` is
how far the weapon turns counter-clockwise from `DefaultYaw` and `MaxYaw` how far clockwise.
`MinPitch` is the upward travel and `MaxPitch` the downward travel.

`AddTurretWeapon` differs only in that the firing position follows the turret rotation.

`TurretPosition = 0.0, 0.0, 0.25` sets the turret pivot. Avoid it if possible; ideally the model
puts the turret pivot at X = 0, Z = 0.

### Lights

```
AddSearchLight         = 0.71, -0.02, 0.02,   0x50FFFFFF, 0x10FFFFC0,   60.0, 20.0,   0, 0
AddFixedSearchLight    = 0.71, -0.02, 0.02,   0x50FFFFFF, 0x10FFFFC0,   60.0, 20.0,   0, 0
AddSteeringSearchLight = -0.52, 0.90, 1.76,   0x50FFFFFF, 0x00FFFFC0,   27.0, 15.0,   5, 0,   45
```

`AddSearchLight = X, Y, Z, start color, end color, length, end radius, yaw, pitch, steering angle`

| Key | Behavior |
|-----|----------|
| `AddSearchLight` | Follows the crew member's view. |
| `AddFixedSearchLight` | Always points the same way. |
| `AddSteeringSearchLight` | Turns with the wheels. Match the steering angle to the wheel steering angle. |

Yaw and pitch offset the beam direction for every kind.

`AddPartLightHatch = X, Y, Z, axis X, Y, Z, angle (-1800 to 1800)` adds a part that opens only
while the search light is on. Requires `AddSearchLight` or `AddFixedSearchLight`.

### Recipes

```
AddRecipe = " Y ", "YXY", " YD", X, iron_block, Y, iron_ingot, D, dye, 2
AddRecipe = "YXY", X, mcheli:ah-6, Y, redstone
AddShapelessRecipe = iron_block, iron_ingot, dye, 2
```

Each `AddRecipe` line adds one shaped recipe in the format of Forge's `GameRegistry.addRecipe`.
Each quoted string is one row of the crafting grid. Items use their registry names; vanilla items
may omit `minecraft:`, mod items need their namespace such as `mcheli:ah-6`. A number after an
item is its damage value, which 1.7.10 used for dyes and potions. `AddShapelessRecipe` adds a
shapeless recipe.

The Rework keeps these lines but will register recipes through the drafting table later, mapping
the old damage values to the flattened item names.

### Survival and damage

| Key | Meaning |
|-----|---------|
| `FlareType = 1` | 0 none, 1 standard, 2 for large aircraft, 3 sideways, 4 forward, 5 downward, 10 tank smoke dischargers. |
| `FlareOption = X, Y, Z` | Flare launch position. |
| `Float = true` | Floats on water. |
| `FloatOffset = -1.0` | Height offset while floating. Negative values allowed. |
| `SubmergedDamageHeight = 2` | No water damage up to this depth, 2 means up to 2 blocks. |
| `MaxHP = 100` | Durability. |
| `ArmorDamageFactor = 0.5` | Damage multiplier, 1.0 = 100%, 0.5 = 50%. |
| `ArmorMinDamage = 5` | Hits below this are ignored. |
| `ArmorMaxDamage = 500` | Hits above this are capped. With 100, a 300 point hit only deals 100. |
| `DamageFactor = 0.2` | Damage multiplier for players inside. 0.2 means 20%. The vehicle still takes the full hit. |
| `InventorySize = 18` | Slots in the cargo chest, up to 54. Sizes between rows round up to a whole row of 9. |
| `Regeneration = true` | Mobs from seat 2 onwards heal over time. |
| `Stealth = 0.5` | 0.0 to 1.0, default 0.0. Higher values make missile locks take longer and shorten lock range. |

`BoundingBox = center X, center Y, center Z, width, height, damage multiplier` adds a hit box that
only the mod's guns and missiles hit; blocks and entities ignore it. Turn on test mode to see it.
The multiplier defaults to 1.0; 0.5 halves damage, 3.0 triples it.

### Fuel and supply

```
MaxFuel         = 600
FuelConsumption = 0.5
```

`MaxFuel` is the tank capacity and `FuelConsumption` the fuel used per second, so flight time in
seconds is `MaxFuel / FuelConsumption`, here 1200 seconds.

| Key | Meaning |
|-----|---------|
| `FuelSupplyRange = 25` | Refuels every vehicle within 25 m. The supplier's own fuel does not drop, but it cannot refuel itself. |
| `AmmoSupplyRange = 35` | Rearms every vehicle within 35 m that stands on something, a tenth of each weapon's maximum every two seconds. |
| `RepairOtherVehicles = 20, 10` | Repairs every vehicle within 20 m by 10 hit points a second. The value is optional (default 10). |

### Rendering and sound

| Key | Meaning |
|-----|---------|
| `Sound = heli` | Engine sound, here `sounds/heli.ogg`. |
| `SmoothShading = false` | `false` for flat shading, `true` for smoothed normals. The `SmoothShading` option in `mcheli.cfg` overrides every vehicle. |
| `HideEntity = false` | Hide the riding players. |
| `EntityWidth = 0.9`, `EntityHeight = 0.9` | Render scale of riding mobs, -100.0 to 100.0. 0.5 halves them. |
| `EntityPitch = 45`, `EntityRoll = 20` | Render angle of riding mobs, -360 to 360. |
| `ParticlesScale = 0.1` | Size of dust and similar effects. |
| `RotorSpeed = 50.0` | Blade spin speed. Negative values reverse it but are not recommended. |
| `CanRide = false` | `true` (default) lets players board, `false` does not. |

### Drones

| Key | Meaning |
|-----|---------|
| `UAV = true` | Unmanned, the pilot seat cannot be used. Only a UAV station can spawn and fly it. Large UAVs cannot be flown from the portable controller. |
| `SmallUAV = true` | Small UAV, also controllable from the portable controller. The station controls every UAV. |
| `TargetDrone = true` | Planes only. An unmanned target that flies to low altitude after spawning and keeps circling. Only a UAV station can spawn it. |

### Paratroopers

| Key | Meaning |
|-----|---------|
| `EnableParachuting = true` | Lets mobs from seat 3 onwards jump out. Players in those seats jump with Space. |
| `MobDropOption = 0.0, 0.0, -11.5, 10` | Drop position X, Y, Z and drop interval in 1/20 s. |
| `ConcurrentGunnerMode = true` | Gunner mode stays available while a player sits in seat 2. |

### Animated parts

Animated parts are separate objects in the model. The original loaded them from files named after
the vehicle (`<name>_hatch0.obj`, `<name>_hatch1.obj`, and so on); later models put them inside the
main model as `$hatch0`, `$hatch1`. Parts without a model are simply not drawn.

| Key | Parameters | Model name |
|-----|------------|------------|
| `AddPartHatch` | X, Y, Z, axis X, Y, Z, angle 0 to 180 | `hatch?`. Opens and closes with Z. |
| `AddPartSlideHatch` | offset X, Y, Z | `hatch?`. Slides open. |
| `AddPartCamera` | X, Y, Z, follows yaw, follows pitch | `camera?`. Always faces the player, or the mob in seat 2 if there is one. |
| `AddPartRotation` | X, Y, Z, axis X, Y, Z, speed, always rotate | Spins at a constant rate. |
| `AddPartWeaponBay` | weapon names, X, Y, Z, axis X, Y, Z, angle 0 to 180 | `wb?`. Rotating weapon bay. |
| `AddPartSlideWeaponBay` | weapon names, offset X, Y, Z | `wb?`. Sliding weapon bay. |
| `AddPartCanopy` | X, Y, Z, axis X, Y, Z, angle 0 to 180 | `canopy?`. Rotating canopy. |
| `AddPartSlideCanopy` | offset X, Y, Z | `canopy?`. Sliding canopy. For compatibility a canopy without a number is treated as `canopy0`. |
| `AddPartThrottle` | X, Y, Z, axis X, Y, Z, angle 0 to 180, offset X, Y, Z | Moves with the throttle. Everything up to the angle is required. |

Weapon parts for helicopters and planes:

```
AddPartWeapon        = m230,       false, true, true,  -2.51,  1.29,  -1.51
AddPartWeapon        = m102_105mm, false, true, true,  -2.51,  1.29,  -1.51, 1.00
AddPartWeapon        = rehinmetall_apfsds / rehinmetall_he, false, true, false,  0.00, 2.10, 0.00, 0
AddPartTurretWeapon  = mg7_62mm,   false, true, true,  -0.83,  3.39,  -0.57, 0
AddPartRotWeapon     = m134_r50,   false, true, true,  -1.825, 1.475, -0.25, 1,0,0
AddPartWeaponChild   = false, true, 0.00, 0.5, 3.00
AddPartWeaponMissile = aim120,     false, false,false, -2.51,  1.29,  -1.51
```

* `AddPartWeapon = linked weapon (none if unlinked), hide in gunner mode, follows yaw, follows pitch, pivot X, Y, Z, recoil distance`
* `AddPartRotWeapon = linked weapon, hide in gunner mode, follows yaw, follows pitch, pivot X, Y, Z, spin axis X, Y, Z`
* `AddPartWeaponChild = follows yaw, follows pitch, pivot X, Y, Z`

The part turns with the weapon added through `AddWeapon`, within the same limits. Several weapon
names can be joined with `/`. The recoil distance is how far the barrel slides back.
`AddPartRotWeapon` is for rotary guns and spins while firing. Models are named `weapon?`.

`AddPartWeaponChild` adds a sub part to the `AddPartWeapon` on the line above, and can repeat.
Children are named `weapon?_0`, `weapon?_1` (with `?` the parent index) and inherit the parent's
linked weapon and gunner flag.

`AddPartWeaponMissile` hides the part after firing until the weapon is ready again, for missiles
and bombs. `AddPartTurretWeapon` follows the turret, otherwise it matches `AddPartWeapon`.

Landing gear:

```
AddPartLG         = X, Y, Z, axis X, Y, Z, angle 0 to 180 [, axis X, Y, Z, angle 0 to 180]
AddPartLGRev      = X, Y, Z, axis X, Y, Z, angle 0 to 180 [, axis X, Y, Z, angle 0 to 180]
AddPartSlideRotLG = offset X, Y, Z, X, Y, Z, axis X, Y, Z, angle 0 to 180
AddPartLGHatch    = X, Y, Z, axis X, Y, Z, angle 0 to 180 [, axis X, Y, Z, angle 0 to 180]
```

The gear extends and retracts automatically around take-off. Models are named `lg?`.

| Key | While retracting |
|-----|------------------|
| `AddPartLG` | 0 to 90 |
| `AddPartLGRev` | 90 to 0 |
| `AddPartSlideRotLG` | 0 to 90 |
| `AddPartLGHatch` | 0 to 90 to 0, only open while the gear moves |

### Tracks and wheels

| Key | Meaning |
|-----|---------|
| `TrackRollerRot = 30` | Road wheel spin speed. Negative values spin backwards. |
| `AddTrackRoller = -1.72, 0.77, 5.04` | Road wheel position. Negative X is the right side, positive X the left. Moves like the track but works without one. |
| `PartWheelRot = 40` | Tyre spin speed. |
| `AddPartWheel = X, Y, Z, steering angle [, axis X, Y, Z, pivot X, Y, Z]` | Tyre. The steering angle is the maximum Y rotation while turning. The axis defaults to (0, 1, 0). |
| `AddPartSteeringWheel = X, Y, Z, axis X, Y, Z, max angle` | Steering wheel. |

```
AddCrawlerTrack = false, 0.37, -2.09,  1.03/-3.41, 0.72/-3.57, 0.37/-3.42, ...
```

`AddCrawlerTrack = flip track, link spacing, track X, pivot Y/Z, pivot Y/Z, ...`

If the track renders inside out or moves strangely, toggle the flip flag. Use `false` when the
pivots are listed clockwise seen from the tank's left side, `true` otherwise. Road wheels are not
required. Test mode shows the pivots as red to blue dots.

## Helicopters

Files: `helicopters/<name>.txt`, `models/helicopters/<name>.mqo` (or `.obj`),
`textures/helicopters/<name>.png`, `textures/item/<name>.png`. All names lower case.

| Key | Meaning |
|-----|---------|
| `EnableFoldBlade = false` | Allow folding the blades. |
| `AddRotor = 6, 60, 0.00, 3.35, 0.00, 0.0, 1.0, 0.0, true` | Rotor: blade count, angle between blades, position X, Y, Z, axis X, Y, Z, foldable. Repeatable; usually the main rotor first, then the tail rotor. Only the first rotor can fold. Model part `$blade?`. |
| `AddRepellingHook = 0.60, 2.75, -14.21, 30` | Rappelling hook position X, Y, Z and the interval between mobs. |

`AddRotorOld` exists for very old models only. Do not use it.

## Planes

Files: `planes/<name>.txt`, `models/planes/<name>.mqo`, `textures/planes/<name>.png`,
`textures/item/<name>.png`.

| Key | Meaning |
|-----|---------|
| `AddPartRotor = X, Y, Z, axis X, Y, Z, angle (-180 to 180)` | Tilting rotor. Does not spin with the throttle; it rotates in VTOL. Model `rotor?`, only needed for tilt rotors such as the Osprey. |
| `AddBlade = blade count, angle between blades, X, Y, Z, axis X, Y, Z` | Blades of the preceding `AddPartRotor`. Model `blade?`. |
| `AddPartWing = X, Y, Z, axis X, Y, Z, angle 0 to 180` | Folding wing, model `wing?`. |
| `AddPartPylon = X, Y, Z, axis X, Y, Z, angle 0 to 180` | Pylon on the preceding folding wing, model `wing?_pylon?`. |
| `PivotTurnThrottle = 0.0` | Movement while turning on the ground. 0 turns on the spot (neutral steer); above 0 the vehicle moves while turning (pivot steer). |
| `EnableBack = true` | Allow reversing. |
| `VariableSweepWing = true` | Wings sweep in flight. Requires `AddPartWing`. |
| `SweepWingSpeed = 1.2` | Speed with swept wings. |
| `AddPartNozzle = X, Y, Z, axis X, Y, Z, angle 0 to 180` | Jet nozzle, smokes with throttle and rotates in VTOL. Model `nozzle?`. `ParticlesScale` sets the smoke size. |
| `EnableVtol = true` | VTOL capable. |
| `DefaultVtol = true` | Switch to VTOL automatically on the ground. |
| `VtolYaw = 0.3`, `VtolPitch = 0.3` | Turn and pitch rate in VTOL. |
| `EnableEjectionSeat = true` | Seats zero and one can eject; each ejection takes a parachute from the vehicle screen, which gets a parachute slot per ejection seat. |
| `AddParticleSplash = X, Y, Z, count, size, speed, lifetime, rise, gravity` | Spray while moving on water. Independent of `EnableSeaSurfaceParticle`. |
| `EnableSeaSurfaceParticle = true` | Spray when flying low over the sea. Scales with `ParticlesScale`, around 0.7 is recommended. |

Example of folding wings with pylons:

```
AddPartWing  =  1.50, 2.50, -4.57,  0, 1,0, 35   -> <name>_wing0
AddPartPylon =  6.69, 2.50, -7.18,  0,-1,0, 35   -> <name>_wing0_pylon0
AddPartPylon =  3.92, 2.50, -6.34,  0,-1,0, 35   -> <name>_wing0_pylon1
```

## Ground vehicles

Files: `vehicles/<name>.txt`, `models/vehicles/<name>.mqo`, `textures/vehicles/<name>.png`,
`textures/item/<name>.png`.

```
AddPart = hide in first person, follows yaw, follows pitch, type, X, Y, Z
AddChildPart = hide in first person, follows yaw, follows pitch, type, X, Y, Z
```

`AddPart` adds a part that turns with the player. The first flag is `true` to show and `false` to
hide it in first person. Type 0 is a plain part, 1 spins while firing, 2 recoils while firing.
Models are named `part?`. `AddChildPart` attaches to the preceding `AddPart` and is named
`part?_0`, `part?_1`:

```
AddPart      =  true, true,  false, 0,   0.00, 0.00, 0.00   -> <name>_part0
AddChildPart = false, false, true,  0,  -1.00, 0.00, 2.00   -> <name>_part0_0
AddChildPart = false, false, true,  0,   1.00, 0.00, 2.00   -> <name>_part0_1
```

`RotationPitchMax` and `RotationPitchMin` are obsolete.

## Tanks and cars

Files: `tanks/<name>.txt`, `models/tanks/<name>.mqo`, `textures/tanks/<name>.png`,
`textures/item/<name>.png`.

| Key | Meaning |
|-----|---------|
| `OnGroundPitchFactor = 2.0`, `OnGroundRollFactor = 1.3` | How quickly the hull follows the terrain. Fast vehicles want larger values, slow ones smaller. Too large shakes the screen, too small sinks into blocks. |
| `WeightType = Tank` | `Tank`: ramming mobs does not hurt the vehicle and it breaks more blocks. `Car`: ramming hurts the vehicle and breaks fewer blocks. Anything else is undefined. The breakable blocks are configured in `mcheli.cfg`. |
| `WeightedCenterZ = 0.0` | Center of gravity Z used for terrain tilting. It does not work very well; leave it out if it looks wrong. |
| `SetWheelPos = 1.75, -0.24, 4.85, 3.02, 1.44, -1.54, -2.91` | Ground contact points: X, Y, then any number of Z values. Only the positive X side is needed. Use Y = -0.24 for every vehicle. |
