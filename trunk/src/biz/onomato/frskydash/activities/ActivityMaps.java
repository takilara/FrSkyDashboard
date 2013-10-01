/*
 * Copyright 2011-2013, Espen Solbu
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

import java.util.Calendar;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMapOptions;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.VisibleRegion;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.WindowManager;
import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.fragments.FragmentStatus;
import biz.onomato.frskydash.fragments.FragmentStatus.OnStatusFragmentInteraction;
import biz.onomato.frskydash.hub.FrSkyHub;
import biz.onomato.frskydash.util.Logger;



public class ActivityMaps extends ActivityBase implements OnStatusFragmentInteraction  {

	// FIXME: Decide how to access channels
	// FrSkyServer.getCurrentModel().getHub().getChannels().values()
	// or 
	// using connection to server
	
	private static final String TAG = "Maps";
	private static final long UPDATE_MAP_DELAY = 200;
	
	private CameraPosition cameraPosition;
	
	private Calendar date = Calendar.getInstance();
	private double zerotimestamp = 0.0;
	
	private double latitude,longitude,gpstime;
	private double oldgpstime=0.0;
	private long lastGpsTimestampAt=0;
	private LatLng modelPosition;
	private Marker modelMarker;
	//private FrSkyServer server = null;
	
	private Handler tickHandler;
	private Runnable runnableTick;
	
	public boolean gpsLock=false;
	
	private GoogleMap mMap = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Logger.i(TAG, "onCreate");
		
		date.set(Calendar.YEAR, 2000);
		zerotimestamp = date.getTimeInMillis()/1000.0;
		
		
		setContentView(R.layout.activity_maps);
		
		
		FragmentManager fragmentManager = getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
		FragmentStatus fragment = new FragmentStatus();
		//fragmentTransaction.add(R.id.llDashboardFull, fragment);
		fragmentTransaction.replace(R.id.status_fragment_placeholder,fragment);
		fragmentTransaction.commit();

		
		
		
		//mv = (MapView) findViewById(R.id.map);
		Logger.w(TAG, "Find the Fragment");
		if (mMap == null) {
			Logger.w(TAG, "Fragment not instantiated, try to hook into it");
			mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
			if (mMap != null) {
				// The Map is verified. It is now safe to manipulate the map.
				Logger.w(TAG, "MAP READY FOR MANIPULATION!!!");
				mMap.setMyLocationEnabled(true);
				
				LocationManager service = (LocationManager) getSystemService(LOCATION_SERVICE);
				Criteria criteria = new Criteria();
				String provider = service.getBestProvider(criteria, false);
				Location location = service.getLastKnownLocation(provider);
				LatLng userLocation = new LatLng(location.getLatitude(),location.getLongitude());
				
				cameraPosition = new CameraPosition.Builder()
			    .target(userLocation)      // Sets the center of the map to the last known user position
			    .zoom(15)                   // Sets the zoom
			    //.bearing(0)                // Sets the orientation of the camera to east
			    //.tilt(30)                   // Sets the tilt of the camera to 30 degrees
			    .build();                   // Creates a CameraPosition from the builder
				//mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));	
				mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
				
			}
			else
			{
				Logger.e(TAG,"Map NOT ready for manipulation");
			}
		}
		
		
		tickHandler = new Handler();
		tickHandler.postDelayed(runnableTick, UPDATE_MAP_DELAY);
		runnableTick = new Runnable() {
			@Override
			public void run() {
				Logger.i(TAG,"Update Map");
				if ((server != null) && (FrSkyServer.getCurrentModel().getHub()!=null)) {
					//Logger.w(TAG, "Polling channels");
//					Logger.w(TAG, "Get currentModel: "+FrSkyServer.getCurrentModel());
//					Logger.w(TAG, "Get currentModel.getHub(): "+FrSkyServer.getCurrentModel().getHub());
//					Logger.w(TAG, "Get Channel, LAT: "+FrSkyServer.getCurrentModel().getHub().getChannel(FrSkyHub.CHANNEL_ID_GPS_LATITUDE));
//					
					latitude = FrSkyServer.getCurrentModel().getHub().getChannel(FrSkyHub.CHANNEL_ID_GPS_LATITUDE).getValue();
					longitude = FrSkyServer.getCurrentModel().getHub().getChannel(FrSkyHub.CHANNEL_ID_GPS_LONGITUDE).getValue();
					gpstime = FrSkyServer.getCurrentModel().getHub().getChannel(FrSkyHub.CHANNEL_ID_GPS_TIMESTAMP).getValue();
					
					// check for GPS lock
					if((gpstime!=zerotimestamp) && (latitude!=-1.0) && (longitude != -1.0))
					{
						gpsLock=true;
					}
					else
					{
						gpsLock=false;
					}
					Logger.w(TAG, "GPS Timestamp: "+gpstime);
					Logger.w(TAG, "GPS Locked: "+gpsLock);
					
					
					if(gpsLock)
					{
						updateModelMarker(latitude,longitude);
					}
					checkForStale();

				
				}

				tickHandler.postDelayed(this, UPDATE_MAP_DELAY);
			}
		};
		
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON|WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		doBindService();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		Logger.i(TAG, "onResume");
		//registerReceiver(mIntentServerReceiver, mIntentServerFilter); // Used to capture server messages relevant for all activities
		
		tickHandler.removeCallbacks(runnableTick);
		tickHandler.post(runnableTick);

	}
	
	@Override
	public void onPause() {
		super.onPause();
		Logger.i(TAG, "onPause");
		//unregisterReceiver(mIntentServerReceiver);
		tickHandler.removeCallbacks(runnableTick);
	}
	
//	protected void doBindService() {
//		Logger.i(TAG, "Start the server service if it is not already started");
//		startService(new Intent(this, FrSkyServer.class));
//		Logger.i(TAG, "Try to bind to the service");
//		getApplicationContext().bindService(
//				new Intent(this, FrSkyServer.class), mConnection, 0);
//	}
	
//	private final ServiceConnection mConnection = new ServiceConnection() {
//		public final void onServiceConnected(ComponentName className, IBinder binder) {
//			Logger.i(TAG, "Bound to Service");
//			server = ((FrSkyServer.MyBinder) binder).getService();
//			onServerConnected();
//		}
//		
//		
//
//		public final void onServiceDisconnected(ComponentName className) {
//			onServerDisconnected();
//			server = null;
//		}
//	};
	
	@Override
	void onServerConnected() {
		// TODO Auto-generated method stub
		// start cyclic polling
		onResume();
		
	}
	
	@Override
	void onServerDisconnected() {
		// TODO Auto-generated method stub
		// stop cyclic polling
		
	}
	

	public void updateModelMarker(double lat, double lng)
	{
		modelPosition = new LatLng(lat,lng);
		if(modelMarker==null)
		{
			Logger.e(TAG, "Setup initial state of map");
			cameraPosition = new CameraPosition.Builder()
		    .target(modelPosition)      // Sets the center of the map to Mountain View
		    .zoom(15)                   // Sets the zoom
		    //.bearing(0)                // Sets the orientation of the camera to east
		    //.tilt(30)                   // Sets the tilt of the camera to 30 degrees
		    .build();                   // Creates a CameraPosition from the builder
			//mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));	
			mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
		
		

			
			modelMarker = mMap.addMarker(new MarkerOptions().position(modelPosition));
		}
		else
		{
			
			modelMarker.setPosition(modelPosition);
			
			VisibleRegion region =mMap.getProjection().getVisibleRegion();
			LatLngBounds r = region.latLngBounds;
			
			if(r.contains(modelPosition) == false)
			{
				Logger.e(TAG, "Model position is not visible, move the map");
				cameraPosition = new CameraPosition.Builder()
			    .target(modelPosition)      // Sets the center of the map to Mountain View
			    .zoom(15)                   // Sets the zoom
			    //.bearing(0)                // Sets the orientation of the camera to east
			    //.tilt(30)                   // Sets the tilt of the camera to 30 degrees
			    .build();                   // Creates a CameraPosition from the builder

				mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
			}
			else
			{
				//Logger.e(TAG, "Model position is visible");
			}
		}
		
		
		
	}
	
	public boolean checkForStale()
	{// Check for stale
		Calendar c = Calendar.getInstance();
		if(gpstime != oldgpstime)
		{
			
			lastGpsTimestampAt = c.getTimeInMillis();
		}
		else
		{
			if(c.getTimeInMillis()-lastGpsTimestampAt>5000)
			{
				Logger.w(TAG, "GPS Timestamp has not updated for 5 seconds. STALE");
				return true;
			}
		}
		oldgpstime = gpstime;
		return false;
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

	
}
