package com.tdhite.dnsqache.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import com.tdhite.dnsqache.db.Constants;

import android.database.Cursor;
import android.util.Log;

/**
 * DnsProvider
 *
 * @author tdhite
 *
 * A sample entry from the dfault providers is as follows:
 * 
 * {
 *   "checked_at":"2014-08-10T16:25:17+02:00",
 *   "city":"",
 *   "country_id":"CZ",
 *   "created_at":"2013-11-04T07:39:42+01:00",
 *   "error":null,
 *   "id":44282,
 *   "ip":"2001:1488:800:400::130",
 *   "name":"dnsres1.nic.cz",
 *   "state":"valid",
 *   "state_changed_at":"2014-07-26T10:44:12+02:00",
 *   "updated_at":"2014-07-26T10:44:12+02:00",
 *   "version":null
 * }
 */
public class DnsProvider
{
	private static final String TAG = "DnsProvider";

	private static final String CHECKED_AT = "checkedAt";
	private static final String CITY = "city";
	private static final String COUNTRY_ID = "country_id";
	private static final String CREATED_AT = "created_at";
	private static final String ERROR = "error";
	private static final String ID = "id";
	private static final String IP = "ip";
	private static final String NAME = "name";
	private static final String STATE = "state";
	private static final String STATE_CHANGED_AT = "state_changed_at";
	private static final String UPDATED_AT = "updated_at";
	private static final String VERSION = "version";

	private static final String US = "US";
	private static final String VALID= "valid";

	public static final String COL_ID = "id";
	public static final String COL_CITY = "city";
	public static final String COL_COUNTRY_ID = "countryId";
	public static final String COL_NAME = "name";
	public static final String COL_IP = "ip";
	public static final String COL_STATE = "state";
	public static final String COL_ERROR = "error";
	public static final String COL_VERSION = "version";
	public static final String COL_CREATED_AT = "createdAt";
	public static final String COL_STATE_CHANGED_AT = "stateChangedAt";
	public static final String COL_UPDATED_AT = "updatedAt";
	public static final String COL_CHECKED_AT = "checked_at";
	public static final String STATE_VALID = "valid";

	private int  id;
	private String city;
	private String countryId;
	private String name;
	private String ip;
	private String state;
	private String error;
	private String version;
	private String createdAt;
	private String stateChangedAt;
	private String updatedAt;
	private String checkedAt;

	public static final int COL_IDX_CITY = 1;
	public static final int COL_IDX_NAME = 3;
	public static final int COL_IDX_IP = 4;

	protected DnsProvider() {}

	DnsProvider(String name, String nsAddr, int id)
	{
		this.setId(0);
		this.setCity(Constants.EMPTY_STRING);
		this.setCountryId(US);
		this.setName(name);
		this.setIp(nsAddr);
		this.setState(VALID);
		this.setError(Constants.EMPTY_STRING);
		this.setVersion(Constants.EMPTY_STRING);
		this.setCreatedAt(Constants.EMPTY_STRING);
		this.setStateChangedAt(Constants.EMPTY_STRING);
		this.setUpdatedAt(Constants.EMPTY_STRING);
		this.setCheckedAt(Constants.EMPTY_STRING);
	}

	DnsProvider(JSONObject nameserver)
	{
		this.setId(nameserver);
		this.setCity(nameserver);
		this.setCountryId(nameserver);
		this.setName(nameserver);
		this.setIp(nameserver);
		this.setState(nameserver);
		this.setError(nameserver);
		this.setVersion(nameserver);
		this.setCreatedAt(nameserver);
		this.setStateChangedAt(nameserver);
		this.setUpdatedAt(nameserver);
		this.setCheckedAt(nameserver);
	}

	public DnsProvider(Cursor cursor)
	{
		this.setId(Integer.parseInt(cursor.getString(0)));
		this.setCity(cursor.getString(1));
		this.setCountryId(cursor.getString(2));
		this.setName(cursor.getString(3));
		this.setIp(cursor.getString(4));
		this.setState(cursor.getString(5));
		this.setError(cursor.getString(6));
		this.setVersion(cursor.getString(7));
		this.setCreatedAt(cursor.getString(8));
		this.setStateChangedAt(cursor.getString(9));
		this.setUpdatedAt(cursor.getString(10));
		this.setCheckedAt(cursor.getString(11));
	}

	private static String jsonStringValue(JSONObject nameserver, String mapname)
	{
		String value = null;
		if (nameserver.has(mapname))
		{
			try
			{
				value = nameserver.getString(mapname);
			}
			catch (JSONException e)
			{
				Log.e(TAG, e.getMessage());
				value = Constants.EMPTY_STRING;
			}
		}
		return value;
	}

