package biz.onomato.frskydash;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.AlertDialog;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.widget.Toast;

public class FrSkyServer extends Service implements OnInitListener {
	    
	private static final String TAG="FrSkyServerService";
	private static final UUID SerialPortServiceClass_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final int NOTIFICATION_ID=56;
	private AudioManager _audiomanager;
	private boolean _scoConnected = false;
	
	// Things for Bluetooth
	private static final int REQUEST_ENABLE_BT = 2;
	private IntentFilter mIntentFilterBt;
	private boolean bluetoothEnabledAtStart;
	private boolean _connecting=false;
    private BluetoothAdapter mBluetoothAdapter = null;
	
    private int MY_DATA_CHECK_CODE;
    private SharedPreferences _settings=null;
	//SharedPreferences settings;
	private SharedPreferences.Editor _editor;

	
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
	
	
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    public static final String DEVICE_NAME = "device_name";
    private String mConnectedDeviceName = null;
    public static final String TOAST = "toast";
    private static BluetoothSerialService mSerialService = null;
    private BluetoothDevice _device = null;
    public boolean reconnectBt = true;
    private boolean _manualBtDisconnect = false;    
    
    // FPS
    public int fps,fpsRx,fpsTx=0;
    private MyStack fpsStack;
    private MyStack fpsRxStack;
	private MyStack fpsTxStack;
	private static final int FRAMES_FOR_FPS_CALC=2;
    
    
    
    private Logger logger;
    private Model _currentModel;
	
	private TextToSpeech mTts;
	private int _speakDelay;
    
	private Handler fpsHandler, watchdogHandler, speakHandler;
    private Runnable runnableFps, runnableSpeaker, runnableWatchdog;
    
    // server Channels, add constants for all known source channels
    public static final int CHANNEL_INDEX_AD1 = 0;
    public static final int CHANNEL_INDEX_AD2 = 1;
    public static final int CHANNEL_INDEX_RSSIRX = 2;
    public static final int CHANNEL_INDEX_RSSITX = 3;
    
    private Channel[] _sourceChannels = new Channel[4];
    
    public DB dbb;
    
    private boolean _dying=false;
	
	private final IBinder mBinder = new MyBinder();
	
	private WakeLock wl;
	private boolean _cyclicSpeechEnabled;
	//private MyApp globals;
	private Context context;
	
	public Simulator sim;

	public boolean statusBt=false;
	public boolean statusTx=false;
	public boolean statusRx=false;
	
	//private HashMap<String,Channel> _serverChannels;
	
	
	private int MAX_CHANNELS=4;

	private int _framecount=0;
	private int _framecountRx=0;
	private int _framecountTx=0;
	private boolean _btAutoEnable;
	private boolean _btAutoConnect;
	private int _minimumVolumeLevel;
	private boolean _autoSetVolume;

//	private int[] hRaw;
//	private double[] hVal;
//	private String[] hName;
//	private String[] hDescription;
//	private double[] hOffset;
//	private double[] hFactor;
//	private String[] hUnit;
//	private String[] hLongUnit;
	private int channels=0;
	
//	private Channel[] objs; //TODO: Deprecate
	
	
	private HashMap<String, String> _myAudibleStreamMap;
	

	
	
	//public Channel AD1,AD2,RSSIrx,RSSItx;
	
	//public Model currentModel;
	
	

