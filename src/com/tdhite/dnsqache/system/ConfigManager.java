/*
 * This file is part of DnsQache. DnsQache is free software: you can
 * redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version. DnsQache is distributed in
 * the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU General Public License for more details. You should have received a
 * copy of the GNU General Public License along with DnsQache. If not, see
 * <http://www.gnu.org/licenses/>. Copyright (c) 2012-2013 Tom Hite
 */

package com.tdhite.dnsqache.system;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

import com.tdhite.dnsqache.QacheService;
import com.tdhite.dnsqache.R;

public class ConfigManager
{
	public static final String TAG = "DNSQACHE -> ConfigManager";

	private static ConfigManager mSingleton = null;

	// SharedPreferences all live here
	public static final String PREF_DONATE = "donatepref";
	public static final String PREF_NOTIFICATION = "notificationpref";

	/* dnsqache */
	public static final String PREF_START_ON_BOOT = "dnsqache.activateQacheOnBoot";
	public static final boolean PREF_DEFAULT_START_ON_BOOT = false;
	public static final String PREF_DNS_OLDPRIMARY = "dnsqache.oldPrimary";
	public static final String PREF_DNS_OLDSECONDARY = "dnsqache.oldSecondary";

	/* dnsmasq */
	public static final String PREF_DNSMASQ_PRIMARY = "dnsqache.primary";
	public static final String PREF_DNSMASQ_DEFAULT_PRIMARY_IP = "208.67.222.222";
	public static final String PREF_DNSMASQ_SECONDARY = "dnsqache.secondary";
	public static final String PREF_DNSMASQ_DEFAULT_SECONDARY_IP = "208.67.220.220";

	public static final String PREF_DNSMASQ_CACHESIZE = "dnsmasq.cache-size";
	public static final int PREF_DNSMASQ_DEFAULT_CACHE_SIZE = 200;
	public static final String PREF_DNSMASQ_PORT = "dnsmasq.port";
	public static final int PREF_DNSMASQ_DEFAULT_PORT = 5353;
	public static final String PREF_DNSMASQ_LOG_QUERIES = "dnsmasq.log-queries";
	public static final boolean PREF_DNSMASQ_DEFAULT_LOG_QUERIES = false;
	public static final String PREF_DNSMASQ_USER = "dnsmasq.user";
	public static final String PREF_DNSMASQ_NEG_TTL = "dnsmasq.neg-ttl";
	public static final String PREF_DNSMASQ_INTERFACE = "dnsmasq.interface";
	public static final String PREF_DNSMASQ_DHCP_INTERFACE = "dnsmasq.no-dhcp-interface";
	public static final String PREF_DNSMASQ_BIND_INTERFACES = "dnsmasq.bind-interfaces";
	public static final String PREF_DNSMASQ_RESOLV_FILE = "dnsmasq.resolv-file";
	public static final String PREF_DNSMASQ_PID_FILE = "dnsmasq.pid-file";
	public static final String PREF_DNSMASQ_LOG_FACILITY = "dnsmasq.log-facility";
	public static final String PREF_DNSMASQ_PROXY_DNSSEC = "dnsmasq.proxy-dnssec";
	public static final String PREF_DNSMASQ_NO_POLL = "dnsmasq.no-poll";

	public static final String PREF_UI_DNS_LOG_QUERIES = "dnsmasq.log-queries";
	public static final String PREF_DNS_PROVIDER = "dnsqache.provider";

	public static final String PREF_PROXY_TYPE = "proxy.type";
	public static final String PREF_PROXY_DEFAULT_TYPE = "polipo";
	public static final String PREF_PROXY_ACTIVATE = "proxy.activate";
	public static boolean PREF_PROXY_DEFAULT_ACTIVATE = false;

	public static final String DNSMASQ_BINARY = "dnsqache";

	/* tinyproxy */
	public static final String TINYPROXY_BINARY = "tinyproxy";

