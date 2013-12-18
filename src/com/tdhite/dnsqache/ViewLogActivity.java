package com.tdhite.dnsqache;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import android.os.Bundle;
import android.app.Activity;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class ViewLogActivity extends Activity {
	static public final String TAG = "DNSMASQ -> ViewLogActivity";

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_log);

		// Init Application
		WebView webView = (WebView) findViewById(R.id.webviewLog);
		webView.getSettings().setJavaScriptEnabled(false);
		webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
		webView.getSettings().setSupportMultipleWindows(false);
		webView.getSettings().setSupportZoom(false);
		setWebViewContent(webView);
	}

	private static final String HTML_HEADER = "<html><head><title>background-color</title> "
			+ "<style type=\"text/css\"> "
			+ "body { background-color:#181818; font-family:Arial; font-size:100%; color: #ffffff } "
			+ ".date { font-family:Arial; font-size:80%; font-weight:bold} "
			+ ".done { font-family:Arial; font-size:80%; color: #2ff425} "
			+ ".failed { font-family:Arial; font-size:80%; color: #ff3636} "
			+ ".skipped { font-family:Arial; font-size:80%; color: #6268e5} "
			+ "</style> " + "</head><body>";
	private static final String HTML_FOOTER = "</body></html>";

	private void setWebViewContent(WebView webView) {
		QacheService svc = QacheService.getSingleton();
		if (svc != null) {
			svc.generateLogFile();
		}
		webView.loadDataWithBaseURL("fake://fakeme",
				HTML_HEADER + this.readLogfile() + HTML_FOOTER, "text/html",
				"UTF-8", "fake://fakeme");
	}

	private String readLogfile() {
		FileInputStream fis = null;
		InputStreamReader isr = null;
		String data = "";
		QacheApplication app = (QacheApplication) this.getApplication();
		try {
			String logFile = app.getConfigManager().getLogFile();
			File file = new File(logFile);
			fis = new FileInputStream(file);
			isr = new InputStreamReader(fis, "utf-8");
			char[] buff = new char[(int) file.length()];
			isr.read(buff);
			data = new String(buff);
		} catch (Exception e) {
			data = this.getString(R.string.log_activity_filenotfound)
					+ ":\n" + app.getConfigManager().getLogFile()
					+ "\nException:\n" + e.toString();
		} finally {
			try {
				if (isr != null)
					isr.close();
				if (fis != null)
					fis.close();
			} catch (Exception e) {
				// nothing
			}
		}
		return data;
	}
}
