package com.tdhite.dnsqache;

import com.tdhite.dnsqache.system.ConfigManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Preferences
{    
	public static final String TAG = "DNSQACHE -> Preferences";

    private Context ctx = null;
    
    //-------------
    // CONSTRUCTOR
    //-------------
    
    public Preferences(Context ctx)
    {
        this.ctx = ctx;
    }
    
    //----------------
    // PUBLIC METHODS
    //----------------
    
    public SharedPreferences getPrefs()
    {
		SharedPreferences sharedPrefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		return sharedPrefs;
    }
    
    public SharedPreferences.Editor getPrefsEditor()
    {
		return getPrefs().edit();
    }
    
    public String getPrefCacheSize()
    {
        return getPrefs().getString(
                   ConfigManager.PREF_DNSMASQ_CACHESIZE,
                   ctx.getResources().getString(R.string.default_cache_size));
    }
    
    public void setPrefCacheSize(String cacheSize)
    {
        SharedPreferences.Editor prefsEditor = getPrefsEditor();
        
        prefsEditor.putString(ConfigManager.PREF_DNSMASQ_CACHESIZE, cacheSize);
        prefsEditor.commit();
    }

    public String getPrefDNSProvider()
    {
        return getPrefs().getString(
                   ConfigManager.PREF_DNS_PROVIDER,
                   ctx.getResources().getString(R.string.default_dns_provider));
    }
    
    public void setPrefDNSProvider(String provider)
    {
        SharedPreferences.Editor prefsEditor = getPrefsEditor();
        
        prefsEditor.putString(ConfigManager.PREF_DNS_PROVIDER, provider);
        prefsEditor.commit();
    }
    
    public int getPrefDNSProviderPosition()
    {
		String provider = getPrefDNSProvider();
		String[] providers = ctx.getResources().getStringArray(
				R.array.array_dns_provider_names);

		for (int pos = 0; pos < providers.length; pos++)
		{
			if (providers[pos].equals(provider))
			{
				return pos;
			}
		}

		return 0;
    }
    
    public String getPrefDNSAddress1()
    {
        return getPrefs().getString(ConfigManager.PREF_DNSMASQ_PRIMARY, "");
    }
    
    public void setPrefDNSAddress1(String address)
    {
		SharedPreferences.Editor prefsEditor = getPrefsEditor();

		prefsEditor.putString(ConfigManager.PREF_DNSMASQ_PRIMARY, address);
		prefsEditor.commit();
    }
    
    public String getPrefDNSAddress2()
    {
		return getPrefs().getString(ConfigManager.PREF_DNSMASQ_SECONDARY, "");
    }
    
    public void setPrefDNSAddress2(String address)
    {
		SharedPreferences.Editor prefsEditor = getPrefsEditor();

		prefsEditor.putString(ConfigManager.PREF_DNSMASQ_SECONDARY, address);
		prefsEditor.commit();
    }
    
    public boolean getPrefActivateOnBoot()
    {
		return getPrefs().getBoolean(
				ConfigManager.PREF_START_ON_BOOT,
				Boolean.valueOf(ctx.getResources().getString(
						R.string.default_activate_on_boot)));
    }
    
    public void setPrefActivateOnBoot(boolean activateOnBoot)
    {
		SharedPreferences.Editor prefsEditor = getPrefsEditor();
		prefsEditor.putBoolean(
				ConfigManager.PREF_START_ON_BOOT, activateOnBoot);
		prefsEditor.commit();
    }

	public void setPrefLogQueries(boolean logQueries)
	{
		SharedPreferences.Editor prefsEditor = getPrefsEditor();
		prefsEditor.putBoolean(
				ConfigManager.PREF_UI_DNS_LOG_QUERIES, logQueries);
		prefsEditor.commit();
	}

	public boolean getPrefLogQueries()
	{
		return getPrefs().getBoolean(
				ConfigManager.PREF_UI_DNS_LOG_QUERIES,
				Boolean.valueOf(ctx.getResources().getString(
						R.string.default_log_queries)));
	}
}
