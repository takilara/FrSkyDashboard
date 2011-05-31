package biz.onomato.frskydash;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Locale;
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

    private int MY_DATA_CHECK_CODE;
    
    //MyApp globals;
    
    private TextView tv_ad1_val,tv_ad2_val,tv_rssitx_val,tv_rssirx_val,tv_fps_val;
    private TextView tv_dash_ch0NameDesc,tv_dash_ch1NameDesc;
    private ToggleButton btnTglSpeak;
    private TextToSpeech mTts;
    
    private IntentFilter mIntentFilter;
    // service stuff
    private FrSkyServer server=null;
    
    private boolean createSpeakerLater=false;
	private SharedPreferences settings;
    
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"onCreate");
        
        
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
        
        // Setup Click Listeners
        View btnTest1 = findViewById(R.id.btnTest1);
        btnTest1.setOnClickListener(this);
        
        View btnTest2 = findViewById(R.id.btnTest2);
        btnTest2.setOnClickListener(this);
        
        View btnSpeak = findViewById(R.id.btnSpeak);
        btnSpeak.setOnClickListener(this);
        
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

		// Service stuff
		doBindService();
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
//			chkCyclicSpeakerEnabled.setChecked(settings.getBoolean("cyclicSpeakerEnabledAtStartup",false));
//	        chkLogToRaw.setChecked(settings.getBoolean("logToRaw",false));
//	        chkLogToHuman.setChecked(settings.getBoolean("logToHuman",false));
//	        chkLogToCsv.setChecked(settings.getBoolean("logToCsv",false));
			
			
			//server.setLogToRaw(settings.getBoolean("logToRaw",false));
			//server.setLogToHuman(settings.getBoolean("logToHuman",false));
			//server.setLogToCsv(settings.getBoolean("logToCsv",false));
			
			btnTglSpeak.setChecked(server.getCyclicSpeechEnabled());
    		tv_dash_ch0NameDesc.setText(server.AD1.getName()+": "+server.AD1.getDescription());
    		tv_dash_ch1NameDesc.setText(server.AD2.getName()+": "+server.AD2.getDescription());
			
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
    	}
    	tickHandler.removeCallbacks(runnableTick);
    	tickHandler.post(runnableTick);

    	registerReceiver(mIntentReceiver, mIntentFilter);
    	
    	// Update text in case change
    	//dash_ch0NameDesc

    	//globals.showIcon();
    	//speakHandler.postDelayed(runnableSpeaker, 20000);
    	
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
    	switch (v.getId()) {
    	case R.id.btnTest1:
    		Log.i(TAG,"Clicked Test");
    		// Testing controlling the service useing bound methods
    		if (server != null) {
    			int[] cf = server.getCurrentFrame();
    			
    			Log.i(TAG,"Data from service:"+Frame.frameToHuman(cf));
    		}
    		else
    		{
    			Log.i(TAG,"Service not bound");
    		}
    		break;
    	case R.id.btnTest2:
    		Log.i(TAG,"Switch activity");
    		Intent intent = new Intent(this, ActivityModuleSettings.class);
    		startActivity(intent);
    		break;
    	case R.id.btnSpeak:
    		Log.i(TAG,"SPEAK something");
    		//globals.saySomething(globals.AD1.toVoiceString());
    		if(server!=null) server.saySomething(server.AD1.toVoiceString());
    		break;
    	
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
    		startActivity(i);
    		break;
    	case R.id.dash_btnEditChannel1:
    		Log.i(TAG,"Edit channel 1");
    		Intent ii = new Intent(this, ActivityChannelConfig.class);
    		ii.putExtra("channelId", 1);
    		startActivity(ii);
    		break;
    	}
    }
    
    
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_DATA_CHECK_CODE) {
        	Log.i(TAG,"Check for TTS complete");
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                //mTts = new TextToSpeech(getApplicationContext(), this);
                
                //mTts = globals.createSpeaker();
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
                
                //sayHello();
            } else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(
                    TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
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
    
}


