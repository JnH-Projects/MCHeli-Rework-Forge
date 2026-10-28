# HUD script format

English translation of `Readme_HUD.txt` (2016/03/28) from MCHeli 1.0.3 by EMB4. The
untranslated original ships in the 1.0.3 zip as `mcheli/assets/mcheli/Readme_HUD.txt`.

A HUD is `hud/<name>.txt`. Unlike vehicle and weapon definitions, order matters: commands run top
to bottom, and that is also the drawing order.

HUD scripts are not rendered by the Rework yet; see [`ROADMAP.md`](../ROADMAP.md).

> The original reloaded HUDs through *R (supply screen) > MOD Option > Development > Reload All HUD*.
> Textures reload through the vanilla resource pack screen.

## Expressions

Every number, except the data arguments of `DrawString` and `DrawCenteredString`, can be an
expression.

* Arithmetic: `Color = 123 + 456`
* Hexadecimal with `#` or `0x`: `Color = #123 + 0x456 + 789`
* Variables: `Color = hp_rto * 100`. `hp_rto` runs from 0.0 to 1.0, so this gives 0.0 to 100.0.
* Conditionals: `condition ? value if true : value if false`, for example
  `Color = hp_rto>0.2? 0xFFFFFFFF: 0x00000000` yields white above 20% HP and transparent below.

### Variables

| Variable | Meaning |
|----------|---------|
| `center_x`, `center_y` | Screen center. |
| `width`, `height` | Screen size. |
| `yaw` | Vehicle yaw, -180.0 to 180.0. |
| `pitch` | Vehicle pitch, -90.0 to 90.0. |
| `roll` | Vehicle roll, -180.0 to 180.0. |
| `plyr_yaw`, `plyr_pitch` | Player yaw and pitch. |
| `altitude` | Distance to the block below the vehicle, never negative. |
| `sea_alt` | Height above sea level, never negative. |
| `hp`, `max_hp` | Remaining and maximum durability. |
| `hp_rto` | Remaining HP ratio, 0.0 to 1.0. |
| `throttle` | Current throttle, 0.0 to 1.0. |
| `pos_x`, `pos_y`, `pos_z` | Vehicle position. |
| `motion_x`, `motion_y`, `motion_z` | Vehicle velocity. |
| `fuel` | Remaining fuel ratio, 0.0 to 1.0. |
| `low_fuel` | Low fuel warning, alternates between 0 and 1 to blink. |
| `stick_x`, `stick_y` | Control stick deflection, -1.0 to 1.0. |
| `reloading` | 1 while the weapon reloads. |
| `reload_time` | Remaining reload ratio, 0.0 when done. |
| `wpn_heat` | Weapon heat, 0.0 to 1.0. |
| `is_heat_wpn` | 1 for heat based weapons. |
| `dsp_mt_dist` | 1 when the impact distance should be shown. |
| `mt_dist` | Impact distance. Negative when it cannot be computed. |
| `have_radar` | 1 if a radar is fitted. |
| `radar_rot` | Radar sweep angle. Entity positions update once per revolution. |
| `vtol_stat` | Planes only. 0 normal, 1 switching, 2 VTOL. |
| `free_look` | 1 during free look. |
| `cam_mode` | 0 normal, 1 night vision, 2 thermal. |
| `cam_zoom` | Camera zoom, 1.0 to 10.0. |
| `auto_pilot` | 1 while the auto pilot is on. |
| `have_flare` | 1 if flares are fitted. |
| `can_flare` | 0 when flares are ready, 1 while recharging. |
| `sight_type` | 0 none, 1 `MoveSight`, 2 `MissileSight`. |
| `lock` | Missile lock progress, 0.0 to 1.0. |
| `color` | Current color, `0x00000000` to `0xFFFFFFFF`. |
| `inventory` | Inventory slot count, 0 to 54. |
| `hovering` | 1 while hovering. |
| `is_uav` | 1 for UAVs. |
| `uav_fs` | UAV signal strength, 0.0 to 1.0. Higher is closer to the station. |
| `gunner_mode` | 1 in gunner mode. |
| `time` | In-game time of day, 0 to 24000. 0 is 6 AM, 6000 is noon. |
| `test_mode` | 1 in test mode. |

## Control flow

**`Call = common_pilot`** runs `hud/common_pilot.txt` in place. Called files can call others, but a
file that is already running cannot be called again: `Call = heli` inside `heli.txt` is ignored,
and so is `heli.txt` calling `plane.txt` calling `heli.txt`.

**`Exit`** stops the current file. Inside a called file, execution returns to the line after the
`Call`.

**`If` / `EndIf`** runs the enclosed commands only when the condition is non-zero. Nesting is not
supported.

```
If = is_heat_wpn==1
    Color = 0xFF28d448
    DrawRect = -145, 57, 43, 10
EndIf
```

## Color

`Color` sets the color for everything drawn afterwards. Either one ARGB value or four
components in the order alpha, red, green, blue:

```
Color = 0xFFFFFFFF
Color = #FFFFFFFF
Color = 0xFF,  40, 212,  72
```

## Text

`DrawString` draws left aligned from the position, `DrawCenteredString` centers on it.

`DrawString = x, y, "format", data...`

| Parameter | Meaning |
|-----------|---------|
| 1, 2 | Offset from the screen center. |
| 3 | `printf` / `String.format` style format. Commas are not allowed inside it. The number of `%` placeholders must match the data count. |
| 4 and up | Data items, optional and unlimited. Case insensitive. Expressions are not allowed here. |

