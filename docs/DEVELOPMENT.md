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
./gradlew build          # compile, run unit tests, produce build/libs/mcheli-26.3-<version>.jar
./gradlew test           # unit tests only
./gradlew runClient      # start a development client from ./run
./gradlew runServer      # start a development dedicated server from ./run
```

Project properties live in `gradle.properties` (`mod_version`, Forge and Minecraft versions).
`mods.toml` and `pack.mcmeta` are filled in from them at build time.

## Repository layout

```
src/main/java/com/jnhprojects/mcheli   mod sources, see ARCHITECTURE.md
src/main/resources                     the rework's own lang keys, damage type and metadata
src/test/java                          unit tests
docs/                                  documentation and content format guides
.github/workflows                      CI and release pipelines
```

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
request. The jar is attached to the run as an artifact for 14 days; test reports are uploaded
when the build fails.

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
`CHANGELOG.md` section as release notes, and publishes a GitHub release with the jar. Versions
containing a hyphen (`-alpha.N`, `-beta.N`, `-rc.N`) are marked as pre-releases.

## Original content

Neither the repository nor the jar may contain EMB4's files. Don't commit anything from the zip
or from `run/mcheli/imported`. As a guard, the `jar` task excludes everything under
`assets/mcheli` except `lang`. After a build, check with:

```
unzip -l build/libs/mcheli-26.3-<version>.jar | grep -v "com/"
```

which should list only the manifest, `mods.toml`, `pack.mcmeta`, the two lang files and the
damage type data.

When the conversion changes, change `content.LegacyImporter` and bump its `FORMAT`, so existing
installs, the dev run and the tests all convert again. Keys the rework adds go into the lang files
in `src/main/resources`; `LegacyImporterTest` fails if one reuses a key the import writes.

To force a fresh import, delete `run/mcheli/imported` (dev run) or `build/legacy-content` (tests).
