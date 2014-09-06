package com.tdhite.dnsqache.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import com.tdhite.dnsqache.R;
import com.tdhite.dnsqache.db.Constants;
import com.tdhite.dnsqache.db.DnsProviderHandler;
import com.tdhite.dnsqache.system.ConfigManager;
import com.tdhite.dnsqache.task.DnsProvidersTask.ProgressCallback;

/**
 * DnsProviders is a hash of countries full of nameservers mapping country and
 * city to nameservers.
 * 
 * @author tdhite
 * 
 */

public class DnsProviders {
	private static final String TAG = "DnsProviders";

	private DnsProviderHandler mDbHandler = null;

	public DnsProviders(Context context) {
		this.mDbHandler = new DnsProviderHandler(context);
	}

	public void fromResources(Context ctx) {
		Resources res = ctx.getResources();
		String[] providers = res
				.getStringArray(R.array.array_dns_provider_names);
		for (int i = 0; i < providers.length; i++) {
			int arrayId = res.getIdentifier(providers[i], Constants.ARRAY,
					ctx.getPackageName());
			try {
				String addrs[] = res.getStringArray(arrayId);

				DnsProvider provider = new DnsProvider(providers[i], addrs[0],
						i * 2);
				mDbHandler.create(provider);

				provider = new DnsProvider(providers[i], addrs[0], (i * 2) + 1);
				mDbHandler.create(provider);
			} catch (NotFoundException e) {
				Log.e(TAG, e.getMessage());
			}
		}
	}

	public JSONArray getJSONFromUrl(Context context, URL url) {
		InputStream is = null;
		JSONArray jsonArr = null;
		String json = Constants.EMPTY_STRING;

		try {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(url.toExternalForm());
			HttpResponse httpResponse = httpClient.execute(httpGet);
			HttpEntity httpEntity = httpResponse.getEntity();
			is = httpEntity.getContent();
		} catch (UnsupportedEncodingException e) {
			String msg = "UnsupportedEncodingException: Pleease check logcat!";
			Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
			Log.e(TAG, msg, e);
		} catch (ClientProtocolException e) {
			String msg = "ClientProcotocolException: Pleease check logcat!";
			Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
			Log.e(TAG, msg, e);
		} catch (IOException e) {
			String msg = "IO Exception: Pleease check logcat!";
			Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
			Log.e(TAG, msg, e);
		}

		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					is, "iso-8859-1"), 8);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + Constants.ENDOFLINE);
			}
			is.close();
			json = sb.toString();

		} catch (Exception e) {
			Log.e("Buffer Error", "Error converting result " + e.toString());
		}

		// try parse the string to a JSON object
		try {
			jsonArr = new JSONArray(json);
		} catch (JSONException e) {
			Log.e("JSON Parser", "Error parsing data " + e.toString());
		}

		// return JSON String
		return jsonArr;

	}

	public void toInternal(Context context, JSONArray jsonProviders,
			ProgressCallback<Integer> progressCallback) {
		if (jsonProviders == null || jsonProviders.length() == 0) {
			// don't wreck what already exists if nothing came in
			return;
		}

		// clear the table before (re)populating
		this.mDbHandler.clear();

		SQLiteDatabase db = null;
		int i, valid = 0, nProviders = jsonProviders.length();
		for (i = 0; i < nProviders; i++) {
			JSONObject nameserver = null;
			try {
				nameserver = jsonProviders.getJSONObject(i);
			} catch (JSONException e) {
				String msg = "Error retrieving nameserver (" + i + ").";
				Log.e(TAG, msg, e);
			}

			DnsProvider provider = new DnsProvider(nameserver);
			if (provider.getState().equals(DnsProvider.STATE_VALID)
					&& ConfigManager.validateIpAddress(context,
							provider.getIp())) {
				db = this.mDbHandler.create(db, provider);
				valid++;
			}

			if ((i % 100) == 0) {
				progressCallback.onUpdateProgress(Integer.valueOf(i),
						Integer.valueOf(nProviders));
			}
		}

		// close the DB since the loop does not do so
		if (db != null) {
			db.close();
		}
		Log.i(TAG, "Processed " + valid + " valid provider records out of " + i
				+ " total.");
	}
}
