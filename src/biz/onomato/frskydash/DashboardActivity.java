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
import java.util.Locale;
import java.math.MathContext;




public class DashboardActivity extends Activity implements OnClickListener, TextToSpeech.OnInitListener {
    private static final String TAG = "Dashboard"; 
    private TextToSpeech mTts;
    
    private int AD1;
    private int AD2;
    private Channel oAd1;
    
    // Used for GUI updates
    private Handler tickHandler;
    private Runnable runnableTick;
    
    // Used for Cyclic speak
    private Handler speakHandler;
    private Runnable runnableSpeaker;
    
    private int _speakDelay;
    private int MY_DATA_CHECK_CODE;
    
    MyApp globals;
    
    private TextView tv_ad1_val,tv_ad2_val,tv_rssitx_val,tv_rssirx_val;
    private View btnTglSpeak;
    private boolean _cyclicSpeakEnabled;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG,"onCreate");
        setContentView(R.layout.activity_dashboard);
        try {
        	speakHandler.removeCallbacks(runnableSpeaker);
        }
        catch(Exception e) {
        }
        finally {}
        
        
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);
        
        
        _cyclicSpeakEnabled = false;
        _speakDelay = 30000;
        //Activity parent = getParent();
        //Context context = (parent == null ? this : parent);
        

        
        // Fetch globals:
        //MyApp appState = ((MyApp)getApplicationContext());
        //globals = (MyApp) this.getApplication();
        globals = ((MyApp)getApplicationContext());

        //AD1 = globals.createChannel("AD1", "Main cell voltage", 0, (float) 0.5, "V","Volt");
        
        //oAd1 = globals.getChannelById(AD1);
        oAd1 = globals.getChannelById(0);
        
       
        
        tv_ad1_val = (TextView) findViewById(R.id.ad1Value);
        tv_ad2_val = (TextView) findViewById(R.id.ad2Value);
        tv_rssitx_val = (TextView) findViewById(R.id.rssitxValue);
        tv_rssirx_val = (TextView) findViewById(R.id.rssirxValue);
        
   
        
        // Click Listeners
        View btnTest1 = findViewById(R.id.btnTest1);
        btnTest1.setOnClickListener(this);
        
        View btnTest2 = findViewById(R.id.btnTest2);
        btnTest2.setOnClickListener(this);
        
        View btnSpeak = findViewById(R.id.btnSpeak);
        btnSpeak.setOnClickListener(this);
        
        btnTglSpeak = findViewById(R.id.dash_tglSpeak);
        btnTglSpeak.setOnClickListener(this);
        btnTglSpeak.setPressed(_cyclicSpeakEnabled);
        
        
        
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
		
		// Cyclic speaker
        speakHandler = new Handler();
		
		runnableSpeaker = new Runnable() {
			@Override
			public void run()
			{
				Log.i(TAG,"Cyclic Speak stuff");
				mTts.speak(globals.AD1.toVoiceString(), TextToSpeech.QUEUE_ADD, null);
				mTts.speak(globals.AD2.toVoiceString(), TextToSpeech.QUEUE_ADD, null);
				mTts.speak(globals.RSSItx.toVoiceString(), TextToSpeech.QUEUE_ADD, null);
				mTts.speak(globals.RSSIrx.toVoiceString(), TextToSpeech.QUEUE_ADD, null);
				
				speakHandler.removeCallbacks(runnableSpeaker);
		    	speakHandler.postDelayed(this, _speakDelay);
			}
		};
		
		
		//speakHandler.postDelayed(runnableSpeaker, _speakDelay);

    }
    
    
    
    @Override
    public void onResume (){
    	super.onResume();
    	
    	// enable updates
    	Log.i(TAG,"onResume");
    	
    	tickHandler.post(runnableTick);
    	//speakHandler.postDelayed(runnableSpeaker, 20000);
    	
    	speakHandler.removeCallbacks(runnableSpeaker);
    	if(_cyclicSpeakEnabled)
    	{
    		speakHandler.postDelayed(runnableSpeaker,_speakDelay);
    	}
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	Log.i(TAG,"onPause");
    	tickHandler.removeCallbacks(runnableTick);
    	//speakHandler.removeCallbacks(runnableSpeaker);
    }
    
    
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
    
    private void sayHello() {
    	String myGreeting = "Text to Speech now enabled.";
    	mTts.speak(myGreeting,TextToSpeech.QUEUE_FLUSH,null);
    }
    
    private void saySomething(String myText) {
    	mTts.speak(myText, TextToSpeech.QUEUE_FLUSH, null);
    	//globals.saySomething(myText);
    }
    
    public void onClick(View v) {
    	switch (v.getId()) {
    	case R.id.btnTest1:
    		Log.i(TAG,"Clicked Test");
    		globals.AD1.setRaw(100);
    		//tv_ad1_val.setText(globals.AD1.toString());
    		break;
    	case R.id.btnTest2:
    		Log.i(TAG,"Switch activity");
    		Intent intent = new Intent(this, ModuleSettingsActivity.class);
    		startActivity(intent);
    		break;
    	case R.id.btnSpeak:
    		Log.i(TAG,"SPEAK something");
    		saySomething(globals.AD1.toVoiceString());
    		break;
    	
    	case R.id.dash_tglSpeak:
			_cyclicSpeakEnabled = !_cyclicSpeakEnabled;
			if(_cyclicSpeakEnabled){
				Log.i(TAG,"Enable cyclic speaker");
				speakHandler.removeCallbacks(runnableSpeaker);
				speakHandler.post(runnableSpeaker);
				//speakHandler.postDelayed(runnableSpeaker, _speakDelay);
			}
			else
			{
				Log.i(TAG,"Disable cyclic speaker");
				speakHandler.removeCallbacks(runnableSpeaker);
			}
			break;
	    	
	    }
    }
    
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                mTts = new TextToSpeech(globals, this);
                sayHello();
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
    	
    	// Stop speaker
    	_cyclicSpeakEnabled=false;
    	speakHandler.removeCallbacks(runnableSpeaker);
    	mTts.shutdown();
    	
 	
    	this.finish();
    	return;
    }
    
    @Override
    public void onDestroy(){
    	//mTts.stop();
    	Log.i(TAG,"onDestroy");
    	super.onDestroy();
    	_cyclicSpeakEnabled=false;
    	speakHandler.removeCallbacks(runnableSpeaker);
    	mTts.shutdown();
    	
    }
    
    
    
    
    public void onStop(){
    	super.onStop();
    	//mTts.stop();
    	Log.i(TAG,"onStop");
    }
    
}


