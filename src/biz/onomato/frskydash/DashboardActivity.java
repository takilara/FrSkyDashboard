package biz.onomato.frskydash;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.Locale;
import java.math.MathContext;




public class DashboardActivity extends Activity implements OnClickListener {
    private static final String TAG = "Dashboard"; 
    private TextToSpeech mTts;
    
    private int AD1;
    private int AD2;
    private Channel oAd1;
    
    // Used for GUI updates
    private Handler tickHandler;
    private Runnable runnableTick;
    
    
	// Used for Cyclic speak

    private int MY_DATA_CHECK_CODE;
    
    MyApp globals;
    
    private TextView tv_ad1_val,tv_ad2_val,tv_rssitx_val,tv_rssirx_val;
    private ToggleButton btnTglSpeak;
    private boolean _cyclicSpeakEnabled;
    
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
        globals = ((MyApp)getApplicationContext());

        //oAd1 = globals.getChannelById(AD1);
        oAd1 = globals.getChannelById(0);
        
       
        // Setup the form items        
        tv_ad1_val = (TextView) findViewById(R.id.ad1Value);
        tv_ad2_val = (TextView) findViewById(R.id.ad2Value);
        tv_rssitx_val = (TextView) findViewById(R.id.rssitxValue);
        tv_rssirx_val = (TextView) findViewById(R.id.rssirxValue);
        
   
        
        // Setup Click Listeners
        View btnTest1 = findViewById(R.id.btnTest1);
        btnTest1.setOnClickListener(this);
        
        View btnTest2 = findViewById(R.id.btnTest2);
        btnTest2.setOnClickListener(this);
        
        View btnSpeak = findViewById(R.id.btnSpeak);
        btnSpeak.setOnClickListener(this);
        
        btnTglSpeak = (ToggleButton) findViewById(R.id.dash_tglSpeak);
        btnTglSpeak.setOnClickListener(this);
        btnTglSpeak.setChecked(globals.getCyclicSpeechEnabled());
        
        globals.getWakeLock();
        
        
        // Code to update GUI cyclic
        tickHandler = new Handler();
		tickHandler.postDelayed(runnableTick, 100);
		runnableTick = new Runnable() {
			@Override
			public void run()
			{
				//Log.i(TAG,"Update GUI");
		    	tv_ad1_val.setText(globals.AD1.toString());
		    	tv_ad2_val.setText(globals.AD2.toString());
		    	tv_rssitx_val.setText(globals.RSSItx.toString());
		    	tv_rssirx_val.setText(globals.RSSIrx.toString());

		    	tickHandler.postDelayed(this, 100);
			}
		};

    }
    
    
    
    @Override
    public void onResume (){
    	super.onResume();
    	
    	// enable updates
    	Log.i(TAG,"onResume");
    	tickHandler.removeCallbacks(runnableTick);
    	tickHandler.post(runnableTick);
    	//globals.showIcon();
    	//speakHandler.postDelayed(runnableSpeaker, 20000);
    	
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	Log.i(TAG,"onPause");
    	tickHandler.removeCallbacks(runnableTick);
    	//speakHandler.removeCallbacks(runnableSpeaker);
    }
    
    
    // Used to 
    /*
    public void onInit(int status) {
    	Log.i(TAG,"TTS init");
    	// status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
    	if (status == TextToSpeech.SUCCESS) {
    	int result = mTts.setLanguage(Locale.US);
    	if (result == TextToSpeech.LANG_MISSING_DATA ||
    	result == TextToSpeech.LANG_NOT_SUPPORTED) {
    	// Lanuage data is missing or the language is not supported.
    	Log.e(TAG, "Language is not available.");
    	} else {
    	// Check the documentation for other possible result codes.
    	// For example, the language may be available for the locale,
    	// but not for the specified country and variant.
    	// The TTS engine has been successfully initialized.
    	// Allow the user to press the button for the app to speak again.
    	
    	// Greet the user.
    	
    	}
    	} else {
    	// Initialization failed.
    	Log.i(TAG,"Something wrong with TTS");
    	Log.e(TAG, "Could not initialize TextToSpeech.");
    	}
    }
    */
    
    
    public void onClick(View v) {
    	switch (v.getId()) {
    	case R.id.btnTest1:
    		Log.i(TAG,"Clicked Test");
    		//globals.AD1.setRaw(100);
    		this.startService(new Intent(this, FrSkyServer.class));
    		//tv_ad1_val.setText(globals.AD1.toString());
    		break;
    	case R.id.btnTest2:
    		Log.i(TAG,"Switch activity");
    		Intent intent = new Intent(this, ModuleSettingsActivity.class);
    		startActivity(intent);
    		break;
    	case R.id.btnSpeak:
    		Log.i(TAG,"SPEAK something");
    		globals.saySomething(globals.AD1.toVoiceString());
    		break;
    	
    	case R.id.dash_tglSpeak:
    		globals.setCyclicSpeech(btnTglSpeak.isChecked());
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
                
                mTts = globals.createSpeaker();
                
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
    	//intent.putExtra("command", "die");
    	//startService(intent);
    	stopService(intent);
    	
    	globals.die();
    	super.onBackPressed();
    	
    	return;
    }
    
    @Override
    public void onDestroy(){
    	//mTts.stop();
    	Log.i(TAG,"onDestroy");
    	super.onDestroy();
    	
    }
    
    
    
    
    public void onStop(){
    	super.onStop();
    	//mTts.stop();
    	Log.i(TAG,"onStop");
    }
    
}


