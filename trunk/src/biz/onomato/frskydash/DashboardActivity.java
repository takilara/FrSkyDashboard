package biz.onomato.frskydash;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.widget.TextView;
import java.util.Locale;




public class DashboardActivity extends Activity implements OnClickListener, TextToSpeech.OnInitListener {
    private static final String TAG = "Dashboard"; 
    private TextToSpeech mTts;
    
    private int AD1;
    private int AD2;
    private Channel oAd1;
    MyApp globals;
    
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
        
        Float newVal = oAd1.setRaw(50);
        
        TextView ad1Val = (TextView) findViewById(R.id.ad1Value);
		ad1Val.setText(Float.toString(newVal));
        
        
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
    		TextView ad1Val = (TextView) findViewById(R.id.ad1Value);
    		//float newVal = globals.setChannelById(AD1, 100);
    		Float newVal = oAd1.setRaw(10);
    		ad1Val.setText(Float.toString(newVal));
    		
    		//ad1Val.setText("3.5");
    		break;
    	case R.id.btnTest2:
    		Log.i(TAG,"Switch activity");
    		Intent intent = new Intent(this, ModuleSettingsActivity.class);
    		startActivity(intent);
    		break;
    	case R.id.btnSpeak:
    		Log.i(TAG,"SPEAK something");
    		saySomething(oAd1.getDescription()+": "+Float.toString(oAd1.getValue())+" "+oAd1.getLongUnit());
    		break;
    	}
    	
    }
    
    
    
}