	public static final String PREF_TINYPROXY_PORT = "tinyproxy.Port";
	public static final String PREF_TINYPROXY_DEFAULT_PORT = "3128";
	public static final String PREF_TINYPROXY_TIMEOUT = "tinyproxy.TimeOut";
	public static final String PREF_TINYPROXY_DEFAULT_TIMEOUT = "600";
	public static final String PREF_TINYPROXY_DEFAULTERRORFILE = "tinyproxy.DefaultErrorFile";
	public static final String PREF_TINYPROXY_LOGFILE = "tinyproxy.LogFile";
	public static final String PREF_TINYPROXY_LOGLEVEL = "tinyproxy.LogLevel";
	public static final String PREF_TINYPROXY_PID_FILE = "tinyproxy.PidFile";
	public static final String PREF_TINYPROXY_MAXCLIENTS = "tinyproxy.MaxClients";
	public static final String PREF_TINYPROXY_MINSPARESERVERS = "tinyproxy.MinSpareServers";
	public static final String PREF_TINYPROXY_MAXSPARESERVERS = "tinyproxy.MaxSpareServers";
	public static final String PREF_TINYPROXY_STARTSERVERS = "tinyproxy.StartServers";
	public static final String PREF_TINYPROXY_MAXREQUESTSPERCHILD = "tinyproxy.MaxRequestsPerChild";
	public static final String PREF_TINYPROXY_ALLOW = "tinyproxy.Allow";
	public static final String PREF_TINYPROXY_VIAPROXYNAME = "tinyproxy.ViaProxyName";

	public static final String[] mProxyHtmlFiles = new String[]
		{
				"default.html", "stats.html", "debug.html"
		};

	/* polipo */
	public static final String POLIPO_BINARY = "polipo";
	public static final String PREF_POLIPO_ALLOWED_CIDRS = "polipo.allowed_cidrs";
	public static final String PREF_POLIPO_DEFAULT_ALLOWED_CIDRS = "172.20.21.0/24";

	/*
	 * Where this application stores its data.
	 */
	private String mDataFileDir = "./";

	/*
	 * Where the config file lives (relative to the dataFilePath).
	 */
	private static String mDnsmasqConfigFile = "dnsqache.conf";
	private static String mResolvFile = "resolv.dnsmasq";

	/*************************************************************************
	 * Singleton manager
	 ************************************************************************/
	private ConfigManager()
	{
		super();
	}

	public static ConfigManager getConfigManager()
	{
		if (ConfigManager.mSingleton == null)
		{
			ConfigManager.mSingleton = new ConfigManager();
		}
		return ConfigManager.mSingleton;
	}

	/*************************************************************************
	 * Configuration UI support
	 ************************************************************************/
	public static boolean validateIpAddress(Context ctx, String newIpAddress)
	{
		final Pattern IP_PATTERN = Pattern
				.compile("(25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9]?[0-9])");
		boolean b = IP_PATTERN.matcher(newIpAddress).matches();
		if (!b) {
			Log.w(TAG, "IP Address not valid: \"" + newIpAddress + "\"");
		}
		return b;
	}

	/*************************************************************************
	 * Private methods
	 ************************************************************************/
	private String quotedString(String inString)
	{
		StringBuilder s = new StringBuilder();
		s.append('"');
		s.append(inString);
		s.append('"');
		return s.toString();
	}

	private boolean assureDirExists(String path, boolean bCreate)
			throws Exception
	{
		File dir = new File(path);
		boolean exists = dir.exists();
		if (!exists)
		{
			if (bCreate)
			{
				exists = dir.mkdirs();
			}

			if (!exists)
			{
				throw new Exception("Failed to create directory " + dir
						+ " - cannot start!");
			}
		}

		return exists;
	}

	private boolean createDirectories()
	{
		boolean allExist = true;
		try
		{
			this.assureDirExists(getDataDir(), false);
			this.assureDirExists(getBinariesDir(), true);
			this.assureDirExists(getConfigDir(), true);

			// note: since mkdirs gets called, this creates intermediate dirs
			this.assureDirExists(getVarDir() + "/www", true);
		}
		catch (Exception e)
		{
			/* log info */
			Log.e(TAG, e.getMessage(), e);
			allExist = false;
		}

		return allExist;
	}

