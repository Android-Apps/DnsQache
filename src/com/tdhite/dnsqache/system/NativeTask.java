package com.tdhite.dnsqache.system;

import android.util.Log;

public class NativeTask {
    
	public static final String TAG = "DNSQACHE -> NativeTask";

	static {
        try {
            Log.i(TAG, "Loading libdqnativetask.so ...");
            System.loadLibrary("dqnativetask");
            Log.i(TAG, "... OK -- loaded.");
        }
        catch (UnsatisfiedLinkError ule) {
            Log.e(TAG, "... FAILED due to unsatisfied link error.");
        }
    }

	public static native String getProp(String name);
    public static native int runCommand(String command);
}
