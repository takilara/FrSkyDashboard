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

import android.util.Log;

/**
 * utility class for centralized control over logging. Relies on the android
 * logging {@link android.util.Log} for actual log statements. Benefit of using
 * this wrapper is that checks on flags to activate/desactivate logging only
 * have to be done once. Also this could contain other specific logic required
 * on all log statements.
 * 
 * @author hcpl
 * 
 */
public class Logger {

	/**
	 * property to indicate if logging is active or not
	 */
	public static boolean D = true;

	public static void i(String tag, String msg) {
		if (Logger.D)
			Log.i(tag, msg);
	}

	public static void d(String tag, String msg) {
		if (Logger.D)
			Log.d(tag, msg);
	}

	public static void e(String tag, String msg) {
		if (Logger.D)
			Log.e(tag, msg);
	}

	public static void e(String tag, String msg, Exception e) {
		if (Logger.D)
			Log.e(tag, msg, e);
	}

	public static void w(String tag, String msg) {
		if (Logger.D)
			Log.w(tag, msg);
	}

}
