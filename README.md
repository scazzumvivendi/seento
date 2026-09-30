# Seento

Seento is an Android playlist manager for compatible Suunto devices.

The Seento source code and original artwork are licensed under the Apache
License, Version 2.0. See [LICENSE](LICENSE). The Inter font is distributed
under the SIL Open Font License; its license text is included in the app
assets. The Movesense/Suunto MDS SDK is a separate third-party component and
is not covered by this project license.

## MDS SDK dependency

The Movesense/Suunto MDS Android SDK is not included in this repository. The
SDK is distributed under its own evaluation/development agreement and must be
obtained directly from Movesense. Do not commit the AAR, native libraries, or
an APK containing them to this repository.

Official SDK downloads:

<https://bitbucket.org/movesense/movesense-mobile-lib/downloads/>

The current local build expects:

`mdslib-3.33.7-release.aar`

After obtaining the SDK under the applicable license terms, build locally by
putting its absolute path in the ignored root `.env` file:

```dotenv
MDSLIB_AAR=/absolute/path/to/mdslib-3.33.7-release.aar
```

Then build with:

```sh
./gradlew :app:assembleDebug
```

You can still override the local value for a single build with
`-PmdslibAar=/absolute/path/to/mdslib-3.33.7-release.aar`.

The AAR used during development must not be uploaded to GitHub Releases or
redistributed through another store without the required permission from
Movesense/Amer Sports Digital.

The SHA-256 of the previously tested `3.33.7` AAR is:

```text
fb4cb186601012fe97ea708e94e5501266a83facf231503a0502590a7b8900bd
```
