package com.omicronapplications.m3ulib;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class M3UParserTest {
    private static final long TEST_TIMEOUT = 1000; // ms
    private static final String EMPTY_M3U = "empty.m3u";
    private static final String TEST_M3U = "test.m3u";
    private static final M3UFile TEST_M3U1 = new M3UFile(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir().getAbsolutePath(), "TestSong.d00", "Some Author", "Some Title", 123000);
    private static final M3UFile TEST_M3U2 = new M3UFile(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir().getAbsolutePath(), "Other song.adl", "- Author with - hyphens -", "  Other  title  with  spaces  ", 456000);
    private static final M3UFile TEST_M3U3 = new M3UFile(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir().getAbsolutePath(), "Some song.s3m", "  Other  author  with  spaces  ", "- Title with - hyphens -", 789000);
    private static final String TEST_PATH1 = "/path/to";
    private static final String TEST_SONG1 = "song.d00";
    private static final String TEST_PATH2 = "/some other/longer path";
    private static final String TEST_SONG2 = "some song.adl";
    private static final String TEST_PATH3 = "root";
    private static final String TEST_SONG3 = "another song.s3m";
    private static final String M3U_FILE =
                    "#EXTM3U" + System.getProperty("line.separator") +
                    System.getProperty("line.separator") +
                    "#EXTINF:123, Some author - Some title" + System.getProperty("line.separator") +
                    TEST_PATH1 + File.separator + TEST_SONG1 + System.getProperty("line.separator") +
                    System.getProperty("line.separator") +
                    "#EXTINF:456, Other author - Other title" + System.getProperty("line.separator") +
                    TEST_PATH2 + File.separator + TEST_SONG2 + System.getProperty("line.separator") +
                    System.getProperty("line.separator") +
                    "#EXTINF:789, An author - A title" + System.getProperty("line.separator") +
                    TEST_PATH3 + File.separator + TEST_SONG3 + System.getProperty("line.separator");

    private Context mAppContext;
    private File mEmptyM3U;
    private File mTestM3U;
    private M3UParser mParser;
    private M3UCallback mCallback;
    private CountDownLatch mLatch;
    boolean mLoaded;
    List<M3UFile> mSongs;
    String mStr;

    private File fileFromString(File path, String fileName, String str) {
        File f = new File(path, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(f);
            PrintStream ps = new PrintStream(fos);
            ps.print(str);
        } catch (IOException e) {
            assertFalse(e.getMessage(), false);
        }
        return f;
    }

    private class M3UCallback implements IM3UCallback {
        @Override
        public void onM3ULoaded(boolean isLoaded) {
            mLoaded = isLoaded;
            mLatch.countDown();
        }

        @Override
        public void onM3UList(List<M3UFile> songs) {
            mSongs = songs;
            mLatch.countDown();
        }

        @Override
        public void onM3UWrite(File list) {
            mLatch.countDown();
        }

        @Override
        public void onM3UDump(String str) {
            mStr = str;
            mLatch.countDown();
        }
    }

    private void await() {
        try {
            assertTrue(mLatch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            assertFalse(e.getMessage(), false);
        }
    }

    @Before
    public void setup() {
        mAppContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File filesDir = mAppContext.getFilesDir();
        mEmptyM3U = new File(filesDir, EMPTY_M3U);
        mTestM3U = fileFromString(filesDir, TEST_M3U, M3U_FILE);
        mCallback = new M3UCallback();
    }

    @After
    public void shutdown() {
        mEmptyM3U.delete();
        mTestM3U.delete();
    }

    @Test
    public void testSongs() {
        mParser = new M3UParser(mAppContext, mCallback);
        mLatch = new CountDownLatch(2);
        mParser.load(EMPTY_M3U, M3UParser.STORAGE_INTERNAL);
        await();
        assertTrue(mLoaded);
        assertTrue(mParser.isLoaded());

        mLatch = new CountDownLatch(1);
        mParser.listSongs();
        await();
        assertFalse(mParser.songExists(mSongs, TEST_M3U1));
        assertFalse(mParser.songExists(mSongs, TEST_M3U2));
        assertFalse(mParser.songExists(mSongs, TEST_M3U3));

        mLatch = new CountDownLatch(1);
        mParser.addSong(TEST_M3U1);
        await();

        mLatch = new CountDownLatch(1);
        mParser.listSongs();
        await();
        assertTrue(mParser.songExists(mSongs, TEST_M3U1));
        assertEquals(0, mParser.songIndex(mSongs, TEST_M3U1));

        mLatch = new CountDownLatch(1);
        mParser.addSong(TEST_M3U2);
        await();

        mLatch = new CountDownLatch(1);
        mParser.listSongs();
        await();
        assertTrue(mParser.songExists(mSongs, TEST_M3U2));
        assertEquals(1, mParser.songIndex(mSongs, TEST_M3U2));

        mLatch = new CountDownLatch(1);
        mParser.addSong(TEST_M3U3);
        await();
        assertTrue(mParser.songExists(mSongs, TEST_M3U3));
        assertEquals(2, mParser.songIndex(mSongs, TEST_M3U3));

        mLatch = new CountDownLatch(1);
        mParser.listSongs();
        await();
        assertNotNull(mSongs);
        assertEquals(3, mSongs.size());

        mLatch = new CountDownLatch(1);
        mParser.dump();
        await();
        assertNotNull(mStr);

        mLatch = new CountDownLatch(2);
        mParser.removeSong(TEST_M3U1);
        mParser.listSongs();
        await();
        assertFalse(mParser.songExists(mSongs, TEST_M3U1));

        mLatch = new CountDownLatch(1);
        mParser.removeSong(TEST_M3U2);
        await();
        assertFalse(mParser.songExists(mSongs, TEST_M3U2));

        mLatch = new CountDownLatch(1);
        mParser.removeSong(TEST_M3U3);
        await();
        assertFalse(mParser.songExists(mSongs, TEST_M3U3));

        mLatch = new CountDownLatch(1);
        mParser.unload();
        await();
        assertTrue(mEmptyM3U.exists());
    }

    @Test
    public void testM3U() {
        mParser = new M3UParser(mAppContext, mCallback);
        mLatch = new CountDownLatch(2);
        mParser.load(mTestM3U);
        await();
        assertTrue(mParser.isLoaded());

        mLatch = new CountDownLatch(1);
        mParser.listSongs();
        await();
        assertNotNull(mSongs);
        assertEquals(3, mSongs.size());

        assertTrue(mParser.songExists(mSongs, TEST_PATH1, TEST_SONG1));
        assertEquals(0, mParser.songIndex(mSongs, TEST_PATH1, TEST_SONG1));

        assertTrue(mParser.songExists(mSongs, TEST_PATH2, TEST_SONG2));
        assertEquals(1, mParser.songIndex(mSongs, TEST_PATH2, TEST_SONG2));

        assertTrue(mParser.songExists(mSongs, TEST_PATH3, TEST_SONG3));
        assertEquals(2, mParser.songIndex(mSongs, TEST_PATH3, TEST_SONG3));

        mLatch = new CountDownLatch(1);
        mParser.dump();
        await();
        assertEquals(M3U_FILE, mStr);

        mLatch = new CountDownLatch(1);
        mParser.removeSong(TEST_PATH1, TEST_SONG1);
        await();
        mLatch = new CountDownLatch(1);
        mParser.removeSong(TEST_PATH2, TEST_SONG2);
        await();
        mLatch = new CountDownLatch(1);
        mParser.removeSong(TEST_PATH3, TEST_SONG3);
        await();

        mLatch = new CountDownLatch(1);
        mParser.listSongs();
        await();
        assertEquals(0, mSongs.size());

        mLatch = new CountDownLatch(1);
        mParser.unload();
        await();
        assertTrue(mTestM3U.exists());
   }
}
