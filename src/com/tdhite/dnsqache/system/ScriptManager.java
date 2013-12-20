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

import android.os.Build;
import android.util.Log;

public class ScriptManager
{
	private static final String TAG = "DNSQACHE -> Scripter";

	public static final String SCRIPT_BINARY = "scripter";

	public static final String SCRIPT_STARTQACHE = "startqache.scr";
	public static final String SCRIPT_STOPQACHE = "stopqache.scr";
	public static final String SCRIPT_RESTARTQACHE = "restartqache.scr";
	public static final String SCRIPT_SETDNS = "setdns.scr";
	public static final String SCRIPT_STATS = "dumpstats.scr";
	public static final String SCRIPT_GENERATELOG = "generatelog.scr";
	public static final String SCRIPT_STARTTINYPROXY = "starttinyproxy.scr";
	public static final String SCRIPT_STOPTINYPROXY = "stoptinyproxy.scr";
	public static final String SCRIPT_STARTPOLIPO= "startpolipo.scr";
	public static final String SCRIPT_STOPPOLIPO = "stoppolipo.scr";

	private ConfigManager mConfigManager = null;

	/*************************************************************************
	 * Private methods
	 */
	@SuppressWarnings("unused")
	private ScriptManager()
	{
		/* disallow default constructor */
	}

	/*************************************************************************
	 * Public methods
	 */
	public ScriptManager(ConfigManager configManager)
	{
		mConfigManager = configManager;
	}

	public boolean runScript(String scriptName, boolean asRoot)
	{
		Log.d(TAG, "Running script: " + scriptName);

		String script = mConfigManager.getBinaryFullPath(
				ScriptManager.SCRIPT_BINARY) + " "
				+ mConfigManager.getScriptFullPath(scriptName);

		// Starting service
		return asRoot ? CoreTask.runRootCommand(script) : CoreTask.runStandardCommand(script); 
	}

	private void addAcceptRule(StringBuilder script, String dnsServer, String proto) {
		script.append("run iptables \"-t\" nat \"-A\" dnsqache \"-p\" ");
		script.append(proto);
		script.append(" \"--dport\" \"53\" \"--destination\" \"");
		script.append(dnsServer);
		script.append("\" \"-j\" RETURN\n");
	}

	private void generateStartScript(ConfigManager config)
	{
		String dnsServers[] = config.getDNSServers();

		StringBuilder script = new StringBuilder();
		script.append("run killall dnsqache\n");
		script.append("run rm -f \"");
			script.append(mConfigManager.getDnsmasqPidFile());
			script.append("\"\n");
		script.append("run \"");
			script.append(mConfigManager.getBinaryFullPath(ConfigManager.DNSMASQ_BINARY));
			script.append("\" \"--conf-file=");
			script.append(mConfigManager.getDnsqacheConfigFile());
			script.append("\"\n");
		script.append("setprop \"dnsqache.status\" running\nrun chmod 644 \"");
			script.append(mConfigManager.getLogFile());
			script.append("\"\n");

		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
		{
				script.append("run iptables \"-t\" nat \"-N\" dnsqache\n");
				for (String dnsServer : dnsServers)
				{
					if (dnsServer != null && dnsServer.length() > 0) {
						this.addAcceptRule(script, dnsServer, "tcp");
						this.addAcceptRule(script, dnsServer, "udp");
					}
				}
				script.append("run \"iptables -t nat -A dnsqache -p udp --dport 53 -j DNAT --to-destination 127.0.0.1:53\n");
				script.append("run \"iptables -t nat -A dnsqache -p tcp --dport 53 -j DNAT --to-destination 127.0.0.1:53\n");
				script.append("run iptables \"-t\" nat \"-I\" OUTPUT \"-j\" dnsqache\n");
		}
		CoreTask.writeLinesToFile(
				mConfigManager.getScriptFullPath(ScriptManager.SCRIPT_STARTQACHE),
				script.toString());
	}

	private void generateStopScript()
	{
		StringBuilder script = new StringBuilder();
		
		script.append("killbypidfile TERM \"");
		script.append(mConfigManager.getDnsmasqPidFile());
		script.append("\"\nsetprop \"dnsqache.status\" stopped\n");

		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
		{
			script.append("run iptables \"-t\" nat \"-D\" OUTPUT \"1\"\n");
			script.append("run iptables \"-t\" nat \"-F\" dnsqache\n");
			script.append("run iptables \"-t\" nat \"-X\" dnsqache\n");
		}

		CoreTask.writeLinesToFile(
				mConfigManager.getScriptFullPath(ScriptManager.SCRIPT_STOPQACHE),
				script.toString());
	}

