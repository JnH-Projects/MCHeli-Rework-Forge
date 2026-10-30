# In-game test plan

Automated tests cover parsing and model loading. Anything involving the game itself is checked by
hand with this plan before a pre-release is tagged.

## Setup

1. `./gradlew build`, then copy the jar for the loader under test from `build/release`
   (`mcheli-<loader>-<minecraft>-<version>.jar`) into the `mods` folder of a profile for that loader
   and Minecraft version,
   and `MC1.7.10_mcheli_1.0.3.zip` into its `mcheli` folder. Fabric also needs Fabric API.
   Alternatively run `./gradlew :<loader>-<minecraft>:runClient`, which imports the zip from `Mod/`.
2. Use a launcher profile with its own game directory. Mods for other loaders or Minecraft
   versions in the same `mods` folder will prevent the game from starting.
3. Create a creative world with cheats enabled.

The phase checklists below were written on Forge and are run in full there. Every other jar gets
the smoke test, which touches each piece of loader glue once.

## Smoke test (every jar)

Run this for each jar in `build/release`: Forge, NeoForge and Fabric on every Minecraft version.

- [ ] The game reaches the title screen. The log has `MCHeli Rework <version> on <loader>` and no
      `mcheli` errors; on the first start it also has `Imported ... files` from the zip.
- [ ] The four MCHeli vehicle tabs list the vehicles with icons and names, after the vanilla tabs.
- [ ] Fly a helicopter (`ah-64`): it takes off, banks and lands. In first person the view rolls
      with the hull and the eye sits in the cockpit; the hand is hidden in the pilot seat.
- [ ] Other players and the rider model tilt with the hull (second player or F5).
- [ ] Drive a tank (`m1a2`): it moves, the turret follows the view, the gun fires.
- [ ] Fire a missile from a helicopter or jet: the use key fires without swinging the arm, the pick
      block key switches weapons, the weapon HUD draws, the missile hits and explodes.
- [ ] Right-clicking the hull outside the main hit box boards the vehicle (the arm swings).
- [ ] Engine and rotor sounds play, including a content pack sound (the example pack).
- [ ] `config/mcheli-client.toml` exists; setting `nightGlow = false` and restarting removes the
      glow.
- [ ] Join a dedicated server running the same jar: the server log has no `mcheli` errors, the
      client downloads the server's content packs, `/mcheli status` and `/mcheli reload` work,
      `serverconfig/mcheli-server.toml` is in the world folder.
- [ ] Crafting: an AH-6X can be crafted from an AH-6 and redstone (the vehicle ingredient), and
      the recipe book lists MCHeli recipes.

### Across loaders

- [ ] A world made on Forge 26.3 with vehicles in it opens on NeoForge and Fabric 26.3 with the
      vehicles and items intact, and back.
- [ ] A Fabric client joins a NeoForge server (both with the mod) and downloads its packs.
- [ ] A `serverconfig/mcheli-server.toml` written by 1.1.0-beta.2 (Forge) keeps its values.

### 1.21.1 only

- [ ] Vehicle icons show in the creative tabs and the hand (the kind model picks them by
      `custom_model_data`); a pack vehicle shows the barrier.
- [ ] `mcheli/imported/item_icons.properties` exists, and the AH-6X recipe crafts an AH-6X with
      its icon.
- [ ] Glow halos, tracers and the glow particle are additive and do not hide smoke behind them.
- [ ] A vehicle hit by a weapon wobbles (hurt time) and takes damage only once per hit.

## Phase 1 checklist

### Loading

- [ ] The game reaches the title screen; the log contains `Loaded 33 helicopters` and no
      `mcheli` errors or warnings.
- [ ] The **MCHeli Helicopters** creative tab lists 33 helicopters with icons and names.
- [ ] Switching the game language to Japanese shows the Japanese item names.

### Placing and boarding

- [ ] Right-clicking a block with a helicopter places it facing the player's direction.
- [ ] The model is textured and the rotors are in the right place for: `ah-64`, `ah-6`, `uh-1c`,
      `ch47` (tandem rotors), `ka-52` (coaxial rotors) and `ec665` (OBJ model).