	public static final String MESSAGE_STARTED = "biz.onomato.frskydash.intent.action.SERVER_STARTED";
	public static final String MESSAGE_SPEAKERCHANGE = "biz.onomato.frskydash.intent.action.SPEAKER_CHANGED";
	
	
	
	
	@Override
	public void onCreate()
	{
		Log.i(TAG,"onCreate");
		super.onCreate();
		context = getApplicationContext();
		//_serverChannels = new HashMap<String,Channel>();
		
		
		
		
		_audiomanager = 
        	    (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		
//		_myAudibleStreamMap = new HashMap();
//		_myAudibleStreamMap.put(TextToSpeech.Engine.KEY_PARAM_STREAM,
//		        String.valueOf(AudioManager.STREAM_VOICE_CALL));
//		
//		if(_audiomanager.isBluetoothScoAvailableOffCall())
//		{
//			_audiomanager.startBluetoothSco();
//		}
		
		
		
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		Toast.makeText(this,"Service created at " + time.getTime(), Toast.LENGTH_LONG).show();
		
		Log.i(TAG,"Try to load settings");
        _settings = context.getSharedPreferences("FrSkyDash",MODE_PRIVATE);
        _editor = _settings.edit();
        
		showNotification();		
		
		setupChannels();

		//AD1.loadFromConfig(_settings);
		//AD2.loadFromConfig(_settings);

		
		//String _prevModel = "FunCub 1";
		int _prevModelId;
		try
		{
			
			_prevModelId= _settings.getInt("prevModelId", -1);
		}
		catch(Exception e)
		{
			_prevModelId = -1;
		}
		
		Log.i(TAG,"Previous ModelId was: "+_prevModelId);
	//	_currentModel = new Model(context);
		
		// DEBUG, List all channels for the model using new databaseadapter
		dbb = new DB(getApplicationContext());
		_currentModel = dbb.getModel(_prevModelId);
		if(_currentModel==null)
		{
			Log.e(TAG,"No model exists, make a new one");
			_currentModel = new Model(context,"Model 1");
			//_currentModel.setId(0);
			dbb.saveModel(_currentModel);
		}
		
		
		
//		if(!_currentModel.loadFromDatabase(_prevModelId))
//		{
//			Log.w(TAG,"The previous model does not exist");
//			Log.w(TAG,"Try to get the first model");
//			if(!_currentModel.getFirstModel())
//			{
//				// no models exist, 
//				// Set defaults
//				_currentModel.saveToDatabase();
//				// and save it
//			}
//		}
		_prevModelId = _currentModel.getId();
		_editor.putInt("prevModelId", _prevModelId);
		_editor.commit();
		
		Log.e(TAG,"The current model is: "+_currentModel.getName()+" and has id: "+_currentModel.getId());

		
		ArrayList<Channel> tChannels = dbb.getChannelsForModel(_currentModel);
		for(Channel c : tChannels)
		{
			Log.e(TAG,"\t"+c.getDescription());
		}
		
		
		
		initializeAlarms();
		
		// Save this model incase it was new...
		
		

		logger = new Logger(getApplicationContext(),_currentModel,true,true,true);
		//logger.setCsvHeader(_sourceChannels[CHANNEL_INDEX_AD1],_sourceChannels[CHANNEL_INDEX_AD2]);
		logger.setCsvHeader();
		logger.setLogToRaw(getLogToRaw());
		logger.setLogToCsv(getLogToCsv());
		logger.setLogToHuman(getLogToHuman());

		
		
		mIntentFilterBt = new IntentFilter();
		mIntentFilterBt.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		mIntentFilterBt.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED);
		//mIntentFilterBt.addAction("android.bluetooth.headset.action.STATE_CHANGED");
		registerReceiver(mIntentReceiverBt, mIntentFilterBt); // Used to receive BT events
		
		
		Log.i(TAG,"Broadcast that i've started");
		Intent i = new Intent();
		i.setAction(MESSAGE_STARTED);
		sendBroadcast(i);
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		 wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
		 getWakeLock();
		 
		 
		 mSerialService = new BluetoothSerialService(this, mHandlerBT);
		 
		 sim = new Simulator(this);
		 
		 _cyclicSpeechEnabled = false;
		 _speakDelay = 30000;
		
		 
		 
        speakHandler = new Handler();
		runnableSpeaker = new Runnable() {
			//@Override
			public void run()
			{
			
				AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
				//Log.d(TAG,"SCO stuff");
				//Log.d(TAG,"isBluetoothScoOn:"+am.isBluetoothScoOn());
				
//				Log.d(TAG,"isBluetoothScoAvailableOffCall:"+am.isBluetoothScoAvailableOffCall());  // <-- CAUSES CRASH
				//Log.d(TAG,"getMode:"+am.getMode());
				//Log.d(TAG,"isSpeakerphoneOn:"+am.isSpeakerphoneOn());
				
				
				Log.i(TAG,"Cyclic Speak stuff");
				if(statusRx)
				{
					for(Channel c : getCurrentModel().getChannels())
					{
						if(!c.getSilent()) mTts.speak(c.toVoiceString(), TextToSpeech.QUEUE_ADD, null);
					}
//					for(int n=0;n<MAX_CHANNELS;n++)
//					{
//						if(!getChannelById(n).getSilent()) mTts.speak(getChannelById(n).toVoiceString(), TextToSpeech.QUEUE_ADD, null);
//					}
				}
				
				speakHandler.removeCallbacks(runnableSpeaker);
		    	speakHandler.postDelayed(this, _speakDelay);
			}
		};
		
		
		fpsStack = new MyStack(FRAMES_FOR_FPS_CALC); // try with 2 seconds..
		fpsRxStack = new MyStack(FRAMES_FOR_FPS_CALC); // try with 2 seconds..
		fpsTxStack = new MyStack(FRAMES_FOR_FPS_CALC); // try with 2 seconds..
		
		fpsHandler = new Handler();
		runnableFps = new Runnable () {
			//@Override
			public void run()
			{

				
				fpsStack.push(_framecount);
				fpsRxStack.push(_framecountRx);
				fpsTxStack.push(_framecountTx);
				
				//fps = _framecount;
				//fpsRx = _framecountRx;
				//fpsTx = _framecountTx;
				
				fps = (int) Math.floor(fpsStack.average());
				fpsRx = (int) Math.floor(fpsRxStack.average());
				fpsTx = (int) Math.floor(fpsTxStack.average());
				
				
				if(fpsRx>0)	// receiving frames from Rx, means Tx comms is up as well 
				{
					// check if we should restart the cyclic speaker
					if((statusRx==false) && (getCyclicSpeechEnabled()))
					{
							// Restart speaker if running
						startCyclicSpeaker();
					}
					statusRx = true;
					statusTx = true;
				}
				else		// not receiving frames from Rx
				{
					// make sure user knows if state changed from ok to not ok
					if(statusRx==true)
					{
						wasDisconnected("Rx");
					}
					statusRx = false;
					
					if(fpsTx>0) // need to check if Tx comms is up
					{
						statusTx = true;
					}
					else
					{
						statusTx = false;
					}
					
				}
				
				
				_framecount = 0;
				_framecountRx = 0;
				_framecountTx = 0;
				//Log.i(TAG,"FPS: "+fps);
				fpsHandler.removeCallbacks(runnableFps);
				fpsHandler.postDelayed(this,1000);
			}
		};
		fpsHandler.postDelayed(runnableFps,1000);
		
		
		watchdogHandler = new Handler();
		runnableWatchdog = new Runnable () {
			//@Override
			public void run()
			{
				// Send get all alarms frame to force frames from Tx
				// only do this if not receiving anything from Rx side
				if((statusRx==false) && (statusBt==true))	send(Frame.InputRequestAll().toInts());
				
				watchdogHandler.removeCallbacks(runnableWatchdog);
				watchdogHandler.postDelayed(this,500);
			}
		};
		watchdogHandler.postDelayed(runnableWatchdog,500);
		
		
		
	}
	
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		Log.i(TAG,"Something tries to bind to me");
		return mBinder;
		//return null;
	}
	

	public Channel getSourceChannel(int index)
	{
		return _sourceChannels[index];
	}
	
	public Channel[] getSourceChannels()
	{
		return _sourceChannels;
	}
	
	
    private void showNotification() {
    	 CharSequence text = "FrSkyServer Started";
    	 Notification notification = new Notification(R.drawable.ic_status, text, System.currentTimeMillis());
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
    	 //Intent notificationIntent = new Intent(this,Frskydash.class);
    	 Intent notificationIntent = new Intent(this,ActivityDashboard.class);
    	 notificationIntent.setAction(Intent.ACTION_MAIN);
         notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

    	 
    	 // http://developer.android.com/guide/topics/ui/notifiers/notifications.html
    	 PendingIntent contentIntent = PendingIntent.getActivity(this, 0,notificationIntent, 0);
    	notification.setLatestEventInfo(this, "FrSkyDash",text, contentIntent);
    	startForeground(NOTIFICATION_ID,notification);
    }
	
	
	
	/**
	 * Set time between reads
	 * @param interval seconds between reads
	 */
	public void setCyclicSpeechInterval(int interval)
	{
		Log.i(TAG,"Set new interval to "+interval+" seconds");
		_editor.putInt("cyclicSpeakerInterval",interval);
		_editor.commit();
		if(interval>0)
		{
			_speakDelay = interval*1000;
			if(getCyclicSpeechEnabled())
			{
				// Restart speaker if running
				startCyclicSpeaker();
			}
		}
	}
	public int getCyclicSpeechInterval()
	{
		return _settings.getInt("cyclicSpeakerInterval", 30);
	}
	
	public void send(byte[] out) {
    	mSerialService.write( out );
    }
	public void send(int[] out) {
    	mSerialService.write( out );
    }
	public void send(Frame f) {
		send(f.toInts());
	}
	
	public void reConnect()
	{
		//if(getConnectionState()==BluetoothSerialService.)
		mSerialService.connect(_device);
	}

	public void setConnecting(boolean connecting)
	{
		_connecting = connecting;
	}
	public boolean getConnecting()
	{
		return _connecting;
	}
	
	public void connect(BluetoothDevice device)
	{
		setConnecting(true);
		
		logger.stop();
		_device = device;
		mSerialService.connect(device);
	}
	
	 public void connect()	// connect to previous device
	 {
    	if(mBluetoothAdapter.isEnabled()) // only connect if adapter is enabled
        {
	       	if(getBtLastConnectedToAddress()!="")
	       	{
	       		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(getBtLastConnectedToAddress());
	            connect(device);
	       	}
	    }
    }
	    
	
	public void disconnect()
	{
		_manualBtDisconnect = true;
		mSerialService.stop();
	}
	
	public void reconnectBt()
	{
		mSerialService.stop();
		mSerialService.start();
	}
	
	public int getConnectionState() {
		return mSerialService.getState();
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
		_dying=true;
		
		Log.i(TAG,"onDestroy");
		
//		AudioManager audioManager = 
//        	    (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		
		_audiomanager.stopBluetoothSco();
		
		
		simStop();
		unregisterReceiver(mIntentReceiverBt);
		//sim.reset();
		
		// disable bluetooth if it was disabled upon start:
		
    	if(!bluetoothEnabledAtStart)	// bluetooth was not enabled at start
    	{
    		if(mBluetoothAdapter!=null) mBluetoothAdapter.disable();	// only do this if bluetooth feature exists
    	}
		Log.i(TAG,"Releasing Wakelock");
		if(wl.isHeld())
		{
			wl.release();
		}
		stopCyclicSpeaker();
		Log.i(TAG,"Shutdown mTts");
		
		try{
			mTts.shutdown();
		}
		catch (Exception e) {}
		
		Log.i(TAG,"Stop BT service if neccessary");
		if(mSerialService.getState()!=BluetoothSerialService.STATE_NONE)
		{
			try
			{
				mSerialService.stop();
			}
			catch (Exception e) {}
		}
		
		// Disable BT
		
		
		Log.i(TAG,"Stop FPS counter");
		fpsHandler.removeCallbacks(runnableFps);
		
		Log.i(TAG,"Reset channels");
		resetChannels();
		
		Log.i(TAG,"Stop Logger");
		try{
			logger.stop();
		}
		catch (Exception e)
		{
			
		}
		
		//stopCyclicSpeaker();
		
		Log.i(TAG,"Remove from foreground");
		try{
			stopForeground(true);
		}
		catch (Exception e)
		{
			Log.d(TAG,"Exeption during stopForeground");
		}
		
		try
		{
			super.onDestroy();
		}
		catch (Exception e)
		{
			Log.d(TAG,"Exeption during super.onDestroy");
		}
		try
		{
			Toast.makeText(this, "Service destroyed at " + time.getTime(), Toast.LENGTH_LONG).show();
		}
		catch (Exception e)
		{
			Log.d(TAG,"Exeption during last toast");
		}
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
        	
        	setCyclicSpeechEnabled(getCyclicSpeechEnabledAtStartup());
        	
    	}
    	} else {
    	// Initialization failed.
    	Log.i(TAG,"Something wrong with TTS");
    	Log.e(TAG, "Could not initialize TextToSpeech.");
    	}
    }
	
	
	private void initializeAlarms()
	{
		// Force alarm creation/initiation
				Frame alarmframe1 = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM1_RSSI, 
				Alarm.ALARMLEVEL_LOW, 
				45, 
				Alarm.LESSERTHAN);
				Frame alarmframe2 = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM2_RSSI, 
				Alarm.ALARMLEVEL_MID, 
				42, 
				Alarm.LESSERTHAN);
				parseFrame(alarmframe1,false);	// don't count in fps
				parseFrame(alarmframe2,false);	// don't count in fps
	}
	
	private void setupChannels()
	{
		//Sets up the hardcoded channels (AD1,AD2,RSSIrx,RSSItx)
		
		Channel ad1 =  new Channel(context, "AD1", 0, 1, "", "");
		ad1.setId(-100);
		ad1.setPrecision(0);
		ad1.setSilent(true);
		_sourceChannels[CHANNEL_INDEX_AD1] = ad1;
		
		
		Channel ad2 =  new Channel(context,"AD2", 0, 1, "", "");
		ad2.setId(-101);
		ad2.setPrecision(0);
		ad2.setSilent(true);
		_sourceChannels[CHANNEL_INDEX_AD2] = ad2;

		Channel rssirx =  new Channel(context, "RSSIrx", 0, 1, "", "");
		rssirx.setId(-102);
		rssirx.setPrecision(0);
		rssirx.setMovingAverage(-1);
		rssirx.setLongUnit("dBm");
		rssirx.setShortUnit("dBm");
		rssirx.setSilent(true);
		_sourceChannels[CHANNEL_INDEX_RSSIRX] = rssirx;
		
		Channel rssitx =  new Channel(context, "RSSItx", 0, 1, "", "");
		rssitx.setId(-103);
		rssitx.setPrecision(0);
		rssitx.setMovingAverage(-1);
		rssitx.setLongUnit("dBm");
		rssitx.setShortUnit("dBm");
		rssitx.setSilent(true);
		_sourceChannels[CHANNEL_INDEX_RSSITX] = rssitx;
		
		
		
		
		
	}
	
	private void resetChannels()
	{
		for(int n=0;n<_sourceChannels.length;n++)
		{
			_sourceChannels[n].setRaw(0);
			//getChannelById(n).setRaw(0);
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

	public void wasDisconnected(String source)
	{
    	_sourceChannels[CHANNEL_INDEX_AD1].reset();
    	_sourceChannels[CHANNEL_INDEX_AD2].reset();
    	_sourceChannels[CHANNEL_INDEX_RSSIRX].reset();
    	_sourceChannels[CHANNEL_INDEX_RSSITX].reset();
    	
    	
    	// speak warning
    	saySomething("Alarm! Alarm! Alarm! Connection Lost!");
    	Toast.makeText(getApplicationContext(), "Lost connection with "+source, Toast.LENGTH_SHORT).show();
    	// Get instance of Vibrator from current Context
    	try
    	{
    		Vibrator v = (Vibrator) getSystemService(this.VIBRATOR_SERVICE);
	
			// Start immediately
			// Vibrate for 200 milliseconds
			// Sleep for 500 milliseconds
			long[] pattern = { 0, 200, 500, 200, 500, 200, 500 };
			v.vibrate(pattern,-1);
    	}
    	catch (Exception e)
    	{
    	 
    	}
    	
    
    	

    	
	}
	
private final Handler mHandlerBT = new Handler() {
    	
        @Override
        public void handleMessage(Message msg) {        	
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothSerialService.STATE_CONNECTED:
                	Log.d(TAG,"BT connected");
                	setConnecting(false);
                	statusBt = true;
                	
                	_manualBtDisconnect = false;
                	send(Frame.InputRequestAll().toInts());
                    
                    break;
                    
                case BluetoothSerialService.STATE_CONNECTING:
                	Log.d(TAG,"BT connecting");
                	setConnecting(true);
                	 //mTitle.setText(R.string.title_connecting);
                    break;
                    
                case BluetoothSerialService.STATE_LISTEN:
                	Log.d(TAG,"BT listening");
                case BluetoothSerialService.STATE_NONE:
                	Log.d(TAG,"BT state changed to NONE");
                	//Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                	setConnecting(false);
                	if((statusBt==true) && (!_dying) && (!_manualBtDisconnect)) wasDisconnected("Bt");	// Only do disconnect message if previously connected
                	statusBt = false;
                	// set all the channels to -1
                	
                	
                	logger.stop();
                }
                break;
            case MESSAGE_WRITE:
            	Log.d(TAG,"BT writing");
                break;
                
            case MESSAGE_READ:
            	if(!_dying)
            	{
	                byte[] readBuf = (byte[]) msg.obj;              
	                int[] i = new int[msg.arg1];
	                
	                for(int n=0;n<msg.arg1;n++)
	                {
	                	//Log.d(TAG,n+": "+readBuf[n]);
	                	if(readBuf[n]<0)
	                	{
	                		i[n]=readBuf[n]+256;
	                	}
	                	else
	                	{
	                		i[n]=readBuf[n];
	                	}
	                }
	                
	                // NEEDS to be changed!!!
	                if(i.length<20)
	                {
	                	Frame f = new Frame(i);
	                	//Log.i(TAG,f.toHuman());
	                	parseFrame(f);
	                }
	            	//Log.d(TAG,readBuf.toString()+":"+msg.arg1);
            	}
                break;
                
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                setBtLastConnectedToAddress(_device.getAddress());
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                Log.d(TAG,"BT connected to...");
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };   
	
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
		//mTts.speak(myText, TextToSpeech.QUEUE_FLUSH, myAudibleStreamMap);
	}
	
	public void startCyclicSpeaker()
	{
		// Stop it before starting it
		Log.i(TAG,"Start Cyclic Speaker");
		speakHandler.removeCallbacks(runnableSpeaker);
		speakHandler.post(runnableSpeaker);
		_cyclicSpeechEnabled = true;
		
		Intent i = new Intent();
		i.setAction(MESSAGE_SPEAKERCHANGE);
		sendBroadcast(i);
	}
	public void stopCyclicSpeaker()
	{
		Log.i(TAG,"Stop Cyclic Speaker");
		try
		{
			speakHandler.removeCallbacks(runnableSpeaker);
			mTts.speak("", TextToSpeech.QUEUE_FLUSH, null);
		}
		catch (Exception e) {}
		_cyclicSpeechEnabled = false;
		Intent i = new Intent();
		i.setAction(MESSAGE_SPEAKERCHANGE);
		sendBroadcast(i);
	}

	
	// Related to startup default for cyclic speaker
	public boolean getCyclicSpeechEnabledAtStartup()
	{
		return _settings.getBoolean("cyclicSpeakerEnabledAtStartup", false);
	}
	
	public void setCyclicSpeechEnabledAtStartup(boolean state)
	{
		Log.i(TAG,"Setting Cyclic speech to: "+state);
		_editor.putBoolean("cyclicSpeakerEnabledAtStartup", state);
		_editor.commit();
		//_cyclicSpeechEnabled = state;
	}
	
	
	// Current state of cyclic speaker
	public boolean getCyclicSpeechEnabled()
	{
		return _cyclicSpeechEnabled;
	}
	public void setCyclicSpeechEnabled(boolean cyclicSpeakerEnabled)
	{
		_cyclicSpeechEnabled = cyclicSpeakerEnabled;
		if(cyclicSpeakerEnabled) 
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
		sim.start();
	}
	
	public void simStop()
	{
		Log.i(TAG,"Sim Stop");
		sim.reset();
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
	
	
	public boolean parseFrame(Frame f,boolean inBound)
	{
		//int [] frame = f.toInts(); 
		boolean ok=true;
		if(inBound) // only log inbound frames
		{
			logger.logFrame(f);
		}
		if(inBound)	_framecount++;
		switch(f.frametype)
		{
			// Analog values
			case Frame.FRAMETYPE_ANALOG:
				// get AD1, AD2 etc from frame
				_sourceChannels[CHANNEL_INDEX_AD1].setRaw(f.ad1);
				_sourceChannels[CHANNEL_INDEX_AD2].setRaw(f.ad2);
				_sourceChannels[CHANNEL_INDEX_RSSIRX].setRaw(f.rssirx);
				_sourceChannels[CHANNEL_INDEX_RSSITX].setRaw(f.rssitx);
				

				
				
				if(inBound)	
				{
					_framecountRx++;
					//TODO: replace with models logged channels
					//logger.logCsv(_sourceChannels[CHANNEL_INDEX_AD1],_sourceChannels[CHANNEL_INDEX_AD2]);
					logger.logCsv();
				}
				break;
			case Frame.FRAMETYPE_FRSKY_ALARM:
				Log.d(TAG,"handle inbound FrSky alarm");
				if(_currentModel!=null)
				{
					_currentModel.addAlarm(new Alarm(f));
				}
				
				if(inBound)	_framecountTx++;
//				switch(f.alarmChannel)
//				{
//				case Channel.CHANNELTYPE_AD1:
//					//_sourceChannels[CHANNEL_INDEX_AD1].setFrSkyAlarm(f.alarmNumber, f.alarmThreshold, f.alarmGreaterThan, f.alarmLevel);
//					getCurrentModel().setFrSkyAlarm(f.alarmNumber, f.alarmThreshold, f.alarmGreaterThan, f.alarmLevel);
//					break;
//				case Channel.CHANNELTYPE_AD2:
//					//_sourceChannels[CHANNEL_INDEX_AD2].setFrSkyAlarm(f.alarmNumber, f.alarmThreshold, f.alarmGreaterThan, f.alarmLevel);
//					getCurrentModel().setFrSkyAlarm(f.alarmNumber, f.alarmThreshold, f.alarmGreaterThan, f.alarmLevel);
//					break;
//				case Channel.CHANNELTYPE_RSSI:
//					//_sourceChannels[CHANNEL_INDEX_RSSITX].setFrSkyAlarm(f.alarmNumber, f.alarmThreshold, f.alarmGreaterThan, f.alarmLevel);
//					getCurrentModel().setFrSkyAlarm(f.alarmNumber, f.alarmThreshold, f.alarmGreaterThan, f.alarmLevel);
//					break;
//				default:
//					Log.i(TAG,"Unsupported FrSky alarm?");
//					Log.i(TAG,"Frame: "+f.toHuman());
//				}
				
				break;
			default:
				Log.i(TAG,"Frametype currently not supported");
				Log.i(TAG,"Frame: "+f.toHuman());
				break;
		}
		return ok;
		
	}
	public boolean parseFrame(Frame f)
	{
		return parseFrame(f,true);
	}
	
	public String getFps()
	{
		return Integer.toString(fpsRx);
	}

	public void deleteAllLogFiles()
	{
		Log.i(TAG,"Really delete all log files");
		// Make logger stop logging, and close files
		logger.stop();
		
		// get list of all ASC files
		File path = getExternalFilesDir(null);
		String[] files = path.list();
		for(int i=0;i<files.length;i++)
		{
			File f = new File(getExternalFilesDir(null), files[i]);
			Log.i(TAG,"Delete: "+f.getAbsolutePath());
			f.delete();
		}
		Toast.makeText(getApplicationContext(),"All logs file deleted", Toast.LENGTH_LONG).show();
	}
	 
	
	
	
	
	
	// Settings setters and getters
	///TODO: Have setters and getters work with settings store
	
	public void setBtLastConnectedToAddress(String lastConnectedToAddress)
	{
		_editor.putString("btLastConnectedToAddress", lastConnectedToAddress);
	    _editor.commit();
	}
	
	public String getBtLastConnectedToAddress()
	{
		return _settings.getString("btLastConnectedToAddress","");
	}
	
	public boolean getLogToRaw()
	{
		return _settings.getBoolean("logToRaw", false);
	}
	
	public boolean getLogToCsv()
	{
		return _settings.getBoolean("logToCsv", false);
	}
	
	public boolean getLogToHuman()
	{
		return _settings.getBoolean("logToHuman", false);
	}
	
	public void setLogToRaw(boolean logToRaw)
	{
		_editor.putBoolean("logToRaw", logToRaw);
		_editor.commit();
		logger.setLogToRaw(logToRaw);
	}
	
	public void setLogToHuman(boolean logToHuman)
	{
		_editor.putBoolean("logToHuman", logToHuman);
		_editor.commit();
		logger.setLogToHuman(logToHuman);
	}
	
	public void setLogToCsv(boolean logToCsv)
	{
		_editor.putBoolean("logToCsv", logToCsv);
		_editor.commit();
		logger.setLogToCsv(logToCsv);
	}
	
	public void setBtAutoEnable(boolean btAutoEnable)
	{
		_btAutoEnable = btAutoEnable;
		_editor.putBoolean("btAutoEnable", btAutoEnable);
		_editor.commit();
	}
	
	public boolean getBtAutoEnable()
	{
		return _settings.getBoolean("btAutoEnable",false);
	}
	
	public void setBtAutoConnect(boolean btAutoConnect)
	{
		_btAutoConnect = btAutoConnect;
		_editor.putBoolean("btAutoConnect", btAutoConnect);
		_editor.commit();
	}
	
	public boolean getBtAutoConnect()
	{
		return _settings.getBoolean("btAutoConnect",false);
	}
	
	public void setMinimumVolume(int minimumVolumePrc)
	{
		_minimumVolumeLevel=minimumVolumePrc;
		_editor.putInt("initialMinimumVolume", minimumVolumePrc);
		_editor.commit();
	}
	public int getMinimumVolume()
	{
		return _settings.getInt("initialMinimumVolume", 70);
	}
	public void setAutoSetVolume(boolean autoSetVolume)
	{
		_autoSetVolume = autoSetVolume;
		_editor.putBoolean("autoSetVolume", _autoSetVolume);
		_editor.commit();
	}
	public boolean getAutoSetVolume()
	{
		return _settings.getBoolean("autoSetVolume", false);
	}
	
	
	public Model getCurrentModel()
	{
		return _currentModel;
	}
	
	public void setCurrentModel(Model currentModel)
	{
		logger.setModel(currentModel);
		_currentModel = currentModel;
		//_prevModelId = _currentModel.getId();
		_editor.putInt("prevModelId", _currentModel.getId());
		_editor.commit();
		//logger.stop();		// SetModel will stop current Logger
		

	}
	
	public SharedPreferences getSettings()
	{
		return _settings;
	}
	
	
	public void createChannelConfigDatabase()
	{
		
	}
	
	// Can be used to detect broadcasts from Bluetooth
    // Remember to add the message to the intentfilter (mIntentFilterBt) above
    private BroadcastReceiver mIntentReceiverBt = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	String msg = intent.getAction();
        	Log.d(TAG,"Received Broadcast: "+msg);
        	if(msg.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
        	{
	        	// does not work?
	    		int cmd = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,-1);
	    		Log.i(TAG,"CMD: "+cmd);
	    		switch(cmd) {
	    			case BluetoothAdapter.STATE_ON:
	    				Log.d(TAG,"Bluetooth state changed to ON");
	    				
	    				if(getBtAutoConnect()) 
	    		    	{
	    					Log.d(TAG,"Autoconnect requested");
	    					connect();
	    		    	}
	    				break;
	    			case BluetoothAdapter.STATE_OFF:
	    				Log.d(TAG,"Blueotooth state changed to OFF");
	    				break;
	    			default:
	    				Log.d(TAG,"No information about "+msg);
	    		
	    		}
        	}
        	else if(msg.equals(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED))
        	{
        		Log.d(TAG,"SCO STATE CHANGED!!!"+msg);
        		int scoState = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
        		switch(scoState) {
        			case AudioManager.SCO_AUDIO_STATE_CONNECTED:
        				Log.i(TAG,"SCO CONNECTED!!!!");
        				//_scoConnected = true;
        				break;
        			case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
        				Log.i(TAG,"SCO DIS-CONNECTED!!!!");
        				//_scoConnected = false;
        				break;
        			default:
        				Log.e(TAG,"Unhandled state");
        				//_scoConnected = false;
        				break;
        		}
        		
        	}

        	else
        	{
        		Log.e(TAG,"Unhandled intent: "+msg);
        		
        	}
        }
    };
    
    
    
    
    public BluetoothAdapter getBluetoothAdapter()
    {
	    Log.i(TAG,"Check for BT");
	    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	    if (mBluetoothAdapter == null) {
	        // Device does not support Bluetooth
	    	Log.i(TAG,"Device does not support Bluetooth");
	    	// Disable all BT related menu items
	    }
	    
	    // popup to enable BT if not enabled
	    if (mBluetoothAdapter != null)
	    {
	        if (!mBluetoothAdapter.isEnabled()) {
	        	bluetoothEnabledAtStart = false;
	        	Log.d(TAG,"BT NOT enabled at start");
	        	if(getBtAutoEnable())
	        	{
	        		mBluetoothAdapter.enable();
	        		Toast.makeText(this, "Bluetooth autoenabled", Toast.LENGTH_LONG).show();
	        	}
	        	else
	        	{
	        		Log.i(TAG,"Request user to enable bt");
	        		
	        	}
	        }
	        else
	        {
	        	bluetoothEnabledAtStart = true;
	        	Log.d(TAG,"BT enabled at start");

		        //autoconnect here if autoconnect
	        	if(getBtAutoConnect()) 
		    	{
	        		connect();
		    	}
	        }
	    }
	    return mBluetoothAdapter;
    }
    
   
	
}

