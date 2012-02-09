package biz.onomato.frskydash;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Binder;
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
import biz.onomato.frskydash.hub.FrSkyHub;

/**
 * The FrSkyServer is receives the buffer from the
 * {@link BluetoothSerialService} and starts by parsing this buffer into
 * individual decoded {@link Frame} objects.
 * 
 */
public class FrSkyServer extends Service implements OnInitListener {
	    
	public static final String TAG="FrSkyServerService";
	public static boolean D = true;
	//private static final UUID SerialPortServiceClass_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final int NOTIFICATION_ID=56;
	private AudioManager _audiomanager;
	//private boolean _scoConnected = false;
	
	// Things for Bluetooth
	//private static final int REQUEST_ENABLE_BT = 2;
	private IntentFilter mIntentFilterBt;
	private boolean bluetoothEnabledAtStart;
	private boolean _connecting=false;
    private BluetoothAdapter mBluetoothAdapter = null;
	
    //private int MY_DATA_CHECK_CODE;
    private SharedPreferences _settings=null;
	//SharedPreferences settings;
	private SharedPreferences.Editor _editor;

	
	//private Long counter = 0L; 
	//private NotificationManager nm;
	//private Timer timer = new Timer();
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
    
    private boolean _watchdogEnabled = true;
	
	private TextToSpeech mTts;
	private int _speakDelay;
    
	private Handler fpsHandler, watchdogHandler, speakHandler;
    private Runnable runnableFps, runnableSpeaker, runnableWatchdog;
    
    // server Channels, add constants for all known source channels
//eso: refactor to ChannelMap
    public static final int CHANNEL_ID_NONE = -1;
    public static final int CHANNEL_ID_AD1 = -100;
    public static final int CHANNEL_ID_AD2 = -101;
    public static final int CHANNEL_ID_RSSIRX = -102;
    public static final int CHANNEL_ID_RSSITX = -103;
    
    
//eso: refactor to ChannelMap    
    private static TreeMap<Integer,Channel> _sourceChannelMap;
    
    public static FrSkyDatabase database;
    
    private boolean _dying=false;
	
	private final IBinder mBinder = new MyBinder();
	
	private WakeLock wl;
	private boolean _cyclicSpeechEnabled;
	//private MyApp globals;
	private static Context context;
	
	public Simulator sim;

	public boolean statusBt=false;
	public boolean statusTx=false;
	public boolean statusRx=false;
	
	//private HashMap<String,Channel> _serverChannels;
	
	//private int MAX_CHANNELS=4;

	private int _framecount=0;
	private int _framecountRx=0;
	private int _framecountTx=0;
	private boolean _btAutoEnable;
	private boolean _btAutoConnect;
	private int _minimumVolumeLevel;
	private boolean _autoSetVolume;

	
	private TreeMap<Integer,Alarm> _alarmMap;
	private boolean _recordingAlarms = false;

//	private int[] hRaw;
//	private double[] hVal;
//	private String[] hName;
//	private String[] hDescription;
//	private double[] hOffset;
//	private double[] hFactor;
//	private String[] hUnit;
//	private String[] hLongUnit;
	//private int channels=0;
	
//	private Channel[] objs; //TODO: Deprecate
	
	//private HashMap<String, String> _myAudibleStreamMap;
	
	//public Channel AD1,AD2,RSSIrx,RSSItx;
	
	//public Model currentModel;

	public static final String MESSAGE_STARTED = "biz.onomato.frskydash.intent.action.SERVER_STARTED";
	public static final String MESSAGE_SPEAKERCHANGE = "biz.onomato.frskydash.intent.action.SPEAKER_CHANGED";
	public static final String MESSAGE_BLUETOOTH_STATE_CHANGED = "biz.onomato.frskydash.intent.action.BLUETOOTH_STATE_CHANGED";
	
	public static final String MESSAGE_ALARM_RECORDING_COMPLETE = "biz.onomato.frskydash.intent.action.ALARM_RECORDING_COMPLETE";
	
