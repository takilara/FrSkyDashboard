package biz.onomato.frskydash;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class Channel implements OnChannelListener, Parcelable  {
	private static final String TAG = "Channel";
	
	public static final int CHANNELTYPE_AD1=0;
	public static final int CHANNELTYPE_AD2=1;
	public static final int CHANNELTYPE_RSSI=2;
	public static final String MESSAGE_CHANNEL_UPDATED = "biz.onomato.frskydash.update.channel.";
	//public static final String crlf="\r\n";
	public static final String delim=";";
	
	private static final boolean DEBUG=true;
	
	public double raw,rawAvg;
	public double eng,engAvg;
	private double _raw;
	
	public double rounder;
	
	private double _val;
	private double _avg;
	private String _name;
	private String _description;
	private float _offset;
	private float _factor;
	private int _precision;
	private Context _context;
	
	private IntentFilter mIntentFilter;
	
	private String _shortUnit;
	private String _longUnit;
	private int _movingAverage;
	private MathContext _mc;
	public boolean silent;
	public Date timestamp;
	
	public Alarm[] alarms;
	public int alarmCount = 0;
	private long _modelId = -1;
	private long _channelId = -1;
	
	private ArrayList <OnChannelListener> _listeners;

	//public static final Channel AD1 = new Channel("ad1","FrSky AD1",(float)0,(float)1,"","");
	//public static final Channel AD2 = new Channel("ad2","FrSky AD2",(float)0,(float)1,"","");
	
	private MyStack _stack;
	SharedPreferences _settings;
	SharedPreferences.Editor editor;

	public Channel()
	{
		this("derived","description",(float)0,(float)1,"Symbol","UnitName");
	}
	
	public Channel(Parcel in)
	{
		readFromParcel(in);
	}
	
	
	public Channel(String name,String description,float offset,float factor,String unit,String longUnit)
	{
		// instanciate listeners list
		_listeners = new ArrayList<OnChannelListener> ();

		rounder=1;
		silent = false;
		_precision = 2;
		
		_name = name;
		_description = description;
		_offset = offset;
		_factor = factor;
		_shortUnit = unit;
		_longUnit = longUnit;
		_mc = new MathContext(2);
		setMovingAverage(0);

//		_movingAverage = 10;
//		_raw=-1;
//		_val=-1;
//		_avg=0;
//		_stack = new MyStack(10);
//		
		reset();
		
		// FRSKY channels only for now
		alarms = new Alarm[2];
	}
	
	public void addListener(OnChannelListener channel)
	{
		_listeners.add(channel);
	}
	
	public void setContext(Context context)
	{
		Log.d(TAG,"Channel '"+_name+"' Set context to:"+context);
		_context = context;
		
	}
	
	public void setId(long channelId)
	{
		
		_channelId = channelId;
	}
	
	public void listenTo(int channelId)
	{
		mIntentFilter = new IntentFilter();
		String bCastAction = MESSAGE_CHANNEL_UPDATED+channelId;
		Log.d(TAG,"Listens for broadcast of values to on context "+_context+", with message: "+bCastAction);
		
	    mIntentFilter.addAction(bCastAction);
	    Log.d(TAG,"Context is : "+_context);
	    _context.registerReceiver(mChannelUpdateReceiver, mIntentFilter);	  // Used to receive messages from Server
	}
	
	public void reset()
	{
		_raw = -1;
		_val = -1;
		_avg = 0;
		raw = _raw;
		rawAvg = _avg;
		eng = _val;
		_stack = new MyStack(_movingAverage);
	}
	
	public void setModelId(Model model)
	{
		_modelId = model.getId();
	}
	public void setModelId(long modelId)
	{
		_modelId = modelId;
	}
	
	public long getModelId()
	{
		return _modelId;
	}
	
	public boolean loadFromConfig(SharedPreferences settings)
	{

		setDescription(settings.getString(_name+"_"+"Description","Main cell voltage"));
		setLongUnit(settings.getString(_name+"_"+"LongUnit","Volt"));
		setShortUnit(settings.getString(_name+"_"+"ShortUnit","V"));
		setFactor(settings.getFloat(_name+"_"+"Factor", (float)(0.1/6)));
		setOffset(settings.getFloat(_name+"_"+"Offset", (0)));
		setMovingAverage(settings.getInt(_name+"_"+"MovingAverage", 8));
		setPrecision(settings.getInt(_name+"_"+"Precision", 2));
		silent = settings.getBoolean(_name+"_"+"Silent", false);
		
		return true;
	}
	
	public boolean saveToConfig(SharedPreferences settings)
	{
		editor = settings.edit();
		
		editor.putString(_name+"_"+"Description", getDescription());
		editor.putString(_name+"_"+"LongUnit", getLongUnit());
		editor.putString(_name+"_"+"ShortUnit", getShortUnit());
		editor.putFloat (_name+"_"+"Factor", getFactor());
		editor.putFloat (_name+"_"+"Offset", getOffset());
		editor.putInt(_name+"_"+"MovingAverage", getMovingAverage());
		editor.putInt(_name+"_"+"Precision", getPrecision());
		editor.putBoolean(_name+"_"+"Silent", silent);
		
		editor.commit();
		
		return true;
	}
	
	public void setMovingAverage(int Size)
	{
		if(Size<1)
		{
			Size = 1;
		}
		_stack = new MyStack(Size);
		_movingAverage = Size;
	}
	
	public double setRaw(int value)
	{
		return setRaw((double) value);
	}
	
	public double setRaw(double value)
	{
		timestamp = new Date();
		_avg = _stack.push(value);

		
		Log.d(TAG,_name+" setting new input value to "+value);
		_raw = value;
		//_val = (_avg * _factor)+_offset;
		_val = convert(_avg);
		double outVal =getValue(true);
		Log.d(TAG,_name+" new outValue should now be "+outVal);
		
		// send new avg value to listeners
		for(OnChannelListener ch : _listeners)
		{
			Log.d(TAG,"\t"+_name+" send to listener");
			ch.onSourceUpdate(outVal);
		}
		
		// send to broadcast receivers
		if((_context!=null) && (_channelId!=-1))
		{
			String bCastAction = MESSAGE_CHANNEL_UPDATED+_channelId;
			Log.d(TAG,"Send broadcast of value to ANY listener on context "+_context+", using message: "+bCastAction);
			
			Intent i = new Intent();
			i.setAction(bCastAction);
			i.putExtra("channelValue", outVal);
			_context.sendBroadcast(i);
		}
		return outVal;
	}
	
	public void onSourceUpdate(double sourceValue)
	{
		
		double v = setRaw(sourceValue);
		Log.d(TAG,_name+" updated by parent to "+sourceValue+" -> "+v+" "+_shortUnit);
	}
	
	private double convert(double inputValue)
	{
		double o = (inputValue * _factor)+_offset;
		Log.d(TAG,_name+" convert from inputvalue ("+inputValue+") to outputvalue ("+o+")");
		return Math.round(o*rounder)/rounder;
	}
	
	public void setPrecision(int precision)
	{
		_precision = precision;
		rounder = 1;
		for(int i=0;i<precision;i++)
		{
			rounder = rounder * 10;
		}
	}
	
	
	// Getters
	public double getValue()
	{
		return getValue(false);
	}
	
	public double getValue(boolean average)
	{
		Log.i(TAG, _name+" Try to calculate new outValue");
		double tVal;
		if(average)
		{
			//tVal = Math.round(_val*100f)/100f;
			return convert(_avg);
			
		}
		else
		{
			return convert(_raw);
		}
		//return getValue(_avg);
	}
	
	public double getRaw()
	{
		return getRaw(false);
	}
	public double getRaw(boolean average)
	{
		if(average)
		{
			return _avg;
		}
		else
		{
			return _raw;
		}
	}
	
	
	
	public String toString()
	{
		return String.format("%."+_precision+"f", _val);
	}

	public String toString(int inputValue)
	{
		return String.format("%."+_precision+"f", convert(inputValue));
	}
	
	//return String.format("%."+_precision+"f",(inputValue*_factor)+_offset);
	
	public String toEng()
	{
		//return String.format("%s %s", getValue(),_shortUnit);
		return toEng(_avg,false);
	}
	
	public String toEng(boolean longUnit)
	{
		//return String.format("%s %s", getValue(),_shortUnit);
		return toEng(_avg,longUnit);
	}
	

	public String toEng(double inputValue)
	{
		//return String.format("%s %s", getValue(inputValue),_shortUnit);
		return toEng(inputValue,false);
	}
	
	public String toEng(double inputValue,boolean longUnit)
	{
		if(longUnit==true)
		{
			return String.format("%."+_precision+"f %s", convert(inputValue),getLongUnit());
		}
		else
		{
			return String.format("%."+_precision+"f %s", convert(inputValue),_shortUnit);
		}
	}

	
	
	
	
	
	public String getDescription()
	{
		return _description;
	}
	
	public void setDescription(String d)
	{
		_description = d;
	}
	
	public String getName()
	{
		return _name;
	}
	public void setName(String n)
	{
		_name = n;
	}
	
	public String getLongUnit()
	{
		return _longUnit;
	}
	
	public void setLongUnit(String unit)
	{
		_longUnit = unit;
	}
	
	public String getShortUnit()
	{
		return _shortUnit;
	}
	
	public void setShortUnit(String unit)
	{
		_shortUnit = unit;
	}
	
	public String toVoiceString()
	{
		return getDescription()+": "+toString()+" "+getLongUnit();
	}
	
	public float getOffset()
	{
		return _offset;
	}
	public void setOffset(float o)
	{
		_offset = o;
	}	
	
	public float getFactor()
	{
		return _factor;
	}	
	public void setFactor(float f)
	{
		_factor = f;
	}	
	
	public int getPrecision()
	{
		return _precision;
	}
	
	public int getMovingAverage()
	{
		return _movingAverage;
	}
	
	public boolean getSpeechEnabled()
	{
		return !silent;
	}

	public void setSpeechEnabled(boolean speech)
	{
		silent = !speech;
	}
	
	public void setFrSkyAlarm(int number,int threshold,int greaterthan,int level)
	{
		alarms[number] = new Alarm(Alarm.ALARMTYPE_FRSKY,level,greaterthan,threshold);
		alarmCount += 1;
	}
	
	public String toCsv()
	{
		
		StringBuilder sb = new StringBuilder();
		
		//sb.append(_description+delim);
		//sb.append(_raw+delim);
		//sb.append(_avg+delim);
		sb.append(String.format("%."+_precision+"f",convert(_raw))+delim);
		sb.append(String.format("%."+_precision+"f",convert(_avg))+delim);
		//sb.append(_longUnit+delim);
		return sb.toString();
	}
	public String toCsvHeader()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%s (%s)",_description,_longUnit)+delim);
		sb.append(String.format("%s (Averaged) (%s)",_description,_longUnit)+delim);
		
		return sb.toString();
	}
	
	// Parcelable
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
 
		// We just need to write each field into the
		// parcel. When we read from parcel, they
		// will come back in the same order
		
