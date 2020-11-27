package com.omicronapplications.m3ulib;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3UParser {
    public static final int STORAGE_ILLEGAL = -1;
    public static final int STORAGE_INTERNAL = 0;
    public static final int STORAGE_EXTERNAL_1 = 1;
    public static final int STORAGE_EXTERNAL_2 = 2;
    private static final String TAG = "M3UParser";
    private static final String EXTM3U = "#EXTM3U";
    private static final String EXTINF = "#EXTINF";
    private static final int M3U_LOAD = 1;
    private static final int M3U_UNLOAD = 2;
    private static final int M3U_ADD = 3;
    private static final int M3U_REMOVE = 4;
    private static final int M3U_LIST = 5;
    private static final int M3U_DUMP = 6;
    private static final String BUNDLE_EXTENDED = "extended";
    private static final String BUNDLE_PATH = "path";
    private static final String BUNDLE_NAME = "name";
    private static final String BUNDLE_AUTHOR = "author";
    private static final String BUNDLE_TITLE = "title";
    private static final String BUNDLE_SONGLENGTH = "songlength";
    private static final String BUNDLE_LIST = "list";
    private static final String BUNDLE_STORAGE = "storage";
    private final Context mContext;
    private HandlerThread mM3UThread;
    private Handler mM3UHandler;
    private M3UHandlerCallback mM3UHandlerCallback;
    private IM3UCallback mCallback;
    private volatile boolean mLoaded;

    public M3UParser(Context context, IM3UCallback callback) {
        mContext = context;
        mCallback = callback;
        mM3UThread = new HandlerThread("M3UParser");
        try {
            mM3UThread.start();
        } catch (IllegalThreadStateException e) {
            Log.e(TAG, "M3UParser: IllegalThreadStateException: " + e.getMessage());
        }
        mM3UHandlerCallback = new M3UHandlerCallback();
        Looper looper = mM3UThread.getLooper();
        mM3UHandler = new Handler(looper, mM3UHandlerCallback);
    }

    private final class M3UHandlerCallback implements Handler.Callback {
        private int mStorage;
        private File mList;
        private List<M3UFile> mSongs;

        public M3UHandlerCallback() {
            mStorage = STORAGE_ILLEGAL;
            mList = null;
            mSongs = null;
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case M3U_LOAD:
                    Bundle data = msg.getData();
                    String list = data.getString(BUNDLE_LIST);
                    mList = new File(list);
                    mStorage = data.getInt(BUNDLE_STORAGE);
                    if (!mList.exists()) {
                        try {
                            mList.createNewFile();
                        } catch (IOException e) {
                            Log.e(TAG, "load: IOException: " + e.getMessage());
                        }
                    }
                    readSongs();
                    mLoaded = (mList != null && mList.exists());
                    break;

                case M3U_UNLOAD:
                    writeSongs();
                    mList = null;
                    mSongs = null;
                    mLoaded = false;
                    break;

                case M3U_ADD:
                    data = msg.getData();
                    boolean extended = data.getBoolean(BUNDLE_EXTENDED);
                    String path = data.getString(BUNDLE_PATH);
                    String name = data.getString(BUNDLE_NAME);
                    M3UFile m3u;
                    if (extended) {
                        String author = data.getString(BUNDLE_AUTHOR);
                        String title = data.getString(BUNDLE_TITLE);
                        long songlength = data.getLong(BUNDLE_SONGLENGTH);
                        m3u = new M3UFile(path, name, author, title, songlength);
                    } else {
                        m3u = new M3UFile(path, name);
                    }
                    if (mSongs != null && !mSongs.contains(m3u)) {
                        mSongs.add(m3u);
                        writeSongs();
                    }
                    break;

                case M3U_REMOVE:
                    data = msg.getData();
                    path = data.getString(BUNDLE_PATH);
                    name = data.getString(BUNDLE_NAME);
                    m3u = new M3UFile(path, name);
                    if (mSongs != null && mSongs.contains(m3u)) {
                        mSongs.remove(m3u);
                        writeSongs();
                    }
                    break;

                case M3U_LIST:
                    if (mCallback != null) {
                        mCallback.onM3UList(mSongs);
                    }
                    break;

                case M3U_DUMP:
                    String str = null;
                    StringWriter sw = new StringWriter();
                    if (writeM3U(sw)) {
                        str = sw.toString();
                    }
                    if (mCallback != null) {
                        mCallback.onM3UDump(str);
                    }

                default:
                    Log.w(TAG, "handleMessage: ignored illegal request: " + msg.what);
                    break;
            }
            return true;
        }

        private void readSongs() {
            boolean readSuccessful = false;
            mSongs = new ArrayList<>();
            if (mList == null) {
                Log.e(TAG, "readSongs: no playlist loaded");
            } else {
                try {
                    FileInputStream fs = new FileInputStream(mList);
                    InputStreamReader sr = new InputStreamReader(fs);
                    BufferedReader br = new BufferedReader(sr);
                    String str = br.readLine();
                    boolean extended = false;
                    while (str != null) {
                        if (str.startsWith(EXTM3U)) {
                            extended = true;
                        } else if (extended && str.startsWith(EXTINF)) {
                            String regex = "^#EXTINF\\s*:\\s*(\\d+)\\s*,\\s*([^-]+)\\s*-\\s*(.*)";
                            Pattern p = Pattern.compile(regex);
                            Matcher m = p.matcher(str);
                            String song = br.readLine();
                            if (m.find() && song != null && !song.isEmpty() && !song.startsWith("#")) {
                                str = m.group(1);
                                long songlength = 1000 * Long.parseLong(str != null ? str : "0");
                                str = m.group(2);
                                String author = str != null ? str.trim() : "";
                                str = m.group(3);
                                String title = str != null ? str.trim() : "";
                                M3UFile m3u = new M3UFile(song, author, title, songlength);
                                mSongs.add(m3u);
                            }
                        } else if (!str.isEmpty() && !str.startsWith("#")) {
                            M3UFile m3u = new M3UFile(str.trim());
                            mSongs.add(m3u);
                        }
                        str = br.readLine();
                    }
                    readSuccessful = true;
                    br.close();
                    sr.close();
                    fs.close();
                } catch (NullPointerException e) {
                    Log.e(TAG, "readSongs: NullPointerException: " + e.getMessage());
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "readSongs: FileNotFoundException: " + e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "readSongs: IOException: " + e.getMessage());
                }
            }
            if (!readSuccessful) {
                Log.w(TAG, "readSongs: read failed");
            } else if (mCallback != null) {
                mCallback.onM3UList(mSongs);
            }
        }

        private void writeSongs() {
            boolean writeSuccessful = false;
            if (mList == null) {
                Log.e(TAG, "writeSongs: no playlist loaded");
            } else if (mSongs == null ){
                Log.e(TAG, "writeSongs: no song array");
            } else {
                try {
                    File dir = getStorage();
                    if (dir == null) {
                        Log.e(TAG, "writeSongs: invalid storage " + dir);
                        return;
                    }
                    File tempList = File.createTempFile("m3u", "tmp", dir);
                    FileOutputStream os = new FileOutputStream(tempList);
                    OutputStreamWriter sw = new OutputStreamWriter(os);
                    BufferedWriter bw = new BufferedWriter(sw);
                    writeM3U(bw);
                    bw.close();
                    sw.close();
                    os.close();
                    if (!mList.delete()) {
                        Log.w(TAG, "writeSongs: delete failed: " + mList.getAbsolutePath());
                    }
                    if (!tempList.renameTo(mList)) {
                        Log.e(TAG, "writeSongs: rename failed: " + tempList.getAbsolutePath() + " -> " + mList.getAbsolutePath());
                    } else {
                        writeSuccessful = true;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "writeSongs: IOException: " + e.getMessage());
                }
            }
            if (!writeSuccessful) {
                Log.w(TAG, "writeSongs: write failed");
            } else if (mCallback != null) {
                mCallback.onM3UWrite(mList);
            }
        }

        private boolean writeM3U(Writer wr) {
            boolean written = false;
            if (mSongs != null) {
                try {
                    wr.write(EXTM3U);
                    wr.append(System.getProperty("line.separator"));
                    for (M3UFile m3u : mSongs) {
                        wr.append(System.getProperty("line.separator"));
                        wr.write(EXTINF);
                        wr.write(":");
                        if (m3u.extended) {
                            int lengthInS = (int) (m3u.songlength / 1000);
                            wr.write(Integer.toString(lengthInS));
                            wr.write(", ");
                            m3u.author = m3u.author.replaceAll("-", "_");
                            wr.append(m3u.author);
                            wr.write(" - ");
                            m3u.title = m3u.title.replaceAll("-", "_");
                            wr.append(m3u.title);
                            wr.append(System.getProperty("line.separator"));
                        }
                        wr.append(m3u.path);
                        wr.write(File.separator);
                        wr.append(m3u.name);
                        wr.append(System.getProperty("line.separator"));
                    }
                    wr.flush();
                    written = true;
                } catch (IOException e) {
                    Log.e(TAG, "writeM3U: IOException: " + e.getMessage());
                }
            }
            return written;
        }

        private File getStorage() {
            File dir = null;
            switch (mStorage) {
                case STORAGE_INTERNAL:
                    dir = mContext.getCacheDir();
                    break;
                case STORAGE_EXTERNAL_1:
                    dir = mContext.getExternalCacheDir();
                    break;
                case STORAGE_EXTERNAL_2:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        File[] externals = mContext.getExternalCacheDirs();
                        if (externals != null && externals.length > 1 && externals[1] != null) {
                            dir = externals[1];
                        } else {
                            Log.w(TAG, "getStorage: storage not available " + externals);
                        }
                    } else {
                        Log.w(TAG, "getStorage: not supported in SDK version " + Build.VERSION.SDK_INT);
                    }
                    break;
                default:
                    Log.w(TAG, "getStorage: storage not supported " + mStorage);
                    break;
            }
            return dir;
        }
    }

    public void load(String name, int storage) {
        File dir = null;
        if (name != null) {
            switch (storage) {
                case STORAGE_INTERNAL:
                    dir = mContext.getFilesDir();
                    break;
                case STORAGE_EXTERNAL_1:
                    dir = mContext.getExternalFilesDir(null);
                    break;
                case STORAGE_EXTERNAL_2:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        File[] externals = mContext.getExternalFilesDirs(null);
                        if (externals != null && externals.length > 1 && externals[1] != null) {
                            dir = externals[1];
                        } else {
                            Log.w(TAG, "load: storage not available " + externals);
                        }
                    } else {
                        Log.w(TAG, "load: Not supported in SDK version " + Build.VERSION.SDK_INT);
                    }
                    break;
                default:
                    Log.w(TAG, "load: storage not supported " + storage);
                    break;
            }
            if (dir == null) {
                Log.e(TAG, "load: invalid storage " + storage);
                return;
            }
            File list = new File(dir, name);
            backgroundLoad(list.getAbsolutePath(), storage);
        }
    }

    public void load(String list) {
        if (list == null || list.isEmpty()) {
            Log.e(TAG, "load: empty path");
        } else {
            int storage = inStorage(list);
            backgroundLoad(list, storage);
        }
    }

    public void load(File list) {
        if (list == null) {
            Log.e(TAG, "load: empty path");
        } else {
            String name = list.getAbsolutePath();
            int storage = inStorage(name);
            backgroundLoad(name, storage);
        }
    }

    public boolean isLoaded() {
        return mLoaded;
    }

    public void unload() {
        backgroundUnload();
    }

    public void addSong(String path, String name) {
        M3UFile m3u = new M3UFile(path, name);
        backgroundAddSong(m3u);
    }

    public void addSong(M3UFile m3u) {
        backgroundAddSong(m3u);
    }

    public void removeSong(String path, String name) {
        M3UFile m3u = new M3UFile(path, name);
        backgroundRemoveSong(m3u);
    }

    public void removeSong(M3UFile m3u) {
        backgroundRemoveSong(m3u);
    }

    public boolean songExists(List<M3UFile> songs, String path, String name) {
        M3UFile m3u = new M3UFile(path, name);
        return songExists(songs, m3u);
    }

    public boolean songExists(List<M3UFile> songs, M3UFile m3u) {
        return (songs != null) && songs.contains(m3u);
    }

    public int songIndex(List<M3UFile> songs, String path, String name) {
        M3UFile m3u = new M3UFile(path, name);
        return songIndex(songs, m3u);
    }

    public int songIndex(List<M3UFile> songs, M3UFile m3u) {
        int index = -1;
        if (songs != null && m3u != null) {
            for (int i = 0; i < songs.size(); i++) {
                if (songs.get(i).equals(m3u)) {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    public void listSongs() {
        backgroundList();
    }

    public void dump() {
        backgroundDump();
    }

    private boolean isValidName(String name) {
        return (name != null) && !name.isEmpty();
    }

    private int inStorage(String name) {
        int storage = STORAGE_ILLEGAL;
        if (!isValidName(name)) {
            Log.w(TAG, "inStorage: illegal file " + name);
            return storage;
        }
        File dir = mContext.getFilesDir();
        if (dir != null) {
            if (name.startsWith(dir.getAbsolutePath())) {
                storage = STORAGE_INTERNAL;
            }
        }
        dir = mContext.getExternalFilesDir(null);
        if (storage == STORAGE_ILLEGAL && dir != null) {
            if (name.startsWith(dir.getAbsolutePath())) {
                storage = STORAGE_EXTERNAL_1;
            }
        }
        if (storage == STORAGE_ILLEGAL && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            File[] externals = mContext.getExternalFilesDirs(null);
            if (externals != null && externals.length > 1 && externals[1] != null) {
                dir = externals[1];
                if (name.startsWith(dir.getAbsolutePath())) {
                    storage = STORAGE_EXTERNAL_2;
                }
            }
        }
        return storage;
    }

    private void sendMessageToHandler(int what, Bundle data) {
        if (mM3UHandler != null) {
            Message msg = mM3UHandler.obtainMessage(what);
            if (data != null) {
                msg.setData(data);
            }
            mM3UHandler.removeMessages(what);
            mM3UHandler.sendMessage(msg);
        } else {
            Log.e(TAG, "sendMessageToHandler: no handler: " + what);
        }
    }

    private void backgroundLoad(String list, int storage) {
        Bundle data = new Bundle();
        data.putString(BUNDLE_LIST, list);
        data.putInt(BUNDLE_STORAGE, storage);
        sendMessageToHandler(M3U_LOAD, data);
    }

    private void backgroundUnload() {
        sendMessageToHandler(M3U_UNLOAD, null);
    }

    private void backgroundAddSong(M3UFile m3u) {
        if (m3u != null) {
            Bundle data = new Bundle();
            data.putBoolean(BUNDLE_EXTENDED, m3u.extended);
            data.putString(BUNDLE_PATH, m3u.path);
            data.putString(BUNDLE_NAME, m3u.name);
            data.putString(BUNDLE_AUTHOR, m3u.author);
            data.putString(BUNDLE_TITLE, m3u.title);
            data.putLong(BUNDLE_SONGLENGTH, m3u.songlength);
            sendMessageToHandler(M3U_ADD, data);
        }
    }

    private void backgroundRemoveSong(M3UFile m3u) {
        if (m3u != null) {
            Bundle data = new Bundle();
            data.putBoolean(BUNDLE_EXTENDED, m3u.extended);
            data.putString(BUNDLE_PATH, m3u.path);
            data.putString(BUNDLE_NAME, m3u.name);
            data.putString(BUNDLE_AUTHOR, m3u.author);
            data.putString(BUNDLE_TITLE, m3u.title);
            data.putLong(BUNDLE_SONGLENGTH, m3u.songlength);
            sendMessageToHandler(M3U_REMOVE, data);
        }
    }

    private void backgroundList() {
        sendMessageToHandler(M3U_LIST, null);
    }

    private void backgroundDump() {
        sendMessageToHandler(M3U_DUMP, null);
    }
}