	private boolean binariesExist()
	{
		File qacher = new File(this.getBinaryFullPath(DNSMASQ_BINARY));
		File scripter = new File(
				this.getBinaryFullPath(ScriptManager.SCRIPT_BINARY));
		File tinyproxy = new File(this.getBinaryFullPath(this
				.getBinaryFullPath(TINYPROXY_BINARY)));
		File polipo = new File(this.getBinaryFullPath(this
				.getBinaryFullPath(POLIPO_BINARY)));
		return (qacher.exists() && scripter.exists() && tinyproxy.exists() && polipo
				.exists());
	}

	private String copyFile(Context context, String filename,
			String permission, int resource)
	{
		String result = this.copyFile(context, filename, resource);
		if (result != null)
		{
			return result;
		}

		if (CoreTask.chmod(filename, permission) != true)
		{
			result = "Can't change file-permission for '" + filename + "'!";
		}

		return result;
	}

	private String copyFile(Context context, String filename, int resource)
	{
		File outFile = new File(filename);
		Log.d(TAG, "Copying file '" + filename + "' ...");
		InputStream is = context.getResources().openRawResource(resource);
		byte buf[] = new byte[1024];
		int len;
		try
		{
			OutputStream out = new FileOutputStream(outFile);
			while ((len = is.read(buf)) > 0)
			{
				out.write(buf, 0, len);
			}
			out.close();
			is.close();
		}
		catch (IOException e)
		{
			return "Couldn't install file - " + filename + "!";
		}
		return null;
	}

