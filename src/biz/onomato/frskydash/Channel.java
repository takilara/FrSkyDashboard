package biz.onomato.frskydash;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;


public class Channel implements Parcelable, Comparator<Channel>  {
	private static final String TAG = "Channel";
	
	
	
	public static final int CHANNELTYPE_AD1=0;
	public static final int CHANNELTYPE_AD2=1;
	public static final int CHANNELTYPE_RSSI=2;
	public static final String MESSAGE_CHANNEL_UPDATED = "biz.onomato.frskydash.update.channel.";
	//public static final String crlf="\r\n";
	public static final String delim=";";
	
	
	public boolean listening = false;
	
	
	
	public double raw,rawAvg;
	public double eng,engAvg;
	private double _raw;
	
	public double rounder;
	
	private double _val;
	//private long _id;
	private double _avg;
	//private String _name;
	private String _description;
	private long _sourceChannelId;
	private float _offset;
	private float _factor;
	private int _precision;
	//private Context _context;
	private int _textViewId=-1;
	
	private IntentFilter mIntentFilter;
	
	private String _shortUnit;
	private String _longUnit;
	private int _movingAverage;
	public boolean _silent;
	public Date timestamp;
	
//	public Alarm[] alarms;
//	public int alarmCount = 0;
	private int _modelId = -1;
	private int _channelId = -1;
	private boolean _dirty = false;
	
//	private static DBAdapterChannel db;
	
	//private ArrayList <OnChannelListener> _listeners;

	//public static final Channel AD1 = new Channel("ad1","FrSky AD1",(float)0,(float)1,"","");
	//public static final Channel AD2 = new Channel("ad2","FrSky AD2",(float)0,(float)1,"","");
	
	private MyStack _stack;
	//SharedPreferences _settings;
	//SharedPreferences.Editor editor;

	public Channel()
	{
		this("description",(float)0,(float)1,"Symbol","UnitName");
	}
	
	public Channel(Parcel in)
	{
		readFromParcel(in);
		//_context = FrSkyServer.getContext();
	}
	
	
	
	public Channel(String description,float offset,float factor,String unit,String longUnit)
	{
		// instanciate listeners list
		//_listeners = new ArrayList<OnChannelListener> ();
		_sourceChannelId = -1;
		rounder=1;
		_silent = false;
		_precision = 2;
		
		//_name = name;
		_description = description;
		_offset = offset;
		_factor = factor;
		_shortUnit = unit;
		_longUnit = longUnit;
		//_mc = new MathContext(2);
		//_context = context;
		//_context = FrSkyServer.getContext();
		setMovingAverage(0);
		
		setDirtyFlag(true);

//		_movingAverage = 10;
//		_raw=-1;
//		_val=-1;
//		_avg=0;
//		_stack = new MyStack(10);
//		
		reset();
		
		// FRSKY channels only for now
//		alarms = new Alarm[2];
		
		//db = new DBAdapterChannel(context);
	}
	
	
	
	// ==========================================================================================
	// ====                        PROPERTIES                                               =====
	// ==========================================================================================
	
	public void setTextViewId(int id)
	{
		_textViewId= id;
	}
	
	public int getTextViewId()
	{
		return _textViewId;
	}
	
	public void setDirtyFlag(boolean dirty)
	{
		_dirty=dirty;
	}
	
	public boolean getDirtyFlag()
	{
		return _dirty;
	}
	
	public void setId(int channelId)
	{
		_channelId = channelId;
		setDirtyFlag(true);
	}
	public int getId()
	{
		return _channelId;
	}
	
	public void setModelId(Model model)
	{
		_modelId = model.getId();
		setDirtyFlag(true);
	}
	public void setModelId(int modelId)
	{
		_modelId = modelId;
		setDirtyFlag(true);
	}
	
	public int getModelId()
	{
		return _modelId;
	}
	public long getSourceChannelId()
	{
		return _sourceChannelId;
	}
	
	public int getMovingAverage()
	{
		return _movingAverage;
	}
	
	public void setMovingAverage(int Size)
	{
		if(Size<1)
		{
			Size = 1;
		}
		_stack = new MyStack(Size);
		_movingAverage = Size;
		setDirtyFlag(true);
	}
	
	public String getDescription()
	{
		return _description;
	}
	
	public void setDescription(String d)
	{
		_description = d;
		setDirtyFlag(true);
	}
	
//	public String getName()
//	{
//		return _name;
//	}
//	public void setName(String n)
//	{
//		_name = n;
//		setDirtyFlag(true);
//	}
	
	public String getLongUnit()
	{
		if(_longUnit==null)
		{
			return "";
		}
		else
		{
			return _longUnit;
		}
	}
	
	public void setLongUnit(String unit)
	{
		_longUnit = unit;
		setDirtyFlag(true);
	}
	
	public String getShortUnit()
	{
		return _shortUnit;
	}
	
	public void setShortUnit(String unit)
	{
		_shortUnit = unit;
		setDirtyFlag(true);
	}
	
	public int getPrecision()
	{
		return _precision;
	}
	
