# M3UParser

> M3U playlist parser

## Description

M3UParser is an example of a minimalistic M3U player parser class.

M3UParser is used in [AndPlug](https://play.google.com/store/apps/details?id=com.omicronapplications.andplug) music player application for Android devices.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Testing](#testing)
- [Usage](#usage)
- [Example](#example)
- [Credits](#credits)
- [Release History](#release-history)
- [License](#license)

## Prerequisites

- [Android 4.0.3](https://developer.android.com/about/versions/android-4.0.3) (API Level: 15) or later (`ICE_CREAM_SANDWICH_MR1`)
- [Android Gradle Plugin](https://developer.android.com/studio/releases/gradle-plugin) 3.4.1 or later (`gradle:3.4.1`)

## Installation

1. Check out a local copy of M3UParser repository
2. Build library with Gradle, using Android Studio or directly from the command line

## Testing

M3UParser includes a set of instrumented unit tests.

### Instrumented tests

Located under `m3ulib/src/androidTest`.

These tests are run on a hardware device or emulator, and verifies correct operation of the `M3UParser` implementation.

## Usage

M3UParser is controlled through the following class:
- `M3UParser` - M3U playlist parser class 

## Example

Create new `M3UParser` instance, set playlist file, and create M3U file as needed:

```
M3UParser parser = new M3UParser(getApplicationContext());
File dir = getApplicationContext().getFilesDir();
File playlist = new File(dir, "playlist.m3u");
parser.load(playlist);
```

Add song to playlist, and write updated M3U file to disk:

```
File dir = getApplicationContext().getFilesDir();
String song = dir.getAbsolutePath() + File.separator + "song.mp3";
parser.addSong(song);
```

List all songs in playlist:

```
ArrayList<String> songs = parser.getSongs();
for (String song : songs) {
    // ...
}
```

Remove song from playlist, and write updated M3U file to disk:

```
parser.removeSong(song);
```

Unset current playlist, and write M3U file to disk:

```
parser.unload();
```

## Credits

Copyright (C) 2019 [Fredrik Claesson](https://github.com/omicronapps)

## Release History

- 1.0.0 Initial release

## License

M3UParser is licensed under [GNU LESSER GENERAL PUBLIC LICENSE](LICENSE).
