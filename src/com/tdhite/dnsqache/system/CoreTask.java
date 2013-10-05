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

	private Hashtable<String, String> runningProcesses = new Hashtable<String, String>();

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

	public boolean isProcessRunning(String processName) throws Exception
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

		File[] processes = procDir.listFiles(filter);
		for (File process : processes)
		{
			String cmdLine = "";

			// Chef for a known know process
			if (this.runningProcesses.containsKey(process.getAbsoluteFile()
					.toString()))
			{
				cmdLine = this.runningProcesses.get(process.getAbsoluteFile()
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
			if (cmdLine.contains(processName))
			{
				processIsRunning = true;
			}
		}
		// Overwriting runningProcesses
		this.runningProcesses = tmpRunningProcesses;

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
