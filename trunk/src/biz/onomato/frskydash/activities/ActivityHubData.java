package biz.onomato.frskydash.activities;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;
import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.hub.ChannelTypes;
import biz.onomato.frskydash.util.Logger;

/**
 * Activity for displaying sensor hub data
 * 
 * @author hcpl
 * 
 */
public class ActivityHubData extends Activity {

	public static final String FIELD_VALUE = "value";
	public static final String FIELD_CHANNEL = "channel-type";
	/**
	 * receiving intent
	 */
	private Intent broadcastIntent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// set layout
		setContentView(R.layout.activity_hubdata);
		// register the receiving intent
		broadcastIntent = new Intent(this, FrSkyServer.class);
	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateUI(intent);
		}
	};

	@Override
	public void onResume() {
		super.onResume();
		// startService(broadcastIntent);
		registerReceiver(broadcastReceiver, new IntentFilter(
				FrSkyServer.BROADCAST_ACTION_HUB_DATA));
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(broadcastReceiver);
		// stopService(broadcastIntent);
	}

	private NumberFormat format = new DecimalFormat("0");

	private void updateUI(Intent intent) {
		String channelType = intent.getStringExtra(FIELD_CHANNEL);
		double value = intent.getDoubleExtra(FIELD_VALUE, 0);

		switch (ChannelTypes.valueOf(channelType)) {
		case altitude_before:
			((TextView) findViewById(R.id.textViewAltBefore)).setText(format
					.format(value));
			break;
		case altitude_after:
			((TextView) findViewById(R.id.textViewAltAfter)).setText(format
					.format(value));
			break;
		case rpm:
			((TextView) findViewById(R.id.textViewRpm)).setText(format
					.format(value));
			break;
		case acc_x:
			((TextView) findViewById(R.id.textViewAccX)).setText(format
					.format(value));
			break;
		case acc_y:
			((TextView) findViewById(R.id.textViewAccY)).setText(format
					.format(value));
			break;
		case acc_z:
			((TextView) findViewById(R.id.textViewAccZ)).setText(format
					.format(value));
			break;
		case course_after:
			((TextView) findViewById(R.id.textViewCourseAfter)).setText(format
					.format(value));
			break;
		case course_before:
			((TextView) findViewById(R.id.textViewCourseBefore)).setText(format
					.format(value));
			break;
		case day:
			((TextView) findViewById(R.id.textViewDay)).setText(format
					.format(value));
			break;
		case month:
			((TextView) findViewById(R.id.textViewMonth)).setText(format
					.format(value));
			break;
		case year:
			((TextView) findViewById(R.id.textViewYear)).setText(format
					.format(value));
			break;
		case fuel:
			((TextView) findViewById(R.id.textViewFuel)).setText(format
					.format(value));
			break;
		case hour:
			((TextView) findViewById(R.id.textViewHour)).setText(format
					.format(value));
			break;

		case minute:
			((TextView) findViewById(R.id.textViewMinute)).setText(format
					.format(value));
			break;
		case second:
			((TextView) findViewById(R.id.textViewSecond)).setText(format
					.format(value));
			break;
		case gps_altitude_after:
			((TextView) findViewById(R.id.textViewGpsAltAfter)).setText(format
					.format(value));
			break;
		case gps_altitude_before:
			((TextView) findViewById(R.id.textViewGpsAltBefore)).setText(format
					.format(value));
			break;
		case gps_speed_after:
			((TextView) findViewById(R.id.textViewSpeedAfter)).setText(format
					.format(value));
			break;
		case gps_speed_before:
			((TextView) findViewById(R.id.textViewSpeedBefore)).setText(format
					.format(value));
			break;
		case latitude_after:
			((TextView) findViewById(R.id.textViewLatAfter)).setText(format
					.format(value));
			break;
		case latitude_before:
			((TextView) findViewById(R.id.textViewLatBefore)).setText(format
					.format(value));
			break;
		case longitude_after:
			((TextView) findViewById(R.id.textViewLonAfter)).setText(format
					.format(value));
			break;
		case longitude_before:
			((TextView) findViewById(R.id.textViewLonBefore)).setText(format
					.format(value));
			break;
		case temp1:
			((TextView) findViewById(R.id.textViewTemp1)).setText(format
					.format(value));
			break;
		case temp2:
			((TextView) findViewById(R.id.textViewTemp2)).setText(format
					.format(value));
			break;
		case volt:
			((TextView) findViewById(R.id.textViewVolt)).setText(format
					.format(value));
			break;
		default:
			// TODO update other fields
			Logger.d(this.getClass().getName(),
					"non implemented display of channel type: " + channelType);
		}

	}

}
