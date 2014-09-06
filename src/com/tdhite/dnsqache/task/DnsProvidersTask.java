package com.tdhite.dnsqache.task;

import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;

import com.tdhite.dnsqache.R;
import com.tdhite.dnsqache.db.CountryHandler;
import com.tdhite.dnsqache.model.DnsProviders;

import android.content.Context;
import android.util.Log;


public class DnsProvidersTask extends Task {
	public static final String TAG = "DnsProvidersTask";

	private static final String DEFAULT_PROVIDERS_URL = "http://public-dns.tk/nameservers.json";

	public DnsProvidersTask(Context context, Callback<Boolean> callback)
	{
		super(Callback.TASK_START, context, callback, false);

		if (mProgressDialog != null) {
			mProgressDialog.setTitle(R.string.text_update_dns_providers);
		}
	}

	@Override
	protected Boolean doInBackground(Void... params)
	{
		String msg = this.mContext.getString(R.string.text_updating_countries);
		publishProgress(msg);
		CountryHandler ch = new CountryHandler(this.mContext);
		ch.clear();

		DnsProviders providers = new DnsProviders(this.mContext);
		URL url = null;
		try
		{
			url = new URL(DEFAULT_PROVIDERS_URL);
		}
		catch (MalformedURLException e)
		{
			Log.e(TAG, "JSON Fetch Failed. Please see logcat!", e);
		}

		msg = this.mContext.getString(R.string.text_fetching_providers);
		publishProgress(msg);
		Log.i(TAG, "Fetching JSON provider information from " + DEFAULT_PROVIDERS_URL);
		JSONArray jsonProviders = providers.getJSONFromUrl(this.mContext, url);

		final String updateMsg = this.mContext.getString(R.string.text_updating_providers);
		msg = String.format(updateMsg, 0, 0);
		publishProgress(updateMsg);
		Log.i(TAG, "Populating provider database.");
		providers.toInternal(this.mContext, jsonProviders, new ProgressCallback<Integer>() {
			@Override
			public void onUpdateProgress(Integer countProcessed, Integer total) {
				String msg = String.format(updateMsg, countProcessed, total);
				publishProgress(msg);
			}
		});

		return true;
	}

	public interface ProgressCallback<T> {
		public void onUpdateProgress(T progress, T total);
	}
}