Width and precision modifiers work as in `printf`: `%.0f` gives `123`, `%.1f` gives `123.4`,
`%3d` pads to three digits.

| Data | Placeholder | Meaning |
|------|-------------|---------|
| `NAME` | `%s` | Vehicle name, same as the item name. |
| `ALTITUDE` | `%d` | Altitude. |
| `DATE` | `%tY %tm %td %tH %tM %tS` | Real date and time. |
| `MC_THOR`, `MC_TMIN`, `MC_TSEC` | `%d` | In-game hours (0 to 23), minutes and seconds. In-game time runs 72 times faster than real time. |
| `MAX_HP`, `HP` | `%d` | Maximum and remaining durability. |
| `HP_PER` | `%f` | Remaining durability, 100.0 to 0.0. |
| `THROTTLE` | `%f` | Throttle, 100.0 to 0.0. |
| `POS_X`, `POS_Y`, `POS_Z` | `%f` | Position. |
| `MOTION_X`, `MOTION_Y`, `MOTION_Z` | `%f` | Velocity. |
| `YAW`, `ROLL` | `%f` | Vehicle yaw and roll. |
| `PITCH` | `%f` | Vehicle pitch. The sign is inverted: up is positive. |
| `PLYR_YAW` | `%f` | Player yaw. |
| `PLYR_PITCH` | `%f` | Player pitch, also inverted. |
| `INVENTORY` | `%d` | Inventory size. |
| `WPN_NAME` | `%s` | Selected weapon. |
| `WPN_AMMO`, `WPN_RM_AMMO` | `%s` | Rounds loaded and rounds in reserve. |
| `RELOAD_PER` | `%f` | Reload progress, 100.0 when done. |
| `RELOAD_SEC` | `%f` | Seconds of reload left, 0.0 when done. |
| `MORTAR_DIST` | `%f` | Impact distance, negative when unknown. |
| `MC_VER`, `MOD_VER`, `MOD_NAME` | `%s` | Minecraft version, mod version, mod name. |
| `TVM_POS_X`, `TVM_POS_Y`, `TVM_POS_Z` | `%f` | TV missile position. |
| `TVM_DIFF` | `%f` | Distance between TV missile and vehicle. |
| `CAM_ZOOM` | `%f` | Camera zoom. |
| `UAV_DIST` | `%f` | Distance to the UAV station. |
| `KEY_GUI` | `%s` | Name of the GUI key (R by default). |

Examples:

```
DrawCenteredString = 0,  40, "[ %3d ]", ALTITUDE
; altitude 12 shows "[  12 ]"; the hundreds digit becomes a space

DrawString = 0,  30, "%tY %tm %td  [ %02d:%02d:%02d ]", DATE, DATE, DATE, MC_THOR, MC_TMIN, MC_TSEC
; shows "2014 10 24  [ 12:34:56 ]"

DrawString = 0,  20, "%3d/%3d  = %.1f%%", HP, MAX_HP, HP_PER
; HP 50 of 100 shows " 50/100  = 50.0%"; %% prints a percent sign

DrawString = 0,  10, "[ %s ]", name
; on an AH-64 shows "[ AH-64D Apache Longbow ]"

DrawCenteredString = 0, -10, "HUD Test"
; no data, always shows "HUD Test"
```

## Shapes

**`DrawTexture = name, x, y, width, height, u, v, uWidth, vHeight[, rotation]`** draws part of
`textures/gui/<name>.png`. Textures should be 256 by 256. Everything after the name accepts
decimals.

```
DrawTexture = heli_hud, -100.0, 20,  50,20,  0,0, 64,64,  90.0
```

**`DrawRect = x, y, width, height`** fills a rectangle.

```
DrawRect = -20, -30, 40*throttle, -20
```

**`DrawLine = x1, y1, x2, y2[, x3, y3, ...]`** draws a connected line through every point.

```
DrawLine = -40,  30,   40, 30
DrawLine = -20, -30,  -20+40*throttle, -30,  -20+40*throttle, -20,  -20, -20,  -20, -30
```

**`DrawLineStipple = pattern, factor, x1, y1, x2, y2[, ...]`** draws a dashed line. The pattern
and factor are those of `glLineStipple`; `0xCCCC` or `0xAAAA` are safe choices.

```
DrawLineStipple = 0xF0F0, 1,   -40,  30,   40, 30
```

## Instruments

**`DrawEntityRadar`** and **`DrawEnemyRadar`** draw a radar of non-hostile mobs and monsters
respectively: `rotation, x, y, width, height`.

```
DrawEntityRadar = plyr_yaw, 40,  30,   32, 32
DrawEnemyRadar  = plyr_yaw, 40,  30,   32, 32
```

**Graduation scales** take `angle, roll, x, y`. `DrawGraduationYaw` shows a heading tape (top of
the screen in the example), `DrawGraduationPitch1` pitch ladders at the sides and
`DrawGraduationPitch2` a pitch and roll ladder in the center. Keep every parameter except the X of
`DrawGraduationYaw` as shown:

```
DrawGraduationYaw    = plyr_yaw,    0,     0, -100
DrawGraduationPitch1 = plyr_pitch,  0,     0, 0
DrawGraduationPitch2 = plyr_pitch, -roll,  0, 0
```

**`DrawCameraRot = x, y`** draws a small box showing where the player looks relative to the
vehicle nose.

```
DrawCameraRot = 0, 60
```
