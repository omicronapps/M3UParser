# M3UParser

> M3U playlist parser

## Description

M3UParser implements support for M3U playlists, including extended M3U support.

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
- [Android Gradle Plugin](https://developer.android.com/studio/releases/gradle-plugin) 4.1.1 or later (`gradle:4.1.1`)

## Installation

1. Check out a local copy of M3UParser repository
2. Build library with Gradle, using Android Studio or directly from the command line

## Testing

M3UParser includes a set of instrumented unit tests.

### Instrumented tests

Located under `m3ulib/src/androidTest`.

These tests are run on a hardware device or emulator, and verifies correct operation of the `M3UParser` implementation.

## Usage

M3UParser is controlled through the following classes:
- `M3UParser` - M3U playlist parser class 
- `IM3UCallback` - callback interface

## Example

Implement `IM3UCallback` callback interface:
```
import com.omicronapplications.m3ulib.IM3UCallback;
import com.omicronapplications.m3ulib.M3UFile;

private class M3UCallback implements IM3UCallback {
    @Override
    public void onM3UList(List<M3UFile> songs) {
        // List of playlist songs received
    }

    @Override
    public void onM3UWrite(File list) {
        // Playlist file written to disk
    }

    @Override
    public void onM3UDump(String str) {
        // Playlist file contents received
    }
}
```

Create new `M3UParser` instance, set playlist file, and create M3U file as needed:
```
import com.omicronapplications.m3ulib.IM3UCallback;
import com.omicronapplications.m3ulib.M3UParser;

IM3UCallback callback = new M3UCallback();
M3UParser parser = new M3UParser(getApplicationContext(), callback);
File dir = getApplicationContext().getFilesDir();
File playlist = new File(dir, "playlist.m3u");
parser.load(playlist);
```

Add song to playlist, and write updated M3U file to disk:
```
String path = getApplicationContext().getFilesDir().getAbsolutePath();
String name = "song.mp3";
parser.addSong(path, name);

public void onM3UWrite(File list) {
    // ...
}
```

List all songs in playlist:
```
parser.listSongs();

public void onM3UList(List<M3UFile> songs) {
    for (M3UFile m3u : songs) {
        // ...
    }
}
```

Remove song from playlist, and write updated M3U file to disk:
```
parser.removeSong(path, name);

public void onM3UWrite(File list) {
    // ...
}
```

Unset current playlist, and write M3U file to disk:
```
parser.unload();

public void onM3UWrite(File list) {
    // ...
}
```

## Credits

Copyright (C) 2019-2020 [Fredrik Claesson](https://github.com/omicronapps)

## Release History

- 1.0.0 Initial release
- 1.1.0 Support for additional external storage device, where available (Android KitKat 4.4 and later only)
- 1.2.0 Migrated to AndroidX
- 1.3.0 Extended M3U support

## License

M3UParser is licensed under [GNU LESSER GENERAL PUBLIC LICENSE](LICENSE).
