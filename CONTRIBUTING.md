# Contributing

Anyone is welcome to contribute. All changes land through pull requests; nobody pushes to `main` directly.

## How

1. Fork the repo (or, if you have write access, create a branch).
2. Make your change on a branch and open a pull request against `main`.
3. CI builds the mod and runs the spell tests (`./gradlew build` and `./gradlew runGametest`). Both must pass.
4. Claude reviews the pull request automatically and leaves comments. For pull requests from forks, the
   maintainer starts that review by commenting `@claude review`.
5. The maintainer ([@avi-it](https://github.com/avi-it)) approves and merges. Every pull request needs that
   approval before it can be merged.

## Rules

- **Clean room only.** Do not add code, textures, sounds or models copied or decompiled from the original
  Minegicka III (by WilliamEze) or from Minecraft. Reimplement behaviour in your own code; generate art with
  the scripts in `tools/` or draw it yourself.
- Keep the spell tests green, and add a GameTest in `SpellGameTests` when you fix a spell bug.
- By contributing you agree your contribution is licensed under the [MIT License](LICENSE).

## Building

JDK 25 is required.

```
./gradlew build
./gradlew runGametest
./gradlew runClientGametest
```
