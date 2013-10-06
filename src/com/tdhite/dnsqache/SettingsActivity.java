package com.tdhite.dnsqache;

import com.tdhite.dnsqache.system.ConfigManager;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
			this.initializeProxySpinner();
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
			case R.id.spinner_proxy_type:
				this.saveProxySpinnerPosition(parent.getSelectedItemPosition());
				break;
		}
	}

	public void onNothingSelected(AdapterView<?> adapterView)
	{
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		int id = adapterView.getId();
		switch (id)
		{
			case R.id.spinner_proxy_type:
				onNoProxySelected(adapterView, sharedPrefs);
				break;
		}
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
