package biz.onomato.frskydash;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Locale;
import java.util.UUID;
import java.io.OutputStream;
import java.math.MathContext;




public class ActivityDashboard extends Activity implements OnClickListener {
    private static final String TAG = "Dashboard"; 
    //private TextToSpeech mTts;
    
    //private int AD1;
    //private int AD2;
    //private Channel oAd1;
    
    // Used for GUI updates
    private Handler tickHandler;
    private Runnable runnableTick;
    
    
	// Used for Cyclic speak

    private static final int MY_DATA_CHECK_CODE = 7;
    private static final int CHANNEL_CONFIG_RETURN = 1;
    
    //MyApp globals;
    
    private TextView tv_ad1_val,tv_ad2_val,tv_ad1_unit,tv_ad2_unit,tv_rssitx_val,tv_rssirx_val,tv_fps_val;
    private TextView tv_dash_ch0NameDesc,tv_dash_ch1NameDesc;
    private ToggleButton btnTglSpeak;
    private TextToSpeech mTts;
    
    private IntentFilter mIntentFilter;
    // service stuff
    private FrSkyServer server=null;
    
    private boolean createSpeakerLater=false;
	private SharedPreferences settings;
    
	
	// Bluetooth stuff
	private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    //Well known SPP UUID (will *probably* map to RFCOMM channel 1 (default) if not in use);
    //see comments in onResume().
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    public static final String DEVICE_NAME = "device_name";
    private String mConnectedDeviceName = null;
    public static final String TOAST = "toast";
    private static BluetoothSerialService mSerialService = null;
    