- [ ] Right-clicking the helicopter seats the player on the pilot seat. A second player (or a
      second right-click from another account) takes the next seat.
- [ ] Shift leaves the helicopter.
- [ ] Punching in creative removes it. In survival it takes damage, flashes red, and drops its
      item when the HP runs out.

### Flying

- [ ] Holding W spools the rotors up; the helicopter lifts off at roughly half throttle.
- [ ] S lowers the throttle and the helicopter descends.
- [ ] Moving the mouse forward pitches the nose down and the helicopter accelerates; back slows it.
- [ ] Moving the mouse sideways turns and banks; the camera rolls with the helicopter.
- [ ] A and D bank in the air and the bank levels out on release.
- [ ] Space toggles hover: the helicopter levels out and holds altitude, and W/A/S/D slide it.
- [ ] Holding Left Alt lets the player look around without steering; releasing it returns the
      view to the nose.
- [ ] Third person (F5) shows the helicopter from behind with matching roll and rotor spin.
- [ ] Landing on uneven ground does not bounce or sink into blocks.
- [ ] Leaving the helicopter in the air: the throttle winds down slowly, so it keeps climbing for
      about ten seconds, then sinks and comes to rest. This matches the original mod.

### Persistence and multiplayer

- [ ] Saving and reloading the world keeps the helicopter type, paint, HP and position.
- [ ] On a LAN or dedicated server, a second player sees the helicopter move smoothly, with the
      correct roll and rotor speed, while someone else flies it.
- [ ] The server log has no "moved too quickly" or "moved wrongly" warnings during normal flight.

## Phase 2 checklist

### Loading

- [ ] The log contains `Loaded 33 helicopters, 51 planes, 12 tanks and 16 vehicles` and no
      `mcheli` errors or warnings. A dedicated server (`./gradlew :forge-26.3:runServer`) starts as
      well.
- [ ] Four creative tabs: helicopters (33), planes (51), tanks and cars (12), ground vehicles (16).
- [ ] The new key bindings show under Controls, category MCHeli, without conflicts.

### Planes

- [ ] `f-35b`, `a-10`, `cessna172` (propeller) and `c5` (OBJ, racks) place, render and take off at
      full throttle. Propellers spin with the throttle.
- [ ] Mouse pitches, rolls and turns; A/D roll in the air and steer on the ground.
- [ ] Canopy on `f-35b`: first click opens it, second boards, it closes after boarding.
- [ ] B raises the gear above ten blocks and lowers it; it drops by itself on low throttle near
      the ground.
- [ ] V on `f-35b` tilts nozzle and rotors; the plane hovers and climbs vertically.
- [ ] Z folds wings on a carrier plane (`fa18f` or `f14d` sweep) and sweeps them in flight.
- [ ] Space twice ejects from `f-35b`; the player floats down with slow falling.
- [ ] A float plane (`us-2` or `h8k`) lands and taxis on water.

### Tanks and cars

- [ ] `m1a2` drives with W/S, turns on the spot with A/D, reverses after braking with S, and stops
      with Space.
- [ ] The hull tilts to follow slopes and steps up blocks; tracks and road wheels move, the track
      runs the other way when reversing.
- [ ] The turret and gun follow the view; the pilot views from the commander position.
- [ ] Left Alt swaps between looking around and looking along the hull.
- [ ] A car (`m1129` or `centauro`) needs to roll before it turns; wheels steer and spin.

### Ground vehicles

- [ ] `mk15` and `bofors40mml60` (OBJ) turn and elevate with the view, within their pitch limits.
- [ ] The view sits at the gun's camera position.

### Crew, cameras and racks

- [ ] A second player boarding takes the next seat; R moves between free seats.
- [ ] A gunner seat (`ah-64` front seat) views through its sight and keeps its pitch limits.
- [ ] H cycles the pilot's camera positions on `bnr32`, the one vehicle with several.
- [ ] Seats survive a world reload with the same players in them.
- [ ] `m1a2` next to the `c5` ramp: J drives it onto the rack, U drives it off. From the `c5`
      pilot seat, J loads nearby vehicles and U unloads them.
