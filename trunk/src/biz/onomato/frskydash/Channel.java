package biz.onomato.frskydash;

import android.content.SharedPreferences;
import android.util.Log;
import java.math.MathContext;
import java.util.List;


public class Channel {
	private static final String TAG = "Channel";
	
	public static final int CHANNELTYPE_AD1=0;
	public static final int CHANNELTYPE_AD2=1;
	public static final int CHANNELTYPE_RSSI=2;
	public static final String crlf="\r\n";
	public static final String delim=";";
	
	private int _raw;
	private double _val;
	private int _avg;
	private String _name;
	private String _description;
	private float _offset;
	private float _factor;
	private int _precision;
	 
	private String _shortUnit;
	private String _longUnit;
	private int _movingAverage;
	private MathContext _mc;
	public boolean silent;
	
	public Alarm[] alarms;
	public int alarmCount = 0;
	
	
	private MyStack _stack;
	SharedPreferences _settings;
	SharedPreferences.Editor editor;

	
	
	
	public Channel(String name,String description,float offset,float factor,String unit,String longUnit)
	{
		silent = false;
		_precision = 2;
		
		_name = name;
		_description = description;
		_offset = offset;
		_factor = factor;
		_shortUnit = unit;
		_longUnit = longUnit;
		_mc = new MathContext(2);

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
	
	public void reset()
	{
		_raw = -1;
		_val = -1;
		_avg = 0;
		_stack = new MyStack(_movingAverage);
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
	
	public double setRaw(int raw)
	{
		_avg = _stack.push(raw);
		
		//Log.i(TAG,"STACK: "+_stack.toString());
		//Log.i(TAG,"Avg: "+_avg);
		_raw = raw;
		//_val = (_avg * _factor)+_offset;
		_val = convert(_avg);
		return getValue();
	}
	
	private double convert(int inputValue)
	{
		double o = (inputValue * _factor)+_offset;
		return o;
	}
	
	public void setPrecision(int precision)
	{
		_precision = precision;
	}
	
	
	// Getters
	public double getValue()
	{
		double tVal = Math.round(_val*100f)/100f;
		
		return tVal;
		//return getValue(_avg);
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
	

	public String toEng(int inputValue)
	{
		//return String.format("%s %s", getValue(inputValue),_shortUnit);
		return toEng(inputValue,false);
	}
	
	public String toEng(int inputValue,boolean longUnit)
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
}
