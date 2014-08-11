package com.tdhite.dnsqache.db;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DnsQacheDB
{
	private Context context;
	private DatabaseHelper dbHelper;
    private SQLiteDatabase db;

	public DnsQacheDB(Context ctx) {
		this.context = ctx;
		dbHelper = new DatabaseHelper(ctx);
	}

	private class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, Constants.DB_NAME, null, Constants.DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			DnsProviderHandler dh = new DnsProviderHandler(
					DnsQacheDB.this.context);
			dh.onCreate(db);

			CountryHandler ch = new CountryHandler(DnsQacheDB.this.context);
			ch.onCreate(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			DnsProviderHandler dh = new DnsProviderHandler(
					DnsQacheDB.this.context);
			dh.onUpgrade(db, oldVersion, newVersion);

			CountryHandler ch = new CountryHandler(DnsQacheDB.this.context);
			ch.onUpgrade(db, oldVersion, newVersion);
		}
	}

	public DnsQacheDB open() throws SQLException {
		this.db = dbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		dbHelper.close();
	}
}