- [ ] Engine sound plays for each kind, rises with throttle, and fades with distance.
- [ ] On a server a second player sees parts, wheels, tracks and turrets move.

## Phase 3 checklist

### Loading and riders

- [ ] The log contains `Loaded 33 helicopters, 51 planes, 12 tanks, 16 vehicles and 229 weapons`
      and no `mcheli` warnings.
- [ ] Bank and loop a plane in third person (F5): the pilot tilts with the hull and stays inside
      the canopy. In first person the view stays in the cockpit while rolled.

### Firing

- [ ] `ah-6`: right mouse fires the M134 in a stream with muzzle flashes, ejected cases and its
      sound; middle mouse (or G) switches to the Hydra rockets, which leave a smoke trail and
      explode on impact. Shift with G goes back.
- [ ] Survival: the readout counts ammunition down and reloads; when the reserve is empty, N with
      iron, gunpowder (and redstone where listed) in the inventory buys one resupply.
- [ ] Creative: ammunition shows as infinite.
- [ ] `f-35b`: selecting the AIM-9X opens its weapon bay. Holding right mouse at a flying mob or a
      second aircraft shows "Locking" and fires once locked; the missile chases the target.
- [ ] `ah-64` gunner seat: Hellfire AT missiles lock on ground targets; X switches to top attack,
      which climbs and dives.
- [ ] `ah-64` gunner seat, AGM-114 TV: in [TV] mode the view rides the missile and steers it.
- [ ] Bombs (`mk82` on `a-10` or `f-35b`) fall from the aircraft with its speed and explode.
- [ ] `m1a2`: the main gun kicks the barrel back and rocks the hull; the turret gun follows the view.
- [ ] The CBU-87 on `a-10` splits into bomblets; a proximity fused round bursts near a target.
- [ ] Machine gun rounds shoot through glass panes and tall grass.

### Damage

- [ ] Shooting a parked vehicle reduces its hit points by the armour rules; it smokes below half
      and, at zero, explodes, burns, spins down (helicopters) and disappears after 25 seconds.
- [ ] A destroyed aircraft hitting the ground goes up in a fireball, with smoke columns, thrown up
      dust and burning debris; shot down while you fly it, it still crashes and explodes.
- [ ] Flying a plane into a hillside at speed, or two vehicles ramming each other fast, destroys
      them; a gentle bump does nothing. Landing a helicopter on its side damages it.
- [ ] Looking out of a canopy (`ah-6`, `f-35b`) over water: the water shows through the glass.
- [ ] Right-clicking any part of a long hull (`ac-130` tail, `ah-64` boom) or from inside its cockpit boards it.
- [ ] Punching an empty vehicle in survival gives its item back; with a player aboard it only
      takes damage.
- [ ] Death messages read "shot down by".

### Flares

- [ ] C on a helicopter or plane with `FlareType` releases flares; a missile locked on it is
      decoyed while they burn. The crew hears the warning sound while locked.

### Multiplayer

- [ ] A second player sees projectiles, muzzle flashes, turret aim and weapon bays.
- [ ] The gunner of a two seat helicopter fires their own weapons; while the gunner seat is empty
      the pilot can select them.

## Phase 4 checklist

### Fixes from the alpha.3 test

- [ ] `bnr32` (Skyline GT-R): once rolling, A and D steer, forwards and in reverse; standing still
      it does not turn. The same for `centauro`, `growler`, `m1129` and `mxtmv`. Tracked tanks
      (`m1a2`, `t-90`) still turn on the spot.
- [ ] Bank a helicopter to the right in first person: the view banks right with it and the horizon
      tilts the way it does from a real cockpit. With free look (Alt) held, looking out of the side
      while the nose pitches up tilts the view.
- [ ] As a passenger or gunner (`ah-64`, `uh-1c`) and as a tank driver on a slope, the view tilts
      with the hull. The TV missile camera stays level.
