package biz.onomato.frskydash;

import android.app.Activity;
import android.app.Dialog;
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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.speech.tts.TextToSpeech;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Locale;
import java.util.UUID;
import java.io.OutputStream;
import java.math.MathContext;




public class ActivityDashboard extends Activity implements OnClickListener {
    private static final String TAG = "Dashboard"; 
    
    private static final int DIALOG_ABOUT_ID=0;
    private Dialog dlgAbout;
    private static final boolean DEBUG=true;
    private static boolean _enableDebugActivity=false;
    
    // Used for GUI updates
    private Handler tickHandler;
    private Runnable runnableTick;
    
    private boolean bluetoothEnabledAtStart;
    private int _clickToDebug=0;
    
    
    
	// Used for Cyclic speak

    private static final int MY_DATA_CHECK_CODE = 7;
    private static final int CHANNEL_CONFIG_RETURN = 1;
    private static final int MODEL_CONFIG_RETURN = 8;
    
    //MyApp globals;
    
    private TextView tv_ad1_val,tv_ad2_val,tv_ad1_unit,tv_ad2_unit;
    private TextView tv_statusBt,tv_statusRx,tv_statusTx;
    private TextView tv_rssitx,tv_rssirx,tv_fps;
    private TextView tv_dash_ch0NameDesc,tv_dash_ch1NameDesc;
    private LinearLayout llDashboardMain;
    private LinearLayout llDashboardChannels;
    private TableLayout tlChannelsTable;
    private ScrollView svDashboard;
    private ToggleButton btnTglSpeak;
    private Button btnConfigCurrentModel;
    private TextToSpeech mTts;
    
    private IntentFilter mIntentFilter;
    private IntentFilter mIntentFilterBt;
    private int _flashCounter=0;
    // service stuff
    private FrSkyServer server=null;
    
    private boolean createSpeakerLater=false;
	//private SharedPreferences settings;
    
	
	// Bluetooth stuff
	private BluetoothAdapter mBluetoothAdapter = null;
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    public static final String DEVICE_NAME = "device_name";
    //private String mConnectedDeviceName = null;
    public static final String TOAST = "toast";
    //private static BluetoothSerialService mSerialService = null;
    
    private static final int REQUEST_CONNECT_DEVICE = 6;
    private static final int REQUEST_ENABLE_BT = 2;
    
