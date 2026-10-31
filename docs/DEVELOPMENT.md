# Development

## Toolchain

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 25 (and 21 for the 1.21.1 modules) | Gradle provisions them through the Foojay resolver if none is installed. |
| Gradle | 9.8.2 | Use the wrapper (`./gradlew`). Loom 1.19 needs 9.8. |
| ForgeGradle | 7.x | Forge modules. Sets up Minecraft through the Mavenizer on first build. |
| ModDevGradle | 2.0.x | NeoForge modules. |
| Fabric Loom | 1.19 | Fabric modules: `net.fabricmc.fabric-loom` for 26.x, `-remap` for 1.21.1. |
| Forge | 66.1.0 for Minecraft 26.3 | |
| NeoForge | 26.3.0.99 for Minecraft 26.3 | |
| Fabric | Loader 0.20.1, Fabric API 0.165.0+26.3 | |
| Forge | 52.1.20 for Minecraft 1.21.1 | Built with ForgeGradle 7 and the official mappings. |
| NeoForge | 21.1.265 for Minecraft 1.21.1 | |
| Fabric | Fabric API 0.116.17+1.21.1 | Loom remaps the jar, which is obfuscated on 1.21.1. |

All three use Mojang's official names, so the shared code reads the same on every loader.

The first build downloads and decompiles Minecraft once per loader, which takes a few minutes.
Avoid running a second Gradle build (for example an IDE import) while that is in progress: both
write the same caches and can corrupt each other's output. If setup fails with a zip or EOF error,
wait for the other process to finish and build again.

## Common tasks

```
./gradlew build                        # compile and test every module, one jar per loader and version in build/release
./gradlew :common:test                 # unit tests (they all live in common)
./gradlew :forge-26.3:runClient        # development client from ./run
./gradlew :neoforge-26.3:runClient
./gradlew :fabric-26.3:runClient
./gradlew :neoforge-1.21.1:runClient       # 1.21.1 modules run from ./run-1.21.1
./gradlew :<module>:runServer          # development dedicated server from ./run
```

Shared properties live in the root `gradle.properties`: `mod_version`, the mod metadata, the list
of loader modules (`platforms`) and the library versions `common` compiles against. Each loader
module has its own `gradle.properties` with its loader, Minecraft and Java versions. During betas
4 to 8, `platforms` lists only `forge-26.3`, so CI and the release build one jar; the other modules
stay in the repository and come back to the list for 1.1.0 (roadmap phase 13).
`mods.toml`, `neoforge.mods.toml`, `fabric.mod.json` and `pack.mcmeta` are filled in from them
at build time. What every loader module shares (source folders, jar contents, manifest, file
names) is in `gradle/loader.gradle`.

## Repository layout

```
common/                        version-free code, built and tested without Minecraft
  src/main/java                content, model, hud, physics, config (see ARCHITECTURE.md)
  src/main/resources           lang files shared by every platform
  src/test/java                all unit tests, including the boundary test
minecraft-26.3/                code shared by every loader on Minecraft 26.3 (not a Gradle project)
  src/main/java                entities, rendering, networking, registries, HUD, sounds, hooks
  src/main/resources           pack.mcmeta, item models, damage type data, mod icon
forge-26.3/                    Forge 66 glue: entry point, events, payload channel, ingredient
neoforge-26.3/                 NeoForge glue, plus a camera mixin
fabric-26.3/                   Fabric glue, plus mixins for what Fabric API has no event for
minecraft-1.21.1/              the same shared code, backported to Minecraft 1.21.1
forge-1.21.1/, neoforge-1.21.1/, fabric-1.21.1/   the loader glue for 1.21.1
gradle/loader.gradle           what every loader module shares
docs/                          documentation and content format guides
.github/workflows              CI and release pipelines
```

### Three layers

1.1.0 runs on several Minecraft versions and three loaders, so the code is split in three:

- **`common`** holds everything that does not depend on the game version: definition parsing,
  the content registry, model loading, the HUD expression language, recipe conversion, rotation
  and impact maths, the config file format. Its classpath has only the JDK, SLF4J, JSpecify and
  JOML (all shipped by every supported Minecraft version), so a stray Minecraft import fails to
  compile, and `CommonBoundaryTest` names the offending line.
- **`minecraft-<version>`** holds everything that touches Minecraft but not a loader: entities,
  rendering, networking, the HUD, sounds, commands, config. It talks to the loader through
  `platform.Platform` (paths, sending packets, creative tab builders) and `platform.Registration`
  (registry entries), and exposes `CommonHooks` and `client.ClientHooks` for the loader to call
  from its events. It is not a Gradle project of its own: every loader module on that version
  compiles it, and since Fabric has none of Forge's classes and Forge none of Fabric's, a loader
  API that slips in breaks a build.
