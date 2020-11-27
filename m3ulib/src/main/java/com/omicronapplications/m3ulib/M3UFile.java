package com.omicronapplications.m3ulib;

import android.text.format.DateUtils;

import java.io.File;

public class M3UFile {
    public M3UFile(String song) {
        if (song != null) {
            File f = new File(song);
            this.path = f.getParent();
            this.name = f.getName();
        } else {
            this.path = "";
            this.name = "";
        }
        this.author = "";
        this.title = "";
        this.songlength = 0;
        this.extended = false;
    }

    public M3UFile(String path, String name) {
        this.path = path != null ? path : "";
        this.name = name != null ? name : "";
        this.author = "";
        this.title = "";
        this.songlength = 0;
        this.extended = false;
    }

    public M3UFile(String song, String author, String title, long songlength) {
        if (song != null) {
            File f = new File(song);
            this.path = f.getParent();
            this.name = f.getName();
        } else {
            this.path = "";
            this.name = "";
        }
        this.author = author;
        this.title = title;
        this.songlength = songlength;
        this.extended = true;
    }

    public M3UFile(String path, String name, String author, String title, long songlength) {
        this.path = path != null ? path : "";
        this.name = name != null ? name : "";
        this.author = author;
        this.title = title;
        this.songlength = songlength;
        this.extended = true;
    }

    public String getFullPath() {
        String fullPath = "";
        if (path != null && name != null) {
            fullPath = new File(path, name).getAbsolutePath();
        } else if (path != null) {
            fullPath = path;
        } else if (name != null) {
            fullPath = name;
        }
        return fullPath;
    }

    public File getFile() {
        File file = null;
        if (path != null && name != null) {
            file = new File(path, name);
        } else if (path != null) {
            file = new File(path);
        } else if (name != null) {
            file = new File(name);
        }
        return file;
    }

    public long getLength() {
        File f = new File(path);
        return f.length();
    }

    public String getElapsedTimeInS() {
        int lengthInMs = (int) songlength;
        int lengthInS = lengthInMs / 1000;
        return DateUtils.formatElapsedTime(lengthInS);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        M3UFile other = (M3UFile) obj;
        return equals(path, other.path) &&
                equals(name, other.name);
    }

    private static boolean equals(Object obj1, Object obj2) {
        return (obj1 == obj2) || (obj1 != null && obj1.equals(obj2));
    }

    public String path;
    public String name;
    public String author;
    public String title;
    public long songlength;
    public boolean extended;
}