    // graphical stuff:
    private float scale;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"onCreate");

        _enableDebugActivity=false;
        // Audio Setup
        
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        
        //bluetoothEnabledAtStart = false;
        
        // Service stuff
        doBindService();
     		
        setContentView(R.layout.activity_dashboard);
        
        // setup scale for programatically setting sizes
        scale = getApplicationContext().getResources().getDisplayMetrics().density;
        if(DEBUG)Log.d(TAG,"Scale is: "+scale);
        
        // Check for TTS
        Log.i(TAG,"Checking for TTS");
        Intent checkSpeakIntent = new Intent();
        checkSpeakIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkSpeakIntent, MY_DATA_CHECK_CODE);
        
        
        // Setup the form items        
        tv_ad1_val = (TextView) findViewById(R.id.ad1Value);
        tv_ad2_val = (TextView) findViewById(R.id.ad2Value);

        
        tv_statusBt = (TextView) findViewById(R.id.dash_tvConnBt);
        tv_statusRx = (TextView) findViewById(R.id.dash_tvConnRx);
        tv_statusTx = (TextView) findViewById(R.id.dash_tvConnTx);
        
        tv_rssitx   = (TextView) findViewById(R.id.dash_tvRSSItx);
        tv_rssirx   = (TextView) findViewById(R.id.dash_tvRSSIrx);
        tv_fps      = (TextView) findViewById(R.id.dash_tvFps);
        
        
        tv_dash_ch0NameDesc = (TextView) findViewById(R.id.dash_ch0NameDesc);
        tv_dash_ch1NameDesc = (TextView) findViewById(R.id.dash_ch1NameDesc);
        
        tv_ad1_unit = (TextView) findViewById(R.id.ad1Unit);
        tv_ad2_unit = (TextView) findViewById(R.id.ad2Unit);
        
        View btnEditChannel0 = findViewById(R.id.dash_btnEditChannel0);
        View btnEditChannel1 = findViewById(R.id.dash_btnEditChannel1);
        btnTglSpeak = (ToggleButton) findViewById(R.id.dash_tglSpeak);

        btnConfigCurrentModel = (Button) findViewById(R.id.dash_btnConfigCurrentModel);

        
        // dynamic content:
        tlChannelsTable	= (TableLayout) findViewById(R.id.dashChannelTable);
        svDashboard	=	(ScrollView) findViewById(R.id.ScrollViewDashboard);
        llDashboardMain = (LinearLayout) findViewById(R.id.llDashboardFull);
        llDashboardChannels = (LinearLayout) findViewById(R.id.llDashboardChannels);
        
        
        
        // Setup Click Listeners
        btnEditChannel0.setOnClickListener(this);
        btnEditChannel1.setOnClickListener(this);
        btnTglSpeak.setOnClickListener(this);
        btnConfigCurrentModel.setOnClickListener(this);

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
			    	tv_rssitx.setText("RSSItx: "+server.RSSItx.toString());
			    	
			    	tv_rssirx.setText("RSSIrx: "+server.RSSIrx.toString());
			    	
			    	tv_fps.setText("FPS: "+server.getFps());
			    	
			    	// set status lights
			    	if(server.statusBt)
			    	{
			    		tv_statusBt.setBackgroundColor(0xff00aa00);
			    		tv_statusBt.setText("Bt: UP");
			    		tv_statusBt.setTextColor(0xff000000);
			    	}
			    	else
			    	{
			    		// if connecting, do something else..
			    		if(server.getConnecting())
			    		{
			    			_flashCounter++;
			    			if(_flashCounter>=8) _flashCounter=0;
			    			if(_flashCounter<4)
			    			{
			    				tv_statusBt.setBackgroundColor(0xff00aa00);
			    				tv_statusBt.setText("Bt: Connecting");
			    				tv_statusBt.setTextColor(0xff000000);
			    			}
			    			else
			    			{
			    				tv_statusBt.setBackgroundColor(0xff000000);
				    			tv_statusBt.setText("Bt: Connecting");
				    			tv_statusBt.setTextColor(0xffaaaaaa);
			    			}
			    		}
			    		else
			    		{
			    			tv_statusBt.setBackgroundColor(0xffff0000);
			    			tv_statusBt.setText("Bt: DOWN");
			    			tv_statusBt.setTextColor(0xff000000);
			    		}
			    	}
			    	
			    	//if(server.fps>0)
			    	if(server.statusRx)
			    	{
			    		tv_statusRx.setBackgroundColor(0xff00aa00);
			    		tv_statusRx.setText("Rx: UP");
			    	}
			    	else
			    	{
			    		tv_statusRx.setBackgroundColor(0xffff0000);
			    		tv_statusRx.setText("Rx: DOWN");
			    	}
			    	
			    	if(server.statusTx)
			    	{
			    		tv_statusTx.setBackgroundColor(0xff00aa00);
			    		tv_statusTx.setText("Tx: UP");
			    	}
			    	else
			    	{
			    		tv_statusTx.setBackgroundColor(0xffff0000);
			    		tv_statusTx.setText("Tx: DOWN");
			    	}
			    	
		    	}

		    	tickHandler.postDelayed(this, 100);
			}
		};

		// Intentfilters for broadcast listeners
		// Listen for server intents
		mIntentFilter = new IntentFilter();
	    mIntentFilter.addAction(FrSkyServer.MESSAGE_STARTED);
	    mIntentFilter.addAction(FrSkyServer.MESSAGE_SPEAKERCHANGE);

	    // Listen for BT events (not used yet)
		mIntentFilterBt = new IntentFilter();
		mIntentFilterBt.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    }
    
    
    // About dialog
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        Log.i(TAG,"Make a dialog on context: "+this.getPackageName());
        
        switch(id) {
        case DIALOG_ABOUT_ID:
        	Log.i(TAG,"About dialog");
        	dialog = new Dialog(this);
            dialog.setContentView(R.layout.about_dialog);
            dialog.setTitle("About "+getString(R.string.app_name));
            TextView tvAboutVersion = (TextView) dialog.findViewById(R.id.tvAboutVersion);
            TextView tvAboutAuthor = (TextView) dialog.findViewById(R.id.tvAboutAuthor);
            tvAboutAuthor.setOnClickListener(new OnClickListener(){
            	public void onClick(View v)
            	{
            		//Log.d(TAG,"clicked author");
            		_clickToDebug++;
            		if(_clickToDebug>5)
            		{
            			Log.d(TAG,"Enable debugging");
            			Toast.makeText(getApplicationContext(), "Debugging enabled", Toast.LENGTH_LONG).show();
            	    	//MenuItem tDebug = (MenuItem) menu.findItem(R.id.menu_debug);
            			enableDebugging();
            	    	//tDebug.setVisible(false);
            		}
            	}
            	
            });
            
        	PackageManager pm = this.getPackageManager();
        	try
        	{
        		PackageInfo pInfo = pm.getPackageInfo(this.getPackageName(), PackageManager.GET_META_DATA);
        		tvAboutVersion.setText("Version: "+pInfo.versionName);
        		tvAboutAuthor.setText("Author: "+getString(R.string.author));
        	}
        	catch (Exception e)
        	{     		
        	}
            break;
        default:
            dialog = null;
        }
        return dialog;
    }
    
    
    
    public void enableDebugging()
    {
    	_enableDebugActivity=true;
    }
    
    // Check for bluetooth capabilities, request if no capabilities
    public void checkForBt()
    {
	    Log.i(TAG,"Check for BT capabilities");
	    mBluetoothAdapter = server.getBluetoothAdapter();
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
	        if (!mBluetoothAdapter.isEnabled() && !server.getBtAutoEnable()) {
	        	//bluetoothEnabledAtStart = false;
	        	Log.d(TAG,"Request BT enabling as BT not enabled and autoenable not set");
        		Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        		startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	        }
	    }
    }
    
    
    // Broadcast Listeners
    // Can be used to detect broadcasts from Service
    // Remember to add the message to the intentfilter (mIntentFilter) above
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	String msg = intent.getAction();
        	Bundle extras = intent.getExtras();
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

        }
    };
    
    
    // Can be used to detect broadcasts from Bluetooth
    // Remember to add the message to the intentfilter (mIntentFilterBt) above
    //TODO: Might not be neccessary, but leave in place if needed for "connecting" blinking
    private BroadcastReceiver mIntentReceiverBt = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	String msg = intent.getAction();

        	// does not work?
    		int cmd = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,-1);
    		Log.i(TAG,"CMD: "+cmd);
    		switch(cmd) {
    			case BluetoothAdapter.STATE_ON:
    				Log.d(TAG,"Bluetooth state changed to ON - try to autoconnect");
    				//btAutoConnect();
    				break;
    			case BluetoothAdapter.STATE_OFF:
    				Log.d(TAG,"Blueotooth state changed to OFF");
    				break;
    			default:
    				Log.d(TAG,"No information about "+msg);
    		}
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
			//server.setSettings(settings);	// Make sure server has settings available
			
			if(createSpeakerLater)	// server was not ready when TTS check finished
			{
				server.createSpeaker();
			}
			Log.i(TAG,"Setting up dashboard");
	
			Log.d(TAG,"Cyclic speaker should be set to "+server.getCyclicSpeechEnabledAtStartup()+" at startup");
			btnTglSpeak.setChecked(server.getCyclicSpeechEnabledAtStartup());
			
			//server.setCyclicSpeechEnabled(server.getCyclicSpeechEnabledAtStartup());

			// Check volume
			AudioManager audioManager = 
	        	    (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			
			//audioManager.startBluetoothSco();
			
	        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
	        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
	        double volPrc = currentVolume*100/maxVolume;
	        Log.d(TAG,String.format("Volume is [%s/%s] (%.2f %%)",currentVolume,maxVolume,volPrc));
	        if(server.getAutoSetVolume())
	        {
		        if(volPrc<server.getMinimumVolume())
		        {
		        	audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)Math.floor(server.getMinimumVolume()*maxVolume/100),AudioManager.FLAG_SHOW_UI);
		        }
	        }
			
			
			// check for bt
			checkForBt();
			
			populateChannelList();
			
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
    	_enableDebugActivity=false;
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

    	registerReceiver(mIntentReceiver, mIntentFilter);	  // Used to receive messages from Server
    	registerReceiver(mIntentReceiverBt, mIntentFilterBt); // Used to receive BT events
    
    	
    	
   	
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	Log.i(TAG,"onPause");
    	unregisterReceiver(mIntentReceiver);
    	unregisterReceiver(mIntentReceiverBt);

    	tickHandler.removeCallbacks(runnableTick);
    	//speakHandler.removeCallbacks(runnableSpeaker);
    }
    
    
    private void populateChannelList()
	{
		if(DEBUG) Log.d(TAG,"Populate list of channels");
		//tlChannelsTable.removeAllViews();
		llDashboardChannels.removeAllViews();
		final Model currentModel = server.getCurrentModel();
		int n = 0;
		if(DEBUG) Log.d(TAG,"Should add this amount of channels: "+currentModel.getChannels().length);
		for(Channel c: currentModel.getChannels())
		{
			if(DEBUG) Log.i(TAG,c.getDescription());
			LinearLayout llLine = new LinearLayout(getApplicationContext());
			llLine.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));

			// Add Description
			TextView tvDesc = new TextView(getApplicationContext());
			tvDesc.setText(c.getDescription());
			tvDesc.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.WRAP_CONTENT));
			
			llDashboardChannels.addView(tvDesc);
			
			// btn
			Button btnEdit = new Button(getApplicationContext());
			btnEdit.setText("...");
			btnEdit.setId(1000+n);// ID for delete should be 100+channelId

			btnEdit.setOnClickListener(new OnClickListener(){
				public void onClick(View v){
					if(DEBUG) Log.d(TAG,"Edit channel "+currentModel.getChannels()[v.getId()-1000].getDescription());
					// Launch editchannel with channel attached.. 
					Intent i = new Intent(getApplicationContext(), ActivityChannelConfig.class);
		    		//i.putExtra("channelId", 1);
					i.putExtra("channel", currentModel.getChannels()[v.getId()-1000]);
					i.putExtra("idInModel", v.getId()-1000);
		    		startActivityForResult(i,CHANNEL_CONFIG_RETURN);
				}
			});
			
			
			llLine.addView(btnEdit);

			// Value
			if(DEBUG) Log.d(TAG,"Add TextView for Value: "+c.getValue(true));
			TextView tvValue = new TextView(getApplicationContext());
			tvValue.setText(""+c.getValue());
			tvValue.setGravity(Gravity.RIGHT);
			
			tvValue.setTextSize(TypedValue.COMPLEX_UNIT_SP, 35);
			tvValue.setLayoutParams(new LinearLayout.LayoutParams(0,LayoutParams.WRAP_CONTENT,1));
			llLine.addView(tvValue);
			
			// Unit
			if(DEBUG) Log.d(TAG,"Add TextView for Unit: "+c.getShortUnit());
			TextView tvUnit = new TextView(getApplicationContext());
			tvUnit.setText(""+c.getShortUnit());
			tvUnit.setGravity(Gravity.LEFT);
			tvUnit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
			llLine.addView(tvUnit);
			
			// View for separator
			View v = new View(getApplicationContext());
			v.setBackgroundColor(0xFF909090);
			v.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,2));

			// Add line to channel List
			llDashboardChannels.addView(llLine);
			// Add separator view to channel List
			llDashboardChannels.addView(v);
			
			n++;
			
			
			
		}
		//ScrollViewDashboard
		//if(DEBUG)Log.d(TAG,"Request new layout of scrollView");
		//llDashboardMain.requestLayout();
	}
    
    public void onClick(View v) {
    	switch (v.getId()) 
    	{
	    	case R.id.dash_tglSpeak:
	    		
	    		//globals.setCyclicSpeech(btnTglSpeak.isChecked());
	    		// This toggles the speaker
	    		if(server!=null) {server.setCyclicSpeechEnabled(btnTglSpeak.isChecked());}
	    		
	    		
	    		
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
	    		
	    	case R.id.dash_btnConfigCurrentModel:
	    		if(DEBUG) Log.i(TAG,"Edit current model");
	    		Intent iii = new Intent(this, ActivityModelConfig.class);
	    		iii.putExtra("modelId", server.getCurrentModel().getId());
	    		startActivityForResult(iii,MODEL_CONFIG_RETURN);
	    		break;
    	}
    }
    
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
    	Channel returnChannel = null;
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
            case CHANNEL_CONFIG_RETURN:			// User edited a channel
	        	switch(resultCode)
	        	{
	        		case RESULT_OK:
	        			Log.i(TAG,"User saved new settings");
	        			break;
	        		case RESULT_CANCELED:
	        			Log.i(TAG,"User cancelled with back");
	        			break;
	        	}
	        	
	        	populateChannelList();
	        	break;
            case MODEL_CONFIG_RETURN:		// User edited a model, or swapped current model
	        	switch(resultCode)
	        	{
	        		case RESULT_OK:
	        			Log.i(TAG,"User saved new settings for current model");
	        			break;
	        		case RESULT_CANCELED:
	        			Log.i(TAG,"User cancelled with back");
	        			break;
	        	}
	        	
	        	populateChannelList();
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
    	
    	// stop bt
    	///TODO: Only do below if state was disabled before..
    	// handled in server
