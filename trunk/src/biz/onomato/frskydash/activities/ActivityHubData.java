package biz.onomato.frskydash.activities;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.hub.EditPreferences;
import biz.onomato.frskydash.hub.SensorTypes;
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
	private HashMap<SensorTypes, Double> sensorValues = new HashMap<SensorTypes, Double>();

	/**
	 * some user specific settings
	 */
	private int nrOfBlades = 2;

	/**
	 * offset for height set by user, always init as zero
	 */
	private double altitudeOffset = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// set layout
		setContentView(R.layout.activity_hubdata);
		// register the receiving intent
		broadcastIntent = new Intent(this, FrSkyServer.class);

		// init all required fields here for performance
		initTextFields();

		Button zeroAlt = (Button) findViewById(R.id.buttonZeroAltitude);
		zeroAlt.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View paramView) {
				// Auto-generated method stub
				altitudeOffset = Double.parseDouble(textViewAlt.getText().toString().replace(",", "."));
			}
		});
		Button removeOffsetAlt = (Button) findViewById(R.id.buttonRemoveAltOffset);
		removeOffsetAlt.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View paramView) {
				// Auto-generated method stub
				altitudeOffset = 0;
			}
		});

		// a thread responsible for updating the values on the gui
		new Thread(new Runnable() {
			public void run() {
				// infinite loop
				while (true) {
					// iterate values
					for (final SensorTypes type : sensorValues.keySet()) {
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

	private TextView textViewAlt, textViewRpm, textViewAccX, textViewAccY,
			textViewAccZ, textViewDay, textViewMonth, textViewYear,
			textViewFuel, textViewHour, textViewMinute, textViewSecond,
			textViewGpsAlt, textViewSpeed, textViewCourse, textViewLat,
			textViewLon, textViewTemp1, textViewTemp2, textViewVoltCell2,
			textViewVoltCell1, textViewVoltCell3, textViewVoltCell4,
			textViewVoltCell5, textViewVoltCell6, textViewNS, textViewEW;

	/**
	 * init all the text fields only once on create of activity
	 */
	private void initTextFields() {
		textViewAlt = (TextView) findViewById(R.id.textViewAlt);
		textViewRpm = (TextView) findViewById(R.id.textViewRpm);
		textViewAccX = (TextView) findViewById(R.id.textViewAccX);
		textViewAccY = (TextView) findViewById(R.id.textViewAccY);
		textViewAccZ = (TextView) findViewById(R.id.textViewAccZ);
		textViewCourse = (TextView) findViewById(R.id.textViewCourse);
		textViewDay = (TextView) findViewById(R.id.textViewDay);
		textViewMonth = (TextView) findViewById(R.id.textViewMonth);
		textViewYear = (TextView) findViewById(R.id.textViewYear);
		textViewFuel = (TextView) findViewById(R.id.textViewFuel);
		textViewHour = (TextView) findViewById(R.id.textViewHour);
		textViewMinute = (TextView) findViewById(R.id.textViewMinute);
		textViewSecond = (TextView) findViewById(R.id.textViewSecond);
		textViewGpsAlt = (TextView) findViewById(R.id.textViewGpsAlt);
		textViewSpeed = (TextView) findViewById(R.id.textViewSpeed);
		textViewLat = (TextView) findViewById(R.id.textViewLat);
		textViewLon = (TextView) findViewById(R.id.textViewLon);
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
		sensorValues.put(SensorTypes.valueOf(channelType), value);
	}

	@Override
	public void onResume() {
		super.onResume();
		// startService(broadcastIntent);
		registerReceiver(broadcastReceiver, new IntentFilter(
				FrSkyServer.BROADCAST_ACTION_HUB_DATA));
		// update prefs
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		nrOfBlades = Integer.parseInt(prefs.getString("rpm_blades", "2"));

		// also update visibility of sensor data
		// FIXME this way still updating in the background
		findViewById(R.id.fgs).setVisibility(
				prefs.getBoolean("visible_fgs", true) ? View.VISIBLE
						: View.GONE);
		findViewById(R.id.flvs).setVisibility(
				prefs.getBoolean("visible_flvs", true) ? View.VISIBLE
						: View.GONE);
		findViewById(R.id.rpms).setVisibility(
				prefs.getBoolean("visible_rpms", true) ? View.VISIBLE
						: View.GONE);
		findViewById(R.id.temp1).setVisibility(
				prefs.getBoolean("visible_tems1", true) ? View.VISIBLE
						: View.GONE);
		findViewById(R.id.temp2).setVisibility(
				prefs.getBoolean("visible_tems2", true) ? View.VISIBLE
						: View.GONE);
		findViewById(R.id.tas).setVisibility(
				prefs.getBoolean("visible_tas", true) ? View.VISIBLE
						: View.GONE);
		findViewById(R.id.gps).setVisibility(
				prefs.getBoolean("visible_gps", true) ? View.VISIBLE
						: View.GONE);
		findViewById(R.id.fvas).setVisibility(
				prefs.getBoolean("visible_fvas", true) ? View.VISIBLE
						: View.GONE);
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
	 * a date to keep collecting date information from sensors
	 */
	private Calendar calendar = Calendar.getInstance();

	/**
	 * actual update of the GUI with new value for given channeltype
	 * 
	 * @param type
	 * @param value
	 */
	private void updateUI(SensorTypes type, double value) {
		// switch based on type of channel
		switch (type) {
		case altitude_before:
			textViewAlt.setText(decFormat.format(value-altitudeOffset));
			break;
		case altitude_after:
			textViewAlt.setText(decFormat.format(value-altitudeOffset));
			break;
		case rpm:
			textViewRpm.setText(intFormat.format(value/nrOfBlades));
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
			textViewCourse.setText(decFormat.format(value));
			break;
		case course_before:
			textViewCourse.setText(decFormat.format(value));
			break;
		case day_month:
			// this is a long now representing date
			calendar.setTimeInMillis((long)value);
			textViewDay.setText(calendar.get(Calendar.DAY_OF_MONTH));
			textViewMonth.setText(calendar.get(Calendar.MONTH)+1);
			break;
		case year:
			// this is a long now representing date
			calendar.setTimeInMillis((long)value);
			textViewYear.setText(calendar.get(Calendar.YEAR));
			break;
		case fuel:
			textViewFuel.setText(intFormat.format(value));
			break;
		case hour_minute:
			// this is a long now representing date
			calendar.setTimeInMillis((long)value);
			textViewHour.setText(calendar.get(Calendar.HOUR));
			textViewMinute.setText(calendar.get(Calendar.MINUTE));
			break;
		case second:
			// this is a long now representing date
			calendar.setTimeInMillis((long)value);
			textViewSecond.setText(calendar.get(Calendar.SECOND));
			break;
		case gps_altitude_after:
			textViewGpsAlt.setText(decFormat.format(value));
			break;
		case gps_altitude_before:
			textViewGpsAlt.setText(decFormat.format(value));
			break;
		case gps_speed_after:
			textViewSpeed.setText(decFormat.format(value));
			break;
		case gps_speed_before:
			textViewSpeed.setText(decFormat.format(value));
			break;
		case latitude_after:
			textViewLat.setText(decFormat.format(value));
			break;
		case latitude_before:
			textViewLat.setText(decFormat.format(value));
			break;
		case longitude_after:
			textViewLon.setText(decFormat.format(value));
			break;
		case longitude_before:
			textViewLon.setText(decFormat.format(value));
			break;
		case temp1:
			textViewTemp1.setText(intFormat.format(value));
			break;
		case temp2:
			textViewTemp2.setText(intFormat.format(value));
			break;
		// for voltage values per cell
		case CELL_0:
			textViewVoltCell1.setText(decFormat.format(value));
			break;
		case CELL_1:
			textViewVoltCell2.setText(decFormat.format(value));
			break;
		case CELL_2:
			textViewVoltCell3.setText(decFormat.format(value));
			break;
		case CELL_3:
			textViewVoltCell4.setText(decFormat.format(value));
			break;
		case CELL_4:
			textViewVoltCell5.setText(decFormat.format(value));
			break;
		case CELL_5:
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
