package com.omicronapplications.m3ulib;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class M3UParserTest {
    private static final String TEST_M3U = "test.m3u";
    private static final String TEST_SONG1 = "TestSong.d00";
    private static final String TEST_SONG2 = "OtherSong.d00";
    private static final String TEST_SONG3 = "SomeSong.d01";

    private Context mAppContext;
    private File mDir;
    private File mM3U;
    private String mSong1, mSong2, mSong3;
    private File mFile1, mFile2, mFile3;
    private M3UParser mParser;

    @Before
    public void setup() {
        mAppContext = InstrumentationRegistry.getTargetContext();
        mDir = mAppContext.getFilesDir();
        mSong1 = mDir.getAbsolutePath() + File.separator + TEST_SONG1;
        mSong2 = mDir.getAbsolutePath() + File.separator + TEST_SONG2;
        mSong3 = mDir.getAbsolutePath() + File.separator + TEST_SONG3;
        mFile1 = new File(mSong1);
        mFile2 = new File(mSong2);
        mFile3 = new File(mSong3);
        mM3U = new File(mDir, TEST_M3U);
        mM3U.delete();
    }

    @Test
    public void testFiles() {
        mParser = new M3UParser(mAppContext);
        mParser.load(mM3U);

        assertFalse(mParser.fileExists(mFile1));
        assertFalse(mParser.fileExists(mFile2));
        assertFalse(mParser.fileExists(mFile3));

        assertTrue(mParser.addFile(mFile1));
        assertTrue(mParser.fileExists(mFile1));

        assertTrue(mParser.addFile(mFile2));
        assertTrue(mParser.fileExists(mFile2));

        assertTrue(mParser.addFile(mFile3));
        assertTrue(mParser.fileExists(mFile3));
        ArrayList<File> files = mParser.getFiles();
        assertNotNull(files);
        assertEquals(files.size(), 3);
        StringBuilder sb = new StringBuilder();
        for (File file : files) {
            sb.append(file.getAbsolutePath());
            sb.append(System.getProperty("line.separator"));
        }
        String dump = mParser.dump();
        assertEquals(dump, sb.toString());

        assertTrue(mParser.removeFile(mFile1));
        assertFalse(mParser.fileExists(mFile1));
        assertTrue(mParser.removeFile(mFile2));
        assertFalse(mParser.fileExists(mFile2));
        assertTrue(mParser.removeFile(mFile3));
        assertFalse(mParser.fileExists(mFile3));

        mParser.unload();
        assertTrue(mM3U.exists());
        assertNull(mParser.getSongs());
    }

    @Test
    public void testSongs() {
        mParser = new M3UParser(mAppContext);
        mParser.load(TEST_M3U, M3UParser.STORAGE_INTERNAL);

        assertFalse(mParser.songExists(mSong1));
        assertFalse(mParser.songExists(mSong2));
        assertFalse(mParser.songExists(mSong3));

        assertTrue(mParser.addSong(mSong1));
        assertTrue(mParser.songExists(mSong1));

        assertTrue(mParser.addSong(mSong2));
        assertTrue(mParser.songExists(mSong2));

        assertTrue(mParser.addSong(mSong3));
        assertTrue(mParser.songExists(mSong3));
        ArrayList<String> songs = mParser.getSongs();
        assertNotNull(songs);
        assertEquals(songs.size(), 3);
        StringBuilder sb = new StringBuilder();
        for (String song : songs) {
            sb.append(song);
            sb.append(System.getProperty("line.separator"));
        }
        String dump = mParser.dump();
        assertEquals(dump, sb.toString());

        assertTrue(mParser.removeSong(mSong1));
        assertFalse(mParser.songExists(mSong1));
        assertTrue(mParser.removeSong(mSong2));
        assertFalse(mParser.songExists(mSong2));
        assertTrue(mParser.removeSong(mSong3));
        assertFalse(mParser.songExists(mSong3));

        mParser.unload();
        File m3u = new File(mDir, TEST_M3U);
        assertTrue(m3u.exists());
        assertNull(mParser.getSongs());
    }
}
