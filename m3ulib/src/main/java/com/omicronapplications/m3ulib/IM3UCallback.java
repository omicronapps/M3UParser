package com.omicronapplications.m3ulib;

import java.io.File;
import java.util.List;

public interface IM3UCallback {
    void onM3ULoaded(boolean isLoaded);
    void onM3UList(List<M3UFile> songs);
    void onM3UWrite(File list);
    void onM3UDump(String str);
}
