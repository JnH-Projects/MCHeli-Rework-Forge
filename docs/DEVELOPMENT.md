# Development

## Toolchain

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 25 (21 for 1.20.6 to 1.21.11, 17 for 1.19.4 and 1.20.2) | Gradle provisions them through the Foojay resolver if none is installed. |
| Gradle | 9.8.2 | Use the wrapper (`./gradlew`). Loom 1.19 needs 9.8. |
| ForgeGradle | 7.x | Forge modules. Sets up Minecraft through the Mavenizer on first build. |
| ModDevGradle | 2.0.x | NeoForge modules. |
| Fabric Loom | 1.19 | Fabric modules: `net.fabricmc.fabric-loom` for 26.x, `-remap` for 1.21.11 and older. |
| Forge | 66.1.0 for Minecraft 26.3 | |
| NeoForge | 26.3.0.99 for Minecraft 26.3 | |
| Fabric | Loader 0.20.1, Fabric API 0.165.0+26.3 | |
| Forge | 65.1.5 for Minecraft 26.2 | |
| NeoForge | 26.2.0.93 for Minecraft 26.2 | |
| Fabric | Loader 0.20.1, Fabric API 0.161.0+26.2 | |
| Forge | 62.0.9 for Minecraft 26.1 | |
| NeoForge | 26.1.2.126 for Minecraft 26.1 | |
| Fabric | Loader 0.20.1, Fabric API 0.145.1+26.1 | |
| Forge | 61.2.4 for Minecraft 1.21.11 | Built with ForgeGradle 7 and the official mappings. |
| NeoForge | 21.11.48 for Minecraft 1.21.11 | |
| Fabric | Fabric API 0.141.6+1.21.11 | Loom remaps the jar, which is obfuscated on 1.21.11. |
| Forge | 55.1.18 for Minecraft 1.21.5 | Built with ForgeGradle 7 and the official mappings. |
| NeoForge | 21.5.101 for Minecraft 1.21.5 | |
| Fabric | Fabric API 0.128.2+1.21.5 | Loom remaps the jar, which is obfuscated on 1.21.5. |
| Forge | 52.1.20 for Minecraft 1.21.1 | Built with ForgeGradle 7 and the official mappings. |
| NeoForge | 21.1.265 for Minecraft 1.21.1 | |
| Fabric | Fabric API 0.116.17+1.21.1 | Loom remaps the jar, which is obfuscated on 1.21.1. |
| Forge | 50.2.14 for Minecraft 1.20.6 | Built with ForgeGradle 7 and the official mappings. |
| NeoForge | 20.6.144 for Minecraft 1.20.6 | |
| Fabric | Fabric API 0.100.8+1.20.6 | Loom remaps the jar, which is obfuscated on 1.20.6. |