- [ ] Walk around and through parked vehicles (`f-35b`, `ah-64`, `m1a2`, `a-10`) and look at thin
      panels (fins, rotor blades, mudguards): no surface flickers between two textures.
- [ ] Canopies (`ah-6`, `f-35b`): the glass shows no flicker from either side, and water still
      shows through it.

- [ ] `s-75`: holding right mouse on a helicopter or plane flying more than 12 blocks up locks the
      SA-2 and fires it, close targets included. Targets lower than that cannot be locked by AA
      missiles, as in the original.

### Pilot gunner mode

- [ ] `ac-130` in level flight above 60 % throttle: H hands over to the autopilot. The plane levels
      off, holds its height and circles; the view moves to the side camera with the gunner HUD, and
      the mouse aims the side guns. A and D still turn the circle.
- [ ] H again gives back the controls; the view returns to the cockpit.
- [ ] H on the ground, banked or at low throttle does nothing.
- [ ] `ah-64`: H makes the helicopter hover while the pilot uses the gunner view and weapons.
- [ ] A second player boarding the `ah-64` gunner seat ends the pilot's gunner mode (no
      `ConcurrentGunnerMode`); on the `ac-130` (concurrent) it stays on.

### Crashes and ground impacts

- [ ] Shoot down a helicopter and a plane: where the wreck hits the ground it goes up in a big
      fireball with a climbing plume of smoke and burning debris, well beyond the old explosion.
- [ ] `bnr32`: driving into a wall at full speed costs roughly a third of the hit points; a
      bump at walking pace costs nothing. Driving off a cliff of 10 blocks costs a little, 20
      blocks most of it, more than 24 blocks wrecks the car with the crash fireball.
- [ ] `m1a2`: the same kind of wall hit costs the tank a far smaller share, because of its armour.
- [ ] Driving off a cliff into deep water does no damage.
- [ ] The first person hand is hidden for the pilot of every vehicle, in gunner mode, on the S-75
      and in gunner seats with weapons; a passenger without weapons still sees it.

### Crafting

- [ ] A new survival player opens the recipe book and finds the MCHeli vehicles (search "ah-6").
- [ ] `ah-6`: iron blocks and ingots as in the recipe book craft the helicopter on a crafting
      table; `ah-6x` is crafted from an `ah-6` and redstone.
- [ ] A recipe with a dye (`macchi_mc72`, `a6m2`) accepts the dye of that colour.
- [ ] The log has no recipe parsing errors (checked on a dedicated server).

### HUD

- [ ] The log contains `Loaded 19 HUD scripts` and no `Skipping HUD` errors.
- [ ] `ah-64` pilot, first person: the helicopter HUD shows hit points, the weapon with its
      ammunition, altitude, the radar with green blips for animals and red ones for monsters, and
      the control stick moving with the mouse. Bank and pitch: the attitude lines follow.
- [ ] `ah-64` gunner seat: the gunner HUD with the camera frame; the small box follows the view.
      With the AGM-114 TV fired in [TV] mode, the TV missile HUD replaces it.
- [ ] `f-35b` and `a-10`: the plane HUD with the heading tape and pitch ladder; holding fire on a
      target with the AIM-9X fills the lock indicator.
- [ ] `oto76mm`: the gun's heat gauge fills while firing.
- [ ] `m1a2` driver and gunner, `bnr32` driver: their own HUD textures (`mbt_hud`, `mbt_gnr`,
      `bnr32_hud`) draw at the right size. The `bnr32` passenger (`none`) sees no HUD.
- [ ] Hit points under 20 % turn the readouts red; reloading turns the ammunition red.
- [ ] F5 to third person: the HUD disappears and the plain readout of hit points and ammunition
      shows instead.
- [ ] F3 + T reloads the scripts without errors.

## Code-only jar checklist

Use the built jar in a launcher profile, not `runClient`, since only that shows what players get.

- [ ] The jar is under 1 MB and holds no models, textures, sounds or definitions (see
      DEVELOPMENT.md, "Original content").