	private void generateSetDnsScript()
	{
		String script = "run setprop \"net.dns1\" \"127.0.0.1\"\n"
		+ "run echo nameserver \"127.0.0.1\" \">/etc/resolv.conf\"\n"
		+ "killbypidfile HUP \"" + mConfigManager.getDnsmasqPidFile() + "\"\n"
		+ "killbypidfile USR2 \"" + mConfigManager.getDnsmasqPidFile() + "\"\n";

		CoreTask.writeLinesToFile(
				mConfigManager.getScriptFullPath(ScriptManager.SCRIPT_SETDNS),
				script);
	}

	private void generateRestartScript()
	{
		String script = "killbypidfile HUP \""
				+ mConfigManager.getDnsmasqPidFile() + "\"\n";

		CoreTask.writeLinesToFile(
				mConfigManager.getScriptFullPath(ScriptManager.SCRIPT_STATS),
				script);
	}

	private void generateStatsScript()
	{
		String logFile = mConfigManager.getLogFile();
		String script = "killbypidfile USR1 \""
				+ mConfigManager.getDnsmasqPidFile()
				+ "\"\nrun \"logcat -d | grep dnsmasq >"
				+ logFile + "\"\nrun chmod 644 \"" + logFile + "\"\n";

		CoreTask.writeLinesToFile(
				mConfigManager.getScriptFullPath(ScriptManager.SCRIPT_STATS),
				script);
	}

	private void generateStartTinyProxyScript()
	{
		String script = "run killall tinyproxy\n"
				+ "run rm -f \"" + mConfigManager.getTinyProxyLogFile() + "\"\n"
				+ "run rm -f \"" + mConfigManager.getTinyProxyPidFile() + "\"\n"
				+ "run \"" + mConfigManager.getBinaryFullPath(ConfigManager.TINYPROXY_BINARY)
				+ "\" \"-c " + mConfigManager.getTinyProxyConfigFile()
				+ "\"\nrun chmod 644 \"" + mConfigManager.getTinyProxyLogFile()
				+ "\"\nsetprop \"proxyqache.status\" running\n";
		CoreTask.writeLinesToFile(
				mConfigManager.getScriptFullPath(ScriptManager.SCRIPT_STARTTINYPROXY),
				script);
	}

	private void generateStopTinyProxyScript()
	{
		String script = "killbypidfile TERM \""
				+ mConfigManager.getTinyProxyPidFile()
				+ "\"\nsetprop \"proxyqache.status\" stopped\n";

		CoreTask.writeLinesToFile(
				mConfigManager.getScriptFullPath(ScriptManager.SCRIPT_STOPTINYPROXY),
				script);
	}

	private void generateStartPolipoScript()
	{
		String script = "run killall polipo\n"
				+ "run rm -f \"" + mConfigManager.getPolipoLogFile() + "\"\n"
				+ "run rm -f \"" + mConfigManager.getPolipoPidFile() + "\"\n"
				+ "run \"" + mConfigManager.getBinaryFullPath(ConfigManager.POLIPO_BINARY)
				+ "\" \"-c " + mConfigManager.getPolipoConfigFile()
				+ "\"\nsetprop \"proxyqache.status\" running\n";
		CoreTask.writeLinesToFile(mConfigManager
				.getScriptFullPath(ScriptManager.SCRIPT_STARTPOLIPO), script);
	}

	private void generateStopPolipoScript()
	{
		String script = "killbypidfile TERM \""
				+ mConfigManager.getPolipoPidFile()
				+ "\"\nsetprop \"proxyqache.status\" stopped\n";

		CoreTask.writeLinesToFile(
				mConfigManager.getScriptFullPath(ScriptManager.SCRIPT_STOPPOLIPO),
				script);
	}

	public void generateScripts(ConfigManager configs)
	{
		this.generateStartScript(configs);
		this.generateStopScript();
		this.generateSetDnsScript();
		this.generateRestartScript();
		this.generateStatsScript();
		this.generateStartTinyProxyScript();
		this.generateStopTinyProxyScript();
		this.generateStartTinyProxyScript();
		this.generateStopTinyProxyScript();
		this.generateStartPolipoScript();
		this.generateStopPolipoScript();
	}
}
