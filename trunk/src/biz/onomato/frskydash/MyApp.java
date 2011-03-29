package biz.onomato.frskydash;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import java.util.HashMap;
import java.util.Locale;





public class MyApp extends Application implements OnInitListener {
	
	private int MAX_CHANNELS=4;
	private int[] hRaw;
	
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
	
	private static final String TAG="Application";
	
	private TextToSpeech mTts;
    private Handler speakHandler;
    private Runnable runnableSpeaker;
    private int _speakDelay;
    private boolean _cyclicSpeechEnabled;
    
    public Simulator sim;
    
    
    PowerManager.WakeLock wl;

	
	
	public MyApp(){
		Log.i(TAG,"Creator");
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
		
		int tad2 = createChannel("AD2", "Receiver cell voltage", 0, (double) 0.5, "V","Volt");
		AD2 = getChannelById(tad2);
		AD2.setPrecision(1);
		
		int trssirx = createChannel("RSSIrx", "Signal strength receiver", 0, 1, "","");
		RSSIrx = getChannelById(trssirx);
		RSSIrx.setPrecision(0);
		
		int trssitx = createChannel("RSSItx", "Signal strength transmitter", 0, 1, "","");
		RSSItx = getChannelById(trssitx);
		RSSItx.setPrecision(0);
		_cyclicSpeechEnabled = false;
		
		sim = new Simulator(this);
		
        // launch simulator service
        //Intent svc = new Intent(this, SimulatorService.class);
        //startService(svc);
		
		// Cyclic speak stuff
		_speakDelay = 30000;
        speakHandler = new Handler();
		runnableSpeaker = new Runnable() {
			@Override
			public void run()
			{
				Log.i(TAG,"Cyclic Speak stuff");
				mTts.speak(AD1.toVoiceString(), TextToSpeech.QUEUE_ADD, null);
				mTts.speak(AD2.toVoiceString(), TextToSpeech.QUEUE_ADD, null);
				mTts.speak(RSSItx.toVoiceString(), TextToSpeech.QUEUE_ADD, null);
				mTts.speak(RSSIrx.toVoiceString(), TextToSpeech.QUEUE_ADD, null);
				
				speakHandler.removeCallbacks(runnableSpeaker);
		    	speakHandler.postDelayed(this, _speakDelay);
			}
		};

		
	}

	@Override
	public void onCreate()
	{
		Log.i(TAG,"onCreate");
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		 wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
		 
		 //wl.acquire();
		 getWakeLock();
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
	
	public TextToSpeech createSpeaker()
	{
		mTts = new TextToSpeech(this, this);
		return mTts;
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
    		String myGreeting = "Application has enabled Text to Speech";
        	mTts.speak(myGreeting,TextToSpeech.QUEUE_FLUSH,null);
    	}
    	} else {
    	// Initialization failed.
    	Log.i(TAG,"Something wrong with TTS");
    	Log.e(TAG, "Could not initialize TextToSpeech.");
    	}
    }
	
	public void saySomething(String myText)
	{
		mTts.speak(myText, TextToSpeech.QUEUE_FLUSH, null);
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
	
	public Channel getChannelById(int id)
	{
		return objs[id];
	}
	
	
	public double setChannelById(int id,int rawVal)
	{
		objs[id].setRaw(rawVal);
		
		Log.i("MyApp","Set channel to some value");
		hRaw[id] = rawVal;
		hVal[id]=hRaw[id]*hFactor[id]+hOffset[id];
		return (double) (hVal[id]);
	}
	
	public boolean parseFrame(int[] frame)
	{
		boolean ok=true;
		
		
		switch(frame[1])
		{
			// Analog values
			case 0xfe:
				ok = parseAnalogFrame(frame);
				break;
			
			default:
				ok=false;
				break;
		}
		return ok;
	}
	
	// we know an analog frame to contain AD1,AD2,RSSItx and RSSIrx,
	// therefore we can use the globals for these.
	
	public boolean parseAnalogFrame(int[] frame)
	{
		//Log.i("Globals","Parse analog frame");
		boolean ok=true;
		int ad1,ad2 = -1;
		int rssirx,rssitx=-1;
		
		// only do bytestuff decoding if neccessary
		if(frame.length>11)
		{
			frame = frameDecode(frame);
		}
		AD1.setRaw(frame[2]);
		AD2.setRaw(frame[3]);
		RSSIrx.setRaw(frame[4]);
		RSSItx.setRaw((int) frame[5]/2);

		
		return ok;
	}
	
	public int[] frameDecode(int[] frame)
	{
		if(frame.length>11)
		{
			int[] outFrame = new int[11];
			
			outFrame[0] = frame[0];
			outFrame[1] = frame[1];
			int xor = 0x00;
			int i = 2;
			
			for(int n=2;n<frame.length;n++)
			{
				if(frame[n]!=0x7d)
				{
					outFrame[i] = frame[n]^xor;
					i++;
					xor = 0x00;
				}
				else
				{
					xor = 0x20;
				}
			}
			
			Log.i("FRAME decode","Pre:  "+frameToHuman(frame));
			Log.i("FRAME decode","Post: "+frameToHuman(outFrame));
			
			return outFrame;
		}
		else
		{
			return frame;
		}	
	}
	
	public String frameToHuman(int[] frame)
	{
		StringBuffer buf = new StringBuffer();
//		byte[] inB = new byte[in.length()];
//		char[] inC = in.toCharArray();
//		inB = in.getBytes();
		for(int n=0;n<frame.length;n++)
		{
			String hex = Integer.toHexString(frame[n]);
			// Need to append in case it returns 0xf etc
			if(hex.length()==1)
			{
				buf.append('0');
			}
			buf.append(hex);
			buf.append(' ');
		}
		String out = buf.toString();
		return out;
	}
	    
	// perform any cleanup
	public void die()
	{
		Log.i(TAG,"Shutting Down");
		Log.i(TAG,"Releasing Wakelock");
		if(wl.isHeld())
		{
			wl.release();
		}
		AD1.setRaw(0);
		AD2.setRaw(0);
		RSSIrx.setRaw(0);
		RSSItx.setRaw(0);
		
		
		sim.reset();
		stopCyclicSpeaker();
		sim.stop();
		mTts.shutdown();
	}

}