- [ ] Without `mcheli/` in the game directory: the game starts, the log says the content was not
      found, the creative tabs are empty, and joining a world prints where to put the zip.
- [ ] With the zip in `mcheli/`: the first start logs `Imported 1141 files, 68 sounds, 130 item
      models and 108 recipes`; the second start imports nothing.
- [ ] Options, Resource Packs: "MCHeli Content" is listed as selected and cannot be removed.
- [ ] Vehicles show their textures and English (and, with the language set to 日本語, Japanese)
      names; the HUD, engine sounds and weapon sounds work.
- [ ] A new survival player finds the MCHeli recipes in the recipe book.
- [ ] Deleting the zip after the import: the next start still has every vehicle.
- [ ] A resource pack replacing `assets/mcheli/textures/helicopters/ah-64.png` wins over the import.
- [ ] Dedicated server with the zip: clients with the zip join without a registry mismatch.

## Phase 5 checklist (`1.1.0-beta.1`)

Run each section in single player and again on a dedicated server (`./gradlew
:forge-26.3:runServer`, a client joining it), unless it says otherwise.

### Upgrading from 1.0.0

Keep a copy of a 1.0.0 world with vehicles parked in it, items in the hotbar and items in a chest.

- [ ] The first start of the beta logs `Importing MCHeli content` once (importer format 2); the
      next start imports nothing. No `imported.old` or `imported.tmp` folder is left behind.
- [ ] The parked vehicles load, fly and keep their damage and ammunition.
- [ ] Hotbar items keep their icon and name, and within a second turn into the new item (F3 + H
      shows `mcheli:helicopter` and a `mcheli:vehicle` component). They still place the vehicle.
- [ ] Items in the chest are the old ones until taken out, then convert. A renamed item keeps its
      name.

### Vehicle items

- [ ] Each creative tab lists every vehicle of its kind with the right icon and name, in both
      English and Japanese.
- [ ] Placing, packing up (punch an empty vehicle) and pick block (middle click) all give back the
      same vehicle.
- [ ] Crafting: the AH-6 recipe makes an AH-6; the AH-6X recipe from an AH-6 accepts only an AH-6,
      not another helicopter, and the recipe book shows the AH-6 in that slot.
- [ ] Two different helicopters never stack together.

### Command

- [ ] `/mcheli` prints the counts (33 helicopters, 51 planes, 12 tanks, 16 ground vehicles, 229
      weapons) and the folder. `/mcheli list tank` lists the 12 tanks.
- [ ] `/mcheli give @s ah-64 2` gives two AH-64s; tab completion suggests vehicle names; an
      unknown name is refused. A non-operator on the server cannot use `give` or `reload`.

### Reload

- [ ] Edit `run/mcheli/imported/assets/mcheli/helicopters/ah-64.txt` (for example
      `MaxHp = 1000`), run `/mcheli reload`: chat confirms, and a placed AH-64 shows the new HP
      maximum without being replaced, and keeps its loaded ammunition.
- [ ] Edit the AH-64 texture PNG, `/mcheli reload`: the new texture shows without F3 + T. F3 + T
      afterwards still shows it, loaded the normal way.
- [ ] Add a copy of a helicopter definition under a new file name, `/mcheli reload`: it appears
      in `/mcheli list` and `/mcheli give` (its creative tab entry too, after reopening the
      inventory). Remove it again and reload: placed copies stay as wrecks of an unknown vehicle
      and do not crash the game.
- [ ] A definition with a broken line still reloads; the line is reported in the log.

### Server config

- [ ] `serverconfig/mcheli-server.toml` is created in the world folder with comments.
- [ ] `explosions.breakBlocks = false`: rockets and crashes no longer break blocks, but still hurt.
      `startFires = false`: incendiary hits leave no fire.
- [ ] `vehicles.damageFactor = 0.5`: a vehicle takes half the hits to destroy.
- [ ] `vehicles.disabled = ["ah-64"]`: placing an AH-64 fails with a message; other vehicles work.
- [ ] `weapons.infiniteAmmo = true`: a survival player never runs out.
- [ ] Edits to the file apply without restarting the server.

