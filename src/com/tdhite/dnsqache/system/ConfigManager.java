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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.tdhite.dnsqache.R;

public class ConfigManager
{
	public static final String TAG = "DNSQACHE -> ConfigManager";

	// the disparate configuration maps
	private HashMap<String, HashMap<String, String>> mConfigMaps = new HashMap<String, HashMap<String, String>>();

	// SharedPreferences all live here
	public static final String PREF_DONATE = "donatepref";
	public static final String PREF_NOTIFICATION = "notificationpref";

	// Configuration maps in which to place final configuration information
	private static final String MAP_TINYPROXY = "proxy";
	private static final String MAP_DNSMASQ = "dnsmasq";
	private static final String MAP_POLIPO = "polipo";
	
	// Configuration maps for use in specifying optional values that may change outside this class.
	public static final String MAP_DNSMASQ_OPTS = "dnsmasq:opts";
	public static final String MAP_POLIPO_OPTS = "polipo:opts";

	public static final int DNS_DEFAULT_SPINNER_POSITION = 3;
	public static final int PROXY_DEFAULT_SPINNER_POSITION = 0;

	/* dnsmasq */
	public static final String PREF_DNSMASQ_PRIMARY = "dns.primary";
	public static final String PREF_DNSMASQ_SECONDARY = "dns.secondary";
	public static final String PREF_DNSMASQ_CACHESIZE = "cache-size";
	public static final String PREF_DNSMASQ_DEFAULT_PRIMARY_IP = "208.67.222.222";
	public static final String PREF_DNSMASQ_DEFAULT_SECONDARY_IP = "208.67.220.220";
	public static final String PREF_DNSMASQ_DEFAULT_PORT = "5353";
	public static final String PREF_DNSMASQ_DEFAULT_CACHE_SIZE = "200";
	public static final String PREF_DNSMASQ_NEG_TTL = "neg-ttl";

	public static final String PREF_DNSMASQ_INTERFACE = "interface";
	public static final String PREF_DNSMASQ_DHCP_INTERFACE = "no-dhcp-interface";
	public static final String PREF_DNSMASQ_BIND_INTERFACES = "bind-interfaces";
	public static final String PREF_DNSMASQ_PORT = "port";
	public static final String PREF_DNSMASQ_USER = "user";
	public static final String PREF_DNSMASQ_LOG_QUERIES = "log-queries";
	public static final String PREF_DNSMASQ_RESOLV_FILE = "resolv-file";
	public static final String PREF_DNSMASQ_PID_FILE = "pid-file";
	public static final String PREF_DNSMASQ_LOG_FACILITY = "log-facility";
	public static final String PREF_DNSMASQ_TINYPROXY_DNSSEC = "proxy-dnssec";
	public static final String PREF_DNSMASQ_NO_POLL = "no-poll";

	public static final String PREF_START_ON_BOOT = "activateQacheOnBoot";
	public static final String PREF_KEEP_REQUEST_COUNT = "keepRequestCount";
	public static final String PREF_DNS_OLDPRIMARY = "dns.oldPrimary";
	public static final String PREF_DNS_OLDSECONDARY = "dns.oldSecondary";

	public static final String PREF_UI_DNS_PROVIDER_POSITION = "dns.providerPosition";
	public static final String PREF_UI_DNS_LOG_QUERIES = "dnsmasq.logQueries";
	public static final String PREF_DNS_PROVIDER = "dns.provider";

	public static final String PREF_UI_PROXY_SPINNER_POSITION = "proxy.position";

	public static final String DNSMASQ_BINARY = "dnsqache";

	/* tinyproxy */
	public static final String TINYPROXY_BINARY = "tinyproxy";

