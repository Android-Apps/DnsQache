package com.tdhite.dnsqache.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.tdhite.dnsqache.model.DnsProvider;

public class DnsProviderHandler extends SQLiteOpenHelper
{
	public static final String TAG = "DnsProviderHandler";

	public static final String DB_TABLE = "DnsProvider";

	public DnsProviderHandler(Context context)
	{
		super(context, Constants.DB_NAME, null, Constants.DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		StringBuilder sql = new StringBuilder();

		sql.append("CREATE TABLE ");
		sql.append(DB_TABLE);
		sql.append(Constants.OPEN_PAREN);
		sql.append(DnsProvider.COL_ID);
		sql.append(" INTEGER PRIMARY KEY,");
		sql.append(DnsProvider.COL_CITY);
		sql.append(Constants.DB_TEXT_TYPE);
		sql.append(DnsProvider.COL_COUNTRY_ID);
		sql.append(Constants.DB_TEXT_TYPE);
		sql.append(DnsProvider.COL_NAME);
		sql.append(Constants.DB_TEXT_TYPE);
		sql.append(DnsProvider.COL_IP);
		sql.append(Constants.DB_TEXT_TYPE);
		sql.append(DnsProvider.COL_STATE);
		sql.append(Constants.DB_TEXT_TYPE);
		sql.append(DnsProvider.COL_ERROR);
		sql.append(Constants.DB_TEXT_TYPE);
		sql.append(DnsProvider.COL_VERSION);
		sql.append(Constants.DB_TEXT_TYPE);
		sql.append(DnsProvider.COL_CREATED_AT);
		sql.append(Constants.DB_TEXT_TYPE);
		sql.append(DnsProvider.COL_STATE_CHANGED_AT);
		sql.append(Constants.DB_TEXT_TYPE);
		sql.append(DnsProvider.COL_UPDATED_AT);
		sql.append(Constants.DB_TEXT_TYPE);
		sql.append(DnsProvider.COL_CHECKED_AT);
		sql.append(Constants.DB_TEXT_TYPE_END);
		db.execSQL(sql.toString());

		sql.setLength(0);
		sql.append("CREATE INDEX ");
		sql.append(DnsProvider.COL_COUNTRY_ID);
		sql.append("_idx on ");
		sql.append(DB_TABLE);
		sql.append(Constants.OPEN_PAREN);
		sql.append(DnsProvider.COL_COUNTRY_ID);
		sql.append(Constants.CLOSE_PAREN);
		db.execSQL(sql.toString());

		sql.setLength(0);
		sql.append("CREATE INDEX ");
		sql.append(DnsProvider.COL_NAME);
		sql.append("_idx on ");
		sql.append(DB_TABLE);
		sql.append(Constants.OPEN_PAREN);
		sql.append(DnsProvider.COL_NAME);
		sql.append(Constants.CLOSE_PAREN);
		db.execSQL(sql.toString());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE);
		onCreate(db);
	}

	public void clear()
	{
		SQLiteDatabase db = this.getWritableDatabase();
		onUpgrade(db, Constants.DB_VERSION, Constants.DB_VERSION);
		db.close();
	}

	public SQLiteDatabase create(SQLiteDatabase db, DnsProvider provider)
	{
		if (db == null)
		{
			db = this.getWritableDatabase();
		}

		ContentValues values = new ContentValues();
		values.put(DnsProvider.COL_ID, provider.getId());
		values.put(DnsProvider.COL_CITY, provider.getCity());
		values.put(DnsProvider.COL_COUNTRY_ID, provider.getCountryId());
		values.put(DnsProvider.COL_NAME, provider.getName());
		values.put(DnsProvider.COL_IP, provider.getIp());
		values.put(DnsProvider.COL_STATE, provider.getState());
		values.put(DnsProvider.COL_ERROR, provider.getError());
		values.put(DnsProvider.COL_VERSION, provider.getVersion());
		values.put(DnsProvider.COL_CREATED_AT, provider.getCreatedAt());
		values.put(DnsProvider.COL_STATE_CHANGED_AT,
				provider.getStateChangedAt());
		values.put(DnsProvider.COL_UPDATED_AT, provider.getUpdatedAt());
		values.put(DnsProvider.COL_CHECKED_AT, provider.getCheckedAt());

		db.insert(DB_TABLE, null, values);
		
		return db;
	}