- **A loader module per loader and version** (`forge-26.3`, `neoforge-26.3`, `fabric-26.3`) is
  the thin glue: the entry point, registering what `Registration` collected, wiring events to the
  hooks, the packet channel, the vehicle ingredient type, and mixins where the loader has no event
  (camera roll, hand, use and pick block keys, rider pose, the imported content pack on Fabric).

Each loader module compiles `common` and its `minecraft-<version>` sources together with its own,
so each loader and version ships as one jar and runs as one module in development.

A feature that is pure logic goes in `common` with a unit test; the part that touches Minecraft
goes in `minecraft-<version>`; only what needs a loader API goes in the loader modules. The run
folder `./run` is shared by every 26.3 module and `./run-1.21.1` by every 1.21.1 module; worlds made by
one loader open on the others, but never open a 26.3 world on 1.21.1.

IDEs import the project as one Gradle project per module. A file under `minecraft-26.3` belongs
to all three loader modules, so the IDE shows it under whichever it picked; it compiles under all
of them on the command line. After pulling the split, re-import the Gradle project; the old
`forge-26.3` run configurations still work, the NeoForge and Fabric ones are new.

`Mod/` holds the original `MC1.7.10_mcheli_1.0.3.zip`. It is git-ignored. Download it from the
[MCHeli CurseForge page](https://www.curseforge.com/minecraft/mc-mods/mcheli-minecraft-helicopter-mod/files)
and put it there: `runClient` and `runServer` import it into `run/mcheli/imported` through the
`mcheli.legacyZip` system property, and the content tests import it into `build/legacy-content`.
Without it the build still passes, but the content tests are skipped (as on CI), so run the tests
locally with the zip before a release.

## Workflow

1. `git checkout main && git pull`
2. `git checkout -b <type>/<topic>`, for example `feat/plane-flight-model` or `fix/rotor-pivot`
3. Commit with signed commits (`git commit -S`).
4. Update `CHANGELOG.md` under the upcoming version, and the relevant page in `docs/`.
5. Push the branch and open a pull request. CI must pass before merging.

## Continuous integration

`.github/workflows/build.yml` runs `./gradlew build` on every push to `main` and on every pull
request, which builds and tests every module. The jars from `build/release` are attached to the
run as an artifact for 14 days; test reports are uploaded when the build fails.

## Releasing

Releases are cut from tags. The release workflow refuses to publish when the tag and
`mod_version` disagree, so both have to change together.

1. Set `mod_version` in `gradle.properties`, for example `1.0.0-alpha.1`.
2. Move the `Unreleased` changelog section to that version with today's date:
   `## [1.0.0-alpha.1] - 2026-10-26`.
3. Merge to `main`.
4. Tag and push:

   ```
   git checkout main && git pull
   git tag -s v1.0.0-alpha.1 -m "MCHeli Rework 1.0.0-alpha.1"
   git push origin v1.0.0-alpha.1
   ```

`.github/workflows/release.yml` then builds and tests the tag, extracts the matching
`CHANGELOG.md` section as release notes, and publishes a GitHub release with one jar per
loader and Minecraft version (`mcheli-<loader>-<minecraft>-<version>.jar`, such as
`mcheli-fabric-26.3-1.1.0-beta.3.jar`, gathered by the `releaseJars` task into `build/release`).
One tag always releases every loader module at the same mod version. Versions
containing a hyphen (`-alpha.N`, `-beta.N`, `-rc.N`) are marked as pre-releases.

Every release, pre-releases included, is mirrored to the public repository and tagged there too.
Only full releases (`1.0.0`, `1.1.0`, ...) are uploaded to CurseForge; betas and release
candidates stay on GitHub, where anyone can still download them from the public repository's
releases page. See [the release channels](ROADMAP.md#release-channels).

## Original content

Neither the repository nor the jar may contain EMB4's files. Don't commit anything from the zip
or from `run/mcheli/imported`. As a guard, the `jar` task excludes everything under
`assets/mcheli` except `lang`, the four fallback item models of the generic vehicle items and
the night glow particle (`particles/glow.json`, `textures/particle/glow.png`, both our own).
After a build, check every jar with:

```
for jar in build/release/*.jar; do unzip -l "$jar" | grep -v "com/"; done
```

which should list only the manifest, `mods.toml`, `pack.mcmeta`, the two lang files, the four
generic item models, the glow particle and its texture, and the damage type data.

When the conversion changes, change `content.LegacyImporter` and bump its `FORMAT`, so existing
installs, the dev run and the tests all convert again. Keys the rework adds go into the lang files
in `common/src/main/resources`; `LegacyImporterTest` fails if one reuses a key the import writes.

To force a fresh import, delete `run/mcheli/imported` (dev run) or `common/build/legacy-content` (tests).
