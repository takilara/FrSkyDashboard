/*
 * Copyright 2011-2013, Espen Solbu, Hans Cappelle
 * 
 * This file is part of FrSky Dashboard.
 *
 *  FrSky Dashboard is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FrSky Dashboard is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FrSky Dashboard.  If not, see <http://www.gnu.org/licenses/>.
 */

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
