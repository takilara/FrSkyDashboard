package biz.onomato.frskydash;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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




public class DashboardActivity extends Activity implements OnClickListener {
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
    private ToggleButton btnTglSpeak;
    
    private IntentFilter mIntentFilter;
    // service stuff
    private FrSkyServer server;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"onCreate");
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
   
        
        // Setup Click Listeners
        View btnTest1 = findViewById(R.id.btnTest1);
        btnTest1.setOnClickListener(this);
        
        View btnTest2 = findViewById(R.id.btnTest2);
        btnTest2.setOnClickListener(this);
        
        View btnSpeak = findViewById(R.id.btnSpeak);
        btnSpeak.setOnClickListener(this);
        
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
		    	tv_ad1_val.setText(server.AD1.toString());
		    	tv_ad2_val.setText(server.AD2.toString());
		    	tv_rssitx_val.setText(server.RSSItx.toString());
		    	tv_rssirx_val.setText(server.RSSIrx.toString());
		    	if(server!=null)
		    	{
		    		tv_fps_val.setText(server.getFps());
		    	}

		    	tickHandler.postDelayed(this, 100);
			}
		};

	    mIntentFilter = new IntentFilter();
	    mIntentFilter.addAction(FrSkyServer.MESSAGE_STARTED);

		// Service stuff
		doBindService();
    }
    
 // Can be used to detect broadcasts from Service
    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          //do something
        	String msg = intent.getAction();
        	Log.i(TAG,"Received Broadcast: '"+msg+"'");
        	//Log.i(TAG,"Comparing '"+msg+"' to '"+FrSkyServer.MESSAGE_STARTED+"'");
        	Log.i(TAG,"I have received BroadCast that the server has started");
        	
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
			server = ((FrSkyServer.MyBinder) binder).getService();
			btnTglSpeak.setChecked(server.getCyclicSpeechEnabled());
			Log.i(TAG,"Bound to Service");
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
    	tickHandler.removeCallbacks(runnableTick);
    	tickHandler.post(runnableTick);

    	registerReceiver(mIntentReceiver, mIntentFilter);

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
    		Intent intent = new Intent(this, ModuleSettingsActivity.class);
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
	    }
    }
    
    
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_DATA_CHECK_CODE) {
        	Log.i(TAG,"Check for TTS complete");
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                //mTts = new TextToSpeech(globals, this);
                
                //mTts = globals.createSpeaker();
            	if(server!=null)	server.createSpeaker();
                
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


