# Development

## Toolchain

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 25 | Gradle provisions one through the Foojay resolver if none is installed. |
| Gradle | 9.7.1 | Use the wrapper (`./gradlew`). |
| ForgeGradle | 7.x | Sets up Minecraft through the Mavenizer on first build. |
| Forge | 66.1.0 for Minecraft 26.3 | Mappings are Mojang's official names. |

The first build downloads and decompiles Minecraft, which takes a few minutes. Avoid running a
second Gradle build (for example an IDE import) while that is in progress: both write the same
cache and can corrupt each other's output. If setup fails with a zip or EOF error, wait for the
other process to finish and build again.

## Common tasks

```
./gradlew build                    # compile and test every module, one jar per Minecraft version in build/release
./gradlew :common:test             # unit tests (they all live in common)
./gradlew :forge-26.3:runClient    # start a development client from ./run
./gradlew :forge-26.3:runServer    # start a development dedicated server from ./run
```

Shared properties live in the root `gradle.properties`: `mod_version`, the mod metadata, the list
of platform modules (`platforms`) and the library versions `common` compiles against. Each
platform module has its own `gradle.properties` with its Minecraft, loader and Java versions.
`mods.toml` and `pack.mcmeta` are filled in from them at build time.

## Repository layout

```
common/                        version-free code, built and tested without Minecraft
  src/main/java                content, model, hud, physics (see ARCHITECTURE.md)
  src/main/resources           lang files shared by every platform
  src/test/java                all unit tests, including the boundary test
forge-26.3/                    the Minecraft 26.3 / Forge 66 platform module
  src/main/java                rendering, networking, registration, entities, events
  src/main/resources           mods.toml, pack.mcmeta, item models, damage type data
  gradle.properties            Minecraft, Forge and Java versions for this module
docs/                          documentation and content format guides
.github/workflows              CI and release pipelines
```

### Common and platform modules

1.1.0 brings older Minecraft versions, so the code is split in two:

- **`common`** holds everything that does not depend on the game version: definition parsing,
  the content registry, model loading, the HUD expression language, recipe conversion, rotation
  and impact maths. Its classpath has only the JDK, SLF4J, JSpecify and JOML (all shipped by every
  supported Minecraft version), so a stray Minecraft import fails to compile, and
  `CommonBoundaryTest` names the offending line.
- **A platform module per Minecraft version** (`forge-26.3` today) holds the glue: entities,
  rendering, networking, registration, config, commands. It compiles `common`'s sources together
  with its own, so each version ships as one jar and runs as one module in development.

A feature that is pure logic goes in `common` with a unit test; only the part that touches
Minecraft goes in the platform module. Adding a version means adding a module directory, listing
it in `platforms`, and porting the platform code; `common` should not need to change. The run
folder `./run` is shared for now; later modules will get their own.

IDEs import the project as three Gradle projects (root, `common`, `forge-26.3`). After pulling the
split, re-import the Gradle project so the IDE picks up the new source folders and run
configurations; the old root-level `run*.launch` files point at a project that no longer exists.

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
Minecraft version (`mcheli-<minecraft>-<version>.jar`, gathered by the `releaseJars` task into
`build/release`). One tag always releases every platform module at the same mod version. Versions
containing a hyphen (`-alpha.N`, `-beta.N`, `-rc.N`) are marked as pre-releases.

Every release, pre-releases included, is mirrored to the public repository and tagged there too.
Only full releases (`1.0.0`, `1.1.0`, ...) are uploaded to CurseForge; betas and release
candidates stay on GitHub, where anyone can still download them from the public repository's
releases page. See [the release channels](ROADMAP.md#release-channels).

## Original content

Neither the repository nor the jar may contain EMB4's files. Don't commit anything from the zip
or from `run/mcheli/imported`. As a guard, the `jar` task excludes everything under
`assets/mcheli` except `lang` and the four fallback item models of the generic vehicle items.
After a build, check every jar with:

```
for jar in build/release/*.jar; do unzip -l "$jar" | grep -v "com/"; done
```

which should list only the manifest, `mods.toml`, `pack.mcmeta`, the two lang files, the four
generic item models and the damage type data.

When the conversion changes, change `content.LegacyImporter` and bump its `FORMAT`, so existing
installs, the dev run and the tests all convert again. Keys the rework adds go into the lang files
in `common/src/main/resources`; `LegacyImporterTest` fails if one reuses a key the import writes.

To force a fresh import, delete `run/mcheli/imported` (dev run) or `common/build/legacy-content` (tests).
