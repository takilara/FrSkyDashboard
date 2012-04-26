package biz.onomato.frskydash.util;

import android.util.Log;
import biz.onomato.frskydash.FrSkyServer;

/**
 * utility class for centralised control over logging. Relies on the android
 * logging for actual performing log statements
 * 
 * @author hcpl
 * 
 */
public class Logger {

	public static void i(String tag, String msg) {
		if (FrSkyServer.D)
			Log.i(tag, msg);
	}

	public static void d(String tag, String msg) {
		if (FrSkyServer.D)
			Log.d(tag, msg);
	}

	public static void e(String tag, String msg) {
		if (FrSkyServer.D)
			Log.e(tag, msg);
	}

	public static void e(String tag, String msg, Exception e) {
		if (FrSkyServer.D)
			Log.e(tag, msg, e);
	}

	public static void w(String tag, String msg) {
		if (FrSkyServer.D)
			Log.w(tag, msg);
	}
}
