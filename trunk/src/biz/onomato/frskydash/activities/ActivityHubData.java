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

package biz.onomato.frskydash.activities;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TreeMap;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.domain.Channel;
import biz.onomato.frskydash.fragments.FragmentStatus;
import biz.onomato.frskydash.fragments.FragmentStatus.OnStatusFragmentInteraction;
import biz.onomato.frskydash.hub.EditPreferences;
import biz.onomato.frskydash.hub.FrSkyHub;
import biz.onomato.frskydash.hub.SensorTypes;
import biz.onomato.frskydash.util.Logger;

/**
 * Activity for displaying sensor hub data
 * 
 * @author hcpl
 * 
 */
public class ActivityHubData extends ActivityBase implements OnStatusFragmentInteraction  {

	private static final int INTERVAL_GUI_UPDATE = 200;
	public static final String FIELD_VALUE = "value";
	public static final String FIELD_SENSORTYPE = "channel-type";
	private static final int ACTIVITY_PREFERENCES = 1;
	private static final String TAG = "ActivityHub";
	
	// Used for GUI updates
	private Handler tickHandler;
	private Runnable runnableTick;


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
		//getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		setContentView(R.layout.activity_hubdata);
		
		
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		FragmentStatus fragment = new FragmentStatus();
		//fragmentTransaction.add(R.id.llDashboardFull, fragment);
		fragmentTransaction.replace(R.id.status_fragment_placeholder,fragment);
		fragmentTransaction.commit();


		// init all required fields here for performance
		initTextFields();

//		Button zeroAlt = (Button) findViewById(R.id.buttonZeroAltitude);
//		zeroAlt.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View paramView) {
//				// Auto-generated method stub
//				altitudeOffset = Double.parseDouble(textViews.get(FrSkyHub.CHANNEL_ID_ALTITUDE).getText().toString().replace(",", "."));
//			}
//		});
//		Button removeOffsetAlt = (Button) findViewById(R.id.buttonRemoveAltOffset);
//		removeOffsetAlt.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View paramView) {
//				// Auto-generated method stub
//				altitudeOffset = 0;
//			}
//		});

