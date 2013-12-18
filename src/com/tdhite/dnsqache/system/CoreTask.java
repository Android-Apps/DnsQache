/*
This file is part of DnsQache.

DnsQache is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Foobar is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Foobar.  If not, see <http://www.gnu.org/licenses/>.

Copyright (c) 2012-2013 Tom Hite
Portions also Copyright (c) 2009 by Harald Mueller and Sofia Lemons.

*/

package com.tdhite.dnsqache.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Hashtable;

import android.util.Log;

public class CoreTask
{
	public static final String TAG = "DNSQACHE -> CoreTask";

	private static Hashtable<String, String> runningProcesses = new Hashtable<String, String>();

	/*************************************************************************
	 * Static methods
	 ************************************************************************/
	public static boolean chmod(String file, String mode)
	{
		if (NativeTask.runCommand("chmod " + mode + " " + file) == 0)
		{
			return true;
		}
		return false;
	}

	public static ArrayList<String> readLinesFromFile(String filename)
	{
		String line = null;
		BufferedReader br = null;
		InputStream ins = null;
		ArrayList<String> lines = new ArrayList<String>();
		File file = new File(filename);

		if (file.canRead() == false)
			return lines;

		try
		{
			ins = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(ins), 8192);
			while ((line = br.readLine()) != null)
			{
				lines.add(line.trim());
			}
		}
		catch (Exception e)
		{
			Log.d(TAG,
					"Unexpected error - Here is what I know: " + e.getMessage());
		}
		finally
		{
			try
			{
				ins.close();
				br.close();
			}
			catch (Exception e)
			{
				Log.d(TAG, "Unexpected error closing " + filename
						+ "- Here is what I know: " + e.getMessage());
			}
		}
		return lines;
	}

	public static boolean writeLinesToFile(String filename, String lines)
	{
		OutputStream out = null;
		boolean returnStatus = false;
		Log.d(TAG, "Writing " + lines.length() + " bytes to file: " + filename);
		try
		{
			out = new FileOutputStream(filename);
			out.write(lines.getBytes());
			out.flush();
		}
		catch (Exception e)
		{
			Log.d(TAG,
					"Unexpected error - Here is what I know: " + e.getMessage());
		}
		finally
		{
			try
			{
				if (out != null)
					out.close();
				returnStatus = true;
			}
			catch (IOException e)
			{
				returnStatus = false;
			}
		}
		return returnStatus;
	}

	public static String getKernelVersion()
	{
		ArrayList<String> lines = readLinesFromFile("/proc/version");
		String version = lines.get(0).split(" ")[2];
		Log.d(TAG, "Kernel version: " + version);
		return version;
	}

	public static boolean isBusyboxInstalled()
	{
		if ((new File("/system/bin/busybox")).exists() == false)
		{
			if ((new File("/system/xbin/busybox")).exists() == false)
			{
				return false;
			}
		}

		return true;
	}

	public static boolean hasRootPermission()
	{
		boolean rooted = true;
		try
		{
			File su = new File("/system/bin/su");
			if (su.exists() == false)
			{
				su = new File("/system/xbin/su");
				if (su.exists() == false)
				{
					rooted = false;
				}
			}
		}
		catch (Exception e)
		{
			Log.d(TAG,
					"Can't obtain root - Here is what I know: "
							+ e.getMessage());
			rooted = false;
		}
		return rooted;
	}

	public static boolean runRootCommand(String command)
	{
		Log.d(TAG, "Root-Command ==> su -c \"" + command + "\"");
		int returncode = NativeTask.runCommand("su -c \"" + command + "\"");
		if (returncode == 0)
		{
			return true;
		}
		Log.d(TAG, "Root-Command error, return code: " + returncode);
		return false;
	}

	public static boolean runStandardCommand(String command)
	{
		Log.d(TAG, "Standard-Command ==> \"" + command + "\"");
		int returncode = NativeTask.runCommand(command);
		if (returncode == 0)
		{
			return true;
		}
		Log.d(TAG, "Standard-Command error, return code: " + returncode);
		return false;
	}

	public static String getProp(String property)
	{
		return NativeTask.getProp(property);
	}

	/*************************************************************************
	 * Public methods
	 ************************************************************************/

	public static long getModifiedDate(String filename)
	{
		File file = new File(filename);
		if (file.exists() == false)
		{
			return -1;
		}
		return file.lastModified();
	}

	static public boolean isProcessRunning(String processName) throws Exception
	{
		boolean processIsRunning = false;
		Hashtable<String, String> tmpRunningProcesses = new Hashtable<String, String>();
		File procDir = new File("/proc");
		FilenameFilter filter = new FilenameFilter()
		{
			public boolean accept(File dir, String name)
			{
				try
				{
					Integer.parseInt(name);
				}
				catch (NumberFormatException ex)
				{
					return false;
				}
				return true;
			}
		};

		synchronized(CoreTask.runningProcesses)
		{
			File[] processes = procDir.listFiles(filter);
			for (File process : processes)
			{
				String cmdLine = "";
	
				// Search for a known known process
				if (CoreTask.runningProcesses.containsKey(process.getAbsoluteFile()
						.toString()))
				{
					cmdLine = CoreTask.runningProcesses.get(process.getAbsoluteFile()
							.toString());
				}
				else
				{
					ArrayList<String> cmdlineContent = CoreTask
							.readLinesFromFile(process.getAbsoluteFile()
									+ "/cmdline");
					if (cmdlineContent != null && cmdlineContent.size() > 0)
					{
						cmdLine = cmdlineContent.get(0);
					}
				}
				// Adding to tmp-Hashtable
				tmpRunningProcesses.put(process.getAbsoluteFile().toString(),
						cmdLine);
	
				// Checking if processName matches
				if (cmdLine.startsWith(processName))
				{
					processIsRunning = true;
				}
			}
	
			// Overwriting runningProcesses
			CoreTask.runningProcesses = tmpRunningProcesses;
		}

		return processIsRunning;
	}

	public static String[] getCurrentDns(
			String defaultPrimary, String defaultSecondary)
	{
		// Get current DNS name servers
		String dns[] = {null, null};

		dns[0] = CoreTask.getProp("net.dns1");
		dns[1] = CoreTask.getProp("net.dns2");

		if (dns[0] == null || dns[0].length() <= 0
				|| dns[0].equals("undefined"))
		{
			dns[0] = defaultPrimary;
		}

		if (dns[1] == null || dns[1].length() <= 0
				|| dns[1].equals("undefined"))
		{
			dns[1] = defaultSecondary;
		}

		return dns;
	}
}
