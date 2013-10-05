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
