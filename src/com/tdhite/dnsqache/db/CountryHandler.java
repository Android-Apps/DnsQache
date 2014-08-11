package com.tdhite.dnsqache.db;

import com.tdhite.dnsqache.model.Country;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class CountryHandler extends SQLiteOpenHelper {
	public static final String TAG = "DNSQACHE.CountryHandler";

	public static final String DB_TABLE = "Country";

	public static final String CREATE = "create table Country columns ('code' TEXT primary key, name TEXT)";

	public CountryHandler(Context context, String name, CursorFactory factory,
			int version) {
		super(context, name, factory, version);
	}

    public CountryHandler(Context context) {
        super(context, Constants.DB_NAME, null, Constants.DB_VERSION);
    }

	@Override
	public void onCreate(SQLiteDatabase db) {
		StringBuffer sql = new StringBuffer();

		sql.append("CREATE TABLE ");
		sql.append(DB_TABLE);
		sql.append(Constants.OPEN_PAREN);

		sql.append(Country.COL_ID);
		sql.append(" TEXT PRIMARY KEY,");

		sql.append(Country.COL_NAME);
		sql.append(Constants.DB_TEXT_TYPE_END);
		Log.d(TAG, sql.toString());
		db.execSQL(sql.toString());

		sql.setLength(0);
		sql.append("CREATE INDEX ");
		sql.append(Country.COL_NAME);
		sql.append("_country_idx on ");
		sql.append(DB_TABLE);
		sql.append(Constants.OPEN_PAREN);
		sql.append(Country.COL_NAME);
		sql.append(Constants.CLOSE_PAREN);
		Log.d(TAG, sql.toString());
		db.execSQL(sql.toString());

		// pre-populate the table since it's really static data
		for (String[] country : Country.CODES) {
			db = create(db, country[Country.COL_IDX_ID], country[Country.COL_IDX_NAME]);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE);
		onCreate(db);
	}

	public void clear() {
		SQLiteDatabase db = this.getWritableDatabase();
		onUpgrade(db, Constants.DB_VERSION, Constants.DB_VERSION);
		db.close();
	}

	private Country[] fromCursor(Cursor cursor)
	{
		Country[] countries = null;

		int count = cursor.getCount();
		if (count > 0) {
			countries = new Country[count];
			for (int i = 0; i < count; i++) {
				countries[i].setId(cursor.getString(0));
				countries[i].setName(cursor.getString(1));
			}
		}
		else
		{
			countries = new Country[0];
		}

		return countries;
	}

	public Country[] read(String countryId)
	{
		SQLiteDatabase db = this.getReadableDatabase();

		// String table, String[] columns, String selection,
		// String[] selectionArgs, String groupBy, String having, String orderBy
		Cursor cursor = db.query(DB_TABLE, new String[] { Country.COL_ID,
				Country.COL_NAME }, Country.COL_ID + "=?",
				new String[] { countryId }, null, null, null);
		if (cursor != null)
			cursor.moveToFirst();

		Country[] provider = fromCursor(cursor);

		cursor.close();
		db.close();

		return provider;
	}

	public Cursor read()
	{
		String selectQuery = "SELECT  * FROM " + DB_TABLE + " ORDER BY "
				+ Country.COL_NAME;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

		return cursor;
	}

	public void create(Country country)
	{
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(Country.COL_ID, country.getId());
		values.put(Country.COL_NAME, country.getName());
		db.insert(DB_TABLE, null, values);

		db.close();
	}

	public SQLiteDatabase create(SQLiteDatabase db, String id, String name)
	{
		if (db == null)
		{
			db = this.getWritableDatabase();
		}

		ContentValues values = new ContentValues();
		values.put(Country.COL_ID, id);
		values.put(Country.COL_NAME, name);
		db.insert(DB_TABLE, null, values);
		
		return db;
	}

	public void create(String id, String name)
	{
		SQLiteDatabase db = this.create(null, id, name);
		if (db != null)
		{
			db.close();
		}
	}
}
