# Contributing

Thanks for helping with MCHeli Rework. Bug reports, fixes and new features are welcome. Please
read this page first; pull requests that skip these rules will be asked to change before merging.

## The one hard rule: no original files

MCHeli's vehicles, models, textures, sounds and HUD scripts are EMB4's work and All Rights
Reserved. They are not in this repository and must never be added, not even a single texture.
Players import them from their own copy of `MC1.7.10_mcheli_1.0.3.zip` (see the README).

* Do not commit anything from the zip, from `run/mcheli/imported` or from `build/legacy-content`.
* Do not attach original files to issues; name the vehicle instead.
* Screenshots and short clips of the game are fine.

## Setting up

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md). In short: JDK 25, put the original zip in
`Mod/` (git-ignored), then `./gradlew build`. Without the zip the build passes but the content
tests are skipped, as on CI, so run the tests with the zip before opening a pull request that
touches content loading, models or HUDs.

## Workflow

1. Fork the repository, or create a branch if you have access: `git checkout main && git pull`,
   then `git checkout -b <type>/<topic>`, for example `fix/rotor-pivot` or `feat/night-vision`.
2. Make the change, with tests where the code allows it (the `content`, `model` and `hud`
   packages are plain Java and easy to test).
3. Update [CHANGELOG.md](CHANGELOG.md) under `Unreleased` (Keep a Changelog format) and the
   relevant page in `docs/`: [ARCHITECTURE.md](docs/ARCHITECTURE.md) for design changes,
   [TESTING.md](docs/TESTING.md) for anything that needs an in-game check, `docs/content` for
   definition keys.
4. Commit and open a pull request against `main`. CI must pass, and `main` only accepts changes
   through pull requests.

## Commits

* **Signed commits only** (`git commit -S`). `main` rejects unsigned commits. See GitHub's guide
  to [signing commits](https://docs.github.com/en/authentication/managing-commit-signature-verification/signing-commits).
* **One file per commit**, with a message saying what changed in that file and why. A deleted or
  moved file is its own commit. Order the commits so each one builds where practical.
* Plain, specific messages in the imperative: "Fix the tail rotor pivot on the AH-64", not
  "fixes" or "update file".

## Style

* Code reads like the code around it: same naming, comment density and structure. Keep the
  `content`, `model` and `hud` packages free of Minecraft and Forge imports.
* Comments, docs and messages are written plainly, the way an experienced developer writes
  them. No marketing tone, no filler, and no em-dashes (use commas, colons, parentheses or a
  plain hyphen).
* Java files are UTF-8 with LF line endings (`.gitattributes` takes care of this).

## Reporting bugs

Use the bug report template. Attach `logs/latest.log` (and the crash report, if there is one),
name the vehicle, and say what you did and what you expected.