    private static final int REQUEST_CONNECT_DEVICE = 6;
    private static final int REQUEST_ENABLE_BT = 2;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"onCreate");
        
        
        // Service stuff
        doBindService();
     		
        //Start the server service
     	//this.startService(new Intent().setClass(this, FrSkyServer.class));
        
		Log.i(TAG,"Try to load settings");
        settings = getPreferences(MODE_PRIVATE);
        
        setContentView(R.layout.activity_dashboard);
        
        //Activity parent = getParent();
        //Context context = (parent == null ? this : parent);
        
		
        
        // Check for TTS
        Log.i(TAG,"Checking for TTS");
        Intent checkSpeakIntent = new Intent();
        checkSpeakIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkSpeakIntent, MY_DATA_CHECK_CODE);
        
        
        
        
        
        // Fetch globals:
        //globals = ((MyApp)getApplicationContext());

        //oAd1 = globals.getChannelById(AD1);
        //oAd1 = globals.getChannelById(0);
        
       
        // Setup the form items        
        tv_ad1_val = (TextView) findViewById(R.id.ad1Value);
        tv_ad2_val = (TextView) findViewById(R.id.ad2Value);
        tv_rssitx_val = (TextView) findViewById(R.id.rssitxValue);
        tv_rssirx_val = (TextView) findViewById(R.id.rssirxValue);
        Log.d(TAG,"Looing for fpsValue");
        tv_fps_val = (TextView) findViewById(R.id.fpsValue);
        Log.d(TAG,"Found fpsValue: "+tv_fps_val.toString());
   
        tv_dash_ch0NameDesc = (TextView) findViewById(R.id.dash_ch0NameDesc);
        tv_dash_ch1NameDesc = (TextView) findViewById(R.id.dash_ch1NameDesc);
        
        tv_ad1_unit = (TextView) findViewById(R.id.ad1Unit);
        tv_ad2_unit = (TextView) findViewById(R.id.ad2Unit);
        
        // Setup Click Listeners
                
        View btnEditChannel0 = findViewById(R.id.dash_btnEditChannel0);
        btnEditChannel0.setOnClickListener(this);
        
        View btnEditChannel1 = findViewById(R.id.dash_btnEditChannel1);
        btnEditChannel1.setOnClickListener(this);
        
        btnTglSpeak = (ToggleButton) findViewById(R.id.dash_tglSpeak);
        btnTglSpeak.setOnClickListener(this);
        //btnTglSpeak.setChecked(globals.getCyclicSpeechEnabled());
        
        
        //globals.getWakeLock();
        
        
        // Code to update GUI cyclic
        tickHandler = new Handler();
		tickHandler.postDelayed(runnableTick, 100);
		runnableTick = new Runnable() {
			@Override
			public void run()
			{
				//Log.i(TAG,"Update GUI");
				if(server!=null)
				{
			    	tv_ad1_val.setText(server.AD1.toString());
			    	tv_ad2_val.setText(server.AD2.toString());
			    	tv_rssitx_val.setText(server.RSSItx.toString());
			    	tv_rssirx_val.setText(server.RSSIrx.toString());
			    	tv_fps_val.setText(server.getFps());
		    	}

		    	tickHandler.postDelayed(this, 100);
			}
		};

	    mIntentFilter = new IntentFilter();
	    mIntentFilter.addAction(FrSkyServer.MESSAGE_STARTED);
	    mIntentFilter.addAction(FrSkyServer.MESSAGE_SPEAKERCHANGE);

		
		
		// check for bt
		checkForBt();
    }
    
    
    
    public void checkForBt()
    {
	    Log.i(TAG,"Check for BT");
	    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	    if (mBluetoothAdapter == null) {
	        // Device does not support Bluetooth
	    	Log.i(TAG,"Device does not support Bluetooth");
	    	// Disable all BT related menu items
	    	// Display message stating only sim is available
	    	notifyBtNotEnabled();
	    }
	    
	    // popup to enable BT if not enabled
	    if (mBluetoothAdapter != null)
	    {
	        if (!mBluetoothAdapter.isEnabled()) {
	            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	        }
	        else
	        {
	        	//MenuItem tItem = (MenuItem)  _menu.findItem(R.id.connect_bluetooth);
	        	//tItem.setEnabled(true);
	        }
	    }
    }
    
    
    
    
    
    // Can be used to detect broadcasts from Service
    // Remember to add the message to the intentfilter (mIntentFilter) above
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          //do something
        	String msg = intent.getAction();
        	Log.i(TAG,"Received Broadcast: '"+msg+"'");
        	Log.i(TAG,"Comparing '"+msg+"' to '"+FrSkyServer.MESSAGE_SPEAKERCHANGE+"'");
        	if(msg.equals(FrSkyServer.MESSAGE_STARTED))
        		Log.i(TAG,"I have received BroadCast that the server has started");
        	if(msg.equals(FrSkyServer.MESSAGE_SPEAKERCHANGE))
        	{
        		Log.i(TAG,"I have received BroadCast that cyclic speaker has toggled");
        		if(server!=null)
        			btnTglSpeak.setChecked(server.getCyclicSpeechEnabled());
        	}
        	
        		
        	// It is currently not doing anything
        	//doBindService();
        }
    };
    
    
    void doBindService() {
    	//bindService(new Intent(this, FrSkyServer.class), mConnection, Context.BIND_AUTO_CREATE);
		Log.i(TAG,"Start the server service if it is not already started");
		startService(new Intent(this, FrSkyServer.class));
		Log.i(TAG,"Try to bind to the service");
		getApplicationContext().bindService(new Intent(this, FrSkyServer.class), mConnection,0);
		//bindService(new Intent(this, FrSkyServer.class), mConnection, Context.BIND_AUTO_CREATE);
    }
    
    void doUnbindService() {
            if (server != null) {
            // Detach our existing connection.
            unbindService(mConnection);
        }
    }
    
    
    

    
    private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			Log.i(TAG,"Bound to Service");
			server = ((FrSkyServer.MyBinder) binder).getService();
			server.setSettings(settings);	// Make sure server has settings available
			
			if(createSpeakerLater)	// server was not ready when TTS check finished
			{
				server.createSpeaker();
			}
			Log.i(TAG,"Setting up server from settings");
	
			btnTglSpeak.setChecked(server.getCyclicSpeechEnabled());

			onResume();
			
		}

		public void onServiceDisconnected(ComponentName className) {
			server = null;
		}
	};
    
    @Override
    public void onResume (){
    	super.onResume();
    	
    	// enable updates
    	Log.i(TAG,"onResume");
    	if(server != null)
    	{
    		btnTglSpeak.setChecked(server.getCyclicSpeechEnabled());
    		
    		tv_dash_ch0NameDesc.setText(server.AD1.getName()+": "+server.AD1.getDescription());
    		tv_dash_ch1NameDesc.setText(server.AD2.getName()+": "+server.AD2.getDescription());
    		tv_ad1_unit.setText(server.AD1.getShortUnit());
    		tv_ad2_unit.setText(server.AD2.getShortUnit());
    	}
    	tickHandler.removeCallbacks(runnableTick);
    	tickHandler.post(runnableTick);

    	registerReceiver(mIntentReceiver, mIntentFilter);
    	
   	
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	Log.i(TAG,"onPause");
    	unregisterReceiver(mIntentReceiver);

    	tickHandler.removeCallbacks(runnableTick);
    	//speakHandler.removeCallbacks(runnableSpeaker);
    }
    
    
   
    
    public void onClick(View v) {
    	switch (v.getId()) 
    	{
	    	case R.id.dash_tglSpeak:
	    		
	    		//globals.setCyclicSpeech(btnTglSpeak.isChecked());
	    		if(server!=null) {server.setCyclicSpeech(btnTglSpeak.isChecked());}
	    		
	    		
	    		
	    		// Testing controlling the service using intents
	    		Intent speechIntent = new Intent(this,FrSkyServer.class);
	    		if(btnTglSpeak.isChecked())
	    		{
	    			speechIntent.putExtra("command", FrSkyServer.CMD_START_SPEECH);
	    		}
	    		else
	    		{
	    			speechIntent.putExtra("command", FrSkyServer.CMD_STOP_SPEECH);
	    		}
	    		this.startService(speechIntent);
				break;
		    
	    	case R.id.dash_btnEditChannel0:
	    		Log.i(TAG,"Edit channel 0");
	    		Intent i = new Intent(this, ActivityChannelConfig.class);
	    		i.putExtra("channelId", 0);
	    		//startActivity(i);
	    		startActivityForResult(i,CHANNEL_CONFIG_RETURN);
	    		break;
	    	case R.id.dash_btnEditChannel1:
	    		Log.i(TAG,"Edit channel 1");
	    		Intent ii = new Intent(this, ActivityChannelConfig.class);
	    		ii.putExtra("channelId", 1);
	    		startActivityForResult(ii,CHANNEL_CONFIG_RETURN);
	    		break;
    	}
    }
    
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
    	switch (requestCode)
    	{
            case REQUEST_CONNECT_DEVICE:

                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                                         .getString(ActivityDeviceList.EXTRA_DEVICE_ADDRESS);
                    // Get the BLuetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    //mSerialService.connect(device);         
                    
                    // pass responsibility to the server
                    server.connect(device);
                }
                break;

            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "BT now enabled");
                    //MenuItem tItem = (MenuItem)  findViewById(R.id.connect_bluetooth);
                    
        //            finishDialogNoBluetooth();                
                }
                else
                {
                	Log.d(TAG,"BT not enabled");
                	// Disable all BT related menu items
                	// Display message stating only sim is available
                	notifyBtNotEnabled();
                }
                break;
            case MY_DATA_CHECK_CODE:
	        	Log.i(TAG,"Check for TTS complete");
	            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) 
	            {
	            	Log.i(TAG,"speech capabilities ok");
	            	if(server!=null)
	            	{
	            		server.createSpeaker();
	            	}
	            	else
	            	{
	            		Log.i(TAG,"Server not ready yet, postpone");
	            		createSpeakerLater= true;
	            	}
	                
	            
	            } 
	            else 
	            {
	                // missing data, install it
	                Intent installIntent = new Intent();
	                installIntent.setAction(
	                    TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
	                startActivity(installIntent);
	            }
	            break;
            case CHANNEL_CONFIG_RETURN:
	        	switch(resultCode)
	        	{
	        		case RESULT_OK:
	        			Log.i(TAG,"User saved new settings");
	        			break;
	        		case RESULT_CANCELED:
	        			Log.i(TAG,"User cancelled with back");
	        			break;
	        	}
	        	break;
        }
        	
        // --
    }
    
    @Override
    public void onBackPressed() {
    	Log.i(TAG,"Back pressed");
    	
    	Intent intent = new Intent(this, FrSkyServer.class);
    	stopService(intent);
//    	server.die();
    	
    	//globals.die();
    	super.onBackPressed();
    	
    	return;
    }
    
    @Override
    public void onDestroy(){
    	//mTts.stop();
    	Log.i(TAG,"onDestroy");
    	super.onDestroy();
    	doUnbindService();
    	
    }
    
    
    
    
    public void onStop(){
    	super.onStop();
    	//mTts.stop();
    	Log.i(TAG,"onStop");
    }
    
    
    // From tabhost
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	Log.i(TAG,"Create Menu");
    	super.onCreateOptionsMenu(menu);
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.menu, menu);
    	
    	return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
    	super.onPrepareOptionsMenu(menu);
    	MenuItem tConItem = (MenuItem)  menu.findItem(R.id.connect_bluetooth);
    	MenuItem tDisConItem = (MenuItem)  menu.findItem(R.id.disconnect_bluetooth);
    	//if(mBluetoothAdapter).
    	if (mBluetoothAdapter != null)
    	{
	    	if (!mBluetoothAdapter.isEnabled()) {
	    		tConItem.setEnabled(false);
	    		tDisConItem.setEnabled(false);
	    	}
	    	else
	    	{
	    		tConItem.setEnabled(true);
	    		tDisConItem.setEnabled(true);
	    	}
    	}
    	else
    	{
    		tConItem.setEnabled(false);
    		tDisConItem.setEnabled(false);
    	}
	    	
    	if(server.getConnectionState()==BluetoothSerialService.STATE_NONE)
    	{
    		tConItem.setVisible(true);
    		tDisConItem.setVisible(false);
    	}
    	else
    	{
    		tConItem.setVisible(false);
    		tDisConItem.setVisible(true);
    	}
	
    		
    	
		return true;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	Log.i(TAG,"User has clicked something");
    	switch(item.getItemId()) 
    	{
    		case R.id.settings:
    			Log.i(TAG,"User clicked on Settings");
    			//Toast.makeText(this, "User clicked on Settings", Toast.LENGTH_LONG).show();
    			Intent intent = new Intent(this,ActivityApplicationSettings.class);
    			startActivity(intent);
    			break;
    		case R.id.module_settings:
    			Log.i(TAG,"User clicked on Module Settings");
    			//Toast.makeText(this, "User clicked on Settings", Toast.LENGTH_LONG).show();
    			Intent mIntent = new Intent(this,ActivityModuleSettings.class);
    			startActivity(mIntent);
    			break;
    		case R.id.menu_choose_simulator:
    			Log.i(TAG,"User clicked on Simulator");
    			//Toast.makeText(this, "User clicked on Settings", Toast.LENGTH_LONG).show();
    			Intent sIntent = new Intent(this,ActivitySimulator.class);
    			startActivity(sIntent);
    			break;
    		
    		case R.id.connect_bluetooth:
    			if (server.getConnectionState() == BluetoothSerialService.STATE_NONE) {
            		// Launch the DeviceListActivity to see devices and do scan
            		Intent serverIntent = new Intent(this, ActivityDeviceList.class);
            		startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            	}
            	else
                	if (server.getConnectionState() == BluetoothSerialService.STATE_CONNECTED) {
                		// Connected, reconnect
                		server.reconnectBt();
                		//mSerialService.stop();
    		    		//mSerialService.start();
                	}
                return true;
    		case R.id.disconnect_bluetooth:
    			server.disconnect();
    			break;
             
    			
    	}
    	return true;
    	
    }
   
    
    
    public void notifyBtNotEnabled()
    {
    	Toast.makeText(this, "Bluetooth not enabled, only simulations are available", Toast.LENGTH_LONG).show();
    	//MenuItem tItem = (MenuItem)  _menu.findItem(R.id.connect_bluetooth);
    	//tItem.setEnabled(false);
    }
    
    
}


