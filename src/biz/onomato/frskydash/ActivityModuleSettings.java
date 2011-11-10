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
	
	
    
	Spinner RSSIalarm1LevelSpinner,RSSIalarm2LevelSpinner,AD1alarm1LevelSpinner,AD1alarm2LevelSpinner,AD2alarm1LevelSpinner,AD2alarm2LevelSpinner;
	Spinner RSSIalarm1RelSpinner,RSSIalarm2RelSpinner,AD1alarm1RelSpinner,AD1alarm2RelSpinner,AD2alarm1RelSpinner,AD2alarm2RelSpinner;
	Spinner RSSIalarm1ValueSpinner,RSSIalarm2ValueSpinner,AD1alarm1ValueSpinner,AD1alarm2ValueSpinner,AD2alarm1ValueSpinner,AD2alarm2ValueSpinner;
	
	private View btnRSSI1Send,btnRSSI2Send,btnAD1_1_Send,btnAD1_2_Send,btnAD2_1_Send,btnAD2_2_Send;
	private TextView tvAD1_1_human;
	
	int minThresholdRSSI=20;
	int maxThresholdRSSI=110;
	int minThresholdAD=1;
	int maxThresholdAD=255;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		
		setContentView(R.layout.activity_modulesettings);
		
		//stringlist for values
		
		// RSSI Alarm 1
		//minThresholdRSSI = 20;
		//maxThresholdRSSI = 110;
		String[] valuesRSSI = new String[maxThresholdRSSI-minThresholdRSSI+1];
		for(int i=minThresholdRSSI;i<=maxThresholdRSSI;i++)
		{
			valuesRSSI[i-minThresholdRSSI]=Integer.toString(i);
		}
		
		//minThresholdAD = 1;
		//maxThresholdAD = 255;
		String[] valuesADx = new String[maxThresholdAD-minThresholdAD+1];
		for(int i=minThresholdAD;i<=maxThresholdAD;i++)
		{
			valuesADx[i-minThresholdAD]=Integer.toString(i);
		}
		
		
		// Spinner contents
		// Alarm Level, Off, Low, Mid, High
		ArrayAdapter<CharSequence> alarmLevelAdapter = ArrayAdapter.createFromResource(this, R.array.alarm_level, android.R.layout.simple_spinner_item );
		// Alarm relativity: Greater than, Lower than
		ArrayAdapter<CharSequence> alarmRelAdapter = ArrayAdapter.createFromResource(this, R.array.alarm_relative, android.R.layout.simple_spinner_item );
		// Alarm thresholds, 1-255 for ADx.x, 20-110 for RSSIx
		ArrayAdapter<String> RSSIalarmValueAdapter = new ArrayAdapter<String> (this, android.R.layout.simple_spinner_dropdown_item,valuesRSSI );
		ArrayAdapter<String> ADalarmValueAdapter = new ArrayAdapter<String> (this, android.R.layout.simple_spinner_dropdown_item,valuesADx );
		
		
		alarmRelAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
		alarmLevelAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
		
		
		// Find UI components
		// RSSI - Alarm 1
		RSSIalarm1LevelSpinner = (Spinner) findViewById( R.id.FrSkySettings_RSSI1_spinner_level );
		RSSIalarm1RelSpinner = (Spinner) findViewById( R.id.FrSkySettings_RSSI1_spinner_relative );
		RSSIalarm1ValueSpinner = (Spinner) findViewById( R.id.FrSkySettings_RSSI1_spinner_value );
		btnRSSI1Send = findViewById(R.id.FrSkySettings_RSSI1_send);
		// RSSI - Alarm 2
		RSSIalarm2LevelSpinner = (Spinner) findViewById( R.id.FrSkySettings_RSSI2_spinner_level );
		RSSIalarm2RelSpinner = (Spinner) findViewById( R.id.FrSkySettings_RSSI2_spinner_relative );
		RSSIalarm2ValueSpinner = (Spinner) findViewById( R.id.FrSkySettings_RSSI2_spinner_value );
		btnRSSI2Send = findViewById(R.id.FrSkySettings_RSSI2_send);
		
		// AD1 - Alarm 1
		AD1alarm1LevelSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD1_1_spinner_level );
		AD1alarm1RelSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD1_1_spinner_relative );
		AD1alarm1ValueSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD1_1_spinner_value );
		tvAD1_1_human = (TextView) findViewById(R.id.tvAD1_1_human);
		btnAD1_1_Send = findViewById(R.id.FrSkySettings_AD1_1_send);
		// AD1 - Alarm 2
		AD1alarm2LevelSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD1_2_spinner_level );
		AD1alarm2RelSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD1_2_spinner_relative );
		AD1alarm2ValueSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD1_2_spinner_value );
		btnAD1_2_Send = findViewById(R.id.FrSkySettings_AD1_2_send);
		
		// AD2 - Alarm 1
		AD2alarm1LevelSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD2_1_spinner_level );
		AD2alarm1RelSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD2_1_spinner_relative );
		AD2alarm1ValueSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD2_1_spinner_value );
		btnAD2_1_Send = findViewById(R.id.FrSkySettings_AD2_1_send);
		// AD2 - Alarm 2		
		AD2alarm2LevelSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD2_2_spinner_level );
		AD2alarm2RelSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD2_2_spinner_relative );
		AD2alarm2ValueSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD2_2_spinner_value );
		btnAD2_2_Send = findViewById(R.id.FrSkySettings_AD2_2_send);
		
		
		// Setup Adapters
		// RSSI - Alarm 1
		RSSIalarm1LevelSpinner.setAdapter( alarmLevelAdapter );
		RSSIalarm1RelSpinner.setAdapter( alarmRelAdapter );
		RSSIalarm1ValueSpinner.setAdapter( RSSIalarmValueAdapter );
		// RSSI - Alarm 2 
		RSSIalarm2LevelSpinner.setAdapter( alarmLevelAdapter );
		RSSIalarm2RelSpinner.setAdapter( alarmRelAdapter );
		RSSIalarm2ValueSpinner.setAdapter( RSSIalarmValueAdapter );
		// AD1 - Alarm 1
		AD1alarm1LevelSpinner.setAdapter( alarmLevelAdapter );
		AD1alarm1RelSpinner.setAdapter( alarmRelAdapter );
		AD1alarm1ValueSpinner.setAdapter( ADalarmValueAdapter );
		// AD1 - Alarm 2
		AD1alarm2LevelSpinner.setAdapter( alarmLevelAdapter );
		AD1alarm2RelSpinner.setAdapter( alarmRelAdapter );
		AD1alarm2ValueSpinner.setAdapter( ADalarmValueAdapter );
		// AD2 - Alarm 1
		AD2alarm1LevelSpinner.setAdapter( alarmLevelAdapter );
		AD2alarm1RelSpinner.setAdapter( alarmRelAdapter );
		AD2alarm1ValueSpinner.setAdapter( ADalarmValueAdapter );
		// AD2 - Alarm 2
		AD2alarm2LevelSpinner.setAdapter( alarmLevelAdapter );
		AD2alarm2RelSpinner.setAdapter( alarmRelAdapter );
		AD2alarm2ValueSpinner.setAdapter( ADalarmValueAdapter );
		
		
		// Setup Click Listeners
		///TODO: Can possibly be removed, since parsing of content is handled by send button
		///TODO: Might need to be in place to enable engineering unit string like:
		///      "Fire when Cell voltage lower than 3.4 Volt" 
		RSSIalarm1LevelSpinner.setOnItemSelectedListener(this);
		RSSIalarm2LevelSpinner.setOnItemSelectedListener(this);
		AD1alarm1LevelSpinner.setOnItemSelectedListener(this);
		AD1alarm2LevelSpinner.setOnItemSelectedListener(this);
		AD2alarm1LevelSpinner.setOnItemSelectedListener(this);
		AD2alarm2LevelSpinner.setOnItemSelectedListener(this);
		
		RSSIalarm1RelSpinner.setOnItemSelectedListener(this);
		RSSIalarm2RelSpinner.setOnItemSelectedListener(this);
		AD1alarm1RelSpinner.setOnItemSelectedListener(this);
		AD1alarm2RelSpinner.setOnItemSelectedListener(this);
		AD2alarm1RelSpinner.setOnItemSelectedListener(this);
		AD2alarm2RelSpinner.setOnItemSelectedListener(this);
		
		RSSIalarm1ValueSpinner.setOnItemSelectedListener(this);		
		RSSIalarm2ValueSpinner.setOnItemSelectedListener(this);
		AD1alarm1ValueSpinner.setOnItemSelectedListener(this);
		AD1alarm2ValueSpinner.setOnItemSelectedListener(this);
		AD2alarm1ValueSpinner.setOnItemSelectedListener(this);
		AD2alarm2ValueSpinner.setOnItemSelectedListener(this);
		
	    btnRSSI1Send.setOnClickListener(this);
	    btnRSSI2Send.setOnClickListener(this);
	    btnAD1_1_Send.setOnClickListener(this);
	    btnAD1_2_Send.setOnClickListener(this);
	    btnAD2_1_Send.setOnClickListener(this);
	    btnAD2_2_Send.setOnClickListener(this);
	    
				
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
				boolean setToWhenConnected=true;
				btnRSSI1Send.setEnabled(setToWhenConnected);
				btnRSSI2Send.setEnabled(setToWhenConnected);
				btnAD1_1_Send.setEnabled(setToWhenConnected);
				btnAD1_2_Send.setEnabled(setToWhenConnected);
				btnAD2_1_Send.setEnabled(setToWhenConnected);
				btnAD2_2_Send.setEnabled(setToWhenConnected);
			}
			else
			{
				boolean setToWhenNotConnected=false;
				btnRSSI1Send.setEnabled(setToWhenNotConnected);
				btnRSSI2Send.setEnabled(setToWhenNotConnected);
				btnAD1_1_Send.setEnabled(setToWhenNotConnected);
				btnAD1_2_Send.setEnabled(setToWhenNotConnected);
				btnAD2_1_Send.setEnabled(setToWhenNotConnected);
				btnAD2_2_Send.setEnabled(setToWhenNotConnected);
			}
			
			Log.i(TAG,"Setup Alarms:");
			// RSSI alarms does not get written from module,
			// Defaults need to come from FrSky, or
			// from settings
			RSSIalarm1LevelSpinner.setSelection(Alarm.ALARMLEVEL_HIGH);
			RSSIalarm1RelSpinner.setSelection(Alarm.LESSERTHAN);
			// 45 is default for RSSI alarm 1
			RSSIalarm1ValueSpinner.setSelection(45-minThresholdRSSI);
			
			RSSIalarm2LevelSpinner.setSelection(Alarm.ALARMLEVEL_HIGH);
			RSSIalarm2RelSpinner.setSelection(Alarm.LESSERTHAN);
			// 42 is default for RSSI alarm 2
			RSSIalarm2ValueSpinner.setSelection(42-minThresholdRSSI);
			
			if(server.RSSItx.alarmCount>0)
			{
				// Alarm 1
				try
				{
					RSSIalarm1LevelSpinner.setSelection(server.RSSItx.alarms[0].level);
					RSSIalarm1RelSpinner.setSelection(server.RSSItx.alarms[0].greaterthan);
					//myAdap = (ArrayAdapter) RSSIalarm1ValueSpinner.getAdapter(); //cast to an ArrayAdapter					
					//RSSIalarm1ValueSpinner.setSelection(myAdap.getPosition(""+server.RSSItx.alarms[0].threshold));
					RSSIalarm1ValueSpinner.setSelection(server.RSSItx.alarms[0].threshold-minThresholdRSSI);
				}
				catch(Exception e){	}
				// Alarm 2
				try
				{
					RSSIalarm2LevelSpinner.setSelection(server.RSSItx.alarms[1].level);
					RSSIalarm2RelSpinner.setSelection(server.RSSItx.alarms[1].greaterthan);
					RSSIalarm2ValueSpinner.setSelection(server.RSSItx.alarms[1].threshold-minThresholdRSSI);
				}
				catch(Exception e){	}
					
			}
			if(server.AD1.alarmCount>0)
			{
				// Alarm 1
				try
				{
					AD1alarm1LevelSpinner.setSelection(server.AD1.alarms[0].level);
					AD1alarm1RelSpinner.setSelection(server.AD1.alarms[0].greaterthan);
					AD1alarm1ValueSpinner.setSelection(server.AD1.alarms[0].threshold-minThresholdAD);
				}
				catch(Exception e){	}
				// Alarm 2
				try
				{
					AD1alarm2LevelSpinner.setSelection(server.AD1.alarms[1].level);
					AD1alarm2RelSpinner.setSelection(server.AD1.alarms[1].greaterthan);
					AD1alarm2ValueSpinner.setSelection(server.AD1.alarms[1].threshold-minThresholdAD);
				}
				catch(Exception e){	}
			}
			if(server.AD2.alarmCount>0)
			{
				// Alarm 1
				try
				{
					AD2alarm1LevelSpinner.setSelection(server.AD2.alarms[0].level);
					AD2alarm1RelSpinner.setSelection(server.AD2.alarms[0].greaterthan);
					AD2alarm1ValueSpinner.setSelection(server.AD2.alarms[0].threshold-minThresholdAD);
				}
				catch(Exception e){	}
				// Alarm 2
				try
				{
					AD2alarm2LevelSpinner.setSelection(server.AD2.alarms[1].level);
					AD2alarm2RelSpinner.setSelection(server.AD2.alarms[1].greaterthan);
					AD2alarm2ValueSpinner.setSelection(server.AD2.alarms[1].threshold-minThresholdAD);
				}
				catch(Exception e){	}
			}
			
		}

		public void onServiceDisconnected(ComponentName className) {
			server = null;
		}
	};



	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		
		String desc = server.AD1.getDescription();
		String rel = AD1alarm1RelSpinner.getSelectedItem().toString();
		float engVal = (server.AD1.getFactor()* Integer.parseInt(AD1alarm1ValueSpinner.getSelectedItem().toString()))+server.AD1.getOffset();
		tvAD1_1_human.setText(desc+" "+rel+" "+engVal+" "+server.AD1.getLongUnit() );
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onClick(View v) {
		//Log.i(TAG,"Some button clicked");
		Frame f;
    	switch (v.getId()) {
    		case R.id.FrSkySettings_RSSI1_send:
    			f = Frame.AlarmFrame(
    					Frame.FRAMETYPE_ALARM1_RSSI, 
    					RSSIalarm1LevelSpinner.getSelectedItemPosition(), 
    					Integer.parseInt(RSSIalarm1ValueSpinner.getSelectedItem().toString()), 
    					RSSIalarm1RelSpinner.getSelectedItemPosition());
    			server.send(f);
    			// RSSI alarms frames should also be parsed outgoing.
    			server.parseFrame(f);
    			break;
    		case R.id.FrSkySettings_RSSI2_send:
    			f = Frame.AlarmFrame(
    					Frame.FRAMETYPE_ALARM2_RSSI, 
    					RSSIalarm2LevelSpinner.getSelectedItemPosition(), 
    					Integer.parseInt(RSSIalarm2ValueSpinner.getSelectedItem().toString()), 
    					RSSIalarm2RelSpinner.getSelectedItemPosition());
    			server.send(f);
    			// RSSI alarms frames should also be parsed outgoing.
    			server.parseFrame(f);
    			break;
    		case R.id.FrSkySettings_AD1_1_send:
    			f = Frame.AlarmFrame(
    					Frame.FRAMETYPE_ALARM1_AD1, 
    					AD1alarm1LevelSpinner.getSelectedItemPosition(), 
    					Integer.parseInt(AD1alarm1ValueSpinner.getSelectedItem().toString()), 
    					AD1alarm1RelSpinner.getSelectedItemPosition());
    			server.send(f);
    			// No need to parse outgoing frames for ADx alarms
    			//server.parseFrame(f);
    			break;
    		case R.id.FrSkySettings_AD1_2_send:
    			f = Frame.AlarmFrame(
    					Frame.FRAMETYPE_ALARM2_AD1, 
    					AD1alarm2LevelSpinner.getSelectedItemPosition(), 
    					Integer.parseInt(AD1alarm2ValueSpinner.getSelectedItem().toString()), 
    					AD1alarm2RelSpinner.getSelectedItemPosition());
    			server.send(f);
    			// No need to parse outgoing frames for ADx alarms
    			//server.parseFrame(f);
    			break;
    		case R.id.FrSkySettings_AD2_1_send:
    			f = Frame.AlarmFrame(
    					Frame.FRAMETYPE_ALARM1_AD2, 
    					AD2alarm1LevelSpinner.getSelectedItemPosition(), 
    					Integer.parseInt(AD2alarm1ValueSpinner.getSelectedItem().toString()), 
    					AD2alarm1RelSpinner.getSelectedItemPosition());
    			server.send(f);
    			// No need to parse outgoing frames for ADx alarms
    			//server.parseFrame(f);
    			break;
    		case R.id.FrSkySettings_AD2_2_send:
    			f = Frame.AlarmFrame(
    					Frame.FRAMETYPE_ALARM2_AD2, 
    					AD2alarm2LevelSpinner.getSelectedItemPosition(), 
    					Integer.parseInt(AD2alarm2ValueSpinner.getSelectedItem().toString()), 
    					AD2alarm2RelSpinner.getSelectedItemPosition());
    			server.send(f);
    			// No need to parse outgoing frames for ADx alarms
    			//server.parseFrame(f);
    			break;
    	}
	}
}


