package com.tdhite.dnsqache.model;

import java.util.TreeSet;

import com.tdhite.dnsqache.R;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.util.Log;

public class DnsProviders extends TreeSet<DnsProvider>
{
    private static final String TAG = "DNSQACHE -> DnsProviders";

	private static final long serialVersionUID = 1L;

	public void fromResources(Context ctx)
	{
		this.clear();
		Resources res = ctx.getResources();
		String[] providers = res.getStringArray(R.array.array_dns_provider_names);
		for (int i = 0; i < providers.length; i++)
		{
			int arrayId = res.getIdentifier(providers[i], "array", ctx.getPackageName());
			try
			{
				String addrs[] = res.getStringArray(arrayId);
				this.add(new DnsProvider(providers[i], addrs[0], addrs[1]));
			}
			catch(NotFoundException e)
			{
				Log.e(TAG, e.getMessage());
			}
		}
	}
	
	public void fromInternet(Context ctx)
	{
	}
}
