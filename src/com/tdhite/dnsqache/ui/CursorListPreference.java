package com.tdhite.dnsqache.ui;

import android.content.Context;
import android.database.Cursor;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.util.Log;

import com.tdhite.dnsqache.db.CountryHandler;
import com.tdhite.dnsqache.db.DnsProviderHandler;
import com.tdhite.dnsqache.model.Country;
import com.tdhite.dnsqache.model.DnsProvider;

public class CursorListPreference extends ListPreference {
	public static final String TAG = "DNSQACHE.CursorListPreference";

	public CursorListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setCountryData() {
		CountryHandler db = new CountryHandler(this.getContext());
		Cursor cursor = db.read();

		CharSequence[] entries = null;
		CharSequence[] values = null;

		if (cursor == null || cursor.getCount() == 0) {
			entries = new CharSequence[] {};
			values = new CharSequence[] {};
			Log.w(TAG, "No DB info returned for countries.");
		} else {
			cursor.moveToFirst();
			entries = new CharSequence[cursor.getCount()];
			values = new CharSequence[cursor.getCount()];
			int i = 0;
			do {
				values[i] = cursor.getString(Country.COL_IDX_ID);
				entries[i] = cursor.getString(Country.COL_IDX_NAME);
				++i;
			} while (cursor.moveToNext());
		}

		this.setEntries(entries);
		this.setEntryValues(values);

		// close the cursor
		cursor.close();
	}

	public void setProviderData(String countryCode) {
		Context context = this.getContext();
		DnsProviderHandler db = new DnsProviderHandler(context);
		Cursor cursor = db.read(countryCode);

		CharSequence[] entries = null;
		CharSequence[] values = null;

		if (cursor == null || !cursor.moveToFirst()) {
			entries = new CharSequence[] {};
			values = new CharSequence[] {};
			Log.w(TAG, "No DB info returned for " + countryCode + ".");
		} else {
			entries = new CharSequence[cursor.getCount()];
			values = new CharSequence[cursor.getCount()];
			int i = 0;
			do {
				values[i] = cursor.getString(DnsProvider.COL_IDX_IP);
				CharSequence entry =  cursor.getString(DnsProvider.COL_IDX_NAME);
				if (entry == null || entry.length() == 0 || entry.equals("null"))
				{
					entry = values[i];
				}
				entries[i] = String.format("%s (%s)", entry,
						cursor.getString(DnsProvider.COL_IDX_CITY));
				++i;
			} while (cursor.moveToNext());
		}

		this.setEntries(entries);
		this.setEntryValues(values);

		// close the cursor
		cursor.close();
	}
}