	private String copyFileFromAssets(Context context, String srcPath,
			String dstPath)
	{
		String msg = null;
		InputStream src = null;
		File dstFile = null;
		OutputStream dst = null;
		int count = 0;

		try
		{
			src = context.getAssets().open(srcPath);
			dstFile = new File(dstPath);
			dst = new FileOutputStream(dstFile);

			byte data[] = new byte[1024];
			while ((count = src.read(data, 0, 1024)) != -1)
			{
				dst.write(data, 0, count);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			msg = "Couldn't install asset file - " + srcPath + "!";
		}
		finally
		{
			if (src != null)
			{
				try
				{
					src.close();
				}
				catch (IOException e)
				{
				}
			}
			if (dst != null)
			{
				try
				{
					dst.close();
				}
				catch (IOException e)
				{
				}

				if (android.os.Build.VERSION.SDK_INT < 9)
				{
					CoreTask.chmod(dstPath, "0644");
				}
				else
				{
					dstFile.setReadable(true, false);
					dstFile.setWritable(true, true);
				}
			}
		}

		return msg;
	}

	private boolean commitConfigFiles(Context context, SharedPreferences prefs)
	{
		boolean ret = false;
		StringBuilder[] lines = new StringBuilder[]
			{
					new StringBuilder(), new StringBuilder(),
					new StringBuilder()
			};

		int fileIndex = -1;
		String separator = null;
		String fileKey = null;

		Map<String, ?> map = prefs.getAll();
		for (String key : map.keySet())
		{
			Object value = map.get(key);

			if (key.startsWith("dnsmasq."))
			{
				fileIndex = 0;
				separator = "=";
				fileKey = key.substring("dnsmasq.".length());
			}
			else if (key.startsWith("polipo."))
			{
				fileIndex = 1;
				separator = "=";
				fileKey = key.substring("polipo.".length());
			}
			else if (key.startsWith("tinyproxy."))
			{
				fileIndex = 2;
				separator = " ";
				fileKey = key.substring("tinyproxy.".length());
			}

			if (fileIndex >= 0)
			{
				try
				{
					if (fileIndex == 0 && value instanceof Boolean)
					{
						if (((Boolean) value).booleanValue())
						{
							lines[fileIndex].append(fileKey);
						}
					}
					else
					{
						lines[fileIndex].append(fileKey);
						lines[fileIndex].append(separator);
						lines[fileIndex].append(value);
					}
					lines[fileIndex].append("\n");
				}
				catch (Exception e)
				{
					Log.e(TAG, "Error in commit(" + key + "," + fileKey + "):", e);
				}
			}

			fileIndex = -1;
		}

		ret = CoreTask.writeLinesToFile(getDnsqacheConfigFile(), lines[0].toString());
		ret = CoreTask.writeLinesToFile(getPolipoConfigFile(), lines[1].toString());
		ret = CoreTask.writeLinesToFile(getTinyProxyConfigFile(), lines[2].toString());

		return ret;
	}

	private void installFiles(Context context)
	{
		String message = null;

		// Assure the directories exist
		this.createDirectories();

		// scripter
		if (message == null)
		{
			message = this.copyFile(context,
					this.getBinaryFullPath(ScriptManager.SCRIPT_BINARY),
					"0755", R.raw.scripter);
		}

		// dnsmasq
		if (message == null)
		{
			message = this.copyFile(context,
					this.getBinaryFullPath(DNSMASQ_BINARY), "0755",
					R.raw.dnsmasq);
		}

		// tinyproxy
		if (message == null)
		{
			message = this.copyFile(context,
					this.getBinaryFullPath(TINYPROXY_BINARY), "0755",
					R.raw.tinyproxy);
		}

		// polipo
		if (message == null)
		{
			message = this
					.copyFile(context, this.getBinaryFullPath(POLIPO_BINARY),
							"0755", R.raw.polipo);
		}

		StringBuilder path = new StringBuilder();
		for (int idx = 0; idx < mProxyHtmlFiles.length && message == null; idx++)
		{
			path.setLength(0);
			path.append(this.getTinyProxyTemplateDir());
			path.append("/");
			path.append(mProxyHtmlFiles[idx]);
			message = this.copyFileFromAssets(context, mProxyHtmlFiles[idx],
					path.toString());
		}

		if (message == null)
		{
			message = context.getString(R.string.app_name) + " "
					+ context.getString(R.string.binaries_installed);
		}

		// Log any messages to the display -- non-invasively.
		// Toast.makeText(mApplication, message, Toast.LENGTH_LONG).show();
		Log.d(TAG, message);
	}

	private void initializePolipo(SharedPreferences prefs, Editor editor)
	{
		if (!prefs.contains("polipo.logFile"))
		{
			editor.putString("polipo.logFile", quotedString(this.getPolipoLogFile()));
		}

		if (!prefs.contains("polipo.pidFile"))
		{
			editor.putString("polipo.pidFile", quotedString(this.getPolipoPidFile()));
		}

		if (!prefs.contains("polipo.disableLocalInterface"))
		{
			editor.putBoolean("dpolipo.isableLocalInterface", false);
		}
		if (!prefs.contains("polipo.disableConfiguration"))
		{
			editor.putBoolean("polipo.disableConfiguration", false);
		}

		if (!prefs.contains("polipo.dnsUseGethostbyname"))
		{
			editor.putString("polipo.dnsUseGethostbyname", "yes");
		}
	}

	private void initializeTinyProxy(SharedPreferences prefs, Editor editor)
	{
		if (!prefs.contains(PREF_TINYPROXY_DEFAULTERRORFILE))
		{
			editor.putString(PREF_TINYPROXY_DEFAULTERRORFILE,
					quotedString(this.getTinyProxyErrorFile()));
		}

		if (!prefs.contains(PREF_TINYPROXY_DEFAULTERRORFILE))
		{
			editor.putString(PREF_TINYPROXY_DEFAULTERRORFILE,
					quotedString("tinyproxy.stats"));
		}

		if (!prefs.contains(PREF_TINYPROXY_LOGFILE))
		{
			editor.putString(PREF_TINYPROXY_LOGFILE,
					quotedString(this.getTinyProxyLogFile()));
		}

		if (!prefs.contains(PREF_TINYPROXY_PID_FILE))
		{
			editor.putString(PREF_TINYPROXY_PID_FILE,
					quotedString(this.getTinyProxyPidFile()));
		}
	}

	private void initializeDnsmasq(SharedPreferences prefs, Editor editor)
	{
		if (!prefs.contains(PREF_DNSMASQ_PRIMARY))
		{
			editor.putString(PREF_DNSMASQ_PRIMARY, PREF_DNSMASQ_DEFAULT_PRIMARY_IP);
			Log.i(TAG, "Updated primary DNS to default: " + PREF_DNSMASQ_DEFAULT_PRIMARY_IP);
		}

		if (!prefs.contains(PREF_DNSMASQ_SECONDARY))
		{
			editor.putString(PREF_DNSMASQ_SECONDARY, PREF_DNSMASQ_DEFAULT_SECONDARY_IP);
			Log.i(TAG, "Updated secondary DNS to default: " + PREF_DNSMASQ_DEFAULT_SECONDARY_IP);
		}

		if (!prefs.contains(PREF_DNSMASQ_INTERFACE))
		{
			editor.putString(PREF_DNSMASQ_INTERFACE, "lo");
		}

		if (!prefs.contains(PREF_DNSMASQ_DHCP_INTERFACE))
		{
			editor.putString(PREF_DNSMASQ_DHCP_INTERFACE, "lo");
		}

		if (!prefs.contains(PREF_DNSMASQ_USER))
		{
			editor.putString(PREF_DNSMASQ_USER, "root");
		}

		if (!prefs.contains(PREF_DNSMASQ_PID_FILE))
		{
			editor.putString(PREF_DNSMASQ_PID_FILE, this.getDnsmasqPidFile());
		}

		if (!prefs.contains(PREF_DNSMASQ_RESOLV_FILE))
		{
			editor.putString(PREF_DNSMASQ_RESOLV_FILE, this.getResolvFile());
		}

		if (!prefs.contains(PREF_DNSMASQ_BIND_INTERFACES))
		{
			editor.putBoolean(PREF_DNSMASQ_BIND_INTERFACES, true);
		}

		if (!prefs.contains(PREF_DNSMASQ_PROXY_DNSSEC))
		{
			editor.putBoolean(PREF_DNSMASQ_PROXY_DNSSEC, true);
		}

		if (!prefs.contains(PREF_DNSMASQ_NEG_TTL))
		{
			editor.putString(PREF_DNSMASQ_NEG_TTL, "3600");
		}

		if (!prefs.contains(PREF_DNSMASQ_NO_POLL))
		{
			editor.putBoolean(PREF_DNSMASQ_NO_POLL, true);
		}
	}

	private boolean writeResolvConf(Context context, SharedPreferences prefs)
	{
		String primary = prefs.getString(PREF_DNSMASQ_PRIMARY,
				ConfigManager.PREF_DNSMASQ_DEFAULT_PRIMARY_IP);
		String secondary = prefs.getString(PREF_DNSMASQ_SECONDARY,
				ConfigManager.PREF_DNSMASQ_DEFAULT_SECONDARY_IP);
		StringBuffer lines = new StringBuffer();
		lines.append("nameserver ");
		lines.append(primary);
		lines.append("\nnameserver ");
		lines.append(secondary);
		lines.append("\n");
		return CoreTask.writeLinesToFile(this.getResolvFile(), lines.toString());
	}

	private boolean commit(Context context, SharedPreferences prefs, Editor editor)
	{
		// set all values from the preferences file
		PreferenceManager.setDefaultValues(context, R.xml.preferences, false);

		// Add any other not destined for user input
		this.initializePolipo(prefs, editor);
		this.initializeTinyProxy(prefs, editor);
		this.initializeDnsmasq(prefs, editor);
		editor.apply();

		if (!binariesExist())
		{
			if (CoreTask.hasRootPermission())
			{
				installFiles(context);
			}
		}

		return this.commitConfigFiles(context, prefs) && this.writeResolvConf(context, prefs);
	}

	/*************************************************************************
	 * Public methods
	 ************************************************************************/
	public static int getResourceId(Context ctx, String resourceName,
			String resourceType)
	{
		try
		{
			return ctx.getResources().getIdentifier(resourceName, resourceType,
					ctx.getPackageName());
		}
		catch (Exception e)
		{
			Log.e(TAG, "getResourceId", e);
			return -1;
		}
	}

	public final String getDataDir()
	{
		return this.mDataFileDir;
	}

	public final String getBinariesDir()
	{
		return this.mDataFileDir + "bin/";
	}

	public final String getConfigDir()
	{
		return this.mDataFileDir + "conf/";
	}

	public String getVarDir()
	{
		return this.mDataFileDir + "var/";
	}

	public String getDnsqacheConfigFile()
	{
		return this.getConfigDir() + mDnsmasqConfigFile;
	}

	public String getResolvFile()
	{
		return this.getConfigDir() + mResolvFile;
	}

	public void setDataFileDir(Context context, String path)
	{
		if (!path.endsWith("/"))
		{
			this.mDataFileDir = path.concat("/");
		}
		else
		{
			this.mDataFileDir = path;
		}
	}

	public String getBinaryFullPath(String binary)
	{
		return this.getBinariesDir() + binary;
	}

	public String getScriptFullPath(String script)
	{
		return this.getConfigDir() + script;
	}

	public String getLogFile()
	{
		return this.getVarDir() + DNSMASQ_BINARY + ".log";
	}

	public String getDnsmasqPidFile()
	{
		return this.getVarDir() + DNSMASQ_BINARY + ".pid";
	}

	public String getTinyProxyPidFile()
	{
		return this.getVarDir() + TINYPROXY_BINARY + ".pid";
	}

	public String getTinyProxyLogFile()
	{
		return this.getVarDir() + TINYPROXY_BINARY + ".log";
	}

	public String getTinyProxyConfigFile()
	{
		return this.getConfigDir() + TINYPROXY_BINARY + ".conf";
	}

	private String getTinyProxyTemplateDir()
	{
		return this.getVarDir() + "/www";
	}

	public String getTinyProxyErrorFile()
	{
		return this.getVarDir() + getTinyProxyTemplateDir() + "/default.html";
	}

	public String getPolipoConfigFile()
	{
		return this.getConfigDir() + POLIPO_BINARY + ".conf";
	}

	public String getPolipoPidFile()
	{
		return this.getVarDir() + POLIPO_BINARY + ".pid";
	}

	public String getPolipoLogFile()
	{
		return this.getVarDir() + POLIPO_BINARY + ".log";
	}

	public String[] getDNSServers(Context ctx)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		String dnsServers[] = new String[]
			{
				prefs.getString(PREF_DNSMASQ_PRIMARY, PREF_DNSMASQ_DEFAULT_PRIMARY_IP),
				prefs.getString(PREF_DNSMASQ_SECONDARY, PREF_DNSMASQ_DEFAULT_SECONDARY_IP)
			};
		return dnsServers;
	}

