package com.tdhite.dnsqache;

import com.tdhite.dnsqache.system.ConfigManager;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class SettingsActivity extends Activity implements
		OnItemSelectedListener
{
	public static final String TAG = "DNSQACHE -> SettingsActivity";

	private boolean mInitialized = false;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
	}

	@Override
	public void onWindowFocusChanged(boolean focused)
	{
		if (focused && !mInitialized)
		{
			// Set the toggle button based on the system property
			// dnsqache.status
			EditText editText = (EditText) findViewById(R.id.edit_cache_size);
			this.setTextField(editText, ConfigManager.PREF_DNSMASQ_CACHESIZE,
					R.string.default_cache_size);

			this.initializeDNSProviderSpinner();
			this.initializeProxySpinner();
			this.setActivateOnBoot(false);
			this.setLogQueries(false);

			mInitialized = true;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}

	// This method is called at button click because we assigned the name to the
	// "OnClick property" of the button
	public void onClick(View view)
	{
		switch (view.getId())
		{
			case R.id.button_set_name_servers:
				setNamServers();
				break;
		}
	}

	public void onCheckboxClicked(View view)
	{
		// Is the view now checked?
		boolean checked = ((CheckBox) view).isChecked();

		// Check which checkbox was clicked
		switch (view.getId())
		{
			case R.id.btn_activateonboot:
				setActivateOnBoot(checked);
				break;
			case R.id.btn_log_queries:
				setLogQueries(checked);
				break;
		}
	}

	private void setLogQueries(boolean logQueries)
	{
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		Editor prefsEditor = sharedPrefs.edit();

		if (!mInitialized)
		{
			CheckBox checkBox = (CheckBox) findViewById(R.id.btn_log_queries);
			logQueries = sharedPrefs.getBoolean(
					ConfigManager.PREF_UI_DNS_LOG_QUERIES, logQueries);
			checkBox.setChecked(logQueries);
		}

		prefsEditor.putBoolean(ConfigManager.PREF_UI_DNS_LOG_QUERIES,
				logQueries);
		prefsEditor.commit();
	}

	private String getTextFromEdit(EditText text, boolean emptyOK)
			throws Exception
	{
		if (!emptyOK && text.getText().length() == 0)
		{
			throw new Exception("Invalid IP address");
		}
		else
		{
			return text.getText().toString();
		}
	}

	private void setNamServers()
	{
		EditText textServer1 = (EditText) findViewById(R.id.name_server1);
		EditText textServer2 = (EditText) findViewById(R.id.name_server2);
		EditText textCacheSize = (EditText) findViewById(R.id.edit_cache_size);
		try
		{
			String server1 = this.getTextFromEdit(textServer1, false);
			String server2 = this.getTextFromEdit(textServer2, false);
			String cacheSize = this.getTextFromEdit(textCacheSize, false);

			this.setNameServers(server1, server2, cacheSize);
		}
		catch (Exception e)
		{
			Toast.makeText(this, "Please enter only valid IP addresses.",
					Toast.LENGTH_LONG).show();
			Log.d(TAG, "Exception writing options: " + e.toString());
		}
	}

	private void setNameServers(String server1, String server2, String cacheSize)
	{
		QacheApplication app = (QacheApplication) this.getApplication();
		app.updateDNSConfiguration(server1, server2, Integer.valueOf(cacheSize));
	}

	private void setTextField(EditText textField, String sharedPrefKey,
			int defaultValueId)
	{
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		String value = sharedPrefs.getString(sharedPrefKey,
				this.getString(defaultValueId));
		textField.setText(value);
	}

	private void setActivateOnBoot(boolean activateOnBoot)
	{
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		Editor prefsEditor = sharedPrefs.edit();

		if (!mInitialized)
		{
			CheckBox btnActivateOnboot = (CheckBox) findViewById(R.id.btn_activateonboot);
			activateOnBoot = sharedPrefs.getBoolean(
					ConfigManager.PREF_START_ON_BOOT, activateOnBoot);
			btnActivateOnboot.setChecked(activateOnBoot);
		}

		prefsEditor
				.putBoolean(ConfigManager.PREF_START_ON_BOOT, activateOnBoot);
		prefsEditor.commit();
	}

	private void initializeDNSProviderSpinner()
	{
		Spinner spinner = (Spinner) findViewById(R.id.spinner_dns_providers);

		// register this activity as the callback
		// note: this activity implements OnItemSelectedListener
		spinner.setOnItemSelectedListener(this);

		// Create an ArrayAdapter using the string array and a default spinner
		// layout
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.array_dns_provider_names,
				android.R.layout.simple_spinner_item);

		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);

		onNothingSelected(spinner);
	}

	private void initializeProxySpinner()
	{
		Spinner spinner = (Spinner) findViewById(R.id.spinner_proxy_type);

		// register this activity as the callback
		// note: this activity implements OnItemSelectedListener
		spinner.setOnItemSelectedListener(this);

		// Create an ArrayAdapter using the string array and a default spinner
		// layout
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.array_proxy_apps,
				android.R.layout.simple_spinner_item);

		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);

		onNothingSelected(spinner);
	}

	private void saveDNSSpinnerPosition(int position)
	{
		QacheApplication app = (QacheApplication) this.getApplication();
		Editor prefsEditor = app.getSettingsEditor();
		prefsEditor.putInt(ConfigManager.PREF_UI_DNS_PROVIDER_POSITION,
				position);
		prefsEditor.commit();
	}

	private void saveProxySpinnerPosition(int position)
	{
		QacheApplication app = (QacheApplication) this.getApplication();
		Editor prefsEditor = app.getSettingsEditor();
		prefsEditor.putInt(ConfigManager.PREF_UI_PROXY_SPINNER_POSITION,
				position);
		prefsEditor.commit();
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long arg3)
	{
		int id = parent.getId();
		switch (id)
		{
			case R.id.spinner_dns_providers:
				onDNSProviderItemSelected(parent, view, pos);
				break;
			case R.id.spinner_proxy_type:
				this.saveProxySpinnerPosition(parent.getSelectedItemPosition());
				break;
		}
	}

	private void onDNSProviderItemSelected(AdapterView<?> parent, View view,
			int pos)
	{
		Resources res = this.getResources();
		String providerName = (String) parent.getSelectedItem();
		int providerId = res.getIdentifier(providerName, "array",
				this.getPackageName());

		if (providerId == 0)
		{
			Toast.makeText(this, "Failed to find id for " + providerName,
					Toast.LENGTH_LONG).show();
		}
		else
		{
			String[] providersIps = res.getStringArray(providerId);

			EditText editText = (EditText) findViewById(R.id.name_server1);
			editText.setText(providersIps[0]);
			editText = (EditText) findViewById(R.id.name_server2);
			editText.setText(providersIps[1]);

			this.saveDNSSpinnerPosition(parent.getSelectedItemPosition());
		}
	}

	public void onNothingSelected(AdapterView<?> adapterView)
	{
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		int id = adapterView.getId();
		switch (id)
		{
			case R.id.spinner_dns_providers:
				onNoDNSProviderSelected(adapterView, sharedPrefs);
				break;
			case R.id.spinner_proxy_type:
				onNoProxySelected(adapterView, sharedPrefs);
				break;
		}
	}

	private void onNoDNSProviderSelected(AdapterView<?> adapterView,
			SharedPreferences sharedPrefs)
	{
		Spinner spinner = (Spinner) adapterView;
		int preferredPosition = sharedPrefs.getInt(
				ConfigManager.PREF_UI_DNS_PROVIDER_POSITION,
				ConfigManager.DNS_DEFAULT_SPINNER_POSITION);
		spinner.setSelection(preferredPosition);
		this.saveDNSSpinnerPosition(preferredPosition);
	}

	private void onNoProxySelected(AdapterView<?> adapterView,
			SharedPreferences sharedPrefs)
	{
		Spinner spinner = (Spinner) adapterView;
		int preferredPosition = sharedPrefs.getInt(
				ConfigManager.PREF_UI_PROXY_SPINNER_POSITION,
				ConfigManager.PROXY_DEFAULT_SPINNER_POSITION);
		spinner.setSelection(preferredPosition);
		this.saveProxySpinnerPosition(preferredPosition);
	}
}
