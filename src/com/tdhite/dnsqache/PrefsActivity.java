package com.tdhite.dnsqache;

import java.util.Set;

import com.tdhite.dnsqache.system.ConfigManager;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.util.Log;

public class PrefsActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Display the fragment as the main content.
		getFragmentManager().beginTransaction()
				.replace(android.R.id.content, new PrefsFragment()).commit();
	}

	public static class PrefsFragment extends PreferenceFragment implements
			OnSharedPreferenceChangeListener
	{
		private static final String TAG = "DNSQACHE -> ConfigManager";

		@Override
		public void onCreate(Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);

			// Load the preferences from an XML resource
			addPreferencesFromResource(R.xml.preferences);

			// set texts correctly
			onSharedPreferenceChanged(null, "");
		}

		@Override
		public void onResume()
		{
			super.onResume();
			// Set up a listener whenever a key changes
			getPreferenceScreen().getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(this);
			initSummary();
		}

		@Override
		public void onPause()
		{
			super.onPause();
			// Set up a listener whenever a key changes
			getPreferenceScreen().getSharedPreferences()
					.unregisterOnSharedPreferenceChangeListener(this);
		}

		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key)
		{
			//update summary  
		   updatePrefsSummary(sharedPreferences, findPreference(key));
		}

		/**
		 * Init summary
		 */
		protected void initSummary()
		{
			for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++)
			{
				initPrefsSummary(getPreferenceManager().getSharedPreferences(),
						getPreferenceScreen().getPreference(i));
			}
		}

		/**
		 * Init single Preference
		 * 
		 * @param sharedPreferences
		 * @param pref
		 */
		protected void initPrefsSummary(SharedPreferences sharedPreferences,
				Preference pref)
		{
			if (pref instanceof PreferenceCategory)
			{
				PreferenceCategory pCat = (PreferenceCategory) pref;
				for (int i = 0; i < pCat.getPreferenceCount(); i++)
				{
					initPrefsSummary(sharedPreferences, pCat.getPreference(i));
				}
			}
			else
			{
				updatePrefsSummary(sharedPreferences, pref);
			}
		}

		/**
		 * Update summary
		 * 
		 * @param sharedPreferences
		 * @param pref
		 */
		protected void updatePrefsSummary(SharedPreferences sharedPreferences,
				Preference pref)
		{
			if (pref == null)
				return;

			if (pref instanceof ListPreference)
			{
				// List Preference
				ListPreference listPref = (ListPreference) pref;
				listPref.setSummary(listPref.getEntry());

				if (pref.getKey().equals(ConfigManager.PREF_DNS_PROVIDER))
				{
					updateDnsProviders(listPref);
				}
				else if (pref.getKey().equals(ConfigManager.PREF_DNSMASQ_CACHESIZE))
				{
					updateDnsCacheSize(listPref);
				}
			}
			else if (pref instanceof EditTextPreference)
			{
				// EditPreference
				EditTextPreference editTextPref = (EditTextPreference) pref;
				editTextPref.setSummary(editTextPref.getText());
			}
			else if (pref instanceof MultiSelectListPreference)
			{
				// MultiSelectList Preference
				MultiSelectListPreference mlistPref = (MultiSelectListPreference) pref;
				String summaryMListPref = "";
				String and = "";

				// Retrieve values
				Set<String> values = mlistPref.getValues();
				for (String value : values)
				{
					// For each value retrieve index
					int index = mlistPref.findIndexOfValue(value);
					// Retrieve entry from index
					CharSequence mEntry = index >= 0
							&& mlistPref.getEntries() != null ? mlistPref
							.getEntries()[index] : null;
					if (mEntry != null)
					{
						// add summary
						summaryMListPref = summaryMListPref + and + mEntry;
						and = ";";
					}
				}
				// set summary
				mlistPref.setSummary(summaryMListPref);
			}
		}
		
		private void updateDnsProviders(ListPreference listPref)
		{
			Activity ctx = this.getActivity();
			int id = ConfigManager.getResourceId(ctx, listPref.getValue(),
					"array");
			if (id > 0)
			{
				try
				{
					String[] providers = ctx.getResources().getStringArray(id);
					Editor editor = this.getPreferenceManager()
							.getSharedPreferences().edit();
					editor.putString(ConfigManager.PREF_DNSMASQ_PRIMARY,
							providers[0]);
					editor.putString(ConfigManager.PREF_DNSMASQ_SECONDARY,
							providers[1]);
					editor.commit();
					ConfigManager.getConfigManager().updateDNSConfiguration(
							ctx, providers[0], providers[1], -1);
				}
				catch (Resources.NotFoundException e)
				{
					Log.e(TAG, "updateDnsProviders trying id: " + id, e);
				}
			}
			else
			{
				Log.e(TAG, "updateDnsProviders: id was " + id);
			}
		}

		private void updateDnsCacheSize(ListPreference listPref)
		{
			Activity ctx = this.getActivity();
			int size = -1;
			String value = listPref.getValue();
			Editor editor = this.getPreferenceManager()
					.getSharedPreferences().edit();

			try
			{
				size = Integer.valueOf(value);
			}
			catch(NumberFormatException e)
			{
				Log.e(TAG, "updateDnsCacheSize: value was " + value, e);
				size = ConfigManager.PREF_DNSMASQ_DEFAULT_CACHE_SIZE;
			}

			editor.putString(ConfigManager.PREF_DNSMASQ_CACHESIZE, "" + size);
			editor.commit();
			ConfigManager.getConfigManager().updateDNSConfiguration(ctx, null,
					null, size);
		}
	}
}
