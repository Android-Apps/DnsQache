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

package com.tdhite.dnsqache;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.tdhite.dnsqache.system.ConfigManager;
import com.tdhite.dnsqache.system.CoreTask;
import com.tdhite.dnsqache.system.ScriptManager;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class QacheService extends Service
{
	private static final String TAG = "DNSQACHE -> QacheService";

	public static final String STATECHANGED_INTENT = "com.tdhite.dnsaqche.intent.STATE";
	public static final String SERVICEMANAGE_INTENT = "com.tdhite.dnsaqche.intent.MANAGE";
	public static final String REQUESTCOUNT_INTENT = "com.tdhite.dnsaqche.intent.TRAFFIC";
	public static final String ZERO = "0";

	public static final String ANDROID_STATUS = "dnsqache.status";
	public static final String ANDROID_CACHE_TTL = "networkaddress.cache.ttl";
	public static final String ANDROID_NEG_TTL = "networkaddress.cache.negative.ttl";

	private static QacheService mSingleton = null;

	// Qaching states
	public enum State
	{
		/* Qache States */
		RUNNING, IDLE, STARTING, STOPPING, RESTARTING, FAILURE_LOG, FAILURE_EXE,

		/* Service states */
		STARTED, START, STOPPED, STOP;
	}

	private final Binder mBinder = new LocalBinder();

	private ScriptManager mScripter = null;
	private QacheApplication mQacheApplication = null;

	// Default state
	private State mState = State.IDLE;

	// Timestamp of the last counter update
	long mTimestampCounterUpdate = 0;

	// Network connectivity changes
	NetworkHandler mNetworkHandler = null;

	// Setforeground
	private Method mSetForeground;
	private Method mStartForeground;
	private Method mStopForeground;
	private Object[] mStartForegroundArgs = new Object[2];
	private Object[] mStopForegroundArgs = new Object[1];
	private Object[] mSetForegroundArgs = new Object[1];

	private static final Class<?>[] mSetForegroundSignature = new Class[]
		{
			boolean.class
		};

	private static final Class<?>[] mStartForegroundSignature = new Class[]
		{
				int.class, Notification.class
		};

	private static final Class<?>[] mStopForegroundSignature = new Class[]
		{
			boolean.class
		};

	/*************************************************************************
	 * Static methods
	 */
	@SuppressWarnings("unused")
	private static IBinder getService(String service) throws Exception
	{
		Class<?> ServiceManager = Class.forName("android.os.ServiceManager");
		Method getService_method = ServiceManager.getMethod("getService",
				new Class[]
					{
						String.class
					});
		IBinder b = (IBinder) getService_method.invoke(null, new Object[]
			{
				service
			});
		return b;
	}

	public static QacheService getSingleton()
	{
		return mSingleton;
	}

	public static boolean isDnsQacheActive()
	{
		boolean bActive = false;

		try
		{
			QacheService _this = QacheService.getSingleton();
			if (_this != null)
			{
				String dnsqache = _this.mQacheApplication.getConfigManager()
						.getBinaryFullPath(ConfigManager.DNSMASQ_BINARY);
				bActive = CoreTask.isProcessRunning(dnsqache);
			}
		}
		catch (Exception e)
		{
			Log.e(TAG, "Exception occurred when checking for DnsQache binary",
					e);
			bActive = false;
		}

		String qacheStatus = CoreTask.getProp(ANDROID_STATUS);
		return bActive && (qacheStatus.equals("running"));
	}

	public static boolean isProxyActive()
	{
		boolean bActive = false;

		try
		{
			QacheService _this = QacheService.getSingleton();
			if (_this != null)
			{
				String polipo = _this.mQacheApplication.getConfigManager()
						.getBinaryFullPath(ConfigManager.POLIPO_BINARY);
				String tinyproxy = _this.mQacheApplication.getConfigManager()
						.getBinaryFullPath(ConfigManager.TINYPROXY_BINARY);
				bActive = CoreTask.isProcessRunning(polipo)
						|| CoreTask.isProcessRunning(tinyproxy);
			}
		}
		catch (Exception e)
		{
			Log.w(TAG, "Exception occurred when checking for DnsQache binary",
					e);
		}

		String qacheStatus = CoreTask.getProp(ANDROID_STATUS);
		return bActive && (qacheStatus.equals("running"));
	}

	/*************************************************************************
	 * Private methods
	 */
	private void invokeMethod(Method method, Object[] args)
	{
		try
		{
			method.invoke(this, args);
		}
		catch (InvocationTargetException e)
		{
			// Should not happen.
			Log.w(TAG, "Unable to invoke method", e);
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			// Should not happen.
			Log.w(TAG, "Unable to invoke method", e);
			e.printStackTrace();
		}
	}

	private void initForeground()
	{
		try
		{
			mStartForeground = getClass().getMethod("startForeground",
					mStartForegroundSignature);
			mStopForeground = getClass().getMethod("stopForeground",
					mStopForegroundSignature);
			return;
		}
		catch (NoSuchMethodException e)
		{
			// Running on an older platform.
			mStartForeground = mStopForeground = null;
		}

		try
		{
			mSetForeground = getClass().getMethod("setForeground",
					mSetForegroundSignature);
		}
		catch (NoSuchMethodException e)
		{
			throw new IllegalStateException(
					"OS doesn't have Service.startForeground OR Service.setForeground!");
		}
	}

	private void handleCommand(Intent intent)
	{
		if (intent != null)
		{
			this.start();
		}
	}

	/**
	 * This stops the DnsMasq daemon, but not the QacheService itself
	 */
	private void stopDaemons()
	{
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		int proxyAppIndex = sharedPrefs.getInt(ConfigManager.PREF_UI_PROXY_SPINNER_POSITION,
				ConfigManager.PROXY_DEFAULT_SPINNER_POSITION);
		String stopProxyScript = null;

		if (proxyAppIndex > 0)
		{
			stopProxyScript = proxyAppIndex == 1 ? ScriptManager.SCRIPT_STOPPOLIPO
					: ScriptManager.SCRIPT_STOPTINYPROXY;
		}

		resetDns();
		boolean worked = mScripter.runScript(ScriptManager.SCRIPT_STOPQACHE, true)
				&& stopProxyScript == null ? true : mScripter.runScript( stopProxyScript,
						true);

		mState = worked ? mState : State.FAILURE_EXE;
	}

	/**
	 * This starts the DnsMasq daemon
	 */
	synchronized private void startDaemons()
	{
		// Check if qache service is already-running
		if (mState != State.RUNNING)
		{
			SharedPreferences sharedPrefs = PreferenceManager
					.getDefaultSharedPreferences(this);
			int proxyAppIndex = sharedPrefs.getInt(ConfigManager.PREF_UI_PROXY_SPINNER_POSITION,
					ConfigManager.PROXY_DEFAULT_SPINNER_POSITION);
			boolean worked = true;

			if (proxyAppIndex > 0)
			{
				Log.d(TAG, "Starting proxy  . . .");
				worked = proxyAppIndex == 1 ?
						mScripter.runScript(ScriptManager.SCRIPT_STARTPOLIPO, true) :
				mScripter.runScript(ScriptManager.SCRIPT_STARTTINYPROXY, true);
			}

			if (!worked)
			{
				Log.d(TAG, ". . . tinyproxy failed to start!");
				mState = State.FAILURE_EXE;
			}
			else
			{
				Log.d(TAG, ". . . tinyproxy started Successfully. Now starting dnsmasq  . . .");
				worked = mScripter.runScript(ScriptManager.SCRIPT_STARTQACHE, true);
				if (worked)
				{
					Log.d(TAG, ". . . dnsmasq started successfully.");
					mState = State.RUNNING;
				}
				else
				{
					Log.d(TAG, ". . . dnsmasq started successfully.");
					mState = State.FAILURE_EXE;
				}
			}
		}
	}

	private boolean saveCurrentDnsServers()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Editor prefEditor = prefs.edit();
		String[] dnsServers = CoreTask.getCurrentDns(
				"127.0.0.1", "");
		boolean saved = false;

		if (dnsServers[0].equals("127.0.0.1"))
		{
			Log.d(TAG, "Not saving old DNS Servers because they point to localhost.");
		}
		else
		{
			prefEditor.putString(ConfigManager.PREF_DNS_OLDPRIMARY, dnsServers[0]);
			prefEditor.putString(ConfigManager.PREF_DNS_OLDSECONDARY, dnsServers[1]);
			prefEditor.commit();
			saved = true;
		}

		return saved;
	}

	private String[] getOldDnsServers()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String[] dnsServers = { null, null };

		dnsServers[0] = prefs.getString(ConfigManager.PREF_DNS_OLDPRIMARY, null);
		dnsServers[1] = prefs.getString(ConfigManager.PREF_DNS_OLDSECONDARY, "");

		if (dnsServers[0] == null)
		{
			Log.d(TAG, "Failed to get old name servers, setting to localhost.");
			dnsServers[0] = "127.0.0.1";
		}

		return dnsServers;
	}

	private void stopAndroidCache()
	{
		// save off the current dns cache values
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Editor prefsEditor = prefs.edit();
		String oldSystemTTL = System.getProperty(ANDROID_CACHE_TTL);
		String oldSecurityTTL = System.getProperty(ANDROID_CACHE_TTL);
		String oldNegTTL = System.getProperty(ANDROID_NEG_TTL);

		// Sanity checks
		if (oldSystemTTL == null) oldSystemTTL = "0";
		if (oldSecurityTTL == null) oldSecurityTTL = "0";
		if (oldNegTTL == null) oldNegTTL = "0";

		// Save the old values for later use
		prefsEditor.putString("system." + ANDROID_CACHE_TTL, oldSystemTTL);
		prefsEditor.putString("security." + ANDROID_CACHE_TTL, oldSecurityTTL);
		prefsEditor.putString("system." + ANDROID_NEG_TTL, oldSecurityTTL);
		prefsEditor.commit();

		// Stop android from caching any more
		System.setProperty(ANDROID_CACHE_TTL, ZERO);
		System.setProperty(ANDROID_CACHE_TTL, ZERO);
		System.setProperty(ANDROID_NEG_TTL, ZERO);
	}

	private void restoreAndroidCache()
	{
		// save off the current dns cache values
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String oldSystemTTL = prefs.getString("system." + ANDROID_CACHE_TTL, ZERO);
		String oldSecurityTTL = prefs.getString("security." + ANDROID_CACHE_TTL, ZERO);
		String oldNegTTL = prefs.getString("security." + ANDROID_NEG_TTL, ZERO);

		// Stop android from caching any more
		System.setProperty(ANDROID_CACHE_TTL, oldSystemTTL);
		System.setProperty(ANDROID_CACHE_TTL, oldSecurityTTL);
		System.setProperty(ANDROID_NEG_TTL, oldNegTTL);
	}

	/*************************************************************************
	 * Protected methods
	 */

	/**
	 * This is a wrapper around the new stopForeground method, using the older
	 * APIs if it is not available.
	 */
	protected void stopForegroundCompat(int id)
	{
		// If we have the new stopForeground API, then use it.
		if (mStopForeground != null)
		{
			mStopForegroundArgs[0] = Boolean.TRUE;
			this.invokeMethod(this.mStopForeground, mStopForegroundArgs);
			return;
		}

		/*
		 * Fall back on the old API. Note to cancel BEFORE changing the
		 * foreground state, since we could be killed at that point.
		 */
		mQacheApplication.getNotificationManager().cancel(id);
		mSetForegroundArgs[0] = Boolean.FALSE;
		this.invokeMethod(mSetForeground, mSetForegroundArgs);
	}

	/**
	 * This is a wrapper around the new startForeground method, using the older
	 * APIs if it is not available.
	 */
	protected void startForegroundCompat(int id, Notification notification)
	{
		// If we have the new startForeground API, then use it.
		if (mStartForeground != null)
		{
			mStartForegroundArgs[0] = Integer.valueOf(id);
			mStartForegroundArgs[1] = notification;
			this.invokeMethod(mStartForeground, mStartForegroundArgs);
			return;
		}

		// Fall back on the old API.
		mSetForegroundArgs[0] = Boolean.TRUE;
		this.invokeMethod(mSetForeground, mSetForegroundArgs);
		mQacheApplication.getNotificationManager().notify(id, notification);
	}

	/*************************************************************************
	 * Public methods
	 */
	/**
	 * Starts the qache (dnsmasq) service
	 */
	public void start()
	{
		sendStateBroadcast(State.STARTING.ordinal());
		mState = State.STARTING;

		// save off the current dns cache values
		stopAndroidCache();
		
		new Thread(new Runnable()
		{
			public void run()
			{
				// Generate configuration
				QacheService.this.mQacheApplication.updateDNSConfiguration();

				// Start the dnsmasq daemon
				QacheService.this.startDaemons();

				// Check if qache.status was set to "running"
				String qacheStatus = CoreTask.getProp(ANDROID_STATUS);

				if (qacheStatus.equals("running"))
				{
					// Set the DNS name servers for the linux and android sides
					QacheService.this.setDns();
				}
				else
				{
					mState = State.FAILURE_LOG;
				}

				QacheService.this.sendStateBroadcast(mState.ordinal());
			}
		}).start();

		mNetworkHandler.startListening(this);
		this.startForegroundCompat(-1, mQacheApplication.getStartNotification());
	}

	/*
	 * Stops the service and underlying processes.
	 */
	public void stop()
	{
		sendStateBroadcast(State.STOPPING.ordinal());
		mState = State.STOPPING;

		new Thread(new Runnable()
		{
			public void run()
			{
				QacheService.this.stopDaemons();

				QacheService.this.mQacheApplication.getNotificationManager()
						.cancelAll();

				// Check for failed-state
				if (mState != State.FAILURE_EXE)
				{
					mState = State.IDLE;
				}

				QacheService.this.sendStateBroadcast(mState.ordinal());
				QacheService.this.sendManageBroadcast(State.STOPPED.ordinal());
			}
		}).start();

		// restore the android (java) dns cache values
		restoreAndroidCache();

		QacheService.this.stopForegroundCompat(-1);
	}

	public void restart()
	{
		mState = State.RESTARTING;
		sendStateBroadcast(mState.ordinal());

		new Thread(new Runnable()
		{
			public void run()
			{
				// Stop the dns cache daemon
				QacheService.this.stopDaemons();

				// Generate configuration
				mQacheApplication.updateDNSConfiguration();

				// Start the dns cache daemon
				QacheService.this.startDaemons();

				// Check if qache.status was set to "running"
				String qacheStatus = CoreTask.getProp(ANDROID_STATUS);
				if (qacheStatus.equals("running") == false)
				{
					mState = State.FAILURE_LOG;
				}
				else
				{
					// Set the DNS name servers for the linux and android sides
					QacheService.this.setDns();
				}
				QacheService.this.sendStateBroadcast(mState.ordinal());
			}
		}).start();
	}

	public int getState()
	{
		return mState.ordinal();
	}

	public void sendStateBroadcast(int state)
	{
		Intent intent = new Intent(STATECHANGED_INTENT);
		intent.setAction(STATECHANGED_INTENT);
		intent.putExtra("state", state);
		this.sendBroadcast(intent);
	}

	public void sendManageBroadcast(int state)
	{
		Intent intent = new Intent(SERVICEMANAGE_INTENT);
		intent.setAction(SERVICEMANAGE_INTENT);
		intent.putExtra("state", state);
		this.sendBroadcast(intent);
	}

	public void sendRequestsBroadcast(long requests)
	{
		Intent intent = new Intent(QacheService.REQUESTCOUNT_INTENT);
		intent.setAction(QacheService.REQUESTCOUNT_INTENT);
		intent.putExtra("request", requests);
		this.sendBroadcast(intent);
	}

	public void setDns()
	{
		if (this.mScripter != null && this.mNetworkHandler != null
				&& saveCurrentDnsServers())
		{
			// run the script to point at the local dnsqache (dnsmasq) server
			boolean worked = mScripter.runScript(ScriptManager.SCRIPT_SETDNS, true);
			mState = worked ? mState : State.FAILURE_EXE;
			if (!worked)
			{
				Log.d(TAG, "setDns failed to execute "
						+ ScriptManager.SCRIPT_SETDNS);
			}
		}
	}

	public void resetDns()
	{
		String[] oldServers = getOldDnsServers();
		String resetCmd = "setprop net.dns1 \""
				+ oldServers[0] + "\" && "
				+ "setprop net.dns2 \""
				+ oldServers[1] + "\"";
		CoreTask.runRootCommand(resetCmd);
	}

	/*************************************************************************
	 * Override methods
	 */
	@Override
	public IBinder onBind(Intent intent)
	{
		return mBinder;
	}

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder
	{
		QacheService getService()
		{
			return QacheService.this;
		}
	}

	@Override
	public void onCreate()
	{
		Log.d(TAG, "============= QacheService started! =============");

		// Init foreground
		this.initForeground();

		// Initialize itself
		mSingleton = this;

		// Initialize QacheApplication
		mQacheApplication = (QacheApplication) mSingleton.getApplication();

		// Initialize the script manager
		mScripter = new ScriptManager(mQacheApplication.getConfigManager());
		mScripter.generateScripts(mQacheApplication.getConfigManager());

		// Init timeStampCounterUpdate
		mTimestampCounterUpdate = System.currentTimeMillis();

		// Check state
		String mQacheStatus = CoreTask.getProp(ANDROID_STATUS);

		if (mNetworkHandler == null)
		{
			mNetworkHandler = new NetworkHandler();
		}

		if (mQacheStatus.equals("running"))
		{
			Editor prefs = QacheService.this.mQacheApplication
					.getSettingsEditor();
			prefs.putBoolean(ConfigManager.PREF_KEEP_REQUEST_COUNT, false);
			prefs.commit();

			mState = State.RUNNING;
			this.sendStateBroadcast(State.RUNNING.ordinal());
			this.startForegroundCompat(-1,
					mQacheApplication.getStartNotification());
		}
		else
		{
			// Send a "state" broadcast -- Qache not running means idle
			this.sendStateBroadcast(State.IDLE.ordinal());
		}

		if (mQacheStatus.equals("running") == false)
			this.sendManageBroadcast(State.STARTED.ordinal());
	}

	@Override
	public void onDestroy()
	{
		Log.d(TAG, "============= QacheService stopped! =============");
		mSingleton = null;

		// Stopping updating DNS
		if (mNetworkHandler != null)
		{
			mNetworkHandler.stopListening();
			mNetworkHandler = null;
		}

		// Stopping the Qache
		this.stop();

		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		this.handleCommand(intent);

		/* Stay alive */
		return START_STICKY;
	}

	public boolean generateLogFile()
	{
		boolean worked = false;

		if (this.mScripter != null)
		{
			worked = mScripter.runScript(ScriptManager.SCRIPT_STATS, true);
			mState = worked ? mState : State.FAILURE_EXE;
			if (!worked)
			{
				Log.d(TAG, "Failed to create log -- try again later.");
			}
		}

		return worked;
	}
}