### Build

- [ ] `./gradlew build` produces `build/release/mcheli-26.3-1.1.0-beta.1.jar`; it holds no EMB4
      file (DEVELOPMENT.md, "Original content").

## Phase 6 checklist (`1.1.0-beta.2`)

Run each section in single player and again on a dedicated server, unless it says otherwise.

### Night glow

Set the time with `/time set midnight` and compare with `/time set noon`.

- [ ] Machine gun rounds trail a warm tracer at night; at noon only a faint shimmer shows.
- [ ] Rockets and missiles show a glowing motor behind them for their whole flight.
- [ ] A flare salvo lights up around each flare; smoke grenades do not glow.
- [ ] A rocket hit gives a short bright flash and a few embers; the smoke behind stays visible
      through the flash (no dark square or cut out).
- [ ] Shooting down a helicopter: a big flash when it hits the ground, then the wreck glows for
      several seconds and the embers thrown out glow as they fall.
- [ ] The wreck keeps burning (flames, glow, rising embers) for 30 seconds, dies down over the
      last five and then disappears. A boat wreck that sank does not glow under water.
- [ ] In a cave or under a roof at noon, the glow is as bright as at night.
- [ ] Nothing glows under water (torpedoes, rounds fired into water).
- [ ] `effects.nightGlow = false` in `config/mcheli-client.toml`: no glow at all, the rest of the
      effects unchanged. Another player on the same server still sees the glow.
- [ ] Video settings, graphics set to Fabulous: the glow still draws and nothing crashes.

### Vehicle hits

- [ ] A vehicle hit by rounds or a rocket no longer flashes red.
- [ ] Flying a plane or helicopter while another player (or a turret) shoots it: no jolt or
      stutter on each hit; the HP still goes down. Same driving a tank.
- [ ] A rocket exploding next to an empty parked vehicle rocks it slightly, much less than before.

### Content packs

Copy `examples/rework-example-pack` into `<game dir>/mcheli/packs` first.

- [ ] Single player: the Rework Scout is in the Helicopters tab (generic icon), places, renders
      with its rotors turning and flies. `AddTexture` cycles to the red rescue paint.
- [ ] `/mcheli packs` lists `rework-example 1.0.0` with 1 vehicle; `/mcheli` shows 1 content pack.
- [ ] Zip the pack folder and replace the folder with the zip: same result after `/mcheli reload`.
- [ ] Edit `rework-scout.txt` (for example `Speed = 1.2`), `/mcheli reload`: a Scout already in the
      world flies with the new speed without being picked up.
- [ ] Break the pack: add a `test.jar` at its top, `/mcheli reload`. The chat names the pack and the
      file, the Scout is gone, everything else still works. Remove the jar, reload, it is back.
- [ ] Put `MaxHp = lots` in the definition: the chat names the file and line, the Scout still loads.
- [ ] Dedicated server with the pack, client without it: on joining, the chat says one pack is
      downloading, then ready; the Scout renders and flies for the client.
      `mcheli/cache/<hash>.zip` exists on the client. Rejoin: no download this time.
- [ ] A second client with the pack in its own `mcheli/packs`: no download.
- [ ] Client with a pack the server does not have: its vehicles are not in the tabs while connected.
- [ ] Server `packs.allowList = ["something-else"]`, `/mcheli reload`: the Scout disappears for
      everyone and operators are told why. Empty list again, reload: it is back for everyone.
- [ ] Server `packs.maxPackSize = 1` with a pack over 1 MB: the pack is left out and reported.
- [ ] Client `packs.cacheSize = 16` with a server pack over 16 MB: the chat says it is too large,
      nothing is downloaded and the game keeps running.
- [ ] Remove the pack from the server and restart it: a world with a Scout still loads; the Scout
      stays in place, inert and invisible, and comes back when the pack does.

## Phase 8 checklist (`1.1.0-beta.4`)

