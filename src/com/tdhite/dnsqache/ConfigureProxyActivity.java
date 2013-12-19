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

import java.util.HashMap;

import com.tdhite.dnsqache.system.ConfigManager;

import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class ConfigureProxyActivity extends Activity
	implements OnItemSelectedListener
{
	public static final String TAG = "DNSQACHE -> SettingsActivity";

    // shared preferences
    private Preferences prefs = null;

	private boolean mInitialized = false;

    /**
     * Returns the text contained in the specified EditText view.
     * 
     * @param edit              The EditText view.
     * @param emptyOK           If false, exception is thrown if view is empty.
     * @param exceptionMsg      The message to generate if an exception is thrown.
     * @return                  The text string contained in the EditText view.
     * @throws Exception        Exception thrown if field is empty and 'emptyOK' is false.
     */
    private String getTextFromEdit(EditText edit, boolean emptyOK, String exceptionMsg) throws Exception
    {
        if (!emptyOK && edit.getText().length() == 0)
        {
            throw new Exception(exceptionMsg);
        }
        else
        {
            return edit.getText().toString();
        }
    }

   /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
        if (this.prefs == null)
        {
            // shared preferences
            this.prefs = new Preferences((QacheApplication)this.getApplication());
        }

		setContentView(R.layout.activity_settings);
	}

	@Override
	public void onWindowFocusChanged(boolean focused)
	{
		if (focused && !mInitialized)
		{
			this.initializeProxySpinner();
			this.initializeProxyCIDRs();
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

	private void initializeProxyCIDRs()
	{		
        String cidrs = prefs.getPrefProxyCIDRs();   
        ((EditText)findViewById(R.id.proxy_allowed_cidrs)).setText(cidrs);
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

    public void onClickUpdateCIDRs(View view)
    {
    	try
    	{
    		long startStamp = System.currentTimeMillis();
            String cidrs = this.getTextFromEdit((EditText)findViewById(R.id.proxy_allowed_cidrs),
            		false, "Invalid CIDR set specified!");

            // Set the value into the shared preferences file
            prefs.setPrefPolipoCIDRs(cidrs);

            // Set the config manager with the values
            QacheApplication app = (QacheApplication)this.getApplication();
            ConfigManager config = app.getConfigManager();
            HashMap<String, String> polipoMap = config.getOptionsMap(ConfigManager.MAP_POLIPO_OPTS);
            polipoMap.put(ConfigManager.PREF_POLIPO_ALLOWED_CIDRS, cidrs);

    		if (config.commit(this))
    		{
    			Log.d(TAG,
    					"Creation of configuration-files took ==> "
    							+ (System.currentTimeMillis() - startStamp)
    							+ " milliseconds.");
    			QacheService svc = QacheService.getSingleton();
    			if (svc != null)
    			{
    				svc.setDns();
    			}
    		}
    		else
    		{
    			Log.e(TAG, "Unable to update configuration preferences!");
    		}
            // return
            this.finish();
        }
        catch (Exception e)
        {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            Log.d(TAG, "Exception writing proxy CIDR option: " + e.toString());
        }
    }
}