	public void setPrecision(int precision)
	{
		if(precision>=0)
		{
			_precision = precision;
		}
		else
		{
			_precision=0;
		}
		rounder = 1;
		for(int i=0;i<_precision;i++)
		{
			rounder = rounder * 10;
		}
		setDirtyFlag(true);
	}
	
	public float getOffset()
	{
		return _offset;
	}
	public void setOffset(float o)
	{
		_offset = o;
		setDirtyFlag(true);
	}	
	
	public float getFactor()
	{
		return _factor;
	}	
	public void setFactor(float f)
	{
		_factor = f;
		setDirtyFlag(true);
	}	
	
	public boolean getSpeechEnabled()
	{
		return !_silent;
	}
	
	public boolean getSilent()
	{
		return _silent;
	}
	
	public void setSilent(boolean setSilent)
	{
		_silent = setSilent;
		setDirtyFlag(true);
	}

	public void setSpeechEnabled(boolean speech)
	{
		_silent = !speech;
		setDirtyFlag(true);
	}

	public Context getContext()
	{
		
		return FrSkyServer.getContext();
		//return FrSkyDash.getContext();
		//return _context;
	}
	
//	public void setContext(Context context)
//	{
//		_context = context;
//	}
	
	
	// ==========================================================================================
	// ====                        CHANNEL METHODS                                          =====
	// ==========================================================================================	
	public double setRaw(int value)
	{
		return setRaw((double) value);
	}
	
	public double setRaw(double value)
	{
		timestamp = new Date();
		_avg = _stack.push(value);

		
		//Log.d(TAG,_name+" setting new input value to "+value);
		_raw = value;
		//_val = (_avg * _factor)+_offset;
		_val = convert(_avg);
		double outVal =getValue(true);
		//Log.d(TAG,_name+" new outValue should now be "+outVal);
		
		// send new avg value to listeners
		// do not use direct listeners
//		for(OnChannelListener ch : _listeners)
//		{
//			//Log.d(TAG,"\t"+_name+" send to listener");
//			ch.onSourceUpdate(outVal);
//		}
		
		// send to broadcast receivers
		//if((_context!=null) && (_channelId!=-1))
		if(_channelId!=-1)
		{
			String bCastAction = MESSAGE_CHANNEL_UPDATED+_channelId;
			//Log.d(TAG,"Send broadcast of value to ANY listener on context "+_context+", using message: "+bCastAction);
			
			Intent i = new Intent();
			i.setAction(bCastAction);
			i.putExtra("channelValue", outVal);
			FrSkyServer.getContext().sendBroadcast(i);
		}
		return outVal;
	}
	
	
	
	
	
	
	
	
	public double getValue()
	{
		return getValue(false);
	}
	
	public double getValue(boolean average)
	{
		//Log.i(TAG, _name+" Try to calculate new outValue");
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
	
	/**
	 * 
	 * @return The description of the Channel
	 * 
	 * eso: This is used when the channel are added to adapters for the spinners
	 */
	@Override
	public String toString() {
		return getDescription();
//		return "Channel [_raw=" + _raw + ", _val=" + _val + ", _avg=" + _avg
//				+ ", _description=" + _description + ", _movingAverage="
//				+ _movingAverage + ", _modelId=" + _modelId + ", _channelId="
//				+ _channelId + "]";
	}

	public String toValueString()
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

//	public void setFrSkyAlarm(int number,int threshold,int greaterthan,int level)
//	{
//		alarms[number] = new Alarm(Alarm.ALARMTYPE_FRSKY,level,greaterthan,threshold);
//		alarmCount += 1;
//		setDirtyFlag(true);
//	}

	
	
	
	
	// ==========================================================================================
	// ====                        INTER CHANNEL COMMUNICATION                              =====
	// ==========================================================================================
	

	public void listenTo(Channel channel)
	{
		listenTo(channel.getId());
	}
	public void listenTo(long channelId)
	{
		if(channelId!=-1)
		{
			if(listening)	// already listening to something
			{
				// remove existing listener before allowing to add new one
				try
				{
					FrSkyServer.getContext().unregisterReceiver(mChannelUpdateReceiver);
				}
				catch (Exception e){
					Log.e(TAG,e.getMessage());
					
				}				
				
			}
			
			
			mIntentFilter = new IntentFilter();
			String bCastAction = MESSAGE_CHANNEL_UPDATED+channelId;
			_sourceChannelId = channelId;
			if(FrSkyServer.D)Log.d(TAG,_description+": Added broadcast listener");
			
		    mIntentFilter.addAction(bCastAction);
		    listening=true;
		    
		    // TODO: try to use "this" as receiver instead of mChannelUpdateReceiver
		    
		    FrSkyServer.getContext().registerReceiver(mChannelUpdateReceiver, mIntentFilter);	  // Used to receive messages from Server

		}
		else
		{
			listening=false;
			_sourceChannelId = channelId;
			
			if(FrSkyServer.D)Log.d(TAG,_description+": Removed broadcast listener");
			try
			{
				FrSkyServer.getContext().unregisterReceiver(mChannelUpdateReceiver);
			}
			catch (Exception e){Log.e(TAG,e.getMessage());}
		}
		setDirtyFlag(true);
	}
	
	private BroadcastReceiver mChannelUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	String msg = intent.getAction();
        	Bundle extras = intent.getExtras();
        	//Log.i(TAG,"Received Broadcast: '"+msg+"'");
        	// no purpose to compare msg, since we should only listen to relevant broadcasts..
        	//Log.i(TAG,"Comparing '"+msg+"' to '"+FrSkyServer.MESSAGE_SPEAKERCHANGE+"'");
        	//if(msg.equals(FrSkyServer.MESSAGE_STARTED))
        	//	Log.i(TAG,"I have received BroadCast that the server has started");
        	// Get the value..
        	double val = intent.getDoubleExtra("channelValue", -1);
        	if(FrSkyServer.D) Log.w(TAG,_description +" on model "+_modelId+" received broadcast input value "+val);
        	
        	double v = setRaw(val);
    		//Log.d(TAG,_name+" updated by parent to "+val+" -> "+v+" "+_shortUnit);

        }
    };	
	
    //TODO: Deprecate
