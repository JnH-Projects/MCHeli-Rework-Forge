# Content packs

A content pack adds vehicles and weapons to MCHeli Rework without changing the mod. It is a zip
file (or a folder) holding definition files, models, textures and sounds, plus a short manifest.
Players put packs in `<game dir>/mcheli/packs`. On a server only the server needs them: every
player who joins gets the server's packs sent automatically.

A pack is data only. The game refuses a pack that holds anything but the file types listed
below, so installing or downloading one can never run code on your machine.

The [example pack](../../examples/rework-example-pack) in this repository has one helicopter, the
Rework Scout, and is the quickest way to start: copy it, rename it and change it.

## Layout

```
my-pack.zip
  pack.properties                                   the manifest (required)
  README.md                                         optional; a licence file is fine too
  assets/mcheli/
    helicopters/my-heli.txt                         definitions, one file per vehicle
    planes/  tanks/  vehicles/                      the other vehicle kinds
    weapons/my-gun.txt                              weapon definitions
    models/helicopters/my-heli.obj                  model, same name as the definition
    textures/helicopters/my-heli.png                default paint, same name as the definition
    textures/helicopters/my-heli-desert.png         more paint schemes (AddTexture)
    sounds/my-heli.ogg                              sounds, named by the definition's Sound key
```

The paths are the ones the original MCHeli used, so a definition, model and texture from an old
content pack can be moved into a pack as they are. A zip whose files all sit inside one folder
(what zipping a folder in Explorer or Finder gives you) works too.

Rules the game checks when it loads a pack:

* Only `pack.properties`, a readme (`.md`, `.txt`) and a licence may sit at the top.
* Under `assets/mcheli/` only definitions (`.txt`), models (`.obj`, `.mqo`, and the `.mtl` file
  exporters write next to an OBJ, which the game ignores), textures (`.png`) and sounds (`.ogg`)
  are allowed.
* File names under `assets/mcheli/` must be lower case. Minecraft identifiers cannot hold capital
  letters, and a zip is case sensitive even where your disk is not.
* At most 10,000 files and 512 MB unpacked. Servers set much smaller limits of their own (see
  [Servers](#servers)).

Files your system leaves behind (`.DS_Store`, `Thumbs.db`, `desktop.ini`, `__MACOSX`) are ignored.

## The manifest

`pack.properties` names the pack:

```properties
id = my-pack
name = My helicopters
author = Your name
version = 1.0.0
description = Two light helicopters for search and rescue.
```

Only `id` is required. It is 1 to 64 characters of lower case letters, digits, `-`, `_` and `.`,
and must be unique: two packs with the same id cannot both load. `original` is reserved for the
imported 1.0.3 content. Raise `version` whenever you share a new build; players see it in
`/mcheli packs`.

## Definitions, models, textures and sounds

* **Definitions** use the same keys as the original vehicles: see [aircraft.md](aircraft.md) and
  [weapons.md](weapons.md). The file name (without `.txt`) is the vehicle's id, used by
  `/mcheli give` and saved in the world, so pick a name nobody else is likely to use (prefix it
  with your pack id) and do not rename it once players have the vehicle.
* **Models** follow [modeling.md](modeling.md): nose along +Z, Y up, one unit per block, moving
  parts as `$` objects.
* **Textures** are PNG files with the same UV layout as the model. Partly transparent pixels are
  drawn as glass.
* **Sounds** are OGG files. A vehicle's engine plays the sound its `Sound` key names, so
  `Sound = my-heli` plays `sounds/my-heli.ogg`.

A vehicle can use weapons from its own pack, from another pack or from the imported content.
HUD scripts, crafting recipes and item icons are not part of packs yet: name an imported HUD in
the `HUD` key (`civilian_aircraft`, `ah-64` and so on; without the import the game shows a plain
text readout), hand pack vehicles out with the creative tabs or `/mcheli give`, and expect the
item to show the generic icon of its kind.

## Load order and replacing vehicles

The imported 1.0.3 content loads first, then the packs in id order. A definition with the same
file name as one loaded earlier replaces it, even if it is a different kind, so a pack can rework
an original vehicle by shipping its own `ah-64.txt`. The log says when that happens.

Models and textures are found the other way round: resource packs first, then the packs, the one
loaded last first. The import's own models and textures come from its resource pack and therefore
win over a content pack. A pack that replaces an original vehicle keeps the original model and
default paint; it can add paint schemes of its own with `AddTexture`. To change the model too,
give the vehicle a new file name instead.

## Trying a pack out

1. Put the zip or folder in `<game dir>/mcheli/packs`.
2. Start the game, or in a world you are an operator in, run `/mcheli reload`.
3. `/mcheli packs` lists the packs in use with their vehicle and weapon counts, and any problems.

A broken pack never stops the game. A pack that cannot be used (bad manifest, a file type that is
not allowed, a zip that cannot be read) is left out; a definition with errors is skipped while the
rest of the pack loads; a bad line in a definition is skipped while the rest of the file loads.
Everything is written to `logs/latest.log`, and operators (and the owner of a single player
world) see the first few problems in the chat when they join and after each reload.

Edit, save and run `/mcheli reload` again: vehicles in the world pick up the new definition
immediately, and every player sees the new model and textures.

## Servers

The server is the source of truth. When a player joins, the server sends the list of its packs;
the player's game downloads the ones it does not have yet and keeps them in
`<game dir>/mcheli/cache`, named by content hash, so the next visit (or another server with the
same pack) needs no download. Players then use exactly the server's packs, in the server's order:
packs the player has installed but the server does not are switched off while they are connected.
The imported 1.0.3 content is never sent; every player imports their own copy.

Server settings, in `serverconfig/mcheli-server.toml` of the world, section `[packs]`:

| Setting | Default | Meaning |
|---------|---------|---------|
| `allowList` | `[]` | Pack ids the server loads. Empty loads every pack in the folder. |
| `maxPackSize` | `8` | Largest pack the server loads and sends, in MB. |
| `maxTotalSize` | `64` | Most the server loads and sends in all, in MB. Packs are taken in id order until the next one would go over. |

A pack left out by these settings is reported like any other problem. Changes apply when the
world starts or after `/mcheli reload`, which also sends every player the new list.

Client setting, in `config/mcheli-client.toml`, section `[packs]`:

| Setting | Default | Meaning |
|---------|---------|---------|
| `cacheSize` | `256` | How much `mcheli/cache` may hold, in MB. The packs used longest ago are deleted first; the current server's packs are always kept. A single pack larger than this is not downloaded and its vehicles stay invisible. |

Downloads run at about 5 MB a second per player alongside the game. A pack that arrives damaged or
does not match the server's hash is thrown away and reported in the chat.

## Sharing a pack

Zip the pack folder and share the zip. Players can drop it into `mcheli/packs`, or simply join a
server that has it. Only share what you have the right to: models, textures and sounds taken from
other mods or games usually may not be passed on.
