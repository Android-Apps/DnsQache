package com.tdhite.dnsqache;

import com.tdhite.dnsqache.system.ConfigManager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class ConfigureDNSActivity extends Activity implements OnItemSelectedListener
{
    // other constants
    public static final String TAG = "DNSQACHE -> MainActivity";

    // activity initialization
    private boolean mInitialized = false;

    // shared preferences
    private Preferences prefs = null;

    //-------
    // DEBUG
    //-------

    @SuppressWarnings("unused")
    private void alert(String msg)
    {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    //----------------
    // PUBLIC METHODS
    //----------------

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_configure);

        // show the 'Up' button in the action bar
        setupActionBar();

        if (this.prefs == null)
        {
            // shared preferences
            this.prefs = new Preferences((QacheApplication)this.getApplication());
        }

        if (!mInitialized)
        {
            // initialize the configuration fields
            initializeCacheSize();
            initializeDNSProvider();
            initializeActivateOnBoot();
            initializeLogQueries();

            // activity is initialized
            mInitialized = true;
        }

        if (savedInstanceState != null)
        {
            // restore instance state
            restoreState(savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        // save instance state
        saveState(outState);
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
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.configure, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        if (hasFocus)
        {
        }
    }

    /**
     * The DNS spinner view handler invoked when the spinner has no selected item.
     * 
     * @param parent    The spinner view.
     */
    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {
        ((Spinner)parent).setSelection(prefs.getPrefDNSProviderPosition());
    }

    /**
     * The DNS spinner handler invoked when an item has been selected.
     * 
     * @param parent    The spinner view.
     * @param view      The item that was clicked.
     * @param pos       The position of the item in the adapter.
     * @param id        The row ID of the selected item.
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id)
    {
        String provider = (String)parent.getSelectedItem();
        int providerId = getProviderId(provider);
        ViewGroup group = (ViewGroup)findViewById(R.id.group_name_servers);
        EditText editText1 = (EditText)findViewById(R.id.proxy_allowed_cidrs);
        EditText editText2 = (EditText)findViewById(R.id.name_server_2);

        if (providerId == 0)  // other (specify IP address)
        {
            // called during initialization (pos < 0) or when item was selected
            String text1 = pos < 0 ? this.prefs.getPrefDNSAddress1() : "";
            String text2 = pos < 0 ? this.prefs.getPrefDNSAddress2() : "";

            // remove background
            group.setBackgroundResource(R.drawable.border_1);

            editText1.setText(text1);
            editText1.setFocusable(true);
            editText1.setFocusableInTouchMode(true);

            if (mInitialized && editText1.requestFocus()) 
            {
                ((InputMethodManager)getSystemService(
                    Context.INPUT_METHOD_SERVICE)).showSoftInput(editText1, 0);            
            }

            editText2.setText(text2);
            editText2.setFocusable(true);
            editText2.setFocusableInTouchMode(true);
        }
        else  // DNS provider selected
        {
            // set background for read-only mode
            group.setBackgroundResource(R.drawable.border_with_fill_1);

            String[] providersIps = this.getResources().getStringArray(providerId);

            editText1.setText(providersIps[0]);
            editText1.setFocusable(false);

            editText2.setText(providersIps[1]);
            editText2.setFocusable(false);
        }
    }

    /**
     * Handler for ACTIVATE ON BOOT checkbox event. 
     * 
     * @param view    The ACTIVATE ON BOOT checkbox.
     */
    public void onClickActivateOnBoot(View view)
    {
        setActivateOnBoot(((CheckBox)view).isChecked());
    }

    /**
     * Handler for ACTIVATE ON BOOT checkbox event. 
     * 
     * @param view    The ACTIVATE ON BOOT checkbox.
     */
    public void onClickLogQueries(View view)
    {
        setLogQueries(((CheckBox)view).isChecked());
    }

    /**
     * Event handler for resetting (clearing) the cache.
     * 
     * @param view    The 'RESET' button.
     */
    public void onClickReset(View view)
    {
        ContextThemeWrapper ctw = new ContextThemeWrapper(this, R.style.AlertDialogTheme);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ctw);

        alertDialogBuilder.setTitle(R.string.reset);
        alertDialogBuilder.setMessage(R.string.resetMsg);
        alertDialogBuilder.setCancelable(false);

        alertDialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) 
            {
                // reset the DNSQache configuration to default
                resetConfiguration();
            }
        });

        alertDialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) 
            {
                dialog.cancel();
            }
        });
 
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    /**
     * Handler for OK button event. 
     * 
     * @param view    The OK button.
     */
    public void onClickOK(View view)
    {
        EditText editCacheSize = (EditText)findViewById(R.id.edit_cache_size);
        Spinner  spinnerProvider = (Spinner)findViewById(R.id.spinner_dns_providers);
        EditText editAddress1 = (EditText)findViewById(R.id.proxy_allowed_cidrs);
        EditText editAddress2 = (EditText)findViewById(R.id.name_server_2);
        CheckBox checkActivateOnBoot = (CheckBox)findViewById(R.id.checkbox_activateonboot);
        CheckBox checkLogQueries = (CheckBox)findViewById(R.id.checkbox_log_queries);

        try
        {
            String cacheSize = this.getTextFromEdit(editCacheSize, false, "Cache size not specified");
            String provider = (String)spinnerProvider.getSelectedItem();
            String address1 = this.getTextFromEdit(editAddress1, false, "IP address required");
            String address2 = this.getTextFromEdit(editAddress2, false, "IP address required");
            boolean activateOnBoot = checkActivateOnBoot.isChecked();
            boolean logQueries = checkLogQueries.isChecked();

            // update the preference settings
            prefs.setPrefCacheSize(cacheSize);
            prefs.setPrefDNSProvider(provider);
            prefs.setPrefDNSAddress1(address1);
            prefs.setPrefDNSAddress2(address2);
            prefs.setPrefActivateOnBoot(activateOnBoot);
            prefs.setPrefLogQueries(logQueries);

            // update the DNSQache configuration
            updateConfiguration();

            // return
            this.finish();
        }
        catch (Exception e)
        {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            Log.d(TAG, "Exception writing options: " + e.toString());
        }
    }

    //-----------------
    // PRIVATE METHODS
    //-----------------

    /**
     * Initialize the CACHE SIZE field to the current preference setting.
     */
    private void initializeCacheSize()
    {
        String cacheSize = prefs.getPrefCacheSize();
        
        ((EditText)findViewById(R.id.edit_cache_size)).setText(cacheSize);
    }

    /**
     * Initialize the DOMAIN NAME SERVER and associated address 
     * fields to the current preference settings.
     */
    private void initializeDNSProvider()
    {
        final Spinner spinner = (Spinner)findViewById(R.id.spinner_dns_providers);
        final OnItemSelectedListener listener = this;

        // set the selection change handler but prevent listener 
        // from firing on the newly instantiated spinner control
        spinner.post(new Runnable() {
            
            public void run() 
            {
                spinner.setOnItemSelectedListener(listener);
            }
        });

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            this, R.array.array_dns_provider_names,
            android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        // set the spinner selection to the current shared preference setting
        onNothingSelected(spinner);

        // process the spinner selection and set the DNS address fields
        onItemSelected(spinner, null, -1, -1);
    }

    /**
     * Initialize the ACTIVATE ON BOOT checkbox to
     * the preference setting.
     */
    private void initializeActivateOnBoot()
    {
        setActivateOnBoot(prefs.getPrefActivateOnBoot());
    }

    /**
     * Initialize the LOG QUERIES checkbox to
     * the preference setting.
     */
    private void initializeLogQueries()
    {
        setLogQueries(prefs.getPrefLogQueries());
    }

    /**
     * Returns the ID associated with the specified DNS provider.
     * 
     * @param provider    The DNS provider.
    * @return             The provider ID (or 0 if no matching provider specified).
     */
    int getProviderId(String provider)
    {
        return this.getResources().getIdentifier(provider, "array",  this.getPackageName());
    }

    /**
     * Returns the text container in the specified EditText view.
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
     * Checks or unchecks ACTIVATE ON BOOT.
     * 
     * @param activateOnBoot    True to check; false to uncheck.
     */
    private void setActivateOnBoot(boolean activateOnBoot)
    {
        ((CheckBox)findViewById(R.id.checkbox_activateonboot)).setChecked(activateOnBoot);
    }

    /**
     * Checks or unchecks LOG QUERIES.
     * 
     * @param logQueries    True to check; false to uncheck.
     */
    private void setLogQueries(boolean logQueries)
    {
        ((CheckBox)findViewById(R.id.checkbox_log_queries)).setChecked(logQueries);
    }

    /**
     * Returns the position in the spinner control for the specified DNS provider.
     * 
     * @param provider    The DNS provider name.
     * @return            The (zero-based) position of the provider name in the spinner control.
     */
    private int getDNSProviderPosition(String provider)
    {
        QacheApplication app = (QacheApplication)this.getApplication();
        String[] providers = app.getResources().getStringArray(R.array.array_dns_provider_names);
        
        for (int pos = 0; pos < providers.length; pos++)
        {
            if (providers[pos].equals(provider))
            {
                return pos;
            }
        }
        
        return 0;
    }

    /**
     * Restores the instance state.
     * 
     * @param savedInstanceState    The bundle to retrieve the instance state.
     */
    private void restoreState(Bundle savedInstanceState)
    {
        String cacheSize = savedInstanceState.getString(ConfigManager.PREF_DNSMASQ_CACHESIZE);
        String provider = savedInstanceState.getString(ConfigManager.PREF_DNS_PROVIDER);
        String dns1 = savedInstanceState.getString(ConfigManager.PREF_DNSMASQ_PRIMARY);
        String dns2 = savedInstanceState.getString(ConfigManager.PREF_DNSMASQ_SECONDARY);
        boolean activateOnBoot = savedInstanceState.getBoolean(ConfigManager.PREF_START_ON_BOOT);
        boolean logQueries = savedInstanceState.getBoolean(ConfigManager.PREF_DNSMASQ_LOG_QUERIES);

        // reset the configuration
        ((EditText)findViewById(R.id.edit_cache_size)).setText(cacheSize);
        ((EditText)findViewById(R.id.edit_cache_size)).setText(cacheSize);
        ((Spinner)findViewById(R.id.spinner_dns_providers)).setSelection(getDNSProviderPosition(provider));
        ((EditText)findViewById(R.id.proxy_allowed_cidrs)).setText(dns1);
        ((EditText)findViewById(R.id.name_server_2)).setText(dns2);
        ((CheckBox)findViewById(R.id.checkbox_activateonboot)).setChecked(activateOnBoot);
        ((CheckBox)findViewById(R.id.checkbox_log_queries)).setChecked(logQueries);
    }

    /**
     * Saves the state of the instance.
     * 
     * @param outState    The bundle to store the instance state.
     */
    private void saveState(Bundle outState)
    {
        EditText editCacheSize = (EditText)findViewById(R.id.edit_cache_size);
        Spinner  spinnerProvider = (Spinner)findViewById(R.id.spinner_dns_providers);
        EditText editAddress1 = (EditText)findViewById(R.id.proxy_allowed_cidrs);
        EditText editAddress2 = (EditText)findViewById(R.id.name_server_2);
        CheckBox checkActivateOnBoot = (CheckBox)findViewById(R.id.checkbox_activateonboot);
        CheckBox checkLogQueries = (CheckBox)findViewById(R.id.checkbox_log_queries);

        String cacheSize = editCacheSize.getText().toString();
        String provider = (String)spinnerProvider.getSelectedItem();
        String dns1 = editAddress1.getText().toString();
        String dns2 = editAddress2.getText().toString();
        boolean activateOnBoot = checkActivateOnBoot.isChecked();
        boolean logQueries = checkLogQueries.isChecked();
        
        outState.putString(ConfigManager.PREF_DNSMASQ_CACHESIZE, cacheSize);
        outState.putString(ConfigManager.PREF_DNS_PROVIDER, provider);
        outState.putString(ConfigManager.PREF_DNSMASQ_PRIMARY, dns1);
        outState.putString(ConfigManager.PREF_DNSMASQ_SECONDARY, dns2);
        outState.putBoolean(ConfigManager.PREF_START_ON_BOOT, activateOnBoot);
        outState.putBoolean(ConfigManager.PREF_DNSMASQ_LOG_QUERIES, logQueries);
    }

    /**
     * Updates the DNSQache configuration using the following settings: 
     * 
     *   1) DNS IP address 1
     *   2) DNS IP address 2
     *   3) Cache size
     */
    private void updateConfiguration()
    {
        String cacheSize = prefs.getPrefCacheSize();
        String address1 = prefs.getPrefDNSAddress1();
        String address2 = prefs.getPrefDNSAddress2();

        ((QacheApplication)this.getApplication()).updateDNSConfiguration(
            address1, address2, Integer.valueOf(cacheSize));
    }

    /**
     * Resets the configuration back to default.
     */
    private void resetConfiguration()
    {
        Resources res = this.getResources();

        // reset the shared preferences
        prefs.setPrefCacheSize(res.getString(R.string.default_cache_size));
        prefs.setPrefDNSProvider(res.getString(R.string.default_dns_provider));
        prefs.setPrefDNSAddress1(res.getString(R.string.default_name_server1));
        prefs.setPrefDNSAddress2(res.getString(R.string.default_name_server2));
        prefs.setPrefActivateOnBoot(Boolean.valueOf(res.getString(R.string.default_activate_on_boot)));
        prefs.setPrefLogQueries(Boolean.valueOf(res.getString(R.string.default_log_queries)));

        // reset the configuration
        ((EditText)findViewById(R.id.edit_cache_size)).setText(prefs.getPrefCacheSize());
        ((Spinner)findViewById(R.id.spinner_dns_providers)).setSelection(prefs.getPrefDNSProviderPosition());
        ((EditText)findViewById(R.id.proxy_allowed_cidrs)).setText(prefs.getPrefDNSAddress1());
        ((EditText)findViewById(R.id.name_server_2)).setText(prefs.getPrefDNSAddress2());
        ((CheckBox)findViewById(R.id.checkbox_activateonboot)).setChecked(prefs.getPrefActivateOnBoot());
        ((CheckBox)findViewById(R.id.checkbox_log_queries)).setChecked(prefs.getPrefLogQueries());
    }
}