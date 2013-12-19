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

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class NetworkHandler extends Handler
{
	private static final String TAG = "DNSQACHE -> NetworkHandler";

	private static NetworkConnectivityListener mNetworkConnectivityListener = null;
	private static int CONNECTIVITY_MSG = 0;

	private boolean mIsListening = false;

	@Override
	public void handleMessage(Message msg)
	{
		QacheService svc = QacheService.getSingleton();

		super.handleMessage(msg);

		if (svc == null)
		{
			Log.d(TAG, "handleMessage called but got NULL for QacheService.getSingleton()");
		}
		else
		{
			svc.setDns();
		}
	}

	public NetworkConnectivityListener getNetworkConnectivityListener()
	{
		if (mNetworkConnectivityListener == null)
		{
			mNetworkConnectivityListener =
					new NetworkConnectivityListener();
		}
		return mNetworkConnectivityListener;
	}

	public void startListening(Context context)
	{
		if (!mIsListening)
		{
			NetworkConnectivityListener ncl = getNetworkConnectivityListener();
			ncl.registerHandler(this, CONNECTIVITY_MSG);
			ncl.startListening(context);
		}
	}

	public void stopListening()
	{
		NetworkConnectivityListener ncl = getNetworkConnectivityListener();
		ncl.stopListening();
		ncl.unregisterHandler(this);
	}
}
