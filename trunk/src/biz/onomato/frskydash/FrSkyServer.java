package biz.onomato.frskydash;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
 * Main server service. This service will get started by the first Activity
 * launched. It will stay alive even if the application is "minimized". It will
 * only be closed by a press on the "Back" button while on the Dashboard. <br>
 * <br>
 * Serves as a store for {@link Model}s, {@link Channel}s and {@link Alarm}s <br>
 * <br>
 * Receives bytebuffer from {@link BluetoothSerialService}, and parses this into
 * individual {@link Frame}s that is then sent to the respective Channel, Alarm
 * or Hub. The frame is also sent to the {@link DataLogger} for logging to file. <br>
 * <br>
 * Activities should bind to this service using startService and bindService <br>
 * <br>
 * Communication from Service to Activities: Send broadcasts<br>
 * FIXME: Cleanup Activity -> Service<br>
 * Communication from Activity to Service: Method call, startservice with
 * intent, or broadcast
 * 
 * 
 * @author eso
 * 
 */
public class FrSkyServer extends Service implements OnInitListener {

	private static final String PREFKEY_PREV_MODEL_ID = "prevModelId";

	/**
	 * tag used for logging
	 */
	public static final String TAG = "FrSkyServerService";

	// private static final UUID SerialPortServiceClass_UUID =
	// UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static final int NOTIFICATION_ID = 56;
	private AudioManager _audiomanager;
	// private boolean _scoConnected = false;

	/**
	 * Things for Bluetooth
	 */
	// private static final int REQUEST_ENABLE_BT = 2;
	private IntentFilter mIntentFilterBt;
	private boolean bluetoothEnabledAtStart;
	private boolean _connecting = false;
	private BluetoothAdapter mBluetoothAdapter = null;

	/**
	 * user preferences
	 */
	private static SharedPreferences _settings = null;

	/**
	 * editor for preferences
	 */
	private static SharedPreferences.Editor _editor;

	/**
	 * current time
	 */
	// private Long counter = 0L;
	// private NotificationManager nm;
	// private Timer timer = new Timer();
	private final Calendar time = Calendar.getInstance();

	public static final int CMD_KILL_SERVICE = 9999;
	public static final int CMD_IGNORE = -1;
	public static final int CMD_START_SIM = 0;
	public static final int CMD_STOP_SIM = 1;
	public static final int CMD_START_SPEECH = 2;
	public static final int CMD_STOP_SPEECH = 3;

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
	private static boolean _hubEnabled = false;
	private boolean _filePlaybackEnabled = false;

	private static boolean _compareAfterRecord = false;
	private boolean _autoSwitch = false;

	/**
	 * Holds frames for threaded handling
	 */
	private BlockingQueue<Frame> mFrameBuffer;
	private static int mDroppedFrames = 0;

	/**
	 * FPS
	 */
	public int fps, fpsRx, fpsTx = 0;
	public static int badFrames = 0;
	private MyStack fpsStack;
	private MyStack fpsRxStack;
	private MyStack fpsTxStack;
	private static final int FRAMES_FOR_FPS_CALC = 2;

	private static DataLogger logger;

	/**
	 * current {@link Model} ID selected by the user. This Model has
	 * {@link Channel} instances that are registered to listen for updates
	 */
	private static int currentModelId = -1;

	/**
	 * A collection of {@link Model} instances available
	 */
	public static TreeMap<Integer, Model> modelMap;

	private boolean _watchdogEnabled = true;
	private static boolean _outGoingWatchdogFlag = false;
	private static long _lastOutGoingWatchdogTime;

	private TextToSpeech mTts;
	private int _speakDelay;

	private Handler fpsHandler, watchdogHandler, speakHandler;
	private Runnable runnableFps, runnableSpeaker, runnableWatchdog;

	// server Channels, add constants for all known source channels
	// eso: refactor to ChannelMap
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

	private boolean _dying = false;

	private final IBinder mBinder = new MyBinder();

	/**
	 * wakelock to keep the screen alive
	 */
	private WakeLock wl;

	/**
	 * if cyclic speech is enabled or not for this user preferences
	 */
	private boolean _cyclicSpeechEnabled;

	/**
	 * the context of this server, used for all activity related things like
	 * broadcasts, toast messages, etc.
	 */
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

	public static boolean statusBt = false;
	public boolean statusTx = false;
	public static boolean statusRx = false;

	// private HashMap<String,Channel> _serverChannels;

	// private int MAX_CHANNELS=4;

	private int _framecount = 0;
	private int _framecountRx = 0;
	private int _framecountTx = 0;
	private boolean _autoSetVolume;

	/**
	 * map of alarms as found on the module. These can be different from the
	 * alarms set on the Model {@link Model#frSkyAlarms}.
	 */
	private static TreeMap<Integer, Alarm> _alarmMap;
	private static boolean _recordingAlarms = false;
	private static int _recordingModelId = -1;

	public static final String MESSAGE_STARTED = "biz.onomato.frskydash.intent.action.SERVER_STARTED";
	public static final String MESSAGE_SPEAKERCHANGE = "biz.onomato.frskydash.intent.action.SPEAKER_CHANGED";
	public static final String MESSAGE_BLUETOOTH_STATE_CHANGED = "biz.onomato.frskydash.intent.action.BLUETOOTH_STATE_CHANGED";

	public static final String MESSAGE_ALARM_RECORDING_COMPLETE = "biz.onomato.frskydash.intent.action.ALARM_RECORDING_COMPLETE";
	public static final String MESSAGE_ALARM_MISMATCH = "biz.onomato.frskydash.intent.action.ALARM_MISMATCH";
	/**
	 * Broadcast event to notify activities regarding a change in the modelmap,
	 * e.g. added or deleted model
	 */
	public static final String MESSAGE_MODELMAP_CHANGED = "biz.onomato.frskydash.intent.action.MODELMAP_CHANGED";

	/**
	 * Broadcast event to notify activities regarding a change of currentmodel
	 */
	public static final String MESSAGE_CURRENTMODEL_CHANGED = "biz.onomato.frskydash.intent.action.CURRENTMODEL_CHANGED";

	/**
	 * for broadcasting messages
	 */
	public LocalBroadcastManager broadcastManager;

	/**
	 * hcpl: intent used to broadcast hub data info
	 */
	private Intent broadcastHubDataIntent;

	/**
	 * a unique identifier for broadcast intents
	 */
	public static final String BROADCAST_ACTION_HUB_DATA = "biz.onomato.frskydash.intent.action.BROADCAST_HUB_DATA";

//	/**
//	 * Broadcast event to trigger a channel reset
//	 */
	// public static final String BROADCAST_CHANNEL_COMMAND_RESET_CHANNELS =
	// "biz.onomato.frskydash.intent.action.BROADCAST_CHANNEL_COMMAND_RESET_CHANNELS";
	// hcpl: these are class members now since we have to collect the data over
	// several method executions since the bytes could be spread over several
	// telemetry 11 bytes frames

	/**
	 * the current user frame we are working on. This is used to pass data
	 * between incomplete frames.
	 */
	private List<Integer> frSkyFrame = new ArrayList<Integer>(
			Frame.SIZE_TELEMETRY_FRAME);

	// private static int[] frSkyFrame = new int[Frame.SIZE_TELEMETRY_FRAME];

	@Override
	public void onCreate() {
		// always call super as first statement
		super.onCreate();
		// log info for debugging
		Logger.i(TAG, "onCreate");
		// get a reference to the application context we are starting in
		context = getApplicationContext();
		// get a broadcastmanager for this context
		broadcastManager = LocalBroadcastManager.getInstance(context);

		// TODO: setting max buffer size to 100 frames for now. Might need to
		// tune this
		mFrameBuffer = new LinkedBlockingQueue<Frame>(100);
		FrameParser frameParser = new FrameParser(mFrameBuffer);
		// thread for parsing frames
		Thread frameParserThread = new Thread(frameParser, "FrameParserThread");
		frameParserThread.setDaemon(true);
		frameParserThread.start();

		// init colections (for alarms and sourcechannels)
		_alarmMap = new TreeMap<Integer, Alarm>();
		_sourceChannelMap = new TreeMap<Integer, Channel>(
				Collections.reverseOrder());

		_audiomanager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

		// hcpl: commented since no longer in use
		// NotificationManager nm =
		// (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		Toast.makeText(this, "Service created at " + time.getTime(),
				Toast.LENGTH_LONG).show();

