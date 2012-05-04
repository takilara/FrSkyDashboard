package biz.onomato.frskydash;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import android.app.Notification;
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
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;
import biz.onomato.frskydash.activities.ActivityDashboard;
import biz.onomato.frskydash.activities.ActivityHubData;
import biz.onomato.frskydash.db.FrSkyDatabase;
import biz.onomato.frskydash.domain.Alarm;
import biz.onomato.frskydash.domain.Channel;
import biz.onomato.frskydash.domain.Frame;
import biz.onomato.frskydash.domain.Model;
import biz.onomato.frskydash.hub.FrSkyHub;
import biz.onomato.frskydash.hub.SensorTypes;
import biz.onomato.frskydash.sim.FileSimulatorThread;
import biz.onomato.frskydash.sim.Simulator;
import biz.onomato.frskydash.util.Logger;

/**
 * Main server service.
 * This service will get started by the first Activity launched. It will stay alive
 * even if the application is "minimized". It will only be closed by a press on the "Back" button while
 * on the Dashboard.
 * <br><br>
 * Serves as a store for {@link Model}s, {@link Channel}s and {@link Alarm}s
 * <br><br>
 * Receives bytebuffer from {@link BluetoothSerialService}, and parses this into individual {@link Frame}s that is then sent 
 * to the respective Channel, Alarm or Hub. The frame is also sent to the {@link DataLogger} for logging to file.
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
	 
	/**
	 * tag used for logging
	 */
	public static final String TAG="FrSkyServerService";
	
	//private static final UUID SerialPortServiceClass_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final int NOTIFICATION_ID=56;
	private AudioManager _audiomanager;
	//private boolean _scoConnected = false;
	
	/**
	 * Things for Bluetooth
	 */
	//private static final int REQUEST_ENABLE_BT = 2;
	private IntentFilter mIntentFilterBt;
	private boolean bluetoothEnabledAtStart;
	private boolean _connecting=false;
    private BluetoothAdapter mBluetoothAdapter = null;
	
    /**
     * user preferences
     */
    private SharedPreferences _settings=null;

    /**
     * editor for preferences
     */
    private SharedPreferences.Editor _editor;

    /**
     * current time
     */
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
    private boolean _hubEnabled = false;
    private boolean _filePlaybackEnabled = false;
    
    private boolean _compareAfterRecord =false;
    private boolean _autoSwitch = false;
    
    /**
     * FPS
     */
    public int fps,fpsRx,fpsTx=0;
    public static int badFrames = 0;
    private MyStack fpsStack;
    private MyStack fpsRxStack;
	private MyStack fpsTxStack;
	private static final int FRAMES_FOR_FPS_CALC=2;
    
    private DataLogger logger;

	/**
	 * current {@link Model} selected by the user. This Model has
	 * {@link Channel} instances that are registered to listen for updates
	 */
    private Model _currentModel=null;
    
	/**
	 * A collection of {@link Model} instances available
	 */
    public static TreeMap<Integer,Model> modelMap;
    
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

	/**
	 * <p>
	 * A collection of {@link Channel} objects as sources. These Channels are
	 * available technical Channels used as source. This means it matches a
	 * certain type of sensor, analog value or rssi value. These are not
	 * registered for listening. Only the Channels the user creates are
	 * registered for listening.
	 * </p>
	 * 
	 * <p>
	 * The Channels created by a user are stored on the {@link Model} instance
	 * and hold a certain configuration for these source channels.
	 * </p>
	 * 
	 * <p>
	 * <b><u>Example:</u></b> If a user for instance wants the Analog port 1 to
	 * be displayed as a voltage divided by 3 (1 cell for a 3 cell lipo being
	 * connected) he can configure this Channel with the divider and then it
	 * will be registered listening and displaying the adapted value. The same
	 * source channel (for Analog port 1) can then also be configured as an
	 * actual channel on the model without divider showing the complete lipo
	 * pack voltage.
	 * </p>
	 */
	private static TreeMap<Integer, Channel> _sourceChannelMap;
   
	/**
	 * backend
	 */
    private static FrSkyDatabase database;
    
    private boolean _dying=false;
	
	private final IBinder mBinder = new MyBinder();
	
	private WakeLock wl;
	private boolean _cyclicSpeechEnabled;
	//private MyApp globals;
	private static Context context;
	
	/**
	 * The simulator can be used to create simulate Analog values in a loop for
	 * testing.
	 */
	private Simulator sim;
	
	/**
	 * a thread for cycling the raw file contents providing to server with a
	 * fixed interval. This is static so we can check state on resume. Moved to
	 * this location so it can be closed on destroy of the service
	 */
	private static FileSimulatorThread fileSim = null;

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

	/**
	 * map of alarms as found on the module. These can be different from the
	 * alarms set on the Model {@link Model#frSkyAlarms}.
	 */
	private TreeMap<Integer,Alarm> _alarmMap;
	private boolean _recordingAlarms = false;
	private int _recordingModelId = -1;

	public static final String MESSAGE_STARTED = "biz.onomato.frskydash.intent.action.SERVER_STARTED";
	public static final String MESSAGE_SPEAKERCHANGE = "biz.onomato.frskydash.intent.action.SPEAKER_CHANGED";
	public static final String MESSAGE_BLUETOOTH_STATE_CHANGED = "biz.onomato.frskydash.intent.action.BLUETOOTH_STATE_CHANGED";
	
	public static final String MESSAGE_ALARM_RECORDING_COMPLETE = "biz.onomato.frskydash.intent.action.ALARM_RECORDING_COMPLETE";
	public static final String MESSAGE_ALARM_MISMATCH = "biz.onomato.frskydash.intent.action.ALARM_MISMATCH";
	
	public LocalBroadcastManager broadcastManager;

	/**
	 * hcpl: intent used to broadcast hub data info
	 * 
	 * TODO update visibility
	 */
	Intent broadcastHubDataIntent;

	/**
	 * a unique identifier for broadcast intents
	 */
	public static final String BROADCAST_ACTION_HUB_DATA = "biz.onomato.frskydash.intent.action.BROADCAST_HUB_DATA";

	/**
	 * Broadcast event to trigger a channel reset
	 */
	public static final String BROADCAST_CHANNEL_COMMAND_RESET_CHANNELS = "biz.onomato.frskydash.intent.action.BROADCAST_CHANNEL_COMMAND_RESET_CHANNELS";
	// hcpl: these are class members now since we have to collect the data over
	// several method executions since the bytes could be spread over several
	// telemetry 11 bytes frames
	
	/**
	 * the current user frame we are working on. This is used to pass data
	 * between incomplete frames.
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
		Logger.i(TAG,"onCreate");
		super.onCreate();
		context = getApplicationContext();
		broadcastManager = LocalBroadcastManager.getInstance(context);
		//_serverChannels = new HashMap<String,Channel>();
		
		_alarmMap = new TreeMap<Integer,Alarm>();
		_sourceChannelMap = new TreeMap<Integer,Channel>(Collections.reverseOrder());
		
		_audiomanager = 
        	    (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		
		//hcpl: commented since no longer in use
		//NotificationManager nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		Toast.makeText(this,"Service created at " + time.getTime(), Toast.LENGTH_LONG).show();
		
		Logger.i(TAG,"Try to load settings");
        _settings = context.getSharedPreferences("FrSkyDash",MODE_PRIVATE);
        _editor = _settings.edit();
        
		showNotification();		
		
		setupFixedChannels();
		
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
		
		Logger.i(TAG,"Previous ModelId was: "+_prevModelId);
		//	_currentModel = new Model(context);
		
		// DEBUG, List all channels for the model using new databaseadapter
		
		modelMap = new TreeMap<Integer,Model>();
		database = new FrSkyDatabase(getApplicationContext());
		
		for(Model m: database.getModels())
		{
			modelMap.put(m.getId(), m);
		}
		
		Model cm = modelMap.get(_prevModelId);
		
		//_currentModel = database.getModel(_prevModelId);
		
		if(cm==null)
		{
			Logger.e(TAG,"No model exists, make a new one");
			cm = new Model("Model 1");
			// Saving to get id
			database.saveModel(cm);
			
			cm.setFrSkyAlarms(initializeFrSkyAlarms());
			// Create Default model channels.
			cm.initializeDefaultChannels();
			
			//_model.addChannel(c);
			
			//_currentModel.setId(0);
			database.saveModel(cm);
			modelMap.put(cm.getId(),cm);
		}
		
		if(cm.getFrSkyAlarms().size()==0)
		{
			Logger.e(TAG,"No alarms exists, setup with defaults");
			cm.setFrSkyAlarms(initializeFrSkyAlarms());
			
			database.saveModel(cm);
		}
		
		_prevModelId = cm.getId();
		_editor.putInt("prevModelId", _prevModelId);
		_editor.commit();
		
		Logger.d(TAG,"The current model is: "+cm.getName()+" and has id: "+cm.getId());
		Logger.d(TAG,"Activating the model");
		setCurrentModel(cm);
		
		logger = new DataLogger(getApplicationContext(),_currentModel,true,true,true);
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
		
		Logger.i(TAG,"Broadcast that i've started");
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
				Logger.i(TAG,"Cyclic Speak stuff");
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
				
				//TODO this might be optimized, would need proper testing though
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
		watchdogHandler.postDelayed(runnableWatchdog, 500);

		// register intent for listening to broadcasts
		broadcastHubDataIntent = new Intent(BROADCAST_ACTION_HUB_DATA);

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
					send(Frame.InputRequestADAlarms());
					_outGoingWatchdogFlag = true;
					_lastOutGoingWatchdogTime = System.currentTimeMillis();
				}
			}
		}
	}
	
	
	@Override
	public IBinder onBind(Intent arg0) {
		Logger.i(TAG,"Something tries to bind to me");
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
	 * FIXME: Get the proper default values => hcpl: I believe the default values are 72<br>
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
		//a.setModelId(_currentModel);
		aMap.put(a.getFrSkyFrameType(), a);
		
		alarmFrame = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM2_RSSI, 
				Alarm.ALARMLEVEL_MID, 
				42, 
				Alarm.LESSERTHAN);
		
		a = new Alarm(alarmFrame);
		a.setUnitChannel(_sourceChannelMap.get(CHANNEL_ID_RSSIRX));
		//a.setModelId(_currentModel);
		aMap.put(a.getFrSkyFrameType(), a);
		
		alarmFrame = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM1_AD1, 
				Alarm.ALARMLEVEL_OFF, 
				200, 
				Alarm.LESSERTHAN);
		a = new Alarm(alarmFrame);
		a.setUnitChannel(_sourceChannelMap.get(CHANNEL_ID_AD1));
		//a.setModelId(_currentModel);
		aMap.put(a.getFrSkyFrameType(), a);
		
		alarmFrame = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM2_AD1, 
				Alarm.ALARMLEVEL_OFF, 
				200, 
				Alarm.LESSERTHAN);
		a = new Alarm(alarmFrame);
		a.setUnitChannel(_sourceChannelMap.get(CHANNEL_ID_AD1));
		//a.setModelId(_currentModel);
		aMap.put(a.getFrSkyFrameType(), a);
		
		alarmFrame = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM1_AD2, 
				Alarm.ALARMLEVEL_OFF, 
				200, 
				Alarm.LESSERTHAN);
		a = new Alarm(alarmFrame);
		a.setUnitChannel(_sourceChannelMap.get(CHANNEL_ID_AD2));
		//a.setModelId(_currentModel);
		aMap.put(a.getFrSkyFrameType(), a);
		
		alarmFrame = Frame.AlarmFrame(
				Frame.FRAMETYPE_ALARM2_AD2, 
				Alarm.ALARMLEVEL_OFF, 
				200, 
				Alarm.LESSERTHAN);
		a = new Alarm(alarmFrame);
		a.setUnitChannel(_sourceChannelMap.get(CHANNEL_ID_AD2));
		//a.setModelId(_currentModel);
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
		Logger.i(TAG,"Set new interval to "+interval+" seconds");
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
		Logger.i(TAG,"Setting Cyclic speech to: "+state);
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
//		return Integer.toString(fpsRx);
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
	 * Enable/Disable hub support
	 * 
	 * @param state true to enable hub support
	 */
	public void setHubEnabled(boolean state)
	{
		_editor.putBoolean("hubEnabled", state);
		_editor.commit();
		_hubEnabled = state;
	}
	
	/**
	 * Get current hub enabled state
	 */
	public boolean getHubEnabled()
	{
		_hubEnabled =_settings.getBoolean("hubEnabled",false); 
		return _hubEnabled;
	}
	
	/**
	 * Enable/Disable simulator playback features
	 * @param state true to enable
	 */
	public void setFilePlaybackEnabled(boolean state)
	{
		_editor.putBoolean("filePlaybackEnabled", state);
		_editor.commit();
		_filePlaybackEnabled = state;
	}
	
	public boolean getFilePlaybackEnabled()
	{
		_filePlaybackEnabled = _settings.getBoolean("filePlaybackEnabled",false);
		return _filePlaybackEnabled;
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
	 * @param modelId the id of the model the application should be monitoring
	 */
	public void setCurrentModel(int modelId)
	{
		setCurrentModel(modelMap.get(modelId));
	}
	/**
	 * 
	 * @param currentModel the model the application should be monitoring
	 */
	public void setCurrentModel(Model currentModel)
	{
		
		// reset old channels 
		//FIXME destroy?
		if(_currentModel!=null)
		{
			Logger.i(TAG,"Changing Models from "+_currentModel.getName()+" to "+currentModel.getName());
			
			_currentModel.unregisterListeners();
		}
		else
		{
			Logger.i(TAG,"Changing Models from NULL to "+currentModel.getName());
		}
		//_currentModel = null;
		
		badFrames=0;
		
		if(logger!=null)
		{
			logger.setModel(currentModel);
		}
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
				sendAlarms(_currentModel);
//				for(Alarm a : _currentModel.getFrSkyAlarms().values())
//				{
//					send(a.toFrame());
//				}
			}
		}
		_currentModel.registerListeners();
		//_currentModel.setFrSkyAlarms(database.getAlarmsForModel(_currentModel));
		//logger.stop();		// SetModel will stop current Logger
		Toast.makeText(this, _currentModel.getName() + " set as the active model", Toast.LENGTH_LONG).show();

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
		Logger.i(TAG,"Receieved startCommand or intent ");
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
			Logger.i(TAG,"Acquire wakelock");
			wl.acquire();
		}
		else
		{
			Logger.i(TAG,"Wakelock already acquired");
		}
	}
	
    /**
     * Handler to handle incoming intents from activities
     * @param intent the intent to react on
     */
	public void handleIntent(Intent intent)
	{
		int cmd = intent.getIntExtra("command",CMD_IGNORE);
		Logger.i(TAG,"CMD: "+cmd);
		switch(cmd) {
			case CMD_START_SIM:
				Logger.i(TAG,"Start Simulator");
				break;
			case CMD_STOP_SIM:
				Logger.i(TAG,"Stop Simulator");
				break;
			case CMD_START_SPEECH:
				Logger.i(TAG,"Start Speaker");
				
				break;
			case CMD_STOP_SPEECH:
				Logger.i(TAG,"Stop Speaker");
				break;	
			case CMD_KILL_SERVICE:
				Logger.i(TAG,"Killing myself");
				die();
				break;
			case CMD_IGNORE:
				//Log.i(TAG,"No command, skipping");
				break;
			default:
				Logger.i(TAG,"Command "+cmd+" not implemented. Skipping");
				break;
		}
	}

	/*
	 * Used to initiate shutdown of the application, will trigger onDestroy()
	 */
	public void die()
	{
		Logger.i(TAG,"Die, perform cleanup");

		stopSelf();
	}
	
	@Override
	public void onDestroy()
	{
		_dying=true;
		
		Logger.i(TAG,"onDestroy");
		
		_audiomanager.stopBluetoothSco();
		
		
		simStop();
		unregisterReceiver(mIntentReceiverBt);
		//sim.reset();
		
		//stop filesim thread also
		if( fileSim != null )
			fileSim.stopThread();
		
		// disable bluetooth if it was disabled upon start:
		
    	if(!bluetoothEnabledAtStart)	// bluetooth was not enabled at start
    	{
    		if(mBluetoothAdapter!=null) mBluetoothAdapter.disable();	// only do this if bluetooth feature exists
    	}
    	Logger.i(TAG,"Releasing Wakelock");
		if(wl.isHeld())
		{
			wl.release();
		}
		stopCyclicSpeaker();
		Logger.i(TAG,"Shutdown mTts");
		
		try{
			mTts.shutdown();
		}
		catch (Exception e) {}
		
		Logger.i(TAG,"Stop BT service if neccessary");
		if(mSerialService.getState()!=BluetoothSerialService.STATE_NONE)
		{
			try
			{
				mSerialService.stop();
			}
			catch (Exception e) {}
		}
		
		// Disable BT
		
		
		Logger.i(TAG,"Stop FPS counter");
		fpsHandler.removeCallbacks(runnableFps);
		
		Logger.i(TAG,"Reset channels");
		destroyChannels();
		
		Logger.i(TAG,"Stop Logger");
		try{
			logger.stop();
		}
		catch (Exception e)
		{
			
		}
		
		//stopCyclicSpeaker();
		
		Logger.i(TAG,"Remove from foreground");
		try{
			stopForeground(true);
		}
		catch (Exception e)
		{
			Logger.d(TAG,"Exeption during stopForeground");
		}
		
		try
		{
			super.onDestroy();
		}
		catch (Exception e)
		{
			Logger.d(TAG,"Exeption during super.onDestroy");
		}
		try
		{
			Toast.makeText(this, "Service destroyed at " + time.getTime(), Toast.LENGTH_LONG).show();
		}
		catch (Exception e)
		{
			Logger.d(TAG,"Exeption during last toast");
		}
	}
	
	
	/**
	 * Called after TextToSpeech was requested
	 */
	public void onInit(int status) {
		Logger.i(TAG,"TTS initialized");
    	// status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
    	if (status == TextToSpeech.SUCCESS) {
    	int result = mTts.setLanguage(Locale.US);
    	if (result == TextToSpeech.LANG_MISSING_DATA ||
    	result == TextToSpeech.LANG_NOT_SUPPORTED) {
    	// Lanuage data is missing or the language is not supported.
    		Logger.e(TAG, "Language is not available.");
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
    		Logger.i(TAG,"Something wrong with TTS");
    		Logger.e(TAG, "Could not initialize TextToSpeech.");
    	}
    }
    
	/**
	 * Setup the server fixed channels
	 */
	private void setupFixedChannels()
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
		ad1.registerListenerForServerCommands();
		_sourceChannelMap.put(CHANNEL_ID_AD1, ad1);
		
		
		Channel ad2 =  new Channel("AD2", 0, 1, "", "");
		ad2.setId(CHANNEL_ID_AD2);
		ad2.setPrecision(0);
		ad2.setSilent(true);
		ad2.registerListenerForServerCommands();
		_sourceChannelMap.put(CHANNEL_ID_AD2, ad2);

		Channel rssirx =  new Channel("RSSIrx", 0, 1, "", "");
		rssirx.setId(CHANNEL_ID_RSSIRX);
		rssirx.setPrecision(0);
		rssirx.setMovingAverage(-1);
		rssirx.setLongUnit("dBm");
		rssirx.setShortUnit("dBm");
		rssirx.setSilent(true);
		rssirx.registerListenerForServerCommands();
		_sourceChannelMap.put(CHANNEL_ID_RSSIRX, rssirx);
		
		Channel rssitx =  new Channel("RSSItx", 0, 1, "", "");
		rssitx.setId(CHANNEL_ID_RSSITX);
		rssitx.setPrecision(0);
		rssitx.setMovingAverage(-1);
		rssitx.setLongUnit("dBm");
		rssitx.setShortUnit("dBm");
		rssitx.setSilent(true);
		rssitx.registerListenerForServerCommands();
		_sourceChannelMap.put(CHANNEL_ID_RSSITX, rssitx);
	}
	
	/**
	 * Destroy all channels
	 */
	private void destroyChannels()
	{
		// loop all models
			// loop all channels
				// channel.close()
		for(Model m : modelMap.values())
		{
			for(Channel c : m.getChannels().values())
			{
				c.close();
			}
		}
		
		for(Channel c : _sourceChannelMap.values())
		{
			//c.setRaw(0);
			c.close();
		}
	}

	/**
	 * Compares the recorded {@link Alarm}s to the alarm set of a {@link Model}.
	 * If no model is given this method will return false. If a model is given
	 * this method will iterate all alarms on that model and return false on the
	 * first mismatch.
	 * 
	 * TODO: as is this method returns true if no alarms are set on the model or
	 * (and this is correct) if all alarms are the same. Wouldn't it be better
	 * to return false if no alarms were set?
	 * 
	 * @param model
	 *            the model to compare to
	 * @return true if the alarms match
	 */
	public boolean alarmsSameAsModel(Model model)
	{
		// makes no sense if no model is given
		if(model==null)
			return false;
		//otherwise we can check the alarms set
		//iterate all alarms on the current model
		for (Alarm a : _alarmMap.values()) {
			// Log.w(TAG,"Checking "+a.getFrSkyFrameType());
			//as soon as we spot a difference
			if (!model.getFrSkyAlarms().containsValue(a)) {
				// Log.w(TAG," Not equal!");
				// mark this and break the loop
				return false;
			}
			// Log.w(TAG," equal");
			// compare a to _currentModel.alarms.get(a.getFrameType)
		}
		// hcpl: again why is default set to true?  
		return true;
	}
	
	/**
	 * Compare incoming alarms to currentModels alarms
	 * <br>
	 * NOTE: Incomplete
	 */
	public void compareAlarms()
	{
		// hcpl: isn't it better to start from not being equal? Otherwise on
		// error you might end up with equal while check hasn't passed = false
		// positive
		boolean equal = true;
		// we can only check if the current model is set
		if(_currentModel!=null){
			//return;
		//at this point the model available so we can compare the alarms
			equal = alarmsSameAsModel(_currentModel);
			if(equal)
			{
				Logger.e(TAG,"Alarm sets are equal");
			}
			else
			{
				Logger.e(TAG,"Alarm sets are not equal, see if i can find a model that is equal");
				boolean found = false;
				for(Model m: modelMap.values())
				{
					if(m!=_currentModel)	// no point checking currentModel again
					{
						if(alarmsSameAsModel(m))
						{
							found = true;
							Logger.w(TAG,"Alarms match model "+m.getName());
							// _autoSwitch should come from settings
							if(_autoSwitch)
							{
								//setCurrentModel(m);
								Logger.e(TAG,"Auto Switch model");
							}
							else
							{
								Logger.e(TAG,"Show popup allow switch of model");
								Intent i = new Intent(MESSAGE_ALARM_MISMATCH);
								i.putExtra("modelId", m.getId());
								sendBroadcast(i);
								
							}
							break;
						}
						else
						{
							//Log.w(TAG,"Alarms does not match model "+m.getName());
						}
					}
				}
				if(!found)
				{
					Logger.e(TAG,"Show popup no switch option");
					Intent i = new Intent(MESSAGE_ALARM_MISMATCH);
					i.putExtra("modelId", -1);
					sendBroadcast(i);
				}
			}
		}
		_compareAfterRecord = false;	
	}
	
	

	/**
	 * Delete all the logfiles
	 */
	public void deleteAllLogFiles()
	{
		Logger.i(TAG,"Really delete all log files");
		// Make logger stop logging, and close files
		logger.stop();
		
		// get list of all ASC files
		File path = getExternalFilesDir(null);
		String[] files = path.list();
		for(int i=0;i<files.length;i++)
		{
			File f = new File(getExternalFilesDir(null), files[i]);
			Logger.i(TAG,"Delete: "+f.getAbsolutePath());
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
    	Logger.i(TAG,"Check for BT");
	    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	    if (mBluetoothAdapter == null) {
	        // Device does not support Bluetooth
	    	Logger.i(TAG,"Device does not support Bluetooth");
	    	// Disable all BT related menu items
	    }
	    
	    // popup to enable BT if not enabled
	    if (mBluetoothAdapter != null)
	    {
	        if (!mBluetoothAdapter.isEnabled()) {
	        	bluetoothEnabledAtStart = false;
	        	Logger.d(TAG,"BT NOT enabled at start");
	        	if(getBtAutoEnable())
	        	{
	        		mBluetoothAdapter.enable();
	        		Toast.makeText(this, "Bluetooth autoenabled", Toast.LENGTH_LONG).show();
	        	}
	        	else
	        	{
	        		Logger.i(TAG,"Request user to enable bt");
	        		
	        	}
	        }
	        else
	        {
	        	bluetoothEnabledAtStart = true;
	        	Logger.d(TAG,"BT enabled at start");

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
		//Log.w(TAG,"Sending: "+f.toHuman());
		send(f.toInts());
	}
	
	public void recordAlarmsFromModule()
	{
		recordAlarmsFromModule(-1);
	}
	
	/**
	 * Used to start recording alarms from the FrSky Module,
	 * To use the alarms, listen to the MESSAGE_ALARM_RECORDING_COMPLETE broadcast, then
	 * use <b>getRecordedAlarmMap()</b> to retrieve them. 	
	 * @param modelId id of the model you want the recorded alarms to be stored on
	 */
	public void recordAlarmsFromModule(int modelId)
	{
		// empty the map, allowing others to monitor it for becoming full again
		_recordingAlarms = true;
		_recordingModelId = modelId;
		_alarmMap.clear();
		
		// Only send request for RSSI alarms if Rx communication is up since we do automatic requests for RSSI alarms otherwise
		if((statusRx==true) && (statusBt==true))
		{
			//send(Frame.InputRequestRSSIAlarms());
			send(Frame.InputRequestADAlarms());
		}
		//send(Frame.InputRequestADAlarms());
	}
	
	/**
	 * Gets called whenever bluetooth connection is unintentionally dropped
	 * @param source the name of the device we lost connection with
	 */
	public void wasDisconnected(String source)
	{
    	///TODO: eso: reset any hub channels as well
		//TODO: eso: this only resets source channels, should reset ALL channels
//		for (Channel c : _sourceChannelMap.values()) {
//			c.reset();
//		}
		
		Intent i = new Intent();
		i.setAction(BROADCAST_CHANNEL_COMMAND_RESET_CHANNELS);
		Logger.i(TAG, "Sending Reset Broadcast");
		sendBroadcast(i);
		
		

		// speak warning, only when not manually disconnected
		if (!_manualBtDisconnect) {
			saySomething("Alarm! Alarm! Alarm! Connection Lost!");
			Toast.makeText(getApplicationContext(),
					"Lost connection with " + source, Toast.LENGTH_SHORT)
					.show();
			// Get instance of Vibrator from current Context
			try {
				Vibrator v = (Vibrator) getSystemService(this.VIBRATOR_SERVICE);
				// Start immediately
				// Vibrate for 200 milliseconds
				// Sleep for 500 milliseconds
				long[] pattern = { 0, 200, 500, 200, 500, 200, 500 };
				v.vibrate(pattern, -1);
			} catch (Exception e) {
				Logger.e(TAG,
						"failure on vibrating for connection lost warning", e);
			}
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
    			Logger.e(TAG, "dropped unparseable byte: "+str);
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
		
//		if(ints.length>11)
//		{
//			Log.d(TAG,"Incoming list has strange length");
//			Log.d(TAG,"\tList: "+Frame.frameToHuman(ints));
//		}
		
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
			_framecount++;
		}
		//Log.w(TAG,f.toHuman());
		switch(f.frametype)
		{
			case Frame.FRAMETYPE_CORRUPT:
				Logger.w(TAG,"Frame most likely corrupt, discarded: "+f.toHuman());
				badFrames++;
				break;
			case Frame.FRAMETYPE_UNDEFINED:
				Logger.w(TAG,"Frame currently not supported, discarded: "+f.toHuman());
				badFrames++;
				break;
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
				//Logger.d(TAG,"handle inbound FrSky alarm");
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
						Logger.w(TAG,"Adding alarm to the recording buffer, alarm id: "+aIn.getFrSkyFrameType());
						_alarmMap.put(aIn.getFrSkyFrameType(), aIn);
						if(_alarmMap.size()>=4)
						{
							Logger.w(TAG,"recording completed");
							_recordingAlarms = false;
							// Update the alarms for the model
							if(_recordingModelId!=-1)
							{
								modelMap.get(_recordingModelId).setFrSkyAlarms(_alarmMap);
								saveModel(modelMap.get(_recordingModelId));
							}
							

							Intent i = new Intent();
							i.setAction(MESSAGE_ALARM_RECORDING_COMPLETE);
							sendBroadcast(i);
							
							if(_compareAfterRecord)
							{
								compareAlarms();
							}
						}
					}
				}
				break;
			case Frame.FRAMETYPE_USER_DATA:
				if(inBound)	
				{
					_framecountRx++;
					// hcpl add handling user data frames!!
					Logger.d(TAG,"Frametype User Data");
					// Use menu item Debug to enable hub support
					if(_hubEnabled)
					{
						FrSkyHub.getInstance().extractUserDataBytes(this, f);
					}
				}
				break;
			case Frame.FRAMETYPE_INPUT_REQUEST_ALARMS_AD:
				//Log.d(TAG,"Frametype Request all alarms");
				break;
			case Frame.FRAMETYPE_INPUT_REQUEST_ALARMS_RSSI:
				//Log.d(TAG,"Frametype Request all alarms");
				break;
			default:
				Logger.i(TAG,"Frametype currently not supported: "+f.frametype);
				Logger.i(TAG,"Frame: "+f.toHuman());
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
		Logger.i(TAG,"Create Speaker");
		mTts = new TextToSpeech(this, this);
		return mTts;
	}
	
	/**
	 * Speaks something using default values
	 * @param myText the text to speak
	 */
	public void saySomething(String myText)
	{
		Logger.i(TAG,"Speak something");
		mTts.speak(myText, TextToSpeech.QUEUE_FLUSH, null);
		//mTts.speak(myText, TextToSpeech.QUEUE_FLUSH, myAudibleStreamMap);
	}
	
	/**
	 * Starts the cyclic speaker threads
	 */
	public void startCyclicSpeaker()
	{
		// Stop it before starting it
		Logger.i(TAG,"Start Cyclic Speaker");
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
		Logger.i(TAG,"Stop Cyclic Speaker");
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
	 * 
	 * @see Simulator
	 */
	public void simStart() {
		Logger.i(TAG, "Sim Start");
		sim.start();
	}

	/**
	 * Stops the cyclic simulator
	 * 
	 * @see Simulator
	 */
	public void simStop() {
		Logger.i(TAG, "Sim Stop");
		sim.reset();
	}

	/**
	 * 
	 * @param state
	 *            true to enable the Cyclic simulator
	 */
	public void setSimStarted(boolean state) {
		if (state) {
			simStart();
		} else {
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
        	Logger.d(TAG,"Received Broadcast: "+msg);
        	if(msg.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
        	{
	        	// does not work?
	    		int cmd = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,-1);
	    		Logger.i(TAG,"CMD: "+cmd);
	    		switch(cmd) {
	    			case BluetoothAdapter.STATE_ON:
	    				Logger.d(TAG,"Bluetooth state changed to ON");
	    				
	    				if(getBtAutoConnect()) 
	    		    	{
	    					Logger.d(TAG,"Autoconnect requested");
	    					connect();
	    		    	}
	    				break;
	    			case BluetoothAdapter.STATE_OFF:
	    				Logger.d(TAG,"Blueotooth state changed to OFF");
	    				break;
	    			default:
	    				Logger.d(TAG,"No information about "+msg);
	    		
	    		}
        	}
        	else if(msg.equals(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED))
        	{
        		Logger.d(TAG,"SCO STATE CHANGED!!!"+msg);
        		int scoState = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
        		switch(scoState) {
        			case AudioManager.SCO_AUDIO_STATE_CONNECTED:
        				Logger.i(TAG,"SCO CONNECTED!!!!");
        				//_scoConnected = true;
        				break;
        			case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
        				Logger.i(TAG,"SCO DIS-CONNECTED!!!!");
        				//_scoConnected = false;
        				break;
        			default:
        				Logger.e(TAG,"Unhandled state");
        				//_scoConnected = false;
        				break;
        		}
        		
        	}

        	else
        	{
        		Logger.e(TAG,"Unhandled intent: "+msg);
        		
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
        		
        		Logger.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothSerialService.STATE_CONNECTED:
                	Logger.d(TAG,"BT connected");
                	setConnecting(false);
                	statusBt = true;
                	
                	_manualBtDisconnect = false;
                	//send(Frame.InputRequestAll().toInts());
                	
                	_compareAfterRecord=true;
                	recordAlarmsFromModule();
                	
                	// Dont autosend when connecting, rather autosend when setting currentModel
//                	if(getAutoSendAlarms())
//        			{
//        				for(Alarm a : _currentModel.getFrSkyAlarms().values())
//        				{
//        					send(a.toFrame());
//        				}
//        			}
                    
                    break;
                    
                case BluetoothSerialService.STATE_CONNECTING:
                	Logger.d(TAG,"BT connecting");
                	setConnecting(true);
                	 //mTitle.setText(R.string.title_connecting);
                    break;
                    
                case BluetoothSerialService.STATE_LISTEN:
                	Logger.d(TAG,"BT listening");
                case BluetoothSerialService.STATE_NONE:
                	Logger.d(TAG,"BT state changed to NONE");
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
            	//Log.w(TAG,"Received bytes: "+msg.obj);
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
                Logger.d(TAG,"BT connected to...");
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };
    
	// **************************************************************************************************************
	//                                   MODEL STUFF
	// **************************************************************************************************************
    /**
     * Adds new model to the model map (will replace if id is positive and exists)
     * @param model the model to add to the modelstore
     */
    public static void addModel(Model model)
    {
    	if(model.getId()==-1)
    	{
    		// save to get id
    		database.saveModel(model);
    		// Update channels with new model id
    		for(Channel c : model.getChannels().values())
    		{
    			c.setModelId(model.getId());
    		}
    	}
    	modelMap.put(model.getId(), model);
    	
    }
    
    /**
     * 
     * @param model the model to delete
     */
    public static void deleteModel(Model model)
    {
    	model.unregisterListeners();
    	model.frSkyAlarms.clear();
    	model.getChannels().clear();
    	database.deleteAllChannelsForModel(model);
    	database.deleteAlarmsForModel(model);
    	database.deleteModel(model.getId());
    	modelMap.remove(model.getId());
    }
    
    /**
     * 
     * @param model the model to save
     */
    public static void saveModel(Model model)
    {
    	//FIXME: What happens if saving a model with -1 id, but with channels?
    	

    	database.saveModel(model);
    }
    
    /**
     * 
     * @param modelId id of the model to save
     */
    public static void saveModel(int modelId)
    {
    	saveModel(modelMap.get(modelId));
    }
    
    /**
     * Saves a channel
     * @param channel the channel to save
     */
    public static void saveChannel(Channel channel)
    {
    	database.saveChannel(channel);
    }
    
    /**
     * Sends a models alarms to the module
     * @param model the model to send alarms for
     */
    public void sendAlarms(Model model)
    {
    	if(statusRx)
    	{
	    	for(Alarm a : model.getFrSkyAlarms().values())
			{
				send(a.toFrame());
			}
    	}
    }

    /**
     * retrieve the simulator set to this server instance
     * 
     * @return
     */
	public Simulator getSim() {
		return sim;
	}

	/**
	 * use this to broadcast new channel information
	 * 
	 * @param channel
	 */
	public void broadcastChannelData(SensorTypes channel, double value) {
		broadcastHubDataIntent.putExtra(ActivityHubData.FIELD_CHANNEL, channel.toString());
		broadcastHubDataIntent.putExtra(ActivityHubData.FIELD_VALUE, value);
		sendBroadcast(broadcastHubDataIntent);
	}

	/**
	 * retrieve the file sim instance
	 * 
	 * @return
	 */
	public static FileSimulatorThread getFileSim() {
		return fileSim;
	}

	/**
	 * set the file sim instance
	 * 
	 * @param fileSimThread
	 */
	public static void setFileSim(FileSimulatorThread fileSimThread) {
		fileSim = fileSimThread;
	}

}