//		editor.putString(_name+"_"+"Description", getDescription());
//		editor.putString(_name+"_"+"LongUnit", getLongUnit());
//		editor.putString(_name+"_"+"ShortUnit", getShortUnit());
//		editor.putFloat (_name+"_"+"Factor", getFactor());
//		editor.putFloat (_name+"_"+"Offset", getOffset());
//		editor.putInt(_name+"_"+"MovingAverage", getMovingAverage());
//		editor.putInt(_name+"_"+"Precision", getPrecision());
//		editor.putBoolean(_name+"_"+"Silent", silent);
//		
		dest.writeString(_name);
		dest.writeString(_description);
		dest.writeString(_longUnit);
		dest.writeString(_shortUnit);
		dest.writeFloat(_factor);
		dest.writeFloat(_offset);
		dest.writeInt(_movingAverage);
		dest.writeInt(_precision);
		//dest.writeBool(silent);
		
		
		//dest.writeInt(intValue);
	}
 

	private void readFromParcel(Parcel in) {
 
		// We just need to read back each
		// field in the order that it was
		// written to the parcel
		_name = in.readString();
		_description = in.readString();
		_longUnit = in.readString();
		_shortUnit = in.readString();
		_factor = in.readFloat();
		_offset = in.readFloat();
		_movingAverage = in.readInt();
		_precision = in.readInt();
	}
	
	public static final Parcelable.Creator CREATOR =
	    	new Parcelable.Creator() {
	            public Channel createFromParcel(Parcel in) {
	                return new Channel(in);
	            }
	 
	            public Channel[] newArray(int size) {
	                return new Channel[size];
	            }
	        };

    private BroadcastReceiver mChannelUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	String msg = intent.getAction();
        	Bundle extras = intent.getExtras();
        	Log.i(TAG,"Received Broadcast: '"+msg+"'");
        	// no purpose to compare msg, since we should only listen to relevant broadcasts..
        	//Log.i(TAG,"Comparing '"+msg+"' to '"+FrSkyServer.MESSAGE_SPEAKERCHANGE+"'");
        	//if(msg.equals(FrSkyServer.MESSAGE_STARTED))
        	//	Log.i(TAG,"I have received BroadCast that the server has started");
        	// Get the value..
        	double val = intent.getDoubleExtra("channelValue", -1);
        	if(DEBUG) Log.d(TAG,"Received input value "+val);
        	double v = setRaw(val);
    		Log.d(TAG,_name+" updated by parent to "+val+" -> "+v+" "+_shortUnit);

        }
    };	 
}
