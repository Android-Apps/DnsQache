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
import android.text.Html;
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

	// CoreTask
	private CoreTask mCoreTask = null;

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate()
	{
		Log.d(TAG, "Calling onCreate()");

		// qache.cfg
		ConfigManager configManager = ConfigManager.getConfigManager();
		configManager.setDataFileDir(this, this.getFilesDir().getParent());
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
		ConfigManager.getConfigManager().updateDNSConfiguration(this);
	}

	@Override
	public void onTerminate()
	{
		Log.d(TAG, "Calling onTerminate()");
	}

	// get preferences on whether donate-dialog should be displayed
	public boolean showDonationDialog()
	{
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		return sharedPrefs.getBoolean(ConfigManager.PREF_DONATE, true);
	}

	public int getNotificationType()
	{
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		return Integer.parseInt(sharedPrefs.getString(
				ConfigManager.PREF_NOTIFICATION, "2"));
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

	public CoreTask getCoreTask()
	{
		return this.mCoreTask;
	}

	@SuppressLint("CommitPrefEdits")
	public Editor getSettingsEditor()
	{
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		Editor prefsEditor = sharedPrefs.edit();
		return prefsEditor;
	}

	public NotificationManager getNotificationManager()
	{
		return this.mNotificationManager;
	}

	public String getVersionString()
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

	public CharSequence getAuthors()
	{
		String authors = this.getString(R.string.about_layout_authors_names);
		return Html.fromHtml(authors);
	}
}