All three use Mojang's official names, so the shared code reads the same on every loader. The
loader versions of the [placeholder modules](#placeholder-modules) are in their own
`gradle.properties`.

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
module has its own `gradle.properties` with its loader, Minecraft and Java versions. During the
betas up to beta.8, `platforms` listed only `forge-26.3`. Roadmap phase 13 adds modules back as each
one catches up, currently the three each on 26.3, 26.2, 26.1, 1.21.11, 1.21.5, 1.21.1 and 1.20.6; the others stay in the repository
until they are ported.
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
minecraft-26.2/                the same shared code on Minecraft 26.2
forge-26.2/, neoforge-26.2/, fabric-26.2/   the loader glue for 26.2
minecraft-26.1/                the same shared code on Minecraft 26.1
forge-26.1/, neoforge-26.1/, fabric-26.1/   the loader glue for 26.1
minecraft-1.21.11/             the same shared code on Minecraft 1.21.11
forge-1.21.11/, neoforge-1.21.11/, fabric-1.21.11/   the loader glue for 1.21.11
minecraft-1.21.5/              the same shared code on Minecraft 1.21.5
forge-1.21.5/, neoforge-1.21.5/, fabric-1.21.5/   the loader glue for 1.21.5
minecraft-1.21.1/              the same shared code, backported to Minecraft 1.21.1
forge-1.21.1/, neoforge-1.21.1/, fabric-1.21.1/   the loader glue for 1.21.1
minecraft-1.20.6/              the same shared code on Minecraft 1.20.6
forge-1.20.6/, neoforge-1.20.6/, fabric-1.20.6/   the loader glue for 1.20.6
<loader>-<version>/            placeholders for the other supported versions (see below)
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
folder `./run` is shared by every 26.3 module, and every other version has its own, such as
`./run-26.1` and `./run-1.21.1`; worlds made by one loader open on the others, but never open a world
on an older Minecraft version than the one that last saved it.

IDEs import the project as one Gradle project per module. A file under `minecraft-26.3` belongs
to all three loader modules, so the IDE shows it under whichever it picked; it compiles under all
of them on the command line. After pulling the split, re-import the Gradle project; the old
`forge-26.3` run configurations still work, the NeoForge and Fabric ones are new.

### Placeholder modules

Every [supported version](ROADMAP.md#supported-versions) has a loader module, but only 26.3, 26.2,
26.1, 1.21.11, 1.21.5, 1.21.1 and 1.20.6 are ported. The other 4 (`forge-` and `fabric-` for 1.20.2 and 1.19.4) are placeholders: each pins
its loader, mappings and Java version, and holds only an entry point that logs a line at startup
and the mod metadata. Their `gradle.properties` sets `scaffold=true`, which tells
`gradle/loader.gradle` to leave out `common` and `minecraft-<version>`, so they build before that
code has been ported to their version.

They are not in `platforms`, so CI and the betas never build them. Build or run one by passing
the list on the command line:

```
./gradlew :forge-1.20.2:build -Pplatforms=forge-1.20.2
./gradlew :fabric-1.19.4:runServer -Pplatforms=fabric-1.19.4    # runs from ./run-1.19.4
```

Porting a version for a final release: add `minecraft-<version>` (copied from the nearest ported
version), copy the loader glue from the ported module of the same loader, remove `scaffold=true`
and add the module to `platforms`.

A clean compile proves little about a port. Compare what the loaders report as well: a recipe that names an
undefined tag is logged and dropped on one loader while the others load it, and on 1.21.1 and older it
loads with an ingredient nothing matches, so count those too. Mixin targets, the Forge access transformer, shaders and
data files only fail at runtime, so start a client and a dedicated server on every loader of that
version, and check a built jar. A loader can also pin a library more strictly than the version the
root `gradle.properties` asks for; override it in that module's `gradle.properties`, as NeoForge 26.2
and 26.1 do with `jspecify_version`. A first run in a new run folder stops at vanilla's accessibility
screen instead of the title screen, which matters only to anything that automates a client.

There is no `neoforge-1.20.2`: ModDevGradle needs a "moddev bundle" that NeoForge 20.2 does not
publish, so NeoForge support starts at 1.20.6, the next supported version.

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
or from `run/mcheli/imported`. As a guard, the `jar` task leaves out anything under `assets/mcheli`
or `data/mcheli/recipe` that the resource folders of `common` and `minecraft-<version>` do not hold,
such as copies left in the build output by an older build or the IDE. So what is committed there
ships, and anything added there is ours to redistribute.

After a build, check every jar with:

```
for jar in build/release/*.jar; do unzip -l "$jar" | grep -v "com/"; done
```

which should list only the manifest, the loader's metadata, `pack.mcmeta`, and the files in those
resource folders.

When the conversion changes, change `content.LegacyImporter` and bump its `FORMAT`, so existing
installs, the dev run and the tests all convert again. Keys the rework adds go into the lang files
in `common/src/main/resources`; `LegacyImporterTest` fails if one reuses a key the import writes.

To force a fresh import, delete `run/mcheli/imported` (dev run) or `common/build/legacy-content` (tests).
