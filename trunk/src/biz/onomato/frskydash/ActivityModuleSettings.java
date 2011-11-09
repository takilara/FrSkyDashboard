package biz.onomato.frskydash;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityModuleSettings extends Activity implements OnItemSelectedListener, OnClickListener {

	private static final String TAG = "FrSky-Settings";
	private FrSkyServer server;
    
	Spinner RSSIalarm1LevelSpinner;
	Spinner RSSIalarm1RelSpinner;
	Spinner RSSIalarm1ValueSpinner;
	
	private View btnRSSISend;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		setContentView(R.layout.activity_modulesettings);
		
		//stringlist for values
		int from = 20;
		int to = 110;
		String[] values = new String[to-from+1];
		for(int i=from;i<=to;i++)
		{
			values[i-from]=Integer.toString(i);
		}
		
		//FrSkySettings_spinner_level
		ArrayAdapter<CharSequence> RSSIalarm1LevelAdapter = ArrayAdapter.createFromResource(this, R.array.alarm_level, android.R.layout.simple_spinner_item );
		RSSIalarm1LevelAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
			 
		RSSIalarm1LevelSpinner = (Spinner) findViewById( R.id.FrSkySettings_RSSI_spinner_level );
		RSSIalarm1LevelSpinner.setAdapter( RSSIalarm1LevelAdapter );
		RSSIalarm1LevelSpinner.setOnItemSelectedListener(this);
		
		//FrSkySettings_spinner_relative
		ArrayAdapter<CharSequence> RSSIalarm1RelAdapter = ArrayAdapter.createFromResource(this, R.array.alarm_relative, android.R.layout.simple_spinner_item );
		RSSIalarm1RelAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
			 
		RSSIalarm1RelSpinner = (Spinner) findViewById( R.id.FrSkySettings_RSSI_spinner_relative );
		RSSIalarm1RelSpinner.setAdapter( RSSIalarm1RelAdapter );
		RSSIalarm1RelSpinner.setOnItemSelectedListener(this);
		
		
		//FrSkySettings_spinner_value
		//ArrayAdapter<CharSequence> RSSIalarm1ValueAdapter = ArrayAdapter.createFromResource(this, values, android.R.layout.simple_spinner_item );
		ArrayAdapter<String> RSSIalarm1ValueAdapter = new ArrayAdapter<String> (this, android.R.layout.simple_spinner_dropdown_item,values );
		//RSSIalarm1ValueAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
			 
		RSSIalarm1ValueSpinner = (Spinner) findViewById( R.id.FrSkySettings_RSSI_spinner_value );
		RSSIalarm1ValueSpinner.setAdapter( RSSIalarm1ValueAdapter );
		RSSIalarm1ValueSpinner.setOnItemSelectedListener(this);		
		
		
		btnRSSISend = findViewById(R.id.FrSkySettings_RSSI_send);
	    btnRSSISend.setOnClickListener(this);
				
		doBindService();
				
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		Log.i(TAG,"onResume");
		

	}
	
	

	void doBindService() {
		Log.i(TAG,"Start the server service if it is not already started");
		startService(new Intent(this, FrSkyServer.class));
		Log.i(TAG,"Try to bind to the service");
		getApplicationContext().bindService(new Intent(this, FrSkyServer.class), mConnection,0);
    }
    
    void doUnbindService() {
            if (server != null) {
            // Detach our existing connection.
	        	try {
	        		unbindService(mConnection);
	        	}
	        	catch (Exception e)
	        	{}
        }
    }
    
    
    

    
    private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			server = ((FrSkyServer.MyBinder) binder).getService();
			Log.i(TAG,"Bound to Service");
			
			// only enable send buttons if Bluetooth is connected
			if(server.getConnectionState()==BluetoothSerialService.STATE_CONNECTED)
			{
				btnRSSISend.setEnabled(true);
			}
			else
			{
				btnRSSISend.setEnabled(false);
			}
			
			Log.i(TAG,"Setup Alarms:");
			// RSSI alarms does not get written from module,
			// Defaults need to come from FrSky, or
			// from settings
			RSSIalarm1LevelSpinner.setSelection(Alarm.ALARMLEVEL_MID);
			RSSIalarm1RelSpinner.setSelection(Alarm.LESSERTHAN);
			// need to be updated to reflect item with value 45, not index 45...
			RSSIalarm1ValueSpinner.setSelection(45);
			if(server.RSSItx.alarmCount>0)
			{
				try
				{
					Log.i(TAG,"\tRSSI 1: "+server.RSSItx.alarms[0].toString());
					//Log.i(TAG,"\tRSSI 2: "+server.RSSItx.alarms[1].toString());
					Log.i(TAG,"Load RSSI alarm 1 from server:");
					Log.i(TAG,"Level: "+server.RSSItx.alarms[0].level+", greaterthan: "+server.RSSItx.alarms[0].greaterthan+", threshold: "+server.RSSItx.alarms[0].threshold);
					RSSIalarm1LevelSpinner.setSelection(server.RSSItx.alarms[0].level);
					RSSIalarm1RelSpinner.setSelection(server.RSSItx.alarms[0].greaterthan);
					//need to be updated to reflect item with value 45, not index 45...
					RSSIalarm1ValueSpinner.setSelection(server.RSSItx.alarms[0].threshold);
					
					

					}
					catch(Exception e)
					{
						Log.e(TAG,"Exception: "+e.getMessage());
					}
			}
			if(server.AD1.alarmCount>0)
			{
				Log.i(TAG,"\tAD1 1: "+server.AD1.alarms[0].toString());
				Log.i(TAG,"\tAD1 2: "+server.AD1.alarms[1].toString());
			}
			if(server.AD2.alarmCount>0)
			{
				Log.i(TAG,"\tAD2 1: "+server.AD2.alarms[0].toString());
				Log.i(TAG,"\tAD2 2: "+server.AD2.alarms[1].toString());
			}
			
		}

		public void onServiceDisconnected(ComponentName className) {
			server = null;
		}
	};



	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		// No point of doing anything here, put code in send button instead
		
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onClick(View v) {
		//Log.i(TAG,"Some button clicked");
    	switch (v.getId()) {
    		case R.id.FrSkySettings_RSSI_send:
    			Log.i(TAG,"Send RSSI alarm to FrSky module");
    			Frame f = Frame.AlarmFrame(
    					Frame.FRAMETYPE_ALARM1_RSSI, 
    					RSSIalarm1LevelSpinner.getSelectedItemPosition(), 
    					Integer.parseInt(RSSIalarm1ValueSpinner.getSelectedItem().toString()), 
    					RSSIalarm1RelSpinner.getSelectedItemPosition());
    			Log.i(TAG,"Send this frame to FrSkyModule: "+f.toHuman());
    			server.send(f);
    			// RSSI alarms frames should also be parsed outgoing.
    			Log.d(TAG,"Trying to send frame to server as well as output");
    			Log.d(TAG,"Alarm Level:"+f.alarmLevel);
    			server.parseFrame(f);
    			
    			break;
    	}
	}
}


