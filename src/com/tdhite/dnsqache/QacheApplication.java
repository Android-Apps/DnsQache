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

*/

package com.tdhite.dnsqache;

import com.tdhite.dnsqache.system.ConfigManager;
import com.tdhite.dnsqache.system.CoreTask;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.preference.PreferenceManager;
import android.util.Log;

public class QacheApplication extends Application
{
	public static final String TAG = "DNSQACHE -> QacheApplication";

	// See private void openAboutDialog()
	public static final String AUTHORS = "<HTML><a href=\"https://plus.google.com/107383659668377669605\">Tom Hite</a></HTML>";

	// Notification
	private NotificationManager mNotificationManager = null;
	private Notification mNotification = null;

	// Intents
	private PendingIntent mMainIntent = null;

	// Configuration Management
	private ConfigManager mConfigManager = null;

	// CoreTask
	private CoreTask mCoreTask = null;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate()
	{
		Log.d(TAG, "Calling onCreate()");

		// qache.cfg
		this.mConfigManager = new ConfigManager();
		this.mConfigManager.setDataFileDir(
				this, this.getFilesDir().getParent());
		Log.d(TAG, "Current directory is "
				+ this.getApplicationContext().getFilesDir().getParent());

		// init notificationManager
		this.mNotificationManager = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);
    	this.mNotification = new Notification(R.drawable.start_notification,
    			"DNSQache", System.currentTimeMillis());

		this.mMainIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				MainActivity.class), 0);

		// Initialize configuration
		this.updateDNSConfiguration();
	}

	@Override
	public void onTerminate()
	{
		Log.d(TAG, "Calling onTerminate()");
	}

	public void updateDNSConfiguration(String primaryDns, String secondaryDns,
			int cacheSize)
	{
		long startStamp = System.currentTimeMillis();
		SharedPreferences sharedPrefs =
				PreferenceManager.getDefaultSharedPreferences(this);
		Editor prefsEditor = sharedPrefs.edit();

		// the number seven is just because 1.1.1.1 is seven chars, eh?
		if (primaryDns == null)
		{
			primaryDns = mConfigManager.get(ConfigManager.MAP_DNSMASQ_OPTS,
					ConfigManager.PREF_DNSMASQ_PRIMARY);
			if (primaryDns == null || primaryDns.length() < 7)
			{
				primaryDns = sharedPrefs.getString(
						this.getString(R.string.default_name_server1),
						ConfigManager.PREF_DNSMASQ_DEFAULT_PRIMARY_IP);
			}
		}

		if (secondaryDns == null)
		{
			secondaryDns = mConfigManager.get(ConfigManager.MAP_DNSMASQ_OPTS,
					ConfigManager.PREF_DNSMASQ_SECONDARY);
			if (secondaryDns == null || secondaryDns.length() < 7)
			{
				secondaryDns = sharedPrefs.getString(
						this.getString(R.string.default_name_server2),
						ConfigManager.PREF_DNSMASQ_DEFAULT_SECONDARY_IP);
			}
		}

		String dnsMaxCacheSize;
		if (cacheSize <= 0)
		{
			dnsMaxCacheSize = sharedPrefs.getString(
				this.getString(R.string.property_dnsmasq_cachesize),
				ConfigManager.PREF_DNSMASQ_DEFAULT_CACHE_SIZE);
		}
		else
		{
			dnsMaxCacheSize = "" + cacheSize;
		}

		// put the values into the config manager
		mConfigManager.put(ConfigManager.MAP_DNSMASQ_OPTS,
				ConfigManager.PREF_DNSMASQ_PRIMARY, primaryDns);
		mConfigManager.put(ConfigManager.MAP_DNSMASQ_OPTS,
				ConfigManager.PREF_DNSMASQ_SECONDARY, secondaryDns);
		mConfigManager.put(ConfigManager.MAP_DNSMASQ_OPTS,
				ConfigManager.PREF_DNSMASQ_CACHESIZE, dnsMaxCacheSize);

		// put the values into shared preferences for others to read
		prefsEditor.putString(
				ConfigManager.PREF_DNSMASQ_PRIMARY, primaryDns);
		prefsEditor.putString(
				ConfigManager.PREF_DNSMASQ_SECONDARY, secondaryDns);
		prefsEditor.putString(
				ConfigManager.PREF_DNSMASQ_CACHESIZE, dnsMaxCacheSize);
		prefsEditor.commit();

		// writing the config
		if (mConfigManager.commit(this))
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

	public void updateDNSConfiguration()
	{
		this.updateDNSConfiguration(null, null, 0);
	}

	// get preferences on whether donate-dialog should be displayed
	public boolean showDonationDialog()
	{
		SharedPreferences sharedPrefs =
				PreferenceManager.getDefaultSharedPreferences(this);
		return sharedPrefs.getBoolean(ConfigManager.PREF_DONATE, true);
	}

	public int getNotificationType()
	{
		SharedPreferences sharedPrefs =
				PreferenceManager.getDefaultSharedPreferences(this);
		return Integer.parseInt(
				sharedPrefs.getString(ConfigManager.PREF_NOTIFICATION, "2"));
	}

	// Notification
	@SuppressWarnings("deprecation")
	public Notification getStartNotification()
	{
		mNotification.flags = Notification.FLAG_ONGOING_EVENT;
		mNotification.setLatestEventInfo(this, getString(R.string.app_name),
				getString(R.string.qache_is_running), this.mMainIntent);
		this.mNotificationManager.notify(-1, this.mNotification);
		return mNotification;
	}

	public int getVersionNumber()
	{
		int version = -1;
		try
		{
			PackageInfo pi = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			version = pi.versionCode;
		}
		catch (Exception e)
		{
			Log.e(TAG, "Package name not found", e);
		}
		return version;
	}

	public String getVersionName()
	{
		String version = "?";
		try
		{
			PackageInfo pi = getPackageManager().getPackageInfo(
					getPackageName(), 0);
			version = pi.versionName;
		}
		catch (Exception e)
		{
			Log.e(TAG, "Package name not found", e);
		}
		return version;
	}

	public ConfigManager getConfigManager()
	{
		return this.mConfigManager;
	}

	public CoreTask getCoreTask()
	{
		return this.mCoreTask;
	}

	@SuppressLint("CommitPrefEdits")
	public Editor getSettingsEditor()
	{
		SharedPreferences sharedPrefs =
				PreferenceManager.getDefaultSharedPreferences(this);
		Editor prefsEditor = sharedPrefs.edit();
		return prefsEditor;
	}

	public NotificationManager getNotificationManager()
	{
		return this.mNotificationManager;
	}
}
