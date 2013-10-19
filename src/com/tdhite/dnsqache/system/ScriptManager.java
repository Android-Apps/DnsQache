package com.tdhite.dnsqache.system;

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

	private void generateStartScript()
	{
		String script ="run killall dnsqache\n"
				+ "run rm -f \"" + mConfigManager.getDnsmasqPidFile() + "\"\n"
				+ "run \"" + mConfigManager.getBinaryFullPath(ConfigManager.DNSMASQ_BINARY)
				+ "\" \"--conf-file=" + mConfigManager.getDnsqacheConfigFile()
				+ "\"\nsetprop \"dnsqache.status\" running\nrun chmod 644 \""
				+ mConfigManager.getLogFile() + "\"\n";
		CoreTask.writeLinesToFile(
				mConfigManager.getScriptFullPath(ScriptManager.SCRIPT_STARTQACHE),
				script);
	}

	private void generateStopScript()
	{
		String script = "killbypidfile TERM \""
				+ mConfigManager.getDnsmasqPidFile()
				+ "\"\nsetprop \"dnsqache.status\" stopped\n";

		CoreTask.writeLinesToFile(
				mConfigManager.getScriptFullPath(ScriptManager.SCRIPT_STOPQACHE),
				script);
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
		String script = "killbypidfile USR1 \""
				+ mConfigManager.getDnsmasqPidFile()
				+ "\"\nrun \"logcat -d | grep dnsmasq >"
				+ mConfigManager.getLogFile() + "\"\n";

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

	public void generateScripts()
	{
		this.generateStartScript();
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