//  	public void onSourceUpdate(double sourceValue)
//  	{
//  		
//  		double v = setRaw(sourceValue);
//  		Log.d(TAG,_name+" updated by parent to "+sourceValue+" -> "+v+" "+_shortUnit);
//  	}
    
    
	// ==========================================================================================
	// ====                        UTILITY METHODS                                          =====
	// ==========================================================================================
	public String toVoiceString()
	{
		return getDescription()+": "+toValueString()+" "+getLongUnit();
	}
	
	public void reset()
	{
		if(FrSkyServer.D)Log.d(TAG,_description+": Resetting self");
		_raw = -1;
		_val = -1;
		_avg = 0;
		raw = _raw;
		rawAvg = _avg;
		eng = _val;
		_stack = new MyStack(_movingAverage);
		listenTo(-1);	// force unregister receiver
		setDirtyFlag(true);
	}
	
	private double convert(double inputValue)
	{
		double o = (inputValue * _factor)+_offset;
		//Log.d(TAG,_name+" convert from inputvalue ("+inputValue+") to outputvalue ("+o+")");
		return Math.round(o*rounder)/rounder;
	}
	
	
	//TODO: Deprecate
//	public void addListener(OnChannelListener channel)
//	{
//		_listeners.add(channel);
//		setDirtyFlag(true);
//	}
	
	public String toCsv()
	{
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(String.format("%."+_precision+"f",convert(_raw))+delim);
		sb.append(String.format("%."+_precision+"f",convert(_avg))+delim);
		return sb.toString();
	}
	public String toCsvHeader()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%s (%s)",_description,_longUnit)+delim);
		sb.append(String.format("%s (Averaged) (%s)",_description,_longUnit)+delim);
		
		return sb.toString();
	}
	

	// ==========================================================================================
	// ====                        COMPAREABLE                                              =====
	// ==========================================================================================


	@Override
	public int compare(Channel lhs, Channel rhs) {
		// TODO Auto-generated method stub
		
		if(lhs.getId()==rhs.getId()) return 0;
		if(lhs.getId()<rhs.getId()) return -1;
		else return 1;
	}

	
	@Override public boolean equals(Object o) {
		if (this == o) {
		   return true;
		}

	    // Return false if the other object has the wrong type.
	    // This type may be an interface depending on the interface's specification.
	    if (!(o instanceof Channel)) {
	       return false;
	    }
		Channel channel = (Channel) o;
	    if(this.getId()==channel.getId())
		{
			return true;
		}
		else
		{
			return false;
		}
		
		
	}
	
	// ==========================================================================================
	// ====                        PARCELABLE                                               =====
	// ==========================================================================================
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
 
		// We just need to write each field into the
		// parcel. When we read from parcel, they
		// will come back in the same order
		
		dest.writeInt(_channelId);
		//dest.writeString(_name);
		dest.writeString(_description);
		dest.writeString(_longUnit);
		dest.writeString(_shortUnit);
		dest.writeFloat(_factor);
		dest.writeFloat(_offset);
		dest.writeInt(_movingAverage);
		dest.writeInt(_precision);
		dest.writeByte((byte) (_silent ? 1 : 0));
		dest.writeLong(_sourceChannelId);
		dest.writeInt(_modelId);

	}
 

	private void readFromParcel(Parcel in) {
 
		// We just need to read back each
		// field in the order that it was
		// written to the parcel
		_channelId = in.readInt();
		//_name = in.readString();
		_description = in.readString();
		_longUnit = in.readString();
		_shortUnit = in.readString();
		_factor = in.readFloat();
		_offset = in.readFloat();
		setMovingAverage(in.readInt());
		setPrecision(in.readInt());
		_silent = in.readByte()==1;
		_sourceChannelId = in.readLong();
		//listenTo(in.readLong());
		_modelId = in.readInt();
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
}
