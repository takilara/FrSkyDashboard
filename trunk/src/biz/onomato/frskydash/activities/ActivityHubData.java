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

	/**
	 * number format for integers
	 */
	private NumberFormat intFormat = new DecimalFormat("0");

	/**
	 * number format for decimals
	 */
	private NumberFormat decFormat = new DecimalFormat("0.00");

	private void updateUI(Intent intent) {
		String channelType = intent.getStringExtra(FIELD_CHANNEL);
		double value = intent.getDoubleExtra(FIELD_VALUE, 0);

		switch (ChannelTypes.valueOf(channelType)) {
		case altitude_before:
			((TextView) findViewById(R.id.textViewAltBefore)).setText(intFormat
					.format(value));
			break;
		case altitude_after:
			((TextView) findViewById(R.id.textViewAltAfter)).setText(intFormat
					.format(value));
			break;
		case rpm:
			((TextView) findViewById(R.id.textViewRpm)).setText(intFormat
					.format(value));
			break;
		case acc_x:
			((TextView) findViewById(R.id.textViewAccX)).setText(intFormat
					.format(value));
			break;
		case acc_y:
			((TextView) findViewById(R.id.textViewAccY)).setText(intFormat
					.format(value));
			break;
		case acc_z:
			((TextView) findViewById(R.id.textViewAccZ)).setText(intFormat
					.format(value));
			break;
		case course_after:
			((TextView) findViewById(R.id.textViewCourseAfter))
					.setText(intFormat.format(value));
			break;
		case course_before:
			((TextView) findViewById(R.id.textViewCourseBefore))
					.setText(intFormat.format(value));
			break;
		case day:
			((TextView) findViewById(R.id.textViewDay)).setText(intFormat
					.format(value));
			break;
		case month:
			((TextView) findViewById(R.id.textViewMonth)).setText(intFormat
					.format(value));
			break;
		case year:
			((TextView) findViewById(R.id.textViewYear)).setText(intFormat
					.format(value));
			break;
		case fuel:
			((TextView) findViewById(R.id.textViewFuel)).setText(intFormat
					.format(value));
			break;
		case hour:
			((TextView) findViewById(R.id.textViewHour)).setText(intFormat
					.format(value));
			break;

		case minute:
			((TextView) findViewById(R.id.textViewMinute)).setText(intFormat
					.format(value));
			break;
		case second:
			((TextView) findViewById(R.id.textViewSecond)).setText(intFormat
					.format(value));
			break;
		case gps_altitude_after:
			((TextView) findViewById(R.id.textViewGpsAltAfter))
					.setText(intFormat.format(value));
			break;
		case gps_altitude_before:
			((TextView) findViewById(R.id.textViewGpsAltBefore))
					.setText(intFormat.format(value));
			break;
		case gps_speed_after:
			((TextView) findViewById(R.id.textViewSpeedAfter))
					.setText(intFormat.format(value));
			break;
		case gps_speed_before:
			((TextView) findViewById(R.id.textViewSpeedBefore))
					.setText(intFormat.format(value));
			break;
		case latitude_after:
			((TextView) findViewById(R.id.textViewLatAfter)).setText(intFormat
					.format(value));
			break;
		case latitude_before:
			((TextView) findViewById(R.id.textViewLatBefore)).setText(intFormat
					.format(value));
			break;
		case longitude_after:
			((TextView) findViewById(R.id.textViewLonAfter)).setText(intFormat
					.format(value));
			break;
		case longitude_before:
			((TextView) findViewById(R.id.textViewLonBefore)).setText(intFormat
					.format(value));
			break;
		case temp1:
			((TextView) findViewById(R.id.textViewTemp1)).setText(intFormat
					.format(value));
			break;
		case temp2:
			((TextView) findViewById(R.id.textViewTemp2)).setText(intFormat
					.format(value));
			break;
		// for voltage values per cell
		case volt_0:
			((TextView) findViewById(R.id.textViewVoltCell1)).setText(decFormat
					.format(value));
			break;
		case volt_1:
			((TextView) findViewById(R.id.textViewVoltCell2)).setText(decFormat
					.format(value));
			break;
		case volt_2:
			((TextView) findViewById(R.id.textViewVoltCell3)).setText(decFormat
					.format(value));
			break;
		case volt_3:
			((TextView) findViewById(R.id.textViewVoltCell4)).setText(decFormat
					.format(value));
			break;
		case volt_4:
			((TextView) findViewById(R.id.textViewVoltCell5)).setText(decFormat
					.format(value));
			break;
		case volt_5:
			((TextView) findViewById(R.id.textViewVoltCell6)).setText(decFormat
					.format(value));
			break;

		default:
			// TODO update other fields (NE, WS from gps? new current sensors?)
			Logger.d(this.getClass().getName(),
					"non implemented display of channel type: " + channelType);
		}

	}

}
