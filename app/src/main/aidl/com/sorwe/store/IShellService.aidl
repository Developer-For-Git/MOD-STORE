package com.sorwe.store;

import android.os.ParcelFileDescriptor;

interface IShellService {
    int runCommand(String cmd);
    int installApk(in ParcelFileDescriptor pfd);
    void destroy();
}
