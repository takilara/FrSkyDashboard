package biz.onomato.frskydash;

import android.util.Log;
import java.math.MathContext;


public class Channel {
	private static final String TAG = "Channel";
	private int _raw;
	private double _val;
	private String _name;
	private String _description;
	private double _offset;
	private double _factor;
	private int _precision;
	private String _unit;
	private String _longUnit;
	private MathContext _mc;
	public Channel(String name,String description,double offset,double factor,String unit,String longUnit)
	{
		_raw=-1;
		_val=-1;
		_name = name;
		_description = description;
		_offset = offset;
		_factor = factor;
		_unit = unit;
		_longUnit = longUnit;
		_mc = new MathContext(2);
		_precision = 2;
	}
	
	public double setRaw(int raw)
	{
		_raw = raw;
		_val = _raw * _factor+_offset;
		return getValue();
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

}
