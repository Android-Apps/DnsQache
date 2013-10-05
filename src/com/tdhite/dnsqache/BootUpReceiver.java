package com.tdhite.dnsqache;

import com.tdhite.dnsqache.system.ConfigManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootUpReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		boolean activateOnBoot = prefs.getBoolean(
				ConfigManager.PREF_START_ON_BOOT, false);

		if (activateOnBoot)
		{
			Intent intentQacheService = new Intent();
	       	intentQacheService.setClass(context, QacheService.class);
	    	context.startService(intentQacheService);
		}
	}
}
