package biz.onomato.frskydash;

import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.widget.Toast;

public class FrSkyServer extends Service implements OnInitListener {
	

    
	private static final String TAG="FrSkyServerService";
	private static final int NOTIFICATION_ID=56;
	
    private int MY_DATA_CHECK_CODE;
	
	private Long counter = 0L; 
	private NotificationManager nm;
	private Timer timer = new Timer();
	private final Calendar time = Calendar.getInstance();
	
	public static final int CMD_KILL_SERVICE	=	9999;
	public static final int CMD_IGNORE			=	 -1;
	public static final int CMD_START_SIM		=	 0;
	public static final int CMD_STOP_SIM		=	 1;
	public static final int CMD_START_SPEECH	=	 2;
	public static final int CMD_STOP_SPEECH		=	 3;
	
	private TextToSpeech mTts;
	private int _speakDelay;
	private Handler speakHandler;
    private Runnable runnableSpeaker;
	
	private final IBinder mBinder = new MyBinder();
	
	private WakeLock wl;
	private boolean _cyclicSpeechEnabled;
	private MyApp globals;
	
	public Simulator sim;
	

	public static final String MESSAGE_STARTED = "biz.onomato.frskydash.intent.action.SERVER_STARTED";
	
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		Log.i(TAG,"Something tries to bind to me");
		return mBinder;
		//return null;
	}
	
    private void showNotification() {
    	 CharSequence text = "FrSkyServer Started";
    	 Notification notification = new Notification(R.drawable.icon, text, System.currentTimeMillis());
    	 //notification.defaults |= Notification.FLAG_ONGOING_EVENT;
    	 //notification.flags = Notification.DEFAULT_LIGHTS;
    	 notification.ledOffMS = 500;
    	 notification.ledOnMS = 500;
    	 notification.ledARGB = 0xff00ff00;

    	 notification.flags |= Notification.FLAG_SHOW_LIGHTS;
    	 notification.flags |= Notification.FLAG_ONGOING_EVENT;
    	 notification.flags |= Notification.FLAG_NO_CLEAR;

    	 
    	 //notification.flags |= Notification.FLAG_FOREGROUND_SERVICE; 
    	 
    	 // The following intent makes sure that the application is "resumed" properly
    	 Intent notificationIntent = new Intent(this,Frskydash.class);
    	 notificationIntent.setAction(Intent.ACTION_MAIN);
         notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

    	 
    	 // http://developer.android.com/guide/topics/ui/notifiers/notifications.html
    	 PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
    			 notificationIntent, 0);
    	notification.setLatestEventInfo(this, "FrSkyDash",
    	      text, contentIntent);
    	startForeground(NOTIFICATION_ID,notification);
    	    }
	
	@Override
	public void onCreate()
	{
		Log.i(TAG,"onCreate");
		super.onCreate();
		
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		Toast.makeText(this,"Service created at " + time.getTime(), Toast.LENGTH_LONG).show();
		showNotification();		
		
		Log.i(TAG,"Broadcast that i've started");
		Intent i = new Intent();
		i.setAction(MESSAGE_STARTED);
		sendBroadcast(i);
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		 wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
		 getWakeLock();
		 
		 globals = ((MyApp)getApplicationContext());
		 
		 
		 //sim = new Simulator(this);
		 
		 _cyclicSpeechEnabled = false;
		 _speakDelay = 30000;
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
	}
	
	
	

	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		Log.i(TAG,"Receieved startCommand or intent ");
		handleIntent(intent);
		return START_STICKY;
	}
	
	
	public void getWakeLock()
	{
		if(!wl.isHeld())
		{
			Log.i(TAG,"Acquire wakelock");
			wl.acquire();
		}
		else
		{
			Log.i(TAG,"Wakelock already acquired");
		}
	}
	
	public void handleIntent(Intent intent)
	{
		int cmd = intent.getIntExtra("command",CMD_IGNORE);
		Log.i(TAG,"CMD: "+cmd);
		switch(cmd) {
			case CMD_START_SIM:
				Log.i(TAG,"Start Simulator");
				break;
			case CMD_STOP_SIM:
				Log.i(TAG,"Stop Simulator");
				break;
			case CMD_START_SPEECH:
				Log.i(TAG,"Start Speaker");
				break;
			case CMD_STOP_SPEECH:
				Log.i(TAG,"Stop Speaker");
				break;	
			case CMD_KILL_SERVICE:
				Log.i(TAG,"Killing myself");
				die();
				break;
			case CMD_IGNORE:
				//Log.i(TAG,"No command, skipping");
				break;
			default:
				Log.i(TAG,"Command "+cmd+" not implemented. Skipping");
				break;
			
		}
		
	}
	
	
	public void die()
	{
		Log.i(TAG,"Die, perform cleanup");

		stopSelf();
	}
	

	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		Log.i(TAG,"onDestroy");
		
		Log.i(TAG,"Releasing Wakelock");
		if(wl.isHeld())
		{
			wl.release();
		}
		stopCyclicSpeaker();
		mTts.shutdown();
		
		
		stopForeground(true);
	    Toast.makeText(this, "Service destroyed at " + time.getTime(), Toast.LENGTH_LONG).show();
	}
	
	public void onInit(int status) {
    	Log.i(TAG,"TTS initialized");
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
    		String myGreeting = "Application has enabled Text to Speech";
        	mTts.speak(myGreeting,TextToSpeech.QUEUE_FLUSH,null);
    	}
    	} else {
    	// Initialization failed.
    	Log.i(TAG,"Something wrong with TTS");
    	Log.e(TAG, "Could not initialize TextToSpeech.");
    	}
    }
	
	
	// *************************************************
	// Public methods
	public class MyBinder extends Binder {
		FrSkyServer getService() {
			return FrSkyServer.this;
		}
	}
	public int[] getCurrentFrame() {
		int[] t = new int[11];
		t[0] = 0x7e;
		t[1] = 0xfe;
		t[10] = 0xfe;
		return t;
	}
	
	public TextToSpeech createSpeaker()
	{
		Log.i(TAG,"Create Speaker");
		mTts = new TextToSpeech(this, this);
		return mTts;
	}
	
	public void saySomething(String myText)
	{
		Log.i(TAG,"Speak something");
		mTts.speak(myText, TextToSpeech.QUEUE_FLUSH, null);
	}
	
	public void startCyclicSpeaker()
	{
		// Stop it before starting it
		Log.i(TAG,"Start Cyclic Speaker");
		speakHandler.removeCallbacks(runnableSpeaker);
		speakHandler.post(runnableSpeaker);
		_cyclicSpeechEnabled = true;
	}
	public void stopCyclicSpeaker()
	{
		Log.i(TAG,"Stop Cyclic Speaker");
		speakHandler.removeCallbacks(runnableSpeaker);
		mTts.speak("", TextToSpeech.QUEUE_FLUSH, null);
		_cyclicSpeechEnabled = false;
	}

	public boolean getCyclicSpeechEnabled()
	{
		return _cyclicSpeechEnabled;
	}
	
	public void setCyclicSpeech(boolean state)
	{
		_cyclicSpeechEnabled = state;
		if(_cyclicSpeechEnabled)
		{
			startCyclicSpeaker();
		}
		else
		{
			stopCyclicSpeaker();
		}
	}
	
	public void simStart()
	{
		Log.i(TAG,"Sim Start");
	}
	
	public void simStop()
	{
		Log.i(TAG,"Sim Stop");
	}
	
	public void setSimStarted(boolean state)
	{
		if(state)
		{
			simStart();
			
		}
		else
		{
			simStop();
		}
	}
}
