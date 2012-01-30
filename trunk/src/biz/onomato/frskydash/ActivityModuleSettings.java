package biz.onomato.frskydash;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityModuleSettings extends Activity implements OnItemSelectedListener, OnClickListener, OnSeekBarChangeListener {

	private static final String TAG = "FrSky-Settings";
	private FrSkyServer server;
	
	
    
	Spinner RSSIalarm1LevelSpinner,RSSIalarm2LevelSpinner,AD1alarm1LevelSpinner,AD1alarm2LevelSpinner,AD2alarm1LevelSpinner,AD2alarm2LevelSpinner;
	Spinner RSSIalarm1RelSpinner,RSSIalarm2RelSpinner,AD1alarm1RelSpinner,AD1alarm2RelSpinner,AD2alarm1RelSpinner,AD2alarm2RelSpinner;
	//Spinner RSSIalarm1ValueSpinner,RSSIalarm2ValueSpinner,AD1alarm1ValueSpinner,AD1alarm2ValueSpinner,AD2alarm1ValueSpinner,AD2alarm2ValueSpinner;
	
	SeekBar RSSIalarm1ValueSb,RSSIalarm2ValueSb,AD1alarm1ValueSb,AD1alarm2ValueSb,AD2alarm1ValueSb,AD2alarm2ValueSb;
	
	//private View btnRSSI1Send,btnRSSI2Send,btnAD1_1_Send,btnAD1_2_Send,btnAD2_1_Send,btnAD2_2_Send;
	private Button btnSend,btnSave;
	private TextView tvAD1_1_human,tvAD1_2_human,tvRSSI_1_human,tvRSSI_2_human,tvAD2_1_human,tvAD2_2_human;
	
	int minThresholdRSSI=20;
	int maxThresholdRSSI=110;
	int minThresholdAD=1;
	int maxThresholdAD=255;
	
	//ArrayAdapter<String> AD1alarmValueAdapter;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		setContentView(R.layout.activity_modulesettings);
		
		// Spinner contents
		// Alarm Level, Off, Low, Mid, High
		ArrayAdapter<CharSequence> alarmLevelAdapter = ArrayAdapter.createFromResource(this, R.array.alarm_level, android.R.layout.simple_spinner_item );
		// Alarm relativity: Greater than, Lower than
		ArrayAdapter<CharSequence> alarmRelAdapter = ArrayAdapter.createFromResource(this, R.array.alarm_relative, android.R.layout.simple_spinner_item );
		
		// Setup "spinner design"
		alarmRelAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
		alarmLevelAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
		
		// Find UI components
		
		// Save button
		btnSave = (Button) findViewById(R.id.FrSkySettings_save);
		
		// Send Button
		btnSend = (Button) findViewById(R.id.FrSkySettings_send);
		
		// RSSI - Alarm 1
		RSSIalarm1ValueSb = (SeekBar) findViewById(R.id.FrSkySettings_RSSI1_seekbar_value);
		RSSIalarm1LevelSpinner = (Spinner) findViewById( R.id.FrSkySettings_RSSI1_spinner_level );
		RSSIalarm1RelSpinner = (Spinner) findViewById( R.id.FrSkySettings_RSSI1_spinner_relative );
		tvRSSI_1_human = (TextView) findViewById(R.id.tvRSSI_1_human);
		
		// RSSI - Alarm 2
		RSSIalarm2ValueSb = (SeekBar) findViewById(R.id.FrSkySettings_RSSI2_seekbar_value);
		RSSIalarm2LevelSpinner = (Spinner) findViewById( R.id.FrSkySettings_RSSI2_spinner_level );
		RSSIalarm2RelSpinner = (Spinner) findViewById( R.id.FrSkySettings_RSSI2_spinner_relative );
		tvRSSI_2_human = (TextView) findViewById(R.id.tvRSSI_2_human);
		
		
		// AD1 - Alarm 1
		AD1alarm1ValueSb = (SeekBar) findViewById(R.id.FrSkySettings_AD1_1_seekbar_value);
		AD1alarm1LevelSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD1_1_spinner_level );
		AD1alarm1RelSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD1_1_spinner_relative );
		tvAD1_1_human = (TextView) findViewById(R.id.tvAD1_1_human);
		
		// AD1 - Alarm 2
		AD1alarm2ValueSb = (SeekBar) findViewById(R.id.FrSkySettings_AD1_2_seekbar_value);
		AD1alarm2LevelSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD1_2_spinner_level );
		AD1alarm2RelSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD1_2_spinner_relative );
		tvAD1_2_human = (TextView) findViewById(R.id.tvAD1_2_human);
		
		
		// AD2 - Alarm 1
		AD2alarm1ValueSb = (SeekBar) findViewById(R.id.FrSkySettings_AD2_1_seekbar_value);
		AD2alarm1LevelSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD2_1_spinner_level );
		AD2alarm1RelSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD2_1_spinner_relative );
		tvAD2_1_human = (TextView) findViewById(R.id.tvAD2_1_human);
		
		// AD2 - Alarm 2		
		AD2alarm2ValueSb = (SeekBar) findViewById(R.id.FrSkySettings_AD2_2_seekbar_value);
		AD2alarm2LevelSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD2_2_spinner_level );
		AD2alarm2RelSpinner = (Spinner) findViewById( R.id.FrSkySettings_AD2_2_spinner_relative );
		tvAD2_2_human = (TextView) findViewById(R.id.tvAD2_2_human);
		
		
		
		// Setup Adapters
		// RSSI - Alarm 1
		RSSIalarm1LevelSpinner.setAdapter( alarmLevelAdapter );
		RSSIalarm1RelSpinner.setAdapter( alarmRelAdapter );
		//RSSIalarm1ValueSpinner.setAdapter( RSSIalarmValueAdapter );
		// RSSI - Alarm 2 
		RSSIalarm2LevelSpinner.setAdapter( alarmLevelAdapter );
		RSSIalarm2RelSpinner.setAdapter( alarmRelAdapter );
		//RSSIalarm2ValueSpinner.setAdapter( RSSIalarmValueAdapter );
		// AD1 - Alarm 1
		AD1alarm1LevelSpinner.setAdapter( alarmLevelAdapter );
		AD1alarm1RelSpinner.setAdapter( alarmRelAdapter );
		//AD1alarm1ValueSpinner.setAdapter( AD1alarmValueAdapter );
		
		
		// AD1 - Alarm 2
		AD1alarm2LevelSpinner.setAdapter( alarmLevelAdapter );
		AD1alarm2RelSpinner.setAdapter( alarmRelAdapter );
		//AD1alarm2ValueSpinner.setAdapter( AD1alarmValueAdapter );
		// AD2 - Alarm 1
		AD2alarm1LevelSpinner.setAdapter( alarmLevelAdapter );
		AD2alarm1RelSpinner.setAdapter( alarmRelAdapter );
		//AD2alarm1ValueSpinner.setAdapter( AD2alarmValueAdapter );
		// AD2 - Alarm 2
		AD2alarm2LevelSpinner.setAdapter( alarmLevelAdapter );
		AD2alarm2RelSpinner.setAdapter( alarmRelAdapter );
		//AD2alarm2ValueSpinner.setAdapter( AD2alarmValueAdapter );
		
		
		// Setup Click Listeners
		btnSave.setOnClickListener(this);
		btnSend.setOnClickListener(this);

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
		
	    RSSIalarm1ValueSb.setOnSeekBarChangeListener(this);
	    RSSIalarm2ValueSb.setOnSeekBarChangeListener(this);
	    AD1alarm1ValueSb.setOnSeekBarChangeListener(this);
	    AD1alarm2ValueSb.setOnSeekBarChangeListener(this);
	    AD2alarm1ValueSb.setOnSeekBarChangeListener(this);
	    AD2alarm2ValueSb.setOnSeekBarChangeListener(this);
	    
	    
				
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
				btnSend.setEnabled(setToWhenConnected);
				
			}
			else
			{
				boolean setToWhenNotConnected=false;
				btnSend.setEnabled(setToWhenNotConnected);
			}
			
			Log.d(TAG,"Try to set up spinners for model: "+server.getCurrentModel().getName());
			Log.d(TAG,"This model has this many alarms: "+server.getCurrentModel().getFrSkyAlarms().size());
			for(int key : server.getCurrentModel().getFrSkyAlarms().keySet())
			{
				Log.d(TAG,"\t"+key+"= "+server.getCurrentModel().getFrSkyAlarms().get(key));
			}

			
			try
			{
				// TODO: Fix here
//				Alarm a = server.getCurrentModel().getFrSkyAlarms().get(Frame.FRAMETYPE_ALARM1_RSSI);
//				RSSIalarm1ValueSb.setMax(a.getMaxThreshold()-a.getMinThreshold());
//				RSSIalarm1ValueSb.setProgress(a.getThreshold()+a.getMinThreshold());
//				
//				a = server.getCurrentModel().getFrSkyAlarms().get(Frame.FRAMETYPE_ALARM2_RSSI);
//				RSSIalarm2ValueSb.setMax(a.getMaxThreshold()-a.getMinThreshold());
//				RSSIalarm2ValueSb.setProgress(a.getThreshold()+a.getMinThreshold());
//				
//				a = server.getCurrentModel().getFrSkyAlarms().get(Frame.FRAMETYPE_ALARM1_AD1);
//				AD1alarm1ValueSb.setMax(a.getMaxThreshold()-a.getMinThreshold());
//				AD1alarm1ValueSb.setProgress(a.getThreshold()+a.getMinThreshold());
//				
//				a = server.getCurrentModel().getFrSkyAlarms().get(Frame.FRAMETYPE_ALARM2_AD1);
//				AD1alarm2ValueSb.setMax(a.getMaxThreshold()-a.getMinThreshold());
//				AD1alarm2ValueSb.setProgress(a.getThreshold()+a.getMinThreshold());
//				
//				a = server.getCurrentModel().getFrSkyAlarms().get(Frame.FRAMETYPE_ALARM1_AD2);
//				AD2alarm1ValueSb.setMax(a.getMaxThreshold()-a.getMinThreshold());
//				AD2alarm1ValueSb.setProgress(a.getThreshold()+a.getMinThreshold());
//				
//				a = server.getCurrentModel().getFrSkyAlarms().get(Frame.FRAMETYPE_ALARM2_AD2);
//				AD2alarm2ValueSb.setMax(a.getMaxThreshold()-a.getMinThreshold());
//				AD2alarm2ValueSb.setProgress(a.getThreshold()+a.getMinThreshold());
			}
			catch(Exception e)
			{
				Log.e(TAG,e.toString());
			}
			
			
		}

		public void onServiceDisconnected(ComponentName className) {
			server = null;
		}
	};

	
	public void updateStrings()
	{
		Alarm a = server.getCurrentModel().getFrSkyAlarms().get(Frame.FRAMETYPE_ALARM1_RSSI);
		a.setThreshold(RSSIalarm1ValueSb.getProgress()+a.getMinThreshold());
		a.setGreaterThan(RSSIalarm1RelSpinner.getSelectedItemPosition());
		a.setAlarmLevel(RSSIalarm1LevelSpinner.getSelectedItemPosition());
		tvRSSI_1_human.setText(a.toString());
		
		a = server.getCurrentModel().getFrSkyAlarms().get(Frame.FRAMETYPE_ALARM2_RSSI);
		a.setThreshold(RSSIalarm2ValueSb.getProgress()+a.getMinThreshold());
		a.setGreaterThan(RSSIalarm2RelSpinner.getSelectedItemPosition());
		a.setAlarmLevel(RSSIalarm2LevelSpinner.getSelectedItemPosition());
		tvRSSI_2_human.setText(a.toString());
		
		a = server.getCurrentModel().getFrSkyAlarms().get(Frame.FRAMETYPE_ALARM1_AD1);
		a.setThreshold(AD1alarm1ValueSb.getProgress()+a.getMinThreshold());
		a.setGreaterThan(AD1alarm1RelSpinner.getSelectedItemPosition());
		a.setAlarmLevel(AD1alarm1LevelSpinner.getSelectedItemPosition());
		tvAD1_1_human.setText(a.toString());
		
		a = server.getCurrentModel().getFrSkyAlarms().get(Frame.FRAMETYPE_ALARM2_AD1);
		a.setThreshold(AD1alarm2ValueSb.getProgress()+a.getMinThreshold());
		a.setGreaterThan(AD1alarm2RelSpinner.getSelectedItemPosition());
		a.setAlarmLevel(AD1alarm2LevelSpinner.getSelectedItemPosition());
		tvAD1_2_human.setText(a.toString());
		
		a = server.getCurrentModel().getFrSkyAlarms().get(Frame.FRAMETYPE_ALARM1_AD2);
		a.setThreshold(AD2alarm1ValueSb.getProgress()+a.getMinThreshold());
		a.setGreaterThan(AD2alarm1RelSpinner.getSelectedItemPosition());
		a.setAlarmLevel(AD2alarm1LevelSpinner.getSelectedItemPosition());
		tvAD2_1_human.setText(a.toString());
		
		a = server.getCurrentModel().getFrSkyAlarms().get(Frame.FRAMETYPE_ALARM2_AD2);
		a.setThreshold(AD2alarm2ValueSb.getProgress()+a.getMinThreshold());
		a.setGreaterThan(AD2alarm2RelSpinner.getSelectedItemPosition());
		a.setAlarmLevel(AD2alarm2LevelSpinner.getSelectedItemPosition());
		tvAD2_2_human.setText(a.toString());
		
	}


	
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub
		
		//String testString = server.getCurrentModel().getFrSkyAlarms().get(Frame.FRAMETYPE_ALARM1_RSSI).toString(RSSIalarm1ValueSb.getProgress());
		if(fromUser)
		{
			updateStrings();
		}
		
		
		
		
	}
	
		

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		

//		tvRSSI_1_human.setText(String.format("%s %s %s", 
//				server.getSourceChannel(FrSkyServer.CHANNEL_INDEX_RSSITX).getDescription(),
//				RSSIalarm1RelSpinner.getSelectedItem().toString().replaceFirst("Value ", ""),
//				server.getSourceChannel(FrSkyServer.CHANNEL_INDEX_RSSITX).toEng(RSSIalarm1ValueSpinner.getSelectedItemPosition()+minThresholdRSSI,true)));
		
		updateStrings();
		
//		
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
    		case R.id.FrSkySettings_send:
    			server.send(server.getCurrentModel().getFrSkyAlarms().get(Frame.FRAMETYPE_ALARM1_RSSI).toFrame());
    			// RSSI alarms frames should also be parsed outgoing.
    			//server.parseFrame(f);
    			break;
    		case R.id.FrSkySettings_save:
    			//server.send(server.getCurrentModel().getFrSkyAlarms().get(Frame.FRAMETYPE_ALARM2_RSSI).toFrame());
    			server.database.saveModel(server.getCurrentModel());
    			// RSSI alarms frames should also be parsed outgoing.
    			//server.parseFrame(f);
    			break;
    	}
	}

	

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
}


