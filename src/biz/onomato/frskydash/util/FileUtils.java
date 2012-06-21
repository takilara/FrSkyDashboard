package biz.onomato.frskydash.util;

import android.os.Environment;

/**
 * some utilities to check for working with files
 * 
 */
public class FileUtils {

	/**
	 * possible state for external storage
	 * 
	 */
	public enum ExternalStorageState {
		AVAILABLE, WRITABLE, UNAVAILABLE
	}

	/**
	 * get the current state for external storage
	 * 
	 * @return
	 */
	public static ExternalStorageState getExternalStorageState() {
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		// return correct state
		if (mExternalStorageWriteable)
			return ExternalStorageState.WRITABLE;
		else if (mExternalStorageAvailable)
			return ExternalStorageState.AVAILABLE;
		else
			return ExternalStorageState.UNAVAILABLE;
	}
}