	// hcpl: these are class members now since we have to collect the data over
	// several method executions since the bytes could be spread over several
	// telemetry 11 bytes frames
	
	/**
	 * the current user frame we are working on. This is used to pass data
	 * between incompletes frames.
	 */
	private List<Integer> frSkyFrame = new ArrayList<Integer>(Frame.SIZE_TELEMETRY_FRAME);
//	private static int[] frSkyFrame = new int[Frame.SIZE_TELEMETRY_FRAME];
//
//	/**
//	 * index of the current user frame. If set to -1 no user frame is under
//	 * construction.
//	 */
//	private static int currentFrSkyFrameIndex = -1;
//
//	/**
//	 * if on previous byte the XOR byte was found or not
//	 */
//	private static boolean frSkyXOR = false;
	
	@Override
	public void onCreate()
	{
		if(D)Log.i(TAG,"onCreate");
		super.onCreate();
		context = getApplicationContext();
		//_serverChannels = new HashMap<String,Channel>();
		
		_alarmMap = new TreeMap<Integer,Alarm>();
		_sourceChannelMap = new TreeMap<Integer,Channel>(Collections.reverseOrder());
		
		
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
		
		
		
		NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		Toast.makeText(this,"Service created at " + time.getTime(), Toast.LENGTH_LONG).show();
		
		if(D)Log.i(TAG,"Try to load settings");
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
		
		if(D)Log.i(TAG,"Previous ModelId was: "+_prevModelId);
	//	_currentModel = new Model(context);
		
		// DEBUG, List all channels for the model using new databaseadapter
		database = new FrSkyDatabase(getApplicationContext());
		_currentModel = database.getModel(_prevModelId);
		if(_currentModel==null)
		{
			if(D)Log.e(TAG,"No model exists, make a new one");
			_currentModel = new Model("Model 1");
			// Saving to get id
			database.saveModel(_currentModel);
			
			_currentModel.setFrSkyAlarms(initializeFrSkyAlarms());
			// Create Default model channels.
			_currentModel.initializeDefaultChannels();
			
			
			//_model.addChannel(c);
			
			//_currentModel.setId(0);
			database.saveModel(_currentModel);
		}
		
		if(_currentModel.getFrSkyAlarms().size()==0)
		{
			if(D)Log.e(TAG,"No alarms exists, setup with defaults");
			_currentModel.setFrSkyAlarms(initializeFrSkyAlarms());
			database.saveModel(_currentModel);
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
		
		if(D)Log.e(TAG,"The current model is: "+_currentModel.getName()+" and has id: "+_currentModel.getId());

		
//		ArrayList<Channel> tChannels = database.getChannelsForModel(_currentModel);
//		for(Channel c : tChannels)
//		{
//			Log.e(TAG,"\t"+c.getDescription());
//		}
		
		
		
		//initializeAlarms();
		
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
		
		
		if(D)Log.i(TAG,"Broadcast that i've started");
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
				
				
				if(D)Log.i(TAG,"Cyclic Speak stuff");
				if(statusRx)
				{
					for(Channel c : getCurrentModel().getChannels().values())
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
				if(_watchdogEnabled)
				{
					if((statusRx==false) && (statusBt==true))	send(Frame.InputRequestAll().toInts());
				}
				
				watchdogHandler.removeCallbacks(runnableWatchdog);
				watchdogHandler.postDelayed(this,500);
			}
		};
		watchdogHandler.postDelayed(runnableWatchdog,500);
		
		
		
	}
	
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		if(D)Log.i(TAG,"Something tries to bind to me");
		return mBinder;
		//return null;
	}
	

	public void setWatchdogEnabled(boolean state)
	{
		_watchdogEnabled = state;
	}
	public boolean getWatchdogEnabled()
	{
		return _watchdogEnabled;
	}
	
	public static Channel getSourceChannel(int id)
	{
		return _sourceChannelMap.get(id);
	}
	
	public static TreeMap<Integer,Channel> getSourceChannels()
	{
		return _sourceChannelMap;
	}
	public static Context getContext()
	{
		//Log.e(TAG,"Someone asked me for context!");
		return context;
	}
	
	
	public TreeMap<Integer,Alarm> initializeFrSkyAlarms()
	{
		TreeMap<Integer,Alarm> aMap = new TreeMap<Integer,Alarm>();
		Frame alarmFrame = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM1_RSSI, 
				Alarm.ALARMLEVEL_LOW, 
				45, 
				Alarm.LESSERTHAN);
		Alarm a = new Alarm(alarmFrame);
		a.setUnitChannel(_sourceChannelMap.get(CHANNEL_ID_RSSIRX));
		a.setModelId(_currentModel);
		aMap.put(a.getFrSkyFrameType(), a);
		
		
		
		
		alarmFrame = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM2_RSSI, 
				Alarm.ALARMLEVEL_MID, 
				42, 
				Alarm.LESSERTHAN);
		
		a = new Alarm(alarmFrame);
		a.setUnitChannel(_sourceChannelMap.get(CHANNEL_ID_RSSIRX));
		a.setModelId(_currentModel);
		aMap.put(a.getFrSkyFrameType(), a);
		
		alarmFrame = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM1_AD1, 
				Alarm.ALARMLEVEL_OFF, 
				200, 
				Alarm.LESSERTHAN);
		a = new Alarm(alarmFrame);
		a.setUnitChannel(_sourceChannelMap.get(CHANNEL_ID_AD1));
		a.setModelId(_currentModel);
		aMap.put(a.getFrSkyFrameType(), a);
		
		alarmFrame = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM2_AD1, 
				Alarm.ALARMLEVEL_OFF, 
				200, 
				Alarm.LESSERTHAN);
		a = new Alarm(alarmFrame);
		a.setUnitChannel(_sourceChannelMap.get(CHANNEL_ID_AD1));
		a.setModelId(_currentModel);
		aMap.put(a.getFrSkyFrameType(), a);
		
		alarmFrame = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM1_AD2, 
				Alarm.ALARMLEVEL_OFF, 
				200, 
				Alarm.LESSERTHAN);
		a = new Alarm(alarmFrame);
		a.setUnitChannel(_sourceChannelMap.get(CHANNEL_ID_AD2));
		a.setModelId(_currentModel);
		aMap.put(a.getFrSkyFrameType(), a);
		
		alarmFrame = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM2_AD2, 
				Alarm.ALARMLEVEL_OFF, 
				200, 
				Alarm.LESSERTHAN);
		a = new Alarm(alarmFrame);
		a.setUnitChannel(_sourceChannelMap.get(CHANNEL_ID_AD2));
		a.setModelId(_currentModel);
		aMap.put(a.getFrSkyFrameType(), a);
		return aMap;
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
		if(D)Log.i(TAG,"Set new interval to "+interval+" seconds");
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
		if(D)Log.i(TAG,"Receieved startCommand or intent ");
		handleIntent(intent);
		return START_STICKY;
	}
	
	
	public void getWakeLock()
	{
		if(!wl.isHeld())
		{
			if(D)Log.i(TAG,"Acquire wakelock");
			wl.acquire();
		}
		else
		{
			if(D)Log.i(TAG,"Wakelock already acquired");
		}
	}
	
	public void handleIntent(Intent intent)
	{
		int cmd = intent.getIntExtra("command",CMD_IGNORE);
		if(D)Log.i(TAG,"CMD: "+cmd);
		switch(cmd) {
			case CMD_START_SIM:
				if(D)Log.i(TAG,"Start Simulator");
				break;
			case CMD_STOP_SIM:
				if(D)Log.i(TAG,"Stop Simulator");
				break;
			case CMD_START_SPEECH:
				if(D)Log.i(TAG,"Start Speaker");
				
				break;
			case CMD_STOP_SPEECH:
				if(D)Log.i(TAG,"Stop Speaker");
				break;	
			case CMD_KILL_SERVICE:
				if(D)Log.i(TAG,"Killing myself");
				die();
				break;
			case CMD_IGNORE:
				//Log.i(TAG,"No command, skipping");
				break;
			default:
				if(D)Log.i(TAG,"Command "+cmd+" not implemented. Skipping");
				break;
			
		}
		
	}
	
	public void die()
	{
		if(D)Log.i(TAG,"Die, perform cleanup");

		stopSelf();
	}
	
	@Override
	public void onDestroy()
	{
		_dying=true;
		
		if(D)Log.i(TAG,"onDestroy");
		
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
    	if(D)Log.i(TAG,"Releasing Wakelock");
		if(wl.isHeld())
		{
			wl.release();
		}
		stopCyclicSpeaker();
		if(D)Log.i(TAG,"Shutdown mTts");
		
		try{
			mTts.shutdown();
		}
		catch (Exception e) {}
		
		if(D)Log.i(TAG,"Stop BT service if neccessary");
		if(mSerialService.getState()!=BluetoothSerialService.STATE_NONE)
		{
			try
			{
				mSerialService.stop();
			}
			catch (Exception e) {}
		}
		
		// Disable BT
		
		
		if(D)Log.i(TAG,"Stop FPS counter");
		fpsHandler.removeCallbacks(runnableFps);
		
		if(D)Log.i(TAG,"Reset channels");
		resetChannels();
		
		if(D)Log.i(TAG,"Stop Logger");
		try{
			logger.stop();
		}
		catch (Exception e)
		{
			
		}
		
		//stopCyclicSpeaker();
		
		if(D)Log.i(TAG,"Remove from foreground");
		try{
			stopForeground(true);
		}
		catch (Exception e)
		{
			if(D)Log.d(TAG,"Exeption during stopForeground");
		}
		
		try
		{
			super.onDestroy();
		}
		catch (Exception e)
		{
			if(D)Log.d(TAG,"Exeption during super.onDestroy");
		}
		try
		{
			Toast.makeText(this, "Service destroyed at " + time.getTime(), Toast.LENGTH_LONG).show();
		}
		catch (Exception e)
		{
			if(D)Log.d(TAG,"Exeption during last toast");
		}
	}
	
	public void onInit(int status) {
		if(D)Log.i(TAG,"TTS initialized");
    	// status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
    	if (status == TextToSpeech.SUCCESS) {
    	int result = mTts.setLanguage(Locale.US);
    	if (result == TextToSpeech.LANG_MISSING_DATA ||
    	result == TextToSpeech.LANG_NOT_SUPPORTED) {
    	// Lanuage data is missing or the language is not supported.
    		if(D)Log.e(TAG, "Language is not available.");
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
    		if(D)Log.i(TAG,"Something wrong with TTS");
    		if(D)Log.e(TAG, "Could not initialize TextToSpeech.");
    	}
    }
	
	
	public void recordAlarmsFromModule()
	{
		// empty the map, allowing others to monitor it for becoming full again
		_recordingAlarms = true;
		_alarmMap.clear();
		send(Frame.InputRequestAll());
	}
	
	public TreeMap<Integer,Alarm> getRecordedAlarmMap()
	{
		return _alarmMap;
	}
	
	private void setupChannels()
	{
		//Sets up the hardcoded channels (AD1,AD2,RSSIrx,RSSItx)
		Channel none =  new Channel("None", 0, 1, "", "");
		none.setId(CHANNEL_ID_NONE);
		
		none.setPrecision(0);
		none.setSilent(true);
		_sourceChannelMap.put(CHANNEL_ID_NONE, none);
		
		
		Channel ad1 =  new Channel("AD1", 0, 1, "", "");
		ad1.setId(CHANNEL_ID_AD1);
		ad1.setPrecision(0);
		ad1.setSilent(true);
		_sourceChannelMap.put(CHANNEL_ID_AD1, ad1);
		
		
		Channel ad2 =  new Channel("AD2", 0, 1, "", "");
		ad2.setId(CHANNEL_ID_AD2);
		ad2.setPrecision(0);
		ad2.setSilent(true);
		_sourceChannelMap.put(CHANNEL_ID_AD2, ad2);

		Channel rssirx =  new Channel("RSSIrx", 0, 1, "", "");
		rssirx.setId(CHANNEL_ID_RSSIRX);
		rssirx.setPrecision(0);
		rssirx.setMovingAverage(-1);
		rssirx.setLongUnit("dBm");
		rssirx.setShortUnit("dBm");
		rssirx.setSilent(true);
		_sourceChannelMap.put(CHANNEL_ID_RSSIRX, rssirx);
		
		Channel rssitx =  new Channel("RSSItx", 0, 1, "", "");
		rssitx.setId(CHANNEL_ID_RSSITX);
		rssitx.setPrecision(0);
		rssitx.setMovingAverage(-1);
		rssitx.setLongUnit("dBm");
		rssitx.setShortUnit("dBm");
		rssitx.setSilent(true);
		_sourceChannelMap.put(CHANNEL_ID_RSSITX, rssitx);
		
		
		
		
		
	}
	
	private void resetChannels()
	{
		for(Channel c : _sourceChannelMap.values())
		{
			c.setRaw(0);
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
    	//eso: TODO: what is reset vs channel.setRaw(0) (used in resetChannels) 
    	for(Channel c: _sourceChannelMap.values())
    	{
    		c.reset();
    	}
    	
    	
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
            	Intent bcI = new Intent();
        		bcI.setAction(MESSAGE_BLUETOOTH_STATE_CHANGED);
        		sendBroadcast(bcI);
        		
        		if(D)Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothSerialService.STATE_CONNECTED:
                	if(D)Log.d(TAG,"BT connected");
                	setConnecting(false);
                	statusBt = true;
                	
                	_manualBtDisconnect = false;
                	//send(Frame.InputRequestAll().toInts());
                	
                	if(getAutoSendAlarms())
        			{
        				for(Alarm a : _currentModel.getFrSkyAlarms().values())
        				{
        					send(a.toFrame());
        				}
        			}
                    
                    break;
                    
                case BluetoothSerialService.STATE_CONNECTING:
                	if(D)Log.d(TAG,"BT connecting");
                	setConnecting(true);
                	 //mTitle.setText(R.string.title_connecting);
                    break;
                    
                case BluetoothSerialService.STATE_LISTEN:
                	if(D)Log.d(TAG,"BT listening");
                case BluetoothSerialService.STATE_NONE:
                	if(D)Log.d(TAG,"BT state changed to NONE");
                	//Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                	setConnecting(false);
                	if((statusBt==true) && (!_dying) && (!_manualBtDisconnect)) wasDisconnected("Bt");	// Only do disconnect message if previously connected
                	statusBt = false;
                	// set all the channels to -1
                	
                	
                	logger.stop();
                }
                break;
            case MESSAGE_WRITE:
            	//Log.d(TAG,"BT writing");
                break;
                
            //handle receiving data from frsky 
            case MESSAGE_READ:
            	if(!_dying)
            	{
            		//hcpl updated to handle the new int array after byte per byte read update
	                byte[] readBuf = (byte[]) msg.obj;
//	                int[] i = new int[msg.arg1];
            		//int[] i = (int[])msg.obj;
            		handleByteBuffer(readBuf);
//	                
//	                for(int n=0;n<msg.arg1;n++)
//	                {
//	                	//Log.d(TAG,n+": "+readBuf[n]);
//	                	if(readBuf[n]<0)
//	                	{
//	                		i[n]=readBuf[n]+256;
//	                	}
//	                	else
//	                	{
//	                		i[n]=readBuf[n];
//	                	}
//	                }
//	                
//	                // NEEDS to be changed!!!
//	                if(i.length<20)
//	                {
	                	//Frame f = new Frame(i);
	                	//Log.i(TAG,f.toHuman());
	                	//parseFrame(f);
//	                }
	            	//Log.d(TAG,readBuf.toString()+":"+msg.arg1);
            	}
                break;
                
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                setBtLastConnectedToAddress(_device.getAddress());
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                if(D)Log.d(TAG,"BT connected to...");
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
  
    /**
     * for simulation and testing only
     * 
     * @param buffer
     * @param signed
     */
    public void handleStringBuffer(String buffer, String separator, boolean signed){
    	//construct an int array out of the string
    	List<Integer> list = new ArrayList<Integer>();
    	//parse string
    	for(String str : buffer.split(separator)){
    		try{
    			list.add(Integer.decode(str.trim()));
    		}catch(NumberFormatException nfe){
    			Log.e(TAG, "dropped unparseable byte: "+str);
    		}
    	}
    	//translate
    	int[] ints = new int[list.size()];
    	for(int i=0; i<list.size(); i++)
    		ints[i] = list.get(i);
    	//and send on
    	handleIntBuffer(ints, signed);
    }
    
    /**
     * for simulation and testing only
     * 
     * @param buffer
     * @param signed
     */
    public void handleIntBuffer(int[] buffer, boolean signed){
    	//rework that int buffer into a byte buffer
    	byte[] byteBuffer = new byte[buffer.length];
    	for(int i =0 ; i < buffer.length ; i++){
    		byteBuffer[i] = (byte)(signed?buffer[i]:buffer[i]-256);
    	}
    	//todo this requires a reset of the buffers state
		//shouldn't be needed since it will reset properly on it's own on reaching delimiters
    	//frSkyFrame = new int[Frame.SIZE_TELEMETRY_FRAME];
		//currentFrSkyFrameIndex = -1;
		//frSkyXOR = false;
		//pass on
    	handleByteBuffer(byteBuffer);
    }
    
	/**
	 * hcpl: This is where the bytes buffer from bluetooth connection (or any
	 * other type of connection like test data or wired) will be handled byte
	 * per byte.
	 * 
	 * @param buffer
	 *            the buffer with bytes from the inputstream. An array of
	 *            <code>byte</code>s is expected representing bytes with a value
	 *            of -127 to 128.
	 */
	public void handleByteBuffer(byte[] buffer) {
		// init current byte
		int b;
		// iterate all bytes in buffer
		for( int i=0 ; i< buffer.length ; i++){
			// use & 0xff to properly convert from byte to 0-255 int
			// value (java only knows signed bytes)
			b = buffer[i] & 0xff;
			// no decoding at this point, just parse the frames from this buffer
			// on start stop byte we need to pass alon the collected frame and 
			// clean up so we can start collecting another frame
			if( b==Frame.START_STOP_TELEMETRY_FRAME){
				// we already have content so we were working on a valid frame
				if( !frSkyFrame.isEmpty()){
					// complete this frame with the stop bit
					frSkyFrame.add(b);
					// pass along
					handleFrame(frSkyFrame);
					// clean up
					frSkyFrame.clear();
				} 
				// otherwise this is a start bit so just register
				else 
					frSkyFrame.add(b);
			} 
			// otherwise just add to the current frame were working on
			else {
				frSkyFrame.add(b);
			}

		}
    }
	
	/**
	 * Handle a single parsed frame. This frame is expected to be exactly 11 bytes long and in proper format.
	 * @param list
	 */
//	public void handleFrame(int[] frame){
	public void handleFrame(List<Integer> list){
		// first convert 
		int[] ints = new int[list.size()];
		//index
		int i=0;
		// iterate
		for(int li = 0 ; li< list.size(); li++){
			ints[i++] = list.get(li);
		}
		//then pass to ctor Frame
		Frame f = new Frame(ints);
		// TODO adapt for encoding and accepting all lengths
    	parseFrame(f);
	}
	
	public TextToSpeech createSpeaker()
	{
		if(D)Log.i(TAG,"Create Speaker");
		mTts = new TextToSpeech(this, this);
		return mTts;
	}
	
	public void saySomething(String myText)
	{
		
		
		if(D)Log.i(TAG,"Speak something");
		mTts.speak(myText, TextToSpeech.QUEUE_FLUSH, null);
		//mTts.speak(myText, TextToSpeech.QUEUE_FLUSH, myAudibleStreamMap);
	}
	
	public void startCyclicSpeaker()
	{
		// Stop it before starting it
		if(D)Log.i(TAG,"Start Cyclic Speaker");
		speakHandler.removeCallbacks(runnableSpeaker);
		speakHandler.post(runnableSpeaker);
		_cyclicSpeechEnabled = true;
		
		Intent i = new Intent();
		i.setAction(MESSAGE_SPEAKERCHANGE);
		sendBroadcast(i);
	}
	public void stopCyclicSpeaker()
	{
		if(D)Log.i(TAG,"Stop Cyclic Speaker");
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
		if(D)Log.i(TAG,"Setting Cyclic speech to: "+state);
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
		if(D)Log.i(TAG,"Sim Start");
		sim.start();
	}
	
	public void simStop()
	{
		if(D)Log.i(TAG,"Sim Stop");
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
	
	/**
	 * parse this frame
	 * 
	 * @param f
	 * @param inBound
	 * @return
	 */
	public boolean parseFrame(Frame f, boolean inBound)
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
				_sourceChannelMap.get(CHANNEL_ID_AD1).setRaw(f.ad1);
				_sourceChannelMap.get(CHANNEL_ID_AD2).setRaw(f.ad2);
				_sourceChannelMap.get(CHANNEL_ID_RSSIRX).setRaw(f.rssirx);
				_sourceChannelMap.get(CHANNEL_ID_RSSITX).setRaw(f.rssitx);
				
				
				if(inBound)	
				{
					_framecountRx++;
					//TODO: replace with models logged channels
					//logger.logCsv(_sourceChannels[CHANNEL_INDEX_AD1],_sourceChannels[CHANNEL_INDEX_AD2]);
					logger.logCsv();
				}
				break;
			case Frame.FRAMETYPE_FRSKY_ALARM:
				if(D)Log.d(TAG,"handle inbound FrSky alarm");
				if(_currentModel!=null)
				{
					// don't copy the entire alarm, as that would kill off sourcechannel
					//TODO: Compare to existing
					//TODO: Ask to load into the alarms
//					Alarm a = _currentModel.getFrSkyAlarms().get(aIn.getFrSkyFrameType());
//					aIn.setThreshold(aIn.getThreshold());
//					aIn.setGreaterThan(aIn.getGreaterThan());
//					aIn.setAlarmLevel(aIn.getAlarmLevel());
				}
				
				if(inBound)	
				{
					_framecountTx++;
					
					if(_recordingAlarms)
					{
						// store alarms for future use
						Alarm aIn = new Alarm(f);
						_alarmMap.put(aIn.getFrSkyFrameType(), aIn);
						if(_alarmMap.size()>=4)
						{
							if(D)Log.i(TAG,"recording completed");
							_recordingAlarms = false;
							//FIXME: send broadcast to allow GUI to update
							Intent i = new Intent();
							i.setAction(MESSAGE_ALARM_RECORDING_COMPLETE);
							sendBroadcast(i);
						}
					}
				}
				break;
			case Frame.FRAMETYPE_USER_DATA:
				// hcpl add handling user data frames!!
				if(D)Log.d(TAG,"Frametype User Data");
				FrSkyHub.getInstance().extractUserDataBytes(f);
				break;
			case Frame.FRAMETYPE_INPUT_REQUEST_ALL:
				//Log.d(TAG,"Frametype Request all alarms");
				break;
			default:
				if(D)Log.i(TAG,"Frametype currently not supported: "+f.frametype);
				if(D)Log.i(TAG,"Frame: "+f.toHuman());
				break;
		}
		return ok;
		
	}
	
	/**
	 * wrapper to parse frames that are inbound so they get logged
	 * 
	 * @param f
	 * @return
	 */
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
		if(D)Log.i(TAG,"Really delete all log files");
		// Make logger stop logging, and close files
		logger.stop();
		
		// get list of all ASC files
		File path = getExternalFilesDir(null);
		String[] files = path.list();
		for(int i=0;i<files.length;i++)
		{
			File f = new File(getExternalFilesDir(null), files[i]);
			if(D)Log.i(TAG,"Delete: "+f.getAbsolutePath());
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
	
	public void setAutoSendAlarms(boolean autoSend)
	{
		
		_editor.putBoolean("autoSendAlarms", autoSend);
		_editor.commit();
	}
	
	public boolean getAutoSendAlarms()
	{

		return _settings.getBoolean("autoSendAlarms", false);
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
		
		if(_currentModel.getFrSkyAlarms().size()==0)
		{
			_currentModel.setFrSkyAlarms(initializeFrSkyAlarms());
			database.saveModel(_currentModel);
		}
		else
		{
			// we already have alarms
			// send them if user wants
			if(getAutoSendAlarms())
			{
				for(Alarm a : _currentModel.getFrSkyAlarms().values())
				{
					send(a.toFrame());
				}
			}
		}
		
		//_currentModel.setFrSkyAlarms(database.getAlarmsForModel(_currentModel));
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
        	if(D)Log.d(TAG,"Received Broadcast: "+msg);
        	if(msg.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
        	{
	        	// does not work?
	    		int cmd = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,-1);
	    		if(D)Log.i(TAG,"CMD: "+cmd);
	    		switch(cmd) {
	    			case BluetoothAdapter.STATE_ON:
	    				if(D)Log.d(TAG,"Bluetooth state changed to ON");
	    				
	    				if(getBtAutoConnect()) 
	    		    	{
	    					if(D)Log.d(TAG,"Autoconnect requested");
	    					connect();
	    		    	}
	    				break;
	    			case BluetoothAdapter.STATE_OFF:
	    				if(D)Log.d(TAG,"Blueotooth state changed to OFF");
	    				break;
	    			default:
	    				if(D)Log.d(TAG,"No information about "+msg);
	    		
	    		}
        	}
        	else if(msg.equals(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED))
        	{
        		if(D)Log.d(TAG,"SCO STATE CHANGED!!!"+msg);
        		int scoState = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
        		switch(scoState) {
        			case AudioManager.SCO_AUDIO_STATE_CONNECTED:
        				if(D)Log.i(TAG,"SCO CONNECTED!!!!");
        				//_scoConnected = true;
        				break;
        			case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
        				if(D)Log.i(TAG,"SCO DIS-CONNECTED!!!!");
        				//_scoConnected = false;
        				break;
        			default:
        				if(D)Log.e(TAG,"Unhandled state");
        				//_scoConnected = false;
        				break;
        		}
        		
        	}

        	else
        	{
        		if(D)Log.e(TAG,"Unhandled intent: "+msg);
        		
        	}
        }
    };
    
    
    
    
    public BluetoothAdapter getBluetoothAdapter()
    {
    	if(D)Log.i(TAG,"Check for BT");
	    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	    if (mBluetoothAdapter == null) {
	        // Device does not support Bluetooth
	    	if(D)Log.i(TAG,"Device does not support Bluetooth");
	    	// Disable all BT related menu items
	    }
	    
	    // popup to enable BT if not enabled
	    if (mBluetoothAdapter != null)
	    {
	        if (!mBluetoothAdapter.isEnabled()) {
	        	bluetoothEnabledAtStart = false;
	        	if(D)Log.d(TAG,"BT NOT enabled at start");
	        	if(getBtAutoEnable())
	        	{
	        		mBluetoothAdapter.enable();
	        		Toast.makeText(this, "Bluetooth autoenabled", Toast.LENGTH_LONG).show();
	        	}
	        	else
	        	{
	        		if(D)Log.i(TAG,"Request user to enable bt");
	        		
	        	}
	        }
	        else
	        {
	        	bluetoothEnabledAtStart = true;
	        	if(D)Log.d(TAG,"BT enabled at start");

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

