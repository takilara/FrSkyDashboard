package biz.onomato.frskydash;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.widget.TextView;
import java.util.Locale;




public class Frskydash extends Activity implements OnClickListener, TextToSpeech.OnInitListener {
    private static final String TAG = "FrSky"; 
    private TextToSpeech mTts;
    
    private int AD1;
    private int AD2;
    MyApp globals;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Fetch globals:
        //MyApp appState = ((MyApp)getApplicationContext());
        globals = (MyApp) this.getApplication();
        AD1 = globals.createChannel("AD1", "First analog channel", 0, (float) 0.5, "V","Volt");
        float newVal = globals.setChannelById(AD1, 200);
        
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
        mTts = new TextToSpeech(this,
        		this //TextToSpeech.OnInitListener
        		);
    }
    
    public void onInit(int status) {
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
    	Log.e(TAG, "Could not initialize TextToSpeech.");
    	}
    }
    
    private void sayHello() {
    	String myGreeting = "Main cell voltage: 3.4 Volt";
    	mTts.speak(myGreeting,TextToSpeech.QUEUE_FLUSH,null);
    }
    
    private void saySomething(String myText) {
    	mTts.speak(myText, TextToSpeech.QUEUE_FLUSH, null);
    }
    
    public void onClick(View v) {
    	switch (v.getId()) {
    	case R.id.btnTest1:
    		Log.i(TAG,"Clicked Test");
    		TextView ad1Val = (TextView) findViewById(R.id.ad1Value);
    		float newVal = globals.setChannelById(AD1, 100);
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
    		//sayHello();
    		saySomething("Main cell voltage: 3.8 Volt, estimated time left: 3.5 minutes");
    		break;
    	}
    	
    }
    
    
    
}