	public static final String PREF_TINYPROXY_PORT = "Port";
	public static final String PREF_TINYPROXY_DEFAULT_PORT = "3128";
	public static final String PREF_TINYPROXY_TIMEOUT = "TimeOut";
	public static final String PREF_TINYPROXY_DEFAULT_TIMEOUT = "600";
	public static final String PREF_TINYPROXY_DEFAULTERRORFILE = "DefaultErrorFile";
	public static final String PREF_TINYPROXY_STATHOST = "StatHost";
	public static final String PREF_TINYPROXY_LOGFILE = "LogFile";
	public static final String PREF_TINYPROXY_LOGLEVEL = "LogLevel";
	public static final String PREF_TINYPROXY_PID_FILE = "PidFile";
	public static final String PREF_TINYPROXY_MAXCLIENTS = "MaxClients";
	public static final String PREF_TINYPROXY_MINSPARESERVERS = "MinSpareServers";
	public static final String PREF_TINYPROXY_MAXSPARESERVERS = "MaxSpareServers";
	public static final String PREF_TINYPROXY_STARTSERVERS = "StartServers";
	public static final String PREF_TINYPROXY_MAXREQUESTSPERCHILD = "MaxRequestsPerChild";
	public static final String PREF_TINYPROXY_ALLOW = "Allow";
	public static final String PREF_TINYPROXY_VIAPROXYNAME = "ViaProxyName";

	public static final String[] mProxyHtmlFiles = new String[]
		{
				"default.html", "stats.html", "debug.html"
		};

	/* polipo */
	public static final String POLIPO_BINARY = "polipo";
	public static final String PREF_POLIPO_ALLOWED_CIDRS = "allowed_cidrs";
	public static final String PREF_POLIPO_DEFAULT_ALLOWED_CIDRS = "172.20.21.0/24";

	/*
	 * Where this application stores its data.
	 */
	private String mDataFileDir = "./";
	private boolean mInitialized = false;

	/*
	 * Where the config file lives (relative to the dataFilePath).
	 */
	private static String mDnsmasqConfigFile = "dnsqache.conf";
	private static String mResolvFile = "resolv.dnsmasq";

	/*************************************************************************
	 * Private methods
	 ************************************************************************/
	private HashMap<String, String> getMap(String mapName)
	{
		HashMap<String, String> map = this.mConfigMaps.get(mapName);
		if (map == null)
		{
			map = new HashMap<String, String>();
			this.mConfigMaps.put(mapName, map);
		}
		return map;
	}

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

