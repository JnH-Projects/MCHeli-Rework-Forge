# Modeling a vehicle in Blockbench or Blender

This guide covers the model side of a new vehicle: the part you build outside Minecraft. The
vehicle itself (seats, rotors, wings, engine, weapons, flight values) is described by a definition
file, see [aircraft.md](aircraft.md). The vehicle workshop planned for 1.1.0 (see
[ROADMAP.md](../ROADMAP.md), phases 9 and 10) will let you do that second half in game by clicking on the
model instead of typing coordinates. The model rules below are the same either way, so a model
made today keeps working in the workshop.

## The short version

1. Model the vehicle with the nose pointing along **+Z**, **Y up**, 1 unit = 1 block, and the
   origin on the ground under the middle of the vehicle.
2. Split every part that moves into its own object named with a `$`: `$body`, `$blade0`,
   `$wing0`, `$lg0` and so on (full list below).
3. UV map everything onto one texture (a PNG, power of two sizes such as 1024 x 1024 work best).
4. Make sure every face points outwards. Back faces are not drawn.
5. Export as **Wavefront OBJ**.

## Axes, scale and origin

| | Rule |
|---|---|
| Forward | +Z. The tail of a helicopter has negative Z. |
| Up | +Y |
| Right | -X (seen from the pilot seat, +X is the left side) |
| Scale | 1 unit = 1 block = 1 metre. An A350 is about 67 units long. |
| Origin | On the ground, under the centre of the vehicle. The vehicle stands on Y = 0. |

The positions in a definition use the same space, so a seat at `0.0, 1.5, -0.3` sits 1.5 blocks
above the origin and slightly behind it. Keep the origin where it is once you start placing
parts; moving the model later moves everything out of place.

Old `.mqo` models (Metasequoia) are also read; they are divided by 100, so 100 MQO units are one
block. New models should be OBJ.

## Parts and names

The game finds moving parts by object name. A name starting with `$` begins a part, and every
object after it belongs to that part until the next `$` object. Objects without a `$` therefore
belong to the part above them in the list: you can keep a part made of several pieces as long as
the `$` object comes first.

Everything that does not move goes into `$body`. A model without `$body` is drawn whole and nothing
animates.

Numbers count from 0 in the order the parts appear in the definition: the first `AddRotor` is
`$blade0`, the second `$blade1`.

| Part | Object name | Pivot / notes |
|------|-------------|---------------|
| Fixed hull, cockpit, seats | `$body` | |
| Helicopter rotor blades | `$blade0`, `$blade1` | One blade only; the game copies it round the hub. Model it pointing along the first blade position. |
| Plane propeller hub | `$rotor0` | Spins about the axis given in `AddPartRotor`. |
| Plane propeller blades | `$blade0` for every blade of `$rotor0` (`AddBlade`) | |
| Swinging wing | `$wing0` | Folds on carriers or sweeps. |
| Pylon on a wing | `$wing0_pylon0` | |
| VTOL nozzle | `$nozzle0` | |
| Landing gear | `$lg0`, `$lg1` | Model it extended. |
| Canopy | `$canopy0` | Model it closed. |
| Hatch or ramp | `$hatch0` | Model it closed. |
| Weapon bay door | `$wb0` | |
| Camera turret | `$camera0` | |
| Turret or gun mount | `$weapon0`, with barrels `$weapon0_0`, `$weapon0_1` | Turns with the gunner. |
| Wheels | `$wheel0`, `$wheel1` | Roll with speed. |
| Steering wheel (the one in the cockpit) | `$steering_wheel0` | |
| Tank tracks | `$crawler_track0`, rollers `$track_roller0` | |
| Throttle lever | `$throttle0` | |
| Spinning decoration (radar dish) | `$rotpart0` | |

Every moving part turns around a point given in the definition, not around its own origin in the
model, so model each part in its real place on the vehicle. Note the pivot positions as you go
(for example the rotor hub centre or a gear hinge); they are what you will type in the
definition or click in the workshop.

## Textures

* One PNG per paint scheme, all with the same UV layout. Extra schemes are listed with
  `AddTexture` and players cycle through them in game.
* Glass: give the canopy area a partly transparent alpha in the PNG. Pixels with alpha are drawn
  as glass, blended with what is behind them; fully opaque pixels are drawn solid.
* A cockpit texture is just part of the same PNG: UV map the instrument panel faces onto their own
  area of the texture and paint the gauges there.
* Panels that should look different from each side need two faces back to back, each facing out
  of its own side.

## Faces and normals

* Back faces are culled, as in the original. A face whose normal points into the vehicle is
  invisible from outside. In Blender, show face orientation (Overlays > Face Orientation): every
  outside face should be blue. Recalculate outside (Shift + N) fixes most of them.
* Triangles and quads are fine; larger polygons should be triangulated before export.
* Normals from the file are used for shading, so smooth or flat shading exported from your tool
  shows as you set it.

## Exporting

### Blockbench

1. Create a **Generic Model** (not a Java block or entity model, which limit the size).
2. Mind the scale: Blockbench measures in pixels, 16 to a block. Check the exported OBJ (or
   open it in Blender): one block must be 1 unit. If the vehicle comes out 16 times too large,
   scale it by 1/16 before exporting.
3. Make one group per part and name it with the `$` names above.
4. File > Export > **Export OBJ Model**. Blockbench writes the PNG next to it.

### Blender

1. Model in metres with the default scene units.
2. Name each object with the `$` names above.
3. File > Export > **Wavefront (.obj)** with:
   * Forward axis: **Z**, Up axis: **Y**
   * Objects as OBJ Objects: on
   * UV coordinates and normals: on
   * Triangulated mesh: on
   * Materials: off (the game uses the PNG named in the definition)
4. Save the texture as PNG.

## Getting it into the game

Since 1.1.0-beta.2 a model, its textures and a definition go into a content pack, which players
install by dropping it into `mcheli/packs` and servers send to everyone who joins. See
[packs.md](packs.md), and the example pack in `examples/rework-example-pack`, whose Rework Scout
follows this guide.

## What the workshop will do with the model

Once the model is imported, the workshop shows it in 3D and asks you to connect each part to
what it does: click the `$blade0` hub to place the rotor, the seat cushions to place seats, the
engine and fuel tank to set where damage starts fires, the wing tips for lift, the gun barrels for
weapons, and drag boxes over the hull for hit boxes. It then writes the definition for you. A
model made with the names and pivots above needs no changes for that.