		// get earlier stored preferences
		Logger.i(TAG, "Try to load settings");
		_settings = context.getSharedPreferences("FrSkyDash", MODE_PRIVATE);
		_editor = _settings.edit();

		// init notification on top to let the user get back to the application
		// at any time
		showNotification();

		// init fixed channels
		setupFixedChannels();

		// init the previous model id from config, this is the id of the last
		// model used before the application was shut down
		int _prevModelId = -1;
		try {
			_prevModelId = _settings.getInt(PREFKEY_PREV_MODEL_ID, -1);
		} catch (Exception e) {
			_prevModelId = -1;
			Logger.e(TAG, "Issue fetching previous model from preferences", e);
		}
		Logger.i(TAG, "Previous ModelId was: " + _prevModelId);

		// init backend and collection to store all models available to have
		// them in memory. For easy access all models are stored in a treemap
		// with their ID as key.
		modelMap = new TreeMap<Integer, Model>();
		database = new FrSkyDatabase(getApplicationContext());
		for (Model m : database.getModels()) {
			modelMap.put(m.getId(), m);
		}
		// retrieve the last used model from this map to start working on
		Model cm = modelMap.get(_prevModelId);
		// if no previous model was selected we can create a first model to
		// start with.
		if (cm == null) {
			Logger.d(TAG, "No model exists, make a new one");
			// use some default model name
			cm = new Model("Model 1");
			// Saving to get id, this save is required since we don't get a
			// proper model id otherwise to init the alarms and so on
			// database.saveModel(cm);
			// Create Default model channels.
			cm.initializeDefaultChannels();
			// update so all alarms are saved
			database.saveModel(cm);
			// don't forget to store this created model in memory also
			modelMap.put(cm.getId(), cm);
		}
		// check if the model has alarms, if not we can init with defaults
		if (cm.getFrSkyAlarms().size() == 0) {
			Logger.d(TAG, "No alarms exists, setup with defaults");
			cm.initializeFrSkyAlarms();
			// update so all alarms are saved
			database.saveModel(cm);
		}
		// store this last model id so we can get back to it on resume
		_prevModelId = cm.getId();
		_editor.putInt(PREFKEY_PREV_MODEL_ID, _prevModelId);
		_editor.commit();

		// actually set the model we prepared as current
		Logger.d(TAG, "The current model is: " + cm.getName() + " and has id: "
				+ cm.getId());
		Logger.d(TAG, "Activating the model");
		// set this model as current
		setCurrentModel(cm);

		// create a logger for the frsky information
		logger = new DataLogger(getApplicationContext(),
				FrSkyServer.getCurrentModel(), true, true, true);
		// logger.setCsvHeader(_sourceChannels[CHANNEL_INDEX_AD1],_sourceChannels[CHANNEL_INDEX_AD2]);
		logger.setCsvHeader();
		logger.setLogToRaw(getLogToRaw());
		logger.setLogToCsv(getLogToCsv());
		logger.setLogToHuman(getLogToHuman());

