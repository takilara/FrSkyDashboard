package biz.onomato.frskydash.activities;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import biz.onomato.frskydash.BluetoothSerialService;
import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.R;
import biz.onomato.frskydash.FrSkyServer.MyBinder;
import biz.onomato.frskydash.R.array;
import biz.onomato.frskydash.R.id;
import biz.onomato.frskydash.R.layout;
import biz.onomato.frskydash.domain.Alarm;
import biz.onomato.frskydash.domain.Channel;
import biz.onomato.frskydash.domain.Frame;
import biz.onomato.frskydash.domain.Model;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

public class ActivityModuleSettings extends Activity implements OnClickListener {

	private static final String TAG = "FrSky-Settings";
	//protected static final boolean DEBUG = true;
	private FrSkyServer server;
	private static final int ID_ALARM_SPINNER_RELATIVE = 5000;
	private static final int ID_ALARM_SPINNER_LEVEL = 6000;
	private static final int ID_ALARM_SPINNER_SOURCECHANNEL = 7000;
	private static final int ID_ALARM_TEXTVIEW_DESCRIPTION = 8000;
	private static final int ID_ALARM_SEEKBAR_THRESHOLD = 9000;
	private static final int ID_ALARM_BUTTON_SEND = 10000;
	
	private boolean _btSendButtonsEnabled = false;
	
	private Button btnSend,btnSave,btnGetAlarmsFromModule;
	private TextView tvModelName;
	private LinearLayout ll;
	
	private int _modelId = -1;
	private Model _model=null;
	private TreeMap<Integer,Alarm> _alarmMap;
	private IntentFilter mIntentFilter;
	
	//ArrayAdapter<String> AD1alarmValueAdapter;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		Intent launcherIntent = getIntent();
		_modelId = launcherIntent.getIntExtra("modelId", -1);
		
		super.onCreate(savedInstanceState);
		
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		setContentView(R.layout.activity_modulesettings);
		
		//_alarmMap = new TreeMap<Integer,Alarm>(Collections.reverseOrder());
		

		
		// Find UI components
		
		ll = (LinearLayout) findViewById(R.id.linearLayoutFrSkyModuleSettings);
		
		// Model Name
		tvModelName = (TextView) findViewById(R.id.tv_FrSkySettings_modelName);
		tvModelName.setText("");
		
		
		// Save button
		btnSave = (Button) findViewById(R.id.FrSkySettings_save);
		
		// Send Button
		btnSend = (Button) findViewById(R.id.FrSkySettings_send);
		
		// Get Alarms from module button
		btnGetAlarmsFromModule = (Button) findViewById(R.id.FrSkySettings_btnGetFromModule);
		
		
		
		
		