	private boolean commitMap(Context context, String mapName, String confFile,
			String separator)
	{
		HashMap<String, String> map = this.getMap(mapName);
		boolean ret = false;
		String lines = new String();

		for (String key : map.keySet())
		{
			String value = map.get(key);
			if (value == null)
			{
				lines += key;
			}
			else
			{
				lines += key + separator + map.get(key);
			}
			lines += "\n";
		}

		ret = CoreTask.writeLinesToFile(confFile, lines);

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

	private boolean initializePolipo(Context context)
	{
		boolean bInitialized = mInitialized;

		// in all cases, turn on or off specific UI options
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		HashMap<String, String> map = this.getMap(MAP_POLIPO);
		HashMap<String, String> optsMap = this.getMap(MAP_POLIPO_OPTS);
		String optCIDRs = optsMap.get(PREF_POLIPO_ALLOWED_CIDRS);
		if (optCIDRs == null)
		{
			optCIDRs = sharedPrefs.getString(PREF_POLIPO_ALLOWED_CIDRS,
					PREF_POLIPO_DEFAULT_ALLOWED_CIDRS);
		}
		map.put("allowedClients", "127.0.0.1, " + optCIDRs);

		// add the rest if not already initialized
		if (!bInitialized)
		{
			map.put("allowUnalignedRangeRequests", "false");
			map.put("allowedPorts", "1-65535");
			map.put("daemonise", "true");
			map.put("disableVia", "true");
			map.put("displayName", "Polipo");
			map.put("laxHttpParser", "true");
			map.put("logSyslog", "false");
			map.put("logFile", quotedString(this.getPolipoLogFile()));
			map.put("pidFile", quotedString(this.getPolipoPidFile()));
			map.put("proxyAddress", "0.0.0.0");
			map.put("proxyPort", "3128");
			map.put("proxyName", "localhost");
			map.put("cacheIsShared", "false");
			map.put("disableLocalInterface", "false");
			map.put("disableConfiguration", "false");
			map.put("dnsUseGethostbyname", "yes");
			map.put("maxConnectionAge", "5m");
			map.put("maxConnectionRequests", "120");
			map.put("serverMaxSlots", "8");
			map.put("serverSlots", "2");
			bInitialized = true;
		}

		return bInitialized;
	}

	private boolean initializeTinyProxy(Context context)
	{
		boolean bInitialized = mInitialized;

		if (!bInitialized)
		{
			HashMap<String, String> proxyMap = this.getMap(MAP_TINYPROXY);
			proxyMap.put(PREF_TINYPROXY_PORT, PREF_TINYPROXY_DEFAULT_PORT);
			proxyMap.put(PREF_TINYPROXY_TIMEOUT, PREF_TINYPROXY_DEFAULT_TIMEOUT);
			proxyMap.put(PREF_TINYPROXY_DEFAULTERRORFILE,
					quotedString(this.getTinyProxyErrorFile()));
			proxyMap.put(PREF_TINYPROXY_STATHOST,
					quotedString("tinyproxy.stats"));
			proxyMap.put(PREF_TINYPROXY_LOGFILE,
					quotedString(this.getTinyProxyLogFile()));
			proxyMap.put(PREF_TINYPROXY_LOGLEVEL, "Connect");
			proxyMap.put(PREF_TINYPROXY_PID_FILE,
					quotedString(this.getTinyProxyPidFile()));
			proxyMap.put(PREF_TINYPROXY_MAXCLIENTS, "100");
			proxyMap.put(PREF_TINYPROXY_MINSPARESERVERS, "5");
			proxyMap.put(PREF_TINYPROXY_MAXSPARESERVERS, "20");
			proxyMap.put(PREF_TINYPROXY_STARTSERVERS, "10");
			proxyMap.put(PREF_TINYPROXY_MAXREQUESTSPERCHILD, "0");
			// proxyMap.put(PREF_TINYPROXY_ALLOW + "1", "127.0.0.1");
			// proxyMap.put(PREF_TINYPROXY_ALLOW + "2", "172.16.0.12/12");
			// proxyMap.put(PREF_TINYPROXY_ALLOW + "3", "192.168.0.0/16");
			proxyMap.put(PREF_TINYPROXY_VIAPROXYNAME, quotedString("DnsQache"));
			bInitialized = true;
		}

		return bInitialized;
	}

	private boolean initializeDnsmasq(Context context)
	{
		HashMap<String, String> dnsmasqMap = this.getMap(MAP_DNSMASQ);
		boolean bInitialized = mInitialized;

		// in all cases, turn on or off specific UI options
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		boolean bLogQueries = sharedPrefs.getBoolean(PREF_UI_DNS_LOG_QUERIES,
				false);
		if (bLogQueries)
		{
			dnsmasqMap.put(PREF_DNSMASQ_LOG_QUERIES, null);
		}
		else
		{
			dnsmasqMap.remove(PREF_DNSMASQ_LOG_QUERIES);
		}

		// dnsmasqMap.put(PREF_DNSMASQ_LOG_FACILITY,
		// this.getLogFile());

		HashMap<String, String> optionsMap = this.getMap(MAP_DNSMASQ_OPTS);
		String maxCacheSize = optionsMap.get(PREF_DNSMASQ_CACHESIZE);
		if (maxCacheSize == null)
		{
			maxCacheSize = sharedPrefs.getString(PREF_DNSMASQ_CACHESIZE,
							PREF_DNSMASQ_DEFAULT_CACHE_SIZE);
			optionsMap.put(PREF_DNSMASQ_CACHESIZE, maxCacheSize);
		}
		dnsmasqMap.put(PREF_DNSMASQ_CACHESIZE, maxCacheSize);

		// Add the rest
		if (!bInitialized)
		{
			/* get the dnsmasq prefs table */
			dnsmasqMap.put(PREF_DNSMASQ_INTERFACE, "lo");
			dnsmasqMap.put(PREF_DNSMASQ_DHCP_INTERFACE, "lo");
			dnsmasqMap.put(PREF_DNSMASQ_USER, "root");
			dnsmasqMap.put(PREF_DNSMASQ_PORT, PREF_DNSMASQ_DEFAULT_PORT);
			dnsmasqMap.put(PREF_DNSMASQ_PID_FILE, this.getDnsmasqPidFile());
			dnsmasqMap.put(PREF_DNSMASQ_RESOLV_FILE, this.getResolvFile());
			dnsmasqMap.put(PREF_DNSMASQ_BIND_INTERFACES, null);
			dnsmasqMap.put(PREF_DNSMASQ_TINYPROXY_DNSSEC, null);
			dnsmasqMap.put(PREF_DNSMASQ_NEG_TTL, "3600");
			dnsmasqMap.put(PREF_DNSMASQ_NO_POLL, null);

			// Check if binaries need updates
			if (!binariesExist())
			{
				if (CoreTask.hasRootPermission())
				{
					installFiles(context);
				}
			}

			bInitialized = true;
		}

		return bInitialized;
	}

	private boolean writeResolvConf(String primary, String secondary)
	{
		String lines = "nameserver " + primary + "\nnameserver " + secondary
				+ "\n";
		return CoreTask.writeLinesToFile(this.getResolvFile(), lines);
	}

	private void readConfigFile(Context context, String mapName,
			String filename, String separator)
	{
		HashMap<String, String> map = this.getMap(mapName);

		// clear out any old thoughts of configuration
		map.clear();

		// read the configuration
		for (String line : CoreTask.readLinesFromFile(filename))
		{
			if (line.startsWith("#"))
				continue;

			if (line.contains(separator))
			{
				String[] data = line.split(separator);
				if (data.length > 1)
				{
					map.put(data[0], data[1]);
				}
				else
				{
					map.put(data[0], "");
				}
			}
			else
			{
				map.put(line, null);
			}
		}
	}

	private void readDnsmasqConfigFile(Context context)
	{
		String filename = this.getDnsqacheConfigFile();
		this.readConfigFile(context, MAP_DNSMASQ, filename, "=");

		// in all cases, turn on or off specific UI options
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		HashMap<String, String> dnsmasqMap = this.getMap(MAP_DNSMASQ);
		boolean bLogQueries = sharedPrefs.getBoolean(PREF_UI_DNS_LOG_QUERIES,
				false);
		if (bLogQueries)
			dnsmasqMap.put(PREF_DNSMASQ_LOG_QUERIES, null);
		else
			dnsmasqMap.remove(PREF_DNSMASQ_LOG_QUERIES);
	}

	private void readTinyProxyConfigFile(Context context)
	{
		String filename = this.getTinyProxyConfigFile();
		this.readConfigFile(context, MAP_TINYPROXY, filename, " ");
	}

	private void readPolipoConfigFile(Context context)
	{
		String filename = this.getPolipoConfigFile();
		this.readConfigFile(context, MAP_POLIPO, filename, "=");
	}

	private void readConfigs(Context context)
	{
		this.readDnsmasqConfigFile(context);
		this.readTinyProxyConfigFile(context);
		this.readPolipoConfigFile(context);
	}

	/*************************************************************************
	 * Public methods
	 ************************************************************************/
	public void put(String mapName, String valueKey, String value)
	{
		HashMap<String, String> map = this.getMap(mapName);
		map.put(valueKey, value);
	}

	public String get(String mapName, String valueKey)
	{
		HashMap<String, String> map = this.getMap(mapName);
		return map.get(valueKey);
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

	public boolean commit(Context context)
	{
		HashMap<String, String> serverMap = this.getMap(MAP_DNSMASQ_OPTS);

		// Initialize the maps
		mInitialized = this.initializeDnsmasq(context)
				&& this.initializeTinyProxy(context)
				&& this.initializePolipo(context);

		return this.commitMap(context, MAP_DNSMASQ,
				this.getDnsqacheConfigFile(), "=")
				&& this.commitMap(context, MAP_TINYPROXY,
						this.getTinyProxyConfigFile(), " ")
				&& this.commitMap(context, MAP_POLIPO,
						this.getPolipoConfigFile(), "=")
				&& this.writeResolvConf(serverMap.get(PREF_DNSMASQ_PRIMARY),
						serverMap.get(PREF_DNSMASQ_SECONDARY));
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

		// assure an uninitialized state since new config dir
		mInitialized = false;

		// reinitialize based on configurations in the new path
		this.readConfigs(context);
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

	public String[] getDNSServers()
	{
		HashMap<String, String> serverMap = this.getMap(MAP_DNSMASQ_OPTS);
		String dnsServers[] = new String[]
			{
					serverMap.get(PREF_DNSMASQ_PRIMARY),
					serverMap.get(PREF_DNSMASQ_SECONDARY)
			};
		return dnsServers;
	}

	public HashMap<String, String> getOptionsMap(String mapName)
	{
		return this.getMap(mapName);
	}
}