	public void create(DnsProvider provider)
	{
		SQLiteDatabase db = this.create(null, provider);
		db.close();
	}

	public DnsProvider read(int id)
	{
		SQLiteDatabase db = this.getReadableDatabase();

		// String table, String[] columns, String selection,
		// String[] selectionArgs, String groupBy, String having, String orderBy
		Cursor cursor = db.query(DB_TABLE, new String[]
			{
					DnsProvider.COL_ID, DnsProvider.COL_CITY,
					DnsProvider.COL_COUNTRY_ID, DnsProvider.COL_NAME,
					DnsProvider.COL_IP, DnsProvider.COL_STATE,
					DnsProvider.COL_ERROR, DnsProvider.COL_VERSION,
					DnsProvider.COL_CREATED_AT,
					DnsProvider.COL_STATE_CHANGED_AT,
					DnsProvider.COL_UPDATED_AT, DnsProvider.COL_CHECKED_AT
			}, DnsProvider.COL_ID + "=?", new String[]
			{
				String.valueOf(id)
			}, null, null, DnsProvider.COL_NAME);
		if (cursor != null)
			cursor.moveToFirst();

		DnsProvider provider = new DnsProvider(cursor);
		return provider;
	}

	public Cursor read(String countryId)
	{
		SQLiteDatabase db = this.getReadableDatabase();

		// String table, String[] columns, String selection,
		// String[] selectionArgs, String groupBy, String having, String orderBy
		Cursor cursor = db.query(DB_TABLE, new String[]
			{
					DnsProvider.COL_ID, DnsProvider.COL_CITY,
					DnsProvider.COL_COUNTRY_ID, DnsProvider.COL_NAME,
					DnsProvider.COL_IP, DnsProvider.COL_STATE,
					DnsProvider.COL_ERROR, DnsProvider.COL_VERSION,
					DnsProvider.COL_CREATED_AT,
					DnsProvider.COL_STATE_CHANGED_AT,
					DnsProvider.COL_UPDATED_AT, DnsProvider.COL_CHECKED_AT
			}, DnsProvider.COL_COUNTRY_ID + "=?", new String[]
			{
				countryId
			}, null, null, DnsProvider.COL_NAME);

		return cursor;
	}

	public int update(DnsProvider provider)
	{
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(DnsProvider.COL_ID, provider.getId());
		values.put(DnsProvider.COL_CITY, provider.getCity());
		values.put(DnsProvider.COL_COUNTRY_ID, provider.getCountryId());
		values.put(DnsProvider.COL_NAME, provider.getName());
		values.put(DnsProvider.COL_IP, provider.getIp());
		values.put(DnsProvider.COL_STATE, provider.getState());
		values.put(DnsProvider.COL_ERROR, provider.getError());
		values.put(DnsProvider.COL_VERSION, provider.getVersion());
		values.put(DnsProvider.COL_CREATED_AT, provider.getCreatedAt());
		values.put(DnsProvider.COL_STATE_CHANGED_AT,
				provider.getStateChangedAt());
		values.put(DnsProvider.COL_UPDATED_AT, provider.getUpdatedAt());
		values.put(DnsProvider.COL_CHECKED_AT, provider.getCheckedAt());

		return db.update(DB_TABLE, values, DnsProvider.COL_ID + " = ?",
				new String[]
					{
						String.valueOf(provider.getId())
					});
	}
	
	public void delete(DnsProvider provider)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(DB_TABLE, DnsProvider.COL_ID + " = ?", new String[]
			{
				String.valueOf(provider.getId())
			});
		db.close();
	}
}
