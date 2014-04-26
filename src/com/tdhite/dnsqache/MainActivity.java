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
Portions also Copyright (c) 2009 by Harald Mueller and Sofia Lemons.

*/

package com.tdhite.dnsqache;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity
{
	public static final String TAG = "DNSQACHE -> MainActivity";
	public static final String VIEW_LOG_FILE_EXTRA = "logFile";
	private boolean mInitialized = false;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public void onWindowFocusChanged(boolean focused)
	{
		if (focused && !mInitialized)
		{
			boolean dnsActive = QacheService.isDnsQacheActive();

			// Set the toggle button based on the system property
			// dnsqache.status
			ToggleButton btnActive = (ToggleButton) this
					.findViewById(R.id.qache_active_button);
			btnActive.setChecked(dnsActive);

			// set the text to reflect the next user action
			setTextViewText(R.id.text_qache_start, dnsActive);

            // Open donate-dialog
            openDonateDialog();

			mInitialized = true;
		}
	}

	private void setTextViewText(int textViewId, boolean active)
	{
		String text = active ? this.getString(R.string.text_qache_stop)
				: this.getString(R.string.text_qache_start);
		TextView textView = (TextView) this.findViewById(textViewId);
		textView.setText(text);
	}

	private boolean openPreferencesActivity()
	{
		Intent intent = new Intent(this, PrefsActivity.class);
		startActivity(intent);
		return true;
	}

	private boolean openViewLogActivity(int id)
	{
		Bundle b = new Bundle();
	    b.putInt(VIEW_LOG_FILE_EXTRA, id);
		Intent intent = new Intent(this, ViewLogActivity.class);
		intent.putExtras(b);
		startActivity(intent);
		return true;
	}

	private void openAboutDialog()
	{
		QacheApplication qacheApp = (QacheApplication) this.getApplicationContext();

		LayoutInflater li = LayoutInflater.from(this);
		View view = li.inflate(R.layout.activity_about, null);
		TextView versionName = (TextView) view.findViewById(R.id.versionName);

		versionName.setText(qacheApp.getVersionName());

		TextView authors = (TextView) view.findViewById(R.id.authors);
		authors.setText(qacheApp.getAuthors());
		authors.setMovementMethod(LinkMovementMethod.getInstance());

		new AlertDialog.Builder(MainActivity.this)
				.setTitle(getString(R.string.activity_main_about))
				.setView(view)
				.setNeutralButton(getString(R.string.activity_main_donate),
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog,
									int whichButton)
							{
								Log.d(TAG, "Donate pressed");
								Uri uri = Uri
										.parse(getString(R.string.activity_main_donate_url));
								startActivity(new Intent(Intent.ACTION_VIEW,
										uri));
							}
						})
				.setNegativeButton(getString(R.string.activity_main_close),
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog,
									int whichButton)
							{
								Log.d(TAG, "Close pressed");
							}
						}).show();
	}

	private void openDonateDialog()
	{
		QacheApplication qacheApp = (QacheApplication) this.getApplicationContext();

		if (qacheApp.showDonationDialog())
		{
			Editor prefs = qacheApp.getSettingsEditor();

			// Disable donate-dialog for later startups
			prefs.putBoolean("donatepref", false);
			prefs.commit();

			// Creating Layout
			LayoutInflater li = LayoutInflater.from(this);
			View view = li.inflate(R.layout.view_donate, null);
			new AlertDialog.Builder(MainActivity.this)
					.setTitle(
							getString(R.string.activity_main_donation_headline))
					.setView(view)
					.setNeutralButton(getString(R.string.activity_main_close),
							new DialogInterface.OnClickListener()
							{
								public void onClick(DialogInterface dialog,
										int whichButton)
								{
									Log.d(TAG, "Close pressed");
								}
							})
					.setNegativeButton(
							getString(R.string.activity_main_donate),
							new DialogInterface.OnClickListener()
							{
								public void onClick(DialogInterface dialog,
										int whichButton)
								{
									Log.d(TAG, "Donate pressed");
									Uri uri = Uri
											.parse(getString(R.string.activity_main_donate_url));
									startActivity(new Intent(
											Intent.ACTION_VIEW, uri));
								}
							}).show();
		}
	}

    private void startQache()
	{
		Intent intent = new Intent();
		intent.setClass(this, QacheService.class);
		startService(intent);
	}

	private void stopQache()
	{
		Intent intent = new Intent();
		intent.setClass(this, QacheService.class);
		stopService(intent);
	}

	private void toggleQache(ToggleButton toggle)
	{
		if (toggle.isChecked())
			startQache();
		else
			stopQache();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean handled = super.onOptionsItemSelected(item);

		// Handle item selection
	    switch (item.getItemId()) {
	        case R.id.action_about:
	        	this.openAboutDialog();
	            handled = true;
	            break;
	        case R.id.action_view_log:
	        	handled = this.openViewLogActivity(item.getItemId());
	        	break;
	        case R.id.action_view_tinyproxy_log:
	        	handled = this.openViewLogActivity(item.getItemId());
	        	break;
	        case R.id.action_view_polipo_log:
	        	handled = this.openViewLogActivity(item.getItemId());
	        	break;
	        case R.id.action_preferences:
	        	handled = this.openPreferencesActivity();
	        	break;
	    }

	    return handled;
	}
	
	// This method is called at button click because we assigned the name to the
	// "OnClick property" of the button
	public void onClick(View view)
	{
		switch (view.getId())
		{
			case R.id.qache_active_button:
				toggleQache((ToggleButton) view);
				break;
		}
	}
}