	private static Date jsonDateValue(JSONObject nameserver, String mapname)
	{
		Date date = null;
		if (nameserver.has(mapname))
		{
			try
			{
				// Sample: 2014-07-26T15:41:26+02:00
				String dateString = nameserver.getString(mapname);
				SimpleDateFormat sdf = new SimpleDateFormat(
						"yyyy-MM-dd'T'HH:mm:ssZ");
				sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
				date = sdf.parse(dateString);
			}
			catch (JSONException e)
			{
				Log.e(TAG, e.getMessage());
				date = null;
			}
			catch (ParseException e)
			{
				Log.e(TAG, "Date parse failure", e);
				e.printStackTrace();
			}
		}
		return date;
	}

	private static int jsonIntValue(JSONObject nameserver, String mapname)
	{
		int value = -1;
		if (nameserver.has(mapname))
		{
			try
			{
				value = nameserver.getInt(mapname);
			}
			catch (JSONException e)
			{
				Log.e(TAG, e.getMessage());
				value = -1;
			}
		}
		return value;
	}

	// Getters and Setters follow
	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public void setId(JSONObject nameserver)
	{
		this.id = DnsProvider.jsonIntValue(nameserver, ID);
	}


	public String getCity()
	{
		return city;
	}

	public void setCity(String city)
	{
		if (city == null || city.equals("null"))
		{
			city = Constants.EMPTY_STRING;
		}
		this.city = city;
	}

	public void setCity(JSONObject nameserver)
	{
		this.city = DnsProvider.jsonStringValue(nameserver, CITY);
	}

	public String getCountryId()
	{
		return countryId;
	}

	public void setCountryId(String countryId)
	{
		this.countryId = countryId;
	}

	public void setCountryId(JSONObject nameserver)
	{
		this.countryId = DnsProvider.jsonStringValue(nameserver, COUNTRY_ID);
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		if (name == null || name.equals("null"))
		{
			name = Constants.EMPTY_STRING;
		}
		this.name = name;
	}

	public void setName(JSONObject nameserver)
	{
		this.name = DnsProvider.jsonStringValue(nameserver, NAME);
	}

	public String getIp()
	{
		return ip;
	}

	public void setIp(String ip)
	{
		this.ip = ip;
	}

	public void setIp(JSONObject nameserver)
	{
		this.ip = DnsProvider.jsonStringValue(nameserver, IP);
	}

	public String getState()
	{
		return state;
	}

	public void setState(String state)
	{
		this.state = state;
	}

	public void setState(JSONObject nameserver)
	{
		this.state = jsonStringValue(nameserver, STATE);
	}

	public String getError()
	{
		return error;
	}

	public void setError(String error)
	{
		this.error = error;
	}

	public void setError(JSONObject nameserver)
	{
		this.error = jsonStringValue(nameserver, ERROR);
	}

	public String getVersion()
	{
		return version;
	}

	public void setVersion(String version)
	{
		this.version = version;
	}

	public void setVersion(JSONObject nameserver)
	{
		this.version = jsonStringValue(nameserver, VERSION);
	}

	public String getCreatedAt()
	{
		return createdAt;
	}

	public void setCreatedAt(String createdAt)
	{
		this.createdAt = createdAt;
	}

	public void setCreatedAt(JSONObject nameserver)
	{
		this.createdAt = jsonStringValue(nameserver, CREATED_AT);
	}

	public String getStateChangedAt()
	{
		return stateChangedAt;
	}

	public void setStateChangedAt(String stateChangedAt)
	{
		this.stateChangedAt = stateChangedAt;
	}

	public void setStateChangedAt(JSONObject nameserver)
	{
		this.createdAt = jsonStringValue(nameserver, STATE_CHANGED_AT);
	}

	public String getUpdatedAt()
	{
		return updatedAt;
	}

	public void setUpdatedAt(String updatedAt)
	{
		this.updatedAt = updatedAt;
	}

	public void setUpdatedAt(JSONObject nameserver)
	{
		this.createdAt = jsonStringValue(nameserver, UPDATED_AT);
	}

	public String getCheckedAt()
	{
		return checkedAt;
	}

	public void setCheckedAt(String checkedAt)
	{
		this.checkedAt = checkedAt;
	}

	public void setCheckedAt(JSONObject nameserver)
	{
		this.createdAt = jsonStringValue(nameserver, CHECKED_AT);
	}
}
