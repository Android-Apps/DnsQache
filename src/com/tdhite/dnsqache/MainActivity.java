package com.tdhite.dnsqache;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity
{
	public static final String TAG = "DNSQACHE -> MainActivity";

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
			boolean proxyActive = QacheService.isProxyActive();

			// Set the toggle button based on the system property
			// dnsqache.status
			ToggleButton btnActive = (ToggleButton) this
					.findViewById(R.id.qache_active_button);
			btnActive.setChecked(dnsActive);

			// set the text to reflect the next user action
			setTextViewText(R.id.text_qache_start, dnsActive);

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

    private boolean openConfigureActivity()
    {
        Intent intent = new Intent(this, ConfigureDNSActivity.class);
        startActivity(intent);
		return true;
    }

	private boolean openSettingsActivity()
	{
		Intent intent = new Intent(this, ConfigureProxyActivity.class);
		startActivity(intent);
		return true;
	}

	private boolean openViewLogActivity()
	{
		Intent intent = new Intent(this, ViewLogActivity.class);
		startActivity(intent);
		return true;
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
	        case R.id.action_configure:
	        	handled = this.openConfigureActivity();
	        	break;
	        case R.id.action_settings:
	        	handled = this.openSettingsActivity();
	        	break;
	        case R.id.action_about:
	        	// Not quite yet
	            handled = false;
	            break;
	        case R.id.action_view_log:
	        	handled = this.openViewLogActivity();
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