//    	if((bluetoothEnabledAtStart) && (mBluetoothAdapter != null))
//    	{
//    		mBluetoothAdapter.disable();
//    	}
    	
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
    	MenuItem tDebug = (MenuItem) menu.findItem(R.id.menu_debug);
    	if(DEBUG) Log.d(TAG,"prepare options");
    	
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
	
    	if(_enableDebugActivity)
    	{
    		tDebug.setVisible(true);
    	}
    	else
    	{
    		tDebug.setVisible(false);
    	}
    	
		return true;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	Log.i(TAG,"User has clicked something");
    	switch(item.getItemId()) 
    	{
	    	case R.id.menu_debug:
				Log.d(TAG,"Open Debug Activity");
				Intent mIntentDebug = new Intent(this,ActivityDebug.class);
    			startActivity(mIntentDebug);
				break;
    		case R.id.menu_about_dialog:
    			Log.d(TAG,"Open About dialog");
    			showDialog(DIALOG_ABOUT_ID);
    			Log.d(TAG,"Dialog now showing");
    			break;
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
    		case R.id.model_management:
    			Log.i(TAG,"User clicked on Manage models");
    			//Toast.makeText(this, "User clicked on Settings", Toast.LENGTH_LONG).show();
    			//Intent mIntent = new Intent(this,ActivityModuleSettings.class);
    			startActivityForResult(new Intent(this,ActivityModelManagement.class),MODEL_CONFIG_RETURN);
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
    }
    
    
}


