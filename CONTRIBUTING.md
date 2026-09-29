# Development and contribution guide

## Local setup

1. Use JDK 21 and Android Studio or the Android command-line tools.
2. Install Android SDK platforms 36 and 37. The library compiles against API 36;
   the demo needs API 37 for its AndroidX Core dependency.
3. Configure the local SDK path in `local.properties`. Do not commit that file.
4. Use the checked-in Gradle wrapper. AGP 9 supplies built-in Kotlin; do not add a
   legacy Kotlin Android plugin alongside it.
5. Start an emulator or connect a device for instrumented tests. The current suite
   has been verified on API 36; minimum supported API remains 24.

On macOS, Android Studio's bundled JDK can be selected for the current shell:

```shell
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
```

## Verification

Run from the repository root:

```shell
./gradlew :charts:testDebugUnitTest :charts:connectedDebugAndroidTest \
  :app:assembleDebug :charts:lintDebug :app:lintDebug :charts:assembleRelease
```

Without a device, omit `:charts:connectedDebugAndroidTest` and explicitly record that
instrumented tests were not run. Do not describe compiled tests as executed tests.
Reports are under each module's `build/reports/`; do not check them into Git.

For a local consumer, publish using
`./gradlew :charts:publishReleasePublicationToMavenLocal`. This writes to the local
Maven cache; it does not upload artifacts to a remote repository.

## Change guidelines

- Keep code identifiers, comments, documentation and demo text in English.
- Keep library APIs independent of Material. Place app-specific integration in `:app`.
- Preserve stable ID, range, fill-ownership and collision contracts. See the
  [module guide](charts/README.md) before changing public behavior.
- Pair geometry/model changes with local unit tests. Use Compose instrumented tests
  for pixel output, measured label layout, semantics and animation behavior.
- Keep rendering and placement deterministic and bounded. Custom layers must supply
  obstacles if labels should avoid their drawing.
- Update documentation when changing defaults, API requirements or supported features.
- Never commit local SDK paths, IDE state, build output, signing keys or credentials.
  Check staged paths and diffs before committing; ignore rules are not a secret scanner.
- Add a license only after the project owner selects one. None is supplied by this
  initial import; do not infer permission to redistribute from publication support.

## Commit conventions and imported history

The repository begins with an intentionally empty root commit. The implementation
already existed when Git was initialized; subsequent import commits group that
snapshot by task. They are not a reconstruction of every earlier edit or evidence
that each intermediate snapshot passed a build. Validate the complete branch.

Use `type(scope): imperative summary`, for example `feat(labels): add staggered
placement`. Include a body explaining the behavior, relevant constraints and checks
actually performed. Group related changes; do not mix unrelated tasks or include
generated artifacts just to make a commit larger.

The initial import separates build setup, models/styles, geometry, label placement,
animation, rendering infrastructure, chart types, tests, the demo and documentation.
Use `git log --reverse --format=fuller` to inspect the task descriptions, and
`git show --stat <commit>` to inspect the file boundaries.

Before committing, review:

```shell
git status --short
git diff --cached --stat
git diff --cached --check
git diff --cached
```

Stage explicit paths instead of all files indiscriminately. Preserve other people's
uncommitted work and do not rewrite published history. Remotes, hosting, release
signing and CI services are intentionally not configured by the initial import.
