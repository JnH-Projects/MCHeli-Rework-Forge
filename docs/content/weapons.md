# Weapon definition format

English translation of `Readme_Weapon.txt` (2016/04/17) from MCHeli 1.0.3 by EMB4. The
untranslated original ships in the 1.0.3 zip as `mcheli/assets/mcheli/Readme_Weapon.txt`.

A weapon is `weapons/<name>.txt`, optionally with `sounds/<name>_snd.ogg`. Names must be lower
case. The sound file has been optional since 0.9.4. Many numeric parameters are clamped to a
range.

Weapons are parsed but not functional in the Rework yet; see [`ROADMAP.md`](../ROADMAP.md).

> The original reloaded every weapon, including hand-held ones, through
> *R (supply screen) > MOD Option > Development > Reload All Weapons*.

## Basics

| Key | Meaning |
|-----|---------|
| `DisplayName = M134 Minigun` | Name shown in the HUD. ASCII only. |
| `Type = MachineGun1` | Weapon type, see below. |
| `Power = 8` | Damage. |
| `DamageFactor = tank, 2.0` | Damage multiplier against a target class: `player`, `heli` or `helicopter`, `plane`, `tank` (tanks and cars) or `vehicle` (ground vehicles). 3.4 with `Power = 10` deals 34. Repeat the line for several classes. |
| `Group = MainGun` | Weapons in the same group reload together. A tank main gun split into `rehinmetall_apfsds`, `rehinmetall_he` and `canistershell` with the same group reloads all three after any shot, without using their ammo. This stops firing one round type and instantly switching to another. |

### Types

| Type | Behavior | Example |
|------|----------|---------|
| `MachineGun1` | Fixed machine gun. | M134 |
| `MachineGun2` | Machine gun that follows the player's view. | M230 |
| `Torpedo` | Seeks its target once it drops into water. | Mk46 |
| `CAS` | Calls close air support on the target. | A-10 |
| `Rocket` | Unguided, fixed direction. | Hydra 70, SNEB 68mm |
| `ASMissile` | Flies to a ground position. | AGM-119 |
| `AAMissile` | Tracks mobs in the air. | AIM-92 |
| `TVMissile` | Steered by the player after launch. | AGM-114 [TV] |
| `ATMissile` | Tracks mobs on the ground. | AGM-114 |
| `Bomb` | Drops straight down. | CBU-100 |
| `MkRocket` | Marks the impact point for artillery. | Hydra 70mm M264RP |
| `Dummy` | Cannot fire. Used to show text in the weapon list. | |
| `Smoke` | Trails smoke. | Smoke white |
| `Dispenser` | Uses an item at the impact point. | Water Dispenser (Till) |
| `TargetingPod` | Spots mobs and players, or marks blocks. | targeting_pod_block |

## Ballistics

| Key | Meaning |
|-----|---------|
| `Acceleration = 4.0` | Projectile speed. Usually at most 4.0; `MachineGun1`, `MachineGun2` and `Rocket` allow up to 100.0. |
| `AccelerationInWater = 4.0` | Torpedo speed in water, at most 4.0. |
| `VelocityInWater = 0.5` | Speed multiplier applied every tick in water. |
| `Gravity = -0.04`, `GravityInWater = 0.0` | Drop rate of the projectile. Larger magnitudes fall faster. |
| `Accuracy = 1` | Spread of unguided bullets and rockets. Larger is less accurate. |
| `Piercing = 2` | Number of blocks the projectile passes through. |
| `Bound = 0.4` | Bounce strength on impact. Needs `DelayFuse`, otherwise it explodes on contact. |
| `GuidedTorpedo = true` | `true` homes on the target block, `false` runs straight from where it entered the water. |

## Explosions and fuses

| Key | Meaning |
|-----|---------|
| `Explosion = 0` | Explosion power on impact. 0 none, 1 roughly a ghast fireball, 2 and up stronger. |
| `ExplosionInWater = 0` | Explosion power under water. |
| `ExplosionBlock = 0` | Block breaking power. 0 leaves blocks intact. |
| `ExplosionAltitude = 10` | Detonates within this many blocks above the ground. |
| `DelayFuse = 30` | Ticks from impact until the projectile disappears, exploding if `Explosion` or `ExplosionInWater` is set. |
| `TimeFuse = 30` | Ticks from launch until the projectile disappears, with the same rule. |
| `Flaming = false` | Spread fire on impact. Only when `Explosion > 0`. |
| `FAE = true` | Fuel air explosive. Does not break blocks. |
| `Bomblet = 25` | Submunitions released after firing, for cluster bombs. |
| `BombletSTime = 5` | Delay before the submunitions release. |
| `BombletDiff = 0.7` | Submunition spread. |
| `Destruct = true` | The vehicle destroys itself on use. Only for `Type = Bomb` on UAV helicopters. |

## Rate of fire and ammunition

| Key | Meaning |
|-----|---------|
| `Delay = 5` | Ticks between shots (about 1/20 s each). Smaller is faster. |
| `ReloadTime = 80` | Reload time in ticks. When 0, `Round` must be non-zero. |
| `Round = 100` | Magazine size. 0 or omitted for none. |
| `HeatCount = 20` | Heat added per shot for heat based weapons. |
| `MaxHeatCount = 150` | Heat limit. |
| `MaxAmmo = 40` | Ammunition the vehicle can carry. |
| `SuppliedNum = 10` | Rounds added per resupply. |
| `Item = 3, iron_ingot` | Items used per resupply. Only `iron_ingot`, `gunpowder` and `redstone` are allowed. Repeatable. |

