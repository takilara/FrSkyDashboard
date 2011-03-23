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
    
    private Handler tickHandler;
    private Runnable runnableTick;
    
    MyApp globals;
    
    private TextView tv_ad1_val,tv_ad2_val,tv_rssitx_val,tv_rssirx_val;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        
        
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
        
        // Text to speech
        Log.i(TAG,"try to create TTS");
        mTts = new TextToSpeech(globals,
        		this //TextToSpeech.OnInitListener
        		);
        
        
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
    	Log.i(TAG,"Resume");
    	tickHandler.post(runnableTick);
    	
    }
    
    @Override
    public void onPause(){
    	super.onPause();
    	Log.i(TAG,"Pause");
    	tickHandler.removeCallbacks(runnableTick);
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
    	sayHello();
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
    		tv_ad1_val.setText(globals.AD1.toString());
    		break;
    	case R.id.btnTest2:
    		Log.i(TAG,"Switch activity");
    		Intent intent = new Intent(this, ModuleSettingsActivity.class);
    		startActivity(intent);
    		break;
    	case R.id.btnSpeak:
    		Log.i(TAG,"SPEAK something");
    		//saySomething(globals.AD1.getDescription()+": "+Float.toString(globals.AD1.getValue())+" "+globals.AD1.getLongUnit());
    		saySomething(globals.AD1.getDescription()+": "+globals.AD1.toString()+" "+globals.AD1.getLongUnit());
    		break;
    	}
    	
    }
    
    
    
}


