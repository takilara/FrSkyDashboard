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
import biz.onomato.frskydash.activities.ActivityDashboard;
import biz.onomato.frskydash.hub.FrSkyHub;


/**
 * Main server service.
 * This service will get started by the first Activity launched. It will stay alive
 * even if the application is "minimized". It will only be closed by a press on the "Back" button while
 * on the Dashboard.
 * <br><br>
 * Serves as a store for {@link Model}s, {@link Channel}s and {@link Alarm}s
 * <br><br>
 * Receives bytebuffer from {@link BluetoothSerialService}, and parses this into individual {@link Frame}s that is then sent 
 * to the respective Channel, Alarm or Hub. The frame is also sent to the {@link Logger} for logging to file.
 * <br><br>
 * Activities should bind to this service using 
 * startService and bindService
 * <br><br>
 * Communication from Service to Activities: Send broadcasts<br>
 * FIXME: Cleanup Activity -> Service<br>
 * Communication from Activity to Service: Method call, startservice with intent, or broadcast
 * 
 * 
 * @author eso
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
    private static boolean _outGoingWatchdogFlag = false;
    private static long _lastOutGoingWatchdogTime;
	
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
		
	
		
		NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		Toast.makeText(this,"Service created at " + time.getTime(), Toast.LENGTH_LONG).show();
		
		if(D)Log.i(TAG,"Try to load settings");
        _settings = context.getSharedPreferences("FrSkyDash",MODE_PRIVATE);
        _editor = _settings.edit();
        
		showNotification();		
		
		setupChannels();


		
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
		
		
		_prevModelId = _currentModel.getId();
		_editor.putInt("prevModelId", _prevModelId);
		_editor.commit();
		
		if(D)Log.e(TAG,"The current model is: "+_currentModel.getName()+" and has id: "+_currentModel.getId());

		
	

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
		
		 
		
		 // Cyclic job to "speak out" the channel values
		 speakHandler = new Handler();
		 runnableSpeaker = new Runnable() {
			//@Override
			public void run()
			{
				if(D)Log.i(TAG,"Cyclic Speak stuff");
				if(statusRx)
				{
					for(Channel c : getCurrentModel().getChannels().values())
					{
						if(!c.getSilent()) mTts.speak(c.toVoiceString(), TextToSpeech.QUEUE_ADD, null);
					}
				}
				
				speakHandler.removeCallbacks(runnableSpeaker);
		    	speakHandler.postDelayed(this, _speakDelay);
		 	}
		 };
		

		 // Cyclic handler to calculate FPS, and set the various connection statuses
		 
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
		// Start the FPS counters
		fpsHandler.postDelayed(runnableFps,1000);
		
		
		// Cyclic job to send watchdog to the Tx module
		watchdogHandler = new Handler();
		runnableWatchdog = new Runnable () {
			//@Override
			public void run()
			{
				sendWatchdog();
				
				watchdogHandler.removeCallbacks(runnableWatchdog);
				watchdogHandler.postDelayed(this,500);
			}
		};
		watchdogHandler.postDelayed(runnableWatchdog,500);
		
		
		
	}
	
	
	/**
	 * eso: Send a "Request all alarms" command to the FrSky radio module. 
	 * The returns from this command can be used to calculate FPS and connection status 
	 */
	public void sendWatchdog()
	{
		// Send get all alarms frame to force frames from Tx
	
		if(_watchdogEnabled)
		{
			// check if we already sent one
			if(_outGoingWatchdogFlag)
			{
				// How long ago?
				if((System.currentTimeMillis()-_lastOutGoingWatchdogTime)>5000)
				{
					// More than 5 seconds ago, reset outgoing flag
					_outGoingWatchdogFlag = false;
				}
			}
			else // no outgoing watchdog
			{
				// only do this if not receiving anything from Rx side
				if((statusRx==false) && (statusBt==true))
				{
					send(Frame.InputRequestAll());
					_outGoingWatchdogFlag = true;
					_lastOutGoingWatchdogTime = System.currentTimeMillis();
				}
			}
		}
	}
	
	
	@Override
	public IBinder onBind(Intent arg0) {
		if(D)Log.i(TAG,"Something tries to bind to me");
		return mBinder;
		//return null;
	}
	
	
	
	// **************************************************************************************************************
	//                                   GETTERS AND SETTERS
	// **************************************************************************************************************
	
	
	/**
	 * 
	 * @param state Determines if the cyclic watchdogs should be sent or not
	 */
	public void setWatchdogEnabled(boolean state)
	{
		_watchdogEnabled = state;
	}
	
	/**
	 * 
	 * @return if the cyclic watchdog is enabled or not 
	 */
	public boolean getWatchdogEnabled()
	{
		return _watchdogEnabled;
	}
	
	/**
	 * 
	 * @return a ServerChannel corresponding to the given id
	 */
	public static Channel getSourceChannel(int id)
	{
		return _sourceChannelMap.get(id);
	}
	
	/**
	 * 
	 * @return all the ServerChannels in a TreeMap (key is the id of the Channel) 
	 */
	public static TreeMap<Integer,Channel> getSourceChannels()
	{
		return _sourceChannelMap;
	}
	
	/**
	 * 
	 * @return the application context 
	 */
	public static Context getContext()
	{
		//Log.e(TAG,"Someone asked me for context!");
		return context;
	}
	
	/**
	 * Used to create initial alarms for a model.<br>
	 * This consists of the following alarms:<br>
	 * <ul>
	 * <li>AD1 Alarm 1 and 2
	 * <li>AD2 Alarm 1 and 2
	 * <li>RSSI Alarm 1 and 2 <i>(Note, RSSI alarms are undocumented)</i>
	 * </ul>
	 * 
	 * FIXME: Get the proper default values<br>
	 * FIXME: consider if this should be moved to Model
	 */
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
	
	
	/**
	 * Set time between voice output.
	 * 
	 * @param interval time in seconds between reads
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

	/**
	 * 
	 * @return The time in seconds between voice output
	 */
	public int getCyclicSpeechInterval()
	{
		return _settings.getInt("cyclicSpeakerInterval", 30);
	}
	

	/**
	 * 
	 * @param set to true while the application is connection to BlueTooth
	 */
	private void setConnecting(boolean connecting)
	{
		_connecting = connecting;
	}
	
	/**
	 * 
	 * @return returns true if the application is trying to connect
	 */
	public boolean getConnecting()
	{
		return _connecting;
	}
	
	/**
	 * 
	 * @return TreeMap containing alarms recorded from the FrSky module
	 */
	public TreeMap<Integer,Alarm> getRecordedAlarmMap()
	{
		return _alarmMap;
	}
	
	
	/**
	 * 
	 * @return true if the cyclic speaker should be enabled at startup
	 */
	public boolean getCyclicSpeechEnabledAtStartup()
	{
		return _settings.getBoolean("cyclicSpeakerEnabledAtStartup", false);
	}
	
	/**
	 * 
	 * @param state set to true if you want Cyclic speaker to be enabled at startup
	 */
	public void setCyclicSpeechEnabledAtStartup(boolean state)
	{
		if(D)Log.i(TAG,"Setting Cyclic speech to: "+state);
		_editor.putBoolean("cyclicSpeakerEnabledAtStartup", state);
		_editor.commit();
		//_cyclicSpeechEnabled = state;
	}
	
	/**
	 * 
	 * @return true if cyclic speaker is enabled
	 */
	public boolean getCyclicSpeechEnabled()
	{
		return _cyclicSpeechEnabled;
	}
	
	/**
	 * 
	 * @param state set to true to enable cyclic speaker
	 * 
	 */
	public void setCyclicSpeechEnabled(boolean state)
	{
		_cyclicSpeechEnabled = state;
		if(state) 
		{
			startCyclicSpeaker();
		}
		else
		{
			stopCyclicSpeaker();
		}
	}
	
	/**
	 * 
	 * @return the current FPS as a string.
	 * 
	 * NOTE: Will return FPS from rx if rx communication is up, and FPS from tx otherwise
	 */
	public String getFps()
	{
		if(statusRx)
		{
			return Integer.toString(fpsRx);
		}
		else
		{
			return Integer.toString(fpsTx);
		}
	}
	
	/**
	 * 
	 * @param lastConnectedToAddress an address to store persistantly, used to attempt autoconnect
	 */
	private void setBtLastConnectedToAddress(String lastConnectedToAddress)
	{
		_editor.putString("btLastConnectedToAddress", lastConnectedToAddress);
	    _editor.commit();
	}
	
	/**
	 * 
	 * @return address of the previously connected Bluetooth device
	 */
	private String getBtLastConnectedToAddress()
	{
		return _settings.getString("btLastConnectedToAddress","");
	}
	
	/**
	 * 
	 * @return true if logging to binary/raw file is enabled
	 */
	public boolean getLogToRaw()
	{
		return _settings.getBoolean("logToRaw", false);
	}
	
	/**
	 * 
	 * @return true if logging to CSV file is enabled
	 */
	public boolean getLogToCsv()
	{
		return _settings.getBoolean("logToCsv", false);
	}
	
	/**
	 * 
	 * @return true if logging to human readable file is enabled
	 */
	public boolean getLogToHuman()
	{
		return _settings.getBoolean("logToHuman", false);
	}
	
	/**
	 * 
	 * @param state true to enable logging to binary file
	 */
	public void setLogToRaw(boolean state)
	{
		_editor.putBoolean("logToRaw", state);
		_editor.commit();
		logger.setLogToRaw(state);
	}

	/**
	 * 
	 * @param state true to enable logging to human readable file
	 */
	public void setLogToHuman(boolean logToHuman)
	{
		_editor.putBoolean("logToHuman", logToHuman);
		_editor.commit();
		logger.setLogToHuman(logToHuman);
	}
	
	/**
	 * 
	 * @param state true to enable logging to CSV file
	 */
	public void setLogToCsv(boolean logToCsv)
	{
		_editor.putBoolean("logToCsv", logToCsv);
		_editor.commit();
		logger.setLogToCsv(logToCsv);
	}
	
	/**
	 *
	 * @param state true to autoenable bluetooth at startup
	 */
	public void setBtAutoEnable(boolean state)
	{
		_btAutoEnable = state;
		_editor.putBoolean("btAutoEnable", state);
		_editor.commit();
	}
	
	/**
	 * 
	 * @return true if Bluetooth auto enable is set
	 */
	public boolean getBtAutoEnable()
	{
		return _settings.getBoolean("btAutoEnable",false);
	}
	
	/**
	 * 
	 * @param state true to attempt autoconnect at startup
	 */
	public void setBtAutoConnect(boolean state)
	{
		_btAutoConnect = state;
		_editor.putBoolean("btAutoConnect", state);
		_editor.commit();
	}
	
	/**
	 * 
	 * @return true if startup autoconnect is enabled
	 */
	public boolean getBtAutoConnect()
	{
		return _settings.getBoolean("btAutoConnect",false);
	}
	
	/**
	 * 
	 * @param minimumVolumePrc the minimum volume (percentage 0 - 100) the application should use for cyclic speaker
	 * @see #setAutoSetVolume(boolean)
	 */
	public void setMinimumVolume(int minimumVolumePrc)
	{
		//_minimumVolumeLevel=minimumVolumePrc;
		_editor.putInt("initialMinimumVolume", minimumVolumePrc);
		_editor.commit();
	}
	
	/**
	 * 
	 * @return the minimum volume (percentage 0 - 100) used for cyclic speaker
	 */
	public int getMinimumVolume()
	{
		return _settings.getInt("initialMinimumVolume", 70);
	}
	
	/**
	 * 
	 * @param state true to have the application autoset the media volume on startup
	 * <br><br>
	 * @see #setMinimumVolume(int)
	 */
	public void setAutoSetVolume(boolean state)
	{
		//_autoSetVolume = state;
		_editor.putBoolean("autoSetVolume", _autoSetVolume);
		_editor.commit();
	}
	
	/**
	 * 
	 * @return true if the application is set to autoset the volume at startup
	 */
	public boolean getAutoSetVolume()
	{
		return _settings.getBoolean("autoSetVolume", false);
	}
	
	/**
	 * 
	 * @param state set true to have application send a models alarms on model change
	 * @see #setCurrentModel(Model)
	 */
	public void setAutoSendAlarms(boolean state)
	{
		_editor.putBoolean("autoSendAlarms", state);
		_editor.commit();
	}
	
	/**
	 * 
	 * @return true if application is set to autosend alarms on model change
	 */
	public boolean getAutoSendAlarms()
	{
		return _settings.getBoolean("autoSendAlarms", false);
	}
	
	/**
	 * 
	 * @return the current Model
	 */
	public Model getCurrentModel()
	{
		return _currentModel;
	}
	
	/**
	 * 
	 * @param currentModel the model the application should be monitoring
	 */
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
	
	/**
	 * 
	 * @return the application settings
	 */
	public SharedPreferences getSettings()
	{
		return _settings;
	}

	/**
	 * 
	 * @return the state of the Bluetooth connection
	 */
	public int getConnectionState() {
		return mSerialService.getState();
	}
	
	// **************************************************************************************************************
	//                                   APPLICATION/SERVICE STUFF
	// **************************************************************************************************************
	
	/**
	 * Used to allow the Activities to attach to the service.
	 * Study the activities for how to use
	 *
	 */
	public class MyBinder extends Binder {
		public FrSkyServer getService() {
			return FrSkyServer.this;
		}
	}

	/** 
	 * Called when activities run startService
	 * @see #handleIntent
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if(D)Log.i(TAG,"Receieved startCommand or intent ");
		handleIntent(intent);
		return START_STICKY;
	}
	
	
	
	/**
	 * Used to show a notification icon in the notification field. 
	 * Necessary since we want the application to be able to run even if the user goes back to the home screen.
	 */
    private void showNotification() {
    	CharSequence text = "FrSkyServer Started";
    	Notification notification = new Notification(R.drawable.ic_status, text, System.currentTimeMillis());

    	notification.ledOffMS = 500;
    	notification.ledOnMS = 500;
    	notification.ledARGB = 0xff00ff00;

    	notification.flags |= Notification.FLAG_SHOW_LIGHTS;
    	notification.flags |= Notification.FLAG_ONGOING_EVENT;
    	notification.flags |= Notification.FLAG_NO_CLEAR;

    	Intent notificationIntent = new Intent(this,ActivityDashboard.class);
    	notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        
    	PendingIntent contentIntent = PendingIntent.getActivity(this, 0,notificationIntent, 0);
    	notification.setLatestEventInfo(this, "FrSkyDash",text, contentIntent);
    	startForeground(NOTIFICATION_ID,notification);
    }
	
    /**
     * Request wakelock
     */
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
	
    /**
     * Handler to handle incoming intents from activities
     * @param intent the intent to react on
     */
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

	/*
	 * Used to initiate shutdown of the application, will trigger onDestroy()
	 */
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
		zeroChannels();
		
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
	
	
	/**
	 * Called after TextToSpeech was requested
	 */
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
    
	/**
	 * Setup the server channels
	 */
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
	
	/**
	 * Set the value of all channels to 0
	 * FIXME: Purpose of this, only used in onDestroy
	 */
	private void zeroChannels()
	{
		for(Channel c : _sourceChannelMap.values())
		{
			c.setRaw(0);
		}
		
	}
	
	/**
	 * Compare incoming alarms to currentModels alarms
	 * <br>
	 * NOTE: Incomplete
	 */
	public void compareAlarms()
	{
		boolean equal = true;
		if(_currentModel!=null)
		{
			for(Alarm a: _alarmMap.values())
			{
				Log.w(TAG,"Checking "+a.getFrSkyFrameType());
				if(!_currentModel.getFrSkyAlarms().containsValue(a))
				{
					Log.w(TAG," Not equal!");
					equal = false;
					break;
				}
				Log.w(TAG," equal");
				// compare a to _currentModel.alarms.get(a.getFrameType)
			}
			
			if(equal)
			{
				Log.e(TAG,"Alarm sets are equal");
			}
			else
			{
				Log.e(TAG,"Alarm sets are not equal");
				// Take 1:
				// Popup with:
				// - Get new alarms
				// - Modify Frsky alarms
				// - Ignore
				
				// Take 2:
				// Compare to other models
				// If other model found
				//    Popup Change Model to <newmodel>?
				//    Yes - No
				//    If No: Popup, update alarms:
				//           On device
				//           On radio
				//           Ignore
				
				// Take 3:
				// Depending on settings
				
				// 1. send currentmodels alarms
				
				// 2. Launch popup, Alarms not equal
				// 2.1. Load alarm from FrSky
				// 2.2. Send model alarms to FrSky
				// 2.3. Ignore
				// 2.4. ....
				
			}
		}
		
	}
	
	

	/**
	 * Delete all the logfiles
	 */
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
	
	
		
    
	// **************************************************************************************************************
	//                                   BLUETOOTH STUFF
	// **************************************************************************************************************
    
	/**
	 * Attempts to reconnect to the bluetooth device we lost connection with
	 */
	public void reConnect()
	{
		//if(getConnectionState()==BluetoothSerialService.)
		mSerialService.connect(_device);
	}

	
	/**
	 * 
	 * @param device the Bluetooth device we want to connect to
	 */
	public void connect(BluetoothDevice device)
	{
		setConnecting(true);
		
		logger.stop();		// stop the logger (will force creation of new files)
		_device = device;
		mSerialService.connect(device);
	}
	
	/** 
	 * Connects to the stored Bluetooth device
	 */
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
	    
	/**
	 * Disconnects from the Bluetooth device
	 */
	public void disconnect()
	{
		_manualBtDisconnect = true;
		mSerialService.stop();
	}
	
	/**
	 * Forces reconnection of the bluetooth link
	 */
	public void reconnectBt()
	{
		mSerialService.stop();
		mSerialService.start();
	}
	
	/**
	 * 
	 * @return the device's default BluetoothAdapter
	 */
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
    
	// **************************************************************************************************************
	//                                   COMMUNICATION
	// **************************************************************************************************************
	    
	/**
	 * Transmits bytes to the Bluetooth serial receiver
	 * 
	 * @param out Array of the bytes to send
	 */
	public void send(byte[] out) {
    	mSerialService.write( out );
    }
	
	/**
	 * Transmits ints to the Bluetooth serial receiver
	 * 
	 * @param out Array of the ints to send
	 */
	public void send(int[] out) {
    	mSerialService.write( out );
    }
	
	/**
	 * Transmits a frame to the Bluetooth serial receiver
	 * 
	 * @param f the frame to send 
	 */	
	public void send(Frame f) {
		send(f.toInts());
	}
	
	
	
	/**
	 * Used to start recording alarms from the FrSky Module,
	 * To use the alarms, listen to the MESSAGE_ALARM_RECORDING_COMPLETE broadcast, then
	 * use <b>getRecordedAlarmMap()</b> to retrieve them. 	
	 */
	public void recordAlarmsFromModule()
	{
		// empty the map, allowing others to monitor it for becoming full again
		_recordingAlarms = true;
		_alarmMap.clear();
		
		// Only send request if Rx communication is up since we do automatic requests for alarms otherwise
		if((statusRx==true) && (statusBt==true))
		{
			send(Frame.InputRequestAll());
		}
	}
	
	/**
	 * Gets called whenever bluetooth connection is unintentionally dropped
	 * @param source the name of the device we lost connection with
	 */
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
	

  
    
	// **************************************************************************************************************
	//                                   BYTE STREAM ANALYSIS
	// **************************************************************************************************************
    
    
    
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
	
	
	// **************************************************************************************************************
	//                                   FRAME HANDLING
	// **************************************************************************************************************
	
	/**
	 * Handle a single parsed frame. This frame is expected to be exactly 11 bytes long and in proper format.
	 * @param list
	 * 
	 * NOTE: eso: we could add Frame(List<Integer>) ctor
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
	
	/**
	 * Determines what to do with a single frame
	 * 
	 * @param f the frame to parse
	 * @param inBound set to false to mask the frame from FPS calculations
	 * @return always true
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
					_outGoingWatchdogFlag=false;
					
					if(_recordingAlarms)
					{
						// store alarms for future use
						Alarm aIn = new Alarm(f);
						Log.w(TAG,"Adding alarm to the recording buffer, alarm id: "+aIn.getFrSkyFrameType());
						_alarmMap.put(aIn.getFrSkyFrameType(), aIn);
						if(_alarmMap.size()>=4)
						{
							if(D)Log.w(TAG,"recording completed");
							_recordingAlarms = false;
							//FIXME: send broadcast to allow GUI to update
							Intent i = new Intent();
							i.setAction(MESSAGE_ALARM_RECORDING_COMPLETE);
							sendBroadcast(i);
							
							compareAlarms();
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
	 * @see #parseFrame(Frame, boolean)
	 */
	public boolean parseFrame(Frame f)
	{
		return parseFrame(f,true);
	}
	
	
	// **************************************************************************************************************
	//                                   TEXT TO SPEECH STUFF
	// **************************************************************************************************************

	/**
	 * Creates a TextToSpeech object
	 * @return the TextToSpeech object we will use
	 */
	public TextToSpeech createSpeaker()
	{
		if(D)Log.i(TAG,"Create Speaker");
		mTts = new TextToSpeech(this, this);
		return mTts;
	}
	
	/**
	 * Speaks something using default values
	 * @param myText the text to speak
	 */
	public void saySomething(String myText)
	{
		if(D)Log.i(TAG,"Speak something");
		mTts.speak(myText, TextToSpeech.QUEUE_FLUSH, null);
		//mTts.speak(myText, TextToSpeech.QUEUE_FLUSH, myAudibleStreamMap);
	}
	
	/**
	 * Starts the cyclic speaker threads
	 */
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
	
	/**
	 * Stops the cyclic speaker thread
	 */
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

	// **************************************************************************************************************
	//                                   SIMULATOR STUFF
	// **************************************************************************************************************
	
	
	/**
	 * Starts the cyclic simulator
	 * @see Simulator
	 */
	public void simStart()
	{
		if(D)Log.i(TAG,"Sim Start");
		sim.start();
	}
	
	/**
	 * Stops the cyclic simulator
	 * @see Simulator
	 */
	public void simStop()
	{
		if(D)Log.i(TAG,"Sim Stop");
		sim.reset();
	}
	
	/**
	 * 
	 * @param state true to enable the Cyclic simulator
	 */
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
	

	
	
	
	// **************************************************************************************************************
	//                                   HANDLERS AND RECEIVERS
	// **************************************************************************************************************
	
	

	/**
	 * Used to detect broadcasts from Bluetooth.
	 * Remember to add the message to the intentfilter (mIntentFilterBt) above
	 */
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

    /**
     * Acts on the Bluetooth events
     */
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
    
    
}