With `MaxAmmo = 40`, `SuppliedNum = 10` and items `3 iron_ingot, 4 gunpowder, 2 redstone`, each
resupply costs 3 iron, 4 gunpowder and 2 redstone for 10 rounds. Filling all 40 rounds costs 12
iron, 16 gunpowder and 8 redstone.

## Guidance

| Key | Meaning |
|-----|---------|
| `Sight = MoveSight` | Sight shown on screen: `MoveSight` moves with the vehicle, `MissileSight` locks onto mobs (required for `AAMissile` and `ATMissile`), `None`. |
| `LockTime = 20` | Time to lock. Larger is slower. |
| `RidableOnly = true` | Players can only be locked while they ride a vehicle. |
| `ProximityFuseDist = 1.0` | Proximity fuse distance for locking missiles, 1 means 1 m. |
| `RigidityTime = 0` | Ticks after launch before tracking starts. Defaults to 7 when omitted. |
| `ModeNum = 2` | Number of modes switchable with X. 1 or 2, default 1. `MachineGun2` switches to HE rounds (needs `Explosion`). `TVMissile` switches to normal guidance without the missile camera. `ATMissile` switches to top attack (climbs in front of the target, then dives). `Rocket` switches to HEIAP, which scatters submunitions in the air. |

## Hand-held weapons

| Key | Meaning |
|-----|---------|
| `Zoom = 4.2, 9.2` | Scope magnification. Several values separated by `,` are cycled with Z. |

## Sound

| Key | Meaning |
|-----|---------|
| `Sound = rocket_snd` | Sound file without extension. Defaults to `<weapon>_snd`. |
| `SoundVolume = 3` | Volume. Minecraft caps playback at 1.0, but values above 1.0 make the sound audible from further away. Use values below 1.0 to make it quieter. |
| `SoundPitch = 1.0` | Pitch, 0.0 to 1.0. |
| `SoundPitchRandom = 0.1` | Random pitch range, 0.0 to 1.0. `SoundPitch = 0.8` with `SoundPitchRandom = 0.2` plays between 0.6 and 0.8. |
| `SoundDelay = 1` | Minimum gap between repeated sounds. Without it, fast guns such as the M134 drown out every other sound. |

## Effects

| Key | Meaning |
|-----|---------|
| `TrajectoryParticle = flame` | Trail effect, for example while a missile tracks: `none`, `explode`, `flame`, `hugeexplosion`, `largeexplode`, `largesmoke`, `smoke`. Any vanilla particle name works. `Particle` was removed in 1.0.0; use the muzzle flash keys instead. |
| `TrajectoryParticleStartTick = 10` | Ticks before the trail starts. |
| `DisableSmoke = true` | Turns off the smoke trail while the projectile flies (not the firing effect). |
| `AddMuzzleFlash = distance, size, lifetime, A, R, G, B` | Muzzle flash. Can render incorrectly with `Delay` around 5, so avoid it there. |
| `AddMuzzleFlashSmoke = distance, count, size, spread, lifetime, A, R, G, B` | Muzzle smoke, with the same caveat. |
| `BulletColor = 255, 255, 255, 255` | Projectile color, 0 to 255, in order Alpha, Red, Green, Blue. |
| `BulletColorInWater = 255, 25, 25, 75` | Projectile color under water. |
| `SmokeColor = 230, 200, 20, 80` | Smoke color, Alpha, Red, Green, Blue. |
| `SmokeSize = 2.0`, `SmokeMaxAge = 500` | Smoke size and lifetime. |
| `ModelBullet = bullet`, `ModelBomblet = cbc` | Projectile models: `models/bullets/<name>.obj` with `textures/bullets/<name>.png`. |
| `Recoil = 1.1` | How much the vehicle shakes when firing. |
| `RecoilBufCount = 40, 5` | Recoil count and recoil speed multiplier. A larger count lengthens the whole recoil, a larger multiplier only shortens the backward stroke. |

```
SetCartridge = cartridge, 0.0, 0, 0, 2.00, -0.04, 0.40
```

Ejects a spent casing: model name (lower case), ejection speed (0 drops straight down), yaw
(positive left, negative right), pitch (positive down, negative up), model scale, gravity, bounce.

## Mortars

| Key | Meaning |
|-----|---------|
| `DisplayMortarDistance = true` | Show the impact distance. |
| `FixCameraPitch = true` | Lock the camera pitch at 0. |
| `CameraRotationSpeedPitch = 0.3` | Camera turn speed multiplier. Smaller values allow finer aiming. |

## Dispensers

| Key | Meaning |
|-----|---------|
| `DispenseItem = flint_and_steel` | Item used at the impact point. Some items do something, some do not; try it. A `water_bucket` is special: instead of placing water it puts out fire and lava around the impact. |
| `DispenseRange = 4` | Radius in blocks. |

## Targeting pods

| Key | Meaning |
|-----|---------|
| `Target = monsters/others` | What to spot, joined with `/`: `planes`, `helicopters`, `vehicles` (MCHeli vehicles), `players`, `monsters`, `others` (other mobs), `block`. `block` turns the pod into a block marker and overrides the rest. |
| `Length = 100` | Spotting distance in blocks. |
| `Radius = 45` | Spotting cone, as a half angle in degrees. |
| `MarkTime = 10` | How long a spot stays visible, in seconds. |
