package com.tdhite.dnsqache.task;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

public abstract class Task extends AsyncTask<Void, String, Boolean> {
	public static final String TAG = "DNSQACHE -> Task";

	protected Integer mId = 0;
	protected Context mContext = null;
	protected ProgressDialog mProgressDialog = null;
	protected Callback<Boolean> mCallback = null;

	private Boolean mStartInBackground = false;

	public Task(int id, Context context, Callback<Boolean> callback, Boolean startInBackground) {
		mId = id;
		mContext = context;
		mCallback = callback;
		mStartInBackground = startInBackground;

		if (mContext != null && mStartInBackground == false) {
			mProgressDialog = new ProgressDialog(mContext);
			mProgressDialog.setTitle(null);
			mProgressDialog.setMessage("Please wait...");
			mProgressDialog.setCancelable(false);
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setIndeterminate(true);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setIcon(0);
		}
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (mProgressDialog != null) {
			mProgressDialog.show();
		}
	}

	@Override
	protected void onProgressUpdate(String... progress) {
		super.onProgressUpdate(progress);
		if (mProgressDialog != null && mProgressDialog.isShowing() == true) {
			mProgressDialog.setMessage(progress[0]);
		}
	}

	@Override
	protected void onPostExecute(Boolean result) {
		dismiss();
		if (mCallback != null) {
			mCallback.onTaskComplete(mId, result);
		}
	}

	@Override
	protected void onCancelled() {
		dismiss();
		super.onCancelled();
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCancelled(Boolean result) {
		dismiss();
		super.onCancelled(result);
	}

	protected Boolean falseWithError(String error) {
		Log.e(TAG, error);
		return false;
	}

	private void dismiss() {
		try {
			if (mProgressDialog != null && mProgressDialog.isShowing() == true) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
		}
		catch (IllegalArgumentException e) {
			Log.w(TAG, "IllegalArgumentException: " + e.getMessage());
		}
	}

	public interface Callback<T> {
		public static final int TASK_CHECK = 0;
		public static final int TASK_INSTALL = 1;
		public static final int TASK_START = 2;
		public static final int TASK_STOP = 3;
		public void onTaskComplete(int id, T result);
	}
}