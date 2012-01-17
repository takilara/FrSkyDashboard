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
	//private long _id;
	private double _avg;
	private String _name;
	private String _description;
	private long _sourceChannelId;
	private float _offset;
	private float _factor;
	private int _precision;
	private Context _context;
	private int _textViewId=-1;
	
	private IntentFilter mIntentFilter;
	
	private String _shortUnit;
	private String _longUnit;
	private int _movingAverage;
	private MathContext _mc;
	public boolean _silent;
	public Date timestamp;
	
	public Alarm[] alarms;
	public int alarmCount = 0;
	private long _modelId = -1;
	private long _channelId = -1;
	private boolean _dirty = false;
	
	private static DBAdapterChannel db;
	
	private ArrayList <OnChannelListener> _listeners;

	//public static final Channel AD1 = new Channel("ad1","FrSky AD1",(float)0,(float)1,"","");
	//public static final Channel AD2 = new Channel("ad2","FrSky AD2",(float)0,(float)1,"","");
	
	private MyStack _stack;
	SharedPreferences _settings;
	SharedPreferences.Editor editor;

	public Channel(Context context)
	{
		this(context,"derived","description",(float)0,(float)1,"Symbol","UnitName");
	}
	
	public Channel(Parcel in)
	{
		readFromParcel(in);
	}
	
	
	public Channel(Context context,String name,String description,float offset,float factor,String unit,String longUnit)
	{
		// instanciate listeners list
		_listeners = new ArrayList<OnChannelListener> ();
		_sourceChannelId = -1;
		rounder=1;
		_silent = false;
		_precision = 2;
		
		_name = name;
		_description = description;
		_offset = offset;
		_factor = factor;
		_shortUnit = unit;
		_longUnit = longUnit;
		_mc = new MathContext(2);
		_context = context;
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
		alarms = new Alarm[2];
		
		db = new DBAdapterChannel(context);
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
	
	public void setId(long channelId)
	{
		_channelId = channelId;
		setDirtyFlag(true);
	}
	public long getId()
	{
		return _channelId;
	}
	
	public void setModelId(Model model)
	{
		_modelId = model.getId();
		setDirtyFlag(true);
	}
	public void setModelId(long modelId)
	{
		_modelId = modelId;
		setDirtyFlag(true);
	}
	
	public long getModelId()
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
	
	public String getName()
	{
		return _name;
	}
	public void setName(String n)
	{
		_name = n;
		setDirtyFlag(true);
	}
	
	public String getLongUnit()
	{
		return _longUnit;
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
		_precision = precision;
		rounder = 1;
		for(int i=0;i<precision;i++)
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
		for(OnChannelListener ch : _listeners)
		{
			//Log.d(TAG,"\t"+_name+" send to listener");
			ch.onSourceUpdate(outVal);
		}
		
		// send to broadcast receivers
		if((_context!=null) && (_channelId!=-1))
		{
			String bCastAction = MESSAGE_CHANNEL_UPDATED+_channelId;
			//Log.d(TAG,"Send broadcast of value to ANY listener on context "+_context+", using message: "+bCastAction);
			
			Intent i = new Intent();
			i.setAction(bCastAction);
			i.putExtra("channelValue", outVal);
			_context.sendBroadcast(i);
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
	
	public String toString()
	{
		return getDescription();
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

	public void setFrSkyAlarm(int number,int threshold,int greaterthan,int level)
	{
		alarms[number] = new Alarm(Alarm.ALARMTYPE_FRSKY,level,greaterthan,threshold);
		alarmCount += 1;
		setDirtyFlag(true);
	}

	
	
	
	
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
			mIntentFilter = new IntentFilter();
			String bCastAction = MESSAGE_CHANNEL_UPDATED+channelId;
			_sourceChannelId = channelId;
			Log.d(TAG,"Listens for broadcast of values to on context "+_context+", with message: "+bCastAction);
			
		    mIntentFilter.addAction(bCastAction);
		    Log.d(TAG,"Context is : "+_context);
		    _context.registerReceiver(mChannelUpdateReceiver, mIntentFilter);	  // Used to receive messages from Server
		}
		else
		{
			try
			{
				_context.unregisterReceiver(mChannelUpdateReceiver);
			}
			catch (Exception e)
			{
				
			}
		}
		setDirtyFlag(true);
	}
	
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
	
    //TODO: Deprecate
  	public void onSourceUpdate(double sourceValue)
  	{
  		
  		double v = setRaw(sourceValue);
  		Log.d(TAG,_name+" updated by parent to "+sourceValue+" -> "+v+" "+_shortUnit);
  	}
    
    
	// ==========================================================================================
	// ====                        UTILITY METHODS                                          =====
	// ==========================================================================================
	public String toVoiceString()
	{
		return getDescription()+": "+toValueString()+" "+getLongUnit();
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
		setDirtyFlag(true);
	}
	
	private double convert(double inputValue)
	{
		double o = (inputValue * _factor)+_offset;
		Log.d(TAG,_name+" convert from inputvalue ("+inputValue+") to outputvalue ("+o+")");
		return Math.round(o*rounder)/rounder;
	}
	
	
	//TODO: Deprecate
	public void addListener(OnChannelListener channel)
	{
		_listeners.add(channel);
		setDirtyFlag(true);
	}
	
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
		
		dest.writeLong(_channelId);
		dest.writeString(_name);
		dest.writeString(_description);
		dest.writeString(_longUnit);
		dest.writeString(_shortUnit);
		dest.writeFloat(_factor);
		dest.writeFloat(_offset);
		dest.writeInt(_movingAverage);
		dest.writeInt(_precision);
		dest.writeByte((byte) (_silent ? 1 : 0));
		dest.writeLong(_sourceChannelId);
		dest.writeLong(_modelId);

	}
 

	private void readFromParcel(Parcel in) {
 
		// We just need to read back each
		// field in the order that it was
		// written to the parcel
		_channelId = in.readLong();
		_name = in.readString();
		_description = in.readString();
		_longUnit = in.readString();
		_shortUnit = in.readString();
		_factor = in.readFloat();
		_offset = in.readFloat();
		_movingAverage = in.readInt();
		_precision = in.readInt();
		_silent = in.readByte()==1;
		_sourceChannelId = in.readLong();
		_modelId = in.readLong();
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

    
    
    
	// ==========================================================================================
	// ====                        DATABASE ACCESS                                          =====
	// ==========================================================================================
    public void saveToDatabase()
	{
		if(_channelId==-1)
		{
			if(DEBUG) Log.d(TAG,"Saving, using insert");
			db.open();
			long id = db.insertChannel(this);
			if(id==-1)
			{
				Log.e(TAG,"Insert Failed");
			}
			else
			{
				if(DEBUG) Log.d(TAG,"Insert ok, id:"+id);
				_channelId = id;
				setDirtyFlag(false);
			}
			db.close();
			// Run insert
		}
		else
		{
			if(DEBUG) Log.d(TAG,"Saving, using update (id:"+_channelId+",description:"+_description+", silent="+getSilent()+")");
			db.open();
			if(db.updateChannel(this))
			{
				if(DEBUG)Log.d(TAG,"Update successful");
				setDirtyFlag(false);
			}
			else
			{
				if(DEBUG)Log.e(TAG,"Update failed");
			}
			db.close();
			// run update
		}
	}
	
    public boolean loadFromDatabase(Cursor c)
	{
    	_channelId = c.getLong(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_ROWID));
		_description = c.getString(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_DESCRIPTION));
		_longUnit = c.getString(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_LONGUNIT));
		_shortUnit = c.getString(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_SHORTUNIT));
		_factor = c.getFloat(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_FACTOR));
		_offset = c.getFloat(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_OFFSET));
		_precision = c.getInt(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_PRECISION));
		_movingAverage = c.getInt(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_MOVINGAVERAGE));
		_silent = c.getInt(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_SILENT))>0;
		_modelId = c.getLong(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_MODELID));
		listenTo(c.getInt(c.getColumnIndexOrThrow(DBAdapterChannel.KEY_SOURCECHANNELID)));
		//db.close();
		
		if(DEBUG) Log.d(TAG,"Loaded '"+getDescription()+"' from database");
		if(DEBUG) Log.d(TAG,"\tSilent:\t"+getSilent());
		setDirtyFlag(false);	// clean after loaded
		return true;
	}
	public boolean loadFromDatabase(long id)
	{
		// False if not found
		db.open();
		Cursor c = db.getChannel(id);
		
		if(c.getCount()==0)
		{
			if(DEBUG) Log.w(TAG,"Channel id "+id+" does not exist.");	
			_channelId= -1;
			c.deactivate();
			db.close();
			return false;
		}
		else
		{
			if(DEBUG) Log.d(TAG,"Found the channel");
			loadFromDatabase(c);
			c.deactivate();
			db.close();
			return true;
		}
	}
	
	public void deleteFromDatabase()
	{
		db.open();
		try
		{
			db.deleteChannel(this._channelId);
		}
		catch(Exception e)
		{
			if(DEBUG) Log.e(TAG,e.toString());
		}
		db.close();
	}
	
	
	
	// ==========================================================================================
	// ====                        STATIC METHODS                                           =====
	// ==========================================================================================
	
	public static Channel[] getChannelsForModel(Context context, Model model)
	{
		//DBAdapterChannel dbb = new DBAdapterChannel(context);
		if(DEBUG) Log.d(TAG,"Try to open channels database");
		db.open();
		if(DEBUG) Log.d(TAG,"Db opened, try to get all channels");
		Cursor c = db.getAllChannelsForModel(model.getId());
		//dbb.close();
		if(DEBUG) Log.d(TAG,"Cursor count: "+c.getCount());
		c.moveToFirst();
		Channel[] channels = new Channel[c.getCount()];
		
		int n = 0;
		while(!c.isAfterLast())
		{
				
			if(DEBUG) Log.d(TAG,"Add Channel "+c.getString(1)+" to channellist");
				channels[n] = new Channel(context);
				channels[n].loadFromDatabase(c);
				c.moveToNext();
				n++;
		}
		c.deactivate();
		db.close();
		return channels;
	}
	
	public static void deleteChannelsForModel(Context context, Model model)
	{
		//DBAdapterChannel dbb = new DBAdapterChannel(context);
		if(DEBUG) Log.d(TAG,"Try to open channels database");
		db.open();
		if(DEBUG) Log.d(TAG,"Db opened, try to delete all channels for model "+model.getId());
		db.deleteAllChannelsForModel(model.getId());
		db.close();
	}
}