Forge 26.3 only. Start from a world made by beta.3 with a few vehicles in it.

### Shaders

Install Sodium and Iris for 26.3 with a shader pack (Complementary or BSL).

- [ ] Fire tracers and rockets, release flares, blow something up, call in the A-10: the glows sit
      where they belong, with no purple, pink or teal lines running to the horizon.
- [ ] The log has `Iris draws mcheli:pipeline/glow with its EMISSIVE_ENTITIES program` at debug
      level (or no warning about Iris). Without Iris the game starts as before.
- [ ] Note whether the A-10 still draws as a flat white shape with the shader pack on.

### Crash spin

- [ ] Shoot down a plane (`f-35b`, `/mcheli give`, then a missile or `/kill`): it rolls into a
      nose-down spiral and stops spinning near the ground. A second plane may spin the other way.
- [ ] Shoot down a helicopter: it spins up faster than in beta.3 and nods and rocks while it falls.
- [ ] A second player watching sees the same spin direction.
- [ ] A tank or car that is destroyed does not spin.

### Fuel

- [ ] A beta.3 vehicle loads with a full tank (helicopter HUD fuel gauge full).
- [ ] Fly with the throttle open: the gauge goes down; with `/data merge` or a long flight down to
      10% the low fuel warning blinks; at empty the throttle runs down and the engine stops.
- [ ] Right-click the vehicle with the fuel can: the tank fills by 100 and the can's bar drops.
      Right-click in the air with coal in the inventory: the coal is used up and the can refills.
- [ ] Park next to a fuel truck: the tank fills and does not drop while the throttle is open.
- [ ] `vehicles.infiniteFuel = true` and `/mcheli reload`: the tank stops going down.
- [ ] A vehicle without `MaxFuel` (check the HUD) still flies forever.

### Wrench and parachute

- [ ] Damage a vehicle, right-click it with the wrench: 10 hit points back per click, once a
      second, with the wrench sound; the wrench loses durability. A wreck cannot be repaired.
- [ ] Shift right-click a vehicle with several paints (`ah-64`) with the wrench: the paint changes
      for everyone.
- [ ] Jump off a cliff and use the parachute: it opens above you, you float down, W glides you
      forward, and you land without damage. The parachute is used up.
- [ ] Eject from a plane with an ejection seat (Space twice): the seat flies up, the canopy opens
      once it falls, and you land safely.
- [ ] The fuel can, wrench and parachute are in the MCHeli Equipment tab with icons and names, and
      craft with the original recipes.

### Hull boxes and collisions

- [ ] F3+B: cyan boxes outline the hull of an `ec665`, an `f-35b` and an `s-75`, next to the white
      main box. The `mh-6` and the other vehicles without `BoundingBox` lines only show the main box.
- [ ] Fire the gun at the tail, the nose and the rotor mast of a parked helicopter: every round stops
      on the hull (no tracer behind it), and the helicopter takes damage.
- [ ] Fire from a helicopter's own guns: no round stops inside the firing helicopter.
- [ ] Drive a tank into the side of a parked plane: it stops at the wing or fuselage, not at the
      plane's feet. At speed both take crash damage.
- [ ] Land a helicopter on top of a parked truck: it rests there.
- [ ] A vehicle that fits a carrier's rack (a car and a truck with a rack) can still drive onto the
      rack, and `J` loads it.
- [ ] Two vehicles placed into each other can drive apart.

### Zoom and night vision

- [ ] Gunner seat or gunner mode in an AH-64 (`H`): Z zooms 2x, 4x, then back to 1x; the HUD's zoom
      readout follows. In the pilot seat without a sight, Z still folds the wings or hatch.
- [ ] K in a vehicle with night vision: the view brightens at night, K again turns it off.
      Leaving the vehicle turns it off. A night vision potion is not cut short.
- [ ] K in a vehicle without night vision plays the "no" sound and does nothing.

## Reporting problems

Attach `logs/latest.log` (and the crash report, if any) with the helicopter name, what you did
and what you expected.
