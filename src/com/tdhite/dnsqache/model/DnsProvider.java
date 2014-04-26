package com.tdhite.dnsqache.model;

public class DnsProvider implements Comparable<DnsProvider>
{
	private String provider;
	private String primary;
	private String secondary;

	DnsProvider(String provider, String primary, String secondary)
	{
		this.setProvider(provider);
		this.setPrimary(primary);
		this.setSecondary(secondary);
	}

	@Override
	public int compareTo(DnsProvider another)
	{
		return this.provider.compareTo(another.provider);
	}

	public String getProvider()
	{
		return provider;
	}

	public void setProvider(String provider)
	{
		this.provider = provider;
	}

	public String getPrimary()
	{
		return primary;
	}

	public void setPrimary(String primary)
	{
		this.primary = primary;
	}

	public String getSecondary()
	{
		return secondary;
	}

	public void setSecondary(String secondary)
	{
		this.secondary = secondary;
	}
}