	public void updateDNSConfiguration(Context ctx, SharedPreferences prefs, String primaryDns,
			String secondaryDns, int cacheSize)
	{
		long startStamp = System.currentTimeMillis();
		Editor editor = prefs.edit();

		// the number seven is just because 1.1.1.1 is seven chars, eh?
		if (primaryDns == null) {
			primaryDns = prefs.getString(PREF_DNSMASQ_PRIMARY, PREF_DNSMASQ_DEFAULT_PRIMARY_IP);
		}
		if (!ConfigManager.validateIpAddress(ctx, primaryDns)) {
			primaryDns = ConfigManager.PREF_DNSMASQ_DEFAULT_PRIMARY_IP;
		}
		editor.putString(PREF_DNSMASQ_PRIMARY, primaryDns);
		Log.i(TAG, "Updated primary DNS to: " + primaryDns);

		if (secondaryDns == null) {
			secondaryDns = prefs.getString(PREF_DNSMASQ_SECONDARY, PREF_DNSMASQ_DEFAULT_SECONDARY_IP);
		}
		if (!ConfigManager.validateIpAddress(ctx, secondaryDns))
		{
			secondaryDns = ConfigManager.PREF_DNSMASQ_DEFAULT_SECONDARY_IP;
		}
		editor.putString(PREF_DNSMASQ_SECONDARY, secondaryDns);
		Log.i(TAG, "Updated secondary DNS to: " + secondaryDns);

		String cs = null;
		if (cacheSize < 0)
		{
			cs = prefs.getString(PREF_DNSMASQ_CACHESIZE, "" + PREF_DNSMASQ_DEFAULT_CACHE_SIZE);
		} else {
			cs = "" + cacheSize;
		}
		editor.putString(PREF_DNSMASQ_CACHESIZE, cs);
		Log.i(TAG, "Updated DNS cache size to: " +  cs);

		editor.apply();

		// writing the configs
		if (this.commit(ctx, prefs, editor))
		{
			Log.d(TAG,
					"Creation of configuration-files took ==> "
							+ (System.currentTimeMillis() - startStamp)
							+ " milliseconds.");
			QacheService svc = QacheService.getSingleton();
			if (svc != null)
			{
				svc.setDns();
			}
		}
		else
		{
			Log.e(TAG, "Unable to update configuration preferences!");
		}
	}
}
