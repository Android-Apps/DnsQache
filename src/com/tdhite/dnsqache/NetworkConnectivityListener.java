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

/*
  * Copyright (C) 2006 The Android Open Source Project
  *
  * Adaptation for DnaQache Copyright (C) 2013 Tom Hite
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *      http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;

public class NetworkConnectivityListener
{
	private static final String TAG = "DNSQACHE -> NetworkConnectivityListener";
	private static final boolean DBG = true;

	private HashMap<Handler, Integer> mHandlers = new HashMap<Handler, Integer>();

	private Context mContext = null;
	private State mState = State.UNKNOWN;
	private boolean mListening = false;
	private String mReason = null;
	private boolean mIsFailover = false;

	/** Network connectivity information */
	private NetworkInfo mNetworkInfo = null;

	/**
	 * In case of a Disconnect, the connectivity manager may have already
	 * established, or may be attempting to establish, connectivity with another
	 * network. If so, {@code mOtherNetworkInfo} will be non-null.
	 */
	private NetworkInfo mOtherNetworkInfo = null;

	private ConnectivityBroadcastReceiver mReceiver = null;

	private class ConnectivityBroadcastReceiver extends BroadcastReceiver
	{
		@SuppressWarnings("deprecation")
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();

			if (DBG)
			{
				Log.d(TAG, "onReceived() called with " + mState.toString()
						+ " and " + intent);
			}

			if (!action.equals(ConnectivityManager.CONNECTIVITY_ACTION)
					|| mListening == false)
			{
				return;
			}

			boolean noConnectivity = intent.getBooleanExtra(
					ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

			if (noConnectivity)
			{
				mState = State.NOT_CONNECTED;
			}
			else
			{
				mState = State.CONNECTED;
			}

			mNetworkInfo = (NetworkInfo) intent
					.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			mOtherNetworkInfo = (NetworkInfo) intent
					.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

			mReason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
			mIsFailover = intent.getBooleanExtra(
					ConnectivityManager.EXTRA_IS_FAILOVER, false);

			if (DBG)
			{
				Log.d(TAG,
						"onReceive(): mNetworkInfo="
								+ mNetworkInfo
								+ " mOtherNetworkInfo = "
								+ (mOtherNetworkInfo == null ? "[none]"
										: mOtherNetworkInfo + " noConn="
												+ noConnectivity) + " mState="
								+ mState.toString());
			}

			// Notify any handlers.
			Iterator<Handler> it = mHandlers.keySet().iterator();
			while (it.hasNext())
			{
				Handler target = it.next();
				Message message = Message.obtain(target, mHandlers.get(target));
				target.sendMessage(message);
			}
		}
	};

	public enum State
	{
		UNKNOWN,

		/** This state is returned if there is connectivity to any network **/
		CONNECTED,

		/**
		 * This state is returned if there is no connectivity to any network.
		 * This is set to true under two circumstances:
		 * <ul>
		 * <li>When connectivity is lost to one network, and there is no other
		 * available network to attempt to switch to.</li>
		 * <li>When connectivity is lost to one network, and the attempt to
		 * switch to another network fails.</li>
		 * </ul>
		 */
		NOT_CONNECTED
	}

	/**
	 * Create a new NetworkConnectivityListener.
	 */
	public NetworkConnectivityListener()
	{
		mState = State.UNKNOWN;
		mReceiver = new ConnectivityBroadcastReceiver();
	}

	/**
	 * This method starts listening for network connectivity state changes.
	 * 
	 * @param context
	 */
	public synchronized void startListening(Context context)
	{
		if (!mListening)
		{
			mContext = context;

			IntentFilter filter = new IntentFilter();
			filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
			context.registerReceiver(mReceiver, filter);
			mListening = true;
		}
	}

	/**
	 * This method stops this class from listening for network changes.
	 */
	public synchronized void stopListening()
	{
		if (mListening)
		{
			mContext.unregisterReceiver(mReceiver);
			mContext = null;
			mNetworkInfo = null;
			mOtherNetworkInfo = null;
			mIsFailover = false;
			mReason = null;
			mListening = false;
		}
	}

	/**
	 * This methods registers a Handler to be called back onto with the
	 * specified what code when the network connectivity state changes.
	 * 
	 * @param target
	 *            The target handler.
	 * @param what
	 *            The what code to be used when posting a message to the
	 *            handler.
	 */
	public void registerHandler(Handler target, int what)
	{
		mHandlers.put(target, what);
	}

	/**
	 * This methods unregisters the specified Handler.
	 * 
	 * @param target
	 */
	public void unregisterHandler(Handler target)
	{
		mHandlers.remove(target);
	}

	public State getState()
	{
		return mState;
	}

	/**
	 * Return the NetworkInfo associated with the most recent connectivity
	 * event.
	 * 
	 * @return {@code NetworkInfo} for the network that had the most recent
	 *         connectivity event.
	 */
	public NetworkInfo getNetworkInfo()
	{
		return mNetworkInfo;
	}

	/**
	 * If the most recent connectivity event was a DISCONNECT, return any
	 * information supplied in the broadcast about an alternate network that
	 * might be available. If this returns a non-null value, then another
	 * broadcast should follow shortly indicating whether connection to the
	 * other network succeeded.
	 * 
	 * @return NetworkInfo
	 */
	public NetworkInfo getOtherNetworkInfo()
	{
		return mOtherNetworkInfo;
	}

	/**
	 * Returns true if the most recent event was for an attempt to switch over
	 * to a new network following loss of connectivity on another network.
	 * 
	 * @return {@code true} if this was a failover attempt, {@code false}
	 *         otherwise.
	 */
	public boolean isFailover()
	{
		return mIsFailover;
	}

	/**
	 * An optional reason for the connectivity state change may have been
	 * supplied. This returns it.
	 * 
	 * @return the reason for the state change, if available, or {@code null}
	 *         otherwise.
	 */
	public String getReason()
	{
		return mReason;
	}
}
