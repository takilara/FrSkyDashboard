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
	public static boolean D = false;

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
