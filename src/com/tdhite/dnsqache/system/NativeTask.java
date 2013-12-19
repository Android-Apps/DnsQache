/*
This file is part of DnsQache.

DnsQache is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

DnsQache is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with DnsQache.  If not, see <http://www.gnu.org/licenses/>.

Copyright (c) 2012-2013 Tom Hite

*/

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
