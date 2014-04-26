package com.tdhite.dnsqache.task;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import com.tdhite.dnsqache.model.DnsProviders;

import android.content.Context;


class DnsProvidersTask extends Task {
	public static final String TAG = "DNSQACHE -> DnsProvidersTask";

	private String dnsProvidersUrl = null;
	private String result = "";

	public DnsProvidersTask(int id, Context context, Callback<Boolean> callback,
			Boolean startInBackground)
	{
		super(id, context, callback, startInBackground);
	}

	public void getResponse()
	{
		HttpClient client = new DefaultHttpClient();    
		String query = dnsProvidersUrl;

		try
		{
			URL url = new URL(query);
			URI uri = new URI(url.getProtocol(), url.getHost(),url.getPath(), url.getQuery(),null);
			HttpGet request = new HttpGet(uri);
			HttpResponse response = client.execute(request);
			result=Userrequest(response); 
		}
		catch(Exception ex)
		{
		}
	}

	public String Userrequest(HttpResponse response)
	{
		try     
		{
			InputStream in = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			StringBuilder str = new StringBuilder();
			String line = null;
			while((line = reader.readLine()) != null)
			{
				str.append(line + "\n");
			}
			in.close();
			result = str.toString();
			updateData(result);         
		}
		catch(Exception ex)
		{
			//responsetxt.setText(ex.getMessage());
		}
		return result;
	}

	public void updateData(String result)
	{
		try     
		{
			JSONObject json = new JSONObject(result);
			JSONArray ja;
			json = json.getJSONObject("responseData");
			ja = json.getJSONArray("results");

			int resultCount = ja.length();

			for (int i =0; i<resultCount; i++)
			{
				JSONObject resultObject = ja.getJSONObject(i);
				result = resultObject.toString();
			}
		}
		catch(Exception ex)
		{
			//responsetxt.setText(ex.getMessage());
		}
	}

	@Override
	protected Boolean doInBackground(Void... params)
	{
		publishProgress("Getting providers from resources ...");
		DnsProviders providers = new DnsProviders();
		providers.fromResources(this.mContext);
		return true;
	}
}
