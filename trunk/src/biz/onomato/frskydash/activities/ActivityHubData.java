package biz.onomato.frskydash.activities;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.hub.ChannelTypes;
import biz.onomato.frskydash.hub.EditPreferences;
import biz.onomato.frskydash.hub.FrSkyHub;
import biz.onomato.frskydash.util.Logger;

/**
 * Activity for displaying sensor hub data
 * 
 * @author hcpl
 * 
 */
public class ActivityHubData extends Activity {

	private static final int INTERVAL_GUI_UPDATE = 100;
	public static final String FIELD_VALUE = "value";
	public static final String FIELD_CHANNEL = "channel-type";
	private static final int ACTIVITY_PREFERENCES = 1;
	/**
	 * receiving intent
	 */
	private Intent broadcastIntent;

	/**
	 * hashmap containing last sensor values, broadcast updates these values,
	 * gui updates on interval based on these values
	 */
	private HashMap<ChannelTypes, Double> sensorValues = new HashMap<ChannelTypes, Double>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// set layout
		setContentView(R.layout.activity_hubdata);
		// register the receiving intent
		broadcastIntent = new Intent(this, FrSkyServer.class);

		// init all required fields here for performance
		initTextFields();

		// a thread responsible for updating the values on the gui
		new Thread(new Runnable() {
			public void run() {
				// infinite loop
				while (true) {
					// iterate values
					for (final ChannelTypes type : sensorValues.keySet()) {
						// back to GUI
						runOnUiThread(new Runnable() {
							public void run() {
								updateUI(type, sensorValues.get(type));
							}
						});
					}
					// and repeat this on interval
					try {
						Thread.sleep(INTERVAL_GUI_UPDATE);
					} catch (InterruptedException e) {
						Logger.e(ActivityHubData.this.getClass().toString(),
								"Sleep for interval GUI update interrupted", e);
					}
				}
			}
		}).start();
	}

	private TextView textViewAltBefore, textViewAltAfter, textViewRpm,
			textViewAccX, textViewAccY, textViewAccZ, textViewCourseAfter,
			textViewDay, textViewMonth, textViewYear, textViewFuel,
			textViewHour, textViewMinute, textViewSecond, textViewGpsAltAfter,
			textViewGpsAltBefore, textViewSpeedAfter, textViewSpeedBefore,
			textViewLatAfter, textViewCourseBefore, textViewLatBefore,
			textViewLonAfter, textViewLonBefore, textViewTemp1, textViewTemp2,
			textViewVoltCell2, textViewVoltCell1, textViewVoltCell3,
			textViewVoltCell4, textViewVoltCell5, textViewVoltCell6, textViewNS, textViewEW;

	/**
	 * init all the text fields only once on create of activity
	 */
	private void initTextFields() {
		textViewAltBefore = (TextView) findViewById(R.id.textViewAltBefore);
		textViewAltAfter = (TextView) findViewById(R.id.textViewAltAfter);
		textViewRpm = (TextView) findViewById(R.id.textViewRpm);
		textViewAccX = (TextView) findViewById(R.id.textViewAccX);
		textViewAccY = (TextView) findViewById(R.id.textViewAccY);
		textViewAccZ = (TextView) findViewById(R.id.textViewAccZ);
		textViewCourseAfter = (TextView) findViewById(R.id.textViewCourseAfter);
		textViewCourseBefore = (TextView) findViewById(R.id.textViewCourseBefore);
		textViewDay = (TextView) findViewById(R.id.textViewDay);
		textViewMonth = (TextView) findViewById(R.id.textViewMonth);
		textViewYear = (TextView) findViewById(R.id.textViewYear);
		textViewFuel = (TextView) findViewById(R.id.textViewFuel);
		textViewHour = (TextView) findViewById(R.id.textViewHour);
		textViewMinute = (TextView) findViewById(R.id.textViewMinute);
		textViewSecond = (TextView) findViewById(R.id.textViewSecond);
		textViewGpsAltAfter = (TextView) findViewById(R.id.textViewGpsAltAfter);
		textViewGpsAltBefore = (TextView) findViewById(R.id.textViewGpsAltBefore);
		textViewSpeedAfter = (TextView) findViewById(R.id.textViewSpeedAfter);
		textViewSpeedBefore = (TextView) findViewById(R.id.textViewSpeedBefore);
		textViewLatAfter = (TextView) findViewById(R.id.textViewLatAfter);
		textViewLatBefore = (TextView) findViewById(R.id.textViewLatBefore);
		textViewLonAfter = (TextView) findViewById(R.id.textViewLonAfter);
		textViewLonBefore = (TextView) findViewById(R.id.textViewLonBefore);
		textViewNS = (TextView) findViewById(R.id.textViewNS);
		textViewEW = (TextView) findViewById(R.id.textViewEW);
		textViewTemp1 = (TextView) findViewById(R.id.textViewTemp1);
		textViewTemp2 = (TextView) findViewById(R.id.textViewTemp2);
		textViewVoltCell1 = (TextView) findViewById(R.id.textViewVoltCell1);
		textViewVoltCell2 = (TextView) findViewById(R.id.textViewVoltCell2);
		textViewVoltCell3 = (TextView) findViewById(R.id.textViewVoltCell3);
		textViewVoltCell4 = (TextView) findViewById(R.id.textViewVoltCell4);
		textViewVoltCell5 = (TextView) findViewById(R.id.textViewVoltCell5);
		textViewVoltCell6 = (TextView) findViewById(R.id.textViewVoltCell6);
	}

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// updateUI(intent);
			updateValues(intent);
		}
	};

	private void updateValues(Intent intent) {
		String channelType = intent.getStringExtra(FIELD_CHANNEL);
		double value = intent.getDoubleExtra(FIELD_VALUE, 0);
		sensorValues.put(ChannelTypes.valueOf(channelType), value);
	}

	@Override
	public void onResume() {
		super.onResume();
		// startService(broadcastIntent);
		registerReceiver(broadcastReceiver, new IntentFilter(
				FrSkyServer.BROADCAST_ACTION_HUB_DATA));
		// update prefs
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		FrSkyHub.getInstance().setNrOfPropBlades(Integer.parseInt(prefs.getString("rpm_blades", "2")));
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

	/**
	 * actual update of the GUI with new value for given channeltype
	 * 
	 * @param type
	 * @param value
	 */
	private void updateUI(ChannelTypes type, double value) {
		// switch based on type of channel
		switch (type) {
		case altitude_before:
			textViewAltBefore.setText(intFormat.format(value));
			break;
		case altitude_after:
			textViewAltAfter.setText(intFormat.format(value));
			break;
		case rpm:
			textViewRpm.setText(intFormat.format(value));
			break;
		case acc_x:
			textViewAccX.setText(intFormat.format(value));
			break;
		case acc_y:
			textViewAccY.setText(intFormat.format(value));
			break;
		case acc_z:
			textViewAccZ.setText(intFormat.format(value));
			break;
		case course_after:
			textViewCourseAfter.setText(intFormat.format(value));
			break;
		case course_before:
			textViewCourseBefore.setText(intFormat.format(value));
			break;
		case day:
			textViewDay.setText(intFormat.format(value));
			break;
		case month:
			textViewMonth.setText(intFormat.format(value));
			break;
		case year:
			textViewYear.setText(intFormat.format(value));
			break;
		case fuel:
			textViewFuel.setText(intFormat.format(value));
			break;
		case hour:
			textViewHour.setText(intFormat.format(value));
			break;
		case minute:
			textViewMinute.setText(intFormat.format(value));
			break;
		case second:
			textViewSecond.setText(intFormat.format(value));
			break;
		case gps_altitude_after:
			textViewGpsAltAfter.setText(intFormat.format(value));
			break;
		case gps_altitude_before:
			textViewGpsAltBefore.setText(intFormat.format(value));
			break;
		case gps_speed_after:
			textViewSpeedAfter.setText(intFormat.format(value));
			break;
		case gps_speed_before:
			textViewSpeedBefore.setText(intFormat.format(value));
			break;
		case latitude_after:
			textViewLatAfter.setText(intFormat.format(value));
			break;
		case latitude_before:
			textViewLatBefore.setText(intFormat.format(value));
			break;
		case longitude_after:
			textViewLonAfter.setText(intFormat.format(value));
			break;
		case longitude_before:
			textViewLonBefore.setText(intFormat.format(value));
			break;
		case temp1:
			textViewTemp1.setText(intFormat.format(value));
			break;
		case temp2:
			textViewTemp2.setText(intFormat.format(value));
			break;
		// for voltage values per cell
		case volt_0:
			textViewVoltCell1.setText(decFormat.format(value));
			break;
		case volt_1:
			textViewVoltCell2.setText(decFormat.format(value));
			break;
		case volt_2:
			textViewVoltCell3.setText(decFormat.format(value));
			break;
		case volt_3:
			textViewVoltCell4.setText(decFormat.format(value));
			break;
		case volt_4:
			textViewVoltCell5.setText(decFormat.format(value));
			break;
		case volt_5:
			textViewVoltCell6.setText(decFormat.format(value));
			break;
		case ew:
			textViewEW.setText(intFormat.format(value));
			break;
		case ns:
			textViewNS.setText(intFormat.format(value));
			break;
		default:
			// TODO update other fields (NE, WS from gps? new current sensors?)
			Logger.d(this.getClass().getName(),
					"non implemented display of channel type: " + type);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		// example of adding an item to the options menu, here for opening
		// preferences
		menu.add(0, ACTIVITY_PREFERENCES, 1, R.string.settings_label);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		boolean bla = super.onMenuItemSelected(featureId, item);

		switch (item.getItemId()) {
		// link the preference option to the preference activity
		case ACTIVITY_PREFERENCES:
			startActivity(new Intent(this, EditPreferences.class));
			return (true);
		}

		return bla;
	}

}
