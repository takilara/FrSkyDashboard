package biz.onomato.frskydash;

import android.util.Log;
import java.math.MathContext;


public class Channel {
	private static final String TAG = "Channel";
	private int _raw;
	private double _val;
	private int _avg;
	private String _name;
	private String _description;
	private double _offset;
	private double _factor;
	private int _precision;
	 
	private String _unit;
	private String _longUnit;
	private MathContext _mc;
	public boolean silent;
	private MyStack _stack;
	
	public Channel(String name,String description,double offset,double factor,String unit,String longUnit)
	{
		silent = false;
		_raw=-1;
		_val=-1;
		_avg=0;
		_name = name;
		_description = description;
		_offset = offset;
		_factor = factor;
		_unit = unit;
		_longUnit = longUnit;
		_mc = new MathContext(2);
		_precision = 2;
		_stack = new MyStack(10);
		
	}
	
	public void setMovingAverage(int Size)
	{
		if(Size<1)
		{
			Size = 1;
		}
		_stack = new MyStack(Size);
	}
	
	public double setRaw(int raw)
	{
		_avg = _stack.push(raw);
		
		//Log.i(TAG,"STACK: "+_stack.toString());
		//Log.i(TAG,"Avg: "+_avg);
		_raw = raw;
		//_val = _raw * _factor+_offset;
		_val = _avg * _factor+_offset;
		return getValue();
	}
	
	public void setPrecision(int precision)
	{
		_precision = precision;
	}
	
	// Getters
	public double getValue()
	{
		double tVal = Math.round(_avg*100f)/100f;
		return tVal;
	}
	
	public String toString()
	{
		return String.format("%."+_precision+"f", getValue());
	}
	
	public String getDescription()
	{
		return _description;
	}
	public String getName()
	{
		return _name;
	}
	public String getLongUnit()
	{
		return _longUnit;
	}
	
	public String toVoiceString()
	{
		return getDescription()+": "+toString()+" "+getLongUnit();
	}

}
