# Seento

**A playlist manager for compatible Suunto devices.** Create and organize
playlists on your phone, then sync them with your device over Bluetooth.

Seento is an independent project and is not affiliated with, endorsed by, or
officially connected to Suunto. Compatibility has so far been tested with one
Suunto Run only; compatibility with other devices has not been verified.

English | [Italiano](#seento-in-italiano)

## Contents

- [Features](#features)
- [Quick start](#quick-start)
- [Development](#development)
- [Licenses](#licenses)
- [Seento in italiano](#seento-in-italiano)

## Features

- Browse the music catalog and playlists on a compatible device.
- Create, rename, copy, reorder, and delete playlists locally.
- Import playlists from M3U files.
- Add device tracks to a playlist.
- Send playlists to the device, overwrite an existing playlist, or make a copy.
- Import and manage playlists stored on the device.
- English and Italian interface.

## Quick start

1. Turn on Bluetooth and keep your compatible Suunto device nearby.
2. Open **Device**, start a search, grant Android's requested Bluetooth
   permissions, and connect to the device.
3. Read or refresh the music catalog to load its tracks and playlists.
4. Open **Playlists** to create a playlist or import an M3U file. Open a
   playlist to add device tracks, reorder them, or rename the playlist.
5. Select **Send playlist** to transfer it to the device. If a playlist with
   the same name already exists, choose whether to overwrite it or create a
   copy.

Playlist data is stored locally. Tracks refer to audio files already on the
device; Seento does not transfer music files.

## Development

Seento is an Android application built with Kotlin and Jetpack Compose. The
Movesense/Suunto MDS Android SDK is a separate dependency and is not included in
this repository. Obtain the SDK from Movesense and follow its applicable terms:

- [Movesense mobile library downloads](https://bitbucket.org/movesense/movesense-mobile-lib/downloads/)
- Expected local file: `mdslib-3.33.7-release.aar`

Set the absolute path to the AAR in the ignored root `.env` file:

```dotenv
MDSLIB_AAR=/absolute/path/to/mdslib-3.33.7-release.aar
```

Then build and install the debug app from Android Studio, or build it from the
repository root:

```sh
./gradlew :app:assembleDebug
```

You can also provide the AAR path for one build with
`-PmdslibAar=/absolute/path/to/mdslib-3.33.7-release.aar`.

The SHA-256 of the previously tested 3.33.7 AAR is:

```text
fb4cb186601012fe97ea708e94e5501266a83facf231503a0502590a7b8900bd
```

## Licenses

The Seento source code and original artwork are licensed under the Apache
License 2.0; see [LICENSE](LICENSE). The Inter font is distributed under the
SIL Open Font License, included in the app assets. The MDS SDK is a separate
third-party component and is not covered by Seento's license.

---

## Seento in italiano

**Un gestore di playlist per dispositivi Suunto compatibili.** Crea e
organizza le playlist sul telefono e sincronizzale con il dispositivo via
Bluetooth.

Seento è un progetto indipendente, non affiliato a Suunto né approvato o
ufficialmente collegato all'azienda. Finora la compatibilità è stata testata
con un solo Suunto Run; quella con altri dispositivi non è stata verificata.

### Funzionalità

- Consulta il catalogo musicale e le playlist presenti sul dispositivo.
- Crea, rinomina, copia, riordina ed elimina playlist locali.
- Importa playlist da file M3U.
- Aggiungi a una playlist i brani presenti sul dispositivo.
- Invia playlist al dispositivo, sovrascrivi quelle esistenti o creane una
  copia.
- Importa e gestisci le playlist presenti sul dispositivo.
- Interfaccia in italiano e inglese.

### Guida rapida

1. Attiva il Bluetooth e tieni vicino il dispositivo Suunto compatibile.
2. Apri **Dispositivo**, avvia la ricerca, concedi i permessi Bluetooth
   richiesti da Android e connettiti al dispositivo.
3. Leggi o aggiorna il catalogo musicale per caricare brani e playlist.
4. Apri **Playlist** per crearne una o importare un file M3U. Apri una playlist
   per aggiungere brani dal dispositivo, riordinarli o rinominarla.
5. Seleziona **Invia playlist**. Se sul dispositivo esiste già una playlist
   con lo stesso nome, scegli se sovrascriverla o crearne una copia.

I dati delle playlist vengono salvati localmente. I brani sono riferimenti ai
file audio già presenti sul dispositivo; Seento non trasferisce file musicali.

### Sviluppo

Seento è un'app Android sviluppata in Kotlin e Jetpack Compose. L'SDK Android
Movesense/Suunto MDS è una dipendenza separata e non è incluso in questo
repository. Scarica l'SDK da Movesense e rispettane le condizioni applicabili:

- [Download della libreria mobile Movesense](https://bitbucket.org/movesense/movesense-mobile-lib/downloads/)
- File locale atteso: `mdslib-3.33.7-release.aar`

Inserisci il percorso assoluto dell'AAR nel file `.env` ignorato da Git nella
cartella principale:

```dotenv
MDSLIB_AAR=/percorso/assoluto/mdslib-3.33.7-release.aar
```

Poi compila e installa l'app di debug da Android Studio, oppure compila dal
terminale nella cartella principale:

```sh
./gradlew :app:assembleDebug
```

Per una singola compilazione puoi anche passare il percorso con
`-PmdslibAar=/percorso/assoluto/mdslib-3.33.7-release.aar`.

SHA-256 dell'AAR 3.33.7 verificato in precedenza:

```text
fb4cb186601012fe97ea708e94e5501266a83facf231503a0502590a7b8900bd
```

### Licenze

Il codice sorgente e le grafiche originali di Seento sono distribuiti con
licenza Apache 2.0; vedere [LICENSE](LICENSE). Il font Inter è distribuito con
licenza SIL Open Font License, inclusa negli asset dell'app. L'SDK MDS è un
componente di terze parti separato e non è coperto dalla licenza di Seento.
