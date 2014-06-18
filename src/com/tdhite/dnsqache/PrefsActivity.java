package com.tdhite.dnsqache;

import java.util.Set;

import com.tdhite.dnsqache.system.ConfigManager;

import android.app.Activity;
import android.content.SharedPreferences;
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
	private static final String CUSTOM_PROVIDER = "custom";

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
		private static final String TAG = "DNSQACHE -> PrefsFragment";

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
					updateDnsProviders(sharedPreferences, listPref);
				}
				else if (pref.getKey().equals(ConfigManager.PREF_DNSMASQ_CACHESIZE))
				{
					updateDnsCacheSize(sharedPreferences, listPref);
				}
			}
			else if (pref instanceof EditTextPreference)
			{
				// EditPreference
				if (pref.getKey().equals(ConfigManager.PREF_DNSQACHE_CUSTOM_PRIMARY) ||
						pref.getKey().equals(ConfigManager.PREF_DNSQACHE_CUSTOM_SECONDARY))
				{
					ListPreference listPref = (ListPreference) this.findPreference(ConfigManager.PREF_DNS_PROVIDER);
					if (listPref.getValue().equalsIgnoreCase(CUSTOM_PROVIDER)) {
						updateDnsProviders(sharedPreferences, listPref);
					}
				}
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

		private void updateDnsProviders(SharedPreferences prefs, ListPreference listPref)
		{
			Activity ctx = this.getActivity();
			int id = -1;
			String value = listPref.getValue();
			if (!value.equalsIgnoreCase(CUSTOM_PROVIDER)) {
				id = ConfigManager.getResourceId(ctx, value, "array");
			}
			if (id > 0)
			{
				try
				{
					String[] providers = ctx.getResources().getStringArray(id);
					ConfigManager.getConfigManager().updateDNSConfiguration(
							ctx, prefs, providers[0], providers[1], -1);
				}
				catch (Resources.NotFoundException e)
				{
					Log.e(TAG, "updateDnsProviders failed on string array id: " + id, e);
				}
			}
			else
			{
				StringBuilder msg = new StringBuilder();
				msg.append("updateDnsProviders: string array id not found: ");
				msg.append(id);
				msg.append(", trying custom provider as backstop.");
				Log.e(TAG, msg.toString());
				String providers[] = ConfigManager.getConfigManager().getCustomDNSProvider(ctx, prefs);
				ConfigManager.getConfigManager().updateDNSConfiguration(
						ctx, prefs, providers[0], providers[1], -1);
			}
		}

		private void updateDnsCacheSize(SharedPreferences prefs, ListPreference listPref)
		{
			Activity ctx = this.getActivity();
			int size = -1;
			String value = listPref.getValue();

			try
			{
				size = Integer.valueOf(value);
			}
			catch(NumberFormatException e)
			{
				Log.e(TAG, "updateDnsCacheSize: invalid value: " + value, e);
				size = ConfigManager.PREF_DNSMASQ_DEFAULT_CACHE_SIZE;
			}

			ConfigManager.getConfigManager().updateDNSConfiguration(ctx, prefs, null,
					null, size);
		}
	}
}
