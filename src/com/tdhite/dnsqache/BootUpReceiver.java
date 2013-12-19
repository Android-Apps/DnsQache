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
