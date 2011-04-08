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
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
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
    public int fps=0;
    
    private Logger logger;
	
	private TextToSpeech mTts;
	private int _speakDelay;
	private Handler speakHandler;
    private Runnable runnableSpeaker;
    
	private Handler fpsHandler;
    private Runnable runnableFps;
    
    private boolean _dying=false;
	
	private final IBinder mBinder = new MyBinder();
	
	private WakeLock wl;
	private boolean _cyclicSpeechEnabled;
	private MyApp globals;
	
	public Simulator sim;
	
	private int MAX_CHANNELS=4;
	private int[] hRaw;
	private int _framecount=0;
	
	private double[] hVal;
	private String[] hName;
	private String[] hDescription;
	private double[] hOffset;
	private double[] hFactor;
	private String[] hUnit;
	private String[] hLongUnit;
	private int channels=0;
	private Channel[] objs;
	
	
	public Channel AD1,AD2,RSSIrx,RSSItx;
	
	

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
    	 PendingIntent contentIntent = PendingIntent.getActivity(this, 0,notificationIntent, 0);
    	notification.setLatestEventInfo(this, "FrSkyDash",text, contentIntent);
    	startForeground(NOTIFICATION_ID,notification);
    }
	
	@Override
	public void onCreate()
	{
		Log.i(TAG,"onCreate");
		super.onCreate();
		logger = new Logger(getApplicationContext(),true,true,true);
		
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		Toast.makeText(this,"Service created at " + time.getTime(), Toast.LENGTH_LONG).show();
		showNotification();		
		
		setupChannels();
		
		Log.i(TAG,"Broadcast that i've started");
		Intent i = new Intent();
		i.setAction(MESSAGE_STARTED);
		sendBroadcast(i);
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		 wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
		 getWakeLock();
		 
		 globals = ((MyApp)getApplicationContext());
		 
		 mSerialService = new BluetoothSerialService(this, mHandlerBT);
		 
		 sim = new Simulator(this);
		 
		 _cyclicSpeechEnabled = false;
		 _speakDelay = 30000;
	        speakHandler = new Handler();
			runnableSpeaker = new Runnable() {
				@Override
				public void run()
				{
					Log.i(TAG,"Cyclic Speak stuff");
					//mTts.speak(globals.AD1.toVoiceString(), TextToSpeech.QUEUE_ADD, null);
					//mTts.speak(globals.AD2.toVoiceString(), TextToSpeech.QUEUE_ADD, null);
					//mTts.speak(globals.RSSItx.toVoiceString(), TextToSpeech.QUEUE_ADD, null);
					//mTts.speak(globals.RSSIrx.toVoiceString(), TextToSpeech.QUEUE_ADD, null);
					
					for(int n=0;n<MAX_CHANNELS;n++)
					{
						if(!getChannelById(n).silent) mTts.speak(getChannelById(n).toVoiceString(), TextToSpeech.QUEUE_ADD, null);
					}
					
					//mTts.speak(AD1.toVoiceString(), TextToSpeech.QUEUE_ADD, null);
					//mTts.speak(AD2.toVoiceString(), TextToSpeech.QUEUE_ADD, null);
					//mTts.speak(RSSItx.toVoiceString(), TextToSpeech.QUEUE_ADD, null);
					//mTts.speak(RSSIrx.toVoiceString(), TextToSpeech.QUEUE_ADD, null);
					
					speakHandler.removeCallbacks(runnableSpeaker);
			    	speakHandler.postDelayed(this, _speakDelay);
				}
			};
			
			fpsHandler = new Handler();
			runnableFps = new Runnable () {
				@Override
				public void run()
				{
					fps = _framecount;
					_framecount = 0;
					//Log.i(TAG,"FPS: "+fps);
					fpsHandler.removeCallbacks(runnableFps);
					fpsHandler.postDelayed(this,1000);
				}
			};
			fpsHandler.postDelayed(runnableFps,1000);
	}
	
	public void send(byte[] out) {
    	mSerialService.write( out );
    }
	public void send(int[] out) {
    	mSerialService.write( out );
    }
	
	public void reConnect()
	{
		//if(getConnectionState()==BluetoothSerialService.)
		mSerialService.connect(_device);
	}

	public void connect(BluetoothDevice device)
	{
		_device = device;
		mSerialService.connect(device);
	}
	
	public void disconnect()
	{
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
		super.onDestroy();
		Log.i(TAG,"onDestroy");
		simStop();
		sim.reset();
		
		Log.i(TAG,"Releasing Wakelock");
		if(wl.isHeld())
		{
			wl.release();
		}
		stopCyclicSpeaker();
		mTts.shutdown();
		mSerialService.stop();
		
		fpsHandler.removeCallbacks(runnableFps);
		
		//AD1.setRaw(0);
		//AD2.setRaw(0);
		//RSSIrx.setRaw(0);
		//RSSItx.setRaw(0);
		resetChannels();
		
		logger.stop();
		
		//stopCyclicSpeaker();
		
		
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
	
	public int createChannel(String name,String description,double offset,double factor,String unit,String longUnit)
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

		int tad1 = createChannel("AD1", "Main cell voltage", 0, (double) 0.1/6, "V","Volt");
		AD1 = getChannelById(tad1);
		AD1.setMovingAverage(8);
		
		int tad2 = createChannel("AD2", "Receiver cell voltage", 0, (double) 0.5, "V","Volt");
		AD2 = getChannelById(tad2);
		AD2.setPrecision(1);
		AD2.setMovingAverage(8);
		
		int trssirx = createChannel("RSSIrx", "Signal strength receiver", 0, 1, "","");
		RSSIrx = getChannelById(trssirx);
		RSSIrx.setPrecision(0);
		RSSIrx.setMovingAverage(-1);
		RSSIrx.silent = true;
		
		int trssitx = createChannel("RSSItx", "Signal strength transmitter", 0, 1, "","");
		RSSItx = getChannelById(trssitx);
		RSSItx.setPrecision(0);
		RSSItx.setMovingAverage(-1);
		RSSItx.silent = true;
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
	
private final Handler mHandlerBT = new Handler() {
    	
        @Override
        public void handleMessage(Message msg) {        	
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothSerialService.STATE_CONNECTED:
                	Log.d(TAG,"BT connected");
                	send(Frame.InputRequestAll().toInts());
//                	if (mMenuItemConnect != null) {
//                		mMenuItemConnect.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
//                		mMenuItemConnect.setTitle(R.string.disconnect);
//                	}
//                	
//                	mInputManager.showSoftInput(mEmulatorView, InputMethodManager.SHOW_IMPLICIT);
//                	
//                    mTitle.setText(R.string.title_connected_to);
//                    mTitle.append(mConnectedDeviceName);
                    
                    break;
                    
                case BluetoothSerialService.STATE_CONNECTING:
                	Log.d(TAG,"BT connecting");
                	 //mTitle.setText(R.string.title_connecting);
                    break;
                    
                case BluetoothSerialService.STATE_LISTEN:
                	Log.d(TAG,"BT listening");
                case BluetoothSerialService.STATE_NONE:
                	Log.d(TAG,"BT state NONE");
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
	            	
	                Log.d(TAG,"BT got frame, length: "+msg.arg1);
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
	                	Log.i(TAG,f.toHuman());
	                	parseFrame(f);
	                }
	            	//Log.d(TAG,readBuf.toString()+":"+msg.arg1);
            	}
                break;
                
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
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
		sim.start();
	}
	
	public void simStop()
	{
		Log.i(TAG,"Sim Stop");
		sim.stop();
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
	
	public boolean parseFrame(Frame f)
	{
		//int [] frame = f.toInts(); 
		boolean ok=true;
		logger.log(f);
		_framecount++;
		switch(f.frametype)
		{
			// Analog values
			case Frame.FRAMETYPE_ANALOG:
				// get AD1, AD2 etc from frame
				AD1.setRaw(f.ad1);
				AD2.setRaw(f.ad2);
				RSSIrx.setRaw(f.rssirx);
				RSSItx.setRaw(f.rssitx);
				break;
			
			default:
				Log.i(TAG,"Frametype currently not supported");
				Log.i(TAG,"Frame: "+f.toHuman());
				break;
		}
		return ok;
	}
	
	public String getFps()
	{
		return Integer.toString(fps);
	}

	

}

