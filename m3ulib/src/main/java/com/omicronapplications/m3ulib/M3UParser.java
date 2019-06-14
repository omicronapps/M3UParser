package com.omicronapplications.m3ulib;

import android.content.Context;
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
import java.util.ArrayList;

public class M3UParser {
    private static final String TAG = "M3UParser";
    private final Context mContext;
    private File mList;
    private ArrayList<String> mSongs;
    private boolean mExternal;

    public M3UParser(Context context) {
        mContext = context;
        mList = null;
        mSongs = null;
        mExternal = false;
    }

    public void load(String name, boolean external) {
        File dir;
        if ((mContext != null) && (name != null)) {
            if (external) {
                dir = mContext.getExternalFilesDir(null);
            } else {
                dir = mContext.getFilesDir();
            }
            mExternal = external;
            mList = new File(dir, name);
            if (!mList.exists()) {
                try {
                    mList.createNewFile();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            if (!readSongs()) {
                Log.e(TAG, "load: read failed");
            }
        }
    }

    public void load(String name) {
        File dir = mContext.getExternalFilesDir(null);
        mExternal = (name != null) && (dir != null) && name.contains(dir.getAbsolutePath());
        mList = new File(name);
        if (!mList.exists()) {
            try {
                mList.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        if (!readSongs()) {
            Log.e(TAG, "load: read failed");
        }
    }

    public void load(File list) {
        mList = list;
        File dir = mContext.getExternalFilesDir(null);
        mExternal = (list != null) && (dir != null) && list.getAbsolutePath().contains(dir.getAbsolutePath());
        if ((mList != null) && !mList.exists()) {
            try {
                mList.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        if (!readSongs()) {
            Log.e(TAG, "load: read failed");
        }
    }

    public boolean isLoaded() {
        return (mList != null) && mList.exists();
    }

    public void unload() {
        if (!writeSongs()) {
            Log.w(TAG, "unload: write failed");
        }
        mList = null;
        mSongs = null;
    }

    public boolean addSong(String song) {
        boolean songAdded = false;
        if ((mSongs != null) && !mSongs.contains(song)) {
            mSongs.add(song);
            if (!writeSongs()) {
                Log.e(TAG, "addSong: write failed");
            }
            songAdded = true;
        }
        return songAdded;
    }

    public boolean addFile(File file) {
        String song = getSong(file);
        return addSong(song);
    }

    public boolean removeSong(String song) {
        boolean songRemoved = false;
        if ((mSongs != null) && mSongs.contains(song)) {
            mSongs.remove(song);
            if (!writeSongs()) {
                Log.e(TAG, "removeSong: write failed");
            }
            songRemoved = true;
        }
        return songRemoved;
    }

    public boolean removeFile(File file) {
        String song = getSong(file);
        return removeSong(song);
    }

    public boolean songExists(String song) {
        return (mSongs != null) && mSongs.contains(song);
    }

    public boolean fileExists(File file) {
        boolean isFound = false;
        if (file != null) {
            String song = file.getAbsolutePath();
            isFound = songExists(song);
        }
        return isFound;
    }

    public int fileIndex(File file) {
        int index = -1;
        if (file != null) {
            String song = getSong(file);
            for (int i = 0; i < mSongs.size(); i++) {
                if (mSongs.get(i).equals(song)) {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    public ArrayList<String> getSongs() {
        return mSongs;
    }

    public String[] listSongs() {
        ArrayList<String> list = mSongs;
        String[] songs = new String[mSongs.size()];
        list.toArray(songs);
        return songs;
    }

    public ArrayList<File> getFiles() {
        ArrayList<File> files = null;
        if (mSongs != null) {
            files = new ArrayList<>();
            for (String song : mSongs) {
                File file = getFile(song);
                files.add(file);
            }
        }
        return files;
    }

    public File[] listFiles() {
        ArrayList<File> list = getFiles();
        File[] files = null;
        if (list != null) {
            files = new File[list.size()];
            list.toArray(files);
        }
        return files;
    }

    public String dump() {
        String str = null;
        if (mSongs != null) {
            StringBuilder sb = new StringBuilder();
            for (String song : mSongs) {
                sb.append(song);
                sb.append(System.getProperty("line.separator"));
            }
            str = sb.toString();
        }
        return str;
    }

    private boolean readSongs() {
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
                while (str != null) {
                    mSongs.add(str);
                    str = br.readLine();
                }
                readSuccessful = true;
                br.close();
                sr.close();
                fs.close();
            } catch (NullPointerException e) {
                Log.e(TAG, e.getMessage());
            } catch (FileNotFoundException e) {
                Log.e(TAG, e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return readSuccessful;
    }

    private boolean writeSongs() {
        boolean writeSuccessful = false;
        if (mList == null) {
            Log.e(TAG, "writeSongs: no playlist loaded");
        } else if (mSongs == null ){
            Log.e(TAG, "writeSongs: no song array");
        } else {
            try {
                File dir;
                if (mExternal) {
                    dir = mContext.getExternalCacheDir();
                } else {
                    dir = mContext.getCacheDir();
                }
                File tempList = File.createTempFile("m3u", "tmp", dir);
                FileOutputStream os = new FileOutputStream(tempList);
                OutputStreamWriter sw = new OutputStreamWriter(os);
                BufferedWriter bw = new BufferedWriter(sw);
                for (String song : mSongs) {
                    bw.write(song);
                    bw.newLine();
                }
                bw.close();
                sw.close();
                os.close();
                if (!mList.delete()) {
                    Log.w(TAG, "Delete failed: " + mList.getAbsolutePath());
                }
                if (!tempList.renameTo(mList)) {
                    Log.e(TAG, "Rename failed: " + tempList.getAbsolutePath() + " -> " + mList.getAbsolutePath());
                } else {
                    writeSuccessful = true;
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return writeSuccessful;
    }

    private String getSong(File file) {
        String song = null;
        if (file != null) {
            song = file.getAbsolutePath();
        }
        return song;
    }

    private File getFile(String song) {
        return new File(song);
    }
}
