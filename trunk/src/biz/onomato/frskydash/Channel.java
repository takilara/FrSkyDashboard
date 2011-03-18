package biz.onomato.frskydash;

public class Channel {
	private int _raw;
	private float _val;
	private String _name;
	private String _description;
	private float _offset;
	private float _factor;
	private String _unit;
	private String _longUnit;
	public Channel(String name,String description,float offset,float factor,String unit,String longUnit)
	{
		_raw=-1;
		_val=-1;
		_name = name;
		_description = description;
		_offset = offset;
		_factor = factor;
		_unit = unit;
		_longUnit = longUnit;
	}
	
	public float setRaw(int raw)
	{
		_raw = raw;
		_val = _raw * _factor+_offset;
		return _val;
	}
	
	// Getters
	public float getValue()
	{
		return _val;
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