		// init bluetooth connection
		mIntentFilterBt = new IntentFilter();
		mIntentFilterBt.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		mIntentFilterBt.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED);
		// mIntentFilterBt.addAction("android.bluetooth.headset.action.STATE_CHANGED");
		// Used to receive BT events
		registerReceiver(mIntentReceiverBt, mIntentFilterBt); 

		// inform via broadcast that server initialised is finished
		Logger.i(TAG, "Broadcast that i've started");
		Intent i = new Intent();
		i.setAction(MESSAGE_STARTED);
		sendBroadcast(i);

		// set a wake lock
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
		getWakeLock();

		mSerialService = new BluetoothSerialService(this, mHandlerBT);

		sim = new Simulator(this);

		// Cyclic job to "speak out" the channel values
		initSpeakHandler();

		// fps counters
		initFPSCountersAndStartCounting();

		// Cyclic job to send watchdog to the Tx module
		initWatchdogHandler();

		// register intent for listening to broadcasts
		broadcastHubDataIntent = new Intent(BROADCAST_ACTION_HUB_DATA);

	}

	/**
	 * Cyclic job to send watchdog to the Tx module
	 */
	private void initWatchdogHandler() {
		watchdogHandler = new Handler();
		runnableWatchdog = new Runnable() {
			// @Override
			public void run() {
				sendWatchdog();

				watchdogHandler.removeCallbacks(runnableWatchdog);
				watchdogHandler.postDelayed(this, 500);
			}
		};
		watchdogHandler.postDelayed(runnableWatchdog, 500);
	}

	/**
	 * init Cyclic job to "speak out" the channel values
	 */
	private void initSpeakHandler() {
		_cyclicSpeechEnabled = false;
		_speakDelay = 30000;
		speakHandler = new Handler();
		runnableSpeaker = new Runnable() {
			// @Override
			public void run() {
				Logger.i(TAG, "Cyclic Speak stuff");
				if (statusRx) {
					for (Channel c : getCurrentModel().getChannels().values()) {
						if (!c.getSilent())
							mTts.speak(c.toVoiceString(),
									TextToSpeech.QUEUE_ADD, null);
					}
				}

				speakHandler.removeCallbacks(runnableSpeaker);
				speakHandler.postDelayed(this, _speakDelay);
			}
		};
	}

	/**
	 * initialise fps counters and start counting (delayed)
	 */
	private void initFPSCountersAndStartCounting() {
		// Cyclic handler to calculate FPS, and set the various connection
		// statuses
		fpsStack = new MyStack(FRAMES_FOR_FPS_CALC); // try with 2 seconds..
		fpsRxStack = new MyStack(FRAMES_FOR_FPS_CALC); // try with 2 seconds..
		fpsTxStack = new MyStack(FRAMES_FOR_FPS_CALC); // try with 2 seconds..
		fpsHandler = new Handler();
		runnableFps = new Runnable() {
			// @Override
			public void run() {
				fpsStack.push(_framecount);
				fpsRxStack.push(_framecountRx);
				fpsTxStack.push(_framecountTx);

				fps = (int) Math.floor(fpsStack.average());
				fpsRx = (int) Math.floor(fpsRxStack.average());
				fpsTx = (int) Math.floor(fpsTxStack.average());

				// TODO this might be optimized, would need proper testing
				// though
				if (fpsRx > 0) // receiving frames from Rx, means Tx comms is up
								// as well
				{
					// check if we should restart the cyclic speaker
					if ((statusRx == false) && (getCyclicSpeechEnabled())) {
						// Restart speaker if running
						startCyclicSpeaker();
					}
					statusRx = true;
					statusTx = true;
				} else // not receiving frames from Rx
				{
					// make sure user knows if state changed from ok to not ok
					// only do this if Bt connection is up
					if ((statusRx == true) && (statusBt == true)) {
						wasDisconnected("Rx");
					}
					statusRx = false;

					if (fpsTx > 0) // need to check if Tx comms is up
					{
						statusTx = true;
					} else {
						statusTx = false;
					}

				}

				_framecount = 0;
				_framecountRx = 0;
				_framecountTx = 0;
				// Log.i(TAG,"FPS: "+fps);
				fpsHandler.removeCallbacks(runnableFps);
				fpsHandler.postDelayed(this, 1000);
			}
		};
		// Start the FPS counters
		fpsHandler.postDelayed(runnableFps, 1000);
	}

	/**
	 * eso: Send a "Request all alarms" command to the FrSky radio module. The
	 * returns from this command can be used to calculate FPS and connection
	 * status
	 */
	public void sendWatchdog() {
		// Send get all alarms frame to force frames from Tx

		if (_watchdogEnabled) {
			// check if we already sent one
			if (_outGoingWatchdogFlag) {
				// How long ago?
				if ((System.currentTimeMillis() - _lastOutGoingWatchdogTime) > 5000) {
					// More than 5 seconds ago, reset outgoing flag
					_outGoingWatchdogFlag = false;
				}
			} else // no outgoing watchdog
			{
				// only do this if not receiving anything from Rx side
				if ((statusRx == false) && (statusBt == true)) {
					send(Frame.InputRequestADAlarms());
					_outGoingWatchdogFlag = true;
					_lastOutGoingWatchdogTime = System.currentTimeMillis();
				}
			}
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		Logger.i(TAG, "Something tries to bind to me");
		return mBinder;
		// return null;
	}

	// **************************************************************************************************************
	// GETTERS AND SETTERS
	// **************************************************************************************************************

	/**
	 * Get dropped frame count
	 * 
	 * @return count of dropped frames since last connect
	 */
	public static int getDroppedFrames() {
		return mDroppedFrames;
	}

	/**
	 * 
	 * @param state
	 *            Determines if the cyclic watchdogs should be sent or not
	 */
	public void setWatchdogEnabled(boolean state) {
		_watchdogEnabled = state;
	}

	/**
	 * 
	 * @return if the cyclic watchdog is enabled or not
	 */
	public boolean getWatchdogEnabled() {
		return _watchdogEnabled;
	}

	/**
	 * 
	 * @return a ServerChannel corresponding to the given id
	 */
	public static Channel getSourceChannel(int id) {
		return _sourceChannelMap.get(id);
	}

	/**
	 * Fetch channel from channellists
	 * 
	 * @param channelId
	 * @return
	 */
	public static Channel getChannel(int channelId) {
		// server source channels
		Channel ch;
		try {
			ch = _sourceChannelMap.get(channelId);
		} catch (Exception e) {
			ch = null;

		}

		if ((ch == null) && (_hubEnabled == true)) {
			try {
				ch = FrSkyHub.getInstance().getChannel(channelId);

			} catch (Exception e) {
				ch = null;
			}
		}

		return ch;

		// hub source channels
	}

	/**
	 * 
	 * @return all the ServerChannels in a TreeMap (key is the id of the
	 *         Channel)
	 */
	public static TreeMap<Integer, Channel> getSourceChannels() {
		return _sourceChannelMap;
	}

	/**
	 * 
	 * @return the application context
	 */
	public static Context getContext() {
		// Log.e(TAG,"Someone asked me for context!");
		return context;
	}

	/**
	 * Set time between voice output.
	 * 
	 * @param interval
	 *            time in seconds between reads
	 */
	public void setCyclicSpeechInterval(int interval) {
		Logger.i(TAG, "Set new interval to " + interval + " seconds");
		_editor.putInt("cyclicSpeakerInterval", interval);
		_editor.commit();
		if (interval > 0) {
			_speakDelay = interval * 1000;
			if (getCyclicSpeechEnabled()) {
				// Restart speaker if running
				startCyclicSpeaker();
			}
		}
	}

	/**
	 * 
	 * @return The time in seconds between voice output
	 */
	public int getCyclicSpeechInterval() {
		return _settings.getInt("cyclicSpeakerInterval", 30);
	}

	/**
	 * 
	 * @param set
	 *            to true while the application is connection to BlueTooth
	 */
	private void setConnecting(boolean connecting) {
		_connecting = connecting;
	}

	/**
	 * 
	 * @return returns true if the application is trying to connect
	 */
	public boolean getConnecting() {
		return _connecting;
	}

	/**
	 * 
	 * @return TreeMap containing alarms recorded from the FrSky module
	 */
	public TreeMap<Integer, Alarm> getRecordedAlarmMap() {
		return _alarmMap;
	}

	/**
	 * 
	 * @return true if the cyclic speaker should be enabled at startup
	 */
	public boolean getCyclicSpeechEnabledAtStartup() {
		return _settings.getBoolean("cyclicSpeakerEnabledAtStartup", false);
	}

	/**
	 * 
	 * @param state
	 *            set to true if you want Cyclic speaker to be enabled at
	 *            startup
	 */
	public void setCyclicSpeechEnabledAtStartup(boolean state) {
		Logger.i(TAG, "Setting Cyclic speech to: " + state);
		_editor.putBoolean("cyclicSpeakerEnabledAtStartup", state);
		_editor.commit();
		// _cyclicSpeechEnabled = state;
	}

	/**
	 * 
	 * @return true if cyclic speaker is enabled
	 */
	public boolean getCyclicSpeechEnabled() {
		return _cyclicSpeechEnabled;
	}

	/**
	 * 
	 * @param state
	 *            set to true to enable cyclic speaker
	 * 
	 */
	public void setCyclicSpeechEnabled(boolean state) {
		_cyclicSpeechEnabled = state;
		if (state) {
			startCyclicSpeaker();
		} else {
			stopCyclicSpeaker();
		}
	}

	/**
	 * 
	 * @return the current FPS as a string.
	 * 
	 *         NOTE: Will return FPS from rx if rx communication is up, and FPS
	 *         from tx otherwise
	 */
	public String getFps() {
		// return Integer.toString(fpsRx);
		if (statusRx) {
			return Integer.toString(fpsRx);
		} else {
			return Integer.toString(fpsTx);
		}
	}

	/**
	 * 
	 * @param lastConnectedToAddress
	 *            an address to store persistantly, used to attempt autoconnect
	 */
	private void setBtLastConnectedToAddress(String lastConnectedToAddress) {
		_editor.putString("btLastConnectedToAddress", lastConnectedToAddress);
		_editor.commit();
	}

	/**
	 * 
	 * @return address of the previously connected Bluetooth device
	 */
	private String getBtLastConnectedToAddress() {
		return _settings.getString("btLastConnectedToAddress", "");
	}

	/**
	 * 
	 * @return true if logging to binary/raw file is enabled
	 */
	public boolean getLogToRaw() {
		return _settings.getBoolean("logToRaw", false);
	}

	/**
	 * 
	 * @return true if logging to CSV file is enabled
	 */
	public boolean getLogToCsv() {
		return _settings.getBoolean("logToCsv", false);
	}

	/**
	 * 
	 * @return true if logging to human readable file is enabled
	 */
	public boolean getLogToHuman() {
		return _settings.getBoolean("logToHuman", false);
	}

	/**
	 * 
	 * @param state
	 *            true to enable logging to binary file
	 */
	public void setLogToRaw(boolean state) {
		_editor.putBoolean("logToRaw", state);
		_editor.commit();
		logger.setLogToRaw(state);
	}

	/**
	 * 
	 * @param state
	 *            true to enable logging to human readable file
	 */
	public void setLogToHuman(boolean logToHuman) {
		_editor.putBoolean("logToHuman", logToHuman);
		_editor.commit();
		logger.setLogToHuman(logToHuman);
	}

	/**
	 * 
	 * @param state
	 *            true to enable logging to CSV file
	 */
	public void setLogToCsv(boolean logToCsv) {
		_editor.putBoolean("logToCsv", logToCsv);
		_editor.commit();
		logger.setLogToCsv(logToCsv);
	}

	/**
	 * 
	 * @param state
	 *            true to autoenable bluetooth at startup
	 */
	public void setBtAutoEnable(boolean state) {
		// _btAutoEnable = state;
		_editor.putBoolean("btAutoEnable", state);
		_editor.commit();
	}

	/**
	 * 
	 * @return true if Bluetooth auto enable is set
	 */
	public boolean getBtAutoEnable() {
		return _settings.getBoolean("btAutoEnable", false);
	}

	/**
	 * 
	 * @param state
	 *            true to attempt autoconnect at startup
	 */
	public void setBtAutoConnect(boolean state) {
		// _btAutoConnect = state;
		_editor.putBoolean("btAutoConnect", state);
		_editor.commit();
	}

	/**
	 * Enable/Disable hub support
	 * 
	 * @param state
	 *            true to enable hub support
	 */
	public void setHubEnabled(boolean state) {
		_editor.putBoolean("hubEnabled", state);
		_editor.commit();
		_hubEnabled = state;
	}

	/**
	 * Get current hub enabled state
	 */
	public boolean getHubEnabled() {
		_hubEnabled = _settings.getBoolean("hubEnabled", false);
		return _hubEnabled;
	}

	/**
	 * Enable/Disable simulator playback features
	 * 
	 * @param state
	 *            true to enable
	 */
	public void setFilePlaybackEnabled(boolean state) {
		_editor.putBoolean("filePlaybackEnabled", state);
		_editor.commit();
		_filePlaybackEnabled = state;
	}

	public boolean getFilePlaybackEnabled() {
		_filePlaybackEnabled = _settings.getBoolean("filePlaybackEnabled",
				false);
		return _filePlaybackEnabled;
	}

	/**
	 * 
	 * @return true if startup autoconnect is enabled
	 */
	public boolean getBtAutoConnect() {
		return _settings.getBoolean("btAutoConnect", false);
	}

	/**
	 * 
	 * @param minimumVolumePrc
	 *            the minimum volume (percentage 0 - 100) the application should
	 *            use for cyclic speaker
	 * @see #setAutoSetVolume(boolean)
	 */
	public void setMinimumVolume(int minimumVolumePrc) {
		// _minimumVolumeLevel=minimumVolumePrc;
		_editor.putInt("initialMinimumVolume", minimumVolumePrc);
		_editor.commit();
	}

	/**
	 * 
	 * @return the minimum volume (percentage 0 - 100) used for cyclic speaker
	 */
	public int getMinimumVolume() {
		return _settings.getInt("initialMinimumVolume", 70);
	}

	/**
	 * 
	 * @param state
	 *            true to have the application autoset the media volume on
	 *            startup <br>
	 * <br>
	 * @see #setMinimumVolume(int)
	 */
	public void setAutoSetVolume(boolean state) {
		// _autoSetVolume = state;
		_editor.putBoolean("autoSetVolume", _autoSetVolume);
		_editor.commit();
	}

	/**
	 * 
	 * @return true if the application is set to autoset the volume at startup
	 */
	public boolean getAutoSetVolume() {
		return _settings.getBoolean("autoSetVolume", false);
	}

	/**
	 * 
	 * @param state
	 *            set true to have application send a models alarms on model
	 *            change
	 * @see #setCurrentModel(Model)
	 */
	public void setAutoSendAlarms(boolean state) {
		_editor.putBoolean("autoSendAlarms", state);
		_editor.commit();
	}

	/**
	 * 
	 * @return true if application is set to autosend alarms on model change
	 */
	public static boolean getAutoSendAlarms() {
		return _settings.getBoolean("autoSendAlarms", false);
	}

	/**
	 * <p>
	 * Get a hold of the current model. If for some reason this current model is
	 * not known the first entry in the list of models will be returned instead.
	 * </p>
	 * <p>
	 * <b><u>ALWAYS USE THIS METHOD TO REFER TO CURRENT MODEL!!</u></b>
	 * </p>
	 * 
	 * @return the current Model
	 */
	public static Model getCurrentModel() {
		// if for some reason current model is -1 (unknown) we need to return the first entry of the collection instead
		if( !validCurrentModel() ){
			Logger.w(TAG, "Invalid current model on server side, send first one instead. CurrentModelId="+currentModelId);
			setCurrentModel(modelMap.get(modelMap.firstKey()));
			// FIXME at this point we could have a situation where no models are
			// available so that no first key is available so this will stil
			// fail. But the app is designed so that always at least a first
			// default Model is available. This is however risky.
		}
		// return requested model from server collection
		return modelMap.get(currentModelId);
	}

	/**
	 * mapper for setting current model on this server 
	 * 
	 * @param modelId
	 *            the id of the model the application should be monitoring
	 */
	public static void setCurrentModel(int modelId) {
		setCurrentModel(modelMap.get(modelId));
	}

	/**
	 * update server to given model
	 * 
	 * @param updateModel
	 *            the model the application should be monitoring
	 */
	public static void setCurrentModel(Model updateModel) {
		// check if we currently have a model set and perform some logging
		if (validCurrentModel()) {
			Logger.i(TAG, "Changing Models from " + getCurrentModel().getName()
					+ " to " + updateModel.getName());
			// need to unregister that model here!
			getCurrentModel().unregisterListeners();
		} 
		// otherwise no valid model was available so we can only log this
		// information, no big deal
		else {
			Logger.i(TAG,
					"Changing Models from NULL to " + updateModel.getName());
		}
		// update current model 
		currentModelId = updateModel.getId();
		// save this new model Id in the settings so we can recover on resume of activity
		_editor.putInt(PREFKEY_PREV_MODEL_ID, currentModelId);
		_editor.commit();
		// reset counter for bad frames
		badFrames = 0;
		// update logger for this model
		if (logger != null) {
			logger.setModel(updateModel);
		}
		// check if alarms are available
		if (updateModel.getFrSkyAlarms().size() == 0) {
			// set default alarms if none were set yet
			updateModel.initializeFrSkyAlarms();
			// we updated the model so we need to save it now
			database.saveModel(updateModel);
		}
		// otherwise we have already alarms set to this model so we can send
		// them to module
		// we already have alarms
		// send them if user wants
		else if (getAutoSendAlarms()) {
			sendAlarms(updateModel);
		}
		// request alarms from module same as connect
		else {
			recordAlarmsFromModule(true);
		}
		// now register listeners on this activated model
		updateModel.registerListeners();
		// _currentModel.setFrSkyAlarms(database.getAlarmsForModel(_currentModel));
		// logger.stop(); // SetModel will stop current Logger
		// inform user about this change
		Toast.makeText(context,
				updateModel.getName() + " set as the active model",
				Toast.LENGTH_LONG).show();
		// and broadcast a message that the current model has changed
		Intent i = new Intent();
		i.setAction(MESSAGE_CURRENTMODEL_CHANGED);
		context.sendBroadcast(i);
	}

	/**
	 * 
	 * @return the application settings
	 */
	public SharedPreferences getSettings() {
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
	// APPLICATION/SERVICE STUFF
	// **************************************************************************************************************

	/**
	 * Used to allow the Activities to attach to the service. Study the
	 * activities for how to use
	 * 
	 */
	public class MyBinder extends Binder {
		public FrSkyServer getService() {
			return FrSkyServer.this;
		}
	}

	/**
	 * Called when activities run startService
	 * 
	 * @see #handleIntent
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Logger.i(TAG, "Receieved startCommand or intent ");
		handleIntent(intent);
		return START_STICKY;
	}

	/**
	 * Used to show a notification icon in the notification field. Necessary
	 * since we want the application to be able to run even if the user goes
	 * back to the home screen.
	 */
	private void showNotification() {
		CharSequence text = "FrSkyServer Started";
		Notification notification = new Notification(R.drawable.ic_status,
				text, System.currentTimeMillis());

		notification.ledOffMS = 500;
		notification.ledOnMS = 500;
		notification.ledARGB = 0xff00ff00;

		notification.flags |= Notification.FLAG_SHOW_LIGHTS;
		notification.flags |= Notification.FLAG_ONGOING_EVENT;
		notification.flags |= Notification.FLAG_NO_CLEAR;

		Intent notificationIntent = new Intent(this, ActivityDashboard.class);
		notificationIntent.setAction(Intent.ACTION_MAIN);
		notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(this, "FrSkyDash", text, contentIntent);
		startForeground(NOTIFICATION_ID, notification);
	}

	/**
	 * Request wakelock
	 */
	public void getWakeLock() {
		if (!wl.isHeld()) {
			Logger.i(TAG, "Acquire wakelock");
			wl.acquire();
		} else {
			Logger.i(TAG, "Wakelock already acquired");
		}
	}

	/**
	 * Handler to handle incoming intents from activities
	 * 
	 * @param intent
	 *            the intent to react on
	 */
	public void handleIntent(Intent intent) {
		int cmd = intent.getIntExtra("command", CMD_IGNORE);
		Logger.i(TAG, "CMD: " + cmd);
		switch (cmd) {
		case CMD_START_SIM:
			Logger.i(TAG, "Start Simulator");
			break;
		case CMD_STOP_SIM:
			Logger.i(TAG, "Stop Simulator");
			break;
		case CMD_START_SPEECH:
			Logger.i(TAG, "Start Speaker");

			break;
		case CMD_STOP_SPEECH:
			Logger.i(TAG, "Stop Speaker");
			break;
		case CMD_KILL_SERVICE:
			Logger.i(TAG, "Killing myself");
			die();
			break;
		case CMD_IGNORE:
			// Log.i(TAG,"No command, skipping");
			break;
		default:
			Logger.i(TAG, "Command " + cmd + " not implemented. Skipping");
			break;
		}
	}

	/*
	 * Used to initiate shutdown of the application, will trigger onDestroy()
	 */
	public void die() {
		Logger.i(TAG, "Die, perform cleanup");

		stopSelf();
	}

	@Override
	public void onDestroy() {
		_dying = true;

		Logger.i(TAG, "onDestroy");

		_audiomanager.stopBluetoothSco();

		simStop();
		unregisterReceiver(mIntentReceiverBt);
		// sim.reset();

		// stop filesim thread also
		if (fileSim != null)
			fileSim.stopThread();

		// disable bluetooth if it was disabled upon start:

		if (!bluetoothEnabledAtStart) // bluetooth was not enabled at start
		{
			if (mBluetoothAdapter != null)
				mBluetoothAdapter.disable(); // only do this if bluetooth
												// feature exists
		}
		Logger.i(TAG, "Releasing Wakelock");
		if (wl.isHeld()) {
			wl.release();
		}
		stopCyclicSpeaker();
		Logger.i(TAG, "Shutdown mTts");

		try {
			mTts.shutdown();
		} catch (Exception e) {
		}

		Logger.i(TAG, "Stop BT service if neccessary");
		if (mSerialService.getState() != BluetoothSerialService.STATE_NONE) {
			try {
				mSerialService.stop();
			} catch (Exception e) {
			}
		}

		// Disable BT

		Logger.i(TAG, "Stop FPS counter");
		fpsHandler.removeCallbacks(runnableFps);

		Logger.i(TAG, "Reset channels");
		destroyChannels();

		Logger.i(TAG, "Stop Logger");
		try {
			logger.stop();
		} catch (Exception e) {

		}

		// stopCyclicSpeaker();

		Logger.i(TAG, "Remove from foreground");
		try {
			stopForeground(true);
		} catch (Exception e) {
			Logger.d(TAG, "Exeption during stopForeground");
		}

		try {
			super.onDestroy();
		} catch (Exception e) {
			Logger.d(TAG, "Exeption during super.onDestroy");
		}
		try {
			Toast.makeText(this, "Service destroyed at " + time.getTime(),
					Toast.LENGTH_LONG).show();
		} catch (Exception e) {
			Logger.d(TAG, "Exeption during last toast");
		}
	}

	/**
	 * Called after TextToSpeech was requested
	 */
	public void onInit(int status) {
		Logger.i(TAG, "TTS initialized");
		// status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
		if (status == TextToSpeech.SUCCESS) {
			int result = mTts.setLanguage(Locale.US);
			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				// Lanuage data is missing or the language is not supported.
				Logger.e(TAG, "Language is not available.");
			} else {
				// Check the documentation for other possible result codes.
				// For example, the language may be available for the locale,
				// but not for the specified country and variant.
				// The TTS engine has been successfully initialized.
				// Allow the user to press the button for the app to speak
				// again.

				// Greet the user.
				String myGreeting = "Application has enabled Text to Speech";
				mTts.speak(myGreeting, TextToSpeech.QUEUE_FLUSH, null);

				setCyclicSpeechEnabled(getCyclicSpeechEnabledAtStartup());

			}
		} else {
			// Initialization failed.
			Logger.i(TAG, "Something wrong with TTS");
			Logger.e(TAG, "Could not initialize TextToSpeech.");
		}
	}

	/**
	 * Setup the server fixed channels
	 */
	private void setupFixedChannels() {
		// Sets up the hardcoded channels (AD1,AD2,RSSIrx,RSSItx)
		Channel none = new Channel("None", 0, 1, "", "");
		none.setId(CHANNEL_ID_NONE);

		none.setPrecision(0);
		none.setSilent(true);
		_sourceChannelMap.put(CHANNEL_ID_NONE, none);

		Channel ad1 = new Channel("AD1", 0, 1, "", "");
		ad1.setId(CHANNEL_ID_AD1);
		ad1.setPrecision(0);
		ad1.setSilent(true);
		// ad1.registerListenerForServerCommands();
		_sourceChannelMap.put(CHANNEL_ID_AD1, ad1);

		Channel ad2 = new Channel("AD2", 0, 1, "", "");
		ad2.setId(CHANNEL_ID_AD2);
		ad2.setPrecision(0);
		ad2.setSilent(true);
		// ad2.registerListenerForServerCommands();
		_sourceChannelMap.put(CHANNEL_ID_AD2, ad2);

		Channel rssirx = new Channel("RSSIrx", 0, 1, "", "");
		rssirx.setId(CHANNEL_ID_RSSIRX);
		rssirx.setPrecision(0);
		rssirx.setMovingAverage(-1);
		rssirx.setLongUnit("dBm");
		rssirx.setShortUnit("dBm");
		rssirx.setSilent(true);
		// rssirx.registerListenerForServerCommands();
		_sourceChannelMap.put(CHANNEL_ID_RSSIRX, rssirx);

		Channel rssitx = new Channel("RSSItx", 0, 1, "", "");
		rssitx.setId(CHANNEL_ID_RSSITX);
		rssitx.setPrecision(0);
		rssitx.setMovingAverage(-1);
		rssitx.setLongUnit("dBm");
		rssitx.setShortUnit("dBm");
		rssitx.setSilent(true);
		// rssitx.registerListenerForServerCommands();
		_sourceChannelMap.put(CHANNEL_ID_RSSITX, rssitx);
	}

	/**
	 * Destroy all channels
	 */
	private void destroyChannels() {
		// loop all models
		// loop all channels
		// channel.close()
		for (Model m : modelMap.values()) {
			for (Channel c : m.getChannels().values()) {
				c.close();
			}
		}

		for (Channel c : _sourceChannelMap.values()) {
			// c.setRaw(0);
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
	public boolean alarmsSameAsModel(Model model) {
		// makes no sense if no model is given
		if (model == null)
			return false;
		// otherwise we can check the alarms set
		// iterate all alarms on the current model
		for (Alarm a : _alarmMap.values()) {
			// Log.w(TAG,"Checking "+a.getFrSkyFrameType());
			// as soon as we spot a difference
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
	 * helper to check if we have a valid current model set or not
	 * @return
	 */
	private static boolean validCurrentModel(){
		return modelMap.containsKey(currentModelId);
	}

	/**
	 * Compare incoming alarms to currentModels alarms <br>
	 * NOTE: Incomplete
	 */
	public void compareAlarms() {
		// hcpl: isn't it better to start from not being equal? Otherwise on
		// error you might end up with equal while check hasn't passed = false
		// positive => fixed by initialising models always with alarms
//		boolean equal = true;
		// we can only check if the current model is set
		if (validCurrentModel()) {
			// at this point the model available so we can compare the alarms
			// hcpl this local param is no longer used? 
//			equal = ;
			if (alarmsSameAsModel(modelMap.get(currentModelId))) {
				// infor models are equals
				Logger.i(TAG, "Alarm sets are equal");
			} 
			// otherwise models aren't equals so we can inform the user
			else {
				Logger.i(TAG,
						"Alarm sets are not equal, see if i can find a model that is equal");
				// start with the assumption that we didn't find the matching model
				boolean found = false;
				// iterate all available models
				for (Model m : modelMap.values()) {
					// no point checking currentModel again
					// skip current model. WARNING it's not a good idea in java
					// to check on objects with these operators! This will fail
					// if we have updated that model some time in DB since then
					// a new object might be created instead (need to check code
					// in depth for that). Best way in Java is to override the
					// equals method. In this case we have the model ID to check
					// on.
					if( m.getId() == currentModelId)
						continue; // this will skip to next iteration
					if (alarmsSameAsModel(m)) {
						// indicate that we found a model with the same alarms
						found = true;
						Logger.d(TAG, "Alarms match model " + m.getName());
						// _autoSwitch should come from settings
						if (_autoSwitch) {
							// setCurrentModel(m);
							// FIXME sure we want this disabled?
							Logger.i(TAG, "Auto Switch model");
						} else {
							Logger.d(TAG,
									"Show popup allow switch of model");
							Intent i = new Intent(MESSAGE_ALARM_MISMATCH);
							i.putExtra("modelId", m.getId());
							sendBroadcast(i);
						}
						// stop iteration at this point, 
						break;
					}
				}
				// check if we found a matching model or not
				if (!found) {
					Logger.i(TAG, "Show popup no switch option");
					Intent i = new Intent(MESSAGE_ALARM_MISMATCH);
					i.putExtra("modelId", -1);
					sendBroadcast(i);
				}
			}
		}
		//?
		_compareAfterRecord = false;
	}

	/**
	 * Delete all the logfiles
	 */
	public void deleteAllLogFiles() {
		Logger.i(TAG, "Really delete all log files");
		// Make logger stop logging, and close files
		logger.stop();

		// get list of all ASC files
		File path = getExternalFilesDir(null);
		String[] files = path.list();
		for (int i = 0; i < files.length; i++) {
			File f = new File(getExternalFilesDir(null), files[i]);
			Logger.i(TAG, "Delete: " + f.getAbsolutePath());
			f.delete();
		}
		Toast.makeText(getApplicationContext(), "All logs file deleted",
				Toast.LENGTH_LONG).show();
	}

	// **************************************************************************************************************
	// BLUETOOTH STUFF
	// **************************************************************************************************************

	/**
	 * Attempts to reconnect to the bluetooth device we lost connection with
	 */
	public void reConnect() {
		// if(getConnectionState()==BluetoothSerialService.)
		mSerialService.connect(_device);
	}

	/**
	 * 
	 * @param device
	 *            the Bluetooth device we want to connect to
	 */
	public void connect(BluetoothDevice device) {

		setConnecting(true);

		logger.stop(); // stop the logger (will force creation of new files)
		_device = device;
		mSerialService.connect(device);
	}

	/**
	 * Connects to the stored Bluetooth device
	 */
	public void connect() // connect to previous device
	{
		if (mBluetoothAdapter.isEnabled()) // only connect if adapter is enabled
		{
			if (getBtLastConnectedToAddress() != "") {
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(getBtLastConnectedToAddress());
				connect(device);
			}
		}
	}

	/**
	 * Disconnects from the Bluetooth device
	 */
	public void disconnect() {
		_manualBtDisconnect = true;
		mSerialService.stop();
	}

	/**
	 * Forces reconnection of the bluetooth link
	 */
	public void reconnectBt() {
		mSerialService.stop();
		mSerialService.start();
	}

	/**
	 * 
	 * @return the device's default BluetoothAdapter
	 */
	public BluetoothAdapter getBluetoothAdapter() {
		Logger.i(TAG, "Check for BT");
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			// Device does not support Bluetooth
			Logger.i(TAG, "Device does not support Bluetooth");
			// Disable all BT related menu items
		}

		// popup to enable BT if not enabled
		if (mBluetoothAdapter != null) {
			if (!mBluetoothAdapter.isEnabled()) {
				bluetoothEnabledAtStart = false;
				Logger.d(TAG, "BT NOT enabled at start");
				if (getBtAutoEnable()) {
					mBluetoothAdapter.enable();
					Toast.makeText(this, "Bluetooth autoenabled",
							Toast.LENGTH_LONG).show();
				} else {
					Logger.i(TAG, "Request user to enable bt");

				}
			} else {
				bluetoothEnabledAtStart = true;
				Logger.d(TAG, "BT enabled at start");

				// autoconnect here if autoconnect
				if (getBtAutoConnect()) {
					connect();
				}
			}
		}
		return mBluetoothAdapter;
	}

	// **************************************************************************************************************
	// COMMUNICATION
	// **************************************************************************************************************

	/**
	 * Transmits bytes to the Bluetooth serial receiver
	 * 
	 * @param out
	 *            Array of the bytes to send
	 */
	public void send(byte[] out) {
		mSerialService.write(out);
	}

	/**
	 * Transmits ints to the Bluetooth serial receiver
	 * 
	 * @param out
	 *            Array of the ints to send
	 */
	public static void send(int[] out) {
		mSerialService.write(out);
	}

	/**
	 * Transmits a frame to the Bluetooth serial receiver
	 * 
	 * @param f
	 *            the frame to send
	 */
	public static void send(Frame f) {
		// Log.w(TAG,"Sending: "+f.toHuman());
		send(f.toInts());
	}

	// public void recordAlarmsFromModule()
	// {
	// recordAlarmsFromModule(-1);
	// }
	public static void recordAlarmsFromModule(boolean compare) {
		_compareAfterRecord = compare;
		recordAlarmsFromModule(-1, compare);
	}

	/**
	 * Used to start recording alarms from the FrSky Module, To use the alarms,
	 * listen to the MESSAGE_ALARM_RECORDING_COMPLETE broadcast, then use
	 * <b>getRecordedAlarmMap()</b> to retrieve them.
	 * 
	 * @param modelId
	 *            id of the model you want the recorded alarms to be stored on
	 */
	public static void recordAlarmsFromModule(int modelId, boolean compare) {
		_compareAfterRecord = compare;
		// empty the map, allowing others to monitor it for becoming full again
		_recordingAlarms = true;
		_recordingModelId = modelId;
		_alarmMap.clear();
		Logger.d(TAG, "Requesting alarms");

		// Only send request for RSSI alarms if Rx communication is up since we
		// do automatic requests for alarms otherwise
		// if((statusRx==true) && (statusBt==true))
		if (statusBt == true) {
			// send(Frame.InputRequestRSSIAlarms());
			send(Frame.InputRequestADAlarms());
		} else {
			Logger.d(TAG, "Request for alarms not sent as StatusRx=" + statusRx
					+ " and statusBt=" + statusBt);
		}
		// send(Frame.InputRequestADAlarms());
	}

	/**
	 * Gets called whenever bluetooth connection is unintentionally dropped
	 * 
	 * @param source
	 *            the name of the device we lost connection with
	 */
	public void wasDisconnected(String source) {
		// /TODO: eso: reset any hub channels as well
		// TODO: eso: this only resets source channels, should reset ALL
		// channels
		// for (Channel c : _sourceChannelMap.values()) {
		// c.reset();
		// }

		// Intent i = new Intent();
		// i.setAction(BROADCAST_CHANNEL_COMMAND_RESET_CHANNELS);
		// Logger.i(TAG, "Sending Reset Broadcast");
		// sendBroadcast(i);
		//
		Logger.i(TAG, "Sending Reset Command to channels");
		for (Model m : modelMap.values()) {
			for (Channel c : m.getChannels().values()) {
				c.reset();
			}
		}

		// speak warning, only when not manually disconnected
		if (!_manualBtDisconnect) {
			saySomething("Alarm! Alarm! Alarm! Connection Lost!");
			Toast.makeText(getApplicationContext(),
					"Lost connection with " + source, Toast.LENGTH_SHORT)
					.show();
			// Get instance of Vibrator from current Context
			try {
				Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
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
	// BYTE STREAM ANALYSIS
	// **************************************************************************************************************

	/**
	 * for simulation and testing only
	 * 
	 * @param buffer
	 * @param signed
	 */
	public void handleStringBuffer(String buffer, String separator,
			boolean signed) {
		// construct an int array out of the string
		List<Integer> list = new ArrayList<Integer>();
		// parse string
		for (String str : buffer.split(separator)) {
			try {
				list.add(Integer.decode(str.trim()));
			} catch (NumberFormatException nfe) {
				Logger.e(TAG, "dropped unparseable byte: " + str);
			}
		}
		// translate
		int[] ints = new int[list.size()];
		for (int i = 0; i < list.size(); i++)
			ints[i] = list.get(i);
		// and send on
		handleIntBuffer(ints, signed);
	}

	/**
	 * for simulation and testing only
	 * 
	 * @param buffer
	 * @param signed
	 */
	public void handleIntBuffer(int[] buffer, boolean signed) {
		// rework that int buffer into a byte buffer
		byte[] byteBuffer = new byte[buffer.length];
		for (int i = 0; i < buffer.length; i++) {
			byteBuffer[i] = (byte) (signed ? buffer[i] : buffer[i] - 256);
		}
		// todo this requires a reset of the buffers state
		// shouldn't be needed since it will reset properly on it's own on
		// reaching delimiters
		// frSkyFrame = new int[Frame.SIZE_TELEMETRY_FRAME];
		// currentFrSkyFrameIndex = -1;
		// frSkyXOR = false;
		// pass on
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
		for (int i = 0; i < buffer.length; i++) {
			// use & 0xff to properly convert from byte to 0-255 int
			// value (java only knows signed bytes)
			b = buffer[i] & 0xff;
			// no decoding at this point, just parse the frames from this buffer
			// on start stop byte we need to pass alon the collected frame and
			// clean up so we can start collecting another frame
			if (b == Frame.START_STOP_TELEMETRY_FRAME) {
				// we already have content so we were working on a valid frame
				if (!frSkyFrame.isEmpty()) {
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
	// FRAME HANDLING
	// **************************************************************************************************************

	/**
	 * Converts a list of ints corresponding to a frame, to a Frame instance.
	 * 
	 * @param list
	 * 
	 *            NOTE: eso: we could add Frame(List<Integer>) ctor
	 */
	// public void handleFrame(int[] frame){
	public void handleFrame(List<Integer> list) {
		// first convert
		int[] ints = new int[list.size()];
		// index
		int i = 0;
		// iterate
		for (int li = 0; li < list.size(); li++) {
			ints[i++] = list.get(li);
		}

		// if(ints.length>11)
		// {
		// Log.d(TAG,"Incoming list has strange length");
		// Log.d(TAG,"\tList: "+Frame.frameToHuman(ints));
		// }

		// then pass to ctor Frame
		Frame f = new Frame(ints);
		// Code for threaded parsing

		// If buffer is full, try to drop oldest frame to make room for new
		// frame
		if (mFrameBuffer.remainingCapacity() == 0) {
			try {
				/*Frame of = */mFrameBuffer.take();
				mDroppedFrames++;
				Logger.w(TAG, "Dropped oldest Frame");
			} catch (Exception e) {

			}

		}
		if (!mFrameBuffer.offer(f)) {
			// The buffer was still unable to take the frame, so this one got
			// dropped as well
			mDroppedFrames++;
			// Logger.e(TAG, "Dropped newest Frame");
		}

		// old unthreaded parsing
		// parseFrame(f);
	}

	/**
	 * Determines what to do with a single frame
	 * 
	 * @param f
	 *            the frame to parse
	 * @param inBound
	 *            set to false to mask the frame from FPS calculations
	 * @return always true
	 */
	public boolean parseFrame(Frame f, boolean inBound) {
		// int [] frame = f.toInts();
		boolean ok = true;
		if (inBound) // only log inbound frames
		{
			logger.logFrame(f);
			_framecount++;
		}
		// Log.w(TAG,f.toHuman());
		switch (f.frametype) {
		case Frame.FRAMETYPE_CORRUPT:
			Logger.w(TAG,
					"Frame most likely corrupt, discarded: " + f.toHuman());
			badFrames++;
			break;
		case Frame.FRAMETYPE_UNDEFINED:
			Logger.w(TAG,
					"Frame currently not supported, discarded: " + f.toHuman());
			badFrames++;
			break;
		// Analog values
		case Frame.FRAMETYPE_ANALOG:
			// get AD1, AD2 etc from frame
			_sourceChannelMap.get(CHANNEL_ID_AD1).setRaw(f.ad1);
			_sourceChannelMap.get(CHANNEL_ID_AD2).setRaw(f.ad2);
			_sourceChannelMap.get(CHANNEL_ID_RSSIRX).setRaw(f.rssirx);
			_sourceChannelMap.get(CHANNEL_ID_RSSITX).setRaw(f.rssitx);

			if (inBound) {
				_framecountRx++;
				// TODO: replace with models logged channels
				// logger.logCsv(_sourceChannels[CHANNEL_INDEX_AD1],_sourceChannels[CHANNEL_INDEX_AD2]);
				logger.logCsv();
			}
			break;
		case Frame.FRAMETYPE_FRSKY_ALARM:
			// Logger.d(TAG,"handle inbound FrSky alarm");
			if (validCurrentModel()) {
				// don't copy the entire alarm, as that would kill off
				// sourcechannel
				// TODO: Compare to existing
				// TODO: Ask to load into the alarms
				// Alarm a =
				// _currentModel.getFrSkyAlarms().get(aIn.getFrSkyFrameType());
				// aIn.setThreshold(aIn.getThreshold());
				// aIn.setGreaterThan(aIn.getGreaterThan());
				// aIn.setAlarmLevel(aIn.getAlarmLevel());
			}

			if (inBound) {
				_framecountTx++;
				_outGoingWatchdogFlag = false;

				if (_recordingAlarms) {
					// store alarms for future use
					Alarm aIn = new Alarm(f);
					Logger.w(TAG,
							"Adding alarm to the recording buffer, alarm id: "
									+ aIn.getFrSkyFrameType());
					_alarmMap.put(aIn.getFrSkyFrameType(), aIn);
					if (_alarmMap.size() >= 4) {
						Logger.w(TAG, "recording completed");
						_recordingAlarms = false;
						// Update the alarms for the model
						if (_recordingModelId != -1) {
							modelMap.get(_recordingModelId).setFrSkyAlarms(
									_alarmMap);
							saveModel(modelMap.get(_recordingModelId));
						}

						Intent i = new Intent();
						i.setAction(MESSAGE_ALARM_RECORDING_COMPLETE);
						sendBroadcast(i);

						if (_compareAfterRecord) {
							compareAlarms();
						}
					}
				}
			}
			break;
		case Frame.FRAMETYPE_USER_DATA:
			if (inBound) {
				_framecountRx++;
				// hcpl add handling user data frames!!
				Logger.d(TAG, "Frametype User Data");
				// Use menu item Debug to enable hub support
				if (_hubEnabled) {
					// FIXME: below should be changed to something like
					// currentModel.getHub().addUserBytes(frame.getUserBytes)
					FrSkyHub.getInstance().extractUserDataBytes(this, f);

					// FIXME: Temporary to add userbytes on hub
					// FrSkyHub.getInstance().addUserBytes(f.getUserBytes());
					// Logger.d(TAG,"Testing Frame.getUserBytes() on f: "+f.toHuman());
					// String s = "";
					// for(int i: f.getUserBytes())
					// {
					// s = s+Integer.toHexString(i)+" ";
					// }
					// Logger.d(TAG,s);

				}
			}
			break;
		case Frame.FRAMETYPE_INPUT_REQUEST_ALARMS_AD:
			// Log.d(TAG,"Frametype Request all alarms");
			break;
		case Frame.FRAMETYPE_INPUT_REQUEST_ALARMS_RSSI:
			// Log.d(TAG,"Frametype Request all alarms");
			break;
		default:
			Logger.i(TAG, "Frametype currently not supported: " + f.frametype);
			Logger.i(TAG, "Frame: " + f.toHuman());
			break;
		}
		return ok;

	}

	/**
	 * @see #parseFrame(Frame, boolean)
	 */
	public boolean parseFrame(Frame f) {
		return parseFrame(f, true);
	}

	// **************************************************************************************************************
	// TEXT TO SPEECH STUFF
	// **************************************************************************************************************

	/**
	 * Creates a TextToSpeech object
	 * 
	 * @return the TextToSpeech object we will use
	 */
	public TextToSpeech createSpeaker() {
		Logger.i(TAG, "Create Speaker");
		mTts = new TextToSpeech(this, this);
		return mTts;
	}

	/**
	 * Speaks something using default values
	 * 
	 * @param myText
	 *            the text to speak
	 */
	public void saySomething(String myText) {
		Logger.i(TAG, "Speak something");
		mTts.speak(myText, TextToSpeech.QUEUE_FLUSH, null);
		// mTts.speak(myText, TextToSpeech.QUEUE_FLUSH, myAudibleStreamMap);
	}

	/**
	 * Starts the cyclic speaker threads
	 */
	public void startCyclicSpeaker() {
		// Stop it before starting it
		Logger.i(TAG, "Start Cyclic Speaker");
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
	public void stopCyclicSpeaker() {
		Logger.i(TAG, "Stop Cyclic Speaker");
		try {
			speakHandler.removeCallbacks(runnableSpeaker);
			mTts.speak("", TextToSpeech.QUEUE_FLUSH, null);
		} catch (Exception e) {
		}
		_cyclicSpeechEnabled = false;
		Intent i = new Intent();
		i.setAction(MESSAGE_SPEAKERCHANGE);
		sendBroadcast(i);
	}

	// **************************************************************************************************************
	// SIMULATOR STUFF
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
	// HANDLERS AND RECEIVERS
	// **************************************************************************************************************

	/**
	 * Used to detect broadcasts from Bluetooth. Remember to add the message to
	 * the intentfilter (mIntentFilterBt) above
	 */
	private BroadcastReceiver mIntentReceiverBt = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String msg = intent.getAction();
			Logger.d(TAG, "Received Broadcast: " + msg);
			if (msg.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				// does not work?
				int cmd = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
				Logger.i(TAG, "CMD: " + cmd);
				switch (cmd) {
				case BluetoothAdapter.STATE_ON:
					Logger.d(TAG, "Bluetooth state changed to ON");

					if (getBtAutoConnect()) {
						Logger.d(TAG, "Autoconnect requested");
						connect();
					}
					break;
				case BluetoothAdapter.STATE_OFF:
					Logger.d(TAG, "Blueotooth state changed to OFF");
					break;
				default:
					Logger.d(TAG, "No information about " + msg);

				}
			} else if (msg.equals(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED)) {
				Logger.d(TAG, "SCO STATE CHANGED!!!" + msg);
				int scoState = intent.getIntExtra(
						AudioManager.EXTRA_SCO_AUDIO_STATE, -1);
				switch (scoState) {
				case AudioManager.SCO_AUDIO_STATE_CONNECTED:
					Logger.i(TAG, "SCO CONNECTED!!!!");
					// _scoConnected = true;
					break;
				case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
					Logger.i(TAG, "SCO DIS-CONNECTED!!!!");
					// _scoConnected = false;
					break;
				default:
					Logger.e(TAG, "Unhandled state");
					// _scoConnected = false;
					break;
				}

			}

			else {
				Logger.e(TAG, "Unhandled intent: " + msg);

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
					Logger.d(TAG, "BT connected");
					mDroppedFrames = 0;
					// Debug.startMethodTracing("frskydash");
					setConnecting(false);
					statusBt = true;

					_manualBtDisconnect = false;
					// send(Frame.InputRequestAll().toInts());

					// _compareAfterRecord=true;
					recordAlarmsFromModule(true);

					// Dont autosend when connecting, rather autosend when
					// setting currentModel
					// if(getAutoSendAlarms())
					// {
					// for(Alarm a : _currentModel.getFrSkyAlarms().values())
					// {
					// send(a.toFrame());
					// }
					// }

					break;

				case BluetoothSerialService.STATE_CONNECTING:
					Logger.d(TAG, "BT connecting");
					setConnecting(true);
					// mTitle.setText(R.string.title_connecting);
					break;

				case BluetoothSerialService.STATE_LISTEN:
					Logger.d(TAG, "BT listening");
				case BluetoothSerialService.STATE_NONE:
					Logger.d(TAG, "BT state changed to NONE");
					// Toast.makeText(getApplicationContext(), "Disconnected",
					// Toast.LENGTH_SHORT).show();
					setConnecting(false);
					if ((statusBt == true) && (!_dying)
							&& (!_manualBtDisconnect))
						wasDisconnected("Bt"); // Only do disconnect message if
												// previously connected
					statusBt = false;
					// set all the channels to -1
					// Debug.stopMethodTracing();

					logger.stop();
				}
				break;
			case MESSAGE_WRITE:
				// Log.d(TAG,"BT writing");
				break;

			// handle receiving data from frsky
			case MESSAGE_READ:
				// Log.w(TAG,"Received bytes: "+msg.obj);
				if (!_dying) {
					// hcpl updated to handle the new int array after byte per
					// byte read update
					byte[] readBuf = (byte[]) msg.obj;
					// int[] i = new int[msg.arg1];
					// int[] i = (int[])msg.obj;
					handleByteBuffer(readBuf);
				}
				break;

			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				setBtLastConnectedToAddress(_device.getAddress());
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				Logger.d(TAG, "BT connected to...");
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	// **************************************************************************************************************
	// MODEL STUFF
	// **************************************************************************************************************
	/**
	 * Adds new model to the model map (will replace if id is positive and
	 * exists)
	 * 
	 * @param model
	 *            the model to add to the modelstore
	 */
	public static void addModel(Model model) {
		if (model.getId() == -1) {
			// save to get id
			database.saveModel(model);
			// Update channels with new model id
			for (Channel c : model.getChannels().values()) {
				c.setModelId(model.getId());
			}
		}
		modelMap.put(model.getId(), model);
		Intent i = new Intent();
		i.setAction(MESSAGE_MODELMAP_CHANGED);
		context.sendBroadcast(i);

	}

	/**
	 * 
	 * @param model
	 *            the model to delete
	 */
	public static void deleteModel(Model model) {
		// FIXME by using an object instead of an id we introduce the
		// possibility that we are working on the wrong object here!!
		// check if model as param requested to delete isn't the current model.
		if( validCurrentModel() && getCurrentModel().equals(model)){
			// if so we need to change the current model first
			setCurrentModel(modelMap.firstKey());
		}
		// no continue unregistering everything for this model
//		model.unregisterListeners();// already done in setCurrentModel
		model.frSkyAlarms.clear();
		model.getChannels().clear();
		database.deleteAllChannelsForModel(model);
		database.deleteAlarmsForModel(model);
		database.deleteModel(model.getId());
		modelMap.remove(model.getId());
		// broadcast this information
		Intent i = new Intent();
		i.setAction(MESSAGE_MODELMAP_CHANGED);
		context.sendBroadcast(i);
	}
	
	/**
	 * 
	 * @param model
	 *            the model to save
	 */
	public static void saveModel(Model model) {
		// FIXME: What happens if saving a model with -1 id, but with channels?
		Logger.w(TAG, "Saving model [" + model.getName() + "]");
		database.saveModel(model);
	}

	/**
	 * 
	 * @param modelId
	 *            id of the model to save
	 */
	public static void saveModel(int modelId) {
		Logger.w(TAG, "Saving model id [" + modelId + "]");
		saveModel(modelMap.get(modelId));
	}

	/**
	 * Saves a channel
	 * 
	 * @param channel
	 *            the channel to save
	 */
	public static void saveChannel(Channel channel) {
		database.saveChannel(channel);
	}

	/**
	 * Sends a models alarms to the module
	 * 
	 * @param model
	 *            the model to send alarms for
	 */
	public static void sendAlarms(Model model) {
		if (statusRx) {
			for (Alarm a : model.getFrSkyAlarms().values()) {
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
	 * Comment: Please do not use this method to communicate from hub to
	 * consumers
	 * 
	 * @deprecated
	 * 
	 * @param sensorType
	 */
	public void broadcastChannelData(SensorTypes sensorType, double value) {
		broadcastHubDataIntent.putExtra(ActivityHubData.FIELD_SENSORTYPE,
				sensorType.toString());
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

	class FrameParser implements Runnable {
		private final BlockingQueue<Frame> queue;
//		private static final String TAG = "RawLogger";

		FrameParser(BlockingQueue<Frame> q) {
			queue = q;
		}

		public void run() {
			try {
				while (true) {
					consume((Frame) queue.take());
				}
			} catch (InterruptedException ex) {
			}
		}

		void consume(Frame f) {
			parseFrame(f, true);
		}
	}
}