		// Update GUI cyclically
		tickHandler = new Handler();
		tickHandler.postDelayed(runnableTick, INTERVAL_GUI_UPDATE);
		runnableTick = new Runnable() {
			@Override
			public void run() {
//				for (final SensorTypes type : sensorValues.keySet()) {
//					updateUI(type, sensorValues.get(type));
//				}
				updateValues();
				tickHandler.postDelayed(this, INTERVAL_GUI_UPDATE);
			}
		};

		
		// a thread responsible for updating the values on the gui
//		new Thread(new Runnable() {
//			public void run() {
//				// infinite loop
//				while (true) {
//					// iterate values
//					for (final SensorTypes type : sensorValues.keySet()) {
//						// back to GUI
//						runOnUiThread(new Runnable() {
//							public void run() {
//								updateUI(type, sensorValues.get(type));
//							}
//						});
//					}
//					// and repeat this on interval
//					try {
//						Thread.sleep(INTERVAL_GUI_UPDATE);
//					} catch (InterruptedException e) {
//						Logger.e(ActivityHubData.this.getClass().toString(),
//								"Sleep for interval GUI update interrupted", e);
//					}
//				}
//			}
//		}).start();
	}

	private TextView 
			//textViewAlt, textViewRpm, textViewAccX, textViewAccY,
			//textViewAccZ, 
			//textViewDay, textViewMonth, textViewYear,
			//textViewFuel, 
			//textViewHour, textViewMinute, textViewSecond,
			//textViewGpsAlt, textViewSpeed, textViewCourse, textViewLat,
			//textViewLon, textViewTemp1, textViewTemp2, textViewVoltCell2,
			//textViewVoltCell1, textViewVoltCell3, textViewVoltCell4,
			//textViewVoltCell5, textViewVoltCell6, 
			//textViewNS, textViewEW;
			textViewDate,textViewTime,textViewTimeStamp;
	
	TreeMap<Integer, TextView> textViews = new TreeMap<Integer,TextView>();

	/**
	 * init all the text fields only once on create of activity
	 */
	private void initTextFields() {
		textViews.put(FrSkyHub.CHANNEL_ID_ALTITUDE,(TextView) findViewById(R.id.textViewAlt));
		textViews.put(FrSkyHub.CHANNEL_ID_VERT_SPEED,(TextView) findViewById(R.id.textViewVertSpeed));
		textViews.put(FrSkyHub.CHANNEL_ID_RPM,(TextView) findViewById(R.id.textViewRpm));
		textViews.put(FrSkyHub.CHANNEL_ID_ACCELEROMETER_X,(TextView) findViewById(R.id.textViewAccX));
		textViews.put(FrSkyHub.CHANNEL_ID_ACCELEROMETER_Y,(TextView) findViewById(R.id.textViewAccY));
		textViews.put(FrSkyHub.CHANNEL_ID_ACCELEROMETER_Z,(TextView) findViewById(R.id.textViewAccZ));
		textViews.put(FrSkyHub.CHANNEL_ID_GPS_COURSE,(TextView) findViewById(R.id.textViewCourse));
		textViews.put(FrSkyHub.CHANNEL_ID_FUEL,(TextView) findViewById(R.id.textViewFuel));
		textViews.put(FrSkyHub.CHANNEL_ID_GPS_ALTITUDE,(TextView) findViewById(R.id.textViewGpsAlt));
		textViews.put(FrSkyHub.CHANNEL_ID_GPS_SPEED,(TextView) findViewById(R.id.textViewSpeed));
		textViews.put(FrSkyHub.CHANNEL_ID_GPS_LATITUDE,(TextView) findViewById(R.id.textViewLat));
		textViews.put(FrSkyHub.CHANNEL_ID_GPS_LONGITUDE,(TextView) findViewById(R.id.textViewLon));
		textViews.put(FrSkyHub.CHANNEL_ID_TEMP1,(TextView) findViewById(R.id.textViewTemp1));
		textViews.put(FrSkyHub.CHANNEL_ID_TEMP2,(TextView) findViewById(R.id.textViewTemp2));
		
		
		textViews.put(FrSkyHub.CHANNEL_ID_LIPO_CELL_1,(TextView) findViewById(R.id.textViewVoltCell1));
		textViews.put(FrSkyHub.CHANNEL_ID_LIPO_CELL_2,(TextView) findViewById(R.id.textViewVoltCell2));
		textViews.put(FrSkyHub.CHANNEL_ID_LIPO_CELL_3,(TextView) findViewById(R.id.textViewVoltCell3));
		textViews.put(FrSkyHub.CHANNEL_ID_LIPO_CELL_4,(TextView) findViewById(R.id.textViewVoltCell4));
		textViews.put(FrSkyHub.CHANNEL_ID_LIPO_CELL_5,(TextView) findViewById(R.id.textViewVoltCell5));
		textViews.put(FrSkyHub.CHANNEL_ID_LIPO_CELL_6,(TextView) findViewById(R.id.textViewVoltCell6));
		
		textViews.put(FrSkyHub.CHANNEL_ID_FAS_VOLTAGE,(TextView) findViewById(R.id.textViewFasVoltage));
		textViews.put(FrSkyHub.CHANNEL_ID_FAS_CURRENT,(TextView) findViewById(R.id.textViewFasCurrent));
		
		
		textViews.put(FrSkyHub.CHANNEL_ID_OPENXVARIO_VFAS_VOLTAGE,(TextView) findViewById(R.id.textViewOpenXVarioVoltage));
		textViews.put(FrSkyHub.CHANNEL_ID_OPENXVARIO_GPS_DISTANCE,(TextView) findViewById(R.id.textViewOpenXVarioDist));
		textViews.put(FrSkyHub.CHANNEL_ID_GPS_TIMESTAMP,(TextView) findViewById(R.id.textViewTimeStamp));

		// not yet supported
		//textViews.put(FrSkyHub.CHANNEL_ID_GPS_NS,(TextView) findViewById(R.id.textViewNS));
		//textViews.put(FrSkyHub.CHANNEL_ID_GPS_EW,(TextView) findViewById(R.id.textViewEW));
		//textViews.put(FrSkyHub.CHANNEL_ID_DAY,(TextView) findViewById(R.id.textViewDay));
		//textViews.put(FrSkyHub.CHANNEL_ID_MONTH,(TextView) findViewById(R.id.textViewMonth));
		//textViews.put(FrSkyHub.CHANNEL_ID_YEAR,(TextView) findViewById(R.id.textViewYear));		
		//textViews.put(FrSkyHub.CHANNEL_ID_HOUR,(TextView) findViewById(R.id.textViewHour));
		//textViews.put(FrSkyHub.CHANNEL_ID_MINUTE,(TextView) findViewById(R.id.textViewMinute));
		//textViews.put(FrSkyHub.CHANNEL_ID_SECOND,(TextView) findViewById(R.id.textViewSecond));

		//textViewDate = (TextView) findViewById(R.id.textViewDate);
		//textViewTime = (TextView) findViewById(R.id.textViewTime);
		
		// old
//		textViewYear = (TextView) findViewById(R.id.textViewYear);
//		textViewMonth = (TextView) findViewById(R.id.textViewMonth);
//		textViewDay = (TextView) findViewById(R.id.textViewDay);
//		textViewHour = (TextView) findViewById(R.id.textViewHour);
//		textViewMinute = (TextView) findViewById(R.id.textViewMinute);
//		textViewSecond = (TextView) findViewById(R.id.textViewSecond);

		
		//textViewNS = (TextView) findViewById(R.id.textViewNS);
		//textViewEW = (TextView) findViewById(R.id.textViewEW);

		//textViewAccX = (TextView) findViewById(R.id.textViewAccX);
		//textViewAccY = (TextView) findViewById(R.id.textViewAccY);
		//textViewAccZ = (TextView) findViewById(R.id.textViewAccZ);
		//textViewCourse = (TextView) findViewById(R.id.textViewCourse);
		//textViewFuel = (TextView) findViewById(R.id.textViewFuel);
		//textViewGpsAlt = (TextView) findViewById(R.id.textViewGpsAlt);
		//textViewSpeed = (TextView) findViewById(R.id.textViewSpeed);
		//textViewLon = (TextView) findViewById(R.id.textViewLon);
		//textViewLat = (TextView) findViewById(R.id.textViewLat);
		//textViewRpm = (TextView) findViewById(R.id.textViewRpm);
		//textViewTemp1 = (TextView) findViewById(R.id.textViewTemp1);
		//textViewTemp2 = (TextView) findViewById(R.id.textViewTemp2);
		//textViewVoltCell1 = (TextView) findViewById(R.id.textViewVoltCell1);
		//textViewVoltCell2 = (TextView) findViewById(R.id.textViewVoltCell2);
		//textViewVoltCell3 = (TextView) findViewById(R.id.textViewVoltCell3);
		//textViewVoltCell4 = (TextView) findViewById(R.id.textViewVoltCell4);
		//textViewVoltCell5 = (TextView) findViewById(R.id.textViewVoltCell5);
		//textViewVoltCell6 = (TextView) findViewById(R.id.textViewVoltCell6);
	}



	@Override
	public void onResume() {
		super.onResume();
		
		tickHandler.removeCallbacks(runnableTick);
		tickHandler.post(runnableTick);

		if(FrSkyServer.getCurrentModel().getHub()==null)
		{
			Toast.makeText(this,
					"Current model has not set a Hub. Values will not update",
					Toast.LENGTH_LONG).show();
		}
		

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
		
		tickHandler.removeCallbacks(runnableTick);
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

	private void updateValues()
	{
		if(FrSkyServer.getCurrentModel().getHub()!=null) // only try to update channels if a hub is configured
		{
			for(Channel c : FrSkyServer.getCurrentModel().getHub().getChannels().values())
			{
				//TODO: Expand this, so that it can dynamically update channels
				Logger.i(TAG, c.getDescription()+":"+c.toValueString());
				
				
				try
				{
					switch (c.getId())
					{
	//				case FrSkyHub.CHANNEL_ID_ALTITUDE:
	//					double val = c.getValue()-altitudeOffset;
	//					textViews.get(c.getId()).setText(decFormat.format(val));
	//					break;
					//case FrSkyHub.CHANNEL_ID_ACCELEROMETER_X:
//						Logger.d(TAG, "Getting value from ACC_X: "+c.toValueString());
						//break;
					case FrSkyHub.CHANNEL_ID_RPM:
						double rpm = c.getValue()/nrOfBlades;
						textViews.get(c.getId()).setText(decFormat.format(rpm));
						break;
					case FrSkyHub.CHANNEL_ID_GPS_TIMESTAMP:
						calendar.setTimeInMillis((long)c.getValue()*1000);
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.US);
						//Logger.e(TAG, "Calendar: "+sdf.format(calendar.getTime()));
						textViews.get(c.getId()).setText(sdf.format(calendar.getTime()));
						break;
					default:
						textViews.get(c.getId()).setText(c.toValueString());
						break;
					}
					
				}
				catch (Exception e)
				{
					Logger.w(TAG, "Channel '"+c.getDescription()+"' Not implemented");
				}
				
			}
		}
		
		
		
	}
	
	/**
	 * actual update of the GUI with new value for given channeltype
	 * 
	 * @param sensorType
	 * @param value
	 */
	private void updateUI(SensorTypes sensorType, double value) {
		// switch based on type of channel
		switch (sensorType) {
		case altitude_before:
			//FIXME: The following two textViewAlt.setText's must be wrong. This textfield should only
			//be set once, and with the combined value of before.after.
			//textViewAlt.setText(decFormat.format(value-altitudeOffset));
			break;
		case altitude_after:
			//textViewAlt.setText(decFormat.format(value-altitudeOffset));
			break;
		case rpm:
			//FIXME: nrOfBlades should be property of a derived channel and not exist in hub at all
			//textViewRpm.setText(intFormat.format(value/nrOfBlades));
			break;
		case acc_x:
			//textViewAccX.setText(intFormat.format(value));
			break;
		case acc_y:
			//textViewAccY.setText(intFormat.format(value));
			break;
		case acc_z:
			//textViewAccZ.setText(intFormat.format(value));
			break;
		case gps_course_after:
			//textViewCourse.setText(decFormat.format(value));
			break;
		case gps_course_before:
			//textViewCourse.setText(decFormat.format(value));
			break;
		case gps_day_month:
			// this is a long now representing date
			//calendar.setTimeInMillis((long)value);
			//textViewDay.setText(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
			//textViewMonth.setText(String.valueOf(calendar.get(Calendar.MONTH)+1));
			break;
		case gps_year:
			// this is a long now representing date
			//calendar.setTimeInMillis((long)value);
			//textViewYear.setText(String.valueOf(calendar.get(Calendar.YEAR)));
			break;
		case fuel:
			//textViewFuel.setText(intFormat.format(value));
			break;
		case gps_hour_minute:
			// this is a long now representing date
			//calendar.setTimeInMillis((long)value);
			//textViewHour.setText(String.valueOf(calendar.get(Calendar.HOUR)));
			//textViewMinute.setText(String.valueOf(calendar.get(Calendar.MINUTE)));
			break;
		case gps_second:
			// this is a long now representing date
			//calendar.setTimeInMillis((long)value);
			//textViewSecond.setText(String.valueOf(calendar.get(Calendar.SECOND)));
			break;
		case gps_altitude_after:
			//textViewGpsAlt.setText(decFormat.format(value));
			break;
		case gps_altitude_before:
			//textViewGpsAlt.setText(decFormat.format(value));
			break;
		case gps_speed_after:
			//textViewSpeed.setText(decFormat.format(value));
			break;
		case gps_speed_before:
			//textViewSpeed.setText(decFormat.format(value));
			break;
		case gps_latitude_after:
			//textViewLat.setText(decFormat.format(value));
			break;
		case gps_latitude_before:
			//textViewLat.setText(decFormat.format(value));
			break;
		case gps_longitude_after:
			//textViewLon.setText(decFormat.format(value));
			break;
		case gps_longitude_before:
			//textViewLon.setText(decFormat.format(value));
			break;
		case temp1:
			//textViewTemp1.setText(intFormat.format(value));
			break;
		case temp2:
			//textViewTemp2.setText(intFormat.format(value));
			break;
		// for voltage values per cell
		case CELL_0:
			//textViewVoltCell1.setText(decFormat.format(value));
			break;
		case CELL_1:
			//textViewVoltCell2.setText(decFormat.format(value));
			break;
		case CELL_2:
			//textViewVoltCell3.setText(decFormat.format(value));
			break;
		case CELL_3:
			//textViewVoltCell4.setText(decFormat.format(value));
			break;
		case CELL_4:
			//textViewVoltCell5.setText(decFormat.format(value));
			break;
		case CELL_5:
			//textViewVoltCell6.setText(decFormat.format(value));
			break;
		case gps_longitude_ew:
			//textViewEW.setText(intFormat.format(value));
			break;
		case gps_latitude_ns:
			//textViewNS.setText(intFormat.format(value));
			break;
		default:
			// TODO update other fields (NE, WS from gps? new current sensors?)
			Logger.d(this.getClass().getName(),
					"non implemented display of channel type: " + sensorType);
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

	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.fragments.FragmentStatus.OnStatusFragmentInteraction#onConfigCurrentModel()
	 */
	@Override
	public void onConfigCurrentModel() {
		// TODO Auto-generated method stub
		Logger.i(TAG, "Edit current model");
		Intent i = new Intent(this, ActivityModelConfig.class);
		i.putExtra("modelId", FrSkyServer.getCurrentModel().getId());
		startActivity(i);
	}



	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.activities.ActivityBase#onCurrentModelChanged()
	 */
	@Override
	protected void onCurrentModelChanged() {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.activities.ActivityBase#onModelMapChanged()
	 */
	@Override
	protected void onModelMapChanged() {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.activities.ActivityBase#onServerConnected()
	 */
	@Override
	void onServerConnected() {
		// TODO Auto-generated method stub
		
	}



	/* (non-Javadoc)
	 * @see biz.onomato.frskydash.activities.ActivityBase#onServerDisconnected()
	 */
	@Override
	void onServerDisconnected() {
		// TODO Auto-generated method stub
		
	}
}
