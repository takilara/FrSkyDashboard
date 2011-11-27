package biz.onomato.frskydash;

import java.io.File;
import java.util.Calendar;
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
	
	
	// Things for Bluetooth
	private static final int REQUEST_ENABLE_BT = 2;
	private IntentFilter mIntentFilterBt;
	private boolean bluetoothEnabledAtStart;
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
    public int fps,fpsRx,fpsTx=0;
    
    private FrskyDatabase channelDb;
    
    private Logger logger;
	
	private TextToSpeech mTts;
	private int _speakDelay;
    
	private Handler fpsHandler, watchdogHandler, speakHandler;
    private Runnable runnableFps, runnableSpeaker, runnableWatchdog;
    
    
    
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
	
	private int MAX_CHANNELS=4;
	private int[] hRaw;
	private int _framecount=0;
	private int _framecountRx=0;
	private int _framecountTx=0;
	private boolean _btAutoEnable;
	private boolean _btAutoConnect;
	private int _minimumVolumeLevel;
	private boolean _autoSetVolume;
	
	private double[] hVal;
	private String[] hName;
	private String[] hDescription;
	private double[] hOffset;
	private double[] hFactor;
	private String[] hUnit;
	private String[] hLongUnit;
	private int channels=0;
	private Channel[] objs;
	
	private String _btLastConnectedToAddress;
	
	

	
	
	public Channel AD1,AD2,RSSIrx,RSSItx;
	
	

	public static final String MESSAGE_STARTED = "biz.onomato.frskydash.intent.action.SERVER_STARTED";
	public static final String MESSAGE_SPEAKERCHANGE = "biz.onomato.frskydash.intent.action.SPEAKER_CHANGED";
	
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		Log.i(TAG,"Something tries to bind to me");
		return mBinder;
		//return null;
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
	
	@Override
	public void onCreate()
	{

        
		
		
		Log.i(TAG,"onCreate");
		super.onCreate();
		context = getApplicationContext();
		
		logger = new Logger(getApplicationContext(),true,true,true);
		
		
		
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		Toast.makeText(this,"Service created at " + time.getTime(), Toast.LENGTH_LONG).show();
		
		Log.i(TAG,"Try to load settings");
        _settings = context.getSharedPreferences("FrSkyDash",MODE_PRIVATE);
        _editor = _settings.edit();
        
        // Apply settings from settingsfile
        setSettings();
        
		showNotification();		
		
		///TODO: in setupChannels, if settings exist, use that for setup
		setupChannels();

		AD1.loadFromConfig(_settings);
		AD2.loadFromConfig(_settings);
		logger.setCsvHeader(AD1,AD2);

		
		
		mIntentFilterBt = new IntentFilter();
		mIntentFilterBt.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(mIntentReceiverBt, mIntentFilterBt); // Used to receive BT events
		
		
		Log.i(TAG,"Broadcast that i've started");
		Intent i = new Intent();
		i.setAction(MESSAGE_STARTED);
		sendBroadcast(i);
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		 wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
		 getWakeLock();
		 
		 //globals = ((MyApp)getApplicationContext());
		 
		 //FrskyDatabase database = new FrskyDatabase(globals);
		 //database.getChannel(0);
		 
		 
		 
		 mSerialService = new BluetoothSerialService(this, mHandlerBT);
		 
		 sim = new Simulator(this);
		 
		 _cyclicSpeechEnabled = false;
		 _speakDelay = 30000;
		
		 
		 
        speakHandler = new Handler();
		runnableSpeaker = new Runnable() {
			//@Override
			public void run()
			{
				Log.i(TAG,"Cyclic Speak stuff");
			
				for(int n=0;n<MAX_CHANNELS;n++)
				{
					if(!getChannelById(n).silent) mTts.speak(getChannelById(n).toVoiceString(), TextToSpeech.QUEUE_ADD, null);
				}
				
				speakHandler.removeCallbacks(runnableSpeaker);
		    	speakHandler.postDelayed(this, _speakDelay);
			}
		};
		
		fpsHandler = new Handler();
		runnableFps = new Runnable () {
			//@Override
			public void run()
			{
				fps = _framecount;
				fpsRx = _framecountRx;
				fpsTx = _framecountTx;
				
				
				if(fpsRx>0)	// receiving frames from Rx, means Tx comms is up as well 
				{
					statusRx = true;
					statusTx = true;
				}
				else		// not receiving frames from Rx
				{
					// make sure user knows if state changed from ok to not ok
					if(statusRx==true)
					{
						wasDisconnected();
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
	
	/**
	 * Set time between reads
	 * @param interval seconds between reads
	 */
	public void setCyclicSpeachInterval(int interval)
	{
		Log.i(TAG,"Set new interval to "+interval+" seconds");
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

	public void connect(BluetoothDevice device)
	{
		logger.stop();
		_device = device;
		mSerialService.connect(device);
	}
	
	 public void connect()	// connect to previous device
	 {
    	if(mBluetoothAdapter.isEnabled()) // only connect if adapter is enabled
        {
	       	if(_btLastConnectedToAddress!="")
	       	{
	       		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(_btLastConnectedToAddress);
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
		simStop();
		//sim.reset();
		
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
		
		//AD1.setRaw(0);
		//AD2.setRaw(0);
		//RSSIrx.setRaw(0);
		//RSSItx.setRaw(0);
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
        	
        	setCyclicSpeech(_settings.getBoolean("cyclicSpeakerEnabledAtStartup",false));
        	
    	}
    	} else {
    	// Initialization failed.
    	Log.i(TAG,"Something wrong with TTS");
    	Log.e(TAG, "Could not initialize TextToSpeech.");
    	}
    }
	
	public int createChannel(String name,String description,float offset,float factor,String unit,String longUnit)
	{
		Channel AD1 =  new Channel(name, description, offset, factor, unit, longUnit);
		objs[channels] = AD1;
		Log.i("MyApp","createChannel");
		hRaw[channels]=-1;
		hVal[channels]=-1;
		hName[channels]=name;
		hDescription[channels]=description;
		hOffset[channels]=offset;
		hFactor[channels]=factor;
		hUnit[channels]=unit;
		hLongUnit[channels]=longUnit;
		channels += 1;
		return channels-1;
	}
	

	
	private void setupChannels()
	{
		hRaw = new int[MAX_CHANNELS];
		hVal = new double[MAX_CHANNELS];
		hName = new String[MAX_CHANNELS];
		hDescription = new String[MAX_CHANNELS];
		hOffset = new double[MAX_CHANNELS];
		hFactor = new double[MAX_CHANNELS];
		hUnit = new String[MAX_CHANNELS];
		hLongUnit = new String[MAX_CHANNELS];
		objs = new Channel[MAX_CHANNELS];
		
		
        
		
		// hardcoded
		// should be set up empty, contents filled from config
		int tad1 = createChannel("AD1", "Default description", 0, (float) 1, "V","Volt");
		AD1 = getChannelById(tad1);
		//AD1.setMovingAverage(8);

        // From config
//		int tad1 = createChannel(cName, cDescription, cOffset, (double) cFactor, cShortUnit,cLongUnit);
//		AD1 = getChannelById(tad1);
//		AD1.setMovingAverage(cMovingAverage);
//		AD1.setPrecision(cPrecision);
//		AD1.silent = cSilent;
		
		
		int tad2 = createChannel("AD2", "Default description", 0, (float) 1, "V","Volt");
		AD2 = getChannelById(tad2);
		//AD2.setPrecision(1);
		//AD2.setMovingAverage(8);
		//AD2.silent = true;
		
		int trssirx = createChannel("RSSIrx", "Signal strength receiver", 0, 1, "","");
		RSSIrx = getChannelById(trssirx);
		RSSIrx.setPrecision(0);
		RSSIrx.setMovingAverage(-1);
		RSSIrx.setLongUnit("dBm");
		RSSIrx.setShortUnit("dBm");
		RSSIrx.silent = true;
		
		int trssitx = createChannel("RSSItx", "Signal strength transmitter", 0, 1, "","");
		RSSItx = getChannelById(trssitx);
		RSSItx.setPrecision(0);
		RSSItx.setMovingAverage(-1);
		RSSItx.setLongUnit("dBm");
		RSSItx.setShortUnit("dBm");
		RSSItx.silent = true;
		
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
	
	private void resetChannels()
	{
		for(int n=0;n<MAX_CHANNELS;n++)
		{
			getChannelById(n).setRaw(0);
		}
	}
	
	public Channel getChannelById(int id)
	{
		return objs[id];
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

	public void wasDisconnected()
	{
		AD1.reset();
    	AD2.reset();
    	RSSItx.reset();
    	RSSIrx.reset();
    	
    	// speak warning
    	saySomething("Alarm! Alarm! Alarm! Connection Lost!");
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
                	statusBt = true;
                	
                	_manualBtDisconnect = false;
                	send(Frame.InputRequestAll().toInts());
                    
                    break;
                    
                case BluetoothSerialService.STATE_CONNECTING:
                	Log.d(TAG,"BT connecting");
                	 //mTitle.setText(R.string.title_connecting);
                    break;
                    
                case BluetoothSerialService.STATE_LISTEN:
                	Log.d(TAG,"BT listening");
                case BluetoothSerialService.STATE_NONE:
                	Log.d(TAG,"BT state changed to NONE");
                	//Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                	
                	if((statusBt==true) && (!_dying) && (!_manualBtDisconnect)) wasDisconnected();	// Only do disconnect message if previously connected
                	statusBt = false;
                	// set all the channels to -1
                	
                	
                	logger.stop();
//                	if (mMenuItemConnect != null) {
//                		mMenuItemConnect.setIcon(android.R.drawable.ic_menu_search);
//                		mMenuItemConnect.setTitle(R.string.connect);
//                	}
//
//            		mInputManager.hideSoftInputFromWindow(mEmulatorView.getWindowToken(), 0);
//                	
//                    mTitle.setText(R.string.title_not_connected);                    break;
                }
                break;
            case MESSAGE_WRITE:
            	Log.d(TAG,"BT writing");
//            	if (mLocalEcho) {
//            		byte[] writeBuf = (byte[]) msg.obj;
//            		mEmulatorView.write(writeBuf, msg.arg1);
//            	}
                
                break;
                
            case MESSAGE_READ:
            	if(!_dying)
            	{
	                byte[] readBuf = (byte[]) msg.obj;              
	                //mEmulatorView.write(readBuf, msg.arg1);
	            	
	                //Log.d(TAG,"BT got frame, length: "+msg.arg1);
	                //for(int n=0;n<readBuf.length;n++)
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
                _btLastConnectedToAddress = _device.getAddress();
                _editor.putString("btLastConnectedToAddress", _btLastConnectedToAddress);
                _editor.commit();
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

	public boolean getCyclicSpeechEnabled()
	{
		return _cyclicSpeechEnabled;
	}
	
	public void setCyclicSpeech(boolean state)
	{
		Log.i(TAG,"Setting Cyclic speech to: "+state);
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
				AD1.setRaw(f.ad1);
				AD2.setRaw(f.ad2);
				RSSIrx.setRaw(f.rssirx);
				RSSItx.setRaw(f.rssitx);
				if(inBound)	
				{
					_framecountRx++;

					
					//logger.logCsv(ad1Raw,ad1RawAvg,ad1Eng,ad1EngAvg,ad2Raw,ad2RawAvg,ad2Eng,ad2EngAvg);
					logger.logCsv(AD1,AD2);
				}
				break;
			case Frame.FRAMETYPE_FRSKY_ALARM:
				Log.d(TAG,"handle inbound FrSky alarm");
				if(inBound)	_framecountTx++;
				switch(f.alarmChannel)
				{
				case Channel.CHANNELTYPE_AD1:
					AD1.setFrSkyAlarm(f.alarmNumber, f.alarmThreshold, f.alarmGreaterThan, f.alarmLevel);
					break;
				case Channel.CHANNELTYPE_AD2:
					AD2.setFrSkyAlarm(f.alarmNumber, f.alarmThreshold, f.alarmGreaterThan, f.alarmLevel);
					break;
				case Channel.CHANNELTYPE_RSSI:
					RSSItx.setFrSkyAlarm(f.alarmNumber, f.alarmThreshold, f.alarmGreaterThan, f.alarmLevel);
					break;
				default:
					Log.i(TAG,"Unsupported FrSky alarm?");
					Log.i(TAG,"Frame: "+f.toHuman());
				}
				
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
	
	public void setLogToRaw(boolean logToRaw)
	{
		Log.i(TAG,"Log to Raw:"+logToRaw);
		logger.setLogToRaw(logToRaw);
	}
	
	public void setLogToHuman(boolean logToHuman)
	{
		Log.i(TAG,"Log to Human:"+logToHuman);
		logger.setLogToHuman(logToHuman);
	}
	
	public void setLogToCsv(boolean logToCsv)
	{
		Log.i(TAG,"Log to Csv:"+logToCsv);
		logger.setLogToCsv(logToCsv);
	}
	
	
	// Settings setters and getters
	///TODO: Have setters and getters work with settings store
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
		//return _minimumVolumeLevel;
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
	
	public boolean setChannelConfiguration(SharedPreferences settings,Channel channel)
	{
		
      
		String cDescription,cLongUnit,cShortUnit,cName;
		float cFactor,cOffset;
		int cMovingAverage,cPrecision;
		boolean cSilent;
      
		cName=channel.getName();
		channel.setDescription(settings.getString(cName+"_"+"Description","Main cell voltage"));
		channel.setLongUnit(cLongUnit = settings.getString(cName+"_"+"LongUnit","Volt"));
		channel.setShortUnit(cShortUnit = settings.getString(cName+"_"+"ShortUnit","V"));
		channel.setFactor(cFactor = settings.getFloat(cName+"_"+"Factor", (float) (0.1/6)));
		channel.setOffset(settings.getFloat(cName+"_"+"Offset", (float) (0)));
		channel.setMovingAverage(cMovingAverage = settings.getInt(cName+"_"+"MovingAverage", 8));
		channel.setPrecision(settings.getInt(cName+"_"+"Precision", 2));
		channel.silent = settings.getBoolean(cName+"_"+"Silent", false);
		return true;
	}
	
	//public void setSettings(SharedPreferences settings)
	public void setSettings()
	///TODO: remove entries here when setters/getters do this work
	
	{
//		_settings = settings;
//		editor = _settings.edit();
//		setCyclicSpeech(settings.getBoolean("cyclicSpeakerEnabledAtStartup",false));
		setLogToRaw(_settings.getBoolean("logToRaw",false));
		setLogToHuman(_settings.getBoolean("logToHuman",false));
		setLogToCsv(_settings.getBoolean("logToCsv",false));
		setCyclicSpeachInterval(_settings.getInt("cyclicSpeakerInterval",30));
		_btLastConnectedToAddress = _settings.getString("btLastConnectedToAddress","");
		
		
      
//      AD1.loadFromConfig(settings);
//      AD2.loadFromConfig(settings);
//      
//
//      
//      logger.setCsvHeader(AD1,AD2);
     
	}
	
	public SharedPreferences getSettings()
	{
		return _settings;
	}
	
	public void saveChannelConfig(Channel channel)
	{
		Log.i(TAG,"Save channel '"+channel+"'");
		
		// Update channelconfig database
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

