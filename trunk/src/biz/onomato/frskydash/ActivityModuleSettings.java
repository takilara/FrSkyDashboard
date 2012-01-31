package biz.onomato.frskydash;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

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
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityModuleSettings extends Activity implements OnItemSelectedListener, OnClickListener, OnSeekBarChangeListener {

	private static final String TAG = "FrSky-Settings";
	protected static final boolean DEBUG = true;
	private FrSkyServer server;
	private static final int ID_ALARM_SPINNER_RELATIVE = 5000;
	private static final int ID_ALARM_SPINNER_LEVEL = 6000;
	
    
	private Spinner RSSIalarm1LevelSpinner,RSSIalarm2LevelSpinner,AD1alarm1LevelSpinner,AD1alarm2LevelSpinner,AD2alarm1LevelSpinner,AD2alarm2LevelSpinner;
	private Spinner RSSIalarm1RelSpinner,RSSIalarm2RelSpinner,AD1alarm1RelSpinner,AD1alarm2RelSpinner,AD2alarm1RelSpinner,AD2alarm2RelSpinner;
	//Spinner RSSIalarm1ValueSpinner,RSSIalarm2ValueSpinner,AD1alarm1ValueSpinner,AD1alarm2ValueSpinner,AD2alarm1ValueSpinner,AD2alarm2ValueSpinner;
	
	private SeekBar RSSIalarm1ValueSb,RSSIalarm2ValueSb,AD1alarm1ValueSb,AD1alarm2ValueSb,AD2alarm1ValueSb,AD2alarm2ValueSb;
	
	//private View btnRSSI1Send,btnRSSI2Send,btnAD1_1_Send,btnAD1_2_Send,btnAD2_1_Send,btnAD2_2_Send;
	private Button btnSend,btnSave;
	private TextView tvAD1_1_human,tvAD1_2_human,tvRSSI_1_human,tvRSSI_2_human,tvAD2_1_human,tvAD2_2_human,tvModelName;
	private LinearLayout ll;
	private ArrayAdapter<CharSequence> alarmLevelAdapter;
	private ArrayAdapter<CharSequence> alarmRelAdapter;
	
	int minThresholdRSSI=20;
	int maxThresholdRSSI=110;
	int minThresholdAD=1;
	int maxThresholdAD=255;
	private int _modelId = -1;
	private Model _model=null;
	private TreeMap<Integer,Alarm> _alarmMap;
	
	//ArrayAdapter<String> AD1alarmValueAdapter;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		Intent launcherIntent = getIntent();
		_modelId = launcherIntent.getIntExtra("modelId", -1);
		
		super.onCreate(savedInstanceState);
		
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		setContentView(R.layout.activity_modulesettings);
		
		//_alarmMap = new TreeMap<Integer,Alarm>(Collections.reverseOrder());
		
		// Spinner contents
		// Alarm Level, Off, Low, Mid, High
		//ArrayAdapter<CharSequence> 
		alarmLevelAdapter = ArrayAdapter.createFromResource(this, R.array.alarm_level, android.R.layout.simple_spinner_item );
		// Alarm relativity: Greater than, Lower than
		//ArrayAdapter<CharSequence> 
		alarmRelAdapter = ArrayAdapter.createFromResource(this, R.array.alarm_relative, android.R.layout.simple_spinner_item );
		
		// Setup "spinner design"
		alarmRelAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
		alarmLevelAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
		
		// Find UI components
		
		ll = (LinearLayout) findViewById(R.id.linearLayoutFrSkyModuleSettings);
		
		// Model Name
		tvModelName = (TextView) findViewById(R.id.tv_FrSkySettings_modelName);
		tvModelName.setText("");
		
		
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
			
			
			if(_modelId==-1)
			{
				if(DEBUG) Log.d(TAG,"Configure new Model object");
				//_model = new Model(getApplicationContext());
				_model = server.getCurrentModel();
			}
			else
			{
				if(DEBUG) Log.d(TAG,"Configure existing Model object (id:"+_modelId+")");
				//_model = new Model(getApplicationContext());
				//_model.loadFromDatabase(_modelId);
				_model = FrSkyServer.database.getModel(_modelId);
			}
			//_alarmMap = _model.getFrSkyAlarms();
			_alarmMap = new TreeMap<Integer,Alarm>(Collections.reverseOrder());
			_alarmMap.putAll(FrSkyServer.database.getAlarmsForModel(_modelId));
			//_alarmMap = FrSkyServer.database.getAlarmsForModel(_modelId);
			
			tvModelName.setText(_model.getName());
			
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
			
			Log.d(TAG,"Try to set up spinners for model: "+_model.getName());
			Log.d(TAG,"This model has this many alarms: "+_alarmMap.size());
			if(_alarmMap.size()==0)
			{
				_alarmMap = server.initializeFrSkyAlarms();
			}
			for(int key : _alarmMap.keySet())
			{
				Log.d(TAG,"\t"+key+"= "+_alarmMap.get(key));
			}

			
			try
			{
				// TODO: Fix here
				Alarm a = _alarmMap.get(Frame.FRAMETYPE_ALARM1_RSSI);
				RSSIalarm1ValueSb.setMax(a.getMaxThreshold()-a.getMinThreshold()+1);
				RSSIalarm1ValueSb.setProgress(a.getThreshold()-a.getMinThreshold());
				RSSIalarm1RelSpinner.setSelection(a.getGreaterThan());
				RSSIalarm1LevelSpinner.setSelection(a.getAlarmLevel());
//				
				a = _alarmMap.get(Frame.FRAMETYPE_ALARM2_RSSI);
				RSSIalarm2ValueSb.setMax(a.getMaxThreshold()-a.getMinThreshold()+1);
				RSSIalarm2ValueSb.setProgress(a.getThreshold()-a.getMinThreshold());
				RSSIalarm2RelSpinner.setSelection(a.getGreaterThan());
				RSSIalarm2LevelSpinner.setSelection(a.getAlarmLevel());
//				
				a = _alarmMap.get(Frame.FRAMETYPE_ALARM1_AD1);
				AD1alarm1ValueSb.setMax(a.getMaxThreshold()-a.getMinThreshold()+1);
				AD1alarm1ValueSb.setProgress(a.getThreshold()-a.getMinThreshold());
				AD1alarm1RelSpinner.setSelection(a.getGreaterThan());
				AD1alarm1LevelSpinner.setSelection(a.getAlarmLevel());
//				
				a = _alarmMap.get(Frame.FRAMETYPE_ALARM2_AD1);
				AD1alarm2ValueSb.setMax(a.getMaxThreshold()-a.getMinThreshold()+1);
				AD1alarm2ValueSb.setProgress(a.getThreshold()-a.getMinThreshold());
				AD1alarm2RelSpinner.setSelection(a.getGreaterThan());
				AD1alarm2LevelSpinner.setSelection(a.getAlarmLevel());
//				
				a = _alarmMap.get(Frame.FRAMETYPE_ALARM1_AD2);
				AD2alarm1ValueSb.setMax(a.getMaxThreshold()-a.getMinThreshold()+1);
				AD2alarm1ValueSb.setProgress(a.getThreshold()-a.getMinThreshold());
				AD2alarm1RelSpinner.setSelection(a.getGreaterThan());
				AD2alarm1LevelSpinner.setSelection(a.getAlarmLevel());
//				
				a = _alarmMap.get(Frame.FRAMETYPE_ALARM2_AD2);
				AD2alarm2ValueSb.setMax(a.getMaxThreshold()-a.getMinThreshold()+1);
				AD2alarm2ValueSb.setProgress(a.getThreshold()-a.getMinThreshold());
				AD2alarm2RelSpinner.setSelection(a.getGreaterThan());
				AD2alarm2LevelSpinner.setSelection(a.getAlarmLevel());
				//AD2alarm2ValueSb.
			}
			catch(Exception e)
			{
				Log.e(TAG,e.toString());
			}
			
			populateGUI();
			
		}

		public void onServiceDisconnected(ComponentName className) {
			server = null;
		}
	};

	
	public void updateStrings()
	{
		Log.d(TAG,"Update the RSSI string!");
		Alarm a = _alarmMap.get(Frame.FRAMETYPE_ALARM1_RSSI);
		a.setThreshold(RSSIalarm1ValueSb.getProgress()+a.getMinThreshold());
		a.setGreaterThan(RSSIalarm1RelSpinner.getSelectedItemPosition());
		a.setAlarmLevel(RSSIalarm1LevelSpinner.getSelectedItemPosition());
		tvRSSI_1_human.setText(a.toString());

		a = _alarmMap.get(Frame.FRAMETYPE_ALARM2_RSSI);
		a.setThreshold(RSSIalarm2ValueSb.getProgress()+a.getMinThreshold());
		a.setGreaterThan(RSSIalarm2RelSpinner.getSelectedItemPosition());
		a.setAlarmLevel(RSSIalarm2LevelSpinner.getSelectedItemPosition());
		tvRSSI_2_human.setText(a.toString());
		
		a = _alarmMap.get(Frame.FRAMETYPE_ALARM1_AD1);
		a.setThreshold(AD1alarm1ValueSb.getProgress()+a.getMinThreshold());
		a.setGreaterThan(AD1alarm1RelSpinner.getSelectedItemPosition());
		a.setAlarmLevel(AD1alarm1LevelSpinner.getSelectedItemPosition());
		tvAD1_1_human.setText(a.toString());
		
		a = _alarmMap.get(Frame.FRAMETYPE_ALARM2_AD1);
		a.setThreshold(AD1alarm2ValueSb.getProgress()+a.getMinThreshold());
		a.setGreaterThan(AD1alarm2RelSpinner.getSelectedItemPosition());
		a.setAlarmLevel(AD1alarm2LevelSpinner.getSelectedItemPosition());
		tvAD1_2_human.setText(a.toString());
		
		a = _alarmMap.get(Frame.FRAMETYPE_ALARM1_AD2);
		a.setThreshold(AD2alarm1ValueSb.getProgress()+a.getMinThreshold());
		a.setGreaterThan(AD2alarm1RelSpinner.getSelectedItemPosition());
		a.setAlarmLevel(AD2alarm1LevelSpinner.getSelectedItemPosition());
		tvAD2_1_human.setText(a.toString());
		
		a = _alarmMap.get(Frame.FRAMETYPE_ALARM2_AD2);
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
		
		if((view.getId()>ID_ALARM_SPINNER_LEVEL)&& (view.getId()<ID_ALARM_SPINNER_LEVEL+255))
		{
			Log.i(TAG,"selected a Level spinner");
		}
		else if ((view.getId()>ID_ALARM_SPINNER_RELATIVE)&& (view.getId()<ID_ALARM_SPINNER_RELATIVE+255))
		{
			Log.i(TAG,"selected a relative spinner");
		}
		else
		{
			updateStrings();
		}
		
		
		
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
    			server.send(_alarmMap.get(Frame.FRAMETYPE_ALARM1_RSSI).toFrame());
    			server.send(_alarmMap.get(Frame.FRAMETYPE_ALARM2_RSSI).toFrame());
    			server.send(_alarmMap.get(Frame.FRAMETYPE_ALARM1_AD1).toFrame());
    			server.send(_alarmMap.get(Frame.FRAMETYPE_ALARM2_AD1).toFrame());
    			server.send(_alarmMap.get(Frame.FRAMETYPE_ALARM1_AD2).toFrame());
    			server.send(_alarmMap.get(Frame.FRAMETYPE_ALARM2_AD2).toFrame());
    			// RSSI alarms frames should also be parsed outgoing.
    			//server.parseFrame(f);
    			break;
    		case R.id.FrSkySettings_save:

//    			for(Alarm a : _alarmMap.values())
//    			{
//    				_model.addAlarm(a);
//    			}
    			_model.setFrSkyAlarms(_alarmMap);
    			// Copy back new _alarmMap
    			FrSkyServer.database.saveModel(_model);

    			this.setResult(RESULT_OK);
				this.finish();

    			break;
    	}
	}

	public void populateGUI()
	{
		//ArrayAdapter<CharSequence> alarmLevelAdapter = ArrayAdapter.createFromResource(this, R.array.alarm_level, android.R.layout.simple_spinner_item );
		// Alarm relativity: Greater than, Lower than
		//ArrayAdapter<CharSequence> alarmRelAdapter = ArrayAdapter.createFromResource(this, R.array.alarm_relative, android.R.layout.simple_spinner_item );
		
		// Setup "spinner design"
		//alarmRelAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
		//alarmLevelAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);

		
		Iterator<Alarm> i = _alarmMap.values().iterator();
		//for(Alarm a : _alarmMap.values())
		while(i.hasNext())
		{
			Alarm a = i.next();
			if(DEBUG)Log.d(TAG,"Creating GUI components for alarm "+a.getFrSkyFrameType());
			// Line 1
			//LinearLayout line1 = new LinearLayout(getApplicationContext());
			TextView tvTitle = new TextView(getApplicationContext());
			String title = "";
			
			
			switch(a.getFrSkyFrameType())
			{
				case Frame.FRAMETYPE_ALARM1_AD1:
					title = "AD1 - Alarm 1";
					break;
				case Frame.FRAMETYPE_ALARM2_AD1:
					title = "AD1 - Alarm 2";
					break;
				case Frame.FRAMETYPE_ALARM1_AD2:
					title = "AD2 - Alarm 1";
					break;
				case Frame.FRAMETYPE_ALARM2_AD2:
					title = "AD2 - Alarm 2";
					break;
				case Frame.FRAMETYPE_ALARM1_RSSI:
					title = "RSSI - Alarm 1";
					break;
				case Frame.FRAMETYPE_ALARM2_RSSI:
					title = "RSSI - Alarm 2";
					break;
			}
			tvTitle.setText(title);
			tvTitle.setBackgroundColor(0xff909090);
			tvTitle.setTextColor(0xffffffff);
			ll.addView(tvTitle);
			
			
			// // Source channel
			
			//ArrayAdapter<CharSequence> lvlAdapter = ArrayAdapter.createFromResource(this, R.array.alarm_level, android.R.layout.simple_spinner_item );
			//Spinner levelSp = new Spinner(getApplicationContext());
			Spinner levelSp = new Spinner(this);
			levelSp.setAdapter(alarmLevelAdapter);
			levelSp.setOnItemSelectedListener(this);
			levelSp.setId(ID_ALARM_SPINNER_LEVEL+a.getFrSkyFrameType());
			levelSp.setSelection(a.getAlarmLevel());
			ll.addView(levelSp);
			
			Spinner relSp = new Spinner(this);
			relSp.setAdapter(alarmRelAdapter);
			relSp.setOnItemSelectedListener(this);
			relSp.setId(ID_ALARM_SPINNER_RELATIVE+a.getFrSkyFrameType());
			relSp.setSelection(a.getGreaterThan());
			ll.addView(relSp);
			

			SeekBar thresholdSb = new SeekBar(this);
			thresholdSb.setMax(a.getMaxThreshold()-a.getMinThreshold()+1);
			thresholdSb.setProgress(a.getThreshold()-a.getMinThreshold());
			//thresholdSb.setOnSeekBarChangeListener(this);
			ll.addView(thresholdSb);
			
			TextView descTv = new TextView(this);
			descTv.setText(a.toString());
			ll.addView(descTv);
			
			
			// Threshold
			// To Text
			// // Individual Send
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