		// Setup Click Listeners
		btnSave.setOnClickListener(this);
		btnSend.setOnClickListener(this);
		btnGetAlarmsFromModule.setOnClickListener(this);


	    
		
	
	    
				
		doBindService();
				
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		if(FrSkyServer.D)Log.i(TAG,"onResume");
		

	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		try
		{
			unregisterReceiver(mIntentReceiver);
		}
		catch(Exception e)
		{}
		
	}

	void doBindService() {
		if(FrSkyServer.D)Log.i(TAG,"Start the server service if it is not already started");
		startService(new Intent(this, FrSkyServer.class));
		if(FrSkyServer.D)Log.i(TAG,"Try to bind to the service");
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
			mIntentFilter = new IntentFilter();
		    mIntentFilter.addAction(FrSkyServer.MESSAGE_ALARM_RECORDING_COMPLETE);
		    mIntentFilter.addAction(FrSkyServer.MESSAGE_BLUETOOTH_STATE_CHANGED);			// listen for bt messages
		    registerReceiver(mIntentReceiver, mIntentFilter);
		    
		    if(FrSkyServer.D)Log.i(TAG,"Bound to Service");
			
			
			if(_modelId==-1)
			{
				if(FrSkyServer.D) Log.d(TAG,"Configure new Model object");
				//_model = new Model(getApplicationContext());
				_model = server.getCurrentModel();
			}
			else
			{
				if(FrSkyServer.D) Log.d(TAG,"Configure existing Model object (id:"+_modelId+")");
				//_model = new Model(getApplicationContext());
				//_model.loadFromDatabase(_modelId);
				//_model = FrSkyServer.database.getModel(_modelId);
				_model = FrSkyServer.modelMap.get(_modelId);
			}
        	
			
			//_alarmMap = _model.getFrSkyAlarms();
			_alarmMap = new TreeMap<Integer,Alarm>(Collections.reverseOrder());
			//_alarmMap.putAll(FrSkyServer.database.getAlarmsForModel(_modelId));
			_alarmMap.putAll(FrSkyServer.modelMap.get(_modelId).getFrSkyAlarms());
			//_alarmMap = FrSkyServer.database.getAlarmsForModel(_modelId);
			
			
	
			
			
			
			tvModelName.setText(_model.getName());
			
			// only enable send buttons if Bluetooth is connected
			
			if(server.getConnectionState()==BluetoothSerialService.STATE_CONNECTED)
			{
				//boolean setToWhenConnected=true;
				_btSendButtonsEnabled = true;
//				btnSend.setEnabled(setToWhenConnected);
				
			}
			else
			{
				_btSendButtonsEnabled = false;
//				boolean setToWhenNotConnected=false;
//				btnSend.setEnabled(setToWhenNotConnected);
			}
			
			btnSend.setEnabled(_btSendButtonsEnabled);
			btnGetAlarmsFromModule.setEnabled(_btSendButtonsEnabled);
			
			if(FrSkyServer.D)Log.d(TAG,"Try to set up spinners for model: "+_model.getName());
			if(FrSkyServer.D)Log.d(TAG,"This model has this many alarms: "+_alarmMap.size());
			if(_alarmMap.size()==0)
			{
				_alarmMap = server.initializeFrSkyAlarms();
			}
			
			
			populateGUI();
			setStateBtButtons(_btSendButtonsEnabled);
		}

		public void onServiceDisconnected(ComponentName className) {
			server = null;
		}
	};

	private void setStateBtButtons(boolean state)
	{
		btnSend.setEnabled(state);
		btnGetAlarmsFromModule.setEnabled(state);
		for(Alarm a : _alarmMap.values())
		{
			Button btn = (Button) findViewById(ID_ALARM_BUTTON_SEND+a.getFrSkyFrameType());
			btn.setEnabled(state);
		}
	}
	
	public void updateAlarmVisualization(Alarm a)
	{
		// find the level spinner
		Spinner splvl = (Spinner) findViewById(ID_ALARM_SPINNER_LEVEL+a.getFrSkyFrameType());
		// update it
		splvl.setSelection(a.getAlarmLevel());
		
		// find the relative spinner
		Spinner sprel = (Spinner) findViewById(ID_ALARM_SPINNER_RELATIVE+a.getFrSkyFrameType());
		// update it
		sprel.setSelection(a.getGreaterThan());
		
		// find the threshold seekbar
		SeekBar sbThreshold = (SeekBar) findViewById(ID_ALARM_SEEKBAR_THRESHOLD+a.getFrSkyFrameType());
		// update it
		sbThreshold.setMax(a.getMaxThreshold()-a.getMinThreshold()+1);
		sbThreshold.setProgress(a.getThreshold()-a.getMinThreshold());
	}
	public void updateAlarmDescription(int alarmId)
	{
		// Get the correct TextView
		TextView tv = (TextView) findViewById(ID_ALARM_TEXTVIEW_DESCRIPTION+alarmId);
		
		// Update the textView with the toString() from the correct alarm
		
		tv.setText(_alarmMap.get(alarmId).toString());
	}
	

	
	@Override
	public void onClick(View v) {
		//Log.i(TAG,"Some button clicked");
		Frame f;
    	switch (v.getId()) {
    		case R.id.FrSkySettings_send:
    			// Send all alarms
    			server.send(_alarmMap.get(Frame.FRAMETYPE_ALARM1_RSSI).toFrame());
    			server.send(_alarmMap.get(Frame.FRAMETYPE_ALARM2_RSSI).toFrame());
    			server.send(_alarmMap.get(Frame.FRAMETYPE_ALARM1_AD1).toFrame());
    			server.send(_alarmMap.get(Frame.FRAMETYPE_ALARM2_AD1).toFrame());
    			server.send(_alarmMap.get(Frame.FRAMETYPE_ALARM1_AD2).toFrame());
    			server.send(_alarmMap.get(Frame.FRAMETYPE_ALARM2_AD2).toFrame());
    			break;
    		case R.id.FrSkySettings_save:
    			_model.setFrSkyAlarms(_alarmMap);
    			//FrSkyServer.database.saveModel(_model);
    			FrSkyServer.saveModel(_model);

    			this.setResult(RESULT_OK);
				this.finish();

    			break;
    		case R.id.FrSkySettings_btnGetFromModule:
    			if(FrSkyServer.D)Log.d(TAG,"Try to fetch alarms from the module");
    			btnGetAlarmsFromModule.setEnabled(false);
    			server.recordAlarmsFromModule(_model.getId());
    			// register a listener for a full update
    			break;
    	}
	}

	public void populateGUI()
	{
		Iterator<Alarm> i = _alarmMap.values().iterator();
		//for(Alarm a : _alarmMap.values())
		int id;
		while(i.hasNext())
		{
			Alarm a = i.next();
			// Line 1
			//LinearLayout line1 = new LinearLayout(getApplicationContext());
			TextView tvTitle = new TextView(getApplicationContext());
			tvTitle.setText(a.getName());
			tvTitle.setBackgroundColor(0xff909090);
			tvTitle.setTextColor(0xffffffff);
			
			
			
			// // Source channel
			ArrayAdapter<Channel> channelDescriptionAdapter  = new ArrayAdapter<Channel> (getApplicationContext(),android.R.layout.simple_spinner_item,_model.getAllowedSourceChannels(a));
			
			channelDescriptionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			
			// Spinner contents
			// Alarm Level, Off, Low, Mid, High
			//ArrayAdapter<CharSequence> 
			

			
			ArrayAdapter<CharSequence> alarmLevelAdapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.alarm_level, android.R.layout.simple_spinner_item );
			// Alarm relativity: Greater than, Lower than
			//ArrayAdapter<CharSequence> 
			ArrayAdapter<CharSequence> alarmRelAdapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.alarm_relative, android.R.layout.simple_spinner_item );
			
			
			
			// Setup "spinner design"
			alarmRelAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
			alarmLevelAdapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item);
			
			
			Spinner sourceSp = new Spinner(this);
			sourceSp.setAdapter(channelDescriptionAdapter);
			sourceSp.setId(ID_ALARM_SPINNER_SOURCECHANNEL+a.getFrSkyFrameType());
			
			int len = channelDescriptionAdapter.getCount();
			for(int j=0;j<len;j++)
			{
				Channel c = (Channel) sourceSp.getItemAtPosition(j);
				if(c.getId()==a.getUnitChannelId())
				{
					sourceSp.setSelection(j);
					break;
				}
			}
			
			
			sourceSp.setOnItemSelectedListener(new OnItemSelectedListener(){
				@Override
				public void onItemSelected(AdapterView<?> spinner, View arg1,
						int arg2, long arg3) {
					if(spinner.getSelectedItem() instanceof Channel)
					{
						Channel sourceChannel = (Channel) spinner.getSelectedItem();
						int alarmId = spinner.getId()-ID_ALARM_SPINNER_SOURCECHANNEL;
						Alarm a = _alarmMap.get(alarmId);
						if(a!=null)
						{
							a.setUnitChannel(sourceChannel);
							updateAlarmDescription(alarmId);
						}
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub
					
				}
			});
			if((a.getFrSkyFrameType()==Frame.FRAMETYPE_ALARM1_RSSI)||(a.getFrSkyFrameType()==Frame.FRAMETYPE_ALARM2_RSSI))
			{
				sourceSp.setEnabled(false);
			}
			//sourceSp.setSelection(a.getAlarmLevel());
			
			
			//ArrayAdapter<CharSequence> lvlAdapter = ArrayAdapter.createFromResource(this, R.array.alarm_level, android.R.layout.simple_spinner_item );
			//Spinner levelSp = new Spinner(getApplicationContext());
			Spinner levelSp = new Spinner(this);
			levelSp.setAdapter(alarmLevelAdapter);
			levelSp.setId(ID_ALARM_SPINNER_LEVEL+a.getFrSkyFrameType());
			levelSp.setSelection(a.getAlarmLevel());
			
			levelSp.setOnItemSelectedListener(new OnItemSelectedListener(){
				@Override
				public void onItemSelected(AdapterView<?> spinner, View arg1,
						int arg2, long arg3) {
					
						int alarmId = spinner.getId()-ID_ALARM_SPINNER_LEVEL;
						Alarm a = _alarmMap.get(alarmId);
						if(a!=null)
						{
							a.setAlarmLevel(spinner.getSelectedItemPosition());
							updateAlarmDescription(alarmId);
						}
					
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub
					
				}
			});
			
			
			Spinner relSp = new Spinner(this);
			relSp.setAdapter(alarmRelAdapter);
			relSp.setId(ID_ALARM_SPINNER_RELATIVE+a.getFrSkyFrameType());
			relSp.setSelection(a.getGreaterThan());
			relSp.setOnItemSelectedListener(new OnItemSelectedListener(){
				@Override
				public void onItemSelected(AdapterView<?> spinner, View arg1,
						int arg2, long arg3) {
					
						int alarmId = spinner.getId()-ID_ALARM_SPINNER_RELATIVE;
						Alarm a = _alarmMap.get(alarmId);
						if(a!=null)
						{
							a.setGreaterThan(spinner.getSelectedItemPosition());
							updateAlarmDescription(alarmId);
						}
					
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub
					
				}
			});
			
			

			SeekBar thresholdSb = new SeekBar(this);
			thresholdSb.setMax(a.getMaxThreshold()-a.getMinThreshold()+1);
			thresholdSb.setProgress(a.getThreshold()-a.getMinThreshold());
			thresholdSb.setId(ID_ALARM_SEEKBAR_THRESHOLD+a.getFrSkyFrameType());
			//thresholdSb.setOnSeekBarChangeListener(this);
			thresholdSb.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					int alarmId = seekBar.getId()-ID_ALARM_SEEKBAR_THRESHOLD;
					Alarm a = _alarmMap.get(alarmId);
					if(a!=null)
					{
						a.setThreshold(seekBar.getProgress()+a.getMinThreshold());
						updateAlarmDescription(alarmId);
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
				
			});
			
			Button sendABtn = new Button(this);
			sendABtn.setText("Send "+a.getName());
			sendABtn.setId(ID_ALARM_BUTTON_SEND+a.getFrSkyFrameType());
			sendABtn.setEnabled(_btSendButtonsEnabled);
			
			sendABtn.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					int alarmId = v.getId()-ID_ALARM_BUTTON_SEND;
					Alarm a = _alarmMap.get(alarmId);
					if(a!=null)
					{
						server.send(a.toFrame());
						
					}
					
				}

				
				
			});
			
			
			TextView descTv = new TextView(this);
			descTv.setId(ID_ALARM_TEXTVIEW_DESCRIPTION+a.getFrSkyFrameType());
			descTv.setText(a.toString());
			
			TextView tvLevel = new TextView(getApplicationContext());
			tvLevel.setText("Alarm Level:");
			
			TextView tvRel = new TextView(getApplicationContext());
			tvRel.setText("When");
			
			TextView tvUnits = new TextView(getApplicationContext());
			tvUnits.setText("Use units from channel");
			
			ll.addView(tvTitle);
			 
			ll.addView(tvLevel);
			
			ll.addView(levelSp);
			
			ll.addView(tvRel);
			ll.addView(relSp);
			ll.addView(thresholdSb);
			
			ll.addView(tvUnits);
			ll.addView(sourceSp);
			
			ll.addView(descTv);
			
			
			ll.addView(sendABtn);
			
			
			// Threshold
			// To Text
			// // Individual Send
		}
	}

	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	String msg = intent.getAction();
        	Bundle extras = intent.getExtras();
        	if(FrSkyServer.D)Log.i(TAG,"Received Broadcast: '"+msg+"'");
        	if(FrSkyServer.D)Log.i(TAG,"Comparing '"+msg+"' to '"+FrSkyServer.MESSAGE_ALARM_RECORDING_COMPLETE+"'");
        	if(msg.equals(FrSkyServer.MESSAGE_ALARM_RECORDING_COMPLETE))
        	{
        		if(server!=null)
    			{
	        		//for(Alarm a : _alarmMap.values())
	        		for(Alarm a : server.getRecordedAlarmMap().values())
	        		{
	        			
	        				if((a.getFrSkyFrameType()!=Frame.FRAMETYPE_ALARM1_RSSI) && (a.getFrSkyFrameType()!=Frame.FRAMETYPE_ALARM2_RSSI))
	        				{
		        				try
		        				{
		        					Log.w(TAG,"Got recorded alarm: "+a.getFrSkyFrameType()+": "+
		        				server.getRecordedAlarmMap().get(a.getFrSkyFrameType()).getThreshold());
	//		        				a.setThreshold(server.getRecordedAlarmMap().get(a.getFrSkyFrameType()).getThreshold());
	//		        				a.setAlarmLevel(server.getRecordedAlarmMap().get(a.getFrSkyFrameType()).getAlarmLevel());
	//		        				a.setGreaterThan(server.getRecordedAlarmMap().get(a.getFrSkyFrameType()).getGreaterThan());
			        				updateAlarmVisualization(a);
			        				updateAlarmDescription(a.getFrSkyFrameType());
		        				}
		        				catch(Exception e)
		        				{
		        					if(FrSkyServer.D)Log.e(TAG,"Failed to get alarms from the server..");
		        					//if(FrSkyServer.D)Log.e(TAG,e.getMessage());
		        				}
	        				}
	        			
	        		}
    			}
        		//registerReceiver(mIntentReceiver, mIntentFilter);
        		//unregisterReceiver(mIntentReceiver);
        		//unregisterReceiver(this);
        		btnGetAlarmsFromModule.setEnabled(true);
        	}
        	else if(msg.equals(FrSkyServer.MESSAGE_BLUETOOTH_STATE_CHANGED))
        	{
        		if(server.getConnectionState()==BluetoothSerialService.STATE_CONNECTED)
    			{
    				//boolean setToWhenConnected=true;
    				_btSendButtonsEnabled = true;
//    				btnSend.setEnabled(setToWhenConnected);
    				
    			}
    			else
    			{
    				_btSendButtonsEnabled = false;
//    				boolean setToWhenNotConnected=false;
//    				btnSend.setEnabled(setToWhenNotConnected);
    			}
    			setStateBtButtons(_btSendButtonsEnabled);
        	}

        }
    };
	
}


